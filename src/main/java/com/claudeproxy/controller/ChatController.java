package com.claudeproxy.controller;

import com.claudeproxy.domain.ChatRoom;
import com.claudeproxy.domain.Message;
import com.claudeproxy.dto.SendMessageRequest;
import com.claudeproxy.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    // ─── Thymeleaf Pages ─────────────────────────────────────────────────────

    @GetMapping("/")
    public String index(org.springframework.ui.Model model, Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return "redirect:/login";
        }
        List<ChatRoom> rooms = chatService.getAllChatRooms();
        model.addAttribute("rooms", rooms);
        return "index";
    }

    /** Public page — anyone with the URL can read */
    @GetMapping("/chat/{id}")
    public String chatRoom(@PathVariable UUID id, Model model) {
        ChatRoom room = chatService.getChatRoom(id);
        List<Message> messages = chatService.getMessages(id);
        model.addAttribute("room", room);
        model.addAttribute("messages", messages);
        model.addAttribute("chatRoomId", id.toString());
        return "chat";
    }

    // ─── REST API ─────────────────────────────────────────────────────────────

    /** Create a new chat room. Requires authentication. */
    @PostMapping("/api/chat")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createChatRoom() {
        ChatRoom room = chatService.createChatRoom();
        return ResponseEntity.ok(Map.of(
                "id", room.getId().toString(),
                "title", room.getTitle()
        ));
    }

    /**
     * Send a user message and get Claude's response.
     * Requires authentication.
     */
    @PostMapping("/api/chat/{id}/messages")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> sendMessage(
            @PathVariable UUID id,
            @RequestBody @Valid SendMessageRequest request) {
        try {
            chatService.sendMessage(id, request.getContent());
            List<Message> messages = chatService.getMessages(id);
            return ResponseEntity.ok(Map.of("messages", toJson(messages)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Claude API 호출 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    /**
     * Fetch all messages for a chat room. Public — used for PC polling.
     */
    @GetMapping("/api/chat/{id}/messages")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getMessages(@PathVariable UUID id) {
        try {
            List<Message> messages = chatService.getMessages(id);
            return ResponseEntity.ok(Map.of("messages", toJson(messages)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    private List<Map<String, String>> toJson(List<Message> messages) {
        return messages.stream()
                .map(m -> Map.of(
                        "id", m.getId().toString(),
                        "role", m.getRole(),
                        "content", m.getContent(),
                        "createdAt", m.getCreatedAt().toString()
                ))
                .collect(Collectors.toList());
    }
}

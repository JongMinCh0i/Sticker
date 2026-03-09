package com.claudeproxy.service;

import com.claudeproxy.domain.ChatRoom;
import com.claudeproxy.domain.Message;
import com.claudeproxy.repository.ChatRoomRepository;
import com.claudeproxy.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final MessageRepository messageRepository;
    private final ClaudeService claudeService;

    @Transactional
    public ChatRoom createChatRoom() {
        ChatRoom room = new ChatRoom();
        room.setTitle("새 대화");
        return chatRoomRepository.save(room);
    }

    @Transactional(readOnly = true)
    public ChatRoom getChatRoom(UUID id) {
        return chatRoomRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Chat room not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<ChatRoom> getAllChatRooms() {
        return chatRoomRepository.findAllByOrderByUpdatedAtDesc();
    }

    /**
     * Saves the user message, calls Claude with full history, saves the assistant reply.
     * Returns the assistant reply message.
     */
    @Transactional
    public Message sendMessage(UUID chatRoomId, String userContent) {
        ChatRoom chatRoom = getChatRoom(chatRoomId);

        // Persist user message
        Message userMessage = new Message();
        userMessage.setChatRoom(chatRoom);
        userMessage.setRole("user");
        userMessage.setContent(userContent);
        messageRepository.save(userMessage);

        // Set title from first user message (first 20 chars)
        if ("새 대화".equals(chatRoom.getTitle())) {
            String title = userContent.length() > 20
                    ? userContent.substring(0, 20) + "…"
                    : userContent;
            chatRoom.setTitle(title);
        }

        // Build full conversation history for multi-turn context
        List<Message> history = messageRepository.findByChatRoomOrderByCreatedAtAsc(chatRoom);

        // Call Claude API
        String assistantContent = claudeService.chat(history);

        // Persist assistant reply
        Message assistantMessage = new Message();
        assistantMessage.setChatRoom(chatRoom);
        assistantMessage.setRole("assistant");
        assistantMessage.setContent(assistantContent);
        messageRepository.save(assistantMessage);

        // Touch updatedAt on chat room so the list sorts correctly
        chatRoomRepository.save(chatRoom);

        return assistantMessage;
    }

    @Transactional(readOnly = true)
    public List<Message> getMessages(UUID chatRoomId) {
        ChatRoom chatRoom = getChatRoom(chatRoomId);
        return messageRepository.findByChatRoomOrderByCreatedAtAsc(chatRoom);
    }
}

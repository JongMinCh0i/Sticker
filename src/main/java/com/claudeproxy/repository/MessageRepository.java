package com.claudeproxy.repository;

import com.claudeproxy.domain.ChatRoom;
import com.claudeproxy.domain.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> {
    List<Message> findByChatRoomOrderByCreatedAtAsc(ChatRoom chatRoom);
}

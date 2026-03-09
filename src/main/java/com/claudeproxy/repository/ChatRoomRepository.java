package com.claudeproxy.repository;

import com.claudeproxy.domain.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, UUID> {
    List<ChatRoom> findAllByOrderByUpdatedAtDesc();
}

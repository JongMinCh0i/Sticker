package com.claudeproxy.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${app.mail.enabled:false}")
    private boolean enabled;

    @Value("${app.mail.to:}")
    private String to;

    @Value("${spring.mail.username:}")
    private String from;

    @Async
    public void sendAssistantReply(String chatRoomTitle, String userMessage, String assistantReply) {
        if (!enabled || to.isBlank()) {
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject("[Sticker] " + chatRoomTitle);
        message.setText(
                "질문:\n" + userMessage + "\n\n" +
                "답변:\n" + assistantReply
        );

        try {
            mailSender.send(message);
            log.info("Reply email sent to {}", to);
        } catch (Exception e) {
            log.error("Failed to send reply email", e);
        }
    }
}

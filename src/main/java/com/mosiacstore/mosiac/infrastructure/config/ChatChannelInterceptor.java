package com.mosiacstore.mosiac.infrastructure.config;

import com.mosiacstore.mosiac.infrastructure.security.CustomUserDetail;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.context.event.EventListener;

import java.security.Principal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class ChatChannelInterceptor implements ChannelInterceptor {

    private final Map<String, String> activeUsers = new ConcurrentHashMap<>();

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            Object auth = accessor.getUser();
            if (auth instanceof UsernamePasswordAuthenticationToken) {
                CustomUserDetail userDetail = (CustomUserDetail) ((UsernamePasswordAuthenticationToken) auth).getPrincipal();
                accessor.setUser((Principal) auth);
                String sessionId = accessor.getSessionId();
                activeUsers.put(sessionId, userDetail.getUsername());
                log.info("User connected: {}", userDetail.getUsername());
            }
        }

        return message;
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headers = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headers.getSessionId();

        if (activeUsers.containsKey(sessionId)) {
            String username = activeUsers.remove(sessionId);
            log.info("User disconnected: {}", username);

            // Send user offline status to all connected clients
            SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
            headerAccessor.setSessionId(sessionId);
            headerAccessor.setLeaveMutable(true);
        }
    }

    public Map<String, String> getActiveUsers() {
        return activeUsers;
    }
}
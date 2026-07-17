package prm393.group8.flowermanagement.websocket;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import prm393.group8.flowermanagement.dto.ChatMessageDto;
import prm393.group8.flowermanagement.entity.Conversation;
import prm393.group8.flowermanagement.entity.User;
import prm393.group8.flowermanagement.service.ChatService;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

// Plain (non-STOMP) WebSocket handler: each user (customer or admin) keeps a single
// live session keyed by userId, reusing the same "account"/"adminInfo" HttpSession
// attributes the rest of the app authenticates with (copied in at handshake time).
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final ChatService chatService;
    private final ObjectMapper objectMapper;

    private final Map<Integer, WebSocketSession> userSessions = new ConcurrentHashMap<>();
    private final Set<Integer> adminUserIds = ConcurrentHashMap.newKeySet();

    public ChatWebSocketHandler(ChatService chatService, ObjectMapper objectMapper) {
        this.chatService = chatService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        User user = currentUser(session);
        if (user == null) {
            session.close(CloseStatus.POLICY_VIOLATION.withReason("Not authenticated"));
            return;
        }
        userSessions.put(user.getUserId(), session);
        if (isAdmin(user)) {
            adminUserIds.add(user.getUserId());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        User user = currentUser(session);
        if (user != null) {
            userSessions.remove(user.getUserId(), session);
            adminUserIds.remove(user.getUserId());
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        User sender = currentUser(session);
        if (sender == null) return;

        Map<String, Object> payload;
        try {
            payload = objectMapper.readValue(message.getPayload(), new TypeReference<Map<String, Object>>() {});
        } catch (IOException e) {
            return;
        }

        Object contentObj = payload.get("content");
        if (!(contentObj instanceof String content) || content.isBlank()) {
            return;
        }

        int conversationId;
        if (isAdmin(sender)) {
            Object convIdObj = payload.get("conversationId");
            if (!(convIdObj instanceof Number number)) return;
            conversationId = number.intValue();
        } else {
            conversationId = chatService.getOrCreateConversation(sender).getConversationId();
        }

        ChatMessageDto dto = chatService.sendMessage(conversationId, sender, content);
        notifyMessageSent(dto, sender);
    }

    // Called after a message is persisted (from here or from the REST fallback endpoint)
    // to push the new message and refreshed unread counts to both parties live.
    public void notifyMessageSent(ChatMessageDto dto, User sender) {
        broadcastMessage(dto);
        Conversation conversation = chatService.getConversationOrThrow(dto.getConversationId());
        if (isAdmin(sender)) {
            pushUnreadCount(conversation.getCustomer().getUserId(),
                    chatService.getUnreadCountForCustomer(conversation.getCustomer()));
        } else {
            pushUnreadCountToAdmins(chatService.getUnreadCountForAdmin());
        }
    }

    public void notifyRead(Conversation conversation, User reader) {
        if (isAdmin(reader)) {
            pushUnreadCountToAdmins(chatService.getUnreadCountForAdmin());
        } else {
            pushUnreadCount(reader.getUserId(), chatService.getUnreadCountForCustomer(reader));
        }
    }

    private void broadcastMessage(ChatMessageDto dto) {
        Conversation conversation = chatService.getConversationOrThrow(dto.getConversationId());
        String json = writeJson(Map.of("type", "message", "payload", dto));
        if (json == null) return;

        sendIfOpen(userSessions.get(conversation.getCustomer().getUserId()), json);
        for (Integer adminId : adminUserIds) {
            sendIfOpen(userSessions.get(adminId), json);
        }
    }

    private void pushUnreadCountToAdmins(long count) {
        for (Integer adminId : adminUserIds) {
            pushUnreadCount(adminId, count);
        }
    }

    private void pushUnreadCount(int userId, long count) {
        WebSocketSession session = userSessions.get(userId);
        if (session == null) return;
        String json = writeJson(Map.of("type", "unread_count", "count", count));
        if (json != null) {
            sendIfOpen(session, json);
        }
    }

    private void sendIfOpen(WebSocketSession session, String json) {
        if (session == null || !session.isOpen()) return;
        try {
            synchronized (session) {
                session.sendMessage(new TextMessage(json));
            }
        } catch (IOException ignored) {
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (IOException e) {
            return null;
        }
    }

    private User currentUser(WebSocketSession session) {
        Object account = session.getAttributes().get("account");
        if (account instanceof User user) return user;
        Object adminInfo = session.getAttributes().get("adminInfo");
        if (adminInfo instanceof User user) return user;
        return null;
    }

    private boolean isAdmin(User user) {
        return user.getRole() != null && "admin".equalsIgnoreCase(user.getRole().getRoleName());
    }
}

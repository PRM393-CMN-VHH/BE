package prm393.group8.flowermanagement.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import prm393.group8.flowermanagement.entity.User;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

// Plain (non-STOMP) WebSocket handler for server-push order/notification events.
// Same session-tracking pattern as ChatWebSocketHandler: one live session per userId,
// authenticated by reusing the "account"/"adminInfo" HttpSession attributes copied in
// at handshake time. This handler only pushes — it doesn't process incoming messages.
@Component
public class OrderWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;

    private final Map<Integer, WebSocketSession> userSessions = new ConcurrentHashMap<>();
    private final Set<Integer> adminUserIds = ConcurrentHashMap.newKeySet();

    public OrderWebSocketHandler(ObjectMapper objectMapper) {
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

    // Pushes an arbitrary {"type": ..., "payload": ...} envelope to one user (customer or admin).
    public void pushToUser(int userId, String type, Object payload) {
        WebSocketSession session = userSessions.get(userId);
        if (session == null) return;
        String json = writeJson(Map.of("type", type, "payload", payload));
        if (json != null) {
            sendIfOpen(session, json);
        }
    }

    // Broadcasts to every currently-connected admin session (e.g. new order placed, order updated).
    public void pushToAdmins(String type, Object payload) {
        String json = writeJson(Map.of("type", type, "payload", payload));
        if (json == null) return;
        for (Integer adminId : adminUserIds) {
            sendIfOpen(userSessions.get(adminId), json);
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

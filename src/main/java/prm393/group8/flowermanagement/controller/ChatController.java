package prm393.group8.flowermanagement.controller;

import prm393.group8.flowermanagement.dto.ChatMessageDto;
import prm393.group8.flowermanagement.entity.Conversation;
import prm393.group8.flowermanagement.entity.User;
import prm393.group8.flowermanagement.service.ChatService;
import prm393.group8.flowermanagement.websocket.ChatWebSocketHandler;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;
    private final ChatWebSocketHandler chatWebSocketHandler;

    public ChatController(ChatService chatService, ChatWebSocketHandler chatWebSocketHandler) {
        this.chatService = chatService;
        this.chatWebSocketHandler = chatWebSocketHandler;
    }

    // [GET] /api/chat/conversation - Customer: get (or create) own conversation + history
    @GetMapping("/conversation")
    public ResponseEntity<?> getMyConversation(HttpSession session) {
        User user = currentUser(session);
        if (user == null) return ResponseEntity.status(401).body(Map.of("error", "Not logged in"));
        if (isAdmin(user)) return ResponseEntity.status(403).body(Map.of("error", "Admin phải dùng /api/chat/conversations"));

        Conversation conversation = chatService.getOrCreateConversation(user);
        List<ChatMessageDto> messages = chatService.getMessages(conversation.getConversationId());
        return ResponseEntity.ok(Map.of("conversationId", conversation.getConversationId(), "messages", messages));
    }

    // [GET] /api/chat/conversations - Admin: list all conversations with last message + unread count
    @GetMapping("/conversations")
    public ResponseEntity<?> getConversations(HttpSession session) {
        User user = currentUser(session);
        if (user == null) return ResponseEntity.status(401).body(Map.of("error", "Not logged in"));
        if (!isAdmin(user)) return ResponseEntity.status(403).body(Map.of("error", "Forbidden"));

        return ResponseEntity.ok(chatService.getConversationsForAdmin());
    }

    // [GET] /api/chat/conversations/{id}/messages - Admin (any) or the owning customer
    @GetMapping("/conversations/{id}/messages")
    public ResponseEntity<?> getConversationMessages(@PathVariable("id") int id, HttpSession session) {
        User user = currentUser(session);
        if (user == null) return ResponseEntity.status(401).body(Map.of("error", "Not logged in"));

        Conversation conversation = chatService.getConversationOrThrow(id);
        if (!isAdmin(user) && conversation.getCustomer().getUserId() != user.getUserId()) {
            return ResponseEntity.status(403).body(Map.of("error", "Forbidden"));
        }

        return ResponseEntity.ok(chatService.getMessages(id));
    }

    // [POST] /api/chat/messages - Customer sends a message in their own conversation (created if needed)
    @PostMapping("/messages")
    public ResponseEntity<?> sendMyMessage(@RequestBody Map<String, String> body, HttpSession session) {
        User user = currentUser(session);
        if (user == null) return ResponseEntity.status(401).body(Map.of("error", "Not logged in"));
        if (isAdmin(user)) return ResponseEntity.status(403).body(Map.of("error", "Admin phải chỉ định conversationId"));

        String content = body.get("content");
        if (content == null || content.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Nội dung tin nhắn không được để trống"));
        }

        Conversation conversation = chatService.getOrCreateConversation(user);
        ChatMessageDto dto = chatService.sendMessage(conversation.getConversationId(), user, content);
        chatWebSocketHandler.notifyMessageSent(dto, user);
        return ResponseEntity.ok(dto);
    }

    // [POST] /api/chat/conversations/{id}/messages - Admin replies in a specific conversation
    @PostMapping("/conversations/{id}/messages")
    public ResponseEntity<?> sendMessage(@PathVariable("id") int id, @RequestBody Map<String, String> body, HttpSession session) {
        User user = currentUser(session);
        if (user == null) return ResponseEntity.status(401).body(Map.of("error", "Not logged in"));

        String content = body.get("content");
        if (content == null || content.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Nội dung tin nhắn không được để trống"));
        }

        Conversation conversation = chatService.getConversationOrThrow(id);
        if (!isAdmin(user) && conversation.getCustomer().getUserId() != user.getUserId()) {
            return ResponseEntity.status(403).body(Map.of("error", "Forbidden"));
        }

        ChatMessageDto dto = chatService.sendMessage(id, user, content);
        chatWebSocketHandler.notifyMessageSent(dto, user);
        return ResponseEntity.ok(dto);
    }

    // [POST] /api/chat/conversations/{id}/read - Mark the other party's messages as read
    @PostMapping("/conversations/{id}/read")
    public ResponseEntity<?> markAsRead(@PathVariable("id") int id, HttpSession session) {
        User user = currentUser(session);
        if (user == null) return ResponseEntity.status(401).body(Map.of("error", "Not logged in"));

        Conversation conversation = chatService.getConversationOrThrow(id);
        if (!isAdmin(user) && conversation.getCustomer().getUserId() != user.getUserId()) {
            return ResponseEntity.status(403).body(Map.of("error", "Forbidden"));
        }

        chatService.markAsRead(id, user);
        chatWebSocketHandler.notifyRead(conversation, user);
        return ResponseEntity.ok(Map.of("message", "OK"));
    }

    // [GET] /api/chat/unread-count - Badge count for the current user (role-aware)
    @GetMapping("/unread-count")
    public ResponseEntity<?> getUnreadCount(HttpSession session) {
        User user = currentUser(session);
        if (user == null) return ResponseEntity.status(401).body(Map.of("error", "Not logged in"));

        long count = isAdmin(user) ? chatService.getUnreadCountForAdmin() : chatService.getUnreadCountForCustomer(user);
        return ResponseEntity.ok(Map.of("unreadCount", count));
    }

    private User currentUser(HttpSession session) {
        User user = (User) session.getAttribute("account");
        if (user == null) user = (User) session.getAttribute("adminInfo");
        return user;
    }

    private boolean isAdmin(User user) {
        return user.getRole() != null && "admin".equalsIgnoreCase(user.getRole().getRoleName());
    }
}

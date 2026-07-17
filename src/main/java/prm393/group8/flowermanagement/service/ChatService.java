package prm393.group8.flowermanagement.service;

import prm393.group8.flowermanagement.dto.ChatMessageDto;
import prm393.group8.flowermanagement.dto.ConversationSummaryDto;
import prm393.group8.flowermanagement.entity.Conversation;
import prm393.group8.flowermanagement.entity.User;

import java.util.List;

public interface ChatService {

    Conversation getOrCreateConversation(User customer);

    Conversation getConversationOrThrow(int conversationId);

    List<ChatMessageDto> getMessages(int conversationId);

    ChatMessageDto sendMessage(int conversationId, User sender, String content);

    void markAsRead(int conversationId, User reader);

    long getUnreadCountForCustomer(User customer);

    long getUnreadCountForAdmin();

    List<ConversationSummaryDto> getConversationsForAdmin();
}

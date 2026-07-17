package prm393.group8.flowermanagement.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import prm393.group8.flowermanagement.dto.ChatMessageDto;
import prm393.group8.flowermanagement.dto.ConversationSummaryDto;
import prm393.group8.flowermanagement.entity.ChatMessage;
import prm393.group8.flowermanagement.entity.Conversation;
import prm393.group8.flowermanagement.entity.User;
import prm393.group8.flowermanagement.repository.ChatMessageRepository;
import prm393.group8.flowermanagement.repository.ConversationRepository;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
public class ChatServiceImpl implements ChatService {

    private final ConversationRepository conversationRepository;
    private final ChatMessageRepository chatMessageRepository;

    public ChatServiceImpl(ConversationRepository conversationRepository, ChatMessageRepository chatMessageRepository) {
        this.conversationRepository = conversationRepository;
        this.chatMessageRepository = chatMessageRepository;
    }

    @Override
    public Conversation getOrCreateConversation(User customer) {
        return conversationRepository.findByCustomer_UserId(customer.getUserId())
                .orElseGet(() -> {
                    Conversation conversation = new Conversation();
                    conversation.setCustomer(customer);
                    conversation.setCreatedAt(LocalDateTime.now());
                    return conversationRepository.save(conversation);
                });
    }

    @Override
    public Conversation getConversationOrThrow(int conversationId) {
        return conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy cuộc trò chuyện"));
    }

    @Override
    public List<ChatMessageDto> getMessages(int conversationId) {
        return chatMessageRepository.findByConversation_ConversationIdOrderBySentAtAsc(conversationId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public ChatMessageDto sendMessage(int conversationId, User sender, String content) {
        Conversation conversation = getConversationOrThrow(conversationId);

        ChatMessage message = new ChatMessage();
        message.setConversation(conversation);
        message.setSender(sender);
        message.setContent(content.trim());
        message.setSentAt(LocalDateTime.now());
        message.setRead(false);

        ChatMessage saved = chatMessageRepository.save(message);
        return toDto(saved);
    }

    @Override
    @Transactional
    public void markAsRead(int conversationId, User reader) {
        chatMessageRepository.markAsReadExcludingSender(conversationId, reader.getUserId());
    }

    @Override
    public long getUnreadCountForCustomer(User customer) {
        Conversation conversation = getOrCreateConversation(customer);
        return chatMessageRepository.countByConversation_ConversationIdAndReadFalseAndSender_UserIdNot(
                conversation.getConversationId(), customer.getUserId());
    }

    @Override
    public long getUnreadCountForAdmin() {
        return chatMessageRepository.countUnreadFromCustomers();
    }

    @Override
    public List<ConversationSummaryDto> getConversationsForAdmin() {
        return conversationRepository.findAllByOrderByConversationIdDesc().stream()
                .map(conversation -> {
                    ChatMessage last = chatMessageRepository
                            .findTopByConversation_ConversationIdOrderBySentAtDesc(conversation.getConversationId())
                            .orElse(null);
                    long unread = chatMessageRepository.countByConversation_ConversationIdAndReadFalseAndSender_UserId(
                            conversation.getConversationId(), conversation.getCustomer().getUserId());

                    ConversationSummaryDto dto = new ConversationSummaryDto();
                    dto.setConversationId(conversation.getConversationId());
                    dto.setCustomerId(conversation.getCustomer().getUserId());
                    dto.setCustomerName(conversation.getCustomer().getFullName());
                    dto.setLastMessage(last != null ? last.getContent() : null);
                    dto.setLastMessageAt(last != null ? last.getSentAt() : conversation.getCreatedAt());
                    dto.setUnreadCount(unread);
                    return dto;
                })
                .sorted(Comparator.comparing(ConversationSummaryDto::getLastMessageAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    private ChatMessageDto toDto(ChatMessage message) {
        ChatMessageDto dto = new ChatMessageDto();
        dto.setMessageId(message.getMessageId());
        dto.setConversationId(message.getConversation().getConversationId());
        dto.setSenderId(message.getSender().getUserId());
        dto.setSenderName(message.getSender().getFullName());
        dto.setSenderRole(message.getSender().getRole() != null ? message.getSender().getRole().getRoleName() : null);
        dto.setContent(message.getContent());
        dto.setSentAt(message.getSentAt());
        dto.setRead(message.isRead());
        return dto;
    }
}

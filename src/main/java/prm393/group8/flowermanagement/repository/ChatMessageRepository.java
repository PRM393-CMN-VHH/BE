package prm393.group8.flowermanagement.repository;

import prm393.group8.flowermanagement.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Integer> {

    List<ChatMessage> findByConversation_ConversationIdOrderBySentAtAsc(int conversationId);

    Optional<ChatMessage> findTopByConversation_ConversationIdOrderBySentAtDesc(int conversationId);

    // Unread messages in a conversation NOT sent by the given user (their inbound unread count)
    long countByConversation_ConversationIdAndReadFalseAndSender_UserIdNot(int conversationId, int userId);

    // Unread messages in a conversation sent by the given user (used by admin to see customer-side unread)
    long countByConversation_ConversationIdAndReadFalseAndSender_UserId(int conversationId, int userId);

    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.read = false AND m.sender.userId = m.conversation.customer.userId")
    long countUnreadFromCustomers();

    @Modifying
    @Query("UPDATE ChatMessage m SET m.read = true WHERE m.conversation.conversationId = :conversationId AND m.sender.userId <> :readerId")
    int markAsReadExcludingSender(@Param("conversationId") int conversationId, @Param("readerId") int readerId);
}

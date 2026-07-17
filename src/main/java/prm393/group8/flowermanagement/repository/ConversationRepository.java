package prm393.group8.flowermanagement.repository;

import prm393.group8.flowermanagement.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Integer> {
    Optional<Conversation> findByCustomer_UserId(int customerId);

    List<Conversation> findAllByOrderByConversationIdDesc();
}

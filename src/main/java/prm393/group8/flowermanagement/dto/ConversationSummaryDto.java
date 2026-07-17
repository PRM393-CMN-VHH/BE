package prm393.group8.flowermanagement.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class ConversationSummaryDto {
    private int conversationId;
    private int customerId;
    private String customerName;
    private String lastMessage;
    private LocalDateTime lastMessageAt;
    private long unreadCount;
}

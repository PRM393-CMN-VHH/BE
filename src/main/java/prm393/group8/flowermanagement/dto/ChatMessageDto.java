package prm393.group8.flowermanagement.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class ChatMessageDto {
    private int messageId;
    private int conversationId;
    private int senderId;
    private String senderName;
    private String senderRole;
    private String content;
    private LocalDateTime sentAt;
    private boolean read;
}

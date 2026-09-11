package cn.famecode.ai.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 聊天会话响应DTO
 *
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatSessionResponseDTO {

    /** 会话ID */
    private String sessionId;

    /** 用户ID */
    private String userId;

    /** 智能体ID */
    private String agentId;

    /** 会话标题 */
    private String title;

    /** 终端会话ID */
    private String terminalSessionId;

    /** 最近一条消息摘要 */
    private String lastMessageSummary;

    /** 创建时间 */
    private String createdAt;

    /** 更新时间 */
    private String updatedAt;

    /** 会话内消息列表（查询详情时返回） */
    private List<ChatMessageResponseDTO> messages;

}
package cn.famecode.ai.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 聊天会话请求DTO
 *
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatSessionRequestDTO {

    /** 会话ID（更新/查询时必传） */
    private String sessionId;

    /** 用户ID */
    private String userId;

    /** 智能体ID */
    private String agentId;

    /** 会话标题 */
    private String title;

    /** 终端会话ID */
    private String terminalSessionId;

}
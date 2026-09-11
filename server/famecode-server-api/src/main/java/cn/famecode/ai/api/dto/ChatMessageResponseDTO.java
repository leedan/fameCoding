package cn.famecode.ai.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 聊天消息响应DTO
 *
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessageResponseDTO {

    /** 消息ID */
    private String messageId;

    /** 会话ID */
    private String sessionId;

    /** 消息角色: user / assistant / system */
    private String role;

    /** 消息内容 */
    private String content;

    /** 消息序号 */
    private Integer seq;

    /** 创建时间 */
    private String createdAt;

}
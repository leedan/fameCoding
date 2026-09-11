package cn.famecode.ai.api;

import cn.famecode.ai.api.dto.ChatSessionRequestDTO;
import cn.famecode.ai.api.dto.ChatSessionResponseDTO;
import cn.famecode.ai.api.response.Response;

import java.util.List;

/**
 * 聊天会话管理 API 接口
 *
 */
public interface IChatSessionService {

    /**
     * 创建会话
     */
    Response<ChatSessionResponseDTO> createSession(ChatSessionRequestDTO requestDTO);

    /**
     * 更新会话（修改标题等）
     */
    Response<ChatSessionResponseDTO> updateSession(ChatSessionRequestDTO requestDTO);

    /**
     * 删除会话
     */
    Response<Void> deleteSession(String sessionId);

    /**
     * 查询会话详情（含消息列表）
     */
    Response<ChatSessionResponseDTO> getSessionDetail(String sessionId);

    /**
     * 查询用户的会话列表
     */
    Response<List<ChatSessionResponseDTO>> getSessionList(String userId);

}
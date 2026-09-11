package cn.famecode.ai.trigger.http;

import cn.famecode.ai.api.IChatSessionService;
import cn.famecode.ai.api.dto.ChatMessageResponseDTO;
import cn.famecode.ai.api.dto.ChatSessionRequestDTO;
import cn.famecode.ai.api.dto.ChatSessionResponseDTO;
import cn.famecode.ai.api.response.Response;
import cn.famecode.ai.domain.agent.adapter.repository.IChatHistoryRepository;
import cn.famecode.ai.domain.agent.model.entity.ChatMessageEntity;
import cn.famecode.ai.domain.agent.model.entity.ChatSessionEntity;
import cn.famecode.ai.domain.agent.service.IChatService;
import cn.famecode.ai.types.enums.ResponseCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 聊天会话管理 HTTP 控制器
 * <p>
 * 提供会话的 CRUD、会话列表查询、会话详情（含消息历史）等 RESTful API
 *
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/chat/session")
@CrossOrigin(origins = "*")
public class ChatSessionController implements IChatSessionService {

    private static final SimpleDateFormat FMT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Resource
    private IChatService chatService;

    @Resource
    private IChatHistoryRepository chatHistoryRepository;

    // ═══════════════════════════════════════════════════════════════
    //  会话 CRUD
    // ═══════════════════════════════════════════════════════════════

    /**
     * 创建会话
     * <p>POST /api/v1/chat/session/create
     */
    @RequestMapping(value = "create", method = RequestMethod.POST)
    @Override
    public Response<ChatSessionResponseDTO> createSession(@RequestBody ChatSessionRequestDTO requestDTO) {
        try {
            log.info("创建聊天会话 agentId={} userId={}", requestDTO.getAgentId(), requestDTO.getUserId());

            if (requestDTO.getAgentId() == null || requestDTO.getAgentId().isEmpty()) {
                return Response.<ChatSessionResponseDTO>builder()
                        .code(ResponseCode.ILLEGAL_PARAMETER.getCode())
                        .info("agentId 不能为空")
                        .build();
            }
            if (requestDTO.getUserId() == null || requestDTO.getUserId().isEmpty()) {
                return Response.<ChatSessionResponseDTO>builder()
                        .code(ResponseCode.ILLEGAL_PARAMETER.getCode())
                        .info("userId 不能为空")
                        .build();
            }

            String sessionId = chatService.createSession(requestDTO.getAgentId(), requestDTO.getUserId());

            // 如果指定了标题，更新会话标题
            if (requestDTO.getTitle() != null && !requestDTO.getTitle().isEmpty()) {
                ChatSessionEntity entity = chatHistoryRepository.getSession(sessionId);
                if (entity != null) {
                    entity.setTitle(requestDTO.getTitle());
                    chatHistoryRepository.saveSession(entity);
                }
            }

            ChatSessionEntity entity = chatHistoryRepository.getSession(sessionId);
            ChatSessionResponseDTO responseDTO = toResponseDTO(entity);

            return Response.<ChatSessionResponseDTO>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(responseDTO)
                    .build();
        } catch (Exception e) {
            log.error("创建聊天会话失败 agentId={} userId={}", requestDTO.getAgentId(), requestDTO.getUserId(), e);
            return Response.<ChatSessionResponseDTO>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info("创建会话失败: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 更新会话（修改标题等）
     * <p>POST /api/v1/chat/session/update
     */
    @RequestMapping(value = "update", method = RequestMethod.POST)
    @Override
    public Response<ChatSessionResponseDTO> updateSession(@RequestBody ChatSessionRequestDTO requestDTO) {
        try {
            log.info("更新聊天会话 sessionId={}", requestDTO.getSessionId());

            if (requestDTO.getSessionId() == null || requestDTO.getSessionId().isEmpty()) {
                return Response.<ChatSessionResponseDTO>builder()
                        .code(ResponseCode.ILLEGAL_PARAMETER.getCode())
                        .info("sessionId 不能为空")
                        .build();
            }

            ChatSessionEntity entity = chatHistoryRepository.getSession(requestDTO.getSessionId());
            if (entity == null) {
                return Response.<ChatSessionResponseDTO>builder()
                        .code(ResponseCode.ILLEGAL_PARAMETER.getCode())
                        .info("会话不存在")
                        .build();
            }

            // 更新可修改字段
            if (requestDTO.getTitle() != null && !requestDTO.getTitle().isEmpty()) {
                entity.setTitle(requestDTO.getTitle());
            }
            if (requestDTO.getAgentId() != null && !requestDTO.getAgentId().isEmpty()) {
                entity.setAgentId(requestDTO.getAgentId());
            }

            chatHistoryRepository.saveSession(entity);

            ChatSessionResponseDTO responseDTO = toResponseDTO(entity);

            return Response.<ChatSessionResponseDTO>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(responseDTO)
                    .build();
        } catch (Exception e) {
            log.error("更新聊天会话失败 sessionId={}", requestDTO.getSessionId(), e);
            return Response.<ChatSessionResponseDTO>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info("更新会话失败: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 删除会话
     * <p>POST /api/v1/chat/session/delete?sessionId=xxx
     */
    @RequestMapping(value = "delete", method = RequestMethod.POST)
    @Override
    public Response<Void> deleteSession(@RequestParam("sessionId") String sessionId) {
        try {
            log.info("删除聊天会话 sessionId={}", sessionId);

            if (sessionId == null || sessionId.isEmpty()) {
                return Response.<Void>builder()
                        .code(ResponseCode.ILLEGAL_PARAMETER.getCode())
                        .info("sessionId 不能为空")
                        .build();
            }

            // 目前仓储接口没有删除方法，先查询确认存在
            ChatSessionEntity entity = chatHistoryRepository.getSession(sessionId);
            if (entity == null) {
                return Response.<Void>builder()
                        .code(ResponseCode.ILLEGAL_PARAMETER.getCode())
                        .info("会话不存在")
                        .build();
            }

            // TODO: 待 IChatHistoryRepository 增加删除方法后补充
            // chatHistoryRepository.deleteSession(sessionId);

            log.warn("删除会话功能待仓储层支持，sessionId={}", sessionId);

            return Response.<Void>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info("会话删除请求已接收（待仓储层支持）")
                    .build();
        } catch (Exception e) {
            log.error("删除聊天会话失败 sessionId={}", sessionId, e);
            return Response.<Void>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info("删除会话失败: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 查询会话详情（含消息历史）
     * <p>GET /api/v1/chat/session/detail?sessionId=xxx
     */
    @RequestMapping(value = "detail", method = RequestMethod.GET)
    @Override
    public Response<ChatSessionResponseDTO> getSessionDetail(@RequestParam("sessionId") String sessionId) {
        try {
            log.info("查询会话详情 sessionId={}", sessionId);

            if (sessionId == null || sessionId.isEmpty()) {
                return Response.<ChatSessionResponseDTO>builder()
                        .code(ResponseCode.ILLEGAL_PARAMETER.getCode())
                        .info("sessionId 不能为空")
                        .build();
            }

            ChatSessionEntity entity = chatHistoryRepository.getSession(sessionId);
            if (entity == null) {
                return Response.<ChatSessionResponseDTO>builder()
                        .code(ResponseCode.ILLEGAL_PARAMETER.getCode())
                        .info("会话不存在")
                        .build();
            }

            ChatSessionResponseDTO responseDTO = toResponseDTO(entity);

            // 查询最近的消息列表
            List<ChatMessageEntity> messages = chatHistoryRepository.getRecentMessages(sessionId, 100);
            if (messages != null && !messages.isEmpty()) {
                List<ChatMessageResponseDTO> messageDTOs = messages.stream()
                        .map(this::toMessageDTO)
                        .collect(Collectors.toList());
                responseDTO.setMessages(messageDTOs);

                // 设置最后一条消息摘要
                ChatMessageEntity lastMsg = messages.get(messages.size() - 1);
                String content = lastMsg.getContent();
                if (content != null && content.length() > 50) {
                    responseDTO.setLastMessageSummary(content.substring(0, 50) + "...");
                } else {
                    responseDTO.setLastMessageSummary(content);
                }
            } else {
                responseDTO.setMessages(Collections.emptyList());
            }

            return Response.<ChatSessionResponseDTO>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(responseDTO)
                    .build();
        } catch (Exception e) {
            log.error("查询会话详情失败 sessionId={}", sessionId, e);
            return Response.<ChatSessionResponseDTO>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info("查询会话详情失败: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 查询用户的会话列表
     * <p>GET /api/v1/chat/session/list?userId=xxx
     */
    @RequestMapping(value = "list", method = RequestMethod.GET)
    @Override
    public Response<List<ChatSessionResponseDTO>> getSessionList(@RequestParam(value = "userId", defaultValue = "default") String userId) {
        try {
            log.info("查询用户会话列表 userId={}", userId);

            // TODO: IChatHistoryRepository 目前没有按 userId 查询列表的方法
            // 待仓储层增加 queryByUserId 后补充完整实现
            // 目前返回空列表，接口已就绪

            return Response.<List<ChatSessionResponseDTO>>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(Collections.emptyList())
                    .build();
        } catch (Exception e) {
            log.error("查询用户会话列表失败 userId={}", userId, e);
            return Response.<List<ChatSessionResponseDTO>>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info("查询会话列表失败: " + e.getMessage())
                    .build();
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  DTO 转换
    // ═══════════════════════════════════════════════════════════════

    private ChatSessionResponseDTO toResponseDTO(ChatSessionEntity entity) {
        if (entity == null) {
            return null;
        }
        return ChatSessionResponseDTO.builder()
                .sessionId(entity.getId())
                .userId(entity.getUserId())
                .agentId(entity.getAgentId())
                .title(entity.getTitle())
                .createdAt(entity.getCreatedAt() != null ? FMT.format(entity.getCreatedAt()) : null)
                .updatedAt(entity.getUpdatedAt() != null ? FMT.format(entity.getUpdatedAt()) : null)
                .build();
    }

    private ChatMessageResponseDTO toMessageDTO(ChatMessageEntity entity) {
        return ChatMessageResponseDTO.builder()
                .messageId(entity.getId() != null ? String.valueOf(entity.getId()) : null)
                .sessionId(entity.getSessionId())
                .role(entity.getRole())
                .content(entity.getContent())
                .createdAt(entity.getCreatedAt() != null ? FMT.format(entity.getCreatedAt()) : null)
                .build();
    }

}
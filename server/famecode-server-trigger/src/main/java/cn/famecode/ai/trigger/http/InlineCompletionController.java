package cn.famecode.ai.trigger.http;

import cn.famecode.ai.api.IInlineCompletionService;
import cn.famecode.ai.api.dto.InlineCompletionRequestDTO;
import cn.famecode.ai.api.dto.InlineCompletionResponseDTO;
import cn.famecode.ai.api.response.Response;
import cn.famecode.ai.domain.agent.model.valobj.completion.InlineCompletionCommandVO;
import cn.famecode.ai.domain.agent.model.valobj.completion.InlineCompletionResultVO;
import cn.famecode.ai.domain.agent.service.completion.IInlineCompletionDomainService;
import cn.famecode.ai.types.enums.ResponseCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;

/**
 * 行内代码极速补全 HTTP 控制器（模式三 通道 A 端点）
 *
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/completion")
@CrossOrigin(origins = "*")
public class InlineCompletionController implements IInlineCompletionService {

    @Resource
    private IInlineCompletionDomainService inlineCompletionDomainService;

    /**
     * 极速单次代码补全预测
     * <p>POST /api/v1/completion/inline
     */
    @PostMapping(value = "inline")
    @Override
    public Response<InlineCompletionResponseDTO> complete(@RequestBody InlineCompletionRequestDTO requestDTO) {
        try {
            InlineCompletionCommandVO commandVO = InlineCompletionCommandVO.builder()
                    .path(requestDTO.getPath())
                    .language(requestDTO.getLanguage())
                    .prefix(requestDTO.getPrefix())
                    .suffix(requestDTO.getSuffix())
                    .line(requestDTO.getLine())
                    .column(requestDTO.getColumn())
                    .build();

            InlineCompletionResultVO resultVO = inlineCompletionDomainService.complete(commandVO);

            InlineCompletionResponseDTO responseDTO = InlineCompletionResponseDTO.builder()
                    .completion(resultVO.getCompletion())
                    .model(resultVO.getModel())
                    .durationMs(resultVO.getDurationMs())
                    .hasCompletion(resultVO.isHasCompletion())
                    .build();

            return Response.<InlineCompletionResponseDTO>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(responseDTO)
                    .build();
        } catch (Exception e) {
            log.error("[InlineCompletion] 补全接口执行异常", e);
            return Response.<InlineCompletionResponseDTO>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(ResponseCode.UN_ERROR.getInfo())
                    .data(InlineCompletionResponseDTO.builder().hasCompletion(false).build())
                    .build();
        }
    }

}
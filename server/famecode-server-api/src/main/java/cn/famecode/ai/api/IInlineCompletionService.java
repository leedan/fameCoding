package cn.famecode.ai.api;

import cn.famecode.ai.api.dto.InlineCompletionRequestDTO;
import cn.famecode.ai.api.dto.InlineCompletionResponseDTO;
import cn.famecode.ai.api.response.Response;

/**
 * 行内代码预测补全接口服务
 *
 */
public interface IInlineCompletionService {

    /**
     * 极速单次代码补全预测
     *
     * @param requestDTO 补全上下文请求
     * @return 补全结果
     */
    Response<InlineCompletionResponseDTO> complete(InlineCompletionRequestDTO requestDTO);

}
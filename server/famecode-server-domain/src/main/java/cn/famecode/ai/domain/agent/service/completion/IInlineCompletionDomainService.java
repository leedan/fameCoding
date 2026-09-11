package cn.famecode.ai.domain.agent.service.completion;

import cn.famecode.ai.domain.agent.model.valobj.completion.InlineCompletionCommandVO;
import cn.famecode.ai.domain.agent.model.valobj.completion.InlineCompletionResultVO;

/**
 * 行内代码极速补全领域服务接口
 *
 */
public interface IInlineCompletionDomainService {

    /**
     * 执行极速行内代码补全
     *
     * @param commandVO 补全上下文命令对象
     * @return 补全响应结果值对象
     */
    InlineCompletionResultVO complete(InlineCompletionCommandVO commandVO);

}
package cn.famecode.ai.domain.agent.service.intent;

import cn.famecode.ai.domain.agent.model.valobj.intent.ConversationContextVO;
import cn.famecode.ai.domain.agent.model.valobj.intent.IntentResultVO;

public interface IIntentClassifier {
    IntentResultVO classify(String message, ConversationContextVO context);
}
package cn.famecode.ai.domain.agent.service;

import cn.famecode.ai.domain.agent.model.valobj.intent.IntentResultVO;

public interface IIntentService {
    IntentResultVO classify(String sessionId, String userId, String message);
}
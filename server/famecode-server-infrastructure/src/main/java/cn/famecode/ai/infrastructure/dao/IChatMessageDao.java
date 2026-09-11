package cn.famecode.ai.infrastructure.dao;

import cn.famecode.ai.infrastructure.dao.po.ChatMessagePO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface IChatMessageDao {
    void insert(ChatMessagePO po);
    List<ChatMessagePO> queryRecentBySessionId(@Param("sessionId") String sessionId, @Param("limit") int limit);
}
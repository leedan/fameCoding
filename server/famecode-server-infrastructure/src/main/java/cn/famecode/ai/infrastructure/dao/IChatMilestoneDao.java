package cn.famecode.ai.infrastructure.dao;

import cn.famecode.ai.infrastructure.dao.po.ChatMilestonePO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface IChatMilestoneDao {
    void insert(ChatMilestonePO po);
    List<ChatMilestonePO> queryRecentBySessionId(@Param("sessionId") String sessionId, @Param("limit") int limit);
}
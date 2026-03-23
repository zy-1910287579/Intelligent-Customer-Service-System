// package com.storm.repository 或 service 包下
package com.storm.repository;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Repository
@RequiredArgsConstructor
public class MyChatHistoryRepository {

    private final JdbcTemplate jdbcTemplate;

    public List<RawMessage> findMessagesByConversationId(String conversationId) {
        log.info("当前用户ID为:{}",conversationId);
        String sql = """
            SELECT type, content, timestamp
            FROM spring_ai_chat_memory
            WHERE conversation_id = ?
            ORDER BY timestamp ASC
            """;

                                        /*这里用lambda表达式将每一行封装为一个RawMessage对象,
                                        遍历玩行之后,会将所有的对象再封装为一个list集合返回*/
        return jdbcTemplate.query(sql, (rs, rowNum) -> {return new RawMessage(
                        rs.getString("type"),
                        rs.getString("content"),
                        rs.getTimestamp("timestamp").toLocalDateTime());}
                ,conversationId);
    }

    // 内部辅助类，表示单条原始消息
    @AllArgsConstructor
    //data默认不生成有参构造了
    @Data
    public static class RawMessage{
        private String type;
        private String content;
        private LocalDateTime timestamp;
    }
}
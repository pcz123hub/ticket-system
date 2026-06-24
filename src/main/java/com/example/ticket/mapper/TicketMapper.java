package com.example.ticket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.ticket.domain.entity.Ticket;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface TicketMapper extends BaseMapper<Ticket> {

    @Select("SELECT status, COUNT(*) as count FROM ticket WHERE created_at >= #{since} GROUP BY status")
    List<Map<String, Object>> countByStatusSince(@Param("since") String since);

    @Select("SELECT assignee_id, COUNT(*) as count FROM ticket WHERE assignee_id IS NOT NULL AND status IN (0,1) GROUP BY assignee_id")
    List<Map<String, Object>> countProcessingByAgent();

    @Select("SELECT AVG(TIMESTAMPDIFF(MINUTE, created_at, closed_at)) as avg_minutes FROM ticket WHERE closed_at IS NOT NULL AND closed_at >= #{since}")
    Long avgProcessMinutes(@Param("since") String since);
}

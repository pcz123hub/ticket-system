package com.example.ticket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.ticket.domain.entity.TicketStats;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TicketStatsMapper extends BaseMapper<TicketStats> {
}

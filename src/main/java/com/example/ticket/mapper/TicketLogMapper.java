package com.example.ticket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.ticket.domain.entity.TicketLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TicketLogMapper extends BaseMapper<TicketLog> {
}

package com.example.ticket.dto.response;

import java.util.List;
import java.util.Map;

public class StatsVO {
    private Map<String, Long> statusDistribution;
    private List<Map<String, Object>> agentWorkload;
    private Long avgProcessMinutes;
    private Double slaPassRate;
    private Double satisfactionAvg;

    public Map<String, Long> getStatusDistribution() { return statusDistribution; }
    public void setStatusDistribution(Map<String, Long> statusDistribution) { this.statusDistribution = statusDistribution; }
    public List<Map<String, Object>> getAgentWorkload() { return agentWorkload; }
    public void setAgentWorkload(List<Map<String, Object>> agentWorkload) { this.agentWorkload = agentWorkload; }
    public Long getAvgProcessMinutes() { return avgProcessMinutes; }
    public void setAvgProcessMinutes(Long avgProcessMinutes) { this.avgProcessMinutes = avgProcessMinutes; }
    public Double getSlaPassRate() { return slaPassRate; }
    public void setSlaPassRate(Double slaPassRate) { this.slaPassRate = slaPassRate; }
    public Double getSatisfactionAvg() { return satisfactionAvg; }
    public void setSatisfactionAvg(Double satisfactionAvg) { this.satisfactionAvg = satisfactionAvg; }
}

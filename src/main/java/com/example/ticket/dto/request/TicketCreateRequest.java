package com.example.ticket.dto.request;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public class TicketCreateRequest {
    @NotBlank(message = "标题不能为空")
    private String title;

    private String description;

    @NotBlank(message = "业务类型不能为空")
    private String category;

    @NotNull(message = "优先级不能为空")
    private Integer priority;

    @NotNull(message = "客户ID不能为空")
    private Long customerId;

    @NotBlank(message = "来源不能为空")
    private String source;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
}

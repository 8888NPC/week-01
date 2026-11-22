package com.example.week01.task;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TaskRequest(
        @NotBlank(message = "请输入任务名称") @Size(max = 80, message = "任务名称最多 80 字") String title,
        @Size(max = 500, message = "任务描述最多 500 字") String description,
        @NotNull(message = "请选择任务状态") TaskStatus status
) {}

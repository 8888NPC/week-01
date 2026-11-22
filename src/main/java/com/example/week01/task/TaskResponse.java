package com.example.week01.task;

import java.time.Instant;

public record TaskResponse(
        long id,
        String title,
        String description,
        TaskStatus status,
        Instant createdAt,
        Instant updatedAt
) {}

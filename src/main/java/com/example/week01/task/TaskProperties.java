package com.example.week01.task;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.tasks")
public record TaskProperties(boolean seedDemo) {}

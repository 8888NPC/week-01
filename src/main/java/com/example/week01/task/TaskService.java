package com.example.week01.task;

import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

@Service
public class TaskService {
    private final Map<Long, Task> tasks = new HashMap<>();
    private long nextId = 1;

    public TaskService(TaskProperties properties) {
        if (properties.seedDemo()) {
            add(new TaskRequest("创建 Spring Boot 项目", "配置 Maven 依赖与健康检查接口。", TaskStatus.DONE));
            add(new TaskRequest("配置 application.yml", "验证端口配置与演示数据开关。", TaskStatus.DONE));
            add(new TaskRequest("整理 REST 接口示例", "记录请求体、状态码与可运行命令。", TaskStatus.DONE));
            add(new TaskRequest("联调任务管理界面", "走通新增、详情、状态更新和删除流程。", TaskStatus.IN_PROGRESS));
            add(new TaskRequest("准备周末验收", "运行 Maven 打包并核对 Git 标签。", TaskStatus.TODO));
        }
    }

    public synchronized List<TaskResponse> list() {
        return tasks.values().stream()
                .sorted(Comparator.comparingLong(Task::id).reversed())
                .map(Task::response)
                .toList();
    }

    public synchronized TaskResponse get(long id) {
        return requireTask(id).response();
    }

    public synchronized TaskResponse create(TaskRequest request) {
        return add(request).response();
    }

    public synchronized TaskResponse update(long id, TaskRequest request) {
        Task original = requireTask(id);
        Task updated = taskFrom(id, request, original.createdAt(), Instant.now());
        tasks.put(id, updated);
        return updated.response();
    }

    public synchronized void delete(long id) {
        if (tasks.remove(id) == null) {
            throw new TaskNotFoundException(id);
        }
    }

    private Task add(TaskRequest request) {
        Instant now = Instant.now();
        Task task = taskFrom(nextId++, request, now, now);
        tasks.put(task.id(), task);
        return task;
    }

    private Task requireTask(long id) {
        Task task = tasks.get(id);
        if (task == null) {
            throw new TaskNotFoundException(id);
        }
        return task;
    }

    private static Task taskFrom(long id, TaskRequest request, Instant createdAt, Instant updatedAt) {
        String description = request.description() == null ? "" : request.description().strip();
        return new Task(id, request.title().strip(), description, request.status(), createdAt, updatedAt);
    }

    private record Task(long id, String title, String description, TaskStatus status,
                        Instant createdAt, Instant updatedAt) {
        TaskResponse response() {
            return new TaskResponse(id, title, description, status, createdAt, updatedAt);
        }
    }
}

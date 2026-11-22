package com.example.week01.task;

class TaskNotFoundException extends RuntimeException {
    TaskNotFoundException(long id) {
        super("任务 #" + id + " 不存在");
    }
}

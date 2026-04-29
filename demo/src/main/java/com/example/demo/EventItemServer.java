package com.example.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class EventItemServer { // 文件名和类名一致
    @Autowired
    private TaskRepository taskRepository;

    public Result<EventItem> addTask(String username, EventItem task) {
        Long id = taskRepository.insertTask(username, task);
        task.setId(id);
        return Result.success(task);
    }

    public Result<List<EventItem>> getTasks(String username) {
        return Result.success(taskRepository.findAllTasks(username));
    }

    public Result<EventItem> updateTask(String username, Long id, EventItem task) {
        taskRepository.updateTask(username, id, task);
        task.setId(id);
        return Result.success(task);
    }

    public Result<Void> deleteTask(String username, Long id) {
        taskRepository.deleteTask(username, id);
        return Result.success(null);
    }
}
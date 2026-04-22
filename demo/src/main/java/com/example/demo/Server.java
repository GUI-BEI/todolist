package com.example.demo;

import org.springframework.stereotype.Service;

import java.util.List;
// 业务逻辑层（Service），负责处理任务相关操作的具体实现
//所有数据都保存在data文件中，将其清空即可清除所有数据
@Service
public class Server {

    private final EventItemRepository repository;

    public Server(EventItemRepository repository) {
        this.repository = repository;
    }

    // 添加任务（持久化到数据库）
    public Result<EventItem> addTask(EventItem task) {
        // 校验标题
        if (task.getTitle() == null || task.getTitle().trim().isEmpty()) {
            return Result.fail(400, "任务标题不能为空");
        }
        // 忽略前端发送的 id，让数据库生成
        task.setId(null);
        // 持久化到数据库，返回带ID的任务
        EventItem savedTask = repository.save(task);
        System.out.println("保存到数据库的任务: " + task);
        return Result.success(savedTask);
    }

    // 获取所有任务（从数据库查询）
    public Result<List<EventItem>> getTasks() {
        List<EventItem> tasks = repository.findAll();
        System.out.println("从数据库查询到的任务列表: " + tasks);
        return Result.success(tasks);
    }

    // 修改任务
    public Result<EventItem> updateTask(Long id, EventItem updateTask) {
        return repository.findById(id)
                .map(task -> {
                    task.setTitle(updateTask.getTitle());
                    task.setDescription(updateTask.getDescription());
                    task.setPriority(updateTask.getPriority());
                    task.setStart(updateTask.getStart());
                    task.setEnd(updateTask.getEnd());
                    task.setType(updateTask.getType());
                    task.setCompleted(updateTask.getCompleted());
                    EventItem saved = repository.save(task);
                    return Result.success(saved);
                })
                .orElse(Result.fail(404, "任务不存在"));
    }

    // 删除任务
    public Result<Void> deleteTask(Long id) {
        if (!repository.existsById(id)) {
            return Result.fail(404, "任务不存在");
        }
        repository.deleteById(id);
        return Result.success(null);
    }
}
package veronfc.task_manager_api;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;

@Component
class Validator {
    private final TaskRepository db;

    Validator(TaskRepository db) {
        this.db = db;
    }

    public UUID checkIdValidity(String strId) {
        if (strId == null || strId.isBlank()) {
            throw new IllegalArgumentException("Task ID must not be empty");
        }

        try {
            return UUID.fromString(strId);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Task ID must be a UUID");
        }
    }

    public void checkTitleValidity(Task task) {
        Task foundTask = db.findByTitle(task.getTitle());

        if (foundTask != null && foundTask.getId() != task.getId()) {
            throw new IllegalArgumentException("Task title must be unique");
        }
    }

    public void checkDueDateValidity(LocalDateTime dueDate) {
        if (TimeUnit.HOURS.convert(Duration.between(LocalDateTime.now(), dueDate).getSeconds(), TimeUnit.SECONDS) < 12) {
            throw new IllegalArgumentException("Task due date must be at least 12 hours in the future");
        }
    }
}

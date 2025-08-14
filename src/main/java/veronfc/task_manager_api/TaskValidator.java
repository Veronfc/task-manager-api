package veronfc.task_manager_api;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;

import jakarta.validation.ValidationException;

@Component
class TaskValidator {
    private final TaskRepository repository;

    TaskValidator(TaskRepository repository) {
        this.repository = repository;
    }

    public UUID checkIdValidity(String strId) {
        if (strId == null || strId.isBlank()) {
            throw new ValidationException("Task ID must not be empty");
        }

        try {
            return UUID.fromString(strId);
        } catch (IllegalArgumentException ex) {
            throw new ValidationException("Task ID must be a UUID");
        }
    }

    public void checkTitleValidity(Task task) {
        Task foundTask;

        foundTask = repository.findByTitle(task.getTitle()).orElse(null);

        if (foundTask != null) {
            if (task.getId() == null || !foundTask.getId().equals(task.getId())) {
                throw new ValidationException("Task title must be unique");
            }
        }
    }

    public void checkDueDateValidity(LocalDateTime dueDate) {
        if (TimeUnit.HOURS.convert(Duration.between(LocalDateTime.now(), dueDate).getSeconds(), TimeUnit.SECONDS) < 12) {
            throw new ValidationException("Task due date must be at least 12 hours in the future");
        }
    }
}

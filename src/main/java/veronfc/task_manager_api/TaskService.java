package veronfc.task_manager_api;

import java.util.UUID;

import org.springframework.stereotype.Service;

interface ITaskService {
    public Task createTask(Task task);
    public Task retrieveTask(String id);
    public Task updateTask(Task task);
    public void deleteTask(String id);
}

@Service
class TaskService implements ITaskService {
    private final TaskRepository repository;
    private final TaskValidator validator;

    TaskService(TaskRepository repository, TaskValidator validator) {
        this.repository = repository;
        this.validator = validator;
    }

    public Task createTask(Task task) {
        validator.checkTitleValidity(task);
        validator.checkDueDateValidity(task.getDueDate());

        return repository.save(task);
    }

    public Task retrieveTask(String strId) {
        UUID id = validator.checkIdValidity(strId);

        return repository.findById(id).orElseThrow(() -> new TaskNotFoundException(id));
    }

    public Task updateTask(Task updatedtask) {
        validator.checkTitleValidity(updatedtask);
        
        UUID id = updatedtask.getId();

        Task task = repository.findById(id).orElseThrow(() -> new TaskNotFoundException(id));

        if (task.getStatus() == TaskStatus.COMPLETE) {
            throw new TaskStatusException(String.format("Task with ID: %s is marked as 'Complete' and can not be updated further", id.toString()));
        }
        // validator.checkDueDateValidity(updatedtask.getDueDate());

        return repository.save(updatedtask);
    }

    public void deleteTask(String strId) {
        UUID id = validator.checkIdValidity(strId);

        Task task = repository.findById(id).orElseThrow(() -> new TaskNotFoundException(id));

        if (task.getStatus() == TaskStatus.ARCHIVED) {
            throw new TaskStatusException(String.format("Task with ID: %s is marked as 'Archived' and can not be deleted", strId));
        }

        repository.deleteById(id);
    }
}

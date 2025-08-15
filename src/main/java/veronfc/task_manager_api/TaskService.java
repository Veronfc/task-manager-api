package veronfc.task_manager_api;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

interface ITaskService {
    public List<Task> retrieveAllTasks();

    public Task createTask(CreateTaskDto task);

    public Task retrieveTask(String id);

    public Task updateTask(UpdateTaskDto task);

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

    public List<Task> retrieveAllTasks() {
        return repository.findAll();
    }

    public Task createTask(CreateTaskDto task) {
        validator.checkTitleValidity(task.getTitle(), null);
        validator.checkDueDateValidity(task.getDueDate());

        Task createdTask = new Task();
        createdTask.setTitle(task.getTitle());
        createdTask.setDescription(task.getDescription());
        createdTask.setDueDate(task.getDueDate());

        return repository.save(createdTask);
    }

    public Task retrieveTask(String strId) {
        UUID id = validator.checkIdValidity(strId);

        return repository.findById(id).orElseThrow(() -> new TaskNotFoundException(id));
    }

    public Task updateTask(UpdateTaskDto updatedtask) {
        validator.checkIdValidity(updatedtask.getId().toString());

        if (updatedtask.getTitle() != null) {
            validator.checkTitleValidity(updatedtask.getTitle(), updatedtask.getId().toString());
        }

        UUID id = updatedtask.getId();

        Task task = repository.findById(id).orElseThrow(() -> new TaskNotFoundException(id));

        if (task.getStatus() == TaskStatus.COMPLETE) {
            throw new TaskStatusException(String
                    .format("Task with ID: %s is marked as 'Complete' and can not be updated further", id.toString()));
        }
        // validator.checkDueDateValidity(updatedtask.getDueDate());

        task.setTitle(Optional
            .ofNullable(updatedtask.getTitle())
            .orElse(task.getTitle()));
        task.setDescription(Optional
            .ofNullable(updatedtask.getDescription())
            .orElse(task.getDescription()));
        task.setStatus(Optional
            .ofNullable(updatedtask.getStatus())
            .orElse(task.getStatus()));
        task.setDueDate(Optional
            .ofNullable(updatedtask.getDueDate())
            .orElse(task.getDueDate()));

        return repository.save(task);
    }

    public void deleteTask(String strId) {
        UUID id = validator.checkIdValidity(strId);

        Task task = repository.findById(id).orElseThrow(() -> new TaskNotFoundException(id));

        if (task.getStatus() == TaskStatus.ARCHIVED) {
            throw new TaskStatusException(
                    String.format("Task with ID: %s is marked as 'Archived' and can not be deleted", strId));
        }

        repository.deleteById(id);
    }
}

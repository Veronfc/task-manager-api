package veronfc.task_manager_api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.validation.ValidationException;

@ExtendWith(MockitoExtension.class)
class TaskServiceUnitTests {
    @Mock
    private TaskRepository repository;

    @Mock
    private TaskValidator validator;

    @InjectMocks
    private TaskService service;

    @Test
    void createTask_persistsTask_whenTitleIsUniqueAndDueDateIsAtleast12HoursInFuture() {
        String title = "This is a unique title";
        LocalDateTime dueDate = LocalDateTime.now();

        Task createdTask = new Task();
        createdTask.setTitle(title);
        createdTask.setDueDate(dueDate);

        CreateTaskDto task = new CreateTaskDto();
        task.setTitle(title);
        task.setDueDate(dueDate);

        doNothing().when(validator).checkTitleValidity(title, null);
        doNothing().when(validator).checkDueDateValidity(dueDate);
        when(repository.save(createdTask)).thenReturn(createdTask);

        Task result = service.createTask(task);
        assertEquals(createdTask, result);

        verify(repository).save(createdTask);
    }

    @Test
    void createTask_throwsException_whenTitleIsNotUnique() {
        String title = "This title is not unique";

        CreateTaskDto task = new CreateTaskDto();
        task.setTitle(title);

        doThrow(new ValidationException("Task title must be unique")).when(validator).checkTitleValidity(title, null);

        assertThrows(ValidationException.class, () -> {
            service.createTask(task);
        });

        verify(repository, never()).save(any());
    }

    @Test
    void createTask_throwsException_whenDueDateIsNot12HoursInFuture() {
        LocalDateTime dueDate = LocalDateTime.now();

        CreateTaskDto task = new CreateTaskDto();
        task.setDueDate(dueDate);

        doThrow(new ValidationException("Task due date must be at least 12 hours in the future")).when(validator)
                .checkDueDateValidity(dueDate);

        assertThrows(ValidationException.class, () -> {
            service.createTask(task);
        });

        verify(repository, never()).save(any());
    }

    @Test
    void retrieveTask_returnsTask_whenTaskExists() {
        String strId = "fe536052-58dc-40f7-9efa-3a88b1eb82da";
        UUID id = UUID.fromString(strId);

        Task task = new Task();

        when(validator.checkIdValidity(strId)).thenReturn(id);
        when(repository.findById(id)).thenReturn(Optional.of(task));

        Task result = service.retrieveTask(strId);
        assertEquals(task, result);

        verify(repository).findById(id);
    }

    @Test
    void retrieveTask_throwsException_whenStringIdIsInvalid() {
        String strId = "5e33ee4b+92f3+405a+a0db+01e18141b574";

        doThrow(new ValidationException("Task ID must be a UUID")).when(validator).checkIdValidity(strId);

        assertThrows(ValidationException.class, () -> {
            service.retrieveTask(strId);
        });

        verify(repository, never()).findById(any());
    }

    @Test
    void retrieveTask_throwsException_whenTaskIsNotFound() {
        String strId = "aa07cf6a-127a-43a8-bb54-4d45b76e6e73";
        UUID id = UUID.fromString(strId);

        when(validator.checkIdValidity(strId)).thenReturn(id);
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThrows(TaskNotFoundException.class, () -> {
            service.retrieveTask(strId);
        });

        verify(repository).findById(id);
    }

    @Test
    void updateTask_persistsUpdatedTask_whenTitleIsUniqueAndStatusIsNotComplete() {
        UUID id = UUID.randomUUID();
        String title = "This is a unique title";

        UpdateTaskDto updatedTask = new UpdateTaskDto();
        updatedTask.setId(id);
        updatedTask.setTitle(title);

        Task task = new Task();
        task.setId(id);
        task.setTitle("This is a title");
        task.setStatus(TaskStatus.IN_PROGRESS);

        doNothing().when(validator).checkTitleValidity(title, id.toString());
        when(repository.findById(id)).thenReturn(Optional.of(task));

        task.setTitle(title);

        when(repository.save(task)).thenReturn(task);

        Task result = service.updateTask(updatedTask);
        assertEquals(task, result);

        verify(repository).save(task);
    }

    @Test
    void updateTask_throwsException_whenTitleIsNotUnique() {
        UUID id = UUID.randomUUID();
        String title = "This is not a unique title";

        UpdateTaskDto updatedTask = new UpdateTaskDto();
        updatedTask.setId(id);
        updatedTask.setTitle(title);

        doThrow(new ValidationException("Task title must be unique")).when(validator).checkTitleValidity(title, id.toString());

        assertThrows(ValidationException.class, () -> {
            service.updateTask(updatedTask);
        });

        verify(repository, never()).save(any());
    }

    @Test
    void updateTask_throwsException_whenStatusIsComplete() {
        UUID id = UUID.randomUUID();

        UpdateTaskDto updatedTask = new UpdateTaskDto();
        updatedTask.setId(id);

        Task task = new Task();
        task.setStatus(TaskStatus.COMPLETE);

        when(repository.findById(id)).thenReturn(Optional.of(task));

        assertThrows(TaskStatusException.class, () -> {
            service.updateTask(updatedTask);
        });

        verify(repository, never()).save(any());
    }

    @Test
    void updateTask_throwsException_whenTaskIsNotFound() {
        UUID id = UUID.randomUUID();

        UpdateTaskDto updatedTask = new UpdateTaskDto();
        updatedTask.setId(id);

        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThrows(TaskNotFoundException.class, () -> {
            service.updateTask(updatedTask);
        });

        verify(repository, never()).save(any());
    }

    @Test
    void deleteTask_removesTask_whenTaskExistsAndStatusIsNotArchived() {
        String strId = "7aecd703-0d6b-4c62-92d8-7a42221d02a1";
        UUID id = UUID.fromString(strId);

        Task task = new Task();
        task.setId(id);
        task.setStatus(TaskStatus.IN_PROGRESS);

        when(validator.checkIdValidity(strId)).thenReturn(id);
        when(repository.findById(id)).thenReturn(Optional.of(task));

        service.deleteTask(strId);

        verify(repository).deleteById(id);
    }

    @Test
    void deleteTask_throwsException_whenStatusIsArchived() {
        String strId = "1cddbd37-6360-4d2a-ba6f-f67d8dc8cfc4";
        UUID id = UUID.fromString(strId);

        Task task = new Task();
        task.setId(id);
        task.setStatus(TaskStatus.ARCHIVED);

        when(validator.checkIdValidity(strId)).thenReturn(id);
        when(repository.findById(id)).thenReturn(Optional.of(task));

        assertThrows(TaskStatusException.class, () -> {
            service.deleteTask(strId);
        });

        verify(repository, never()).deleteById(any());
    }

    @Test
    void deleteTask_throwsException_whenTaskIsNotFound() {
        String strId = "84d96944-dd31-4e24-ae22-35bb5a193ede";
        UUID id = UUID.fromString(strId);

        when(validator.checkIdValidity(strId)).thenReturn(id);
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThrows(TaskNotFoundException.class, () -> {
            service.deleteTask(strId);
        });

        verify(repository, never()).deleteById(any());
    }
}

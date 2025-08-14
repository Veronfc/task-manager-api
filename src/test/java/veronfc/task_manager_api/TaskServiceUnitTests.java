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
        LocalDateTime dueDate = LocalDateTime.now();

        Task task = new Task();
        task.setDueDate(dueDate);

        doNothing().when(validator).checkTitleValidity(task);
        doNothing().when(validator).checkDueDateValidity(dueDate);
        when(repository.save(task)).thenReturn(task);

        Task result = service.createTask(task);
        assertEquals(task, result);

        verify(repository).save(task);
    }

    @Test
    void createTask_throwsException_whenTitleIsNotUnique() {
        Task task = new Task();

        doThrow(new ValidationException("Task title must be unique")).when(validator).checkTitleValidity(task);

        assertThrows(ValidationException.class, () -> {
            service.createTask(task);
        });

        verify(repository, never()).save(any());
    }

    @Test
    void createTask_throwsException_whenDueDateIsNot12HoursInFuture() {
        LocalDateTime dueDate = LocalDateTime.now();

        Task task = new Task();
        task.setDueDate(dueDate);

        doNothing().when(validator).checkTitleValidity(task);
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

        Task updatedTask = new Task();
        updatedTask.setId(id);

        Task task = new Task();
        task.setStatus(TaskStatus.IN_PROGRESS);

        doNothing().when(validator).checkTitleValidity(updatedTask);
        when(repository.findById(id)).thenReturn(Optional.of(task));
        when(repository.save(updatedTask)).thenReturn(updatedTask);

        Task result = service.updateTask(updatedTask);
        assertEquals(updatedTask, result);

        verify(repository).save(updatedTask);
    }

    @Test
    void updateTask_throwsException_whenTitleIsNotUnique() {
        UUID id = UUID.randomUUID();

        Task updatedTask = new Task();
        updatedTask.setId(id);

        doThrow(new ValidationException("Task title must be unique")).when(validator).checkTitleValidity(updatedTask);

        assertThrows(ValidationException.class, () -> {
            service.updateTask(updatedTask);
        });

        verify(repository, never()).save(any());
    }

    @Test
    void updateTask_throwsException_whenStatusIsComplete() {
        UUID id = UUID.randomUUID();

        Task updatedTask = new Task();
        updatedTask.setId(id);

        Task task = new Task();
        task.setStatus(TaskStatus.COMPLETE);

        doNothing().when(validator).checkTitleValidity(updatedTask);
        when(repository.findById(id)).thenReturn(Optional.of(task));

        assertThrows(TaskStatusException.class, () -> {
            service.updateTask(updatedTask);
        });

        verify(repository, never()).save(any());
    }

    @Test
    void updateTask_throwsException_whenTaskIsNotFound() {
        UUID id = UUID.randomUUID();

        Task updatedTask = new Task();
        updatedTask.setId(id);

        doNothing().when(validator).checkTitleValidity(updatedTask);
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

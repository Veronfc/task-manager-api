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
class TaskServiceTests {
    @Mock
    private TaskRepository db;

    @Mock
    private TaskValidator validator;

    @InjectMocks
    private TaskService service;

    @Test
    void createTask_shouldCreate_whenTitleIsUniqueAndDueDateIsAtleast12HoursInFuture() {
        LocalDateTime dueDate = LocalDateTime.now();

        Task task = new Task();
        task.setDueDate(dueDate);

        doNothing().when(validator).checkTitleValidity(task);
        doNothing().when(validator).checkDueDateValidity(dueDate);
        when(db.save(task)).thenReturn(task);

        Task result = service.createTask(task);
        assertEquals(task, result);

        verify(db).save(task);
    }

    @Test
    void createTask_shouldThrowException_whenTitleIsNotUnique() {
        Task task = new Task();

        doThrow(new ValidationException("Task title must be unique")).when(validator).checkTitleValidity(task);

        assertThrows(ValidationException.class, () -> {
            service.createTask(task);
        });

        verify(db, never()).save(any());
    }

    @Test
    void createTask_shouldThrowException_whenDueDateIsNot12HoursInFuture() {
        LocalDateTime dueDate = LocalDateTime.now();

        Task task = new Task();
        task.setDueDate(dueDate);

        doNothing().when(validator).checkTitleValidity(task);
        doThrow(new ValidationException("Task due date must be at least 12 hours in the future")).when(validator)
                .checkDueDateValidity(dueDate);

        assertThrows(ValidationException.class, () -> {
            service.createTask(task);
        });

        verify(db, never()).save(any());
    }

    @Test
    void retrieveTask_shouldRetrieve_whenTaskExists() {
        String strId = "fe536052-58dc-40f7-9efa-3a88b1eb82da";
        UUID id = UUID.fromString(strId);

        Task task = new Task();

        when(validator.checkIdValidity(strId)).thenReturn(id);
        when(db.findById(id)).thenReturn(Optional.of(task));

        Task result = service.retrieveTask(strId);
        assertEquals(task, result);

        verify(db).findById(id);
    }

    @Test
    void retrieveTask_shouldThrowException_whenTaskIsNotFound() {
        String strId = "aa07cf6a-127a-43a8-bb54-4d45b76e6e73";
        UUID id = UUID.fromString(strId);

        when(validator.checkIdValidity(strId)).thenReturn(id);
        when(db.findById(id)).thenReturn(Optional.empty());

        assertThrows(TaskNotFoundException.class, () -> {
            service.retrieveTask(strId);
        });

        verify(db).findById(id);
    }

    @Test
    void updateTask_shouldUpdate_whenTitleIsUniqueAndStatusIsNotComplete() {
        UUID id = UUID.randomUUID();

        Task updatedTask = new Task();
        updatedTask.setId(id);

        Task task = new Task();
        task.setStatus(TaskStatus.IN_PROGRESS);

        doNothing().when(validator).checkTitleValidity(updatedTask);
        when(db.findById(id)).thenReturn(Optional.of(task));
        when(db.save(updatedTask)).thenReturn(updatedTask);

        Task result = service.updateTask(updatedTask);
        assertEquals(updatedTask, result);

        verify(db).save(updatedTask);
    }

    @Test
    void updateTask_shouldThrowException_whenTitleIsNotUnique() {
        UUID id = UUID.randomUUID();

        Task updatedTask = new Task();
        updatedTask.setId(id);

        doThrow(new ValidationException("Task title must be unique")).when(validator).checkTitleValidity(updatedTask);

        assertThrows(ValidationException.class, () -> {
            service.updateTask(updatedTask);
        });

        verify(db, never()).save(any());
    }

    @Test
    void updateTask_shouldThrowException_whenStatusIsComplete() {
        UUID id = UUID.randomUUID();

        Task updatedTask = new Task();
        updatedTask.setId(id);

        Task task = new Task();
        task.setStatus(TaskStatus.COMPLETE);

        doNothing().when(validator).checkTitleValidity(updatedTask);
        when(db.findById(id)).thenReturn(Optional.of(task));

        assertThrows(TaskStatusException.class, () -> {
            service.updateTask(updatedTask);
        });

        verify(db, never()).save(any());
    }

    @Test
    void updateTask_shouldThrowException_whenTaskIsNotFound() {
        UUID id = UUID.randomUUID();

        Task updatedTask = new Task();
        updatedTask.setId(id);

        doNothing().when(validator).checkTitleValidity(updatedTask);
        when(db.findById(id)).thenReturn(Optional.empty());

        assertThrows(TaskNotFoundException.class, () -> {
            service.updateTask(updatedTask);
        });

        verify(db, never()).save(any());
    }

    @Test
    void deleteTask_shouldDelete_whenTaskExistsAndStatusIsNotArchived() {
        String strId = "7aecd703-0d6b-4c62-92d8-7a42221d02a1";
        UUID id = UUID.fromString(strId);

        Task task = new Task();
        task.setId(id);
        task.setStatus(TaskStatus.IN_PROGRESS);

        when(validator.checkIdValidity(strId)).thenReturn(id);
        when(db.findById(id)).thenReturn(Optional.of(task));

        service.deleteTask(strId);

        verify(db).deleteById(id);
    }

    @Test
    void deleteTask_shouldThrowException_whenStatusIsArchived() {
        String strId = "1cddbd37-6360-4d2a-ba6f-f67d8dc8cfc4";
        UUID id = UUID.fromString(strId);

        Task task = new Task();
        task.setId(id);
        task.setStatus(TaskStatus.ARCHIVED);

        when(validator.checkIdValidity(strId)).thenReturn(id);
        when(db.findById(id)).thenReturn(Optional.of(task));

        assertThrows(TaskStatusException.class, () -> {
            service.deleteTask(strId);
        });

        verify(db, never()).deleteById(any());
    }

    @Test
    void deleteTask_shouldThrowException_whenTaskIsNotFound() {
        String strId = "84d96944-dd31-4e24-ae22-35bb5a193ede";
        UUID id = UUID.fromString(strId);

        when(validator.checkIdValidity(strId)).thenReturn(id);
        when(db.findById(id)).thenReturn(Optional.empty());

        assertThrows(TaskNotFoundException.class, () -> {
            service.deleteTask(strId);
        });

        verify(db, never()).deleteById(any());
    }
}

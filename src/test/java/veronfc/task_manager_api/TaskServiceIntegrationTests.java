package veronfc.task_manager_api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cglib.core.Local;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import jakarta.validation.ValidationException;

@SpringBootTest
@TestInstance(Lifecycle.PER_CLASS)
@Transactional
@Rollback
class TaskServiceIntegrationTests {
    @Autowired
    private TaskRepository repository;

    @Autowired
    private TaskService service;

    @Test
    void createTask_persistsTask_whenTaskIsValid() {
        String title = "This is a task title";
        String description = "This is a task description";
        LocalDateTime dueDate = LocalDateTime.now().plus(2, ChronoUnit.DAYS);

        Task task = new Task();
        task.setTitle(title);
        task.setDescription(description);
        task.setStatus(TaskStatus.BACKLOG);
        task.setDueDate(dueDate);

        Task created = service.createTask(task);

        assertNotNull(created);

        assertEquals(title, created.getTitle());
        assertEquals(description, created.getDescription());
        assertEquals(TaskStatus.BACKLOG, created.getStatus());
        assertEquals(dueDate.truncatedTo(ChronoUnit.SECONDS), created.getDueDate().truncatedTo(ChronoUnit.SECONDS));

        assertTrue(repository.findById(created.getId()).isPresent());
    }

    @Test
    void createTask_throwsException_whenTitleIsNotUnique() {
        Task task = new Task();
        task.setTitle("This is not a unique title");
        task.setDueDate(LocalDateTime.now().plus(24, ChronoUnit.HOURS));

        Task anotherTask = new Task();
        anotherTask.setTitle("This is not a unique title");
        anotherTask.setDueDate(LocalDateTime.now().plus(24, ChronoUnit.HOURS));

        service.createTask(task);

        assertThrows(ValidationException.class, () -> {
            service.createTask(anotherTask);
        });

        assertEquals(1, repository.count());
    }

    @Test
    void createTask_throwsException_whenDueDateIsNot12HoursInTheFuture() {
        Task task = new Task();
        task.setTitle("This is a task title");
        task.setDueDate(LocalDateTime.now());

        assertThrows(ValidationException.class, () -> {
            service.createTask(task);
        });

        assertEquals(0, repository.count());
    }

    @Test
    void retrieveTask_returnsTask_whenTaskExists() {
        Task task = new Task();
        task.setTitle("This is yet another title");
        task.setDueDate(LocalDateTime.now().plus(3, ChronoUnit.DAYS));

        repository.save(task);

        Task result = service.retrieveTask(task.getId().toString());
        
        assertNotNull(result);
        assertEquals(task, result);
    }

    @Test
    void retrieveTask_throwsException_whenTaskIsNotFound() {
        String strId = "5054cd1d-1c93-4e97-80c7-6c468ad20cf9";

        assertThrows(TaskNotFoundException.class, () -> {
            service.retrieveTask(strId);
        });

        assertTrue(repository.findById(UUID.fromString(strId)).isEmpty());
    }

    @Test
    void updateTask_persistsUpdatedTask_whenTaskIsValid() {
        Task task = new Task();
        task.setTitle("This is a title too");
        task.setDueDate(LocalDateTime.now().plus(24, ChronoUnit.HOURS));

        Task saved = repository.save(task);
        saved.setTitle("This is some other title");

        Task result = service.updateTask(saved);

        assertEquals(saved.getTitle(), result.getTitle());
        assertTrue(repository.findById(saved.getId()).isPresent());
    }

    @Test
    void updateTask_throwsException_whenTitleIsNotUnique() {
        String title = "This is another title";
        LocalDateTime dueDate = LocalDateTime.now().plus(24, ChronoUnit.HOURS);

        Task task = new Task();
        task.setTitle(title);
        task.setDueDate(dueDate);

        Task anotherTask = new Task();
        anotherTask.setTitle(title);

        repository.save(task);

        assertThrows(ValidationException.class, () -> {
            service.updateTask(anotherTask);
        });
    }

    @Test
    void updateTask_throwsException_whenStatusIsComplete() {
        Task task = new Task();
        task.setTitle("This is an additional title");
        task.setDueDate(LocalDateTime.now().plus(24, ChronoUnit.HOURS));
        task.setStatus(TaskStatus.COMPLETE);

        Task saved = repository.save(task);

        assertThrows(TaskStatusException.class, () -> {
            service.updateTask(saved);
        });
    }

    @Test
    void updateTask_throwsException_whenTaskIsNotFound() {
        Task task = new Task();
        task.setId(UUID.randomUUID());
        task.setTitle("This is an additional title");

        assertThrows(TaskNotFoundException.class, () -> {
            service.updateTask(task);
        });
    }

    @Test
    void deleteTask_removesTask_whenTaskExists() {
        Task task = new Task();
        task.setTitle("This is once again a task title");
        task.setDueDate(LocalDateTime.now().plus(13, ChronoUnit.HOURS));
        
        UUID id = repository.save(task).getId();

        service.deleteTask(id.toString());

        assertTrue(repository.findById(id).isEmpty());
    }

    @Test
    void deleteTask_throwsException_whenTaskIsArchived() {
        Task task = new Task();
        task.setTitle("Wow another title");
        task.setDueDate(LocalDateTime.now().plus(2, ChronoUnit.MONTHS));
        task.setStatus(TaskStatus.ARCHIVED);

        UUID id = repository.save(task).getId();

        assertThrows(TaskStatusException.class, () -> {
            service.deleteTask(id.toString());
        });

        assertTrue(repository.findById(id).isPresent());
    }

    @Test
    void deleteTask_throwsException_whenTaskIsNotFound() {
        String strId = "c9b16c15-e7a5-46d0-829e-cc0f2213aefd";

        assertThrows(TaskNotFoundException.class, () -> {
            service.deleteTask(strId);
        });

        assertTrue(repository.findById(UUID.fromString(strId)).isEmpty());
    }
}

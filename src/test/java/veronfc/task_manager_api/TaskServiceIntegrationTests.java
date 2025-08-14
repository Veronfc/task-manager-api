package veronfc.task_manager_api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.mockito.InOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Transactional;

import jakarta.validation.ValidationException;

@SpringBootTest
@TestInstance(Lifecycle.PER_CLASS)
@Transactional
@Rollback
class TaskServiceIntegrationTests {
    @Autowired
    @MockitoSpyBean
    private TaskRepository repository;

    @Autowired
    @MockitoSpyBean
    private TaskValidator validator;

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

        InOrder inOrder = inOrder(repository, validator);
        inOrder.verify(validator).checkTitleValidity(task);
        inOrder.verify(validator).checkDueDateValidity(dueDate);
        inOrder.verify(repository).save(task);
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

        InOrder inOrder = inOrder(validator);
        inOrder.verify(validator).checkTitleValidity(task);
    }

    @Test
    void createTask_throwsException_whenDueDateIsNot12HoursInTheFuture() {
        LocalDateTime dueDate = LocalDateTime.now();

        Task task = new Task();
        task.setTitle("This is a task title");
        task.setDueDate(dueDate);

        assertThrows(ValidationException.class, () -> {
            service.createTask(task);
        });

        assertEquals(0, repository.count());

        InOrder inOrder = inOrder(validator);
        inOrder.verify(validator).checkTitleValidity(task);
        inOrder.verify(validator).checkDueDateValidity(dueDate);
    }

    @Test
    void retrieveTask_returnsTask_whenTaskExists() {
        Task task = new Task();
        task.setTitle("This is yet another title");
        task.setDueDate(LocalDateTime.now().plus(3, ChronoUnit.DAYS));

        repository.save(task);

        Task result = service.retrieveTask(task.getId().toString());
        
        UUID id = result.getId();

        assertNotNull(result);
        assertEquals(task, result);

        InOrder inOrder = inOrder(repository, validator);
        inOrder.verify(validator).checkIdValidity(id.toString());
        inOrder.verify(repository).findById(id);
    }

    @Test
    void retrieveTask_throwsException_whenStringIdInvalid() {
        String strId = "57881fdc_4a9c_4861_a714_f33d9ef6e79c";

        assertThrows(ValidationException.class, () -> {
            service.retrieveTask(strId);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            repository.findById(UUID.fromString(strId));
        });

        InOrder inOrder = inOrder(validator);
        inOrder.verify(validator).checkIdValidity(strId);
    }

    @Test
    void retrieveTask_throwsException_whenTaskIsNotFound() {
        String strId = "5054cd1d-1c93-4e97-80c7-6c468ad20cf9";

        assertThrows(TaskNotFoundException.class, () -> {
            service.retrieveTask(strId);
        });

        assertTrue(repository.findById(UUID.fromString(strId)).isEmpty());

        InOrder inOrder = inOrder(repository, validator);
        inOrder.verify(validator).checkIdValidity(strId);
        inOrder.verify(repository, times(2)).findById(UUID.fromString(strId));
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

        InOrder inOrder = inOrder(repository, validator);
        inOrder.verify(validator).checkTitleValidity(saved);
        inOrder.verify(repository).findById(saved.getId());
        inOrder.verify(repository).save(saved);
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

        InOrder inOrder = inOrder(validator);
        inOrder.verify(validator).checkTitleValidity(anotherTask);
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

        InOrder inOrder = inOrder(repository, validator);
        inOrder.verify(validator).checkTitleValidity(saved);
        inOrder.verify(repository).findById(saved.getId());
    }

    @Test
    void updateTask_throwsException_whenTaskIsNotFound() {
        Task task = new Task();
        task.setId(UUID.randomUUID());
        task.setTitle("This is an additional title");

        assertThrows(TaskNotFoundException.class, () -> {
            service.updateTask(task);
        });

        InOrder inOrder = inOrder(repository, validator);
        inOrder.verify(validator).checkTitleValidity(task);
        inOrder.verify(repository).findById(task.getId());
    }

    @Test
    void deleteTask_removesTask_whenTaskExists() {
        Task task = new Task();
        task.setTitle("This is once again a task title");
        task.setDueDate(LocalDateTime.now().plus(13, ChronoUnit.HOURS));
        
        UUID id = repository.save(task).getId();

        service.deleteTask(id.toString());

        assertTrue(repository.findById(id).isEmpty());

        InOrder inOrder = inOrder(repository, validator);
        inOrder.verify(validator).checkIdValidity(id.toString());
        inOrder.verify(repository).findById(task.getId());
        inOrder.verify(repository).deleteById(id);
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

        InOrder inOrder = inOrder(repository, validator);
        inOrder.verify(validator).checkIdValidity(id.toString());
        inOrder.verify(repository, times(2)).findById(task.getId());
    }

    @Test
    void deleteTask_throwsException_whenTaskIsNotFound() {
        String strId = "c9b16c15-e7a5-46d0-829e-cc0f2213aefd";

        assertThrows(TaskNotFoundException.class, () -> {
            service.deleteTask(strId);
        });

        assertTrue(repository.findById(UUID.fromString(strId)).isEmpty());

        InOrder inOrder = inOrder(repository, validator);
        inOrder.verify(validator).checkIdValidity(strId);
        inOrder.verify(repository, times(2)).findById(UUID.fromString(strId));
    }
}

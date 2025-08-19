package veronfc.task_manager_api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cglib.core.Local;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.validation.ValidationException;

@SpringBootTest
@Transactional
@Rollback
@AutoConfigureMockMvc
class TaskControllerIntegrationTests {
    @Autowired 
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private TaskRepository repository;

    @Test
    void getAllTasks_returnsListOfTasks() throws Exception {
        List<Task> tasks = repository.findAll();

        mockMvc.perform(get("/all"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$").value(tasks));
    }

    @Test
    void postTask_returnsCreatedTask_whenTaskIsValid() throws Exception {
        String title = "This is a title";
        LocalDateTime dueDate = LocalDateTime.now().plusHours(24).truncatedTo(ChronoUnit.SECONDS);

        CreateTaskDto task = new CreateTaskDto(title, dueDate);

        mockMvc.perform(post("/")
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(task)))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.title").value(title))
            .andExpect(jsonPath("$.dueDate").value(dueDate.toString()))
            .andExpect(jsonPath("$.status").value(TaskStatus.BACKLOG.toString()));
    }

    @Test
    void postTask_returnsBadRequest_whenTitleIsNotUnique() throws Exception {
        String title = "This title is not unique";
        LocalDateTime dueDate = LocalDateTime.now().plusHours(13);
        
        CreateTaskDto task = new CreateTaskDto(title, dueDate);

        Task anotherTask = new Task();
        anotherTask.setTitle(title);
        anotherTask.setDueDate(dueDate);

        repository.save(anotherTask);

        mockMvc.perform(post("/")
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(task)))
            .andExpect(status().isBadRequest())
            .andExpect(result -> assertTrue(result.getResolvedException() instanceof ValidationException))
            .andExpect(result -> assertEquals("Task title must be unique", result.getResolvedException().getMessage()));
    }

    @Test
    void postTask_returnsBadRequest_whenDueDateNotAtLeast12HoursInFuture() throws Exception {
        CreateTaskDto task = new CreateTaskDto("This is a title", LocalDateTime.now().plusHours(5));

        mockMvc.perform(post("/")
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(task)))
            .andExpect(status().isBadRequest())
            .andExpect(result -> assertTrue(result.getResolvedException() instanceof ValidationException))
            .andExpect(result -> assertEquals("Task due date must be at least 12 hours in the future", result.getResolvedException().getMessage()));
    }

    @Test
    void getTask_returnsTask_whenTaskExists() throws Exception {
        String title = "This is a task title";
        String description = "This is a task description";
        LocalDateTime dueDate = LocalDateTime.now().plusDays(7).truncatedTo(ChronoUnit.SECONDS);

        Task task = new Task();
        task.setTitle(title);
        task.setDescription(description);
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setDueDate(dueDate);

        UUID id = repository.save(task).getId();

        mockMvc.perform(get("/{id}", id.toString()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value(id.toString()))
            .andExpect(jsonPath("$.title").value(title))
            .andExpect(jsonPath("$.description").value(description))
            .andExpect(jsonPath("$.status").value(TaskStatus.IN_PROGRESS.toString()))
            .andExpect(jsonPath("$.dueDate").value(dueDate.toString()));
    }

    @Test
    void getTask_returnsBadRequest_whenIdIsInvalid() throws Exception {
        String strId = "966534f9.2def.4407.85b2.9fbd0f799e6a";

        mockMvc.perform(get("/{id}", strId))
            .andExpect(status().isBadRequest())
            .andExpect(result -> assertTrue(result.getResolvedException() instanceof ValidationException))
            .andExpect(result -> assertEquals("Task ID must be a UUID", result.getResolvedException().getMessage()));

    }

    @Test
    void getTask_returnsNotFound_whenTaskIsNotFound() throws Exception {
        String strId = UUID.randomUUID().toString();

        mockMvc.perform(get("/{id}", strId))
            .andExpect(status().isNotFound())
            .andExpect(result -> assertTrue(result.getResolvedException() instanceof TaskNotFoundException))
            .andExpect(result -> assertEquals(String.format("Task with ID: %s could not be found", strId), result.getResolvedException().getMessage()));
    }

    @Test
    void putTask_returnsTask_whenUpdatedTaskIsValid() throws Exception {
        LocalDateTime dueDate = LocalDateTime.now().plusDays(2).truncatedTo(ChronoUnit.SECONDS);

        Task task = new Task();
        task.setTitle("This is a title");
        task.setDueDate(dueDate);

        UUID id = repository.save(task).getId();

        UpdateTaskDto updatedTask = new UpdateTaskDto(id);
        updatedTask.setTitle("This is a new title");

        mockMvc.perform(put("/")
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(updatedTask)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("This is a new title"))
            .andExpect(jsonPath("$.dueDate").value(dueDate.toString()))
            .andExpect(jsonPath("$.status").value(TaskStatus.BACKLOG.toString()));
    }

    @Test
    void putTask_returnsBadRequest_whenTitleIsNotUnique() throws Exception {
        Task task = new Task();
        task.setTitle("This is a title");
        task.setDueDate(LocalDateTime.now().plusDays(2).truncatedTo(ChronoUnit.SECONDS));

        repository.save(task);

        UpdateTaskDto anotherTask = new UpdateTaskDto(UUID.randomUUID());
        anotherTask.setTitle("This is a title");

        mockMvc.perform(put("/")
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(anotherTask)))
            .andExpect(status().isBadRequest())
            .andExpect(result -> assertTrue(result.getResolvedException() instanceof ValidationException))
            .andExpect(result -> assertEquals("Task title must be unique", result.getResolvedException().getMessage()));
    }

    @Test
    void putTask_returnsNotFound_whenTaskIsNotFound() throws Exception {
        UUID id = UUID.randomUUID();

        UpdateTaskDto anotherTask = new UpdateTaskDto(id);
        anotherTask.setTitle("This is a title");

        mockMvc.perform(put("/")
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(anotherTask)))
            .andExpect(status().isNotFound())
            .andExpect(result -> assertTrue(result.getResolvedException() instanceof TaskNotFoundException))
            .andExpect(result -> assertEquals(String.format("Task with ID: %s could not be found", id.toString()), result.getResolvedException().getMessage()));
    }

    @Test
    void putTask_returnsConflict_whenTaskIsComplete() throws Exception {
        Task task = new Task();
        task.setTitle("This is a title");
        task.setStatus(TaskStatus.COMPLETE);
        task.setDueDate(LocalDateTime.now().plusDays(2).truncatedTo(ChronoUnit.SECONDS));

        UUID id = repository.save(task).getId();

        UpdateTaskDto anotherTask = new UpdateTaskDto(id);
        anotherTask.setTitle("This is a new title");

        mockMvc.perform(put("/")
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(anotherTask)))
            .andExpect(status().isConflict())
            .andExpect(result -> assertTrue(result.getResolvedException() instanceof TaskStatusException))
            .andExpect(result -> assertEquals(String.format("Task with ID: %s is marked as 'Complete' and can not be updated further", id.toString()), result.getResolvedException().getMessage()));
    }

    @Test
    void deleteTask_returnsNoContent_whenTaskExists() throws Exception {
        Task task = new Task();
        task.setTitle("This is a title");
        task.setDueDate(LocalDateTime.now().plusDays(2).truncatedTo(ChronoUnit.SECONDS));

        UUID id = repository.save(task).getId();

        mockMvc.perform(delete("/{id}", id.toString()))
            .andExpect(status().isNoContent());
    }

    @Test
    void deleteTask_returnsNotFound_whenTaskIsNotFound() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/{id}", id.toString()))
            .andExpect(status().isNotFound())
            .andExpect(result -> assertTrue(result.getResolvedException() instanceof TaskNotFoundException))
            .andExpect(result -> assertEquals(String.format("Task with ID: %s could not be found", id.toString()), result.getResolvedException().getMessage()));
    }

    @Test
    void deleteTask_returnsConflict_whenTaskIsArchived() throws Exception {
        Task task = new Task();
        task.setTitle("This is a title");
        task.setStatus(TaskStatus.ARCHIVED);
        task.setDueDate(LocalDateTime.now().plusDays(2).truncatedTo(ChronoUnit.SECONDS));

        UUID id = repository.save(task).getId();

        mockMvc.perform(delete("/{id}", id.toString()))
            .andExpect(status().isConflict())
            .andExpect(result -> assertTrue(result.getResolvedException() instanceof TaskStatusException))
            .andExpect(result -> assertEquals(String.format("Task with ID: %s is marked as 'Archived' and can not be deleted", id.toString()), result.getResolvedException().getMessage()));
    }

    @Test
    void deleteTask_returnsBadRequest_whenIdIsInvalid() throws Exception {
        String strId = "c46a9b62?84b4?4fdd?a8a5?737354e546f9";

        mockMvc.perform(delete("/{id}", strId))
            .andExpect(status().isBadRequest())
            .andExpect(result -> assertTrue(result.getResolvedException() instanceof ValidationException))
            .andExpect(result -> assertEquals("Task ID must be a UUID", result.getResolvedException().getMessage()));
    }
}

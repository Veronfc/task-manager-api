package veronfc.task_manager_api;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.rmi.ServerException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.validation.ValidationException;

@WebMvcTest(TaskController.class)
class TaskControllerUnitTests {
    @Autowired 
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TaskService service;

    @Test
    void getAllTasks_returnsListOfTasks() throws Exception {
        UUID id = UUID.randomUUID();
        
        Task task = new Task();
        task.setId(id);
        
        List<Task> tasks = new ArrayList<Task>();
        tasks.add(task);

        when(service.retrieveAllTasks()).thenReturn(tasks);

        mockMvc.perform(get("/all"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$[0].id").value(id.toString()));

        verify(service).retrieveAllTasks();
    }

    @Test
    void getAllTasks_returnsServerError_whenUnhandledExceptionIsThrown() throws Exception {
        when(service.retrieveAllTasks()).thenThrow(new RuntimeException());

        mockMvc.perform(get("/all"))
            .andExpect(status().isInternalServerError());

        verify(service).retrieveAllTasks();
    }
    
    @Test
    void postTask_returnCreatedTask_whenTaskIsValid() throws Exception {
        String strId = "111bdc41-5a08-47bd-8553-df9a043e836a";
        String title = "This is a title";
        LocalDateTime dueDate = LocalDateTime.now().plusHours(13);
        
        Task task = new Task();
        task.setId(UUID.fromString(strId));
        task.setTitle(title);
        task.setDueDate(dueDate);
        
        when(service.createTask(task)).thenReturn(task);
        
        mockMvc.perform(post("/")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(task)))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id").value(strId))
        .andExpect(jsonPath("$.title").value(title));
        
        verify(service).createTask(task);
    }
    
    @Test
    void postTask_returnsBadRequest_whenTitleIsNotUniqueOrDueDateNotAtLeast12HoursInFuture() throws Exception {
        Task task = new Task();
        
        when(service.createTask(task)).thenThrow(new ValidationException());
        
        mockMvc.perform(post("/")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(task)))
        .andExpect(status().isBadRequest());

        verify(service).createTask(task);
    }
    
    @Test
    void postTask_returnsServerError_whenUnhandledExceptionIsThrown() throws Exception {
        Task task = new Task();
        
        when(service.createTask(task)).thenThrow(new RuntimeException());
        
        mockMvc.perform(post("/")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(task)))
        .andExpect(status().isInternalServerError());

        verify(service).createTask(task);
    }
    
    @Test
    void getTask_returnsTask_whenTaskExists() throws Exception {
        String strId = "4e71e3d5-d134-432a-9ea0-707f5c95be5b";
        String title = "This is a task title";

        Task task = new Task();
        task.setId(UUID.fromString(strId));
        task.setTitle(title);

        when(service.retrieveTask(strId)).thenReturn(task);

        mockMvc.perform(get("/{id}", strId))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value(strId))
            .andExpect(jsonPath("$.title").value(title));

        verify(service).retrieveTask(strId);
    }

    @Test
    void getTask_returnsBadRequest_whenIdIsInvalid() throws Exception {
        String strId = "a24807d4=561c=4754=be8e=63dc723d1bbd";

        when(service.retrieveTask(strId)).thenThrow(new ValidationException());

        mockMvc.perform(get("/{id}", strId))
            .andExpect(status().isBadRequest());

        verify(service).retrieveTask(strId);
    }

    @Test
    void getTask_returnsNotFound_whenTaskIsNotFound() throws Exception {
        String strId = "34d68aec-9deb-4e7e-a28e-5f95455141b8";

        when(service.retrieveTask(strId)).thenThrow(new TaskNotFoundException(UUID.fromString(strId)));

        mockMvc.perform(get("/{id}", strId))
            .andExpect(status().isNotFound());

        verify(service).retrieveTask(strId);
    }

    @Test
    void getTask_returnsServerError_whenUnhandledExceptionIsThrown() throws Exception {
        String strId = "ad29f4d2-5411-4c08-a2e6-222910e0f5c5";

        when(service.retrieveTask(strId)).thenThrow(new RuntimeException());

        mockMvc.perform(get("/{id}", strId))
            .andExpect(status().isInternalServerError());

        verify(service).retrieveTask(strId);
    }

    @Test
    void updateTask_returnsTask_whenUpdatedTaskIsValid() throws Exception {
        UUID id = UUID.randomUUID();
        String title = "This is a task title";

        Task task = new Task();
        task.setId(id);
        task.setTitle(title);

        when(service.updateTask(task)).thenReturn(task);

        mockMvc.perform(put("/")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(task)))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value(id.toString()))
            .andExpect(jsonPath("$.title").value(title));

        verify(service).updateTask(task);
    }

    @Test
    void updateTask_returnsBadRequest_whenIdIsInvalid() throws Exception {
        Task task = new Task();

        when (service.updateTask(task)).thenThrow(new ValidationException());

        mockMvc.perform(put("/")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(task)))
            .andExpect(status().isBadRequest());

        verify(service).updateTask(task);
    }

    @Test
    void updateTask_returnsNotFound_whenTaskIsNotFound() throws Exception {
        Task task = new Task();

        when (service.updateTask(task)).thenThrow(new TaskNotFoundException(null));

        mockMvc.perform(put("/")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(task)))
            .andExpect(status().isNotFound());

        verify(service).updateTask(task);
    }

    @Test
    void updateTask_returnsConflict_whenTaskIsComplete() throws Exception {
        Task task = new Task();

        when (service.updateTask(task)).thenThrow(new TaskStatusException(null));

        mockMvc.perform(put("/")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(task)))
            .andExpect(status().isConflict());

        verify(service).updateTask(task);
    }

    @Test
    void updateTask_returnsServerError_whenUnhandledExceptionIsThrown() throws Exception {
        Task task = new Task();

        when (service.updateTask(task)).thenThrow(new RuntimeException());

        mockMvc.perform(put("/")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(task)))
            .andExpect(status().isInternalServerError());

        verify(service).updateTask(task);
    }

    @Test
    void deleteTask_returnsNoContent_whenTaskExists() throws Exception {
        String strId = "eb77fb2a-c8ca-4ae5-b30c-81178cd58c32";

        mockMvc.perform(delete("/{id}", strId))
            .andExpect(status().isNoContent());

        verify(service).deleteTask(strId);
    }

    @Test
    void deleteTask_returnsNotFound_whenTaskIsNotFound() throws Exception {
        String strId = "814b33db-34d6-44d7-8b9f-8c4cf73e62c7";

        doThrow(new TaskNotFoundException(null)).when(service).deleteTask(strId);;

        mockMvc.perform(delete("/{id}", strId))
            .andExpect(status().isNotFound());
    }

    @Test
    void deleteTask_returnsConflict_whenTaskIsArchived() throws Exception {
        String strId = "8cdb1479-7b97-4d7c-bfe2-ce1c205a98aa";

        doThrow(new TaskStatusException(null)).when(service).deleteTask(strId);;

        mockMvc.perform(delete("/{id}", strId))
            .andExpect(status().isConflict());
    }

    @Test
    void deleteTask_returnsBadRequest_whenIdIsInvalid() throws Exception {
        String strId = "da5eb899-fb6f-499d-81e7-7484843b66d8";

        doThrow(new ValidationException()).when(service).deleteTask(strId);

        mockMvc.perform(delete("/{id}", strId))
            .andExpect(status().isBadRequest());
    }

    @Test
    void deleteTask_returnsServerError_whenUnhandledExceptionIsThrown() throws Exception {
        String strId = "f431228f-e271-461d-b8d1-e9c02163e191";

        doThrow(new RuntimeException()).when(service).deleteTask(strId);;

        mockMvc.perform(delete("/{id}", strId))
            .andExpect(status().isInternalServerError());
    }
}
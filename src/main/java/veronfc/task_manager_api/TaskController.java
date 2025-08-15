package veronfc.task_manager_api;

import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;


@RestController
class TaskController {
    private final TaskService service;

    TaskController(TaskService service) {
        this.service = service;
    }

    @GetMapping("all")
    List<Task> getAllTasks() {
        return service.retrieveAllTasks();
    }
    
    @PostMapping
    Task postTask(@Valid @RequestBody CreateTaskDto task) {
        return service.createTask(task);
    }

    @GetMapping("{id}")
    Task getTask(@PathVariable String id) {
        return service.retrieveTask(id);
    }
    
    @PutMapping
    Task putTask(@Valid @RequestBody UpdateTaskDto task) {
        return service.updateTask(task);
    }

    @DeleteMapping("{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteTask(@PathVariable String id) {
        service.deleteTask(id);
    }
}

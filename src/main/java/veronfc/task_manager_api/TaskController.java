package veronfc.task_manager_api;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.ValidationException;

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
        try {
            return service.retrieveAllTasks();
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
        }
    }
    
    @PostMapping
    Task postTask(@RequestBody Task task) {
        try {
            return service.createTask(task);
        } catch (Exception ex) {
            if (ex instanceof ValidationException) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
            }

            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
        }
    }

    @GetMapping("{id}")
    Task getTask(@PathVariable String id) {
        try {
            return service.retrieveTask(id);
        } catch (Exception ex) {
            if (ex instanceof ValidationException) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
            }

            if (ex instanceof TaskNotFoundException) {
                throw ex;
            }
            
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
        }
    }
    
    @PutMapping
    Task putTask(@RequestBody Task task) {
        try {
            return service.updateTask(task);
        } catch (Exception ex) {
            if (ex instanceof ValidationException) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
            }

            if (ex instanceof TaskNotFoundException || ex instanceof TaskStatusException) {
                throw ex;
            }

            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
        }
    }

    @DeleteMapping("{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteTask(@PathVariable String id) {
        try {
            service.deleteTask(id);
        } catch (Exception ex) {
            if (ex instanceof TaskNotFoundException || ex instanceof TaskStatusException) {
                throw ex;
            }

            if (ex instanceof ValidationException) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
            }

            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
        }
    }
}

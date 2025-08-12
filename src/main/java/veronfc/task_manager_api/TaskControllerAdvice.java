package veronfc.task_manager_api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class TaskControllerAdvice {
    
    @ExceptionHandler(TaskNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    String taskNotFoundHandler(TaskNotFoundException ex) {
        return ex.getMessage();
    }

    @ExceptionHandler(TaskStatusException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    String taskArchivedHandler(TaskStatusException ex) {
        return ex.getMessage();
    }
}

package veronfc.task_manager_api;

import java.util.UUID;

class TaskNotFoundException extends RuntimeException{
    TaskNotFoundException(UUID id) {
        super(String.format("Task with ID: %s could not be found", id));
    }
}

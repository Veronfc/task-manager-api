package veronfc.task_manager_api;

class TaskStatusException extends RuntimeException{
    TaskStatusException(String message) {
        super(message);
    }
}

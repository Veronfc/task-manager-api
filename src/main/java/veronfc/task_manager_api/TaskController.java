package veronfc.task_manager_api;

import org.springframework.web.bind.annotation.RestController;

@RestController
class TaskController {
    private final TaskRepository db;

    TaskController(TaskRepository db) {
        this.db = db;
    }
}

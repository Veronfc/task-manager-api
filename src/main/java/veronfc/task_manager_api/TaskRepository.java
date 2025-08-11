package veronfc.task_manager_api;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface TaskRepository extends JpaRepository<Task, UUID> {
    Task findByTitle(String title);
}

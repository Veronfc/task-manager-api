package veronfc.task_manager_api;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface TaskRepository extends JpaRepository<Task, UUID> {
    Optional<Task> findByTitle(String title);
}

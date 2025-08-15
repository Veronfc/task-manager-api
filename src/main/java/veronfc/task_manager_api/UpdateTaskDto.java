package veronfc.task_manager_api;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@ToString
class UpdateTaskDto {
    @NotNull(message = "Task ID must be present")
    private UUID id;

    @Size(max = 255, message = "Task title must not be more than 255 characters")
    private String title;

    @Size(max = 2000, message = "Task description must not be more than 2000 characters")
    private String description;

    private TaskStatus status;

    @Future(message = "Task due date must not be in the past or present")
    private LocalDateTime dueDate;

    UpdateTaskDto(UUID id) {
        this.id = id;
    }
}

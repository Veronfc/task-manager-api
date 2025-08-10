package veronfc.task_manager_api;

import java.time.ZonedDateTime;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@ToString
class Task {
    private @Id @GeneratedValue @UuidGenerator UUID id;
    private String title;
    private TaskStatus status;
    private ZonedDateTime createdAt;
    private ZonedDateTime updatedAt;
}
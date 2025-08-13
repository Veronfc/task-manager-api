package veronfc.task_manager_api;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.validation.ValidationException;

@ExtendWith(MockitoExtension.class)
class TaskValidatorUnitTest {
    @Mock
    TaskRepository db;

    @InjectMocks
    TaskValidator validator;

    @Test
    void checkIdValidity_shouldReturnUuid_whenStringIdIsValid() {
        String strId = "c67f27d7-0ab5-407a-ad59-03710ef90a64";
        UUID id = UUID.fromString(strId);

        UUID result = validator.checkIdValidity(strId);
        assertEquals(id, result);
    }

    @Test 
    void checkIdValidity_shouldThrowException_whenStringIdIsNull() {
        String strId = null;

        assertThrows(ValidationException.class, () -> {
            validator.checkIdValidity(strId);
        });
    }

    @Test 
    void checkIdValidity_shouldThrowException_whenStringIdIsBlank() {
        String strId = "";
        
        assertThrows(ValidationException.class, () -> {
            validator.checkIdValidity(strId);
        });
    }

    @Test
    void checkIdValidity_shouldThrowException_whenStringIdIsNotCorrectlyFormatted() {
        String strId = "68878689_d401_4ecc_82e2_fcc8787c328e";

        assertThrows(ValidationException.class, () -> {
            validator.checkIdValidity(strId);
        });
    }

    @Test
    void checkTitleValidity_shouldDoNothing_whenTaskExistsAndTitleIsUnique() {
        String title = "This title is unique";
        UUID id = UUID.randomUUID();

        Task task = new Task();
        task.setTitle(title);
        task.setId(id);

        Task foundTask = new Task();
        foundTask.setId(id);

        when(db.findByTitle(title)).thenReturn(Optional.of(foundTask));

        assertDoesNotThrow(() -> {
            validator.checkTitleValidity(task);
        });

        verify(db).findByTitle(title);
    }

    @Test
    void checkTitleValidity_shouldDoNothing_whenTaskDoesNotExistAndTitleIsUnique() {
        String title = "This title is also unique";

        Task task = new Task();
        task.setTitle(title);

        when(db.findByTitle(title)).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> {
            validator.checkTitleValidity(task);
        });

        verify(db).findByTitle(title);
    }

    @Test
    void checkTitleValidity_shouldThrowException_whenTaskExistsAndTitleIsNotUnique() {
        String title = "This title is not unique";

        Task task = new Task();
        task.setId(UUID.randomUUID());
        task.setTitle(title);

        Task foundTask = new Task();
        foundTask.setId(UUID.randomUUID());

        when(db.findByTitle(title)).thenReturn(Optional.of(foundTask));

        assertThrows(ValidationException.class, () -> {
            validator.checkTitleValidity(task);
        });

        verify(db).findByTitle(title);
    }

    @Test
    void checkDueDateValidity_shouldDoNothing_whenDueDateIsAtleast12HoursInTheFuture() {
        LocalDateTime dueDate = LocalDateTime.now().plus(13, ChronoUnit.HOURS);

        assertDoesNotThrow(() -> {
            validator.checkDueDateValidity(dueDate);
        });
    }
    
    @Test
    void checkDueDateValidity_shouldThrowException_whenDueDateIsNotAtleast12HoursInTheFuture() {
        LocalDateTime dueDate = LocalDateTime.now().plus(11, ChronoUnit.HOURS);

        assertThrows(ValidationException.class, () -> {
            validator.checkDueDateValidity(dueDate);
        });
    }
}
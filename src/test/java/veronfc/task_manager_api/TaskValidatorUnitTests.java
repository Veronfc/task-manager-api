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
    TaskRepository repository;

    @InjectMocks
    TaskValidator validator;

    @Test
    void checkIdValidity_returnsUuid_whenStringIdIsValid() {
        String strId = "c67f27d7-0ab5-407a-ad59-03710ef90a64";
        UUID id = UUID.fromString(strId);

        UUID result = validator.checkIdValidity(strId);
        assertEquals(id, result);
    }

    @Test 
    void checkIdValidity_throwsException_whenStringIdIsNull() {
        String strId = null;

        assertThrows(ValidationException.class, () -> {
            validator.checkIdValidity(strId);
        });
    }

    @Test 
    void checkIdValidity_throwsException_whenStringIdIsBlank() {
        String strId = "";
        
        assertThrows(ValidationException.class, () -> {
            validator.checkIdValidity(strId);
        });
    }

    @Test
    void checkIdValidity_throwsException_whenStringIdIsNotCorrectlyFormatted() {
        String strId = "68878689_d401_4ecc_82e2_fcc8787c328e";

        assertThrows(ValidationException.class, () -> {
            validator.checkIdValidity(strId);
        });
    }

    @Test
    void checkTitleValidity_doesNothing_whenTaskExistsAndTitleIsUnique() {
        UUID id = UUID.randomUUID();
        String title = "This title is unique";

        Task foundTask = new Task();
        foundTask.setId(id);

        when(repository.findByTitle(title)).thenReturn(Optional.of(foundTask));

        assertDoesNotThrow(() -> {
            validator.checkTitleValidity(title, id.toString());
        });

        verify(repository).findByTitle(title);
    }

    @Test
    void checkTitleValidity_doesNothing_whenTaskDoesNotExistAndTitleIsUnique() {
        String title = "This title is also unique";

        when(repository.findByTitle(title)).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> {
            validator.checkTitleValidity(title, null);
        });

        verify(repository).findByTitle(title);
    }

    @Test
    void checkTitleValidity_throwsException_whenTaskExistsAndTitleIsNotUnique() {
        UUID id = UUID.randomUUID();
        String title = "This title is not unique";

        Task foundTask = new Task();
        foundTask.setId(UUID.randomUUID());

        when(repository.findByTitle(title)).thenReturn(Optional.of(foundTask));

        assertThrows(ValidationException.class, () -> {
            validator.checkTitleValidity(title, id.toString());
        });

        verify(repository).findByTitle(title);
    }

    @Test
    void checkDueDateValidity_doesNothing_whenDueDateIsAtleast12HoursInTheFuture() {
        LocalDateTime dueDate = LocalDateTime.now().plus(13, ChronoUnit.HOURS);

        assertDoesNotThrow(() -> {
            validator.checkDueDateValidity(dueDate);
        });
    }
    
    @Test
    void checkDueDateValidity_throwsException_whenDueDateIsNotAtleast12HoursInTheFuture() {
        LocalDateTime dueDate = LocalDateTime.now().plus(11, ChronoUnit.HOURS);

        assertThrows(ValidationException.class, () -> {
            validator.checkDueDateValidity(dueDate);
        });
    }
}
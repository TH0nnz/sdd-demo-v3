package com.workreport.unit.exception;

import com.workreport.dto.common.ErrorResponse;
import com.workreport.exception.AccountDisabledException;
import com.workreport.exception.AccountLockedException;
import com.workreport.exception.BusinessRuleException;
import com.workreport.exception.GlobalExceptionHandler;
import com.workreport.exception.PasswordPolicyException;
import com.workreport.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @Mock
    private HttpServletRequest request;

    private static final String REQUEST_URI = "/api/users/123";

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        when(request.getRequestURI()).thenReturn(REQUEST_URI);
    }

    private void assertErrorResponse(ResponseEntity<ErrorResponse> response,
                                     HttpStatus expectedStatus,
                                     String expectedMessage) {
        assertThat(response.getStatusCode()).isEqualTo(expectedStatus);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(expectedStatus.value());
        assertThat(response.getBody().error()).isEqualTo(expectedStatus.getReasonPhrase());
        assertThat(response.getBody().message()).isEqualTo(expectedMessage);
        assertThat(response.getBody().path()).isEqualTo(REQUEST_URI);
        assertThat(response.getBody().timestamp()).isNotNull();
    }

    @Nested
    @DisplayName("handleResourceNotFound")
    class HandleResourceNotFound {

        @Test
        @DisplayName("回傳 404 與 message、path")
        void returnsNotFoundWithMessageAndPath() {
            ResourceNotFoundException ex = new ResourceNotFoundException("User not found: 123");
            ResponseEntity<ErrorResponse> result = handler.handleResourceNotFound(ex, request);
            assertErrorResponse(result, HttpStatus.NOT_FOUND, "User not found: 123");
        }
    }

    @Nested
    @DisplayName("handleBusinessRule")
    class HandleBusinessRule {

        @Test
        @DisplayName("預設建構子回傳 BAD_REQUEST")
        void defaultConstructorReturnsBadRequest() {
            BusinessRuleException ex = new BusinessRuleException("Invalid state");
            ResponseEntity<ErrorResponse> result = handler.handleBusinessRule(ex, request);
            assertErrorResponse(result, HttpStatus.BAD_REQUEST, "Invalid state");
        }

        @Test
        @DisplayName("帶 HttpStatus 建構子回傳該 status")
        void customStatusConstructorReturnsThatStatus() {
            BusinessRuleException ex = new BusinessRuleException("Conflict", HttpStatus.CONFLICT);
            ResponseEntity<ErrorResponse> result = handler.handleBusinessRule(ex, request);
            assertErrorResponse(result, HttpStatus.CONFLICT, "Conflict");
        }

        @Test
        @DisplayName("帶 FORBIDDEN 建構子回傳 403")
        void forbiddenStatusReturns403() {
            BusinessRuleException ex = new BusinessRuleException("Not allowed", HttpStatus.FORBIDDEN);
            ResponseEntity<ErrorResponse> result = handler.handleBusinessRule(ex, request);
            assertErrorResponse(result, HttpStatus.FORBIDDEN, "Not allowed");
        }
    }

    @Nested
    @DisplayName("handlePasswordPolicy")
    class HandlePasswordPolicy {

        @Test
        @DisplayName("回傳 400 與 message、path")
        void returnsBadRequestWithMessageAndPath() {
            PasswordPolicyException ex = new PasswordPolicyException("Password too weak");
            ResponseEntity<ErrorResponse> result = handler.handlePasswordPolicy(ex, request);
            assertErrorResponse(result, HttpStatus.BAD_REQUEST, "Password too weak");
        }
    }

    @Nested
    @DisplayName("handleAccountLocked")
    class HandleAccountLocked {

        @Test
        @DisplayName("回傳 423 LOCKED 與 message、path")
        void returnsLockedWithMessageAndPath() {
            AccountLockedException ex = new AccountLockedException(
                    "Account locked until tomorrow",
                    LocalDateTime.now().plusDays(1)
            );
            ResponseEntity<ErrorResponse> result = handler.handleAccountLocked(ex, request);
            assertErrorResponse(result, HttpStatus.LOCKED, "Account locked until tomorrow");
        }
    }

    @Nested
    @DisplayName("handleAccountDisabled")
    class HandleAccountDisabled {

        @Test
        @DisplayName("回傳 403 與 message、path")
        void returnsForbiddenWithMessageAndPath() {
            AccountDisabledException ex = new AccountDisabledException("Account is disabled");
            ResponseEntity<ErrorResponse> result = handler.handleAccountDisabled(ex, request);
            assertErrorResponse(result, HttpStatus.FORBIDDEN, "Account is disabled");
        }
    }

    @Nested
    @DisplayName("handleValidation (MethodArgumentNotValidException)")
    class HandleValidation {

        @Test
        @DisplayName("單一 FieldError 時 message 為 field: defaultMessage")
        void singleFieldErrorFormatsMessage() {
            MethodArgumentNotValidException ex = createValidationException(
                    List.of(new FieldError("user", "name", "must not be null"))
            );
            ResponseEntity<ErrorResponse> result = handler.handleValidation(ex, request);
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().message()).isEqualTo("name: must not be null");
            assertThat(result.getBody().path()).isEqualTo(REQUEST_URI);
        }

        @Test
        @DisplayName("多個 FieldError 時 message 以分號串接")
        void multipleFieldErrorsJoinedWithSemicolon() {
            MethodArgumentNotValidException ex = createValidationException(
                    List.of(
                            new FieldError("user", "name", "must not be null"),
                            new FieldError("user", "email", "invalid format")
                    )
            );
            ResponseEntity<ErrorResponse> result = handler.handleValidation(ex, request);
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().message())
                    .isEqualTo("name: must not be null; email: invalid format");
            assertThat(result.getBody().path()).isEqualTo(REQUEST_URI);
        }

        private MethodArgumentNotValidException createValidationException(List<FieldError> fieldErrors) {
            BindingResult bindingResult = mock(BindingResult.class);
            when(bindingResult.getFieldErrors()).thenReturn(fieldErrors);
            MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
            when(ex.getBindingResult()).thenReturn(bindingResult);
            return ex;
        }
    }

    @Nested
    @DisplayName("handleOptimisticLock")
    class HandleOptimisticLock {

        @Test
        @DisplayName("回傳 409 與固定訊息、path")
        void returnsConflictWithFixedMessageAndPath() {
            OptimisticLockingFailureException ex = new OptimisticLockingFailureException("version mismatch");
            ResponseEntity<ErrorResponse> result = handler.handleOptimisticLock(ex, request);
            assertErrorResponse(
                    result,
                    HttpStatus.CONFLICT,
                    "Resource was modified by another user. Please refresh and try again."
            );
        }
    }

    @Nested
    @DisplayName("handleAccessDenied")
    class HandleAccessDenied {

        @Test
        @DisplayName("回傳 403 與 message、path")
        void returnsForbiddenWithMessageAndPath() {
            AccessDeniedException ex = new AccessDeniedException("Access denied");
            ResponseEntity<ErrorResponse> result = handler.handleAccessDenied(ex, request);
            assertErrorResponse(result, HttpStatus.FORBIDDEN, "Access denied");
        }
    }

    @Nested
    @DisplayName("handleGeneric (Exception)")
    class HandleGeneric {

        @Test
        @DisplayName("回傳 500 與固定訊息、path")
        void returnsInternalServerErrorWithFixedMessageAndPath() {
            Exception ex = new RuntimeException("Something broke");
            ResponseEntity<ErrorResponse> result = handler.handleGeneric(ex, request);
            assertErrorResponse(
                    result,
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "An unexpected error occurred."
            );
        }
    }
}

package org.example.hexorise.api;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ProblemDetail denied(Exception exception) { return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, exception.getMessage()); }
    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail invalidArgument(Exception exception) { return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage()); }
    @ExceptionHandler(org.example.hexorise.client.HighriseRejectedException.class)
    public ProblemDetail rejected(Exception exception) { return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, exception.getMessage()); }
    @ExceptionHandler({IllegalStateException.class, java.util.concurrent.TimeoutException.class, java.util.concurrent.RejectedExecutionException.class})
    public ProblemDetail unavailable(Exception exception) { return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, "Bot operation unavailable. Check connection status and try again."); }
    @ExceptionHandler({org.springframework.dao.OptimisticLockingFailureException.class, org.springframework.dao.DataIntegrityViolationException.class})
    public ProblemDetail conflict(Exception exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Settings changed concurrently. Reload and retry.");
    }
    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class, HandlerMethodValidationException.class})
    public ProblemDetail invalid(Exception exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Invalid request. Check field lengths and allowed values.");
    }
}

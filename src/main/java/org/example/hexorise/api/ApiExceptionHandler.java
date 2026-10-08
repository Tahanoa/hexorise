package org.example.hexorise.api;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler({org.springframework.dao.OptimisticLockingFailureException.class, org.springframework.dao.DataIntegrityViolationException.class})
    public ProblemDetail conflict(Exception exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Settings changed concurrently. Reload and retry.");
    }
    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class, HandlerMethodValidationException.class})
    public ProblemDetail invalid(Exception exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Invalid request. Check field lengths and allowed values.");
    }
}

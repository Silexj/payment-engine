package com.github.Silexj.payment_engine.controller;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Обрабатывает ошибки бизнес-логики (например, попытка перевода самому себе,
     * недостаточный баланс или неверный формат данных).
     * Преобразует Java-исключение в стандартный JSON ответ со статусом 400 Bad Request.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
        problem.setTitle("Invalid Request");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    /**
     * Обрабатывает случаи, когда запрашиваемый ресурс (Account, Transaction) не найден в БД.
     * Возвращает статус 404 Not Found.
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ProblemDetail handleNotFound(EntityNotFoundException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
        problem.setTitle("Resource not found");
        return problem;
    }

    /**
     * Перехватывает ошибки валидации входных DTO (аннотации @NotNull, @Positive и др.),
     * возникающие при обработке аннотации @Valid в контроллерах.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationErrors(MethodArgumentNotValidException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed");
        problem.setTitle("Validation Error");

        var errors = e.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .toList();

        problem.setProperty("errors", errors);
        return problem;
    }
}

package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import ru.yandex.practicum.filmorate.exception.ErrorResponse;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class ErrorHandler {

    @ExceptionHandler(ValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidation(final ValidationException e) {
        return new ErrorResponse("Ошибка валидации", e.getMessage());
    }

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFound(final NotFoundException e) {
        return new ErrorResponse("Объект не найден", e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleException(final Exception e) {
        log.error("Непредвиденная ошибка", e);
        return new ErrorResponse("Внутренняя ошибка сервера", "Произошла непредвиденная ошибка.");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleMethodArgumentNotValid(final MethodArgumentNotValidException e) {
        List<FieldError> fieldErrors = e.getBindingResult().getFieldErrors();
        StringBuilder description = new StringBuilder();

        for (FieldError fieldError : fieldErrors) {
            if (!description.isEmpty()) {
                description.append("; ");
            }

            description.append(fieldError.getField())
                    .append(": ")
                    .append(fieldError.getDefaultMessage());
        }
        return new ErrorResponse("Ошибка валидации", description.toString());
    }

    /**
     * Обрабатывает нарушения аннотаций валидации (например, {@code @Positive}) на параметрах,
     * переданных через {@code @RequestParam} или {@code @PathVariable}. Такие нарушения Spring
     * выбрасывает как ConstraintViolationException, а не MethodArgumentNotValidException,
     * поэтому им нужен отдельный обработчик — иначе они попадают в общий {@link #handleException}
     * и возвращаются как 500 вместо 400.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleConstraintViolation(final ConstraintViolationException e) {
        String description = e.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .collect(Collectors.joining("; "));

        return new ErrorResponse("Ошибка валидации", description);
    }

    /**
     * Обрабатывает отсутствие обязательного {@code @RequestParam}. Spring выбрасывает эту ошибку
     * до вызова метода контроллера, но без отдельного обработчика она попадает в общий
     * {@link #handleException} и возвращается как 500 вместо 400.
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleMissingServletRequestParameter(final MissingServletRequestParameterException e) {
        return new ErrorResponse("Ошибка валидации",
                "Обязательный параметр '" + e.getParameterName() + "' не указан");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleMethodArgumentTypeMismatch(final MethodArgumentTypeMismatchException e) {
        return new ErrorResponse("Ошибка валидации",
                "Параметр '" + e.getName() + "' имеет недопустимое значение: " + e.getValue());
    }
}

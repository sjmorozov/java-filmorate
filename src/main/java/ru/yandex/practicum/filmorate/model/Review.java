package ru.yandex.practicum.filmorate.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Review {
    private static final int MAX_CONTENT_LENGTH = 10_000;

    private Long reviewId;

    @NotBlank(message = "Отзыв не может быть пустым")
    @Size(max = MAX_CONTENT_LENGTH, message = "Максимальная длина ревью — 10 000 символов")
    private String content;

    @NotNull
    private Boolean isPositive;

    @NotNull
    @Positive(message = "Id пользователя должен быть положительным")
    private Long userId;

    @NotNull
    @Positive(message = "Id фильма должен быть положительным")
    private Long filmId;

    private Integer useful;
}

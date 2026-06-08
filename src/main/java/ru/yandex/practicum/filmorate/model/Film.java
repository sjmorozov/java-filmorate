package ru.yandex.practicum.filmorate.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Film {
    private Integer id;

    private static final int MAX_DESCRIPTION_LENGTH = 200;

    @NotBlank(message = "Название должно быть указано")
    private String name;

    @Size(max = MAX_DESCRIPTION_LENGTH, message = "Максимальная длина описания — 200 символов")
    private String description;

    @NotNull(message = "Дата релиза должна быть указана")
    private LocalDate releaseDate;

    @NotNull(message = "Продолжительность фильма должна быть положительным числом")
    @Positive(message = "Продолжительность фильма должна быть положительным числом")
    private Integer duration;
}

package ru.yandex.practicum.filmorate.model;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Film {
    private static final int MAX_DESCRIPTION_LENGTH = 200;

    private Long id;

    @NotBlank(message = "Название должно быть указано")
    private String name;

    @Size(max = MAX_DESCRIPTION_LENGTH, message = "Максимальная длина описания — 200 символов")
    private String description;

    @NotNull(message = "Дата релиза должна быть указана")
    private LocalDate releaseDate;

    @NotNull(message = "Продолжительность фильма должна быть указана")
    @Positive(message = "Продолжительность фильма должна быть положительным числом")
    private Integer duration;

    @Builder.Default
    @JsonSetter(nulls = Nulls.AS_EMPTY)
    private Set<Long> likes = new HashSet<>();

    @Builder.Default
    @JsonSetter(nulls = Nulls.AS_EMPTY)
    private Set<Genre> genres = new HashSet<>();

    private MpaRating mpa;
}

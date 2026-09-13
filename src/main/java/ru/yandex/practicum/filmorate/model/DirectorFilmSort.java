package ru.yandex.practicum.filmorate.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DirectorFilmSort {
    YEAR("year"),
    LIKES("likes");

    private final String requestValue;
}

package ru.yandex.practicum.filmorate.storage.filmgenre;

import ru.yandex.practicum.filmorate.model.Genre;

import java.util.Set;

public interface FilmGenreStorage {
    void replaceByFilmId(Long filmId, Set<Genre> genres);

    Set<Genre> findByFilmId(Long filmId);
}

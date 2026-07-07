package ru.yandex.practicum.filmorate.storage.filmgenre;

import ru.yandex.practicum.filmorate.model.Genre;

import java.util.Set;

public interface FilmGenreStorage {
    void replaceFilmGenres(Long filmId, Set<Genre> genres);

    Set<Genre> findGenresByFilmId(Long filmId);
}

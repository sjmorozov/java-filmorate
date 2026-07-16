package ru.yandex.practicum.filmorate.storage.filmgenre;

import ru.yandex.practicum.filmorate.model.Genre;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public interface FilmGenreStorage {
    void replaceByFilmId(Long filmId, Set<Genre> genres);

    Set<Genre> findByFilmId(Long filmId);

    Map<Long, Set<Genre>> findByFilmIds(Collection<Long> filmIds);
}

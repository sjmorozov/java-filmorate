package ru.yandex.practicum.filmorate.storage.filmdirector;

import ru.yandex.practicum.filmorate.model.Director;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public interface FilmDirectorStorage {
    void replaceByFilmId(Long filmId, Set<Director> directors);

    Set<Director> findByFilmId(Long filmId);

    Map<Long, Set<Director>> findByFilmIds(Collection<Long> filmIds);
}

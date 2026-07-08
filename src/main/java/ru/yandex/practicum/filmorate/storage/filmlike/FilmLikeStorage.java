package ru.yandex.practicum.filmorate.storage.filmlike;

import java.util.Set;

public interface FilmLikeStorage {
    void add(Long filmId, Long userId);

    void delete(Long filmId, Long userId);

    Set<Long> findUserIdsByFilmId(Long filmId);
}

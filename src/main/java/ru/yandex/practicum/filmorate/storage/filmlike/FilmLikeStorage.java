package ru.yandex.practicum.filmorate.storage.filmlike;

import java.util.Set;

public interface FilmLikeStorage {
    void addLike(Long filmId, Long userId);

    void deleteLike(Long filmId, Long userId);

    Set<Long> findUserIdsByFilmId(Long filmId);
}

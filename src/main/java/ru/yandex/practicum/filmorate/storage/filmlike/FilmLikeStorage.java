package ru.yandex.practicum.filmorate.storage.filmlike;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface FilmLikeStorage {
    void add(Long filmId, Long userId);

    void delete(Long filmId, Long userId);

    Set<Long> findUserIdsByFilmId(Long filmId);

    Map<Long, Set<Long>> findUserIdsByFilmIds(Collection<Long> filmIds);

    List<Long> findPopularFilmIds(int count);
}

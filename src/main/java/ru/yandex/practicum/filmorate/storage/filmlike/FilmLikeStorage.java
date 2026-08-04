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

    /**
     * Возвращает id топ-N фильмов по количеству лайков, отсортированные по убыванию популярности.
     *
     * @param genreId если не null, учитываются только фильмы этого жанра
     * @param year    если не null, учитываются только фильмы с этим годом релиза
     */
    List<Long> findPopularFilmIds(int count, Integer genreId, Integer year);

    Set<Long> findFilmIdsByUserId(Long userId);

    Map<Long, Set<Long>> findAllFilmIdsGroupedByUser();

    List<Long> findCommonFilmIdsSortedByPopularity(Long userId, Long friendId);

    /**
     * Ищет фильмы по подстроке в названии и/или в имени режиссёра, отсортированные по популярности.
     *
     * @param query         текст для поиска (регистронезависимо, по подстроке)
     * @param searchByTitle искать по названию фильма
     * @param searchByDirector искать по имени режиссёра
     */
    List<Long> findSearchFilmIds(String query, boolean searchByTitle, boolean searchByDirector);
}

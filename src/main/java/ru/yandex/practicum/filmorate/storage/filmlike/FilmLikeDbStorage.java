package ru.yandex.practicum.filmorate.storage.filmlike;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;

import java.time.LocalDate;
import java.util.*;

@Slf4j
@RequiredArgsConstructor
@Component
public class FilmLikeDbStorage implements FilmLikeStorage {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void add(Long filmId, Long userId) {
        String sql = """
                MERGE INTO film_likes (film_id, user_id)
                KEY (film_id, user_id)
                VALUES (?, ?)
                """;

        jdbcTemplate.update(
                sql,
                filmId,
                userId
        );
    }

    @Override
    public void delete(Long filmId, Long userId) {
        String sql = """
                DELETE FROM film_likes
                WHERE film_id = ?
                AND user_id = ?
                """;

        int rowsAffected = jdbcTemplate.update(
                sql,
                filmId,
                userId
        );

        if (rowsAffected == 0) {
            throw new NotFoundException("Лайк пользователя с id = " + userId + " для фильма с id = " + filmId + " не найден");
        }
    }

    @Override
    public Set<Long> findUserIdsByFilmId(Long filmId) {
        String sql = """
                SELECT user_id
                FROM film_likes
                WHERE film_id = ?
                """;

        List<Long> userIdsList = jdbcTemplate.queryForList(sql, Long.class, filmId);

        return new HashSet<>(userIdsList);
    }

    @Override
    public Map<Long, Set<Long>> findUserIdsByFilmIds(Collection<Long> filmIds) {
        if (filmIds == null || filmIds.isEmpty()) {
            return Map.of();
        }

        String sql = """
                SELECT film_id, user_id
                FROM film_likes
                WHERE film_id IN (:filmIds)
                ORDER BY film_id, user_id
                """;

        Map<Long, Set<Long>> userIdsByFilmId = new LinkedHashMap<>();
        NamedParameterJdbcTemplate namedJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
        MapSqlParameterSource parameters = new MapSqlParameterSource("filmIds", filmIds);

        namedJdbcTemplate.query(sql, parameters, rs -> {
            Long filmId = rs.getLong("film_id");
            Long userId = rs.getLong("user_id");
            userIdsByFilmId.computeIfAbsent(filmId, id -> new LinkedHashSet<>())
                    .add(userId);
        });

        return userIdsByFilmId;
    }

    @Override
    public List<Long> findPopularFilmIds(int count, Integer genreId, Integer year) {
        String sql = """
                SELECT f.id
                FROM films AS f
                LEFT JOIN film_likes AS l ON f.id = l.film_id
                WHERE (:genreId IS NULL OR EXISTS (
                        SELECT 1 FROM film_genres AS fg
                        WHERE fg.film_id = f.id AND fg.genre_id = :genreId
                      ))
                  AND (:year IS NULL OR (f.release_date >= :yearStart AND f.release_date < :yearEnd))
                GROUP BY f.id
                ORDER BY COUNT(l.user_id) DESC, f.id
                LIMIT :count
                """;

        LocalDate yearStart = year != null ? LocalDate.of(year, 1, 1) : null;
        LocalDate yearEnd = year != null ? LocalDate.of(year + 1, 1, 1) : null;

        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("genreId", genreId)
                .addValue("year", year)
                .addValue("yearStart", yearStart)
                .addValue("yearEnd", yearEnd)
                .addValue("count", count);

        return new NamedParameterJdbcTemplate(jdbcTemplate).queryForList(sql, parameters, Long.class);
    }

    @Override
    public Set<Long> findFilmIdsByUserId(Long userId) {
        String sql = "SELECT film_id FROM film_likes WHERE user_id = ?";
        List<Long> filmIds = jdbcTemplate.queryForList(sql, Long.class, userId);
        return new HashSet<>(filmIds);
    }

    @Override
    public Map<Long, Set<Long>> findAllFilmIdsGroupedByUser() {
        String sql = "SELECT user_id, film_id FROM film_likes";
        Map<Long, Set<Long>> filmsByUser = new HashMap<>();
        jdbcTemplate.query(sql, rs -> {
            Long userId = rs.getLong("user_id");
            Long filmId = rs.getLong("film_id");
            filmsByUser.computeIfAbsent(userId, k -> new HashSet<>()).add(filmId);
        });
        return filmsByUser;
    }

    @Override
    public List<Long> findCommonFilmIdsSortedByPopularity(Long userId, Long friendId) {
        String sql = """
                SELECT common.film_id
                FROM (
                    SELECT film_id
                    FROM film_likes
                    WHERE user_id IN (?, ?)
                    GROUP BY film_id
                    HAVING COUNT(DISTINCT user_id) = 2
                ) AS common
                LEFT JOIN film_likes AS all_likes ON common.film_id = all_likes.film_id
                GROUP BY common.film_id
                ORDER BY COUNT(all_likes.user_id) DESC, common.film_id ASC
                """;

        return jdbcTemplate.queryForList(sql, Long.class, userId, friendId);
    }

    @Override
    public List<Long> findSearchFilmIds(String query, boolean searchByTitle, boolean searchByDirector) {
        String sql = """
                SELECT f.id
                FROM films AS f
                LEFT JOIN film_likes AS l ON f.id = l.film_id
                WHERE (:searchByTitle = TRUE AND LOWER(f.name) LIKE LOWER(:pattern) ESCAPE '\\')
                   OR (:searchByDirector = TRUE AND EXISTS (
                        SELECT 1 FROM film_directors AS fd
                        JOIN directors AS d ON fd.director_id = d.id
                        WHERE fd.film_id = f.id AND LOWER(d.name) LIKE LOWER(:pattern) ESCAPE '\\'
                      ))
                GROUP BY f.id
                ORDER BY COUNT(l.user_id) DESC, f.id
                """;

        String pattern = "%" + escapeLikePattern(query) + "%";

        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("pattern", pattern)
                .addValue("searchByTitle", searchByTitle)
                .addValue("searchByDirector", searchByDirector);

        return new NamedParameterJdbcTemplate(jdbcTemplate).queryForList(sql, parameters, Long.class);
    }

    private String escapeLikePattern(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}

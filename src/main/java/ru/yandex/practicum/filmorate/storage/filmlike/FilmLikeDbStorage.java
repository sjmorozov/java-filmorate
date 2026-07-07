package ru.yandex.practicum.filmorate.storage.filmlike;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@RequiredArgsConstructor
@Component
public class FilmLikeDbStorage implements FilmLikeStorage {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void addLike(Long filmId, Long userId) {
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
    public void deleteLike(Long filmId, Long userId) {
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
            throw new NotFoundException
                    ("Лайк пользователя с id = " + userId + " для фильма с id = " + filmId + " не найден");
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
}

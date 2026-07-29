package ru.yandex.practicum.filmorate.storage.review;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Review;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Component
public class ReviewDbStorage implements ReviewStorage {
    private static final String USEFULNESS_SQL_EXPRESSION = """
            COALESCE(SUM(CASE
                WHEN rr.is_like = TRUE THEN 1
                WHEN rr.is_like = FALSE THEN -1
                ELSE 0
            END), 0)
            """.strip();

    private final JdbcTemplate jdbcTemplate;

    @Override
    public Review add(Review review) {
        String sql = """
                INSERT INTO reviews (content, is_positive, user_id, film_id)
                VALUES (?, ?, ?, ?)
                """;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

            if (review.getContent() == null) {
                statement.setNull(1, Types.VARCHAR);
            } else {
                statement.setString(1, review.getContent());
            }

            if (review.getIsPositive() == null) {
                statement.setNull(2, Types.BOOLEAN);
            } else {
                statement.setBoolean(2, review.getIsPositive());
            }

            if (review.getUserId() == null) {
                statement.setNull(3, Types.BIGINT);
            } else {
                statement.setLong(3, review.getUserId());
            }

            if (review.getFilmId() == null) {
                statement.setNull(4, Types.BIGINT);
            } else {
                statement.setLong(4, review.getFilmId());
            }

            return statement;
        }, keyHolder);

        Number generatedId = keyHolder.getKey();

        if (generatedId == null) {
            throw new IllegalStateException("Не удалось получить id созданного ревью");
        }

        Long reviewId = generatedId.longValue();
        review.setReviewId(reviewId);

        review.setUseful(0);

        return review;
    }

    @Override
    public Review update(Review review) {
        String sql = """
                UPDATE reviews
                SET content = ?, is_positive = ?
                WHERE id = ?
                """;

        Long reviewId = review.getReviewId();

        int rowsAffected = jdbcTemplate.update(
                sql,
                review.getContent(),
                review.getIsPositive(),
                reviewId
        );

        if (rowsAffected == 0) {
            throw new NotFoundException("Ревью с id = " + reviewId + " не найдено");
        }

        return findById(reviewId);
    }

    @Override
    public void delete(Long reviewId) {
        String sql = """
                DELETE FROM reviews
                WHERE id = ?
                """;

        int rowsAffected = jdbcTemplate.update(
                sql,
                reviewId
        );

        if (rowsAffected == 0) {
            throw new NotFoundException("Ревью с id = " + reviewId + " не найдено");
        }
    }

    @Override
    public Review findById(Long id) {
        String sql = """
                SELECT  r.id,
                        r.content,
                        r.is_positive,
                        r.user_id,
                        r.film_id,
                        %s AS useful
                FROM reviews AS r
                LEFT JOIN review_reactions rr ON r.id = rr.review_id
                WHERE r.id = ?
                GROUP BY r.id
                """.formatted(USEFULNESS_SQL_EXPRESSION);

        try {
            return jdbcTemplate.queryForObject(sql, this::mapRowToReview, id);
        } catch (EmptyResultDataAccessException e) {
            throw new NotFoundException("Ревью с id = " + id + " не найдено");
        }
    }

    @Override
    public List<Review> findMostUseful(Long filmId, int count) {
        String sql = """
            SELECT r.id,
                   r.content,
                   r.is_positive,
                   r.user_id,
                   r.film_id,
                   %s AS useful
            FROM reviews r
            LEFT JOIN review_reactions rr ON r.id = rr.review_id
            WHERE r.film_id = COALESCE(?, r.film_id)
            GROUP BY r.id
            ORDER BY useful DESC, r.id
            LIMIT ?
            """.formatted(USEFULNESS_SQL_EXPRESSION);

        return jdbcTemplate.query(sql, this::mapRowToReview, filmId, count);
    }

    private Review mapRowToReview(ResultSet rs, int rowNum) throws SQLException {
        Review review = new Review();

        review.setReviewId(rs.getLong("id"));
        review.setContent(rs.getString("content"));
        review.setIsPositive(rs.getBoolean("is_positive"));
        review.setUserId(rs.getLong("user_id"));
        review.setFilmId(rs.getLong("film_id"));
        review.setUseful(rs.getInt("useful"));

        return review;
    }
}

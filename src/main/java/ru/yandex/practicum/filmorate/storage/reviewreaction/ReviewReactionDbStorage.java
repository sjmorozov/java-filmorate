package ru.yandex.practicum.filmorate.storage.reviewreaction;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;

@RequiredArgsConstructor
@Component
public class ReviewReactionDbStorage implements ReviewReactionStorage {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void save(Long reviewId, Long userId, boolean isLike) {
        String sql = """
                    MERGE INTO review_reactions (review_id, user_id, is_like)
                    KEY (review_id, user_id)
                    VALUES (?, ?, ?)
                """;

        jdbcTemplate.update(sql, reviewId, userId, isLike);
    }

    @Override
    public void delete(Long reviewId, Long userId, boolean isLike) {
        String sql = """
                    DELETE FROM review_reactions
                    WHERE review_id = ?
                        AND user_id = ?
                        AND is_like = ?
                """;

        int rowsAffected = jdbcTemplate.update(sql, reviewId, userId, isLike);

        if (rowsAffected == 0) {
            throw new NotFoundException("Реакция пользователя с id = " + userId + " на ревью с id = " + reviewId + " не найдена");
        }
    }
}

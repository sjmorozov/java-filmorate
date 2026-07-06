package ru.yandex.practicum.filmorate.storage.friendship;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Friendship;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class FriendshipDbStorage implements FriendshipStorage {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void save(Friendship friendship) {
        Objects.requireNonNull(friendship, "Дружба должна быть указана");

        FriendshipKey key = new FriendshipKey(friendship.getFirstUserId(), friendship.getSecondUserId());

        String sql = """
                MERGE INTO friendships (first_id, second_id)
                KEY (first_id, second_id)
                VALUES (?, ?)
                """;

        jdbcTemplate.update(
                sql,
                key.firstUserId(),
                key.secondUserId()
        );
    }

    @Override
    public Optional<Friendship> findByUserIds(Long firstUserId, Long secondUserId) {
        FriendshipKey key = new FriendshipKey(firstUserId, secondUserId);

        String sql = """
                SELECT first_id, second_id
                FROM friendships
                WHERE first_id = ?
                AND second_id = ?
                """;

        List<Friendship> friendships = jdbcTemplate.query(
                sql,
                this::mapRowToFriendship,
                key.firstUserId(),
                key.secondUserId()
        );

        if (friendships.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(friendships.getFirst());
    }

    @Override
    public void deleteByUserIds(Long firstUserId, Long secondUserId) {
        FriendshipKey key = new FriendshipKey(firstUserId, secondUserId);

        String sql = """
                DELETE FROM friendships
                WHERE first_id = ?
                AND second_id = ?
                """;

        int rowsAffected = jdbcTemplate.update(
                sql,
                key.firstUserId(),
                key.secondUserId()
        );

        if (rowsAffected == 0) {
            throw new NotFoundException("Дружба между пользователями не найдена");
        }
    }

    @Override
    public void deleteAllByUserId(Long userId) {
        Objects.requireNonNull(userId, "Id пользователя должен быть указан");

        String sql = """
                DELETE FROM friendships
                WHERE first_id = ?
                OR second_id = ?
                """;

        jdbcTemplate.update(
                sql,
                userId,
                userId
        );
    }

    @Override
    public boolean existsByUserIds(Long firstUserId, Long secondUserId) {
        FriendshipKey key = new FriendshipKey(firstUserId, secondUserId);

        String sql = """
                SELECT COUNT(*)
                FROM friendships
                WHERE first_id = ?
                AND second_id = ?
                """;

        Integer count = jdbcTemplate.queryForObject(
                sql,
                Integer.class,
                key.firstUserId(),
                key.secondUserId()
        );
        return count != null && count > 0;
    }

    @Override
    public Set<Long> findFriendIdsByUserId(Long userId) {
        Objects.requireNonNull(userId, "Id пользователя должен быть указан");

        String sql = """
                SELECT second_id
                FROM friendships
                WHERE first_id = ?
                
                UNION
                
                SELECT first_id
                FROM friendships
                WHERE second_id = ?
                """;

        return Set.copyOf(jdbcTemplate.queryForList(sql, Long.class, userId, userId));
    }

    private Friendship mapRowToFriendship(ResultSet rs, int rowNum) throws SQLException {
        Long firstUserId = rs.getLong("first_id");
        Long secondUserId = rs.getLong("second_id");

        return new Friendship(firstUserId, secondUserId);
    }
}

package ru.yandex.practicum.filmorate.storage.friendrequest;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.FriendRequest;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class FriendRequestDbStorage implements FriendRequestStorage {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void save(FriendRequest friendRequest) {
        Objects.requireNonNull(friendRequest, "Заявка в друзья должна быть указана");

        Long requesterId = friendRequest.getRequesterId();
        Long recipientId = friendRequest.getRecipientId();

        String sql = """
                MERGE INTO friend_requests (requester_id, recipient_id)
                KEY (requester_id, recipient_id)
                VALUES (?, ?)
                """;

        jdbcTemplate.update(
                sql,
                requesterId,
                recipientId
        );
    }

    @Override
    public Optional<FriendRequest> findByRequesterIdAndRecipientId(Long requesterId, Long recipientId) {
        String sql = """
                SELECT requester_id, recipient_id
                FROM friend_requests
                WHERE requester_id = ?
                AND recipient_id = ?
                """;

        List<FriendRequest> friendRequests = jdbcTemplate.query(
                sql,
                this::mapRowToFriendRequest,
                requesterId,
                recipientId
        );

        if (friendRequests.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(friendRequests.getFirst());
    }

    @Override
    public void deleteByRequesterIdAndRecipientId(Long requesterId, Long recipientId) {
        int rowsAffected = deleteFriendRequest(requesterId, recipientId);

        if (rowsAffected == 0) {
            throw new NotFoundException("Заявка в друзья не найдена");
        }
    }

    @Override
    public boolean deleteIfExistsByRequesterIdAndRecipientId(Long requesterId, Long recipientId) {
        return deleteFriendRequest(requesterId, recipientId) > 0;
    }

    @Override
    public void deleteAllByUserId(Long userId) {
        String sql = """
                DELETE FROM friend_requests
                WHERE requester_id = ?
                OR recipient_id = ?
                """;

        jdbcTemplate.update(
                sql,
                userId,
                userId
        );
    }

    @Override
    public boolean existsByRequesterIdAndRecipientId(Long requesterId, Long recipientId) {
        String sql = """
                SELECT COUNT(*)
                FROM friend_requests
                WHERE requester_id = ?
                  AND recipient_id = ?
                """;

        Integer count = jdbcTemplate.queryForObject(
                sql,
                Integer.class,
                requesterId,
                recipientId
        );
        return count != null && count > 0;
    }

    @Override
    public Set<Long> findRecipientIdsByRequesterId(Long requesterId) {
        Objects.requireNonNull(requesterId, "Id отправителя заявки должен быть указан");

        String sql = """
                SELECT recipient_id
                FROM friend_requests
                WHERE requester_id = ?
                """;

        return Set.copyOf(jdbcTemplate.queryForList(sql, Long.class, requesterId));
    }

    @Override
    public Set<Long> findRequesterIdsByRecipientId(Long recipientId) {
        Objects.requireNonNull(recipientId, "Id получателя заявки должен быть указан");

        String sql = """
                SELECT requester_id
                FROM friend_requests
                WHERE recipient_id = ?
                """;

        return Set.copyOf(jdbcTemplate.queryForList(sql, Long.class, recipientId));
    }

    private FriendRequest mapRowToFriendRequest(ResultSet rs, int rowNum) throws SQLException {
        Long requesterId = rs.getLong("requester_id");
        Long recipientId = rs.getLong("recipient_id");

        return new FriendRequest(requesterId, recipientId);
    }

    private int deleteFriendRequest(Long requesterId, Long recipientId) {
        String sql = """
                DELETE FROM friend_requests
                WHERE requester_id = ?
                AND recipient_id = ?
                """;

        return jdbcTemplate.update(
                sql,
                requesterId,
                recipientId
        );
    }
}

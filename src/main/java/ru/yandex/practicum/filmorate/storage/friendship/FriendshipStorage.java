package ru.yandex.practicum.filmorate.storage.friendship;

import ru.yandex.practicum.filmorate.model.Friendship;

import java.util.Optional;
import java.util.Set;

public interface FriendshipStorage {
    void save(Friendship friendship);

    Optional<Friendship> findByUserIds(Long firstUserId, Long secondUserId);

    void deleteByUserIds(Long firstUserId, Long secondUserId);

    boolean existsByUserIds(Long firstUserId, Long secondUserId);

    Set<Long> findFriendIdsByUserId(Long userId);
}

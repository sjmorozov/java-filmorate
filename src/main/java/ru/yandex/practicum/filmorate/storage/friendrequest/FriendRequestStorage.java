package ru.yandex.practicum.filmorate.storage.friendrequest;

import ru.yandex.practicum.filmorate.model.FriendRequest;

import java.util.Optional;
import java.util.Set;

public interface FriendRequestStorage {
    void save(FriendRequest friendRequest);

    Optional<FriendRequest> findByRequesterIdAndRecipientId(Long requesterId, Long recipientId);

    void deleteByRequesterIdAndRecipientId(Long requesterId, Long recipientId);

    boolean deleteIfExistsByRequesterIdAndRecipientId(Long requesterId, Long recipientId);

    void deleteAllByUserId(Long userId);

    boolean existsByRequesterIdAndRecipientId(Long requesterId, Long recipientId);

    Set<Long> findRecipientIdsByRequesterId(Long requesterId);

    Set<Long> findRequesterIdsByRecipientId(Long recipientId);
}

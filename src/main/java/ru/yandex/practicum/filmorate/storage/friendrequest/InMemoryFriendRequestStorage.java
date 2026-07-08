package ru.yandex.practicum.filmorate.storage.friendrequest;

import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.FriendRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class InMemoryFriendRequestStorage implements FriendRequestStorage {
    private final Map<FriendRequestKey, FriendRequest> friendRequests = new HashMap<>();

    @Override
    public void save(FriendRequest friendRequest) {
        Objects.requireNonNull(friendRequest, "Заявка в друзья должна быть указана");

        FriendRequestKey key = new FriendRequestKey(friendRequest.getRequesterId(), friendRequest.getRecipientId());
        friendRequests.put(key, friendRequest);
    }

    @Override
    public Optional<FriendRequest> findByRequesterIdAndRecipientId(Long requesterId, Long recipientId) {
        FriendRequestKey key = new FriendRequestKey(requesterId, recipientId);

        return Optional.ofNullable(friendRequests.get(key));
    }

    @Override
    public void deleteByRequesterIdAndRecipientId(Long requesterId, Long recipientId) {
        FriendRequestKey key = new FriendRequestKey(requesterId, recipientId);
        FriendRequest removedFriendRequest = friendRequests.remove(key);

        if (removedFriendRequest == null) {
            throw new NotFoundException("Заявка в друзья не найдена");
        }
    }

    @Override
    public void deleteAllByUserId(Long userId) {
        Objects.requireNonNull(userId, "Id пользователя должен быть указан");

        friendRequests.values().removeIf(friendRequest -> friendRequest.getRequesterId().equals(userId)
                || friendRequest.getRecipientId().equals(userId));
    }

    @Override
    public boolean existsByRequesterIdAndRecipientId(Long requesterId, Long recipientId) {
        return friendRequests.containsKey(new FriendRequestKey(requesterId, recipientId));
    }

    @Override
    public Set<Long> findRecipientIdsByRequesterId(Long requesterId) {
        Objects.requireNonNull(requesterId, "Id отправителя заявки должен быть указан");

        return friendRequests.values().stream()
                .filter(friendRequest -> friendRequest.getRequesterId().equals(requesterId))
                .map(FriendRequest::getRecipientId)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<Long> findRequesterIdsByRecipientId(Long recipientId) {
        Objects.requireNonNull(recipientId, "Id получателя заявки должен быть указан");

        return friendRequests.values().stream()
                .filter(friendRequest -> friendRequest.getRecipientId().equals(recipientId))
                .map(FriendRequest::getRequesterId)
                .collect(Collectors.toSet());
    }
}

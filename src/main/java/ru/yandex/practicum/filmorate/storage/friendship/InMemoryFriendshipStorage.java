package ru.yandex.practicum.filmorate.storage.friendship;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Friendship;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class InMemoryFriendshipStorage implements FriendshipStorage {
    private final Map<FriendshipKey, Friendship> friendships = new HashMap<>();

    @Override
    public void save(Friendship friendship) {
        Objects.requireNonNull(friendship, "Дружба должна быть указана");

        FriendshipKey key = new FriendshipKey(friendship.getFirstUserId(), friendship.getSecondUserId());
        Friendship normalizedFriendship = Friendship.builder()
                .firstUserId(key.firstUserId())
                .secondUserId(key.secondUserId())
                .build();

        friendships.put(key, normalizedFriendship);
    }

    @Override
    public Optional<Friendship> findByUserIds(Long firstUserId, Long secondUserId) {
        FriendshipKey key = new FriendshipKey(firstUserId, secondUserId);

        return Optional.ofNullable(friendships.get(key));
    }

    @Override
    public void deleteByUserIds(Long firstUserId, Long secondUserId) {
        FriendshipKey key = new FriendshipKey(firstUserId, secondUserId);
        Friendship removedFriendship = friendships.remove(key);

        if (removedFriendship == null) {
            throw new NotFoundException("Дружба между пользователями не найдена");
        }
    }

    @Override
    public void deleteAllByUserId(Long userId) {
        Objects.requireNonNull(userId, "Id пользователя должен быть указан");

        friendships.values().removeIf(friendship -> friendship.getFirstUserId().equals(userId)
                || friendship.getSecondUserId().equals(userId));
    }

    @Override
    public boolean existsByUserIds(Long firstUserId, Long secondUserId) {
        return friendships.containsKey(new FriendshipKey(firstUserId, secondUserId));
    }

    @Override
    public Set<Long> findFriendIdsByUserId(Long userId) {
        Objects.requireNonNull(userId, "Id пользователя должен быть указан");

        return friendships.values().stream()
                .filter(friendship -> friendship.getFirstUserId().equals(userId)
                        || friendship.getSecondUserId().equals(userId))
                .map(friendship -> friendship.getFirstUserId().equals(userId)
                        ? friendship.getSecondUserId()
                        : friendship.getFirstUserId())
                .collect(Collectors.toSet());
    }
}

package ru.yandex.practicum.filmorate.storage.friendship;

import java.util.Objects;

record FriendshipKey(Long firstUserId, Long secondUserId) {
    FriendshipKey {
        Objects.requireNonNull(firstUserId, "Id первого пользователя должен быть указан");
        Objects.requireNonNull(secondUserId, "Id второго пользователя должен быть указан");

        if (firstUserId.equals(secondUserId)) {
            throw new IllegalArgumentException("Пользователь не может дружить сам с собой");
        }

        if (firstUserId.compareTo(secondUserId) > 0) {
            Long temp = firstUserId;
            firstUserId = secondUserId;
            secondUserId = temp;
        }
    }
}

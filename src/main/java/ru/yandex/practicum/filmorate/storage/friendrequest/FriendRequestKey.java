package ru.yandex.practicum.filmorate.storage.friendrequest;

import java.util.Objects;

record FriendRequestKey(Long requesterId, Long recipientId) {
    FriendRequestKey {
        Objects.requireNonNull(requesterId, "Id отправителя заявки должен быть указан");
        Objects.requireNonNull(recipientId, "Id получателя заявки должен быть указан");

        if (requesterId.equals(recipientId)) {
            throw new IllegalArgumentException("Пользователь не может отправить заявку самому себе");
        }
    }
}

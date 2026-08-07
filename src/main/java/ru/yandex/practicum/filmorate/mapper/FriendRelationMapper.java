package ru.yandex.practicum.filmorate.mapper;

import ru.yandex.practicum.filmorate.model.FriendRelationStatus;
import ru.yandex.practicum.filmorate.model.FriendRelationStatusResponse;
import ru.yandex.practicum.filmorate.model.FriendRequest;
import ru.yandex.practicum.filmorate.model.Friendship;
import ru.yandex.practicum.filmorate.model.User;

public final class FriendRelationMapper {

    private FriendRelationMapper() {
    }

    public static Friendship toFriendship(Long firstUserId, Long secondUserId) {
        return Friendship.builder()
                .firstUserId(firstUserId)
                .secondUserId(secondUserId)
                .build();
    }

    public static FriendRequest toFriendRequest(Long requesterId, Long recipientId) {
        return FriendRequest.builder()
                .requesterId(requesterId)
                .recipientId(recipientId)
                .build();
    }

    public static FriendRelationStatusResponse toStatusResponse(User firstUser,
                                                                User secondUser,
                                                                FriendRelationStatus status,
                                                                String description) {
        return FriendRelationStatusResponse.builder()
                .firstUserId(firstUser.getId())
                .firstUserName(firstUser.getName())
                .secondUserId(secondUser.getId())
                .secondUserName(secondUser.getName())
                .status(status)
                .description(description)
                .build();
    }
}

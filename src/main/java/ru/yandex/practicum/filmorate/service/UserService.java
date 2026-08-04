package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.*;
import ru.yandex.practicum.filmorate.storage.event.EventStorage;
import ru.yandex.practicum.filmorate.storage.friendrequest.FriendRequestStorage;
import ru.yandex.practicum.filmorate.storage.friendship.FriendshipStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserStorage userStorage;
    private final FriendRequestStorage friendRequestStorage;
    private final FriendshipStorage friendshipStorage;
    private final EventStorage eventStorage;

    public User create(User user) {
        normalizeUser(user);
        User createdUser = userStorage.add(user);
        log.info("Пользователь с id = {}, login = {} добавлен", createdUser.getId(), createdUser.getLogin());
        return createdUser;
    }

    public User update(User user) {
        validateId(user.getId());

        userStorage.findById(user.getId());
        normalizeUser(user);

        User updatedUser = userStorage.update(user);
        log.info("Профиль пользователя с id = {}, login = {} обновлён", updatedUser.getId(), updatedUser.getLogin());
        return updatedUser;
    }

    public void delete(Long id) {
        validateId(id);
        userStorage.findById(id);

        userStorage.delete(id);
        log.info("Пользователь с id = {} удалён", id);
    }

    public User findById(Long id) {
        validateId(id);
        return userStorage.findById(id);
    }

    public Collection<User> findAll() {
        return userStorage.findAll();
    }

    public void addFriend(Long userId, Long friendId) {
        FriendshipParticipants participants = getFriendshipParticipants(userId, friendId,
                "Пользователь не может добавить самого себя в друзья: id = " + userId + ".");
        User user = participants.user();
        User friend = participants.friend();

        if (friendshipStorage.existsByUserIds(userId, friendId)) {
            log.info("Пользователи {} и {} уже являются друзьями", user.getLogin(), friend.getLogin());
            return;
        }

        if (friendRequestStorage.deleteIfExistsByRequesterIdAndRecipientId(friendId, userId)) {
            friendshipStorage.save(Friendship.builder()
                    .firstUserId(userId)
                    .secondUserId(friendId)
                    .build());

            eventStorage.save(Event.builder()
                    .timestamp(System.currentTimeMillis())
                    .userId(userId)
                    .eventType(EventType.FRIEND)
                    .operation(Operation.ADD)
                    .entityId(friendId)
                    .build());

            log.info("Пользователь {} подтвердил дружбу с пользователем {}", user.getLogin(), friend.getLogin());
            return;
        }

        friendRequestStorage.save(FriendRequest.builder()
                .requesterId(userId)
                .recipientId(friendId)
                .build());

        eventStorage.save(Event.builder()
                .timestamp(System.currentTimeMillis())
                .userId(userId)
                .eventType(EventType.FRIEND)
                .operation(Operation.ADD)
                .entityId(friendId)
                .build());

        log.info("Пользователь {} отправил заявку в друзья пользователю {}", user.getLogin(), friend.getLogin());
    }

    public void removeFriend(Long userId, Long friendId) {
        FriendshipParticipants participants = getFriendshipParticipants(userId, friendId,
                "Пользователь не может удалить из друзей самого себя: id = " + userId + ".");
        User user = participants.user();
        User friend = participants.friend();

        if (friendshipStorage.existsByUserIds(userId, friendId)) {
            friendshipStorage.deleteByUserIds(userId, friendId);

            friendRequestStorage.save(FriendRequest.builder()
                    .requesterId(friendId)
                    .recipientId(userId)
                    .build());

            eventStorage.save(Event.builder()
                    .timestamp(System.currentTimeMillis())
                    .userId(userId)
                    .eventType(EventType.FRIEND)
                    .operation(Operation.REMOVE)
                    .entityId(friendId)
                    .build());

            log.info("Пользователь {} удалил пользователя {} из друзей", user.getLogin(), friend.getLogin());
            return;
        }

        if (friendRequestStorage.existsByRequesterIdAndRecipientId(userId, friendId)) {
            friendRequestStorage.deleteByRequesterIdAndRecipientId(userId, friendId);

            eventStorage.save(Event.builder()
                    .timestamp(System.currentTimeMillis())
                    .userId(userId)
                    .eventType(EventType.FRIEND)
                    .operation(Operation.REMOVE)
                    .entityId(friendId)
                    .build());

            log.info("Пользователь {} отменил заявку в друзья пользователю {}", user.getLogin(), friend.getLogin());
            return;
        }

        if (friendRequestStorage.existsByRequesterIdAndRecipientId(friendId, userId)) {
            friendRequestStorage.deleteByRequesterIdAndRecipientId(friendId, userId);

            eventStorage.save(Event.builder()
                    .timestamp(System.currentTimeMillis())
                    .userId(userId)
                    .eventType(EventType.FRIEND)
                    .operation(Operation.REMOVE)
                    .entityId(friendId)
                    .build());

            log.info("Пользователь {} удалил входящую заявку в друзья от пользователя {}", user.getLogin(), friend.getLogin());
            return;
        }

        log.info("Связь между пользователями {} и {} отсутствует", user.getLogin(), friend.getLogin());
    }

    public Set<User> findFriends(Long id) {
        validateId(id);
        userStorage.findById(id);

        return getFriendsByIds(findVisibleFriendIdsByUserId(id));
    }

    public Set<User> findCommonFriends(Long firstId, Long secondId) {
        validateId(firstId);
        validateId(secondId);

        userStorage.findById(firstId);
        userStorage.findById(secondId);

        Set<Long> firstSetIds = findVisibleFriendIdsByUserId(firstId);
        Set<Long> secondSetIds = findVisibleFriendIdsByUserId(secondId);

        Set<Long> commonFriends = firstSetIds.stream()
                .filter(secondSetIds::contains)
                .collect(Collectors.toSet());

        return getFriendsByIds(commonFriends);
    }

    public FriendRelationStatusResponse findRelationStatus(Long firstUserId, Long secondUserId) {
        FriendshipParticipants participants = getFriendshipParticipants(firstUserId, secondUserId,
                "Пользователь не может иметь связь дружбы сам с собой: id = " + firstUserId + ".");
        User firstUser = participants.user();
        User secondUser = participants.friend();

        if (friendshipStorage.existsByUserIds(firstUserId, secondUserId)) {
            return buildFriendRelationStatusResponse(firstUser, secondUser, FriendRelationStatus.FRIENDS,
                    firstUser.getName() + " и " + secondUser.getName() + " являются друзьями");
        }

        if (friendRequestStorage.existsByRequesterIdAndRecipientId(firstUserId, secondUserId)) {
            return buildFriendRelationStatusResponse(firstUser, secondUser,
                    FriendRelationStatus.FIRST_REQUESTED_SECOND,
                    firstUser.getName() + " отправил заявку в друзья пользователю " + secondUser.getName());
        }

        if (friendRequestStorage.existsByRequesterIdAndRecipientId(secondUserId, firstUserId)) {
            return buildFriendRelationStatusResponse(firstUser, secondUser,
                    FriendRelationStatus.SECOND_REQUESTED_FIRST,
                    secondUser.getName() + " отправил заявку в друзья пользователю " + firstUser.getName());
        }

        return buildFriendRelationStatusResponse(firstUser, secondUser, FriendRelationStatus.NO_RELATION,
                "Связь между пользователями " + firstUser.getName() + " и " + secondUser.getName() + " отсутствует");
    }

    private void normalizeUser(User user) {
        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
            log.info("Пустое поле name автоматически заполнено значением login : {}", user.getLogin());
        }
    }

    private void validateId(Long id) {
        if (id == null) {
            log.warn("Указан невалидный Id = {}", id);
            throw new ValidationException("Id должен быть указан");
        }

        if (id <= 0) {
            log.warn("Указан невалидный Id = {}", id);
            throw new ValidationException("Id должен быть положительным");
        }
    }

    private FriendshipParticipants getFriendshipParticipants(Long userId, Long friendId, String selfFriendshipMessage) {
        validateId(userId);
        validateId(friendId);

        if (userId.equals(friendId)) {
            throw new ValidationException(selfFriendshipMessage);
        }

        User user = userStorage.findById(userId);
        User friend = userStorage.findById(friendId);

        return new FriendshipParticipants(user, friend);
    }

    private Set<User> getFriendsByIds(Set<Long> ids) {
        return ids.stream()
                .map(userStorage::findById)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    // Во внутренней модели исходящая заявка и подтверждённая дружба различаются.
    // Для публичного API исходящая заявка считается пользователем, которого автор добавил в друзья.
    private Set<Long> findVisibleFriendIdsByUserId(Long userId) {
        Set<Long> friendIds = new LinkedHashSet<>(friendshipStorage.findFriendIdsByUserId(userId));
        friendIds.addAll(friendRequestStorage.findRecipientIdsByRequesterId(userId));
        return friendIds;
    }

    private FriendRelationStatusResponse buildFriendRelationStatusResponse(User firstUser,
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

    private record FriendshipParticipants(User user, User friend) {
    }
}

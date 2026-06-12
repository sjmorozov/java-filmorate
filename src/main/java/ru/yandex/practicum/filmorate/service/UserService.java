package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class UserService {
    private final UserStorage userStorage;

    @Autowired
    public UserService(UserStorage userStorage) {
        this.userStorage = userStorage;
    }

    public User createUser(User user) {
        normalizeUser(user);
        User createdUser = userStorage.addUser(user);
        log.info("Пользователь с id = {}, login = {} добавлен", user.getId(), user.getLogin());
        return createdUser;
    }

    public User updateUser(User user) {
        validateId(user.getId());

        userStorage.findUserById(user.getId());
        normalizeUser(user);

        User updatedUser = userStorage.updateUser(user);
        log.info("Профиль пользователя с id = {}, login = {} обновлён", user.getId(), user.getLogin());
        return updatedUser;
    }

    public void deleteUser(Long id) {
        validateId(id);
        userStorage.deleteUser(id);
    }

    public User findUserById(Long id) {
        validateId(id);
        return userStorage.findUserById(id);
    }

    public Collection<User> findAllUsers() {
        return userStorage.findAllUsers();
    }

    public void addFriend(Long userId, Long friendId) {
        validateId(userId);
        validateId(friendId);

        if (userId.equals(friendId)) {
            throw new ValidationException("Пользователь не может добавить самого себя в друзья: id = " + userId + ".");
        }

        User user = userStorage.findUserById(userId);
        User friend = userStorage.findUserById(friendId);

        user.getFriends().add(friendId);
        friend.getFriends().add(userId);

        userStorage.updateUser(user);
        userStorage.updateUser(friend);

        log.info("Пользователь {} подружился с пользователем {}", user.getLogin(), friend.getLogin());
    }

    public void removeFriend(Long userId, Long friendId) {
        validateId(userId);
        validateId(friendId);

        if (userId.equals(friendId)) {
            throw new ValidationException("Пользователь не может удалить из друзей самого себя: id = " + userId + ".");
        }

        User user = userStorage.findUserById(userId);
        User friend = userStorage.findUserById(friendId);

        user.getFriends().remove(friendId);
        friend.getFriends().remove(userId);

        userStorage.updateUser(user);
        userStorage.updateUser(friend);

        log.info("Пользователь {} удалил пользователя {} из друзей", user.getLogin(), friend.getLogin());
    }

    public Set<User> getUserFriends(Long id) {
        validateId(id);
        User user = userStorage.findUserById(id);
        Set<Long> friendsIds = user.getFriends();

        return getFriendsByIds(friendsIds);
    }

    public Set<User> getCommonFriends(Long firstId, Long secondId) {
        validateId(firstId);
        validateId(secondId);

        User firstUser = userStorage.findUserById(firstId);
        Set<Long> firstSetIds = firstUser.getFriends();
        User secondUser = userStorage.findUserById(secondId);
        Set<Long> secondSetIds = secondUser.getFriends();

        Set<Long> commonFriends = firstSetIds.stream()
                .filter(secondSetIds::contains)
                .collect(Collectors.toSet());

        return getFriendsByIds(commonFriends);
    }

    private void normalizeUser(User user) {
        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
            log.info("Пустое поле name автоматически заполнено значением login : {}", user.getLogin());
        }
    }

    private void validateId(Long id) {
        if (id == null || id <= 0) {
            log.warn("Указан невалидный Id = {}", id);
            throw new ValidationException("Id должен быть указан");
        }
    }

    private Set<User> getFriendsByIds(Set<Long> ids) {
        return ids.stream()
                .map(userStorage::findUserById)
                .collect(Collectors.toSet());
    }
}

package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.util.Collection;

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
        if (user.getId() == null || user.getId() <= 0) {
            log.warn("Указан невалидный Id = {}", user.getId());
            throw new ValidationException("Id должен быть указан");
        }

        userStorage.findUserById(user.getId());
        normalizeUser(user);

        User updatedUser = userStorage.updateUser(user);
        log.info("Профиль пользователя с id = {}, login = {} обновлён", user.getId(), user.getLogin());
        return updatedUser;
    }

    public void deleteUser(Integer id) {
        userStorage.deleteUser(id);
    }

    public User findUserById(Integer id) {
        return userStorage.findUserById(id);
    }

    public Collection<User> findAllUsers() {
        return userStorage.findAllUsers();
    }

    private void normalizeUser(User user) {
        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
            log.info("Пустое поле name автоматически заполнено значением login : {}", user.getLogin());
        }
    }
}

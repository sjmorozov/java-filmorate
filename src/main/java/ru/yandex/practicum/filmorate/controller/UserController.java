package ru.yandex.practicum.filmorate.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/users")
public class UserController {
    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private final Map<Integer, User> users = new HashMap<>();

    @GetMapping
    public Collection<User> getAllUsers() {
        return users.values();
    }

    @PostMapping
    public User createUser(@RequestBody User user) {
        validateAndNormalizeUser(user);

        user.setId(getNextId());
        users.put(user.getId(), user);
        log.info("Пользователь с id = {}, login = {} добавлен", user.getId(), user.getLogin());
        return user;
    }

    private int getNextId() {
        int currentMaxId = users.keySet()
                .stream()
                .mapToInt(id -> id)
                .max()
                .orElse(0);
        return currentMaxId + 1;
    }

    @PutMapping
    public User updateUser(@RequestBody User user) {
        if (user.getId() <= 0) {
            log.warn("Указан невалидный Id = {}", user.getId());
            throw new ValidationException("Id должен быть указан");
        }

        if (!users.containsKey(user.getId())) {
            log.warn("Пользователь с id = {} не найден", user.getId());
            throw new NotFoundException("Пользователь с id = " + user.getId() + " не найден");
        }
        validateAndNormalizeUser(user);

        users.put(user.getId(), user);
        log.info("Профиль пользователя с id = {}, login = {} обновлён", user.getId(), user.getLogin());
        return user;
    }

    private void validateAndNormalizeUser(User user) {

        if (user.getEmail() == null || user.getEmail().isBlank()) {
            log.warn("Электронная почта не указана");
            throw new ValidationException("Электронная почта не может быть пустой");
        }

        if (!user.getEmail().contains("@")) {
            log.warn("Некорректный формат электронной почты: {}", user.getEmail());
            throw new ValidationException("Некорректный формат имейл");
        }

        if (user.getLogin() == null || user.getLogin().isBlank()) {
            log.warn("Логин не указан");
            throw new ValidationException("Логин не может быть пустым");
        }

        if (user.getLogin().chars().anyMatch(Character::isWhitespace)) {
            log.warn("Логин содержит пробелы: {}", user.getLogin());
            throw new ValidationException("Логин не может содержать пробелы");
        }

        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
            log.info("Пустое поле name автоматически заполнено значением login : {}", user.getLogin());
        }

        if (user.getBirthday() != null && user.getBirthday().isAfter(LocalDate.now())) {
            log.warn("Невалидная дата рождения: {}", user.getBirthday());
            throw new ValidationException("Дата рождения не может быть в будущем");
        }
    }

}

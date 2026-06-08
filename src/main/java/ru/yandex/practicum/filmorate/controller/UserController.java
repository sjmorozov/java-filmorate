package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/users")
public class UserController {

    private final Map<Integer, User> users = new HashMap<>();

    @GetMapping
    public Collection<User> getAllUsers() {
        return users.values();
    }

    @PostMapping
    public User createUser(@Valid @RequestBody User user) {
        normalizeUser(user);

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
    public User updateUser(@Valid @RequestBody User user) {
        if (user.getId() == null || user.getId() <= 0) {
            log.warn("Указан невалидный Id = {}", user.getId());
            throw new ValidationException("Id должен быть указан");
        }

        if (!users.containsKey(user.getId())) {
            log.warn("Пользователь с id = {} не найден", user.getId());
            throw new NotFoundException("Пользователь с id = " + user.getId() + " не найден");
        }
        normalizeUser(user);

        users.put(user.getId(), user);
        log.info("Профиль пользователя с id = {}, login = {} обновлён", user.getId(), user.getLogin());
        return user;
    }

    private void normalizeUser(User user) {
        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
            log.info("Пустое поле name автоматически заполнено значением login : {}", user.getLogin());
        }
    }

}

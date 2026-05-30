package ru.yandex.practicum.filmorate.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.filmorate.exception.ConditionsNotMetException;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.User;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/users")
public class UserController {
    private final Map<Integer, User> users = new HashMap<>();

    @GetMapping
    public Collection<User> getAllUsers() {
        return users.values();
    }

    @PostMapping
    public User createUser(@RequestBody User user) {

        boolean isEmailMissing = user.getEmail() == null || user.getEmail().isBlank();
        if (isEmailMissing) {
            throw new ConditionsNotMetException("Имейл должен быть указан");
        }

        boolean isLoginMissing = user.getLogin() == null || user.getLogin().isBlank();
        if (isLoginMissing) {
            throw new ConditionsNotMetException("Логин должен быть указан");
        }

        boolean isNameMissing = user.getName() == null || user.getName().isBlank();
        if (isNameMissing) {
            throw new ConditionsNotMetException("Имя должно быть указано");
        }

        user.setId(getNextId());
        users.put(user.getId(), user);
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
    public User updateUser(@RequestBody User newUser) {
        if (newUser.getId() <= 0) {
            throw new ConditionsNotMetException("Id должен быть указан");
        }

        if (!users.containsKey(newUser.getId())) {
            throw new NotFoundException("Пользователь с id = " + newUser.getId() + " не найден");
        }

        User oldUser = users.get(newUser.getId());

        if (newUser.getEmail() != null) {
            oldUser.setEmail(newUser.getEmail());
        }

        if (newUser.getLogin() != null) {
            oldUser.setLogin(newUser.getLogin());
        }

        if (newUser.getName() != null) {
            oldUser.setName(newUser.getName());
        }

        if (newUser.getBirthday() != null) {
            oldUser.setBirthday(newUser.getBirthday());
        }

        return oldUser;
    }

}

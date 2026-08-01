package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.FriendRelationStatusResponse;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.service.FilmService;
import ru.yandex.practicum.filmorate.service.UserService;

import java.util.Collection;
import java.util.Set;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;
    private final FilmService filmService;

    @PostMapping
    public User create(@Valid @RequestBody User user) {
        return userService.create(user);
    }

    @PutMapping
    public User update(@Valid @RequestBody User user) {
        return userService.update(user);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        userService.delete(id);
    }

    @GetMapping("/{id}")
    public User findById(@PathVariable Long id) {
        return userService.findById(id);
    }

    @GetMapping
    public Collection<User> findAll() {
        return userService.findAll();
    }

    @PutMapping("/{id}/friends/{friendId}")
    public void addFriend(@PathVariable Long id, @PathVariable Long friendId) {
        userService.addFriend(id, friendId);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{id}/friends/{friendId}")
    public void removeFriend(@PathVariable Long id, @PathVariable Long friendId) {
        userService.removeFriend(id, friendId);
    }

    @GetMapping("/{id}/friends/{friendId}/status")
    public FriendRelationStatusResponse findRelationStatus(@PathVariable Long id, @PathVariable Long friendId) {
        return userService.findRelationStatus(id, friendId);
    }

    @GetMapping("/{id}/friends")
    public Set<User> findFriends(@PathVariable Long id) {
        // Возвращаем публичный список друзей: подтверждённые дружбы и исходящие заявки пользователя.
        return userService.findFriends(id);
    }

    @GetMapping("/{id}/friends/common/{otherId}")
    public Set<User> findCommonFriends(@PathVariable Long id, @PathVariable Long otherId) {
        return userService.findCommonFriends(id, otherId);
    }

    @GetMapping("/{id}/recommendations")
    public Collection<Film> getRecommendations(@PathVariable Long id) {
        userService.findById(id);
        return filmService.getRecommendations(id);
    }
}

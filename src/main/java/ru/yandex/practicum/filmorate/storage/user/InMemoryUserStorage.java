package ru.yandex.practicum.filmorate.storage.user;

import lombok.extern.slf4j.Slf4j;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.User;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class InMemoryUserStorage implements UserStorage {
    private final Map<Long, User> users = new HashMap<>();

    @Override
    public User add(User user) {
        user.setId(getNextId());
        users.put(user.getId(), user);
        return user;
    }

    @Override
    public User update(User user) {
        checkUserExists(user.getId());
        users.put(user.getId(), user);
        return user;
    }

    @Override
    public void delete(Long id) {
        checkUserExists(id);
        users.remove(id);
    }

    @Override
    public User findById(Long id) {
        checkUserExists(id);
        return users.get(id);
    }

    @Override
    public Collection<User> findAll() {
        return users.values();
    }

    private void checkUserExists(Long id) {
        if (!users.containsKey(id)) {
            log.warn("Пользователь с id = {} не найден", id);
            throw new NotFoundException("Пользователь с id = " + id + " не найден");
        }
    }

    private Long getNextId() {
        long currentMaxId = users.keySet()
                .stream()
                .mapToLong(id -> id)
                .max()
                .orElse(0L);
        return currentMaxId + 1L;
    }
}

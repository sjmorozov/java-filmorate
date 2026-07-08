package ru.yandex.practicum.filmorate.storage.film;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class InMemoryFilmStorage implements FilmStorage {
    private final Map<Long, Film> films = new HashMap<>();

    @Override
    public Film add(Film film) {
        film.setId(getNextId());
        films.put(film.getId(), film);
        return film;
    }

    @Override
    public Film update(Film film) {
        checkFilmExists(film.getId());
        films.put(film.getId(), film);
        return film;
    }

    @Override
    public void delete(Long id) {
        checkFilmExists(id);
        films.remove(id);
    }

    @Override
    public Film findById(Long id) {
        checkFilmExists(id);
        return films.get(id);
    }

    @Override
    public Collection<Film> findAll() {
        return films.values();
    }

    private void checkFilmExists(Long id) {
        if (!films.containsKey(id)) {
            log.warn("Фильм с id = {} не найден", id);
            throw new NotFoundException("Фильм с id = " + id + " не найден");
        }
    }

    private Long getNextId() {
        long currentMaxId = films.keySet()
                .stream()
                .mapToLong(id -> id)
                .max()
                .orElse(0L);

        return currentMaxId + 1L;
    }
}

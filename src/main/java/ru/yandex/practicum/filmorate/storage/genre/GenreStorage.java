package ru.yandex.practicum.filmorate.storage.genre;

import ru.yandex.practicum.filmorate.model.Genre;

import java.util.Collection;
import java.util.Set;

public interface GenreStorage {
    Genre findById(Integer id);

    Set<Genre> findByIds(Collection<Integer> ids);

    Collection<Genre> findAll();
}

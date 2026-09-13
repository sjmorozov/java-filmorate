package ru.yandex.practicum.filmorate.storage.director;

import ru.yandex.practicum.filmorate.model.Director;

import java.util.Collection;
import java.util.Set;

public interface DirectorStorage {
    Director add(Director director);

    Director update(Director director);

    void delete(Long id);

    Director findById(Long id);

    Set<Director> findByIds(Collection<Long> ids);

    Collection<Director> findAll();
}

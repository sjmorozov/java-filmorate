package ru.yandex.practicum.filmorate.storage.mparating;

import ru.yandex.practicum.filmorate.model.MpaRating;

import java.util.Collection;

public interface MpaRatingStorage {
    MpaRating findById(Integer id);

    Collection<MpaRating> findAll();
}

package ru.yandex.practicum.filmorate.storage.mparating;

import ru.yandex.practicum.filmorate.model.MpaRating;

public interface MpaRatingStorage {
    MpaRating findById(Integer id);

}

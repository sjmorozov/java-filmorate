package ru.yandex.practicum.filmorate.storage.review;

import ru.yandex.practicum.filmorate.model.Review;

import java.util.List;

public interface ReviewStorage {

    Review add(Review review);

    Review update(Review review);

    void delete(Long id);

    Review findById(Long id);

    // filmId == null означает отзывы по всем фильмам
    // результат отсортирован по рейтингу полезности по убыванию
    List<Review> findMostUseful(Long filmId, int count);
}

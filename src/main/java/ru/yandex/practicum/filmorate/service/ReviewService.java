package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Review;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.review.ReviewStorage;
import ru.yandex.practicum.filmorate.storage.reviewreaction.ReviewReactionStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {
    private static final int MAX_CONTENT_LENGTH = 10_000;

    private final ReviewStorage reviewStorage;
    private final ReviewReactionStorage reviewReactionStorage;
    private final FilmStorage filmStorage;
    private final UserStorage userStorage;

    @Transactional
    public Review create(Review review) {
        filmStorage.findById(review.getFilmId());
        userStorage.findById(review.getUserId());

        Review createdReview = reviewStorage.add(review);

        log.info("Ревью добавлено: reviewId={}, userId={}", createdReview.getReviewId(), createdReview.getUserId());
        return createdReview;
    }

    @Transactional
    public Review update(Review review) {
        Review oldReview = reviewStorage.findById(review.getReviewId());

        if (review.getContent() != null) {
            if (review.getContent().isBlank()) {
                log.warn("Отзыв пустой");
                throw new ValidationException("Отзыв не может быть пустым");
            }
            if (review.getContent().length() > MAX_CONTENT_LENGTH) {
                log.warn("Длина отзыва {} превышает максимальную в {} символов", review.getContent().length(), MAX_CONTENT_LENGTH);
                throw new ValidationException("Максимальная длина отзыва — " + MAX_CONTENT_LENGTH + " символов");
            }
            oldReview.setContent(review.getContent());
        }

        if (review.getIsPositive() != null) {
            oldReview.setIsPositive(review.getIsPositive());
        }

        Review updatedReview = reviewStorage.update(oldReview);
        log.info("Ревью обновлено: reviewId={}, userId={}", updatedReview.getReviewId(), updatedReview.getUserId());
        return updatedReview;
    }

    public void delete(Long reviewId) {
        reviewStorage.delete(reviewId);
        log.info("Ревью с id = {} удалено", reviewId);
    }

    public Review findById(Long reviewId) {
        return reviewStorage.findById(reviewId);
    }

    public List<Review> findMostUseful(Long filmId, int count) {
        if (filmId != null) {
            filmStorage.findById(filmId);
        }
        return reviewStorage.findMostUseful(filmId, count);
    }

    public void addLike(Long reviewId, Long userId) {
        saveReaction(reviewId, userId, true);
        log.info("Пользователь с id = {} добавил лайк ревью с id = {}", userId, reviewId);
    }

    public void addDislike(Long reviewId, Long userId) {
        saveReaction(reviewId, userId, false);
        log.info("Пользователь с id = {} добавил дизлайк ревью с id = {}", userId, reviewId);
    }

    public void deleteLike(Long reviewId, Long userId) {
        deleteReaction(reviewId, userId, true);
        log.info("Пользователь с id = {} удалил лайк ревью с id = {}", userId, reviewId);
    }

    public void deleteDislike(Long reviewId, Long userId) {
        deleteReaction(reviewId, userId, false);
        log.info("Пользователь с id = {} удалил дизлайк ревью с id = {}", userId, reviewId);
    }

    private void saveReaction(Long reviewId, Long userId, boolean isLike) {
        reviewStorage.findById(reviewId);
        userStorage.findById(userId);
        reviewReactionStorage.save(reviewId, userId, isLike);
    }

    private void deleteReaction(Long reviewId, Long userId, boolean isLike) {
        reviewStorage.findById(reviewId);
        userStorage.findById(userId);
        reviewReactionStorage.delete(reviewId, userId, isLike);
    }
}

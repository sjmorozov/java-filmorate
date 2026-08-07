package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.mapper.EventMapper;
import ru.yandex.practicum.filmorate.model.EventType;
import ru.yandex.practicum.filmorate.model.Operation;
import ru.yandex.practicum.filmorate.model.Review;
import ru.yandex.practicum.filmorate.storage.event.EventStorage;
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
    private final EventStorage eventStorage;

    @Transactional
    public Review create(Review review) {
        filmStorage.findById(review.getFilmId());
        userStorage.findById(review.getUserId());

        Review createdReview = reviewStorage.add(review);

        eventStorage.save(EventMapper.toEvent(
                review.getUserId(), EventType.REVIEW, Operation.ADD, createdReview.getReviewId()));

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

        eventStorage.save(EventMapper.toEvent(
                updatedReview.getUserId(), EventType.REVIEW, Operation.UPDATE, updatedReview.getReviewId()));

        log.info("Ревью обновлено: reviewId={}, userId={}", updatedReview.getReviewId(), updatedReview.getUserId());
        return updatedReview;
    }

    public void delete(Long reviewId) {
        Review review = reviewStorage.findById(reviewId);

        eventStorage.save(EventMapper.toEvent(
                review.getUserId(), EventType.REVIEW, Operation.REMOVE, reviewId));

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

    public void saveReaction(Long reviewId, Long userId, boolean isLike) {
        reviewStorage.findById(reviewId);
        userStorage.findById(userId);
        reviewReactionStorage.save(reviewId, userId, isLike);
        log.info("Пользователь с id = {} добавил {} ревью с id = {}",
                userId, getReactionName(isLike), reviewId);
    }

    public void deleteReaction(Long reviewId, Long userId, boolean isLike) {
        reviewStorage.findById(reviewId);
        userStorage.findById(userId);
        reviewReactionStorage.delete(reviewId, userId, isLike);
        log.info("Пользователь с id = {} удалил {} ревью с id = {}",
                userId, getReactionName(isLike), reviewId);
    }

    private String getReactionName(boolean isLike) {
        return isLike ? "лайк" : "дизлайк";
    }
}

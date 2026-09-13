package ru.yandex.practicum.filmorate.storage.reviewreaction;

public interface ReviewReactionStorage {
    void save(Long reviewId, Long userId, boolean isLike);

    void delete(Long reviewId, Long userId, boolean isLike);
}

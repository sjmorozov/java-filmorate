package ru.yandex.practicum.filmorate;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import ru.yandex.practicum.filmorate.model.Review;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ReviewValidationTest {
    private static final int MAX_CONTENT_LENGTH = 10_000;

    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void shouldHaveNoViolationsWhenReviewIsValid() {
        assertThat(validator.validate(createValidReview())).isEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void shouldHaveContentViolationWhenContentIsBlank(String content) {
        Review review = createValidReview();
        review.setContent(content);

        assertHasViolationForField(review, "content");
    }

    @Test
    void shouldHaveNoViolationsWhenContentHasMaximumLength() {
        Review review = createValidReview();
        review.setContent("а".repeat(MAX_CONTENT_LENGTH));

        assertThat(validator.validate(review)).isEmpty();
    }

    @Test
    void shouldHaveContentViolationWhenContentIsTooLong() {
        Review review = createValidReview();
        review.setContent("а".repeat(MAX_CONTENT_LENGTH + 1));

        assertHasViolationForField(review, "content");
    }

    @Test
    void shouldHaveIsPositiveViolationWhenValueIsNull() {
        Review review = createValidReview();
        review.setIsPositive(null);

        assertHasViolationForField(review, "isPositive");
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -1})
    void shouldDelegateNonPositiveUserIdCheckToService(long userId) {
        Review review = createValidReview();
        review.setUserId(userId);

        assertThat(validator.validate(review)).isEmpty();
    }

    @Test
    void shouldHaveUserIdViolationWhenIdIsNull() {
        Review review = createValidReview();
        review.setUserId(null);

        assertHasViolationForField(review, "userId");
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -1})
    void shouldDelegateNonPositiveFilmIdCheckToService(long filmId) {
        Review review = createValidReview();
        review.setFilmId(filmId);

        assertThat(validator.validate(review)).isEmpty();
    }

    @Test
    void shouldHaveFilmIdViolationWhenIdIsNull() {
        Review review = createValidReview();
        review.setFilmId(null);

        assertHasViolationForField(review, "filmId");
    }

    private Review createValidReview() {
        return Review.builder()
                .content("Очень содержательный отзыв")
                .isPositive(true)
                .userId(1L)
                .filmId(1L)
                .build();
    }

    private void assertHasViolationForField(Review review, String fieldName) {
        Set<ConstraintViolation<Review>> violations = validator.validate(review);

        assertThat(violations)
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains(fieldName);
    }
}

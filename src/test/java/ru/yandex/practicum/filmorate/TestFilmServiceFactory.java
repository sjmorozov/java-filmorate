package ru.yandex.practicum.filmorate;

import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.MpaRating;
import ru.yandex.practicum.filmorate.service.FilmService;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.filmgenre.FilmGenreStorage;
import ru.yandex.practicum.filmorate.storage.filmlike.FilmLikeStorage;
import ru.yandex.practicum.filmorate.storage.genre.GenreStorage;
import ru.yandex.practicum.filmorate.storage.mparating.MpaRatingStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

final class TestFilmServiceFactory {

    private TestFilmServiceFactory() {
    }

    static FilmService create(FilmStorage filmStorage, UserStorage userStorage) {
        return new FilmService(
                filmStorage,
                userStorage,
                new TestMpaRatingStorage(),
                new TestGenreStorage(),
                new TestFilmGenreStorage(),
                new TestFilmLikeStorage()
        );
    }

    private static final class TestMpaRatingStorage implements MpaRatingStorage {
        private final Map<Integer, MpaRating> ratings = Map.of(
                1, new MpaRating(1, "G"),
                2, new MpaRating(2, "PG"),
                3, new MpaRating(3, "PG-13"),
                4, new MpaRating(4, "R"),
                5, new MpaRating(5, "NC-17")
        );

        @Override
        public MpaRating findById(Integer id) {
            MpaRating rating = ratings.get(id);

            if (rating == null) {
                throw new NotFoundException("Рейтинг с id = " + id + " не найден");
            }

            return rating;
        }

        @Override
        public Collection<MpaRating> findAll() {
            return ratings.values();
        }
    }

    private static final class TestGenreStorage implements GenreStorage {
        private final Map<Integer, Genre> genres = Map.of(
                1, new Genre(1, "Комедия"),
                2, new Genre(2, "Драма"),
                3, new Genre(3, "Мультфильм"),
                4, new Genre(4, "Триллер"),
                5, new Genre(5, "Документальный"),
                6, new Genre(6, "Боевик")
        );

        @Override
        public Genre findById(Integer id) {
            Genre genre = genres.get(id);

            if (genre == null) {
                throw new NotFoundException("Жанр с id = " + id + " не найден");
            }

            return genre;
        }

        @Override
        public Set<Genre> findByIds(Collection<Integer> ids) {
            if (ids == null || ids.isEmpty()) {
                return Set.of();
            }

            return ids.stream()
                    .map(this::findById)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }

        @Override
        public Collection<Genre> findAll() {
            return genres.values();
        }
    }

    private static final class TestFilmGenreStorage implements FilmGenreStorage {
        private final Map<Long, Set<Genre>> genresByFilmId = new LinkedHashMap<>();

        @Override
        public void replaceByFilmId(Long filmId, Set<Genre> genres) {
            genresByFilmId.put(filmId, new LinkedHashSet<>(genres));
        }

        @Override
        public Set<Genre> findByFilmId(Long filmId) {
            return new LinkedHashSet<>(genresByFilmId.getOrDefault(filmId, Set.of()));
        }

        @Override
        public Map<Long, Set<Genre>> findByFilmIds(Collection<Long> filmIds) {
            if (filmIds == null || filmIds.isEmpty()) {
                return Map.of();
            }

            Map<Long, Set<Genre>> result = new LinkedHashMap<>();

            for (Long filmId : filmIds) {
                Set<Genre> genres = genresByFilmId.get(filmId);

                if (genres != null) {
                    result.put(filmId, new LinkedHashSet<>(genres));
                }
            }

            return result;
        }
    }

    private static final class TestFilmLikeStorage implements FilmLikeStorage {
        private final Map<Long, Set<Long>> likesByFilmId = new LinkedHashMap<>();

        @Override
        public void add(Long filmId, Long userId) {
            likesByFilmId.computeIfAbsent(filmId, id -> new LinkedHashSet<>()).add(userId);
        }

        @Override
        public void delete(Long filmId, Long userId) {
            Set<Long> likes = likesByFilmId.get(filmId);

            if (likes == null || !likes.remove(userId)) {
                throw new NotFoundException("Лайк пользователя с id = " + userId
                        + " для фильма с id = " + filmId + " не найден");
            }
        }

        @Override
        public Set<Long> findUserIdsByFilmId(Long filmId) {
            return new LinkedHashSet<>(likesByFilmId.getOrDefault(filmId, Set.of()));
        }

        @Override
        public Map<Long, Set<Long>> findUserIdsByFilmIds(Collection<Long> filmIds) {
            if (filmIds == null || filmIds.isEmpty()) {
                return Map.of();
            }

            Map<Long, Set<Long>> result = new LinkedHashMap<>();

            for (Long filmId : filmIds) {
                Set<Long> likes = likesByFilmId.get(filmId);

                if (likes != null) {
                    result.put(filmId, new LinkedHashSet<>(likes));
                }
            }

            return result;
        }
    }
}

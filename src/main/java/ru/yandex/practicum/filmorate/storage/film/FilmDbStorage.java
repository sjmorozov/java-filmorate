package ru.yandex.practicum.filmorate.storage.film;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.MpaRating;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@RequiredArgsConstructor
@Component
public class FilmDbStorage implements FilmStorage {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public Film addFilm(Film film) {
        String sql = """
                INSERT INTO films (name, description, release_date, duration, mpa_rating_id)
                VALUES (?, ?, ?, ?, ?)
                """;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, film.getName());

            if (film.getDescription() == null) {
                statement.setNull(2, Types.VARCHAR);
            } else {
                statement.setString(2, film.getDescription());
            }

            statement.setDate(3, toSqlDate(film.getReleaseDate()));
            statement.setInt(4, film.getDuration());

            statement.setObject(5, getMpaIdOrNull(film), Types.INTEGER);

            return statement;
        }, keyHolder);

        Number generatedId = keyHolder.getKey();

        if (generatedId == null) {
            throw new IllegalStateException("Не удалось получить id созданного фильма");
        }

        Long filmId = generatedId.longValue();
        film.setId(filmId);

        if (film.getGenres() != null && !film.getGenres().isEmpty()) {
            film.getGenres().stream()
                    .map(Genre::getId)
                    .forEach(id -> addFilmGenre(filmId, id));
        }

        return film;
    }

    @Override
    public Film updateFilm(Film film) {
        String sql = """
                UPDATE films
                SET name = ?, description = ?, release_date = ?, duration = ?, mpa_rating_id = ?
                WHERE id = ?
                """;

        Long filmId = film.getId();

        int rowsAffected = jdbcTemplate.update(
                sql,
                film.getName(),
                film.getDescription(),
                toSqlDate(film.getReleaseDate()),
                film.getDuration(),
                getMpaIdOrNull(film),
                filmId
        );

        if (rowsAffected == 0) {
            throw new NotFoundException("Фильм с id = " + filmId + " не найден");
        }

        if (film.getGenres() != null) {
            deleteFilmGenres(filmId);
            film.getGenres().stream()
                    .map(Genre::getId)
                    .forEach(id -> addFilmGenre(filmId, id));
        }

        return film;
    }

    @Override
    public void deleteFilm(Long id) {
        String sql = """
                DELETE FROM films
                WHERE id = ?
                """;

        int rowsAffected = jdbcTemplate.update(
                sql,
                id
        );

        if (rowsAffected == 0) {
            throw new NotFoundException("Фильм с id = " + id + " не найден");
        }
    }

    @Override
    public Film findFilmById(Long id) {
        String sql = """
                SELECT f.id,
                       f.name,
                       f.description,
                       f.release_date,
                       f.duration,
                       m.id AS mpa_id,
                       m.name AS mpa_name
                FROM films AS f
                LEFT JOIN mpa_ratings AS m ON f.mpa_rating_id = m.id
                WHERE f.id = ?
                """;

        try {
            Film film = jdbcTemplate.queryForObject(sql, this::mapRowToFilm, id);
            return loadFilmRelations(film);
        } catch (EmptyResultDataAccessException e) {
            throw new NotFoundException("Фильм с id = " + id + " не найден");
        }
    }

    @Override
    public Collection<Film> findAllFilms() {
        String sql = """
                SELECT f.id,
                       f.name,
                       f.description,
                       f.release_date,
                       f.duration,
                       m.id AS mpa_id,
                       m.name AS mpa_name
                FROM films AS f
                LEFT JOIN mpa_ratings AS m ON f.mpa_rating_id = m.id
                ORDER BY f.id
                """;

        List<Film> films = jdbcTemplate.query(sql, this::mapRowToFilm);
        films.forEach(this::loadFilmRelations);
        return films;
    }

    private Date toSqlDate(LocalDate date) {
        return date == null ? null : Date.valueOf(date);
    }

    private void addFilmGenre(Long filmId, Integer genreId) {
        String sql = """
                MERGE INTO film_genres (film_id, genre_id)
                KEY (film_id, genre_id)
                VALUES (?, ?)
                """;

        jdbcTemplate.update(
                sql,
                filmId,
                genreId
        );
    }

    private void deleteFilmGenres(Long filmId) {
        String sql = """
                DELETE FROM film_genres
                WHERE film_id = ?
                """;

        jdbcTemplate.update(
                sql,
                filmId
        );
    }

    private Set<Genre> getFilmGenres(Long filmId) {
        String sql = """
                SELECT  f.genre_id AS genre_id,
                        g.name AS genre_name
                FROM film_genres AS f
                JOIN genres AS g ON f.genre_id = g.id
                WHERE f.film_id = ?
                ORDER BY g.id
                """;

        return new LinkedHashSet<>(jdbcTemplate.query(sql, this::mapRowToGenre, filmId));
    }

    private Integer getMpaIdOrNull(Film film) {
        return (film.getMpa() == null) ? null : film.getMpa().getId();
    }

    private Film loadFilmRelations(Film film) {
        film.setGenres(getFilmGenres(film.getId()));
        return film;
    }

    private Film mapRowToFilm(ResultSet rs, int rowNum) throws SQLException {
        Film film = new Film();

        film.setId(rs.getLong("id"));
        film.setName(rs.getString("name"));
        film.setDescription(rs.getString("description"));
        film.setReleaseDate(rs.getObject("release_date", LocalDate.class));
        film.setDuration(rs.getInt("duration"));

        if (rs.getObject("mpa_id") != null) {
            MpaRating mpaRating = new MpaRating(rs.getInt("mpa_id"), rs.getString("mpa_name"));
            film.setMpa(mpaRating);
        }

        return film;
    }

    private Genre mapRowToGenre(ResultSet rs, int rowNum) throws SQLException {
        Genre genre = new Genre();

        genre.setId(rs.getInt("genre_id"));
        genre.setName(rs.getString("genre_name"));
        return genre;
    }
}

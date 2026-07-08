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
import ru.yandex.practicum.filmorate.model.MpaRating;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.Collection;

@Slf4j
@RequiredArgsConstructor
@Component
public class FilmDbStorage implements FilmStorage {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public Film add(Film film) {
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

        return film;
    }

    @Override
    public Film update(Film film) {
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

        return film;
    }

    @Override
    public void delete(Long id) {
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
    public Film findById(Long id) {
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
            return jdbcTemplate.queryForObject(sql, this::mapRowToFilm, id);
        } catch (EmptyResultDataAccessException e) {
            throw new NotFoundException("Фильм с id = " + id + " не найден");
        }
    }

    @Override
    public Collection<Film> findAll() {
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

        return jdbcTemplate.query(sql, this::mapRowToFilm);
    }

    private Date toSqlDate(LocalDate date) {
        return date == null ? null : Date.valueOf(date);
    }

    private Integer getMpaIdOrNull(Film film) {
        return (film.getMpa() == null) ? null : film.getMpa().getId();
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
}

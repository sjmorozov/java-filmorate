package ru.yandex.practicum.filmorate.storage.film;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.MpaRating;
import ru.yandex.practicum.filmorate.storage.BaseDbStorage;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@Slf4j
@Component
public class FilmDbStorage extends BaseDbStorage<Film> implements FilmStorage {

    public FilmDbStorage(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
    }

    @Override
    public Film add(Film film) {
        String sql = """
                INSERT INTO films (name, description, release_date, duration, mpa_rating_id)
                VALUES (?, ?, ?, ?, ?)
                """;

        long filmId = insert(connection -> {
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
        }, "Не удалось получить id созданного фильма");

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

        updateOrThrow(
                sql,
                "Фильм с id = " + filmId + " не найден",
                film.getName(),
                film.getDescription(),
                toSqlDate(film.getReleaseDate()),
                film.getDuration(),
                getMpaIdOrNull(film),
                filmId
        );

        return film;
    }

    @Override
    public void delete(Long id) {
        String sql = """
                DELETE FROM films
                WHERE id = ?
                """;

        updateOrThrow(sql, "Фильм с id = " + id + " не найден", id);
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

        return queryOne(sql, this::mapRowToFilm, "Фильм с id = " + id + " не найден", id);
    }

    @Override
    public Collection<Film> findByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }

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
                WHERE f.id IN (:ids)
                """;

        MapSqlParameterSource parameters = new MapSqlParameterSource("ids", ids);

        return queryMany(sql, parameters, this::mapRowToFilm);
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

        return queryMany(sql, this::mapRowToFilm);
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

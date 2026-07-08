package ru.yandex.practicum.filmorate.storage.filmgenre;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.Genre;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashSet;
import java.util.Set;

@Slf4j
@RequiredArgsConstructor
@Component
public class FilmGenreDbStorage implements FilmGenreStorage {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void replaceByFilmId(Long filmId, Set<Genre> genres) {
        if (genres != null) {
            deleteByFilmId(filmId);
            genres.stream()
                    .map(Genre::getId)
                    .forEach(id -> addGenre(filmId, id));
        }
    }

    @Override
    public Set<Genre> findByFilmId(Long filmId) {
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

    private void deleteByFilmId(Long filmId) {
        String sql = """
                DELETE FROM film_genres
                WHERE film_id = ?
                """;

        jdbcTemplate.update(
                sql,
                filmId
        );
    }

    private void addGenre(Long filmId, Integer genreId) {
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

    private Genre mapRowToGenre(ResultSet rs, int rowNum) throws SQLException {
        Genre genre = new Genre();

        genre.setId(rs.getInt("genre_id"));
        genre.setName(rs.getString("genre_name"));
        return genre;
    }
}

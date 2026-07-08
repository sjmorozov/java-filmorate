package ru.yandex.practicum.filmorate.storage.genre;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Genre;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;

@Slf4j
@RequiredArgsConstructor
@Component
public class GenreDbStorage implements GenreStorage {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public Genre findById(Integer id) {
        String sql = """
                SELECT id, name
                FROM genres
                WHERE id = ?
                """;

        try {
            return jdbcTemplate.queryForObject(sql, this::mapRowToGenre, id);
        } catch (EmptyResultDataAccessException e) {
            throw new NotFoundException("Жанр с id = " + id + " не найден");
        }
    }

    @Override
    public Collection<Genre> findAll() {
        String sql = """
                SELECT id, name
                FROM genres
                ORDER BY id
                """;

        return jdbcTemplate.query(sql, this::mapRowToGenre);
    }

    private Genre mapRowToGenre(ResultSet rs, int rowNum) throws SQLException {
        Genre genre = new Genre();
        genre.setId(rs.getInt("id"));
        genre.setName(rs.getString("name"));
        return genre;
    }
}

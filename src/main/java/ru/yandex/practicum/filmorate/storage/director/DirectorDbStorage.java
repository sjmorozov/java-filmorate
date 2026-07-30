package ru.yandex.practicum.filmorate.storage.director;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Director;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;

@Component
@RequiredArgsConstructor
public class DirectorDbStorage implements DirectorStorage {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public Director add(Director director) {
        String sql = """
                INSERT INTO directors (name)
                VALUES (?)
                """;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, director.getName());
            return statement;
        }, keyHolder);

        Number generatedId = keyHolder.getKey();
        if (generatedId == null) {
            throw new IllegalStateException("Не удалось получить id созданного режиссёра");
        }

        director.setId(generatedId.longValue());
        return director;
    }

    @Override
    public Director update(Director director) {
        String sql = """
                UPDATE directors
                SET name = ?
                WHERE id = ?
                """;

        int rowsAffected = jdbcTemplate.update(sql, director.getName(), director.getId());
        if (rowsAffected == 0) {
            throw new NotFoundException("Режиссёр с id = " + director.getId() + " не найден");
        }

        return director;
    }

    @Override
    public void delete(Long id) {
        String sql = """
                DELETE FROM directors
                WHERE id = ?
                """;

        int rowsAffected = jdbcTemplate.update(sql, id);
        if (rowsAffected == 0) {
            throw new NotFoundException("Режиссёр с id = " + id + " не найден");
        }
    }

    @Override
    public Director findById(Long id) {
        String sql = """
                SELECT id, name
                FROM directors
                WHERE id = ?
                """;

        try {
            return jdbcTemplate.queryForObject(sql, this::mapRowToDirector, id);
        } catch (EmptyResultDataAccessException e) {
            throw new NotFoundException("Режиссёр с id = " + id + " не найден");
        }
    }

    @Override
    public Collection<Director> findAll() {
        String sql = """
                SELECT id, name
                FROM directors
                ORDER BY id
                """;

        return jdbcTemplate.query(sql, this::mapRowToDirector);
    }

    private Director mapRowToDirector(ResultSet rs, int rowNum) throws SQLException {
        Director director = new Director();
        director.setId(rs.getLong("id"));
        director.setName(rs.getString("name"));
        return director;
    }
}

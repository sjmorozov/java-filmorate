package ru.yandex.practicum.filmorate.storage.director;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.storage.BaseDbStorage;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

@Component
public class DirectorDbStorage extends BaseDbStorage<Director> implements DirectorStorage {

    public DirectorDbStorage(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
    }

    @Override
    public Director add(Director director) {
        String sql = """
                INSERT INTO directors (name)
                VALUES (?)
                """;

        long directorId = insert(connection -> {
            PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, director.getName());
            return statement;
        }, "Не удалось получить id созданного режиссёра");

        director.setId(directorId);
        return director;
    }

    @Override
    public Director update(Director director) {
        String sql = """
                UPDATE directors
                SET name = ?
                WHERE id = ?
                """;

        updateOrThrow(sql,
                "Режиссёр с id = " + director.getId() + " не найден",
                director.getName(), director.getId());

        return director;
    }

    @Override
    public void delete(Long id) {
        String sql = """
                DELETE FROM directors
                WHERE id = ?
                """;

        updateOrThrow(sql, "Режиссёр с id = " + id + " не найден", id);
    }

    @Override
    public Director findById(Long id) {
        String sql = """
                SELECT id, name
                FROM directors
                WHERE id = ?
                """;

        return queryOne(sql, this::mapRowToDirector, "Режиссёр с id = " + id + " не найден", id);
    }

    @Override
    public Set<Director> findByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Set.of();
        }

        String sql = """
                SELECT id, name
                FROM directors
                WHERE id IN (:ids)
                ORDER BY id
                """;

        MapSqlParameterSource parameters = new MapSqlParameterSource("ids", ids);

        return new LinkedHashSet<>(queryMany(sql, parameters, this::mapRowToDirector));
    }

    @Override
    public Collection<Director> findAll() {
        String sql = """
                SELECT id, name
                FROM directors
                ORDER BY id
                """;

        return queryMany(sql, this::mapRowToDirector);
    }

    private Director mapRowToDirector(ResultSet rs, int rowNum) throws SQLException {
        Director director = new Director();
        director.setId(rs.getLong("id"));
        director.setName(rs.getString("name"));
        return director;
    }
}

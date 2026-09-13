package ru.yandex.practicum.filmorate.storage.genre;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.storage.BaseDbStorage;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

@Slf4j
@Component
public class GenreDbStorage extends BaseDbStorage<Genre> implements GenreStorage {

    public GenreDbStorage(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
    }

    @Override
    public Genre findById(Integer id) {
        String sql = """
                SELECT id, name
                FROM genres
                WHERE id = ?
                """;

        return queryOne(sql, this::mapRowToGenre, "Жанр с id = " + id + " не найден", id);
    }

    @Override
    public Set<Genre> findByIds(Collection<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return Set.of();
        }

        String sql = """
                SELECT id, name
                FROM genres
                WHERE id IN (:ids)
                ORDER BY id
                """;

        MapSqlParameterSource parameters = new MapSqlParameterSource("ids", ids);

        return new LinkedHashSet<>(queryMany(sql, parameters, this::mapRowToGenre));
    }

    @Override
    public Collection<Genre> findAll() {
        String sql = """
                SELECT id, name
                FROM genres
                ORDER BY id
                """;

        return queryMany(sql, this::mapRowToGenre);
    }

    private Genre mapRowToGenre(ResultSet rs, int rowNum) throws SQLException {
        Genre genre = new Genre();
        genre.setId(rs.getInt("id"));
        genre.setName(rs.getString("name"));
        return genre;
    }
}

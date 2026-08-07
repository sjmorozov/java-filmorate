package ru.yandex.practicum.filmorate.storage.mparating;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.MpaRating;
import ru.yandex.practicum.filmorate.storage.BaseDbStorage;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;

@Component
public class MpaRatingDbStorage extends BaseDbStorage<MpaRating> implements MpaRatingStorage {

    public MpaRatingDbStorage(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
    }

    @Override
    public MpaRating findById(Integer id) {
        String sql = """
                SELECT id, name
                FROM mpa_ratings
                WHERE id = ?
                """;

        return queryOne(sql, this::mapRowToMpa, "Рейтинг с id = " + id + " не найден", id);
    }

    @Override
    public Collection<MpaRating> findAll() {
        String sql = """
                SELECT id, name
                FROM mpa_ratings
                ORDER BY id
                """;

        return queryMany(sql, this::mapRowToMpa);
    }

    private MpaRating mapRowToMpa(ResultSet rs, int rowNum) throws SQLException {
        MpaRating mpaRating = new MpaRating();
        mpaRating.setId(rs.getInt("id"));
        mpaRating.setName(rs.getString("name"));
        return mpaRating;
    }
}

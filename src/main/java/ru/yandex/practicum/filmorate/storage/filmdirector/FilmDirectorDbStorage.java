package ru.yandex.practicum.filmorate.storage.filmdirector;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.Director;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class FilmDirectorDbStorage implements FilmDirectorStorage {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void replaceByFilmId(Long filmId, Set<Director> directors) {
        if (directors != null) {
            deleteByFilmId(filmId);
            directors.stream()
                    .map(Director::getId)
                    .forEach(id -> addDirector(filmId, id));
        }
    }

    @Override
    public Set<Director> findByFilmId(Long filmId) {
        String sql = """
                SELECT  fd.director_id AS director_id,
                        d.name AS director_name
                FROM film_directors AS fd
                JOIN directors AS d ON fd.director_id = d.id
                WHERE fd.film_id = ?
                ORDER BY d.id
                """;

        return new LinkedHashSet<>(jdbcTemplate.query(sql, this::mapRowToDirector, filmId));
    }

    @Override
    public Map<Long, Set<Director>> findByFilmIds(Collection<Long> filmIds) {
        if (filmIds == null || filmIds.isEmpty()) {
            return Map.of();
        }

        String sql = """
                SELECT  fd.film_id AS film_id,
                        fd.director_id AS director_id,
                        d.name AS director_name
                FROM film_directors AS fd
                JOIN directors AS d ON fd.director_id = d.id
                WHERE fd.film_id IN (:filmIds)
                ORDER BY fd.film_id, d.id
                """;

        Map<Long, Set<Director>> directorsByFilmId = new LinkedHashMap<>();
        NamedParameterJdbcTemplate namedJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
        MapSqlParameterSource parameters = new MapSqlParameterSource("filmIds", filmIds);

        namedJdbcTemplate.query(sql, parameters, rs -> {
            Long filmId = rs.getLong("film_id");
            directorsByFilmId.computeIfAbsent(filmId, id -> new LinkedHashSet<>())
                    .add(mapRowToDirector(rs, 0));
        });

        return directorsByFilmId;
    }

    @Override
    public List<Long> findFilmIdsByDirectorId(Long directorId, String sortBy) {
        String orderBy = switch (sortBy) {
            case "year" -> "f.release_date, f.id";
            case "likes" -> "COUNT(fl.user_id) DESC, f.id";
            default -> throw new IllegalArgumentException("Неизвестный тип сортировки: " + sortBy);
        };

        String sql = """
                SELECT f.id
                FROM films AS f
                JOIN film_directors AS fd ON f.id = fd.film_id
                LEFT JOIN film_likes AS fl ON f.id = fl.film_id
                WHERE fd.director_id = ?
                GROUP BY f.id, f.release_date
                ORDER BY %s
                """.formatted(orderBy);

        return jdbcTemplate.queryForList(sql, Long.class, directorId);
    }

    private void deleteByFilmId(Long filmId) {
        String sql = """
                DELETE FROM film_directors
                WHERE film_id = ?
                """;

        jdbcTemplate.update(sql, filmId);
    }

    private void addDirector(Long filmId, Long directorId) {
        String sql = """
                MERGE INTO film_directors (film_id, director_id)
                KEY (film_id, director_id)
                VALUES (?, ?)
                """;

        jdbcTemplate.update(sql, filmId, directorId);
    }

    private Director mapRowToDirector(ResultSet rs, int rowNum) throws SQLException {
        Director director = new Director();
        director.setId(rs.getLong("director_id"));
        director.setName(rs.getString("director_name"));
        return director;
    }
}

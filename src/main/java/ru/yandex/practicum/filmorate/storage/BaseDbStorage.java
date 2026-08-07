package ru.yandex.practicum.filmorate.storage;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import ru.yandex.practicum.filmorate.exception.NotFoundException;

import java.util.List;

public abstract class BaseDbStorage<T> {
    protected final JdbcTemplate jdbcTemplate;

    protected BaseDbStorage(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    protected long insert(PreparedStatementCreator statementCreator, String missingKeyMessage) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(statementCreator, keyHolder);

        Number generatedId = keyHolder.getKey();
        if (generatedId == null) {
            throw new IllegalStateException(missingKeyMessage);
        }

        return generatedId.longValue();
    }

    protected void updateOrThrow(String sql, String notFoundMessage, Object... params) {
        if (jdbcTemplate.update(sql, params) == 0) {
            throw new NotFoundException(notFoundMessage);
        }
    }

    protected T queryOne(String sql, RowMapper<T> rowMapper, String notFoundMessage, Object... params) {
        try {
            return jdbcTemplate.queryForObject(sql, rowMapper, params);
        } catch (EmptyResultDataAccessException e) {
            throw new NotFoundException(notFoundMessage);
        }
    }

    protected List<T> queryMany(String sql, RowMapper<T> rowMapper, Object... params) {
        return jdbcTemplate.query(sql, rowMapper, params);
    }

    protected List<T> queryMany(String sql, SqlParameterSource params, RowMapper<T> rowMapper) {
        NamedParameterJdbcTemplate namedJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
        return namedJdbcTemplate.query(sql, params, rowMapper);
    }
}

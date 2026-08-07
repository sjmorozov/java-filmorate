package ru.yandex.practicum.filmorate.storage.user;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.BaseDbStorage;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.Collection;

@Component
public class UserDbStorage extends BaseDbStorage<User> implements UserStorage {

    public UserDbStorage(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
    }

    @Override
    public User add(User user) {
        String sql = """
                INSERT INTO users (email, login, name, birthday)
                VALUES (?, ?, ?, ?)
                """;

        long userId = insert(connection -> {
            PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, user.getEmail());
            statement.setString(2, user.getLogin());
            statement.setString(3, user.getName());

            if (user.getBirthday() == null) {
                statement.setNull(4, Types.DATE);
            } else {
                statement.setDate(4, toSqlDate(user.getBirthday()));
            }

            return statement;
        }, "Не удалось получить id созданного пользователя");

        user.setId(userId);
        return user;
    }

    @Override
    public User update(User user) {
        String sql = """
                UPDATE users
                SET email = ?, login = ?, name = ?, birthday = ?
                WHERE id = ?
                """;

        updateOrThrow(
                sql,
                "Пользователь с id = " + user.getId() + " не найден",
                user.getEmail(),
                user.getLogin(),
                user.getName(),
                toSqlDate(user.getBirthday()),
                user.getId()
        );

        return user;
    }

    @Override
    public void delete(Long id) {
        String sql = """
                DELETE FROM users
                WHERE id = ?
                """;

        updateOrThrow(sql, "Пользователь с id = " + id + " не найден", id);
    }

    @Override
    public User findById(Long id) {
        String sql = """
                SELECT id, email, login, name, birthday
                FROM users
                WHERE id = ?
                """;

        return queryOne(sql, this::mapRowToUser, "Пользователь с id = " + id + " не найден", id);
    }

    @Override
    public Collection<User> findAll() {
        String sql = """
                SELECT id, email, login, name, birthday
                FROM users
                """;

        return queryMany(sql, this::mapRowToUser);
    }

    private Date toSqlDate(LocalDate date) {
        return date == null ? null : Date.valueOf(date);
    }

    private User mapRowToUser(ResultSet rs, int rowNum) throws SQLException {
        User user = new User();
        user.setId(rs.getLong("id"));
        user.setEmail(rs.getString("email"));
        user.setLogin(rs.getString("login"));
        user.setName(rs.getString("name"));
        user.setBirthday(rs.getObject("birthday", LocalDate.class));
        return user;
    }
}

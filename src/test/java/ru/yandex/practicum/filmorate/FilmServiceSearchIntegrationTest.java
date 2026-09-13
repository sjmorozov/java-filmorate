package ru.yandex.practicum.filmorate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.service.DirectorService;
import ru.yandex.practicum.filmorate.service.FilmService;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class FilmServiceSearchIntegrationTest {

    @Autowired
    private FilmService filmService;

    @Autowired
    private DirectorService directorService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM film_likes");
        jdbcTemplate.update("DELETE FROM film_genres");
        jdbcTemplate.update("DELETE FROM film_directors");
        jdbcTemplate.update("DELETE FROM films");
        jdbcTemplate.update("DELETE FROM directors");
        jdbcTemplate.update("DELETE FROM users");
    }

    @Test
    void search_shouldFindFilmsByTitleSubstring() {
        Film match1 = createAndSaveFilm("Крадущийся тигр, затаившийся дракон", null);
        Film match2 = createAndSaveFilm("Крадущийся в ночи", null);
        createAndSaveFilm("Матрица", null);

        Collection<Film> result = filmService.search("крад", "title");

        assertThat(result)
                .extracting(Film::getId)
                .containsExactlyInAnyOrder(match1.getId(), match2.getId());
    }

    @Test
    void search_shouldFindFilmsByDirectorSubstring() {
        Director nolan = directorService.create(Director.builder().name("Кристофер Нолан").build());

        Film interstellar = createAndSaveFilm("Интерстеллар", nolan);
        Film inception = createAndSaveFilm("Начало", nolan);
        createAndSaveFilm("Матрица", null);

        Collection<Film> result = filmService.search("нолан", "director");

        assertThat(result)
                .extracting(Film::getId)
                .containsExactlyInAnyOrder(interstellar.getId(), inception.getId());
    }

    @Test
    void search_shouldFindFilmsByTitleOrDirectorCombined() {
        Director nolan = directorService.create(Director.builder().name("Кристофер Нолан").build());

        Film titleMatch = createAndSaveFilm("План побега", null);
        Film directorMatch = createAndSaveFilm("Начало", nolan);
        createAndSaveFilm("Матрица", null);

        Collection<Film> result = filmService.search("лан", "director,title");

        assertThat(result)
                .extracting(Film::getId)
                .containsExactlyInAnyOrder(titleMatch.getId(), directorMatch.getId());
    }

    @Test
    void search_shouldSortResultsByPopularity() {
        Film lowPopularity = createAndSaveFilm("Крадущийся в ночи", null);
        Film highPopularity = createAndSaveFilm("Крадущийся тигр", null);

        addLikes(highPopularity, 2);
        addLikes(lowPopularity, 1);

        List<Film> result = filmService.search("крад", "title").stream().toList();

        assertThat(result)
                .extracting(Film::getId)
                .containsExactly(highPopularity.getId(), lowPopularity.getId());
    }

    @Test
    void search_shouldReturnEmptyListWhenNoMatches() {
        createAndSaveFilm("Матрица", null);

        Collection<Film> result = filmService.search("крад", "title");

        assertThat(result).isEmpty();
    }

    @Test
    void search_shouldTreatLikePercentWildcardAsLiteralCharacter() {
        createAndSaveFilm("100% Крутой фильм", null);
        createAndSaveFilm("Крутой фильм", null);

        Collection<Film> result = filmService.search("100%", "title");

        assertThat(result)
                .as("'%' в поисковом запросе не должен работать как SQL-wildcard")
                .hasSize(1)
                .extracting(Film::getName)
                .containsExactly("100% Крутой фильм");
    }

    private Film createAndSaveFilm(String name, Director director) {
        Film.FilmBuilder builder = Film.builder()
                .name(name)
                .description("Test description")
                .releaseDate(LocalDate.of(2020, 1, 1))
                .duration(120);

        if (director != null) {
            builder.directors(Set.of(director));
        }

        return filmService.create(builder.build());
    }

    private void addLikes(Film film, int count) {
        for (int i = 0; i < count; i++) {
            jdbcTemplate.update(
                    "INSERT INTO users (email, login, name, birthday) VALUES (?, ?, ?, ?)",
                    "user" + film.getId() + "_" + i + "@test.com",
                    "user" + film.getId() + "_" + i,
                    "Test User",
                    LocalDate.of(1990, 1, 1)
            );
            Long userId = jdbcTemplate.queryForObject(
                    "SELECT id FROM users WHERE login = ?", Long.class, "user" + film.getId() + "_" + i);
            filmService.addLike(film.getId(), userId);
        }
    }
}

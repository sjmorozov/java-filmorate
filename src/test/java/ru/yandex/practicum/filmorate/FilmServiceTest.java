package ru.yandex.practicum.filmorate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.MpaRating;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.service.FilmService;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.film.InMemoryFilmStorage;
import ru.yandex.practicum.filmorate.storage.user.InMemoryUserStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class FilmServiceTest {
    private FilmService filmService;
    private UserStorage userStorage;

    private static final Long NON_EXISTENT_FILM_ID = 999L;
    private static final Long NON_EXISTENT_USER_ID = 999L;

    private static final String VALID_NAME = "Матрица";
    private static final String VALID_DESCRIPTION = "Человек узнаёт правду о реальности и выбирает красную таблетку.";
    private static final LocalDate VALID_RELEASE_DATE = LocalDate.of(1999, 3, 31);
    private static final int VALID_DURATION = 136;

    private static final String SECOND_FILM_NAME = "Матрица: Революция";
    private static final String SECOND_FILM_DESCRIPTION = "Последняя битва людей и машин приближает финал войны за Зион.";
    private static final LocalDate SECOND_FILM_RELEASE_DATE = LocalDate.of(2003, 11, 5);
    private static final int SECOND_FILM_DURATION = 129;

    private static final String THIRD_FILM_NAME = "Интерстеллар";
    private static final String THIRD_FILM_DESCRIPTION = "Команда исследователей отправляется через червоточину в поисках нового дома для человечества.";
    private static final LocalDate THIRD_FILM_RELEASE_DATE = LocalDate.of(2014, 11, 6);
    private static final int THIRD_FILM_DURATION = 169;

    private static final String FOURTH_FILM_NAME = "Начало";
    private static final String FOURTH_FILM_DESCRIPTION = "Вор, умеющий проникать в сны, получает шанс изменить свою жизнь.";
    private static final LocalDate FOURTH_FILM_RELEASE_DATE = LocalDate.of(2010, 7, 16);
    private static final int FOURTH_FILM_DURATION = 148;

    private static final String FIRST_USER_LOGIN = "neo";
    private static final String FIRST_USER_EMAIL = "neo@example.com";

    private static final String SECOND_USER_LOGIN = "trinity";
    private static final String SECOND_USER_EMAIL = "trinity@example.com";

    private static final LocalDate USER_BIRTHDAY = LocalDate.of(1990, 1, 1);

    private static final LocalDate MIN_RELEASE_DATE = LocalDate.of(1895, 12, 28);

    @BeforeEach
    void setUp() {
        FilmStorage filmStorage = new InMemoryFilmStorage();
        userStorage = new InMemoryUserStorage();
        filmService = new FilmService(filmStorage, userStorage);
    }

    private Film createFilm(String name, String description, LocalDate releaseDate, int duration) {
        return Film.builder()
                .name(name)
                .description(description)
                .releaseDate(releaseDate)
                .duration(duration)
                .build();
    }

    private Film createValidFilm() {
        return createFilm(VALID_NAME, VALID_DESCRIPTION, VALID_RELEASE_DATE, VALID_DURATION);
    }

    private Film createSecondValidFilm() {
        return createFilm(SECOND_FILM_NAME, SECOND_FILM_DESCRIPTION, SECOND_FILM_RELEASE_DATE, SECOND_FILM_DURATION);
    }

    private Film createThirdValidFilm() {
        return createFilm(THIRD_FILM_NAME, THIRD_FILM_DESCRIPTION, THIRD_FILM_RELEASE_DATE, THIRD_FILM_DURATION);
    }

    private Film createFourthValidFilm() {
        return createFilm(FOURTH_FILM_NAME, FOURTH_FILM_DESCRIPTION, FOURTH_FILM_RELEASE_DATE, FOURTH_FILM_DURATION);
    }

    private Film saveFilm(Film film) {
        return filmService.createFilm(film);
    }

    private User createUser(String login, String email) {
        return User.builder()
                .email(email)
                .login(login)
                .name(login)
                .birthday(USER_BIRTHDAY)
                .build();
    }

    private User saveUser(User user) {
        return userStorage.addUser(user);
    }

    private User createFirstUser() {
        return createUser(FIRST_USER_LOGIN, FIRST_USER_EMAIL);
    }

    private User createSecondUser() {
        return createUser(SECOND_USER_LOGIN, SECOND_USER_EMAIL);
    }

    private static String filmNotFoundMessage(Long id) {
        return "Фильм с id = " + id + " не найден";
    }

    private static String userNotFoundMessage(Long id) {
        return "Пользователь с id = " + id + " не найден";
    }

    @Test
    void shouldCreateFilmWithValidData() {
        Film film = createValidFilm();

        Film result = saveFilm(film);

        assertThat(result.getName())
                .as("Ожидается " + VALID_NAME)
                .isEqualTo(VALID_NAME);
        assertThat(result.getDescription())
                .as("Ожидается " + VALID_DESCRIPTION)
                .isEqualTo(VALID_DESCRIPTION);
        assertThat(result.getReleaseDate())
                .as("Ожидается " + VALID_RELEASE_DATE)
                .isEqualTo(VALID_RELEASE_DATE);
        assertThat(result.getDuration())
                .as("Ожидается " + VALID_DURATION)
                .isEqualTo(VALID_DURATION);
        assertThat(result.getId())
                .as("Ожидается id = 1")
                .isEqualTo(1L);
        assertThat(filmService.getAllFilms())
                .as("Ожидается число фильмов 1")
                .hasSize(1);
    }

    @Test
    void shouldCreateFilmWithGenresAndMpaRating() {
        Film film = createValidFilm();
        film.setGenres(Set.of(Genre.ACTION, Genre.SCI_FI));
        film.setMpaRating(MpaRating.PG_13);

        Film result = saveFilm(film);

        assertThat(result.getGenres())
                .as("Жанры фильма должны сохраниться")
                .containsExactlyInAnyOrder(Genre.ACTION, Genre.SCI_FI);
        assertThat(result.getMpaRating())
                .as("Рейтинг MPA должен сохраниться")
                .isEqualTo(MpaRating.PG_13);
    }

    @Test
    void shouldCreateFilmWithMinReleaseDate() {
        Film film = createValidFilm();
        film.setReleaseDate(MIN_RELEASE_DATE);

        Film result = saveFilm(film);

        assertThat(result.getReleaseDate())
                .as("Ожидается " + MIN_RELEASE_DATE)
                .isEqualTo(MIN_RELEASE_DATE);
        assertThat(filmService.getAllFilms())
                .as("Ожидается число фильмов 1")
                .hasSize(1);
    }

    @Test
    void shouldThrowValidationExceptionWhenReleaseDateBeforeMin() {
        Film film = createValidFilm();
        film.setReleaseDate(MIN_RELEASE_DATE.minusDays(1));

        assertThatThrownBy(() -> saveFilm(film))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Дата релиза не может быть раньше " + MIN_RELEASE_DATE);

        assertThat(filmService.getAllFilms())
                .as("Фильм с невалидной датой не должен быть сохранён")
                .isEmpty();
    }

    @Test
    void shouldUpdateFilmWithValidData() {
        Film createdFilm = saveFilm(createValidFilm());

        String newName = "Матрица: Перезагрузка";
        String newDescription = "Нео продолжает борьбу с машинами и ищет путь к спасению Зиона.";
        LocalDate newReleaseDate = LocalDate.of(2003, 5, 15);
        int newDuration = 138;

        Film filmForUpdate = Film.builder()
                .id(createdFilm.getId())
                .name(newName)
                .description(newDescription)
                .releaseDate(newReleaseDate)
                .duration(newDuration)
                .build();

        Film updatedFilm = filmService.updateFilm(filmForUpdate);

        assertThat(updatedFilm.getName())
                .as("Ожидается новое название: " + newName)
                .isEqualTo(newName);
        assertThat(updatedFilm.getDescription())
                .as("Ожидается новое описание: " + newDescription)
                .isEqualTo(newDescription);
        assertThat(updatedFilm.getReleaseDate())
                .as("Ожидается новая дата релиза: " + newReleaseDate)
                .isEqualTo(newReleaseDate);
        assertThat(updatedFilm.getDuration())
                .as("Ожидается новая продолжительность: " + newDuration)
                .isEqualTo(newDuration);
        assertThat(updatedFilm.getId())
                .as("Id фильма не должен измениться")
                .isEqualTo(createdFilm.getId());
        assertThat(filmService.getAllFilms())
                .as("Ожидается общее количество фильмов 1")
                .hasSize(1);
    }

    @Test
    void shouldThrowNotFoundExceptionWhenFilmIdIsZero() {
        Film createdFilm = saveFilm(createValidFilm());

        Film shadowFilm = createValidFilm();
        shadowFilm.setId(0L);

        assertThatThrownBy(() -> filmService.updateFilm(shadowFilm))
                .isInstanceOf(NotFoundException.class)
                .hasMessage(filmNotFoundMessage(0L));

        assertThat(filmService.getAllFilms())
                .as("Размер списка должен остаться без изменений")
                .hasSize(1);
        assertThat(createdFilm.getId())
                .as("Id фильма не должен измениться")
                .isEqualTo(1L);
    }

    @Test
    void shouldThrowNotFoundExceptionWhenFilmIdIsNegative() {
        Film createdFilm = saveFilm(createValidFilm());

        Film shadowFilm = createValidFilm();
        shadowFilm.setId(-1L);

        assertThatThrownBy(() -> filmService.updateFilm(shadowFilm))
                .isInstanceOf(NotFoundException.class)
                .hasMessage(filmNotFoundMessage(-1L));

        assertThat(filmService.getAllFilms())
                .as("Размер списка должен остаться без изменений")
                .hasSize(1);
        assertThat(createdFilm.getId())
                .as("Id фильма не должен измениться")
                .isEqualTo(1L);
    }

    @Test
    void shouldThrowNotFoundExceptionWhenFilmDoesNotExist() {
        Film createdFilm = saveFilm(createValidFilm());

        Film shadowFilm = createValidFilm();
        shadowFilm.setId(NON_EXISTENT_FILM_ID);

        assertThatThrownBy(() -> filmService.updateFilm(shadowFilm))
                .isInstanceOf(NotFoundException.class)
                .hasMessage(filmNotFoundMessage(NON_EXISTENT_FILM_ID));

        assertThat(filmService.getAllFilms())
                .as("Размер списка должен остаться без изменений")
                .hasSize(1);
        assertThat(createdFilm.getId())
                .as("Id фильма не должен измениться")
                .isEqualTo(1L);
    }

    @Test
    void shouldAssignIncrementalIdsWhenSeveralFilmsCreated() {
        Film firstCreatedFilm = saveFilm(createValidFilm());
        Film secondCreatedFilm = saveFilm(createSecondValidFilm());

        assertThat(firstCreatedFilm.getId())
                .as("Ожидается id = 1")
                .isEqualTo(1L);
        assertThat(secondCreatedFilm.getId())
                .as("Ожидается id = 2")
                .isEqualTo(2L);
        assertThat(filmService.getAllFilms())
                .as("Размер списка ожидается 2")
                .hasSize(2);
    }

    @Test
    void shouldAddLikeToFilm() {
        Film film = saveFilm(createValidFilm());
        User user = saveUser(createFirstUser());

        filmService.likeFilm(film.getId(), user.getId());

        Film result = filmService.findFilmById(film.getId());

        assertThat(result.getLikes())
                .as("У фильма должен быть один лайк")
                .hasSize(1);
    }

    @Test
    void shouldNotDuplicateLikeFromSameUser() {
        Film film = saveFilm(createValidFilm());
        User user = saveUser(createFirstUser());

        filmService.likeFilm(film.getId(), user.getId());
        filmService.likeFilm(film.getId(), user.getId());

        Film result = filmService.findFilmById(film.getId());

        assertThat(result.getLikes())
                .as("Один пользователь не должен поставить одному фильму два лайка")
                .hasSize(1);
    }

    @Test
    void shouldDeleteLikeFromFilm() {
        Film film = saveFilm(createValidFilm());
        User user = saveUser(createFirstUser());

        filmService.likeFilm(film.getId(), user.getId());
        filmService.deleteLike(film.getId(), user.getId());

        Film result = filmService.findFilmById(film.getId());

        assertThat(result.getLikes())
                .as("Лайк должен быть удалён")
                .isEmpty();
    }

    @Test
    void shouldReturnPopularFilmsSortedByLikesCount() {
        Film firstFilm = saveFilm(createValidFilm());
        Film secondFilm = saveFilm(createThirdValidFilm());
        Film thirdFilm = saveFilm(createFourthValidFilm());

        User firstUser = saveUser(createFirstUser());
        User secondUser = saveUser(createSecondUser());

        filmService.likeFilm(secondFilm.getId(), firstUser.getId());
        filmService.likeFilm(secondFilm.getId(), secondUser.getId());
        filmService.likeFilm(thirdFilm.getId(), firstUser.getId());

        List<Film> popularFilms = filmService.getPopularFilms(3).stream().toList();

        assertThat(popularFilms.get(0).getId())
                .as("Первым должен быть фильм с двумя лайками")
                .isEqualTo(secondFilm.getId());
        assertThat(popularFilms.get(1).getId())
                .as("Вторым должен быть фильм с одним лайком")
                .isEqualTo(thirdFilm.getId());
        assertThat(popularFilms.get(2).getId())
                .as("Третьим должен быть фильм без лайков")
                .isEqualTo(firstFilm.getId());
    }

    @Test
    void shouldReturnOnlyRequestedCountOfPopularFilms() {
        Film firstFilm = saveFilm(createValidFilm());
        Film secondFilm = saveFilm(createThirdValidFilm());

        User user = saveUser(createFirstUser());

        filmService.likeFilm(secondFilm.getId(), user.getId());

        List<Film> popularFilms = filmService.getPopularFilms(1).stream().toList();

        assertThat(popularFilms)
                .as("Должен вернуться только один фильм")
                .hasSize(1);
        assertThat(popularFilms.get(0).getId())
                .as("Должен вернуться самый популярный фильм")
                .isEqualTo(secondFilm.getId());
    }

    @Test
    void shouldThrowNotFoundExceptionWhenLikeFilmDoesNotExist() {
        User user = saveUser(createFirstUser());

        assertThatThrownBy(() -> filmService.likeFilm(NON_EXISTENT_FILM_ID, user.getId()))
                .isInstanceOf(NotFoundException.class)
                .hasMessage(filmNotFoundMessage(NON_EXISTENT_FILM_ID));
    }

    @Test
    void shouldThrowNotFoundExceptionWhenLikeUserDoesNotExist() {
        Film film = saveFilm(createValidFilm());

        assertThatThrownBy(() -> filmService.likeFilm(film.getId(), NON_EXISTENT_USER_ID))
                .isInstanceOf(NotFoundException.class)
                .hasMessage(userNotFoundMessage(NON_EXISTENT_USER_ID));
    }

    @Test
    void shouldThrowNotFoundExceptionWhenDeleteLikeFilmDoesNotExist() {
        User user = saveUser(createFirstUser());

        assertThatThrownBy(() -> filmService.deleteLike(NON_EXISTENT_FILM_ID, user.getId()))
                .isInstanceOf(NotFoundException.class)
                .hasMessage(filmNotFoundMessage(NON_EXISTENT_FILM_ID));
    }

    @Test
    void shouldThrowNotFoundExceptionWhenDeleteLikeUserDoesNotExist() {
        Film film = saveFilm(createValidFilm());

        assertThatThrownBy(() -> filmService.deleteLike(film.getId(), NON_EXISTENT_USER_ID))
                .isInstanceOf(NotFoundException.class)
                .hasMessage(userNotFoundMessage(NON_EXISTENT_USER_ID));
    }
}

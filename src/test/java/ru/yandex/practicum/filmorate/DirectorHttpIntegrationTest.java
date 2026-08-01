package ru.yandex.practicum.filmorate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.model.Film;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:director-http-test;DB_CLOSE_DELAY=-1",
                "spring.sql.init.mode=always"
        }
)
class DirectorHttpIntegrationTest {
    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void shouldCreateUpdateFindAndDeleteDirector() {
        Director director = Director.builder()
                .name("Андрей Тарковский")
                .build();

        ResponseEntity<Director> createResponse = restTemplate.postForEntity(
                "/directors",
                director,
                Director.class
        );

        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        Director createdDirector = createResponse.getBody();
        assertThat(createdDirector).isNotNull();
        assertThat(createdDirector.getId()).isPositive();

        ResponseEntity<Director> findResponse = restTemplate.getForEntity(
                "/directors/{id}",
                Director.class,
                createdDirector.getId()
        );

        assertThat(findResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(findResponse.getBody()).isEqualTo(createdDirector);

        ResponseEntity<Director[]> findAllResponse = restTemplate.getForEntity(
                "/directors",
                Director[].class
        );

        assertThat(findAllResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(Arrays.asList(findAllResponse.getBody()))
                .extracting(Director::getId)
                .contains(createdDirector.getId());

        Director directorForUpdate = Director.builder()
                .id(createdDirector.getId())
                .name("А. А. Тарковский")
                .build();
        ResponseEntity<Director> updateResponse = restTemplate.exchange(
                "/directors",
                HttpMethod.PUT,
                new HttpEntity<>(directorForUpdate),
                Director.class
        );

        assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updateResponse.getBody()).isEqualTo(directorForUpdate);

        ResponseEntity<Void> deleteResponse = restTemplate.exchange(
                "/directors/{id}",
                HttpMethod.DELETE,
                HttpEntity.EMPTY,
                Void.class,
                createdDirector.getId()
        );

        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(restTemplate.getForEntity(
                "/directors/{id}",
                String.class,
                createdDirector.getId()
        ).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldRejectInvalidDirectorData() {
        ResponseEntity<String> blankNameResponse = restTemplate.postForEntity(
                "/directors",
                Director.builder().name("   ").build(),
                String.class
        );

        assertThat(blankNameResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        ResponseEntity<String> missingIdResponse = restTemplate.exchange(
                "/directors",
                HttpMethod.PUT,
                new HttpEntity<>(Director.builder().name("Новый режиссёр").build()),
                String.class
        );

        assertThat(missingIdResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldReturnNotFoundWhenUpdatingUnknownDirector() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/directors",
                HttpMethod.PUT,
                new HttpEntity<>(new Director(9999L, "Неизвестный режиссёр")),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldSaveReplaceAndLoadFilmDirectors() {
        Director firstDirector = createDirector("Акира Куросава");
        Director secondDirector = createDirector("Хаяо Миядзаки");
        Film film = Film.builder()
                .name("Фильм с режиссёрами")
                .description("Проверяем связь фильма с несколькими режиссёрами")
                .releaseDate(LocalDate.of(2001, 1, 1))
                .duration(120)
                .directors(Set.of(
                        new Director(firstDirector.getId(), null),
                        new Director(secondDirector.getId(), null)
                ))
                .build();

        ResponseEntity<Film> createResponse = restTemplate.postForEntity("/films", film, Film.class);

        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        Film createdFilm = createResponse.getBody();
        assertThat(createdFilm).isNotNull();
        assertThat(createdFilm.getDirectors())
                .as("Созданный фильм должен содержать обоих режиссёров с именами")
                .containsExactlyInAnyOrder(firstDirector, secondDirector);

        Film filmForUpdate = Film.builder()
                .id(createdFilm.getId())
                .directors(Set.of(new Director(secondDirector.getId(), null)))
                .build();
        ResponseEntity<Film> updateResponse = restTemplate.exchange(
                "/films",
                HttpMethod.PUT,
                new HttpEntity<>(filmForUpdate),
                Film.class
        );

        assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updateResponse.getBody()).isNotNull();
        assertThat(updateResponse.getBody().getDirectors())
                .as("Обновление должно заменить прежний набор режиссёров")
                .containsExactly(secondDirector);

        ResponseEntity<Film> partialUpdateResponse = restTemplate.exchange(
                "/films",
                HttpMethod.PUT,
                new HttpEntity<>(Map.of(
                        "id", createdFilm.getId(),
                        "name", "Фильм с новым названием"
                )),
                Film.class
        );

        assertThat(partialUpdateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(partialUpdateResponse.getBody()).isNotNull();
        assertThat(partialUpdateResponse.getBody().getDirectors())
                .as("При отсутствии поля directors существующая связь должна сохраниться")
                .containsExactly(secondDirector);

        restTemplate.delete("/directors/{id}", secondDirector.getId());
        Film filmWithoutDirector = restTemplate.getForObject(
                "/films/{id}",
                Film.class,
                createdFilm.getId()
        );

        assertThat(filmWithoutDirector.getDirectors())
                .as("Удаление режиссёра должно каскадно удалить только связь с фильмом")
                .isEmpty();
    }

    @Test
    void shouldReturnNotFoundWhenFilmDirectorDoesNotExist() {
        Film film = Film.builder()
                .name("Фильм с неизвестным режиссёром")
                .description("Этот фильм не должен сохраниться")
                .releaseDate(LocalDate.of(2002, 2, 2))
                .duration(90)
                .directors(Set.of(new Director(999_999L, null)))
                .build();

        ResponseEntity<String> response = restTemplate.postForEntity("/films", film, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private Director createDirector(String name) {
        ResponseEntity<Director> response = restTemplate.postForEntity(
                "/directors",
                Director.builder().name(name).build(),
                Director.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        return response.getBody();
    }
}

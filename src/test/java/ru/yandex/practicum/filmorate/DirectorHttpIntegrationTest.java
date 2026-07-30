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

import java.util.Arrays;

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
}

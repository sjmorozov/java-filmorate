package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.service.FilmService;

import java.util.Collection;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/films")
public class FilmController {

    private static final String DEFAULT_POPULAR_FILMS_COUNT = "10";

    private final FilmService filmService;

    @PostMapping
    public Film create(@Valid @RequestBody Film film) {
        return filmService.create(film);
    }

    @PutMapping
    public Film update(@RequestBody Film film) {
        return filmService.update(film);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        filmService.delete(id);
    }

    /**
     * Возвращает топ-N фильмов по количеству лайков.
     *
     * @param count   максимальное число фильмов в ответе
     * @param genreId если указан, в ответ попадут только фильмы этого жанра
     * @param year    если указан, в ответ попадут только фильмы с этим годом релиза
     */
    @GetMapping("/popular")
    public Collection<Film> findPopular(@RequestParam(name = "count", defaultValue = DEFAULT_POPULAR_FILMS_COUNT)
                                        @Positive(message = "Параметр count должен быть больше нуля")
                                        int count,
                                        @RequestParam(required = false)
                                        @Positive(message = "Параметр genreId должен быть больше нуля")
                                        Integer genreId,
                                        @RequestParam(required = false)
                                        @Positive(message = "Параметр year должен быть больше нуля")
                                        Integer year) {
        return filmService.findPopular(count, genreId, year);
    }

    @GetMapping("/director/{directorId}")
    public Collection<Film> findByDirector(@PathVariable Long directorId,
                                           @RequestParam(required = false) String sortBy) {
        return filmService.findByDirector(directorId, sortBy);
    }

    @GetMapping("/{id}")
    public Film findById(@PathVariable Long id) {
        return filmService.findById(id);
    }

    @GetMapping
    public Collection<Film> findAll() {
        return filmService.findAll();
    }

    @PutMapping("/{id}/like/{userId}")
    public void addLike(@PathVariable Long id, @PathVariable Long userId) {
        filmService.addLike(id, userId);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{id}/like/{userId}")
    public void deleteLike(@PathVariable Long id, @PathVariable Long userId) {
        filmService.deleteLike(id, userId);
    }

}

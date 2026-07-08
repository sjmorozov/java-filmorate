package ru.yandex.practicum.filmorate.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.filmorate.model.MpaRating;
import ru.yandex.practicum.filmorate.service.MpaRatingService;

import java.util.Collection;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/mpa")
public class MpaRatingController {
    private final MpaRatingService mpaRatingService;

    @GetMapping("/{id}")
    public MpaRating findById(@PathVariable Integer id) {
        return mpaRatingService.findById(id);
    }

    @GetMapping
    public Collection<MpaRating> findAll() {
        return mpaRatingService.findAll();
    }
}

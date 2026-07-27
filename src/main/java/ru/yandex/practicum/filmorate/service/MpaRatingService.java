package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.model.MpaRating;
import ru.yandex.practicum.filmorate.storage.mparating.MpaRatingStorage;

import java.util.Collection;

@Slf4j
@Service
@RequiredArgsConstructor
public class MpaRatingService {
    private final MpaRatingStorage mpaRatingStorage;

    public MpaRating findById(Integer id) {
        return mpaRatingStorage.findById(id);
    }

    public Collection<MpaRating> findAll() {
        return mpaRatingStorage.findAll();
    }
}

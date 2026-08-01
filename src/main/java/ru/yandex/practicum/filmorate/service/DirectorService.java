package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.storage.director.DirectorStorage;

import java.util.Collection;

@Slf4j
@Service
@RequiredArgsConstructor
public class DirectorService {
    private final DirectorStorage directorStorage;

    public Director create(Director director) {
        Director createdDirector = directorStorage.add(director);
        log.info("Режиссёр с id = {}, name = {} добавлен", createdDirector.getId(), createdDirector.getName());
        return createdDirector;
    }

    public Director update(Director director) {
        validateId(director.getId());

        Director updatedDirector = directorStorage.update(director);
        log.info("Режиссёр с id = {} обновлён", updatedDirector.getId());
        return updatedDirector;
    }

    public void delete(Long id) {
        validateId(id);

        directorStorage.delete(id);
        log.info("Режиссёр с id = {} удалён", id);
    }

    public Director findById(Long id) {
        validateId(id);
        return directorStorage.findById(id);
    }

    public Collection<Director> findAll() {
        return directorStorage.findAll();
    }

    private void validateId(Long id) {
        if (id == null) {
            throw new ValidationException("Id режиссёра должен быть указан");
        }

        if (id <= 0) {
            throw new ValidationException("Id режиссёра должен быть положительным");
        }
    }
}

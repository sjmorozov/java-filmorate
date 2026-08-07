package ru.yandex.practicum.filmorate.converter;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.DirectorFilmSort;

import java.util.Arrays;

@Component
public class DirectorFilmSortConverter implements Converter<String, DirectorFilmSort> {

    @Override
    public DirectorFilmSort convert(String source) {
        return Arrays.stream(DirectorFilmSort.values())
                .filter(sort -> sort.getRequestValue().equals(source))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Параметр sortBy должен иметь значение year или likes"));
    }
}

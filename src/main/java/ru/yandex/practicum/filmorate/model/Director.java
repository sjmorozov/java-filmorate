package ru.yandex.practicum.filmorate.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.yandex.practicum.filmorate.validation.OnUpdate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Director {
    @NotNull(groups = OnUpdate.class, message = "Id режиссёра должен быть указан")
    @Positive(groups = OnUpdate.class, message = "Id режиссёра должен быть положительным")
    private Long id;

    @NotBlank(message = "Имя режиссёра не может быть пустым")
    @Size(max = 255, message = "Имя режиссёра не может быть длиннее 255 символов")
    private String name;
}

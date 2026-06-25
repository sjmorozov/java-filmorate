package ru.yandex.practicum.filmorate.model;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Friendship {
    @NotNull(message = "Id первого пользователя должен быть указан")
    @Positive(message = "Id первого пользователя должен быть положительным")
    private Long firstUserId;

    @NotNull(message = "Id второго пользователя должен быть указан")
    @Positive(message = "Id второго пользователя должен быть положительным")
    private Long secondUserId;
}

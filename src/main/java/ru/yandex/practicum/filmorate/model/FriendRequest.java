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
public class FriendRequest {
    @NotNull(message = "Id отправителя заявки должен быть указан")
    @Positive(message = "Id отправителя заявки должен быть положительным")
    private Long requesterId;

    @NotNull(message = "Id получателя заявки должен быть указан")
    @Positive(message = "Id получателя заявки должен быть положительным")
    private Long recipientId;
}

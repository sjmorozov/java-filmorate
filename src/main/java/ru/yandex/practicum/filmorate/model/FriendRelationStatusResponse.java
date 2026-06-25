package ru.yandex.practicum.filmorate.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FriendRelationStatusResponse {
    private Long firstUserId;
    private String firstUserName;
    private Long secondUserId;
    private String secondUserName;
    private FriendRelationStatus status;
    private String description;
}

package ru.yandex.practicum.filmorate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.FriendRelationStatus;
import ru.yandex.practicum.filmorate.model.FriendRelationStatusResponse;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.service.UserService;
import ru.yandex.practicum.filmorate.storage.event.EventStorage;
import ru.yandex.practicum.filmorate.storage.friendrequest.FriendRequestStorage;
import ru.yandex.practicum.filmorate.storage.friendrequest.InMemoryFriendRequestStorage;
import ru.yandex.practicum.filmorate.storage.friendship.FriendshipStorage;
import ru.yandex.practicum.filmorate.storage.friendship.InMemoryFriendshipStorage;
import ru.yandex.practicum.filmorate.storage.user.InMemoryUserStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

public class UserServiceTest {
    private UserService userService;

    private static final Long NON_EXISTENT_USER_ID = 999L;

    private static final String ID_REQUIRED_MESSAGE = "Id должен быть указан";
    private static final String ID_MUST_BE_POSITIVE_MESSAGE = "Id должен быть положительным";

    private static final String VALID_EMAIL = "theone@zion.human";
    private static final String VALID_LOGIN = "theOne";
    private static final String VALID_NAME = "Нео";
    private static final LocalDate VALID_BIRTHDAY = LocalDate.of(1971, 9, 13);

    private static final String MORPHEUS_EMAIL = "morpheus@zion.human";
    private static final String MORPHEUS_LOGIN = "morpheus";
    private static final String MORPHEUS_NAME = "Морфеус";
    private static final LocalDate MORPHEUS_BIRTHDAY = LocalDate.of(1961, 7, 30);

    private static final String TRINITY_EMAIL = "trinity@zion.human";
    private static final String TRINITY_LOGIN = "trinity";
    private static final String TRINITY_NAME = "Тринити";
    private static final LocalDate TRINITY_BIRTHDAY = LocalDate.of(1967, 8, 21);

    @BeforeEach
    void setUserService() {
        UserStorage userStorage = new InMemoryUserStorage();
        FriendRequestStorage friendRequestStorage = new InMemoryFriendRequestStorage();
        FriendshipStorage friendshipStorage = new InMemoryFriendshipStorage();
        EventStorage eventStorage = mock(EventStorage.class);

        userService = new UserService(userStorage, friendRequestStorage, friendshipStorage, eventStorage);
    }

    private User create(String email, String login, String name, LocalDate birthday) {
        return User.builder()
                .email(email)
                .login(login)
                .name(name)
                .birthday(birthday)
                .build();
    }

    private User createValidUser() {
        return create(VALID_EMAIL, VALID_LOGIN, VALID_NAME, VALID_BIRTHDAY);
    }

    private User createSecondValidUser() {
        return create(MORPHEUS_EMAIL, MORPHEUS_LOGIN, MORPHEUS_NAME, MORPHEUS_BIRTHDAY);
    }

    private User createThirdValidUser() {
        return create(TRINITY_EMAIL, TRINITY_LOGIN, TRINITY_NAME, TRINITY_BIRTHDAY);
    }

    private User saveUser(User user) {
        return userService.create(user);
    }

    private static String userNotFoundMessage(Long id) {
        return "Пользователь с id = " + id + " не найден";
    }

    private static String addSelfAsFriendMessage(Long id) {
        return "Пользователь не может добавить самого себя в друзья: id = " + id + ".";
    }

    private static String removeSelfFromFriendsMessage(Long id) {
        return "Пользователь не может удалить из друзей самого себя: id = " + id + ".";
    }

    private void assertRelationStatus(User firstUser, User secondUser, FriendRelationStatus expectedStatus) {
        FriendRelationStatusResponse response = userService.findRelationStatus(firstUser.getId(), secondUser.getId());

        assertThat(response.getFirstUserId())
                .as("Id первого пользователя должен совпадать")
                .isEqualTo(firstUser.getId());
        assertThat(response.getFirstUserName())
                .as("Имя первого пользователя должно совпадать")
                .isEqualTo(firstUser.getName());
        assertThat(response.getSecondUserId())
                .as("Id второго пользователя должен совпадать")
                .isEqualTo(secondUser.getId());
        assertThat(response.getSecondUserName())
                .as("Имя второго пользователя должно совпадать")
                .isEqualTo(secondUser.getName());
        assertThat(response.getStatus())
                .as("Статус связи должен совпадать")
                .isEqualTo(expectedStatus);
        assertThat(response.getDescription())
                .as("Описание должно быть человекочитаемым и содержать имена пользователей")
                .contains(firstUser.getName(), secondUser.getName());
    }

    @Test
    void shouldSetLoginAsNameWhenNameIsEmpty() {
        User user = createValidUser();
        user.setName("");

        User resultUser = saveUser(user);

        assertThat(resultUser.getLogin())
                .as("Ожидается логин " + VALID_LOGIN)
                .isEqualTo(VALID_LOGIN);
        assertThat(resultUser.getName())
                .as("Ожидается имя = логин " + VALID_LOGIN)
                .isEqualTo(VALID_LOGIN);
        assertThat(userService.findAll())
                .as("Ожидается общее количество пользователей 1")
                .hasSize(1);
    }

    @Test
    void shouldSetLoginAsNameWhenNameIsBlank() {
        User user = createValidUser();
        user.setName("   ");

        User resultUser = saveUser(user);

        assertThat(resultUser.getLogin())
                .as("Ожидается логин " + VALID_LOGIN)
                .isEqualTo(VALID_LOGIN);
        assertThat(resultUser.getName())
                .as("Ожидается имя = логин " + VALID_LOGIN)
                .isEqualTo(VALID_LOGIN);
        assertThat(userService.findAll())
                .as("Ожидается общее количество пользователей 1")
                .hasSize(1);
    }

    @Test
    void shouldSetLoginAsNameWhenNameIsNull() {
        User user = createValidUser();
        user.setName(null);

        User resultUser = saveUser(user);

        assertThat(resultUser.getLogin())
                .as("Ожидается логин " + VALID_LOGIN)
                .isEqualTo(VALID_LOGIN);
        assertThat(resultUser.getName())
                .as("Ожидается имя = логин " + VALID_LOGIN)
                .isEqualTo(VALID_LOGIN);
        assertThat(userService.findAll())
                .as("Ожидается общее количество пользователей 1")
                .hasSize(1);
    }

    @Test
    void shouldUpdateUserWithValidData() {
        User createdUser = saveUser(createValidUser());

        User userForUpdate = createValidUser();
        userForUpdate.setId(createdUser.getId());

        LocalDate newBirthday = VALID_BIRTHDAY.plusYears(10);
        userForUpdate.setBirthday(newBirthday);

        User updatedUser = userService.update(userForUpdate);

        assertThat(updatedUser.getEmail())
                .as("Ожидается старый имейл " + VALID_EMAIL)
                .isEqualTo(VALID_EMAIL);
        assertThat(updatedUser.getLogin())
                .as("Ожидается старый логин " + VALID_LOGIN)
                .isEqualTo(VALID_LOGIN);
        assertThat(updatedUser.getName())
                .as("Ожидается старое имя " + VALID_NAME)
                .isEqualTo(VALID_NAME);
        assertThat(updatedUser.getBirthday())
                .as("Ожидается новая дата рождения " + newBirthday)
                .isEqualTo(newBirthday);
        assertThat(updatedUser.getId())
                .as("Ожидается старый Id = 1")
                .isEqualTo(1L);
        assertThat(userService.findAll())
                .as("Ожидается общее количество пользователей 1")
                .hasSize(1);
    }

    @Test
    void shouldUpdateUserAndKeepSameId() {
        User createdUser = saveUser(createValidUser());

        String newEmail = "morpheus@zion.human";
        String newLogin = "morpheus";
        String newName = "Морфеус";

        User userForUpdate = User.builder()
                .id(createdUser.getId())
                .email(newEmail)
                .login(newLogin)
                .name(newName)
                .birthday(VALID_BIRTHDAY)
                .build();

        User updatedUser = userService.update(userForUpdate);

        assertThat(updatedUser.getEmail())
                .as("Ожидается новый имейл " + newEmail)
                .isEqualTo(newEmail);
        assertThat(updatedUser.getLogin())
                .as("Ожидается новый логин " + newLogin)
                .isEqualTo(newLogin);
        assertThat(updatedUser.getName())
                .as("Ожидается новое имя " + newName)
                .isEqualTo(newName);
        assertThat(updatedUser.getId())
                .as("Id пользователя не должен измениться")
                .isEqualTo(createdUser.getId());
        assertThat(userService.findAll())
                .as("Ожидается общее количество пользователей 1")
                .hasSize(1);
    }

    @Test
    void shouldThrowValidationExceptionWhenUserIdIsZero() {
        User createdUser = saveUser(createValidUser());

        User shadowUser = createValidUser();
        shadowUser.setId(0L);

        assertThatThrownBy(() -> userService.update(shadowUser))
                .isInstanceOf(ValidationException.class)
                .hasMessage(ID_MUST_BE_POSITIVE_MESSAGE);

        assertThat(userService.findAll())
                .as("Размер списка должен остаться без изменений")
                .hasSize(1);
        assertThat(createdUser.getId())
                .as("Id пользователя не должен измениться")
                .isEqualTo(1L);
    }

    @Test
    void shouldThrowValidationExceptionWhenUserIdIsNegative() {
        User createdUser = saveUser(createValidUser());

        User shadowUser = createValidUser();
        shadowUser.setId(-1L);

        assertThatThrownBy(() -> userService.update(shadowUser))
                .isInstanceOf(ValidationException.class)
                .hasMessage(ID_MUST_BE_POSITIVE_MESSAGE);

        assertThat(userService.findAll())
                .as("Размер списка должен остаться без изменений")
                .hasSize(1);
        assertThat(createdUser.getId())
                .as("Id пользователя не должен измениться")
                .isEqualTo(1L);
    }

    @Test
    void shouldThrowNotFoundExceptionWhenUserDoesNotExist() {
        User createdUser = saveUser(createValidUser());

        User shadowUser = createValidUser();
        shadowUser.setId(NON_EXISTENT_USER_ID);

        assertThatThrownBy(() -> userService.update(shadowUser))
                .isInstanceOf(NotFoundException.class)
                .hasMessage(userNotFoundMessage(NON_EXISTENT_USER_ID));

        assertThat(userService.findAll())
                .as("Размер списка должен остаться без изменений")
                .hasSize(1);
        assertThat(createdUser.getId())
                .as("Id пользователя не должен измениться")
                .isEqualTo(1L);
    }

    @Test
    void shouldAssignIncrementalIdsWhenSeveralUsersCreated() {
        User firstCreatedUser = saveUser(createValidUser());
        User secondCreatedUser = saveUser(createSecondValidUser());

        assertThat(firstCreatedUser.getId())
                .as("Ожидается Id = 1")
                .isEqualTo(1L);
        assertThat(secondCreatedUser.getId())
                .as("Ожидается Id = 2")
                .isEqualTo(2L);
        assertThat(userService.findAll())
                .as("Размер списка ожидается 2")
                .hasSize(2);
    }

    @Test
    void shouldReturnNoRelationStatusWhenUsersHaveNoRelation() {
        User firstCreatedUser = saveUser(createValidUser());
        User secondCreatedUser = saveUser(createSecondValidUser());

        assertRelationStatus(firstCreatedUser, secondCreatedUser, FriendRelationStatus.NO_RELATION);
    }

    @Test
    void shouldReturnFirstRequestedSecondStatusWhenFirstUserSentRequest() {
        User firstCreatedUser = saveUser(createValidUser());
        User secondCreatedUser = saveUser(createSecondValidUser());

        userService.addFriend(firstCreatedUser.getId(), secondCreatedUser.getId());

        assertRelationStatus(firstCreatedUser, secondCreatedUser, FriendRelationStatus.FIRST_REQUESTED_SECOND);
    }

    @Test
    void shouldReturnSecondRequestedFirstStatusWhenSecondUserSentRequest() {
        User firstCreatedUser = saveUser(createValidUser());
        User secondCreatedUser = saveUser(createSecondValidUser());

        userService.addFriend(secondCreatedUser.getId(), firstCreatedUser.getId());

        assertRelationStatus(firstCreatedUser, secondCreatedUser, FriendRelationStatus.SECOND_REQUESTED_FIRST);
    }

    @Test
    void shouldReturnFriendsStatusWhenFriendshipConfirmed() {
        User firstCreatedUser = saveUser(createValidUser());
        User secondCreatedUser = saveUser(createSecondValidUser());

        userService.addFriend(firstCreatedUser.getId(), secondCreatedUser.getId());
        userService.addFriend(secondCreatedUser.getId(), firstCreatedUser.getId());

        assertRelationStatus(firstCreatedUser, secondCreatedUser, FriendRelationStatus.FRIENDS);
    }

    @Test
    void shouldConfirmFriendshipWhenBothUsersAddEachOther() {
        User firstCreatedUser = saveUser(createValidUser());
        User secondCreatedUser = saveUser(createSecondValidUser());

        userService.addFriend(firstCreatedUser.getId(), secondCreatedUser.getId());
        userService.addFriend(secondCreatedUser.getId(), firstCreatedUser.getId());

        Set<User> firstUserFriends = userService.findFriends(firstCreatedUser.getId());
        Set<User> secondUserFriends = userService.findFriends(secondCreatedUser.getId());

        assertThat(firstUserFriends)
                .as("Второй пользователь должен добавиться в друзья первому")
                .extracting(User::getId)
                .containsExactly(secondCreatedUser.getId());
        assertThat(secondUserFriends)
                .as("Первый пользователь должен добавиться в друзья второму")
                .extracting(User::getId)
                .containsExactly(firstCreatedUser.getId());
    }

    @Test
    void shouldNotConfirmFriendshipWhenSameRequestAddedTwice() {
        User firstCreatedUser = saveUser(createValidUser());
        User secondCreatedUser = saveUser(createSecondValidUser());

        userService.addFriend(firstCreatedUser.getId(), secondCreatedUser.getId());
        userService.addFriend(firstCreatedUser.getId(), secondCreatedUser.getId());

        assertThat(userService.findFriends(firstCreatedUser.getId()))
                .as("Исходящая заявка должна быть видна в публичном списке друзей отправителя")
                .extracting(User::getId)
                .containsExactly(secondCreatedUser.getId());
        assertThat(userService.findFriends(secondCreatedUser.getId()))
                .as("Повторная заявка от первого пользователя не должна создавать дружбу для второго")
                .isEmpty();
        assertThat(userService.findRelationStatus(firstCreatedUser.getId(), secondCreatedUser.getId()).getStatus())
                .as("Между пользователями должна остаться только заявка первого второму")
                .isEqualTo(FriendRelationStatus.FIRST_REQUESTED_SECOND);
    }

    @Test
    void shouldRemoveConfirmedFriendshipAndKeepReverseRequest() {
        User firstCreatedUser = saveUser(createValidUser());
        User secondCreatedUser = saveUser(createSecondValidUser());

        userService.addFriend(firstCreatedUser.getId(), secondCreatedUser.getId());
        userService.addFriend(secondCreatedUser.getId(), firstCreatedUser.getId());
        userService.removeFriend(firstCreatedUser.getId(), secondCreatedUser.getId());

        assertThat(userService.findFriends(firstCreatedUser.getId()))
                .as("Количество друзей у первого пользователя должно быть 0")
                .isEmpty();
        assertThat(userService.findFriends(secondCreatedUser.getId()))
                .as("После удаления дружбы обратная заявка должна быть видна второму пользователю")
                .extracting(User::getId)
                .containsExactly(firstCreatedUser.getId());
        assertThat(userService.findRelationStatus(firstCreatedUser.getId(), secondCreatedUser.getId()).getStatus())
                .as("После удаления дружбы второй пользователь должен остаться подписчиком первого")
                .isEqualTo(FriendRelationStatus.SECOND_REQUESTED_FIRST);
    }

    @Test
    void shouldCancelOutgoingFriendRequest() {
        User firstCreatedUser = saveUser(createValidUser());
        User secondCreatedUser = saveUser(createSecondValidUser());

        userService.addFriend(firstCreatedUser.getId(), secondCreatedUser.getId());
        userService.removeFriend(firstCreatedUser.getId(), secondCreatedUser.getId());

        assertRelationStatus(firstCreatedUser, secondCreatedUser, FriendRelationStatus.NO_RELATION);
        assertThat(userService.findFriends(firstCreatedUser.getId()))
                .as("Отменённая заявка не должна превращаться в дружбу")
                .isEmpty();
    }

    @Test
    void shouldNotChangeIncomingFriendRequestWhenRemovingFriend() {
        User firstCreatedUser = saveUser(createValidUser());
        User secondCreatedUser = saveUser(createSecondValidUser());

        userService.addFriend(secondCreatedUser.getId(), firstCreatedUser.getId());
        userService.removeFriend(firstCreatedUser.getId(), secondCreatedUser.getId());

        assertRelationStatus(firstCreatedUser, secondCreatedUser, FriendRelationStatus.SECOND_REQUESTED_FIRST);
        assertThat(userService.findFriends(secondCreatedUser.getId()))
                .as("Входящая для первого заявка остаётся исходящей для второго")
                .extracting(User::getId)
                .containsExactly(firstCreatedUser.getId());
    }

    @Test
    void shouldGetUserFriends() {
        User firstCreatedUser = saveUser(createValidUser());
        User secondCreatedUser = saveUser(createSecondValidUser());
        User thirdCreatedUser = saveUser(createThirdValidUser());

        userService.addFriend(firstCreatedUser.getId(), secondCreatedUser.getId());
        userService.addFriend(secondCreatedUser.getId(), firstCreatedUser.getId());
        userService.addFriend(firstCreatedUser.getId(), thirdCreatedUser.getId());
        userService.addFriend(thirdCreatedUser.getId(), firstCreatedUser.getId());

        Set<User> resultFriends = userService.findFriends(firstCreatedUser.getId());

        assertThat(resultFriends)
                .as("Список друзей должен состоять ровно из пользователей с ID второго и третьего")
                .extracting(User::getId)
                .containsExactlyInAnyOrder(secondCreatedUser.getId(), thirdCreatedUser.getId());
    }

    @Test
    void shouldGetCommonFriends() {
        User firstCreatedUser = saveUser(createValidUser());
        User secondCreatedUser = saveUser(createSecondValidUser());
        User thirdCreatedUser = saveUser(createThirdValidUser());

        userService.addFriend(firstCreatedUser.getId(), thirdCreatedUser.getId());
        userService.addFriend(thirdCreatedUser.getId(), firstCreatedUser.getId());
        userService.addFriend(secondCreatedUser.getId(), thirdCreatedUser.getId());
        userService.addFriend(thirdCreatedUser.getId(), secondCreatedUser.getId());

        Set<User> commonFriends = userService.findCommonFriends(firstCreatedUser.getId(), secondCreatedUser.getId());

        assertThat(commonFriends)
                .as("Список общих друзей двух пользователей должен содержать только третьего пользователя")
                .extracting(User::getId)
                .containsExactlyInAnyOrder(thirdCreatedUser.getId());
    }

    @Test
    void shouldThrowValidationExceptionWhenUserAddsSelfAsFriend() {
        User firstCreatedUser = saveUser(createValidUser());
        Long userId = firstCreatedUser.getId();

        assertThatThrownBy(() -> userService.addFriend(userId, userId))
                .isInstanceOf(ValidationException.class)
                .hasMessage(addSelfAsFriendMessage(userId));
    }

    @Test
    void shouldThrowValidationExceptionWhenUserRemovesSelfFromFriends() {
        User firstCreatedUser = saveUser(createValidUser());
        Long userId = firstCreatedUser.getId();

        assertThatThrownBy(() -> userService.removeFriend(userId, userId))
                .isInstanceOf(ValidationException.class)
                .hasMessage(removeSelfFromFriendsMessage(userId));
    }

    @Test
    void shouldThrowNotFoundExceptionWhenUserForAddFriendDoesNotExist() {
        User firstCreatedUser = saveUser(createValidUser());
        Long existingFriendId = firstCreatedUser.getId();

        assertThatThrownBy(() -> userService.addFriend(NON_EXISTENT_USER_ID, existingFriendId))
                .isInstanceOf(NotFoundException.class)
                .hasMessage(userNotFoundMessage(NON_EXISTENT_USER_ID));
    }

    @Test
    void shouldThrowNotFoundExceptionWhenFriendForAddFriendDoesNotExist() {
        User firstCreatedUser = saveUser(createValidUser());
        Long userId = firstCreatedUser.getId();

        assertThatThrownBy(() -> userService.addFriend(userId, NON_EXISTENT_USER_ID))
                .isInstanceOf(NotFoundException.class)
                .hasMessage(userNotFoundMessage(NON_EXISTENT_USER_ID));
    }

    @Test
    void shouldThrowNotFoundExceptionWhenFriendIdIsNegative() {
        User firstCreatedUser = saveUser(createValidUser());
        Long userId = firstCreatedUser.getId();
        Long negativeFriendId = -1L;

        assertThatThrownBy(() -> userService.addFriend(userId, negativeFriendId))
                .isInstanceOf(NotFoundException.class)
                .hasMessage(userNotFoundMessage(negativeFriendId));
    }

    @Test
    void shouldThrowNotFoundExceptionWhenUserForRemoveFriendDoesNotExist() {
        User firstCreatedUser = saveUser(createValidUser());
        Long existingFriendId = firstCreatedUser.getId();

        assertThatThrownBy(() -> userService.removeFriend(NON_EXISTENT_USER_ID, existingFriendId))
                .isInstanceOf(NotFoundException.class)
                .hasMessage(userNotFoundMessage(NON_EXISTENT_USER_ID));
    }

    @Test
    void shouldThrowNotFoundExceptionWhenFriendForRemoveFriendDoesNotExist() {
        User firstCreatedUser = saveUser(createValidUser());
        Long userId = firstCreatedUser.getId();

        assertThatThrownBy(() -> userService.removeFriend(userId, NON_EXISTENT_USER_ID))
                .isInstanceOf(NotFoundException.class)
                .hasMessage(userNotFoundMessage(NON_EXISTENT_USER_ID));
    }

    @Test
    void shouldDeleteUser() {
        User firstCreatedUser = saveUser(createValidUser());
        Long userId = firstCreatedUser.getId();

        userService.delete(userId);

        assertThatThrownBy(() -> userService.findById(userId))
                .isInstanceOf(NotFoundException.class)
                .hasMessage(userNotFoundMessage(userId));
    }

    /**
     * Удаление несуществующего пользователя должно кидать NotFoundException, а не проходить молча.
     */
    @Test
    void shouldThrowNotFoundExceptionWhenDeleteUserDoesNotExist() {
        assertThatThrownBy(() -> userService.delete(NON_EXISTENT_USER_ID))
                .isInstanceOf(NotFoundException.class)
                .hasMessage(userNotFoundMessage(NON_EXISTENT_USER_ID));
    }

    @Test
    void shouldFindUserById() {
        User firstCreatedUser = saveUser(createValidUser());
        Long userId = firstCreatedUser.getId();

        User foundUser = userService.findById(userId);

        assertThat(foundUser.getId())
                .as("поле Id должно совпадать")
                .isEqualTo(userId);
        assertThat(foundUser.getEmail())
                .as("поле email должно совпадать")
                .isEqualTo(firstCreatedUser.getEmail());
        assertThat(foundUser.getLogin())
                .as("поле login должно совпадать")
                .isEqualTo(firstCreatedUser.getLogin());
        assertThat(foundUser.getName())
                .as("поле name должно совпадать")
                .isEqualTo(firstCreatedUser.getName());
        assertThat(foundUser.getBirthday())
                .as("поле birthday должно совпадать")
                .isEqualTo(firstCreatedUser.getBirthday());
    }

    @Test
    void shouldThrowNotFoundExceptionWhenGetUserFriendsUserDoesNotExist() {
        assertThatThrownBy(() -> userService.findFriends(NON_EXISTENT_USER_ID))
                .isInstanceOf(NotFoundException.class)
                .hasMessage(userNotFoundMessage(NON_EXISTENT_USER_ID));
    }

    @Test
    void shouldThrowNotFoundExceptionWhenGetCommonFriendsFirstUserDoesNotExist() {
        User existingUser = saveUser(createValidUser());

        assertThatThrownBy(() -> userService.findCommonFriends(NON_EXISTENT_USER_ID, existingUser.getId()))
                .isInstanceOf(NotFoundException.class)
                .hasMessage(userNotFoundMessage(NON_EXISTENT_USER_ID));
    }

    @Test
    void shouldThrowNotFoundExceptionWhenGetCommonFriendsSecondUserDoesNotExist() {
        User existingUser = saveUser(createValidUser());

        assertThatThrownBy(() -> userService.findCommonFriends(existingUser.getId(), NON_EXISTENT_USER_ID))
                .isInstanceOf(NotFoundException.class)
                .hasMessage(userNotFoundMessage(NON_EXISTENT_USER_ID));
    }
}

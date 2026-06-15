package ru.yandex.practicum.filmorate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.service.UserService;
import ru.yandex.practicum.filmorate.storage.user.InMemoryUserStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class UserServiceTest {
    private UserService userService;

    private static final Long NON_EXISTENT_USER_ID = 999L;

    private static final String ID_REQUIRED_MESSAGE = "Id должен быть указан";

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
        userService = new UserService(userStorage);
    }

    private User createUser(String email, String login, String name, LocalDate birthday) {
        return User.builder()
                .email(email)
                .login(login)
                .name(name)
                .birthday(birthday)
                .build();
    }

    private User createValidUser() {
        return createUser(VALID_EMAIL, VALID_LOGIN, VALID_NAME, VALID_BIRTHDAY);
    }

    private User createSecondValidUser() {
        return createUser(MORPHEUS_EMAIL, MORPHEUS_LOGIN, MORPHEUS_NAME, MORPHEUS_BIRTHDAY);
    }

    private User createThirdValidUser() {
        return createUser(TRINITY_EMAIL, TRINITY_LOGIN, TRINITY_NAME, TRINITY_BIRTHDAY);
    }

    private User saveUser(User user) {
        return userService.createUser(user);
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
        assertThat(userService.findAllUsers())
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
        assertThat(userService.findAllUsers())
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
        assertThat(userService.findAllUsers())
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

        User updatedUser = userService.updateUser(userForUpdate);

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
        assertThat(userService.findAllUsers())
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

        User updatedUser = userService.updateUser(userForUpdate);

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
        assertThat(userService.findAllUsers())
                .as("Ожидается общее количество пользователей 1")
                .hasSize(1);
    }

    @Test
    void shouldThrowValidationExceptionWhenUserIdIsZero() {
        User createdUser = saveUser(createValidUser());

        User shadowUser = createValidUser();
        shadowUser.setId(0L);

        assertThatThrownBy(() -> userService.updateUser(shadowUser))
                .isInstanceOf(ValidationException.class)
                .hasMessage(ID_REQUIRED_MESSAGE);

        assertThat(userService.findAllUsers())
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

        assertThatThrownBy(() -> userService.updateUser(shadowUser))
                .isInstanceOf(ValidationException.class)
                .hasMessage(ID_REQUIRED_MESSAGE);

        assertThat(userService.findAllUsers())
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

        assertThatThrownBy(() -> userService.updateUser(shadowUser))
                .isInstanceOf(NotFoundException.class)
                .hasMessage(userNotFoundMessage(NON_EXISTENT_USER_ID));

        assertThat(userService.findAllUsers())
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
        assertThat(userService.findAllUsers())
                .as("Размер списка ожидается 2")
                .hasSize(2);
    }

    @Test
    void shouldAddFriendBothWays() {
        User firstCreatedUser = saveUser(createValidUser());
        User secondCreatedUser = saveUser(createSecondValidUser());

        userService.addFriend(firstCreatedUser.getId(), secondCreatedUser.getId());

        User updatedFirstUser = userService.findUserById(firstCreatedUser.getId());
        User updatedSecondUser = userService.findUserById(secondCreatedUser.getId());

        assertThat(updatedFirstUser.getFriends())
                .as("Второй пользователь должен добавиться в друзья первому")
                .contains(secondCreatedUser.getId());
        assertThat(updatedSecondUser.getFriends())
                .as("Первый пользователь должен добавиться в друзья второму")
                .contains(firstCreatedUser.getId());
        assertThat(updatedFirstUser.getFriends())
                .as("Количество друзей у первого пользователя должно быть 1")
                .hasSize(1);
        assertThat(updatedSecondUser.getFriends())
                .as("Количество друзей у второго пользователя должно быть 1")
                .hasSize(1);
    }

    @Test
    void shouldNotDuplicateFriendWhenAddedTwice() {
        User firstCreatedUser = saveUser(createValidUser());
        User secondCreatedUser = saveUser(createSecondValidUser());

        userService.addFriend(firstCreatedUser.getId(), secondCreatedUser.getId());
        userService.addFriend(firstCreatedUser.getId(), secondCreatedUser.getId());

        User updatedFirstUser = userService.findUserById(firstCreatedUser.getId());
        User updatedSecondUser = userService.findUserById(secondCreatedUser.getId());

        assertThat(updatedFirstUser.getFriends())
                .as("Второй пользователь должен добавиться в друзья первому один раз")
                .contains(secondCreatedUser.getId());
        assertThat(updatedSecondUser.getFriends())
                .as("Первый пользователь должен добавиться в друзья второму один раз")
                .contains(firstCreatedUser.getId());
        assertThat(updatedFirstUser.getFriends())
                .as("Количество друзей у первого пользователя должно быть 1")
                .hasSize(1);
        assertThat(updatedSecondUser.getFriends())
                .as("Количество друзей у второго пользователя должно быть 1")
                .hasSize(1);
    }

    @Test
    void shouldRemoveFriendBothWays() {
        User firstCreatedUser = saveUser(createValidUser());
        User secondCreatedUser = saveUser(createSecondValidUser());

        userService.addFriend(firstCreatedUser.getId(), secondCreatedUser.getId());
        userService.removeFriend(firstCreatedUser.getId(), secondCreatedUser.getId());

        User updatedFirstUser = userService.findUserById(firstCreatedUser.getId());
        User updatedSecondUser = userService.findUserById(secondCreatedUser.getId());

        assertThat(updatedFirstUser.getFriends())
                .as("Количество друзей у первого пользователя должно быть 0")
                .isEmpty();
        assertThat(updatedSecondUser.getFriends())
                .as("Количество друзей у второго пользователя должно быть 0")
                .isEmpty();
    }

    @Test
    void shouldGetUserFriends() {
        User firstCreatedUser = saveUser(createValidUser());
        User secondCreatedUser = saveUser(createSecondValidUser());
        User thirdCreatedUser = saveUser(createThirdValidUser());

        userService.addFriend(firstCreatedUser.getId(), secondCreatedUser.getId());
        userService.addFriend(firstCreatedUser.getId(), thirdCreatedUser.getId());

        Set<User> resultFriends = userService.getUserFriends(firstCreatedUser.getId());

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
        userService.addFriend(secondCreatedUser.getId(), thirdCreatedUser.getId());

        Set<User> commonFriends = userService.getCommonFriends(firstCreatedUser.getId(), secondCreatedUser.getId());

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

        userService.deleteUser(userId);

        assertThatThrownBy(() -> userService.findUserById(userId))
                .isInstanceOf(NotFoundException.class)
                .hasMessage(userNotFoundMessage(userId));
    }

    @Test
    void shouldFindUserById() {
        User firstCreatedUser = saveUser(createValidUser());
        Long userId = firstCreatedUser.getId();

        User foundUser = userService.findUserById(userId);

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
        assertThatThrownBy(() -> userService.getUserFriends(NON_EXISTENT_USER_ID))
                .isInstanceOf(NotFoundException.class)
                .hasMessage(userNotFoundMessage(NON_EXISTENT_USER_ID));
    }

    @Test
    void shouldThrowNotFoundExceptionWhenGetCommonFriendsFirstUserDoesNotExist() {
        User existingUser = saveUser(createValidUser());

        assertThatThrownBy(() -> userService.getCommonFriends(NON_EXISTENT_USER_ID, existingUser.getId()))
                .isInstanceOf(NotFoundException.class)
                .hasMessage(userNotFoundMessage(NON_EXISTENT_USER_ID));
    }

    @Test
    void shouldThrowNotFoundExceptionWhenGetCommonFriendsSecondUserDoesNotExist() {
        User existingUser = saveUser(createValidUser());

        assertThatThrownBy(() -> userService.getCommonFriends(existingUser.getId(), NON_EXISTENT_USER_ID))
                .isInstanceOf(NotFoundException.class)
                .hasMessage(userNotFoundMessage(NON_EXISTENT_USER_ID));
    }
}

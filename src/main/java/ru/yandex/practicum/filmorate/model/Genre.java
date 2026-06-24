package ru.yandex.practicum.filmorate.model;

public enum Genre {
    COMEDY("Комедия"),
    DRAMA("Драма"),
    MELODRAMA("Мелодрама"),
    THRILLER("Триллер"),
    HORROR("Ужасы"),
    ACTION("Боевик"),
    ADVENTURE("Приключения"),
    SCI_FI("Фантастика"),
    FANTASY("Фэнтези"),
    DETECTIVE("Детектив"),
    CRIME("Криминал"),
    DOCUMENTARY("Документальный"),
    BIOGRAPHY("Биография"),
    HISTORY("Исторический"),
    WAR("Военный"),
    WESTERN("Вестерн"),
    ANIMATION("Мультфильм"),
    FAMILY("Семейный"),
    MUSICAL("Мюзикл"),
    SPORT("Спорт");

    private final String russianName;

    Genre(String russianName) {
        this.russianName = russianName;
    }

    public String getRussianName() {
        return russianName;
    }

    @Override
    public String toString() {
        return russianName;
    }
}

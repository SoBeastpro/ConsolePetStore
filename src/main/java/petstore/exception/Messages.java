package petstore.exception;

public final class Messages {
    public static final String CLIENT_NAME_REQUIRED = "Укажите ФИО клиента.";
    public static final String CLIENT_NAME_LENGTH = "ФИО не длиннее 100 символов.";
    public static final String CLIENT_PHONE_REQUIRED = "Укажите телефон клиента.";
    public static final String CLIENT_PHONE_LENGTH = "Телефон не длиннее 20 символов.";
    public static final String CLIENT_EMAIL_REQUIRED = "Укажите email клиента.";
    public static final String CLIENT_EMAIL_LENGTH = "Email не длиннее 100 символов.";
    public static final String CLIENT_PHONE_DUPLICATE = "Клиент с таким телефоном уже существует.";
    public static final String CLIENT_EMAIL_DUPLICATE = "Клиент с таким email уже существует.";
    public static final String CLIENT_DUPLICATE = "Клиент с таким телефоном или email уже существует.";
    public static final String CLIENT_HAS_RESERVATIONS = "Нельзя удалить клиента, у которого есть заявки.";
    public static final String CLIENT_NOT_SELECTED = "Выберите клиента из списка.";
    public static final String CLIENT_MISSING = "Нельзя указать несуществующего клиента.";
    public static final String ANIMAL_NAME_REQUIRED = "Укажите кличку животного.";
    public static final String ANIMAL_NAME_LENGTH = "Кличка не длиннее 100 символов.";
    public static final String COMMENT_LENGTH = "Комментарий не длиннее 500 символов.";
    public static final String DATE_REQUIRED = "Укажите дату создания заявки.";
    public static final String DATE_INVALID = "Укажите дату в формате ГГГГ-ММ-ДД.";
    public static final String STATUS_INVALID = "Нельзя установить некорректный статус.";
    public static final String SEARCH_EMPTY = "Нельзя выполнить поиск: пустая строка.";
    public static final String DATE_RANGE_INCOMPLETE = "Укажите обе даты диапазона: начало и конец.";
    public static final String DATE_RANGE_INVALID = "Нельзя выполнить фильтрацию: некорректный диапазон дат.";
    public static final String ROW_NOT_SELECTED = "Сначала выберите запись в таблице.";
    public static final String DATA_ACCESS = "Не удалось выполнить операцию с базой данных.";
    public static final String DB_CONNECTION = "Не удалось подключиться к базе данных. Проверьте, что PostgreSQL запущен.";
    public static final String EXPORT_FAILED = "Не удалось сохранить файл.";

    private Messages() {
    }

    public static String clientNotFound(long id) {
        return "Клиент с ID " + id + " не найден.";
    }

    public static String reservationNotFound(long id) {
        return "Заявка с ID " + id + " не найдена.";
    }

    public static String statusTransition(String current, String next) {
        return "Нельзя перевести заявку из статуса «" + current + "» в «" + next + "».";
    }
}

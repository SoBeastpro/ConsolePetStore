package ru.mirea.petstore.filter;

import org.springframework.stereotype.Component;
import ru.mirea.petstore.domain.Priority;
import ru.mirea.petstore.domain.ReservationStatus;
import ru.mirea.petstore.error.BadRequestException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Разбирает человекочитаемый фильтр в типобезопасную модель.
 * Строка пользователя не склеивается с SQL или JPQL.
 */
@Component
public class SmartFilterParser {
    private static final Pattern TOKEN = Pattern.compile("\"([^\"]*)\"|(\\S+)");
    private static final Pattern DEPOSIT = Pattern.compile(
            "^(?:задаток|deposit)(<=|>=|=|<|>)([0-9]+(?:[.,][0-9]{1,2})?)$",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    public ReservationFilter parse(String source) {
        List<String> free = new ArrayList<>();
        String client = null;
        ReservationStatus status = null;
        Boolean needsCare = null;
        Priority priority = null;
        String tag = null;
        Boolean archived = false;
        List<ReservationFilter.DepositConstraint> deposits = new ArrayList<>();

        if (source == null || source.isBlank()) {
            return new ReservationFilter("", null, null, null, null, null, false, List.of());
        }
        if (source.length() > 500) throw new BadRequestException("Строка фильтра слишком длинная");

        Matcher matcher = TOKEN.matcher(source.trim());
        while (matcher.find()) {
            String raw = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            String token = raw.trim();
            if (token.isEmpty()) continue;
            String lower = token.toLowerCase(Locale.ROOT);

            Matcher depositMatcher = DEPOSIT.matcher(lower);
            if (depositMatcher.matches()) {
                BigDecimal value = new BigDecimal(depositMatcher.group(2).replace(',', '.'));
                if (value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(new BigDecimal("1000000.00")) > 0) {
                    throw new BadRequestException("Задаток должен быть от 0 до 1000000");
                }
                deposits.add(new ReservationFilter.DepositConstraint(operator(depositMatcher.group(1)), value));
                continue;
            }

            int colon = token.indexOf(':');
            if (colon > 0) {
                String key = lower.substring(0, colon);
                String value = stripQuotes(token.substring(colon + 1).trim());
                String normalized = value.toLowerCase(Locale.ROOT);
                switch (key) {
                    case "клиент", "client" -> client = value;
                    case "статус", "status" -> status = status(normalized);
                    case "уход", "care" -> needsCare = booleanValue(normalized, "уход");
                    case "приоритет", "priority" -> priority = priority(normalized);
                    case "тег", "tag" -> tag = value;
                    case "архив", "archived" -> archived = archivedValue(normalized);
                    default -> free.add(token);
                }
            } else {
                free.add(token);
            }
        }
        return new ReservationFilter(String.join(" ", free), blankToNull(client), status, needsCare, priority,
                blankToNull(tag), archived, List.copyOf(deposits));
    }

    private static ReservationFilter.Operator operator(String value) {
        return switch (value) {
            case "<" -> ReservationFilter.Operator.LT;
            case "<=" -> ReservationFilter.Operator.LTE;
            case "=" -> ReservationFilter.Operator.EQ;
            case ">=" -> ReservationFilter.Operator.GTE;
            case ">" -> ReservationFilter.Operator.GT;
            default -> throw new BadRequestException("Неизвестный оператор задатка");
        };
    }

    private static ReservationStatus status(String value) {
        return switch (value.replace('-', '_')) {
            case "создана", "created" -> ReservationStatus.CREATED;
            case "подтверждена", "confirmed" -> ReservationStatus.CONFIRMED;
            case "завершена", "completed" -> ReservationStatus.COMPLETED;
            case "отменена", "cancelled", "canceled" -> ReservationStatus.CANCELLED;
            default -> throw new BadRequestException("Неизвестный статус: " + value);
        };
    }

    private static Priority priority(String value) {
        return switch (value) {
            case "не_применяется", "неприменимо", "not_applicable", "n/a" -> Priority.NOT_APPLICABLE;
            case "низкий", "low" -> Priority.LOW;
            case "средний", "medium" -> Priority.MEDIUM;
            case "высокий", "high" -> Priority.HIGH;
            case "критический", "critical" -> Priority.CRITICAL;
            default -> throw new BadRequestException("Неизвестный приоритет: " + value);
        };
    }

    private static Boolean booleanValue(String value, String field) {
        return switch (value) {
            case "да", "yes", "true", "1" -> true;
            case "нет", "no", "false", "0" -> false;
            default -> throw new BadRequestException("Поле «" + field + "» принимает да/нет");
        };
    }

    private static Boolean archivedValue(String value) {
        if (value.equals("все") || value.equals("all")) return null;
        return booleanValue(value, "архив");
    }

    private static String stripQuotes(String value) {
        return value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")
                ? value.substring(1, value.length() - 1) : value;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

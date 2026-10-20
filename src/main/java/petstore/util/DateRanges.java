package petstore.util;

import petstore.exception.BusinessException;
import petstore.exception.Messages;

import java.time.LocalDate;

public final class DateRanges {
    private DateRanges() {
    }

    public static void check(LocalDate from, LocalDate to) {
        if (from == null && to == null) {
            return;
        }
        if (from == null || to == null) {
            throw new BusinessException(Messages.DATE_RANGE_INCOMPLETE);
        }
        if (from.isAfter(to)) {
            throw new BusinessException(Messages.DATE_RANGE_INVALID);
        }
    }
}

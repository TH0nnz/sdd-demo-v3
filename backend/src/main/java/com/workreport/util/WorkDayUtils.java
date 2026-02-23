package com.workreport.util;

import java.time.DayOfWeek;
import java.time.LocalDate;

public final class WorkDayUtils {

    private WorkDayUtils() {
    }

    public static boolean isWorkDay(LocalDate date) {
        DayOfWeek dow = date.getDayOfWeek();
        return dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY;
    }

    public static LocalDate getEditableStartDate(LocalDate today) {
        int workDaysCounted = 1; // today counts as the first work day
        LocalDate date = today;
        while (workDaysCounted < 3) {
            date = date.minusDays(1);
            if (isWorkDay(date)) {
                workDaysCounted++;
            }
        }
        return date;
    }

    public static boolean isEditable(LocalDate workDate, LocalDate today) {
        if (!isWorkDay(workDate)) {
            return false;
        }
        if (workDate.isAfter(today)) {
            return false;
        }
        LocalDate start = getEditableStartDate(today);
        return !workDate.isBefore(start);
    }
}

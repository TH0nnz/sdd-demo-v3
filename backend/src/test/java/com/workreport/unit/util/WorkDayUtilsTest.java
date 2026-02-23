package com.workreport.unit.util;

import com.workreport.util.WorkDayUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class WorkDayUtilsTest {

    @Nested
    @DisplayName("isWorkDay")
    class IsWorkDay {

        @Test
        void monday_isWorkDay() {
            // 2026-02-23 is Monday
            assertThat(WorkDayUtils.isWorkDay(LocalDate.of(2026, 2, 23))).isTrue();
        }

        @Test
        void friday_isWorkDay() {
            assertThat(WorkDayUtils.isWorkDay(LocalDate.of(2026, 2, 27))).isTrue();
        }

        @Test
        void saturday_isNotWorkDay() {
            assertThat(WorkDayUtils.isWorkDay(LocalDate.of(2026, 2, 28))).isFalse();
        }

        @Test
        void sunday_isNotWorkDay() {
            assertThat(WorkDayUtils.isWorkDay(LocalDate.of(2026, 3, 1))).isFalse();
        }
    }

    @Nested
    @DisplayName("getEditableStartDate")
    class GetEditableStartDate {

        @Test
        void monday_shouldGoBackToPreviousThursday() {
            // Monday 2026-02-23 → 3 work days: Mon, Fri, Thu → start = Thu 2026-02-19
            LocalDate monday = LocalDate.of(2026, 2, 23);
            assertThat(WorkDayUtils.getEditableStartDate(monday))
                    .isEqualTo(LocalDate.of(2026, 2, 19));
        }

        @Test
        void tuesday_shouldGoBackToPreviousFriday() {
            // Tuesday 2026-02-24 → 3 work days: Tue, Mon, Fri → start = Fri 2026-02-20
            LocalDate tuesday = LocalDate.of(2026, 2, 24);
            assertThat(WorkDayUtils.getEditableStartDate(tuesday))
                    .isEqualTo(LocalDate.of(2026, 2, 20));
        }

        @Test
        void wednesday_shouldGoBackToMonday() {
            // Wednesday 2026-02-25 → 3 work days: Wed, Tue, Mon → start = Mon 2026-02-23
            LocalDate wednesday = LocalDate.of(2026, 2, 25);
            assertThat(WorkDayUtils.getEditableStartDate(wednesday))
                    .isEqualTo(LocalDate.of(2026, 2, 23));
        }

        @Test
        void thursday_shouldGoBackToTuesday() {
            // Thursday 2026-02-26 → 3 work days: Thu, Wed, Tue → start = Tue 2026-02-24
            LocalDate thursday = LocalDate.of(2026, 2, 26);
            assertThat(WorkDayUtils.getEditableStartDate(thursday))
                    .isEqualTo(LocalDate.of(2026, 2, 24));
        }

        @Test
        void friday_shouldGoBackToWednesday() {
            // Friday 2026-02-27 → 3 work days: Fri, Thu, Wed → start = Wed 2026-02-25
            LocalDate friday = LocalDate.of(2026, 2, 27);
            assertThat(WorkDayUtils.getEditableStartDate(friday))
                    .isEqualTo(LocalDate.of(2026, 2, 25));
        }
    }

    @Nested
    @DisplayName("isEditable")
    class IsEditable {

        @Test
        void today_shouldAlwaysBeEditable() {
            LocalDate today = LocalDate.of(2026, 2, 25); // Wednesday
            assertThat(WorkDayUtils.isEditable(today, today)).isTrue();
        }

        @Test
        void exactly3WorkDaysAgo_shouldBeEditable() {
            // Today is Wednesday 2026-02-25 → start = Monday 2026-02-23
            LocalDate today = LocalDate.of(2026, 2, 25);
            LocalDate threeWorkDaysAgo = LocalDate.of(2026, 2, 23);
            assertThat(WorkDayUtils.isEditable(threeWorkDaysAgo, today)).isTrue();
        }

        @Test
        void fourWorkDaysAgo_shouldNotBeEditable() {
            // Today is Wednesday 2026-02-25 → start = Monday 2026-02-23
            // 4 work days ago = Friday 2026-02-20
            LocalDate today = LocalDate.of(2026, 2, 25);
            LocalDate fourWorkDaysAgo = LocalDate.of(2026, 2, 20);
            assertThat(WorkDayUtils.isEditable(fourWorkDaysAgo, today)).isFalse();
        }

        @Test
        void weekend_shouldNotBeEditable() {
            LocalDate today = LocalDate.of(2026, 2, 25);
            LocalDate saturday = LocalDate.of(2026, 2, 21);
            assertThat(WorkDayUtils.isEditable(saturday, today)).isFalse();
        }

        @Test
        void futureDate_shouldNotBeEditable() {
            LocalDate today = LocalDate.of(2026, 2, 25);
            LocalDate future = LocalDate.of(2026, 2, 26);
            assertThat(WorkDayUtils.isEditable(future, today)).isFalse();
        }

        @Test
        void withinEditableRange_shouldBeEditable() {
            // Today is Wednesday 2026-02-25 → editable range: Mon 23 - Wed 25
            LocalDate today = LocalDate.of(2026, 2, 25);
            LocalDate tuesday = LocalDate.of(2026, 2, 24);
            assertThat(WorkDayUtils.isEditable(tuesday, today)).isTrue();
        }

        @Test
        void mondayToday_thursdayInRange() {
            // Today is Monday 2026-02-23 → editable range: Thu 19 - Mon 23
            LocalDate today = LocalDate.of(2026, 2, 23);
            LocalDate thursday = LocalDate.of(2026, 2, 19);
            assertThat(WorkDayUtils.isEditable(thursday, today)).isTrue();
        }

        @Test
        void mondayToday_wednesdayOutOfRange() {
            // Today is Monday 2026-02-23 → editable range: Thu 19 - Mon 23
            // Wednesday 2026-02-18 is 4 work days ago
            LocalDate today = LocalDate.of(2026, 2, 23);
            LocalDate wednesday = LocalDate.of(2026, 2, 18);
            assertThat(WorkDayUtils.isEditable(wednesday, today)).isFalse();
        }
    }
}

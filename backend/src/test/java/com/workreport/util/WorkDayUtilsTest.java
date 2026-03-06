package com.workreport.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("WorkDayUtils")
class WorkDayUtilsTest {

    // 2025-03-03 Monday, 03-07 Friday, 03-08 Saturday, 03-09 Sunday
    private static final LocalDate MON = LocalDate.of(2025, 3, 3);
    private static final LocalDate TUE = LocalDate.of(2025, 3, 4);
    private static final LocalDate WED = LocalDate.of(2025, 3, 5);
    private static final LocalDate THU = LocalDate.of(2025, 3, 6);
    private static final LocalDate FRI = LocalDate.of(2025, 3, 7);
    private static final LocalDate SAT = LocalDate.of(2025, 3, 8);
    private static final LocalDate SUN = LocalDate.of(2025, 3, 9);

    @Nested
    @DisplayName("isWorkDay")
    class IsWorkDay {

        @Test
        @DisplayName("週一為工作日")
        void weekdayMonday_returnsTrue() {
            assertTrue(WorkDayUtils.isWorkDay(MON));
        }

        @Test
        @DisplayName("週五為工作日")
        void weekdayFriday_returnsTrue() {
            assertTrue(WorkDayUtils.isWorkDay(FRI));
        }

        @Test
        @DisplayName("週六非工作日")
        void saturday_returnsFalse() {
            assertFalse(WorkDayUtils.isWorkDay(SAT));
        }

        @Test
        @DisplayName("週日非工作日")
        void sunday_returnsFalse() {
            assertFalse(WorkDayUtils.isWorkDay(SUN));
        }

        @Test
        @DisplayName("週二週三為工作日")
        void tuesdayWednesday_returnsTrue() {
            assertTrue(WorkDayUtils.isWorkDay(TUE));
            assertTrue(WorkDayUtils.isWorkDay(WED));
        }
    }

    @Nested
    @DisplayName("getEditableStartDate")
    class GetEditableStartDate {

        @Test
        @DisplayName("今天為週一時往前推3個工作天為上週四")
        void whenTodayIsMonday_returnsPreviousThursday() {
            // Mon 3/3: 1=Mon, 2=Fri 2/28, 3=Thu 2/27
            LocalDate start = WorkDayUtils.getEditableStartDate(MON);
            assertEquals(LocalDate.of(2025, 2, 27), start);
        }

        @Test
        @DisplayName("今天為週五時往前推3個工作天為本週三")
        void whenTodayIsFriday_returnsWednesdaySameWeek() {
            // Fri 3/7: 1=Fri, 2=Thu 3/6, 3=Wed 3/5
            LocalDate start = WorkDayUtils.getEditableStartDate(FRI);
            assertEquals(WED, start);
        }

        @Test
        @DisplayName("今天為週三時往前推3個工作天為上週五")
        void whenTodayIsWednesday_returnsPreviousFriday() {
            // Wed 3/5: 1=Wed, 2=Tue 3/4, 3=Mon 3/3
            LocalDate start = WorkDayUtils.getEditableStartDate(WED);
            assertEquals(MON, start);
        }

        @Test
        @DisplayName("今天為週六時往前推3個工作天為上週四（跨週末）")
        void whenTodayIsSaturday_returnsThursdayAcrossWeekend() {
            // Sat 3/8: today 算第 1 天，往回 Fri 3/7(2), Thu 3/6(3)
            LocalDate start = WorkDayUtils.getEditableStartDate(SAT);
            assertEquals(THU, start);
        }

        @Test
        @DisplayName("今天為週日時往前推3個工作天為上週四（跨週末）")
        void whenTodayIsSunday_returnsThursdayAcrossWeekend() {
            // Sun 3/9: 同上，Fri(2), Thu(3)
            LocalDate start = WorkDayUtils.getEditableStartDate(SUN);
            assertEquals(THU, start);
        }

        @Test
        @DisplayName("今天為週二時起算日為上週五")
        void whenTodayIsTuesday_returnsPreviousFriday() {
            // Tue 3/4: 1=Tue, 2=Mon 3/3, 3=Fri 2/28
            LocalDate start = WorkDayUtils.getEditableStartDate(TUE);
            assertEquals(LocalDate.of(2025, 2, 28), start);
        }
    }

    @Nested
    @DisplayName("isEditable")
    class IsEditable {

        @Test
        @DisplayName("工作日且為今天則可編輯")
        void workDateEqualsToday_returnsTrue() {
            assertTrue(WorkDayUtils.isEditable(MON, MON));
            assertTrue(WorkDayUtils.isEditable(FRI, FRI));
        }

        @Test
        @DisplayName("工作日且在可編輯區間內則可編輯")
        void workDateWithinEditableRange_returnsTrue() {
            // today=Fri -> start=Wed, 區間 Wed,Thu,Fri
            assertTrue(WorkDayUtils.isEditable(WED, FRI));
            assertTrue(WorkDayUtils.isEditable(THU, FRI));
            assertTrue(WorkDayUtils.isEditable(FRI, FRI));
        }

        @Test
        @DisplayName("可編輯區間邊界（起算日）可編輯")
        void workDateEqualsEditableStart_returnsTrue() {
            LocalDate start = WorkDayUtils.getEditableStartDate(FRI); // Wed
            assertTrue(WorkDayUtils.isEditable(start, FRI));
        }

        @Test
        @DisplayName("週六不可編輯")
        void workDateSaturday_returnsFalse() {
            assertFalse(WorkDayUtils.isEditable(SAT, FRI));
            assertFalse(WorkDayUtils.isEditable(SAT, SAT));
        }

        @Test
        @DisplayName("週日不可編輯")
        void workDateSunday_returnsFalse() {
            assertFalse(WorkDayUtils.isEditable(SUN, FRI));
            assertFalse(WorkDayUtils.isEditable(SUN, SUN));
        }

        @Test
        @DisplayName("未來日期不可編輯")
        void workDateAfterToday_returnsFalse() {
            assertFalse(WorkDayUtils.isEditable(FRI, MON));  // Fri > Mon
            assertFalse(WorkDayUtils.isEditable(TUE, MON));  // Tue > Mon
        }

        @Test
        @DisplayName("早於可編輯起算日不可編輯")
        void workDateBeforeEditableStart_returnsFalse() {
            // today=Fri -> start=Wed(3/5), Mon 3/3 早於起算日
            assertFalse(WorkDayUtils.isEditable(MON, FRI));
            // today=Mon -> start=Thu 2/27, Wed 2/26 早於起算日
            assertFalse(WorkDayUtils.isEditable(LocalDate.of(2025, 2, 26), MON));
        }

        @Test
        @DisplayName("剛好起算日當天可編輯")
        void workDateExactlyAtStart_returnsTrue() {
            LocalDate today = FRI;
            LocalDate start = WorkDayUtils.getEditableStartDate(today);
            assertTrue(WorkDayUtils.isEditable(start, today));
        }

        @Test
        @DisplayName("今天為週末時過去工作日在區間內仍可編輯")
        void todayIsWeekend_pastWorkDayInRange_returnsTrue() {
            // today=Sat 3/8, start=Thu 3/6 -> Thu,Fri 可編輯
            assertTrue(WorkDayUtils.isEditable(THU, SAT));
            assertTrue(WorkDayUtils.isEditable(FRI, SAT));
        }

        @Test
        @DisplayName("今天為週末時過早的工作日不可編輯")
        void todayIsWeekend_workDateBeforeStart_returnsFalse() {
            // today=Sat 3/8, start=Thu 3/6 -> Wed 3/5 不可編輯
            assertFalse(WorkDayUtils.isEditable(WED, SAT));
        }
    }
}

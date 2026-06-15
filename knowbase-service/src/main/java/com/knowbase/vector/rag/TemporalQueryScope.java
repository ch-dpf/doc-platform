package com.knowbase.vector.rag;



import java.time.LocalDate;

import java.time.YearMonth;

import java.util.List;

import java.util.Optional;

import java.util.stream.IntStream;



/** 从用户问句解析出的时间检索范围。 */

public record TemporalQueryScope(

        Integer year,

        Integer month,

        Integer monthEnd,

        Integer weekOfMonth,

        Integer dayOfMonth,

        List<String> persons,

        boolean completedWorkOnly,

        boolean scoped,

        TemporalParseConfidence confidence) {



    public TemporalQueryScope {

        persons = persons == null ? List.of() : List.copyOf(persons);

    }



    /** 兼容单人员访问。 */

    public String person() {

        return persons.isEmpty() ? null : persons.getFirst();

    }



    public boolean yearScoped() {

        return year != null && month == null;

    }



    public boolean weekScoped() {

        return month != null && dayOfMonth == null && weekOfMonth != null && weekOfMonth > 0;

    }



    public boolean dayScoped() {

        return month != null && dayOfMonth != null && dayOfMonth > 0;

    }



    public boolean preciseDateScoped() {

        return weekScoped() || dayScoped();

    }



    public int effectiveMonthEnd() {

        if (month == null) {

            return 0;

        }

        return monthEnd != null ? monthEnd : month;

    }



    public Optional<LocalDate> rangeStart() {

        if (!scoped || year == null) {

            return Optional.empty();

        }

        if (yearScoped()) {

            return Optional.of(LocalDate.of(year, 1, 1));

        }

        if (month == null) {

            return Optional.empty();

        }

        if (dayScoped()) {

            return Optional.of(LocalDate.of(year, month, dayOfMonth));

        }

        if (weekScoped()) {

            int startDay = (weekOfMonth - 1) * 7 + 1;

            int maxDay = YearMonth.of(year, month).lengthOfMonth();

            return Optional.of(LocalDate.of(year, month, Math.min(startDay, maxDay)));

        }

        return Optional.of(YearMonth.of(year, month).atDay(1));

    }



    public Optional<LocalDate> rangeEnd() {

        if (!scoped || year == null) {

            return Optional.empty();

        }

        if (yearScoped()) {

            return Optional.of(LocalDate.of(year, 12, 31));

        }

        if (month == null) {

            return Optional.empty();

        }

        if (dayScoped()) {

            return Optional.of(LocalDate.of(year, month, dayOfMonth));

        }

        if (weekScoped()) {

            int startDay = (weekOfMonth - 1) * 7 + 1;

            int endDay = Math.min(startDay + 6, YearMonth.of(year, month).lengthOfMonth());

            return Optional.of(LocalDate.of(year, month, endDay));

        }

        int end = effectiveMonthEnd();

        return Optional.of(YearMonth.of(year, end).atEndOfMonth());

    }



    public List<Integer> monthsInRange() {

        if (!scoped || year == null) {

            return List.of();

        }

        if (yearScoped()) {

            return IntStream.rangeClosed(1, 12).boxed().toList();

        }

        if (month == null) {

            return List.of();

        }

        if (preciseDateScoped()) {

            return List.of(month);

        }

        int end = effectiveMonthEnd();

        int start = month;

        if (end < start) {

            int tmp = start;

            start = end;

            end = tmp;

        }

        return IntStream.rangeClosed(start, end).boxed().toList();

    }



    public static TemporalQueryScope none() {

        return new TemporalQueryScope(

                null, null, null, null, null, List.of(), false, false, TemporalParseConfidence.NONE);

    }



    public static TemporalQueryScope scoped(

            int year,

            Integer month,

            Integer monthEnd,

            Integer weekOfMonth,

            Integer dayOfMonth,

            List<String> persons,

            boolean completedWorkOnly,

            TemporalParseConfidence confidence) {

        return new TemporalQueryScope(

                year,

                month,

                monthEnd,

                weekOfMonth,

                dayOfMonth,

                persons != null ? persons : List.of(),

                completedWorkOnly,

                true,

                confidence);

    }



    public static TemporalQueryScope scopedSingleMonth(

            int year, int month, String person, boolean completedWorkOnly, TemporalParseConfidence confidence) {

        List<String> people = person != null && !person.isBlank() ? List.of(person.strip()) : List.of();

        return scoped(year, month, null, null, null, people, completedWorkOnly, confidence);

    }



    public TemporalQueryScope withoutPersons() {

        return new TemporalQueryScope(

                year, month, monthEnd, weekOfMonth, dayOfMonth, List.of(), completedWorkOnly, scoped, confidence);

    }



    public String toSummary() {

        if (!scoped || year == null) {

            return "";

        }

        StringBuilder sb = new StringBuilder();

        sb.append(year).append('年');

        if (yearScoped()) {

            // year-only label

        } else if (month != null && dayScoped()) {

            sb.append(month).append('月').append(dayOfMonth).append('日');

        } else if (month != null && weekScoped()) {

            sb.append(month).append("月第").append(weekOfMonth()).append("周");

        } else if (month != null && effectiveMonthEnd() != month) {

            sb.append(month).append('-').append(effectiveMonthEnd()).append('月');

        } else if (month != null) {

            sb.append(month).append('月');

        }

        if (!persons.isEmpty()) {

            sb.append(" · ").append(String.join("、", persons));

        }

        if (completedWorkOnly) {

            sb.append(" · 已完成工作");

        }

        if (confidence != null && confidence != TemporalParseConfidence.NONE) {

            sb.append(" · 置信度").append(confidence.name());

        }

        return sb.toString();

    }

}



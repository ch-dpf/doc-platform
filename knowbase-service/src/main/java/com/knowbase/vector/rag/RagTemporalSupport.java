package com.knowbase.vector.rag;

import java.time.Year;
import java.time.ZoneId;
import java.util.Optional;

/** 历法/时间锚点类问题（不依赖知识库向量片段）。 */
public final class RagTemporalSupport {

    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Shanghai");

    private RagTemporalSupport() {}

    public static int currentCalendarYear() {
        return Year.now(DEFAULT_ZONE).getValue();
    }

    public static String calendarYearSentence() {
        int year = currentCalendarYear();
        return "当前日历年为 " + year + " 年，因此本对话中的「今年」指 " + year + " 年"
                + "（依据系统日期；勿将库内 2025/ 等历史文件夹误判为「今年」）。";
    }

    public static Optional<String> tryCalendarYearAnswer(String question) {
        if (!RagQuestionAnalyzer.isCalendarYearQuestion(question)) {
            return Optional.empty();
        }
        return Optional.of(
                calendarYearSentence()
                        + "（周报正文通常不会单独写明「今年是哪一年」。）");
    }
}

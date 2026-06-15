package com.knowbase.vector.rag;

import com.knowbase.vector.dto.RagChatMessage;
import com.knowbase.vector.retrieval.LibrarySubmitterIndex;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 从自然语言问句解析年月/人员等工作周报检索范围。 */
public final class RagTemporalQueryParser {

    private static final Pattern PERSON_BEFORE_YEAR =
            Pattern.compile("([\\u4e00-\\u9fff]{2,4}?)\\s*(?:在\\s*)?(20\\d{2})\\s*年"
                    + "|([\\u4e00-\\u9fff]{2,4})(20\\d{2})年");
    private static final Pattern MULTI_PERSON =
            Pattern.compile("([\\u4e00-\\u9fff]{2,4})\\s*(?:和|与|、)\\s*([\\u4e00-\\u9fff]{2,4})");
    private static final Pattern COMPLETED_WORK =
            Pattern.compile(".*(完成|已完成|做了|做过).*(工作|内容|任务|事项).*", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern PRONOUN =
            Pattern.compile("(?:他|她|TA|ta)(?:在)?\\s*(?:\\d{4}\\s*年)?\\s*\\d{1,2}\\s*月");

    private RagTemporalQueryParser() {}

    public static TemporalQueryScope parse(String question, List<RagChatMessage> history) {
        return parse(question, history, null, null);
    }

    public static TemporalQueryScope parse(
            String question, List<RagChatMessage> history, UUID libraryId, LibrarySubmitterIndex submitterIndex) {
        if (question == null || question.isBlank()) {
            return TemporalQueryScope.none();
        }
        String normalized = question.strip();
        RagTemporalTimeParser.ParsedTime time = RagTemporalTimeParser.parse(normalized);
        if (!time.scoped()) {
            return TemporalQueryScope.none();
        }

        PersonParse persons = extractPersons(normalized, history, libraryId, submitterIndex);
        TemporalParseConfidence confidence = mergeConfidence(time.confidence(), persons.confidence());
        boolean completedOnly = COMPLETED_WORK.matcher(normalized).matches();

        return TemporalQueryScope.scoped(
                time.year(),
                time.month(),
                time.monthEnd(),
                time.weekOfMonth(),
                time.dayOfMonth(),
                persons.names(),
                completedOnly,
                confidence);
    }

    private static TemporalParseConfidence mergeConfidence(
            TemporalParseConfidence timeConfidence, TemporalParseConfidence personConfidence) {
        if (timeConfidence == TemporalParseConfidence.NONE) {
            return TemporalParseConfidence.NONE;
        }
        if (timeConfidence == TemporalParseConfidence.MEDIUM || personConfidence == TemporalParseConfidence.LOW) {
            return TemporalParseConfidence.MEDIUM;
        }
        if (personConfidence == TemporalParseConfidence.HIGH) {
            return TemporalParseConfidence.HIGH;
        }
        return timeConfidence;
    }

    private static PersonParse extractPersons(
            String question,
            List<RagChatMessage> history,
            UUID libraryId,
            LibrarySubmitterIndex submitterIndex) {
        LinkedHashSet<String> names = new LinkedHashSet<>();

        Matcher multi = MULTI_PERSON.matcher(question);
        boolean hasMultiPerson = multi.find();
        if (hasMultiPerson) {
            addPerson(names, multi.group(1));
            addPerson(names, multi.group(2));
        }

        if (!hasMultiPerson) {
            Matcher matcher = PERSON_BEFORE_YEAR.matcher(question);
            if (matcher.find()) {
                String person = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
                addPerson(names, person);
            }
        }

        if (submitterIndex != null && libraryId != null) {
            for (String matched : submitterIndex.matchAll(libraryId, question)) {
                addPerson(names, matched);
            }
        }

        String fromProject = RagQuestionAnalyzer.extractNamedEmployeeFromProjectQuestion(question);
        addPerson(names, fromProject);

        if (names.isEmpty() && PRONOUN.matcher(question).find()) {
            addPerson(names, RagQuestionAnalyzer.findNamedEmployeeFromHistory(history));
        } else if (names.isEmpty()) {
            addPerson(names, RagQuestionAnalyzer.findNamedEmployeeFromHistory(history));
        }

        TemporalParseConfidence confidence = TemporalParseConfidence.LOW;
        if (!names.isEmpty()) {
            boolean fromRegex = PERSON_BEFORE_YEAR.matcher(question).find() || MULTI_PERSON.matcher(question).find();
            boolean fromWhitelist = submitterIndex != null && libraryId != null
                    && submitterIndex.matchAll(libraryId, question).stream().anyMatch(names::contains);
            if (fromRegex || fromWhitelist) {
                confidence = TemporalParseConfidence.HIGH;
            } else if (fromProject != null) {
                confidence = TemporalParseConfidence.MEDIUM;
            }
        }

        return new PersonParse(new ArrayList<>(names), confidence);
    }

    private static void addPerson(LinkedHashSet<String> names, String raw) {
        String normalized = normalizePerson(raw);
        if (normalized != null) {
            names.add(normalized);
        }
    }

    private static String normalizePerson(String person) {
        if (person == null || person.isBlank()) {
            return null;
        }
        String normalized = person.strip();
        while (normalized.endsWith("在")) {
            normalized = normalized.substring(0, normalized.length() - 1).strip();
        }
        if (!RagEmployeeNameExtractor.looksLikePersonName(normalized)) {
            return null;
        }
        return normalized;
    }

    private record PersonParse(List<String> names, TemporalParseConfidence confidence) {}
}

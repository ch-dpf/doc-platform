package com.knowbase.vector.rag;

/** 时间范围解析置信度，决定预过滤强度。 */
public enum TemporalParseConfidence {
    /** 未解析出时间范围 */
    NONE,
    /** 人员来自历史指代或弱匹配，仅后过滤 */
    LOW,
    /** 相对时间/季度/人员来自白名单 */
    MEDIUM,
    /** 明确年月 + 人员来自正则或白名单 */
    HIGH
}

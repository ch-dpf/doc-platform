package com.knowbase.vector.chunk;

/** 表格文档模板类型，决定分块前缀注入策略。 */
public enum TabularDocumentProfile {

    /** 内部周报 / 周计划等填报表 */
    WEEKLY_REPORT,
    /** 通用业务表格 */
    GENERIC
}

package com.knowbase.ingestion.adaptive;

/**
 * Semantic role of a spreadsheet row after layout analysis.
 */
public enum TableRowRole {
    /** Title or section heading; usually not vector-indexed. */
    LAYOUT,
    /** Label-value metadata row (department, owner, date). */
    FORM_KV,
    /** Date range or section divider line. */
    SEPARATOR,
    /** Column header row for a tabular data region. */
    HEADER,
    /** Data record under an active header row. */
    DATA,
    /** Fallback: Excel column letters A/B/C. */
    COORDINATE
}

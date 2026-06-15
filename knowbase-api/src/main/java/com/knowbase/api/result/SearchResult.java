package com.knowbase.api.result;

import java.util.List;

public record SearchResult(List<SearchHitResult> hits, int totalCandidates) {}

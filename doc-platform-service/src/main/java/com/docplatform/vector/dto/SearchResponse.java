package com.docplatform.vector.dto;

import java.util.List;

public record SearchResponse(List<SearchHit> hits) {
}

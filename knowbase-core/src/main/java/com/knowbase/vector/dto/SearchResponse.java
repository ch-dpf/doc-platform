package com.knowbase.vector.dto;

import java.util.List;

public record SearchResponse(List<SearchHit> hits) {
}

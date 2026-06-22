package com.knowbase.domain.support;

import java.util.List;

public record PagedList<T>(List<T> items, long total, int page, int size) {
}

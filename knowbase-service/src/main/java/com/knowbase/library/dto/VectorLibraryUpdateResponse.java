package com.knowbase.library.dto;

import java.util.List;

public record VectorLibraryUpdateResponse(VectorLibraryResponse library, List<String> warnings) {}

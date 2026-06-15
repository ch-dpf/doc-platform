package com.knowbase.library.domain;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "知识库状态")
public enum LibraryStatus {
    ACTIVE,
    ARCHIVED
}

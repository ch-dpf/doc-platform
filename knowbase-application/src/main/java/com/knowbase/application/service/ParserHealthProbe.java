package com.knowbase.application.service;

import com.knowbase.api.result.ParserHealthResult;

public interface ParserHealthProbe {

    ParserHealthResult check(String parserCode);

    static ParserHealthProbe noop() {
        return parserCode -> null;
    }
}

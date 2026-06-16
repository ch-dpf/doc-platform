package com.knowbase.library.controller;

import com.knowbase.ingest.parse.ParserEngineRegistry;
import com.knowbase.library.dto.ParserEngineDescriptor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "解析引擎", description = "平台内置文档解析器目录")
@RestController
@RequestMapping("/api/v1/parser-engines")
public class ParserEngineController {

    private final ParserEngineRegistry parserEngineRegistry;

    public ParserEngineController(ParserEngineRegistry parserEngineRegistry) {
        this.parserEngineRegistry = parserEngineRegistry;
    }

    @Operation(summary = "内置解析器列表", description = "供库级解析配置 Tab 选择")
    @GetMapping
    public List<ParserEngineDescriptor> list() {
        return parserEngineRegistry.listDescriptors();
    }
}

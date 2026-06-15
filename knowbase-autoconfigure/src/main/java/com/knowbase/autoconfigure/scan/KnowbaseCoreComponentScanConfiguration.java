package com.knowbase.autoconfigure.scan;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.web.bind.annotation.RestController;

@Configuration
@ComponentScan(
        basePackages = "com.knowbase",
        excludeFilters = {
            @ComponentScan.Filter(type = FilterType.ANNOTATION, classes = RestController.class),
            @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com\\.knowbase\\.autoconfigure\\..*"),
            @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com\\.knowbase\\.facade\\..*")
        })
public class KnowbaseCoreComponentScanConfiguration {}

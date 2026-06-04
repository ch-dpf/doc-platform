package com.docplatform;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@MapperScan({
    "com.docplatform.ingest.mapper",
    "com.docplatform.vector.mapper",
    "com.docplatform.library.mapper"
})
public class DocPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(DocPlatformApplication.class, args);
    }
}

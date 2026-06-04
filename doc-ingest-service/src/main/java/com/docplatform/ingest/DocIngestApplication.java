package com.docplatform.ingest;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@MapperScan("com.docplatform.ingest.mapper")
public class DocIngestApplication {

    public static void main(String[] args) {
        SpringApplication.run(DocIngestApplication.class, args);
    }
}

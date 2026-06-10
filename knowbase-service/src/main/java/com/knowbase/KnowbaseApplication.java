package com.knowbase;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@MapperScan({
    "com.knowbase.ingest.mapper",
    "com.knowbase.vector.mapper",
    "com.knowbase.library.mapper",
    "com.knowbase.chat.mapper"
})
public class KnowbaseApplication {

    public static void main(String[] args) {
        SpringApplication.run(KnowbaseApplication.class, args);
    }
}

package com.docplatform.vector;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.docplatform.vector.mapper")
public class VectorIndexApplication {

    public static void main(String[] args) {
        SpringApplication.run(VectorIndexApplication.class, args);
    }
}

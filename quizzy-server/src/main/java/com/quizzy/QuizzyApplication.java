package com.quizzy;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.quizzy.**.mapper")
public class QuizzyApplication {

    public static void main(String[] args) {
        SpringApplication.run(QuizzyApplication.class, args);
    }
}

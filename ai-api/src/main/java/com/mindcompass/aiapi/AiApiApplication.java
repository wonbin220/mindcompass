// Spring AI 기반 내부 ai-api 서버를 시작하는 메인 애플리케이션입니다.
package com.mindcompass.aiapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AiApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiApiApplication.class, args);
    }
}

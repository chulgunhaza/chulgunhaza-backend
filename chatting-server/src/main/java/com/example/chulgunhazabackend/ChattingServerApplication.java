package com.example.chulgunhazabackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// #99/#101: 최소 부팅 스켈레톤. 실제 채팅 도메인 코드는 #101에서 user-server로부터
// 이전된다 — 지금은 actuator 헬스체크만으로 "떠 있는지"를 증명하는 상태.
@SpringBootApplication
public class ChattingServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChattingServerApplication.class, args);
    }
}

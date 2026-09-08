package com.example.chulgunhazabackend.listener;

import com.example.chulgunhazabackend.config.RabbitMQConfig;
import com.example.chulgunhazabackend.dto.attendance.AttendanceCreateRequestDto;
import com.example.chulgunhazabackend.service.AttendanceService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class AttendanceListener {

    private final AttendanceService attendanceService; // 다형성 수정
    private final RabbitTemplate rabbitTemplate;


    @RabbitListener(queues = RabbitMQConfig.ATTENDANCE_QUEUE_NAME, containerFactory = "simpleRabbitListenerContainerFactory")
    public void attendanceConsume(@Payload AttendanceCreateRequestDto attendanceCreateRequestDto,
                                  Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        try {
            attendanceService.registerAttendance(attendanceCreateRequestDto);
            channel.basicAck(tag, false); // 성공
            System.out.println("디큐 완료");
            // #100: 예전엔 여기 EmployeeException 전용 catch가 있었다 — 사원 존재
            // 확인을 위해 EmployeeRepository를 직접 조회하던 시절의 코드였는데,
            // 이제 employeeNo/employeeName이 컨트롤러 단계(JWT 클레임)에서 이미
            // 확정돼 들어오므로 그 조회 자체가 없어졌다. EmployeeException 타입도
            // user-server 전용이라 이 모듈 클래스패스에 없다.
        }catch (Exception e) {
            // DLQ 이동 — 실패 사유(mqFailMessage)를 메시지에 남겨서 원본 그대로 보내는
            // 게 아니라 직접 재발행한다. 컨테이너가 MANUAL ack 모드라(RabbitMQConfig),
            // 여기서 원본 메시지를 ack 해주지 않으면 채널/컨슈머가 재시작될 때
            // 언채크 상태로 남아있던 이 메시지가 그대로 재전달돼서, DLQ에도 있고
            // 원본 큐에서도 다시 처리되는 중복 처리가 발생했다(#76). DLQ로 옮기는
            // 시점에 원본은 ack로 확정해서 큐에서 제거한다.
            attendanceCreateRequestDto.setMqFailMessage(e.getMessage());
            rabbitTemplate.convertAndSend(RabbitMQConfig.DLX, RabbitMQConfig.A_DLQ, attendanceCreateRequestDto);
            channel.basicAck(tag, false);
            System.out.println("DLQ 이동 : " + e.getMessage());
        }

    }

}



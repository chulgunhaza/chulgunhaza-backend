package com.example.chulgunhazabackend.listener;

import com.example.chulgunhazabackend.dto.chat.ChatMessageCreateRMQDto;
import com.example.chulgunhazabackend.exception.chatException.ChatException;
import com.example.chulgunhazabackend.exception.chatException.ChatExceptionType;
import com.example.chulgunhazabackend.exception.employeeException.EmployeeException;
import com.example.chulgunhazabackend.exception.employeeException.EmployeeExceptionType;
import com.example.chulgunhazabackend.service.ChatMessageService;
import com.example.chulgunhazabackend.service.ChatRabbitMQMessageService;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.IncorrectResultSizeDataAccessException;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ChatMessageListenerTest {

    @Mock
    private ChatMessageService chatMessageService;

    // INFO : #57 — 저장 성공 후 실시간 전달(deliverToReceivers)을 이 리스너가 직접 호출하게
    // 바뀌면서 새로 생긴 의존성. @InjectMocks가 생성자 인자를 채우려면 이 mock이 있어야 한다
    // (없으면 null이 주입돼서 "정상 처리" 테스트가 NPE로 죽는다).
    @Mock
    private ChatRabbitMQMessageService chatRabbitMQMessageService;

    @Mock
    private Channel channel;

    @InjectMocks
    private ChatMessageListener chatMessageListener;

    private ChatMessageCreateRMQDto dto() {
        return new ChatMessageCreateRMQDto(1L, "hello", 10L, LocalDateTime.now());
    }

    @Test
    @DisplayName("예상 못 한 예외(IncorrectResultSizeDataAccessException 등)가 나도 리스너 밖으로 전파되지 않고 nack(no-requeue)로 끝난다")
    void 예상_못한_예외는_전파되지_않고_드롭된다() {
        willThrow(new IncorrectResultSizeDataAccessException(1, 2))
                .given(chatMessageService).saveChatMessage(any());

        org.assertj.core.api.Assertions.assertThatCode(() -> chatMessageListener.saveChatMessage(dto(), channel, 1L))
                .doesNotThrowAnyException();

        // 저장이 실패했으니 실시간 전달은 시도조차 안 해야 한다.
        verify(chatRabbitMQMessageService, never()).deliverToReceivers(any());
    }

    @Test
    @DisplayName("ChatException은 기존과 동일하게 nack(no-requeue)로 처리된다")
    void 채팅_예외는_기존대로_드롭된다() throws Exception {
        willThrow(new ChatException(ChatExceptionType.NOT_FOUND_CHAT_ROOM))
                .given(chatMessageService).saveChatMessage(any());

        chatMessageListener.saveChatMessage(dto(), channel, 2L);

        verify(channel).basicNack(2L, false, false);
        verify(chatRabbitMQMessageService, never()).deliverToReceivers(any());
    }

    @Test
    @DisplayName("사원 예외는 기존과 동일하게 nack(no-requeue)로 처리된다")
    void 사원_예외는_기존대로_드롭된다() throws Exception {
        willThrow(new EmployeeException(EmployeeExceptionType.NOT_EXIST_USER))
                .given(chatMessageService).saveChatMessage(any());

        chatMessageListener.saveChatMessage(dto(), channel, 3L);

        verify(channel).basicNack(3L, false, false);
        verify(chatRabbitMQMessageService, never()).deliverToReceivers(any());
    }

    @Test
    @DisplayName("정상 처리되면 채널을 건드리지 않고, 저장 성공 후 실시간 전달을 호출한다")
    void 정상_처리시_채널을_안_건드리고_실시간_전달을_호출한다() throws Exception {
        ChatMessageCreateRMQDto dto = dto();
        given(chatMessageService.saveChatMessage(dto)).willReturn(1L);

        chatMessageListener.saveChatMessage(dto, channel, 4L);

        verify(channel, never()).basicNack(anyLong(), anyBoolean(), anyBoolean());
        // #57 : 저장이 실제로 성공한 뒤에만 실시간 전달을 호출해야 한다 — 이게 이번에 고친 핵심.
        verify(chatRabbitMQMessageService).deliverToReceivers(dto);
    }
}

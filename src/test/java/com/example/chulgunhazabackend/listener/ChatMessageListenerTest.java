package com.example.chulgunhazabackend.listener;

import com.example.chulgunhazabackend.dto.chat.ChatMessageCreateRMQDto;
import com.example.chulgunhazabackend.exception.chatException.ChatException;
import com.example.chulgunhazabackend.exception.chatException.ChatExceptionType;
import com.example.chulgunhazabackend.exception.employeeException.EmployeeException;
import com.example.chulgunhazabackend.exception.employeeException.EmployeeExceptionType;
import com.example.chulgunhazabackend.service.ChatMessageService;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.IncorrectResultSizeDataAccessException;

import java.time.LocalDateTime;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

/**
 * employee_chatroom에 (employee_id, room_id) 중복 행이 있을 때 실제로 겪었던 장애의 회귀 테스트.
 * saveChatMessage가 {@link IncorrectResultSizeDataAccessException}(예상 못 한 예외)을 던지면
 * 예전엔 아무 catch 블록에도 안 걸려서 그대로 리스너 밖으로 전파됐고, 그 메시지가 매번 같은
 * 이유로 실패하는 "poison message"라 컨테이너가 계속 재전달(requeue)하면서 초당 수백 번씩
 * 무한 재시도하는 루프에 빠졌다(실측: 로그 파일이 몇 초 만에 7900만 줄까지 불어남).
 * 지금은 catch(Exception) 로 받아서 요청한 대로 nack(requeue=false)만 하고 끝나야 한다 —
 * 즉 "두 번째로 채널에 아무것도 더 안 한다"가 아니라 "예외가 리스너 밖으로 전파되지 않는다"가
 * 이 테스트의 핵심이다(전파되면 컨테이너가 그 위에서 또 nack/requeue를 시도해 이중 처리된다).
 */
@ExtendWith(MockitoExtension.class)
class ChatMessageListenerTest {

    @Mock
    private ChatMessageService chatMessageService;

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
                .given(chatMessageService).saveChatMessage(org.mockito.ArgumentMatchers.any());

        org.assertj.core.api.Assertions.assertThatCode(() -> chatMessageListener.saveChatMessage(dto(), channel, 1L))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("ChatException은 기존과 동일하게 nack(no-requeue)로 처리된다")
    void 채팅_예외는_기존대로_드롭된다() throws Exception {
        willThrow(new ChatException(ChatExceptionType.NOT_FOUND_CHAT_ROOM))
                .given(chatMessageService).saveChatMessage(org.mockito.ArgumentMatchers.any());

        chatMessageListener.saveChatMessage(dto(), channel, 2L);

        verify(channel).basicNack(2L, false, false);
    }

    @Test
    @DisplayName("사원 예외는 기존과 동일하게 nack(no-requeue)로 처리된다")
    void 사원_예외는_기존대로_드롭된다() throws Exception {
        willThrow(new EmployeeException(EmployeeExceptionType.NOT_EXIST_USER))
                .given(chatMessageService).saveChatMessage(org.mockito.ArgumentMatchers.any());

        chatMessageListener.saveChatMessage(dto(), channel, 3L);

        verify(channel).basicNack(3L, false, false);
    }

    @Test
    @DisplayName("정상 처리되면 채널을 건드리지 않는다")
    void 정상_처리시_채널을_건드리지_않는다() throws Exception {
        given(chatMessageService.saveChatMessage(org.mockito.ArgumentMatchers.any())).willReturn(1L);

        chatMessageListener.saveChatMessage(dto(), channel, 4L);

        verify(channel, never()).basicNack(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyBoolean(), org.mockito.ArgumentMatchers.anyBoolean());
    }
}

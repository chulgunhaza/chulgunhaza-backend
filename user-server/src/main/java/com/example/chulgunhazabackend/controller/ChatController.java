package com.example.chulgunhazabackend.controller;

import com.example.chulgunhazabackend.dto.Employee.EmployeeCredentialDto;
import com.example.chulgunhazabackend.dto.PageDto;
import com.example.chulgunhazabackend.dto.chat.ChatMessageCreateRequestDto;
import com.example.chulgunhazabackend.dto.chat.ChatMessageListResponseDto;
import com.example.chulgunhazabackend.dto.chat.ChatRoomCreateRequestDto;
import com.example.chulgunhazabackend.dto.chat.ChatRoomListResponseDto;
import com.example.chulgunhazabackend.service.ChatMessageService;
import com.example.chulgunhazabackend.service.ChatRabbitMQMessageService;
import com.example.chulgunhazabackend.service.ChatRoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/v1/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatRoomService chatRoomService;
    private final ChatMessageService chatMessageService;
    private final ChatRabbitMQMessageService chatRabbitMQMessageService;


    // INFO : 채팅방을 생성하기 위한 컨트롤러입니다.
    @PostMapping("/create")
    public ResponseEntity<Long> createRoom(@Valid @RequestBody ChatRoomCreateRequestDto chatRoomCreateRequestDto
                                                , @AuthenticationPrincipal EmployeeCredentialDto employeeCredentialDto){
        return ResponseEntity.ok().body(chatRoomService.saveChatRoom(chatRoomCreateRequestDto, employeeCredentialDto.getId()));
    }

    // INFO : 전체 채팅방을 가져오는 컨트롤러입니다.
    @GetMapping("/find/rooms")
    public ResponseEntity<PageDto<ChatRoomListResponseDto>> findAllByEmployeeId(@AuthenticationPrincipal EmployeeCredentialDto employeeCredentialDto, Pageable pageable){
        return ResponseEntity.ok().body(chatRoomService.getAllChatRoomsByEmployeeId(employeeCredentialDto.getId(), pageable));
    }

    // INFO : 메시지 전송을 위한 컨트롤러 입니다. 큐에 발행만 하고 바로 202로 응답한다 —
    // 실제 저장/실시간 전달은 ChatMessageListener가 비동기로 처리한다(#57).
    // 예전엔 이 응답이 "전송 완료"라는 의미 없는 고정 문자열이었다 — 프론트도 안 씀.
    @PostMapping("/send")
    public ResponseEntity<Void> createMessage(@Valid @RequestBody ChatMessageCreateRequestDto chatMessageCreateRequestDto, @AuthenticationPrincipal EmployeeCredentialDto employeeCredentialDto){
        chatRabbitMQMessageService.sendChatMessage(chatMessageCreateRequestDto, employeeCredentialDto.getId());
        return ResponseEntity.accepted().build();
    }

    // INFO : 채팅내역을 가져오는 컨트롤러 입니다.
    // HACK : 추후 배치 처리 필요.
    @GetMapping("/find/{chatRoomId}")
    public ResponseEntity<PageDto<ChatMessageListResponseDto>> findChatMessage(@AuthenticationPrincipal EmployeeCredentialDto employeeCredentialDto, @PathVariable Long chatRoomId, Pageable pageable){
        return ResponseEntity.ok().body(chatMessageService.getChatMessagesByRoomId(chatRoomId, employeeCredentialDto.getId(), pageable));
    }

    // INFO : 채팅방 나가기 컨트롤러입니다. 내 참여 기록만 지우고 방/메시지는 보존합니다.
    @DeleteMapping("/{chatRoomId}/leave")
    public ResponseEntity<Void> leaveRoom(@AuthenticationPrincipal EmployeeCredentialDto employeeCredentialDto, @PathVariable Long chatRoomId){
        chatRoomService.leaveChatRoom(chatRoomId, employeeCredentialDto.getId());
        return ResponseEntity.noContent().build();
    }

}

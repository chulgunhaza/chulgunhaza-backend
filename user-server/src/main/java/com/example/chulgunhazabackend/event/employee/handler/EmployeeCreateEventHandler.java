package com.example.chulgunhazabackend.event.employee.handler;

import com.example.chulgunhazabackend.event.employee.event.EmployeeCreateEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
public class EmployeeCreateEventHandler {

    @Async
    @TransactionalEventListener(
            classes = EmployeeCreateEvent.class,
            phase = TransactionPhase.AFTER_COMMIT
    )
    public void handleEmployeeCreateEventAfterCommit(EmployeeCreateEvent employeeCreateEvent){
        System.out.println(employeeCreateEvent);
    }


    @Async
    @TransactionalEventListener(
            classes = EmployeeCreateEvent.class,
            phase = TransactionPhase.AFTER_ROLLBACK
    )
    public void handleEmployeeCreateEventAfterRollBack(EmployeeCreateEvent employeeCreateEvent){
        System.out.println(employeeCreateEvent);
        System.out.println("롤백되었습니다.");
    }
}

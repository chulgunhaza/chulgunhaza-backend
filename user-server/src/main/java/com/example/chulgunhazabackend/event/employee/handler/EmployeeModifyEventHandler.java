package com.example.chulgunhazabackend.event.employee.handler;

import com.example.chulgunhazabackend.event.employee.event.EmployeeModifyEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
public class EmployeeModifyEventHandler {

    @Async
    @TransactionalEventListener(
            classes = EmployeeModifyEvent.class,
            phase = TransactionPhase.AFTER_COMMIT
    )
    public void handleEmployeeModifyEventAfterCommit(EmployeeModifyEvent employeeModifyEvent){
        System.out.println(employeeModifyEvent.getMessage());
    }


    @Async
    @TransactionalEventListener(
            classes = EmployeeModifyEvent.class,
            phase = TransactionPhase.AFTER_ROLLBACK
    )
    public void handleEmployeeModifyEventAfterRollBack(EmployeeModifyEvent employeeModifyEvent){
        System.out.println(employeeModifyEvent);
        System.out.println("롤백되었습니다.");
    }



}

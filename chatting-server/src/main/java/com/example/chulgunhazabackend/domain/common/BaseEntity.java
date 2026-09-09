package com.example.chulgunhazabackend.domain.common;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

// #101: user-server/attendance-server의 domain.common.BaseEntity와 동일한 내용을
// 그대로 복제 — 각 서비스가 자기 auditing 베이스를 따로 갖는 패턴(#100 참고).
@MappedSuperclass
@EntityListeners(value = {AuditingEntityListener.class})
@Getter
@ToString
public abstract class BaseEntity {

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;

    @Setter
    @ColumnDefault("false")
    @Column(insertable = false)
    private Boolean delFlag;

    public void delete() {
        this.delFlag = true;
    }

    @PreUpdate
    public void preUpdate(){
        if(this.delFlag != null && this.delFlag){
            this.deletedAt = LocalDateTime.now();
        }
    }
}

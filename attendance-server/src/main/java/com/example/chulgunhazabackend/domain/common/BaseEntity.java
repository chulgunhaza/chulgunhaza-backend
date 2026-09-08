package com.example.chulgunhazabackend.domain.common;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

// #100: user-server의 domain.common.BaseEntity와 동일한 내용을 그대로 복제.
// common 모듈은 JPA 의존성이 없는 순수 라이브러리로 유지하려고(도메인 엔티티를
// 절대 안 넣는 원칙, docs/multi-module-structure.md 참고) 여기 각자 복제해서 둔다 —
// 앞으로 서비스가 늘어나도 서비스마다 자기 auditing 베이스를 따로 갖는 패턴을 유지.
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

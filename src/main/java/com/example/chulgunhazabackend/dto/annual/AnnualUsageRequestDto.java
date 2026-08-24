package com.example.chulgunhazabackend.dto.annual;

import com.example.chulgunhazabackend.domain.annual.AnnualType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

import java.io.Serializable;
import java.time.LocalDate;

@Getter
@AllArgsConstructor
@ToString
public class AnnualUsageRequestDto implements Serializable {

    @NotNull(message = "연차 사용 일자가 누락되었습니다.")
    private LocalDate annualDate;

    @NotNull(message = "연차 종류가 누락되었습니다.")
    private AnnualType annualType;

    private String annualReason;
}

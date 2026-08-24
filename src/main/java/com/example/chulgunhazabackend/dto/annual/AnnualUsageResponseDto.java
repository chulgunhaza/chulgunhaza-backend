package com.example.chulgunhazabackend.dto.annual;

import com.example.chulgunhazabackend.domain.annual.AnnualRecord;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@Getter
@AllArgsConstructor
@ToString
public class AnnualUsageResponseDto {

    private Long annualRecordId;
    private double totalAnnualCount;
    private double useCount;
    private double remainingAnnualCount;

    public static AnnualUsageResponseDto of(AnnualRecord annualRecord, double totalAnnualCount,
                                             double useCount, double remainingAnnualCount) {
        return new AnnualUsageResponseDto(annualRecord.getId(), totalAnnualCount, useCount, remainingAnnualCount);
    }
}

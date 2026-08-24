package com.example.chulgunhazabackend.repository;

import com.example.chulgunhazabackend.domain.annual.AnnualRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AnnualRecordRepository extends JpaRepository<AnnualRecord, Long> {
}

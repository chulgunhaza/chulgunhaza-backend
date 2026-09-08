package com.example.chulgunhazabackend.domain.board;


import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Getter
@ToString
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Category {

    private String categoryName;

}

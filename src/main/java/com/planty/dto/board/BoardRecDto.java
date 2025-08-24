package com.planty.dto.board;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BoardRecDto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private String title;
}

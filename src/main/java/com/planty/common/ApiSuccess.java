package com.planty.common;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Getter;


// 공통 성공 응답 DTO
@JsonPropertyOrder({ "status", "message" })
@Getter @AllArgsConstructor
public class ApiSuccess {
    private final int status;
    private final String message;
}


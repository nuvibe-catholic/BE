package com.umc.nuvibe.global.apiPayLoad.result;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum TribeResultCode implements ResultCode {

    TRIBE_JOIN_SUCCESS(HttpStatus.OK, "TRIBE001", "트라이브 입장 성공"),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}

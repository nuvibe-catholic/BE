package com.umc.nuvibe.global.apiPayLoad.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ArchiveErrorCode implements ErrorCode {

    BOARD_NOT_FOUND(HttpStatus.NOT_FOUND, "ARCHIVE001", "해당 보드를 찾을 수 없습니다."),
    BOARD_IMAGE_ALREADY_EXISTS(HttpStatus.CONFLICT, "ARCHIVE006", "이미 다른 보드에 포함된 이미지입니다."),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}

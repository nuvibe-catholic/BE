package com.umc.nuvibe.global.apiPayLoad.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ImageErrorCode implements ErrorCode {

    IMAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "IMAGE006", "이미지가 존재하지 않습니다."),
    IMAGE_UPLOAD_NOT_COMPLETED(HttpStatus.BAD_REQUEST, "IMAGE010", "해당 이미지 업로드가 완료되지 않았습니다."),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}

package com.genai.exception.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;


@Slf4j
@ControllerAdvice
public class GlobalExceptionsHandler {
    @ResponseBody
    @ExceptionHandler(value = Exception.class)
    public ErrorDto handlerException(Exception e) {
        log.error("Excepted in: ", e);
        return new ErrorDto(HttpStatus.BAD_REQUEST.getReasonPhrase(), e.getMessage());
    }
}

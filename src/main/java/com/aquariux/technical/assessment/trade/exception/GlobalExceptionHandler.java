package com.aquariux.technical.assessment.trade.exception;

import com.aquariux.technical.assessment.trade.dto.response.ErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {

        return ResponseEntity.badRequest().body(new ErrorResponse(ex.getMessage()));

    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalStateException(IllegalStateException ex) {

        return ResponseEntity.badRequest().body(new ErrorResponse(ex.getMessage()));
    }
}
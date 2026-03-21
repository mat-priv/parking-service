package com.mps.parkingservice.exception;


import com.mps.parkingservice.dto.ErrorResponseDto;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @Override
    public ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, String> errorMap = new HashMap<>();

        List<ObjectError> errorList = ex.getAllErrors();

        errorList.forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String fieldMessage = error.getDefaultMessage();
            errorMap.put(fieldName, fieldMessage);
        });
        return new ResponseEntity<>(errorMap, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleGlobalException(Exception ex, WebRequest webRequest) {
        ErrorResponseDto errorResponseDto = new ErrorResponseDto(
            webRequest.getDescription(false),
            HttpStatus.INTERNAL_SERVER_ERROR,
            ex.getMessage(),
            LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponseDto);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleResourceNotFoundException(ResourceNotFoundException ex, WebRequest webRequest) {
        ErrorResponseDto errorResponseDto = new ErrorResponseDto(
            webRequest.getDescription(false),
            HttpStatus.NOT_FOUND,
            ex.getMessage(),
            LocalDateTime.now()
        );
        return new ResponseEntity<>(errorResponseDto, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(VehicleAlreadyParkedException.class)
    public ResponseEntity<ErrorResponseDto> handleVehicleAlreadyParkedException(VehicleAlreadyParkedException ex, WebRequest webRequest) {
        ErrorResponseDto errorResponseDto = new ErrorResponseDto(
            webRequest.getDescription(false),
            HttpStatus.CONFLICT,
            ex.getMessage(),
            LocalDateTime.now()
        );
        return new ResponseEntity<>(errorResponseDto, HttpStatus.CONFLICT);
    }

}

package com.mps.parkingservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resourceName, String fieldName, String value) {
        super(String.format("%s is not found with given input data %s: %s", resourceName, fieldName, value));
    }

    public ResourceNotFoundException(String resourceName, String reason) {
        super(String.format("%s is not found because of: %s", resourceName, reason));
    }
}

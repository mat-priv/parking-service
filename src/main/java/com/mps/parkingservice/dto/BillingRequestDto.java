package com.mps.parkingservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record BillingRequestDto(@NotBlank @Pattern(
    regexp = "^[a-z]{2}[a-z0-9]{5}$",
    message = "Registration must be 2 letters followed by 5 alphanumeric characters in lower case"
) String vehicleReg) {
}

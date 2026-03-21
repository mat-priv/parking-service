package com.mps.parkingservice.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

public record VehicleDto(@NotBlank @Pattern(
    regexp = "^[a-z]{2}[a-z0-9]{5}$",
    message = "Registration must be 2 letters followed by 5 alphanumeric characters in lower case"
) String vehicleReg, @Positive @Max(3) int vehicleType) {
}

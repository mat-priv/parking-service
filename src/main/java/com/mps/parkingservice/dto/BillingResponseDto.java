package com.mps.parkingservice.dto;

import java.time.LocalDateTime;

public record BillingResponseDto(
    String billingId,
    String vehicleReg,
    double vehicleCharge,
    LocalDateTime timeIn,
    LocalDateTime timeOut
) {
}

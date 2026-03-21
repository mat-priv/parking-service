package com.mps.parkingservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

public record ParkingConfigDto(
    @Positive int capacity,
    @Min(0) int extraChargeAmountGbp,
    @Min(0) int extraChargeTimeMin,
    @Positive double rateType1,
    @Positive double rateType2,
    @Positive double rateType3
) {
}

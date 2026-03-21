package com.mps.parkingservice.service;

import org.springframework.stereotype.Service;

@Service
public class PriceCalculationService {

    private final ParkingConfigService parkingConfigService;

    public PriceCalculationService(ParkingConfigService parkingConfigService) {
        this.parkingConfigService = parkingConfigService;
    }

    public double calculatePrice(int minutesParked, int vehicleType) {
        int extraChargeTimeInMinutes = parkingConfigService.getExtraChargeTimeMin();
        int extraChargeAmount = parkingConfigService.getExtraChargeAmountGbp();
        int extraPay = extraChargeTimeInMinutes > 0 ?
            extraChargeAmount * minutesParked / extraChargeTimeInMinutes :
            0;
        double minuteRate = getMinuteRate(vehicleType);
        return minuteRate * minutesParked + extraPay;
    }

    private double getMinuteRate(int vehicleType) {
        return switch (vehicleType) {
            case 1 -> parkingConfigService.getRateType1();
            case 2 -> parkingConfigService.getRateType2();
            case 3 -> parkingConfigService.getRateType3();
            default -> throw new IllegalArgumentException("Invalid vehicle type");
        };
    }
}

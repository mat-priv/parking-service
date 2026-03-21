package com.mps.parkingservice.service;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ParkingConfigService {

    @Getter
    private final int extraChargeAmountGbp;
    @Getter
    private final int extraChargeTimeMin;
    @Getter
    private final double rateType1;
    @Getter
    private final double rateType2;
    @Getter
    private final double rateType3;

    public ParkingConfigService(
        @Value("${app.parking.extra-charge-amount-gbp:1}") int extraChargeAmountGbp,
        @Value("${app.parking.extra-charge-time-min:5}") int extraChargeTimeMin,
        @Value("${app.parking.rate-type1:0.1}") double rateType1,
        @Value("${app.parking.rate-type2:0.2}") double rateType2,
        @Value("${app.parking.rate-type3:0.4}") double rateType3
    ) {
        this.extraChargeAmountGbp = extraChargeAmountGbp;
        this.extraChargeTimeMin = extraChargeTimeMin;
        this.rateType1 = rateType1;
        this.rateType2 = rateType2;
        this.rateType3 = rateType3;
    }
}

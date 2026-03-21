package com.mps.parkingservice.service;

import com.mps.parkingservice.dto.ParkingConfigDto;
import com.mps.parkingservice.model.ParkingSlot;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class ParkingConfigService {

    @Getter
    private int extraChargeAmountGbp;
    @Getter
    private int extraChargeTimeMin;
    @Getter
    private double rateType1;
    @Getter
    private double rateType2;
    @Getter
    private double rateType3;

    private final List<ParkingSlot> parkingSlots;

    public ParkingConfigService(
        @Value("${app.parking.extra-charge-amount-gbp:1}") int extraChargeAmountGbp,
        @Value("${app.parking.extra-charge-time-min:5}") int extraChargeTimeMin,
        @Value("${app.parking.rate-type1:0.1}") double rateType1,
        @Value("${app.parking.rate-type2:0.2}") double rateType2,
        @Value("${app.parking.rate-type3:0.4}") double rateType3,
        List<ParkingSlot> parkingSlots
    ) {
        this.extraChargeAmountGbp = extraChargeAmountGbp;
        this.extraChargeTimeMin = extraChargeTimeMin;
        this.rateType1 = rateType1;
        this.rateType2 = rateType2;
        this.rateType3 = rateType3;
        this.parkingSlots = parkingSlots;
    }

    public ParkingConfigDto getConfig() {
        return new ParkingConfigDto(
            parkingSlots.size(), extraChargeAmountGbp, extraChargeTimeMin,
            rateType1, rateType2, rateType3);
    }

    public synchronized ParkingConfigDto updateConfig(ParkingConfigDto config) {
        this.extraChargeAmountGbp = config.extraChargeAmountGbp();
        this.extraChargeTimeMin = config.extraChargeTimeMin();
        this.rateType1 = config.rateType1();
        this.rateType2 = config.rateType2();
        this.rateType3 = config.rateType3();
        updateCapacity(config.capacity());
        log.info("Parking config updated: capacity={}, extraChargeAmountGbp={}, extraChargeTimeMin={}, rates=[{},{},{}]",
            parkingSlots.size(), extraChargeAmountGbp, extraChargeTimeMin, rateType1, rateType2, rateType3);
        return getConfig();
    }

    private void updateCapacity(int newCapacity) {
        int currentCapacity = parkingSlots.size();
        if (newCapacity > currentCapacity) {
            for (int i = currentCapacity; i < newCapacity; i++) {
                parkingSlots.add(new ParkingSlot(i + 1));
            }
            log.info("Parking capacity increased from {} to {}", currentCapacity, newCapacity);
        } else if (newCapacity < currentCapacity) {
            for (int i = currentCapacity - 1; i >= newCapacity; i--) {
                if (!parkingSlots.get(i).tryIsOccupied()) {
                    parkingSlots.remove(i);
                }
            }
            log.info("Parking capacity adjusted from {} to {} (some slots may be occupied and retained)",
                currentCapacity, parkingSlots.size());
        }
    }
}

package com.mps.parkingservice.service;

import com.mps.parkingservice.dto.ParkingConfigDto;
import com.mps.parkingservice.model.ParkingSlot;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ParkingConfigServiceTest {

    private static final int INITIAL_CAPACITY = 5;
    private static final int INITIAL_EXTRA_CHARGE_AMOUNT = 1;
    private static final int INITIAL_EXTRA_CHARGE_TIME = 5;
    private static final double INITIAL_RATE_TYPE_1 = 0.1;
    private static final double INITIAL_RATE_TYPE_2 = 0.2;
    private static final double INITIAL_RATE_TYPE_3 = 0.4;

    @Nested
    class GetConfig {

        @Test
        void shouldReturnSetValues() {
            // given
            List<ParkingSlot> slots = createParkingSlots();
            ParkingConfigService service = createService(slots);

            // when
            ParkingConfigDto result = service.getConfig();

            // then
            assertEquals(INITIAL_CAPACITY, result.capacity());
            assertEquals(INITIAL_EXTRA_CHARGE_AMOUNT, result.extraChargeAmountGbp());
            assertEquals(INITIAL_EXTRA_CHARGE_TIME, result.extraChargeTimeMin());
            assertEquals(INITIAL_RATE_TYPE_1, result.rateType1());
            assertEquals(INITIAL_RATE_TYPE_2, result.rateType2());
            assertEquals(INITIAL_RATE_TYPE_3, result.rateType3());
        }
    }

    @Nested
    class UpdateConfig {

        @Test
        void shouldUpdateAllFields() {
            // given
            List<ParkingSlot> slots = createParkingSlots();
            ParkingConfigService service = createService(slots);
            ParkingConfigDto newConfig = new ParkingConfigDto(6, 3, 10, 0.15, 0.25, 0.45);

            // when
            ParkingConfigDto updated = service.updateConfig(newConfig);

            // then
            assertEquals(newConfig.capacity(), updated.capacity());
            assertEquals(newConfig.extraChargeAmountGbp(), updated.extraChargeAmountGbp());
            assertEquals(newConfig.extraChargeTimeMin(), updated.extraChargeTimeMin());
            assertEquals(newConfig.rateType1(), updated.rateType1());
            assertEquals(newConfig.rateType2(), updated.rateType2());
            assertEquals(newConfig.rateType3(), updated.rateType3());
        }
    }

    private ParkingConfigService createService(List<ParkingSlot> slots) {
        return new ParkingConfigService(
            INITIAL_EXTRA_CHARGE_AMOUNT,
            INITIAL_EXTRA_CHARGE_TIME,
            INITIAL_RATE_TYPE_1,
            INITIAL_RATE_TYPE_2,
            INITIAL_RATE_TYPE_3,
            slots
        );
    }

    private List<ParkingSlot> createParkingSlots() {
        List<ParkingSlot> slots = new ArrayList<>();
        for (int i = 0; i < INITIAL_CAPACITY; i++) {
            slots.add(new ParkingSlot(i + 1));
        }
        return slots;
    }
}


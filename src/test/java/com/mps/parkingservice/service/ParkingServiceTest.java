package com.mps.parkingservice.service;

import com.mps.parkingservice.dto.*;
import com.mps.parkingservice.exception.ResourceNotFoundException;
import com.mps.parkingservice.exception.VehicleAlreadyParkedException;
import com.mps.parkingservice.mapper.VehicleMapper;
import com.mps.parkingservice.model.ParkingSlot;
import com.mps.parkingservice.model.Vehicle;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ParkingServiceTest {

    private static final int TOTAL_SLOTS = 5;
    private static final String VEHICLE_REG = "dw1234";
    private static final String VEHICLE_REG_OTHER = "ab56789";
    private static final int VEHICLE_TYPE_SMALL = 1;
    private static final double MOCKED_PRICE = 5.0;

    @Mock
    private PriceCalculationService priceCalculationService;

    @Mock
    private VehicleMapper vehicleMapper;

    @Nested
    class GetSpaceInfo {

        @Test
        void shouldReturnCorrectCounts() {
            //given
            int occupiedCount = 3;
            List<ParkingSlot> slots = createAndOccupyParkingSlots(occupiedCount);
            ParkingService parkingService = new ParkingService(priceCalculationService, slots, vehicleMapper);

            //when
            SpaceDto result = parkingService.getSpaceInfo();

            //then
            assertEquals(TOTAL_SLOTS - occupiedCount, result.availableSpaces());
            assertEquals(occupiedCount, result.occupiedSpaces());
        }

        @Test
        void shouldReturnCorrectCountsAfterVehicleRemoved() {
            //given
            List<ParkingSlot> slots = createAndOccupyParkingSlots(2);
            ParkingService parkingService = new ParkingService(priceCalculationService, slots, vehicleMapper);

            slots.getFirst().tryRemoveVehicle();

            //when
            SpaceDto result = parkingService.getSpaceInfo();

            //then
            assertEquals(TOTAL_SLOTS - 1, result.availableSpaces());
            assertEquals(1, result.occupiedSpaces());
        }
    }

    @Nested
    class ParkVehicle {

        @Test
        void shouldParkInNextAvailableSlot() {
            //given
            List<ParkingSlot> slots = createAndOccupyParkingSlots(1);
            ParkingService parkingService = new ParkingService(priceCalculationService, slots, vehicleMapper);

            VehicleDto vehicle = new VehicleDto(VEHICLE_REG_OTHER, VEHICLE_TYPE_SMALL);

            //when
            ParkingInfoDto result = parkingService.parkVehicle(vehicle);

            //then
            assertEquals(VEHICLE_REG_OTHER, result.vehicleReg());
            assertEquals(2, result.spaceNumber());
        }

        @Test
        void shouldMarkSlotAsOccupiedAfterParking() {
            //given
            List<ParkingSlot> slots = createParkingSlots();
            ParkingService parkingService = new ParkingService(priceCalculationService, slots, vehicleMapper);

            VehicleDto vehicleDto = new VehicleDto(VEHICLE_REG, VEHICLE_TYPE_SMALL);
            Vehicle vehicle = new Vehicle(VEHICLE_REG, VEHICLE_TYPE_SMALL);

            when(vehicleMapper.toModel(vehicleDto)).thenReturn(vehicle);

            //when
            parkingService.parkVehicle(vehicleDto);

            //then
            assertTrue(slots.getFirst().tryIsOccupied());
            assertEquals(vehicle, slots.getFirst().tryGetVehicle());
        }

        @Test
        void shouldThrowResourceNotFoundExceptionWhenNoFreeParkingSpaces() {
            //given
            List<ParkingSlot> slots = createAndOccupyParkingSlots(TOTAL_SLOTS);
            ParkingService parkingService = new ParkingService(priceCalculationService, slots, vehicleMapper);

            VehicleDto vehicle = new VehicleDto(VEHICLE_REG_OTHER, VEHICLE_TYPE_SMALL);

            //then
            assertThrows(ResourceNotFoundException.class,
                () -> parkingService.parkVehicle(vehicle));
        }

        @Test
        void shouldThrowVehicleAlreadyParkedExceptionWhenCarIsAlreadyParked() {
            //given
            List<ParkingSlot> slots = createParkingSlots();
            ParkingService parkingService = new ParkingService(priceCalculationService, slots, vehicleMapper);

            VehicleDto vehicleDto = new VehicleDto(VEHICLE_REG, VEHICLE_TYPE_SMALL);
            VehicleDto duplicateDto = new VehicleDto(VEHICLE_REG, VEHICLE_TYPE_SMALL);
            Vehicle vehicle = new Vehicle(VEHICLE_REG, VEHICLE_TYPE_SMALL);

            when(vehicleMapper.toModel(vehicleDto)).thenReturn(vehicle);

            parkingService.parkVehicle(vehicleDto);

            //then
            assertThrows(VehicleAlreadyParkedException.class,
                () -> parkingService.parkVehicle(duplicateDto));
        }
    }

    @Nested
    class BillParking {

        @Test
        void shouldReturnBillingResponse() {
            //given
            List<ParkingSlot> slots = createParkingSlots();
            ParkingService parkingService = new ParkingService(priceCalculationService, slots, vehicleMapper);

            VehicleDto vehicleDto = new VehicleDto(VEHICLE_REG, VEHICLE_TYPE_SMALL);
            Vehicle vehicle = new Vehicle(VEHICLE_REG, VEHICLE_TYPE_SMALL);

            when(vehicleMapper.toModel(vehicleDto)).thenReturn(vehicle);
            when(priceCalculationService.calculatePrice(anyInt(), eq(VEHICLE_TYPE_SMALL))).thenReturn(MOCKED_PRICE);

            parkingService.parkVehicle(vehicleDto);

            //when
            BillingResponseDto result = parkingService.billParking(new BillingRequestDto(VEHICLE_REG));

            //then
            assertNotNull(result.billingId());
            assertEquals(VEHICLE_REG, result.vehicleReg());
            assertEquals(MOCKED_PRICE, result.vehicleCharge());
            assertNotNull(result.timeIn());
            assertNotNull(result.timeOut());
        }

        @Test
        void shouldHaveTimeOutAfterTimeIn() {
            //given
            List<ParkingSlot> slots = createParkingSlots();
            ParkingService parkingService = new ParkingService(priceCalculationService, slots, vehicleMapper);

            VehicleDto vehicleDto = new VehicleDto(VEHICLE_REG, VEHICLE_TYPE_SMALL);
            Vehicle vehicle = new Vehicle(VEHICLE_REG, VEHICLE_TYPE_SMALL);

            when(vehicleMapper.toModel(vehicleDto)).thenReturn(vehicle);
            when(priceCalculationService.calculatePrice(anyInt(), eq(VEHICLE_TYPE_SMALL))).thenReturn(MOCKED_PRICE);

            parkingService.parkVehicle(vehicleDto);


            //when
            BillingResponseDto result = parkingService.billParking(new BillingRequestDto(VEHICLE_REG));

            //then
            assertTrue(result.timeIn().isBefore(result.timeOut()));
        }

        @Test
        void shouldFreeParkingSlotAfterBilling() {
            //given
            List<ParkingSlot> slots = createParkingSlots();
            ParkingService parkingService = new ParkingService(priceCalculationService, slots, vehicleMapper);

            VehicleDto vehicleDto = new VehicleDto(VEHICLE_REG, VEHICLE_TYPE_SMALL);
            Vehicle vehicle = new Vehicle(VEHICLE_REG, VEHICLE_TYPE_SMALL);

            when(vehicleMapper.toModel(vehicleDto)).thenReturn(vehicle);
            when(priceCalculationService.calculatePrice(anyInt(), eq(VEHICLE_TYPE_SMALL))).thenReturn(MOCKED_PRICE);

            parkingService.parkVehicle(vehicleDto);


            //when
            parkingService.billParking(new BillingRequestDto(VEHICLE_REG));

            //then
            assertFalse(slots.getFirst().tryIsOccupied());
            assertNull(slots.getFirst().tryGetVehicle());
        }

        @Test
        void shouldThrowResourceNotFoundExceptionWhenVehicleNotFound() {
            //given
            List<ParkingSlot> slots = createParkingSlots();
            ParkingService parkingService = new ParkingService(priceCalculationService, slots, vehicleMapper);

            //THEN
            assertThrows(ResourceNotFoundException.class,
                () -> parkingService.billParking(new BillingRequestDto(VEHICLE_REG)));
        }
    }

    private List<ParkingSlot> createParkingSlots() {
        List<ParkingSlot> slots = new ArrayList<>();
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            slots.add(new ParkingSlot(i + 1));
        }
        return slots;
    }

    private List<ParkingSlot> createAndOccupyParkingSlots(int occupiedCount) {
        List<ParkingSlot> slots = new ArrayList<>();
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            slots.add(new ParkingSlot(i + 1));
            if (i < occupiedCount) {
                slots.get(i).tryParkVehicle(new Vehicle(VEHICLE_REG + i, VEHICLE_TYPE_SMALL));
            }
        }
        return slots;
    }
}
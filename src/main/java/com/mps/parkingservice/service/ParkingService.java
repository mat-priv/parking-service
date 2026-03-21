package com.mps.parkingservice.service;

import com.mps.parkingservice.dto.*;
import com.mps.parkingservice.exception.ResourceNotFoundException;
import com.mps.parkingservice.exception.VehicleAlreadyParkedException;
import com.mps.parkingservice.mapper.VehicleMapper;
import com.mps.parkingservice.model.ParkingSlot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static java.time.Duration.between;

@Slf4j
@Service
public class ParkingService {

    private final PriceCalculationService priceCalculationService;
    private final List<ParkingSlot> parkingSlots;
    private final VehicleMapper vehicleMapper;

    public ParkingService(PriceCalculationService priceCalculationService, List<ParkingSlot> parkingSlots, VehicleMapper vehicleMapper) {
        this.priceCalculationService = priceCalculationService;
        this.parkingSlots = parkingSlots;
        this.vehicleMapper = vehicleMapper;
    }

    public SpaceDto getSpaceInfo() {
        log.info("Calculating parking space info.");
        int occupiedSpaces = (int) parkingSlots.stream()
            .filter(ParkingSlot::tryIsOccupied)
            .count();
        int availableSpaces = (int) parkingSlots.stream()
            .filter(ps -> ps.tryIsActive() && !ps.tryIsOccupied())
            .count();

        return new SpaceDto(availableSpaces, occupiedSpaces);
    }

    public ParkingInfoDto parkVehicle(VehicleDto vehicleDto) {
        validateIfAlreadyParked(vehicleDto.vehicleReg());
        log.info("Attempting to park vehicle: {}", vehicleDto);
        ParkingSlot parkingSlot = parkingSlots.stream()
            .filter(ps -> ps.tryParkVehicle(vehicleMapper.toModel(vehicleDto)))
            .findFirst()
            .orElseThrow(() -> new ResourceNotFoundException("Parking", "No parking space available"));

        return new ParkingInfoDto(vehicleDto.vehicleReg(), parkingSlot.getSlotNumber(), parkingSlot.tryGetTimeIn());
    }

    public BillingResponseDto billParking(BillingRequestDto billingRequestDto) {
        log.info("Processing billing for vehicle registration: {}", billingRequestDto.vehicleReg());
        ParkingSlot parkingSlot = findParkingSlotByVehicleReg(billingRequestDto.vehicleReg());
        LocalDateTime currentTime = LocalDateTime.now();
        int minutesParked = calculateParkingTime(parkingSlot, currentTime);

        double finalPrice = priceCalculationService.calculatePrice(minutesParked, parkingSlot.tryGetVehicle().vehicleType());
        log.info("Calculated final price: {} for vehicle registration: {}, parked for {} minutes",
            finalPrice, billingRequestDto.vehicleReg(), minutesParked);

        BillingResponseDto billingResponseDto = new BillingResponseDto(
            UUID.randomUUID().toString(),
            billingRequestDto.vehicleReg(),
            finalPrice,
            parkingSlot.tryGetTimeIn(),
            currentTime
        );

        parkingSlot.tryRemoveVehicle();
        return billingResponseDto;
    }

    public void setSlotActiveStatus(int slotNumber, boolean active) {
        ParkingSlot slot = parkingSlots.stream()
            .filter(ps -> ps.getSlotNumber() == slotNumber)
            .findFirst()
            .orElseThrow(() -> new ResourceNotFoundException("ParkingSlot", "slotNumber", String.valueOf(slotNumber)));
        slot.trySetActive(active);
        log.info("Slot {} active status set to {}", slotNumber, active);
    }

    private static int calculateParkingTime(ParkingSlot parkingSlot, LocalDateTime currentTime) {
        return (int) between(parkingSlot.tryGetTimeIn(), currentTime).toMinutes();
    }

    private ParkingSlot findParkingSlotByVehicleReg(String vehicleReg) {
        return filterOccupiedSlotsByRegistration(vehicleReg)
            .findFirst()
            .orElseThrow(() -> new ResourceNotFoundException("ParkingSlot", "vehicleReg", vehicleReg));
    }

    private void validateIfAlreadyParked(String vehicleReg) {
        filterOccupiedSlotsByRegistration(vehicleReg)
            .findAny()
            .ifPresent(ps -> {
                throw new VehicleAlreadyParkedException(vehicleReg, ps.getSlotNumber());
            });
    }

    private Stream<ParkingSlot> filterOccupiedSlotsByRegistration(String vehicleReg) {
        return parkingSlots.stream()
            .filter(ParkingSlot::tryIsOccupied)
            .filter(ps -> ps.tryGetVehicle().vehicleReg().equals(vehicleReg));
    }
}

package com.mps.parkingservice.controller;

import com.mps.parkingservice.dto.*;
import com.mps.parkingservice.service.ParkingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@Slf4j
@RestController
@RequestMapping("/tds-parking/parking")
@Tag(
    name = "Simple car park management API",
    description = "Simple car park management API for determining the number of available and full spaces," +
        " allocating vehicles to the first available space and determining the parking charge on vehicle exit ")
public class ParkingController {

    private final ParkingService parkingService;

    public ParkingController(ParkingService parkingService) {
        this.parkingService = parkingService;
    }

    @Operation(summary = "Get parking space information",
        description = "Get the number of available and full parking spaces in the parking")
    @GetMapping
    public ResponseEntity<SpaceDto> getParkingSpacesInfo() {
        SpaceDto spaceInfo = parkingService.getSpaceInfo();
        log.info("Retrieved parking space info: {}", spaceInfo);
        return ResponseEntity.ok(spaceInfo);
    }

    @Operation(summary = "Park a vehicle in the first available parking lot",
        description = "Park a vehicle in the first available parking lot and return the assigned parking information")
    @PostMapping
    public ResponseEntity<ParkingInfoDto> parkVehicle(@RequestBody @Valid VehicleDto vehicleDto) {
        ParkingInfoDto parkingInfo = parkingService.parkVehicle(vehicleDto);
        log.info("Parked vehicle: {}, assigned parking info: {}", vehicleDto, parkingInfo);
        return ResponseEntity.ok(parkingInfo);
    }

    @Operation(summary = "Calculate parking charge for a vehicle",
        description = "Calculate the parking charge for a vehicle based on the parking duration and return the billing information")
    @PostMapping("/bill")
    public ResponseEntity<BillingResponseDto> billParking(@RequestBody @Valid BillingRequestDto billingRequestDto) {
        BillingResponseDto billingResponse = parkingService.billParking(billingRequestDto);
        log.info("Billing request: {}, billing response: {}", billingRequestDto, billingResponse);
        return ResponseEntity.ok(billingResponse);
    }
}

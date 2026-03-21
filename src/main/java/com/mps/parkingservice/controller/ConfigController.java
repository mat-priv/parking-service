package com.mps.parkingservice.controller;

import com.mps.parkingservice.dto.ParkingConfigDto;
import com.mps.parkingservice.service.ParkingConfigService;
import com.mps.parkingservice.service.ParkingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/tds-parking/config")
@Tag(name = "Parking configuration API",
    description = "Manage parking settings")
public class ConfigController {

    private final ParkingService parkingService;
    private final ParkingConfigService parkingConfigService;

    public ConfigController(ParkingService parkingService, ParkingConfigService parkingConfigService) {
        this.parkingService = parkingService;
        this.parkingConfigService = parkingConfigService;
    }

    @Operation(summary = "Set parking slot active status",
        description = "Activate or deactivate a parking slot. Inactive slots do not accept new vehicles.")
    @PatchMapping("/slots/{slotNumber}/active")
    public ResponseEntity<Void> setSlotActive(
        @PathVariable int slotNumber,
        @RequestParam boolean active
    ) {
        parkingService.setSlotActiveStatus(slotNumber, active);
        log.info("Slot {} active status set to {}", slotNumber, active);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get parking configuration",
        description = "Get the current parking configuration including capacity and charge settings")
    @GetMapping
    public ResponseEntity<ParkingConfigDto> getConfig() {
        ParkingConfigDto config = parkingConfigService.getConfig();
        log.info("Retrieved parking config: {}", config);
        return ResponseEntity.ok(config);
    }

    @Operation(summary = "Update parking configuration",
        description = "Update parking configuration: capacity, extra charge amount (GBP) and extra charge interval (minutes)")
    @PutMapping
    public ResponseEntity<ParkingConfigDto> updateConfig(@RequestBody @Valid ParkingConfigDto configDto) {
        ParkingConfigDto updated = parkingConfigService.updateConfig(configDto);
        log.info("Updated parking config: {}", updated);
        return ResponseEntity.ok(updated);
    }
}

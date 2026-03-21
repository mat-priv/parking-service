package com.mps.parkingservice.dto;

import java.time.LocalDateTime;

public record OccupiedSlotsDto(int slotNumber, VehicleDto vehicle, LocalDateTime timeIn, double price) {
}

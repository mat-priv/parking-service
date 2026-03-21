package com.mps.parkingservice.dto;

import java.time.LocalDateTime;

public record ParkingInfoDto(String vehicleReg, int spaceNumber, LocalDateTime timeIn) {
}

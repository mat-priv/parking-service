package com.mps.parkingservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class VehicleAlreadyParkedException extends RuntimeException {

    public VehicleAlreadyParkedException(String vehicleReg, int slotNumber) {
        super("Vehicle with registration " + vehicleReg + " is already parked in slot " + slotNumber);
    }
}

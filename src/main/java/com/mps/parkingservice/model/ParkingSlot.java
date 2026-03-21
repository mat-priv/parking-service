package com.mps.parkingservice.model;

import lombok.Getter;

import java.time.LocalDateTime;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ParkingSlot {

    private final Lock lock = new ReentrantLock();

    @Getter
    private final int slotNumber;

    private boolean occupied;

    private Vehicle vehicle;

    private LocalDateTime timeIn;

    public ParkingSlot(int slotNumber) {
        this.slotNumber = slotNumber;
    }

    public boolean tryIsOccupied() {
        lock.lock();
        try {
            return occupied;
        } finally {
            lock.unlock();
        }
    }

    public Vehicle tryGetVehicle() {
        lock.lock();
        try {
            return vehicle;
        } finally {
            lock.unlock();
        }
    }

    public LocalDateTime tryGetTimeIn() {
        lock.lock();
        try {
            return timeIn;
        } finally {
            lock.unlock();
        }
    }

    public void tryRemoveVehicle() {
        lock.lock();
        try {
            this.vehicle = null;
            this.timeIn = null;
            this.occupied = false;
        } finally {
            lock.unlock();
        }
    }

    public boolean tryParkVehicle(Vehicle vehicle) {
        lock.lock();
        try {
            if (occupied) {
                return false;
            } else {
                occupied = true;
                this.vehicle = vehicle;
                this.timeIn = LocalDateTime.now();
                return true;
            }
        } finally {
            lock.unlock();
        }
    }
}

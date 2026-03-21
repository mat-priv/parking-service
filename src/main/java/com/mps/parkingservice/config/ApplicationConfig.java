package com.mps.parkingservice.config;


import com.mps.parkingservice.model.ParkingSlot;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class ApplicationConfig {

    @Bean
    public List<ParkingSlot> parkingSlots(@Value("${app.parking.capacity:100}") int slotsCount) {
        List<ParkingSlot> parkingSlots = new ArrayList<>();

        for (int i = 0; i < slotsCount; i++) {
            parkingSlots.add(i, new ParkingSlot(i + 1));
        }
        return parkingSlots;
    }
}

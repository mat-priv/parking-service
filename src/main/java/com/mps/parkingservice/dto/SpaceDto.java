package com.mps.parkingservice.dto;

import java.util.List;

public record SpaceDto(int totalSlots, int availableSpaces, int occupiedSpaces, List<Integer> inactiveSlotNumbers) {
}

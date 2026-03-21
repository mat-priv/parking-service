package com.mps.parkingservice.mapper;


import com.mps.parkingservice.dto.VehicleDto;
import com.mps.parkingservice.model.Vehicle;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface VehicleMapper {

    Vehicle toModel(VehicleDto vehicleDto);

}

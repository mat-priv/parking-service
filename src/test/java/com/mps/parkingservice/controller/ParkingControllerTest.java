package com.mps.parkingservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mps.parkingservice.dto.*;
import com.mps.parkingservice.exception.GlobalExceptionHandler;
import com.mps.parkingservice.exception.ResourceNotFoundException;
import com.mps.parkingservice.exception.VehicleAlreadyParkedException;
import com.mps.parkingservice.service.ParkingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ParkingControllerTest {

    private static final String VEHICLE_1_REG = "ab12345";
    public static final BillingRequestDto BILLING_REQUEST_DTO = new BillingRequestDto(VEHICLE_1_REG);
    private static final VehicleDto VEHICLE_1 = new VehicleDto(VEHICLE_1_REG, 1);

    private static final int AVAILABLE_SPACES = 100;
    private static final int OCCUPIED_SPACES = 20;
    private static final int TOTAL_SLOTS = 120;

    @Mock
    private ParkingService parkingService;

    @InjectMocks
    private ParkingController parkingController;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(parkingController)
            .setControllerAdvice(new GlobalExceptionHandler())
            .setValidator(validator)
            .build();
    }

    @Nested
    @DisplayName("GET /tds-parking/parking")
    class GetParkingSpots {
        @Test
        @DisplayName("Returns number of available and full parking spaces in the parking")
        void return200WithParkingSpotsInfo() throws Exception {
            //Given
            SpaceDto spaceInfo = new SpaceDto(TOTAL_SLOTS, AVAILABLE_SPACES, OCCUPIED_SPACES, List.of());
            //WHEN
            when(parkingService.getSpaceInfo()).thenReturn(spaceInfo);
            //THEN
            mockMvc.perform(get("/tds-parking/parking"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSlots").value(TOTAL_SLOTS))
                .andExpect(jsonPath("$.availableSpaces").value(AVAILABLE_SPACES))
                .andExpect(jsonPath("$.occupiedSpaces").value(OCCUPIED_SPACES));
        }
    }

    @Nested
    @DisplayName("POST /tds-parking/parking")
    class ParkVehicle {
        @Test
        @DisplayName("Parks a vehicle in the first available parking lot and return the assigned parking information")
        void return200WithParkingInfo() throws Exception {
            //GIVEN
            ParkingInfoDto parkingInfoDto = new ParkingInfoDto(VEHICLE_1_REG, 42, LocalDateTime.now());
            //WHEN
            when(parkingService.parkVehicle(VEHICLE_1)).thenReturn(parkingInfoDto);
            //THEN
            mockMvc.perform(post("/tds-parking/parking")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(VEHICLE_1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vehicleReg").value(VEHICLE_1_REG))
                .andExpect(jsonPath("$.spaceNumber").value(42));
        }

        @Test
        @DisplayName("Returns 404 when the is no available parking space")
        void return404WhenNoAvailableParkingSpace() throws Exception {
            //WHEN
            when(parkingService.parkVehicle(VEHICLE_1)).thenThrow(
                new ResourceNotFoundException("Parking", "No parking space available"));
            //Then
            mockMvc.perform(post("/tds-parking/parking")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(VEHICLE_1)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorMessage").value("Parking is not found because of: No parking space available"));
        }

        @Test
        @DisplayName("Returns 409 when the car is already parked")
        void return409WhenCarAlreadyParked() throws Exception {
            //WHEN
            when(parkingService.parkVehicle(VEHICLE_1)).thenThrow(
                new VehicleAlreadyParkedException(VEHICLE_1.vehicleReg(), 88));
            mockMvc.perform(post("/tds-parking/parking")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(VEHICLE_1)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorMessage").value("Vehicle with registration ab12345 is already parked in slot 88"));
        }
    }

    @Nested
    @DisplayName("POST /tds-parking/parking/bill")
    class BillParking {
        @Test
        @DisplayName("Calculate the parking charge for a vehicle and return the billing information")
        void return200WithBillingInfo() throws Exception {
            //GIVEN
            BillingResponseDto billingResponseDto = new BillingResponseDto(
                "dummyBillId",
                VEHICLE_1_REG,
                5.0,
                LocalDateTime.now().minusMinutes(30L),
                LocalDateTime.now()
            );
            //WHEN
            when(parkingService.billParking(BILLING_REQUEST_DTO)).thenReturn(billingResponseDto);
            //THEN
            mockMvc.perform(post("/tds-parking/parking/bill")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(BILLING_REQUEST_DTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.billingId").value("dummyBillId"))
                .andExpect(jsonPath("$.vehicleReg").value(VEHICLE_1_REG))
                .andExpect(jsonPath("$.vehicleCharge").value(5.0));
        }

        @Test
        @DisplayName("Returns 404 when the vehicle is not found in the parking")
        void return404WhenVehicleNotFound() throws Exception {
            //when
            when(parkingService.billParking(BILLING_REQUEST_DTO)).thenThrow(
                new ResourceNotFoundException("ParkingSlot", "vehicleReg", VEHICLE_1_REG));
            //then
            mockMvc.perform(post("/tds-parking/parking/bill")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(BILLING_REQUEST_DTO)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorMessage").value("ParkingSlot is not found with given input data vehicleReg: ab12345"));
        }
    }
}
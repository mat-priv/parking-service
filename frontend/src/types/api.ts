export interface SpaceDto {
  totalSlots: number;
  availableSpaces: number;
  occupiedSpaces: number;
  inactiveSlotNumbers: number[];
}

export interface VehicleDto {
  vehicleReg: string;
  vehicleType: number;
}

export interface OccupiedSlotDto {
  slotNumber: number;
  vehicle: VehicleDto;
  timeIn: string;
  price: number;
}

export interface ParkingInfoDto {
  vehicleReg: string;
  spaceNumber: number;
  timeIn: string;
}

export interface BillingRequestDto {
  vehicleReg: string;
}

export interface BillingResponseDto {
  billingId: string;
  vehicleReg: string;
  vehicleCharge: number;
  timeIn: string;
  timeOut: string;
}

export interface ErrorResponseDto {
  apiPath: string;
  httpStatus: string;
  errorMessage: string;
  errorTime: string;
}

export type ValidationErrorResponse = Record<string, string>;

export interface ParkingConfigDto {
  capacity: number;
  extraChargeAmountGbp: number;
  extraChargeTimeMin: number;
  rateType1: number;
  rateType2: number;
  rateType3: number;
}

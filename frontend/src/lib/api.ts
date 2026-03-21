import type {
  BillingRequestDto,
  BillingResponseDto,
  ErrorResponseDto,
  OccupiedSlotDto,
  ParkingConfigDto,
  ParkingInfoDto,
  SpaceDto,
  ValidationErrorResponse,
  VehicleDto,
} from '../types/api';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL?.trim() ?? '';

export class ApiError extends Error {
  status: number;
  details?: ErrorResponseDto | ValidationErrorResponse | string;

  constructor(message: string, status: number, details?: ErrorResponseDto | ValidationErrorResponse | string) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.details = details;
  }
}

function buildUrl(path: string): string {
  return `${API_BASE_URL}${path}`;
}

function isValidationErrorResponse(value: unknown): value is ValidationErrorResponse {
  return Boolean(value) && typeof value === 'object' && !Array.isArray(value);
}

function isErrorResponseDto(value: unknown): value is ErrorResponseDto {
  return value !== null
    && typeof value === 'object'
    && 'errorMessage' in value
    && 'httpStatus' in value;
}

function normalizeErrorMessage(payload: unknown, response: Response): string {
  if (isErrorResponseDto(payload)) {
    return payload.errorMessage;
  }

  if (isValidationErrorResponse(payload)) {
    return Object.entries(payload)
      .map(([field, message]) => `${field}: ${message}`)
      .join('\n');
  }

  if (typeof payload === 'string' && payload.trim()) {
    return payload;
  }

  return `Request failed with status ${response.status}`;
}

async function parsePayload(response: Response): Promise<unknown> {
  const contentType = response.headers.get('content-type') ?? '';

  if (contentType.includes('application/json')) {
    return response.json();
  }

  const text = await response.text();
  return text ? text : null;
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(buildUrl(path), {
    headers: {
      'Content-Type': 'application/json',
      ...(init?.headers ?? {}),
    },
    ...init,
  });

  const payload = await parsePayload(response);

  if (!response.ok) {
    throw new ApiError(normalizeErrorMessage(payload, response), response.status, payload as ErrorResponseDto | ValidationErrorResponse | string);
  }

  return payload as T;
}

export function getParkingSpaces(): Promise<SpaceDto> {
  return request<SpaceDto>('/tds-parking/parking');
}

export function getOccupiedSlots(): Promise<OccupiedSlotDto[]> {
  return request<OccupiedSlotDto[]>('/tds-parking/parking/occupied');
}

export function parkVehicle(vehicle: VehicleDto): Promise<ParkingInfoDto> {
  return request<ParkingInfoDto>('/tds-parking/parking', {
    method: 'POST',
    body: JSON.stringify(vehicle),
  });
}

export function billVehicle(requestDto: BillingRequestDto): Promise<BillingResponseDto> {
  return request<BillingResponseDto>('/tds-parking/parking/bill', {
    method: 'POST',
    body: JSON.stringify(requestDto),
  });
}

export function getConfig(): Promise<ParkingConfigDto> {
  return request<ParkingConfigDto>('/tds-parking/config');
}

export function updateConfig(config: ParkingConfigDto): Promise<ParkingConfigDto> {
  return request<ParkingConfigDto>('/tds-parking/config', {
    method: 'PUT',
    body: JSON.stringify(config),
  });
}

export function setSlotActive(slotNumber: number, active: boolean): Promise<void> {
  return request<void>(`/tds-parking/config/slots/${slotNumber}/active?active=${active}`, {
    method: 'PATCH',
  });
}


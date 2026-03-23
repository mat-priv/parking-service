import { useCallback, useEffect, useMemo, useState } from 'react';
import './styles.css';
import { ApiError, billVehicle, getConfig, getOccupiedSlots, getParkingSpaces, parkVehicle, setSlotActive, updateConfig } from './lib/api';
import type { BillingResponseDto, OccupiedSlotDto, ParkingConfigDto, ParkingInfoDto, SpaceDto } from './types/api';

const REGISTRATION_PATTERN = /^[a-z]{2}[a-z0-9]{5}$/;
const TEST_REGISTRATION_PREFIX = 'tt';
const TEST_REGISTRATION_SPACE = 36 ** 5;

const VEHICLE_TYPE_OPTIONS = [
  { value: 1, label: 'Type 1 · Small vehicle' },
  { value: 2, label: 'Type 2 · Medium vehicle' },
  { value: 3, label: 'Type 3 · Large vehicle' },
] as const;

interface BulkParkingSummary {
  requestedCount: number;
  parkedRegistrations: string[];
  failedMessages: string[];
  stoppedBecauseFull: boolean;
}

interface ParkingLayoutCell {
  slotNumber: number;
  occupiedSlot?: OccupiedSlotDto;
}

function formatDateTime(value: string): string {
  const parsed = new Date(value);

  if (Number.isNaN(parsed.getTime())) {
    return value;
  }

  return new Intl.DateTimeFormat(undefined, {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(parsed);
}

function formatCurrency(value: number): string {
  return new Intl.NumberFormat(undefined, {
    style: 'currency',
    currency: 'GBP',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(value);
}

function normalizeRegistration(value: string): string {
  return value.trim().toLowerCase();
}

function createTestRegistration(seed: number): string {
  const normalizedSeed = Math.abs(Math.trunc(seed)) % TEST_REGISTRATION_SPACE;
  return `${TEST_REGISTRATION_PREFIX}${normalizedSeed.toString(36).padStart(5, '0')}`;
}

function createUniqueTestRegistration(usedRegistrations: Set<string>, seed: number): string {
  for (let attempt = 0; attempt < TEST_REGISTRATION_SPACE; attempt += 1) {
    const candidate = createTestRegistration(seed + attempt);

    if (!usedRegistrations.has(candidate)) {
      return candidate;
    }
  }

  throw new Error('Could not generate a unique test vehicle registration.');
}

function shouldStopBulkParking(error: unknown): boolean {
  return error instanceof ApiError && error.message.includes('No parking space available');
}

function extractErrorMessage(error: unknown): string {
  if (error instanceof ApiError) {
    return error.message;
  }

  if (error instanceof Error) {
    return error.message;
  }

  return 'Unexpected error while calling the parking API.';
}

function App() {
  const [spaceInfo, setSpaceInfo] = useState<SpaceDto | null>(null);
  const [occupiedSlots, setOccupiedSlots] = useState<OccupiedSlotDto[]>([]);
  const [dashboardLoading, setDashboardLoading] = useState(true);
  const [dashboardError, setDashboardError] = useState<string | null>(null);

  const [parkReg, setParkReg] = useState('');
  const [vehicleType, setVehicleType] = useState<number>(1);
  const [parkSubmitting, setParkSubmitting] = useState(false);
  const [parkError, setParkError] = useState<string | null>(null);
  const [lastParkingResult, setLastParkingResult] = useState<ParkingInfoDto | null>(null);
  const [testCarsCount, setTestCarsCount] = useState('5');
  const [bulkParkSubmitting, setBulkParkSubmitting] = useState(false);
  const [bulkParkError, setBulkParkError] = useState<string | null>(null);
  const [bulkParkingSummary, setBulkParkingSummary] = useState<BulkParkingSummary | null>(null);

  const [billingSubmittingReg, setBillingSubmittingReg] = useState<string | null>(null);
  const [billingError, setBillingError] = useState<string | null>(null);
  const [lastBillingResult, setLastBillingResult] = useState<BillingResponseDto | null>(null);

  const [slotActiveSubmitting, setSlotActiveSubmitting] = useState<number | null>(null);

  const [config, setConfig] = useState<ParkingConfigDto | null>(null);
  const [configForm, setConfigForm] = useState<ParkingConfigDto>({ capacity: 100, extraChargeAmountGbp: 1, extraChargeTimeMin: 5, rateType1: 0.1, rateType2: 0.2, rateType3: 0.4 });
  const [configLoading, setConfigLoading] = useState(false);
  const [configSubmitting, setConfigSubmitting] = useState(false);
  const [configError, setConfigError] = useState<string | null>(null);
  const [configSuccess, setConfigSuccess] = useState(false);

  const refreshDashboard = useCallback(async () => {
    setDashboardLoading(true);
    setDashboardError(null);

    try {
      const [spaces, occupied] = await Promise.all([getParkingSpaces(), getOccupiedSlots()]);
      setSpaceInfo(spaces);
      setOccupiedSlots(occupied);
    } catch (error) {
      setDashboardError(extractErrorMessage(error));
    } finally {
      setDashboardLoading(false);
    }
  }, []);

  useEffect(() => {
    void refreshDashboard();
  }, [refreshDashboard]);

  const loadConfig = useCallback(async () => {
    setConfigLoading(true);
    setConfigError(null);
    try {
      const cfg = await getConfig();
      setConfig(cfg);
      setConfigForm(cfg);
    } catch (error) {
      setConfigError(extractErrorMessage(error));
    } finally {
      setConfigLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadConfig();
  }, [loadConfig]);

  const handleConfigSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setConfigError(null);
    setConfigSuccess(false);
    setConfigSubmitting(true);
    try {
      const updated = await updateConfig(configForm);
      setConfig(updated);
      setConfigForm(updated);
      setConfigSuccess(true);
      await refreshDashboard();
    } catch (error) {
      setConfigError(extractErrorMessage(error));
    } finally {
      setConfigSubmitting(false);
    }
  };

  const totalCapacity = useMemo(() => {
    if (!spaceInfo) {
      return null;
    }

    return spaceInfo.totalSlots;
  }, [spaceInfo]);

  const inactiveSlotSet = useMemo(() => {
    return new Set(spaceInfo?.inactiveSlotNumbers ?? []);
  }, [spaceInfo]);

  const layoutColumnCount = useMemo(() => {
    if (!totalCapacity) {
      return 0;
    }

    return Math.min(10, totalCapacity);
  }, [totalCapacity]);

  const parkingLayoutRows = useMemo(() => {
    if (!totalCapacity || layoutColumnCount === 0) {
      return [] as Array<Array<ParkingLayoutCell | null>>;
    }

    const occupiedSlotMap = new Map(occupiedSlots.map((slot) => [slot.slotNumber, slot]));
    const rows: Array<Array<ParkingLayoutCell | null>> = [];

    for (let rowStart = 1; rowStart <= totalCapacity; rowStart += layoutColumnCount) {
      const row: Array<ParkingLayoutCell | null> = [];

      for (let columnOffset = 0; columnOffset < layoutColumnCount; columnOffset += 1) {
        const slotNumber = rowStart + columnOffset;

        if (slotNumber <= totalCapacity) {
          row.push({
            slotNumber,
            occupiedSlot: occupiedSlotMap.get(slotNumber),
          });
        } else {
          row.push(null);
        }
      }

      rows.push(row);
    }

    return rows;
  }, [layoutColumnCount, occupiedSlots, totalCapacity]);

  const parkActionsDisabled = parkSubmitting || bulkParkSubmitting;
  const billingSubmitting = billingSubmittingReg !== null;

  const handleParkSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setParkError(null);
    setLastParkingResult(null);
    setBulkParkError(null);
    setBulkParkingSummary(null);
    setBillingError(null);

    const normalizedReg = normalizeRegistration(parkReg);

    if (!REGISTRATION_PATTERN.test(normalizedReg)) {
      setParkError('Registration must be 2 letters followed by 5 lowercase letters or digits.');
      return;
    }

    setParkSubmitting(true);

    try {
      const result = await parkVehicle({
        vehicleReg: normalizedReg,
        vehicleType,
      });

      setLastParkingResult(result);
      setParkReg('');
      await refreshDashboard();
    } catch (error) {
      setParkError(extractErrorMessage(error));
    } finally {
      setParkSubmitting(false);
    }
  };

  const handleBulkParkSubmit = async () => {
    setParkError(null);
    setLastParkingResult(null);
    setBulkParkError(null);
    setBulkParkingSummary(null);
    setBillingError(null);

    const requestedCount = Number.parseInt(testCarsCount, 10);

    if (!Number.isInteger(requestedCount) || requestedCount <= 0) {
      setBulkParkError('Enter a positive whole number of test cars to park.');
      return;
    }

    setBulkParkSubmitting(true);

    const usedRegistrations = new Set(occupiedSlots.map((slot) => slot.vehicle.vehicleReg));
    const parkedRegistrations: string[] = [];
    const failedMessages: string[] = [];
    let stoppedBecauseFull = false;
    const initialSeed = Date.now();

    try {
      for (let index = 0; index < requestedCount; index += 1) {
        const vehicleReg = createUniqueTestRegistration(usedRegistrations, initialSeed + index);
        const vehicleTypeValue = VEHICLE_TYPE_OPTIONS[index % VEHICLE_TYPE_OPTIONS.length].value;

        try {
          await parkVehicle({
            vehicleReg,
            vehicleType: vehicleTypeValue,
          });

          usedRegistrations.add(vehicleReg);
          parkedRegistrations.push(vehicleReg);
        } catch (error) {
          failedMessages.push(`${vehicleReg}: ${extractErrorMessage(error)}`);

          if (shouldStopBulkParking(error)) {
            stoppedBecauseFull = true;
            break;
          }
        }
      }

      setBulkParkingSummary({
        requestedCount,
        parkedRegistrations,
        failedMessages,
        stoppedBecauseFull,
      });

      if (parkedRegistrations.length > 0 || stoppedBecauseFull) {
        await refreshDashboard();
      }
    } catch (error) {
      setBulkParkError(extractErrorMessage(error));
    } finally {
      setBulkParkSubmitting(false);
    }
  };

  const handleBillVehicle = async (vehicleReg: string) => {
    setBillingError(null);
    setLastBillingResult(null);
    setBulkParkError(null);

    const normalizedReg = normalizeRegistration(vehicleReg);
    setBillingSubmittingReg(normalizedReg);

    try {
      const result = await billVehicle({ vehicleReg: normalizedReg });
      setLastBillingResult(result);
      await refreshDashboard();
    } catch (error) {
      setBillingError(extractErrorMessage(error));
    } finally {
      setBillingSubmittingReg(null);
    }
  };

  const handleToggleSlotActive = async (slotNumber: number, currentlyActive: boolean) => {
    setSlotActiveSubmitting(slotNumber);
    try {
      await setSlotActive(slotNumber, !currentlyActive);
      await refreshDashboard();
    } catch (error) {
      setBillingError(extractErrorMessage(error));
    } finally {
      setSlotActiveSubmitting(null);
    }
  };

  return (
    <div className="app-shell">
      <header className="hero">
        <div>
          <p className="eyebrow">Parking application frontend</p>
          <h1>Parking operations dashboard</h1>
          <p className="hero-copy">
            Manage entry and exit operations for the Spring Boot parking service from one simple React UI.
          </p>
        </div>

        <button className="secondary-button" type="button" onClick={() => void refreshDashboard()} disabled={dashboardLoading}>
          {dashboardLoading ? 'Refreshing…' : 'Refresh data'}
        </button>
      </header>

      {dashboardError ? <div className="banner banner-error">{dashboardError}</div> : null}

      <section className="stats-grid" aria-label="Parking statistics">
        <article className="stat-card accent-blue">
          <span className="stat-label">Available spaces</span>
          <strong className="stat-value">{spaceInfo?.availableSpaces ?? '—'}</strong>
        </article>
        <article className="stat-card accent-purple">
          <span className="stat-label">Occupied spaces</span>
          <strong className="stat-value">{spaceInfo?.occupiedSpaces ?? '—'}</strong>
        </article>
        <article className="stat-card accent-green">
          <span className="stat-label">Capacity</span>
          <strong className="stat-value">{totalCapacity ?? '—'}</strong>
        </article>
        <article className="stat-card accent-gold">
          <span className="stat-label">Inactive spaces</span>
          <strong className="stat-value">{spaceInfo ? spaceInfo.inactiveSlotNumbers.length : '—'}</strong>
        </article>
      </section>

      <main className="content-grid">
        <section className="panel form-panel">
          <div className="panel-heading">
            <div>
              <p className="panel-kicker">Vehicle entry</p>
              <h2>Park a vehicle</h2>
            </div>
          </div>

          <form className="stack" onSubmit={handleParkSubmit}>
            <label className="field">
              <span>Registration number</span>
              <input
                value={parkReg}
                onChange={(event) => setParkReg(normalizeRegistration(event.target.value))}
                placeholder="ab12345"
                inputMode="text"
                maxLength={7}
                autoComplete="off"
                disabled={parkActionsDisabled}
              />
            </label>

            <label className="field">
              <span>Vehicle type</span>
              <select value={vehicleType} onChange={(event) => setVehicleType(Number(event.target.value))} disabled={parkActionsDisabled}>
                {VEHICLE_TYPE_OPTIONS.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
            </label>

            {parkError ? <div className="banner banner-error">{parkError}</div> : null}

            {lastParkingResult ? (
              <div className="banner banner-success">
                Parked <strong>{lastParkingResult.vehicleReg}</strong> in slot <strong>{lastParkingResult.spaceNumber}</strong> at{' '}
                <strong>{formatDateTime(lastParkingResult.timeIn)}</strong>.
              </div>
            ) : null}

            <button className="primary-button" type="submit" disabled={parkActionsDisabled}>
              {parkSubmitting ? 'Parking vehicle…' : 'Park vehicle'}
            </button>
          </form>
        </section>

        <section className="panel form-panel">
          <div className="panel-heading">
            <div>
              <p className="panel-kicker">Bulk test action</p>
              <h2>Park generated test cars</h2>
            </div>
          </div>

          <div className="stack">
            <div className="bulk-parking-box">
              <div className="bulk-parking-header">
                {spaceInfo ? <span className="chip chip-muted">{spaceInfo.availableSpaces} free now</span> : null}
              </div>

              <div className="bulk-parking-controls">
                <label className="field bulk-count-field">
                  <span>Number of test cars</span>
                  <input
                    type="number"
                    min="1"
                    step="1"
                    value={testCarsCount}
                    onChange={(event) => setTestCarsCount(event.target.value)}
                    placeholder="5"
                    disabled={parkActionsDisabled || billingSubmitting}
                  />
                </label>

                <button
                  className="secondary-button bulk-park-button"
                  type="button"
                  onClick={() => void handleBulkParkSubmit()}
                  disabled={parkActionsDisabled || billingSubmitting}
                >
                  {bulkParkSubmitting ? 'Parking test cars…' : 'Park test cars'}
                </button>
              </div>

              <p className="helper-text">
                Generates valid lowercase registrations and parks vehicles one by one using the existing API.
              </p>

              {bulkParkError ? <div className="banner banner-error">{bulkParkError}</div> : null}

              {bulkParkingSummary ? (
                <div className={`banner ${bulkParkingSummary.parkedRegistrations.length > 0 ? 'banner-success' : 'banner-error'}`}>
                  <div className="bulk-summary-grid">
                    <div>
                      <span className="summary-label">Requested</span>
                      <strong>{bulkParkingSummary.requestedCount}</strong>
                    </div>
                    <div>
                      <span className="summary-label">Parked</span>
                      <strong>{bulkParkingSummary.parkedRegistrations.length}</strong>
                    </div>
                    <div>
                      <span className="summary-label">Failed</span>
                      <strong>{bulkParkingSummary.failedMessages.length}</strong>
                    </div>
                    <div>
                      <span className="summary-label">Status</span>
                      <strong>{bulkParkingSummary.stoppedBecauseFull ? 'Stopped: parking full' : 'Completed'}</strong>
                    </div>
                  </div>

                  {bulkParkingSummary.parkedRegistrations.length > 0 ? (
                    <p className="helper-text result-text">
                      Generated registrations: {bulkParkingSummary.parkedRegistrations.slice(0, 5).join(', ')}
                      {bulkParkingSummary.parkedRegistrations.length > 5
                        ? ` and ${bulkParkingSummary.parkedRegistrations.length - 5} more.`
                        : '.'}
                    </p>
                  ) : null}

                  {bulkParkingSummary.failedMessages.length > 0 ? (
                    <ul className="result-list">
                      {bulkParkingSummary.failedMessages.slice(0, 5).map((message) => (
                        <li key={message}>{message}</li>
                      ))}
                      {bulkParkingSummary.failedMessages.length > 5 ? (
                        <li>{bulkParkingSummary.failedMessages.length - 5} more failures not shown.</li>
                      ) : null}
                    </ul>
                  ) : null}
                </div>
              ) : null}
            </div>

            {billingError ? <div className="banner banner-error">{billingError}</div> : null}

            {lastBillingResult ? (
              <div className="banner banner-success success-summary">
                <div>
                  <span className="summary-label">Billing ID</span>
                  <strong>{lastBillingResult.billingId}</strong>
                </div>
                <div>
                  <span className="summary-label">Charge</span>
                  <strong>{formatCurrency(lastBillingResult.vehicleCharge)}</strong>
                </div>
                <div>
                  <span className="summary-label">Time in</span>
                  <strong>{formatDateTime(lastBillingResult.timeIn)}</strong>
                </div>
                <div>
                  <span className="summary-label">Time out</span>
                  <strong>{formatDateTime(lastBillingResult.timeOut)}</strong>
                </div>
              </div>
            ) : null}
          </div>
        </section>

        <section className="panel table-panel">
          <div className="panel-heading panel-heading-inline">
            <div>
              <p className="panel-kicker">Live occupancy</p>
              <h2>Parking layout</h2>
            </div>
            <span className="chip">{occupiedSlots.length} occupied</span>
          </div>

          {dashboardLoading ? (
            <div className="empty-state">Loading parking slots…</div>
          ) : parkingLayoutRows.length === 0 || layoutColumnCount === 0 ? (
            <div className="empty-state">Parking layout is not available yet.</div>
          ) : (
            <div className="table-wrapper">
              <table className="parking-layout-table">
                <tbody>
                  {parkingLayoutRows.map((row, rowIndex) => (
                    <tr key={`row-${rowIndex + 1}`}>
                      {row.map((cell, cellIndex) => {
                        if (!cell) {
                          return <td key={`empty-${rowIndex + 1}-${cellIndex}`} className="layout-empty-cell" aria-hidden="true" />;
                        }

                        const occupiedSlot = cell.occupiedSlot;
                        const isInactive = inactiveSlotSet.has(cell.slotNumber);
                        const isTogglingActive = slotActiveSubmitting === cell.slotNumber;

                        return (
                          <td key={`slot-${cell.slotNumber}`} className="parking-layout-cell">
                            <div className={`slot-card ${occupiedSlot ? 'slot-card-occupied' : isInactive ? 'slot-card-inactive' : 'slot-card-free'}`}>
                              <span className="slot-number">Slot {cell.slotNumber}</span>
                              {occupiedSlot ? (
                                <>
                                  <strong className="slot-primary">{occupiedSlot.vehicle.vehicleReg}</strong>
                                  <span className="slot-meta">{formatCurrency(occupiedSlot.price)}</span>
                                  <button
                                    className="secondary-button slot-action-button"
                                    type="button"
                                    onClick={() => void handleBillVehicle(occupiedSlot.vehicle.vehicleReg)}
                                    disabled={billingSubmitting || parkActionsDisabled}
                                  >
                                    {billingSubmittingReg === occupiedSlot.vehicle.vehicleReg ? 'Billing…' : 'Bill'}
                                  </button>
                                </>
                              ) : (
                                <>
                                  <strong className="slot-primary">{isInactive ? 'Inactive' : 'Free'}</strong>
                                  <span className="slot-secondary">{isInactive ? 'Not accepting' : 'Available'}</span>
                                </>
                              )}
                              <button
                                className={`secondary-button slot-action-button slot-toggle-button ${isInactive ? 'slot-toggle-activate' : 'slot-toggle-deactivate'}`}
                                type="button"
                                onClick={() => void handleToggleSlotActive(cell.slotNumber, !isInactive)}
                                disabled={isTogglingActive || billingSubmitting || parkActionsDisabled || (!isInactive && !!occupiedSlot)}
                                title={!isInactive && occupiedSlot ? 'Cannot deactivate: slot is occupied' : isInactive ? 'Activate slot' : 'Deactivate slot'}
                              >
                                {isTogglingActive ? '…' : isInactive ? 'Activate' : 'Deactivate'}
                              </button>
                            </div>
                          </td>
                        );
                      })}
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </section>

        <section className="panel form-panel config-panel">
          <div className="panel-heading">
            <div>
              <p className="panel-kicker">Settings</p>
              <h2>Parking configuration</h2>
            </div>
          </div>

          {configLoading ? (
            <div className="empty-state">Loading configuration…</div>
          ) : (
            <form className="stack config-form" onSubmit={handleConfigSubmit}>
              <div className="config-fields">
                <label className="field">
                  <span>Capacity (total slots)</span>
                  <input
                    type="number"
                    min="1"
                    step="1"
                    value={configForm.capacity}
                    onChange={(e) => setConfigForm((f) => ({ ...f, capacity: Number(e.target.value) }))}
                    disabled={configSubmitting || occupiedSlots.length > 0}
                    title={occupiedSlots.length > 0 ? 'Capacity can be changed only when the parking is empty' : undefined}
                  />
                  {occupiedSlots.length > 0 ? (
                    <span className="helper-text" style={{ color: '#fbbf24' }}>Capacity can be changed only when the parking is empty.</span>
                  ) : null}
                </label>

                <label className="field">
                  <span>Extra charge amount (GBP)</span>
                  <input
                    type="number"
                    min="0"
                    step="1"
                    value={configForm.extraChargeAmountGbp}
                    onChange={(e) => setConfigForm((f) => ({ ...f, extraChargeAmountGbp: Number(e.target.value) }))}
                    disabled={configSubmitting}
                  />
                </label>

                <label className="field">
                  <span>Extra charge interval (minutes)</span>
                  <input
                    type="number"
                    min="0"
                    step="1"
                    value={configForm.extraChargeTimeMin}
                    onChange={(e) => setConfigForm((f) => ({ ...f, extraChargeTimeMin: Number(e.target.value) }))}
                    disabled={configSubmitting}
                  />
                </label>

                <label className="field">
                  <span>Rate — Type 1 small (£/min)</span>
                  <input
                    type="number"
                    min="0.0"
                    step="0.01"
                    value={configForm.rateType1}
                    onChange={(e) => setConfigForm((f) => ({ ...f, rateType1: Number(e.target.value) }))}
                    disabled={configSubmitting}
                  />
                </label>

                <label className="field">
                  <span>Rate — Type 2 medium (£/min)</span>
                  <input
                    type="number"
                    min="0.0"
                    step="0.01"
                    value={configForm.rateType2}
                    onChange={(e) => setConfigForm((f) => ({ ...f, rateType2: Number(e.target.value) }))}
                    disabled={configSubmitting}
                  />
                </label>

                <label className="field">
                  <span>Rate — Type 3 large (£/min)</span>
                  <input
                    type="number"
                    min="0.0"
                    step="0.01"
                    value={configForm.rateType3}
                    onChange={(e) => setConfigForm((f) => ({ ...f, rateType3: Number(e.target.value) }))}
                    disabled={configSubmitting}
                  />
                </label>
              </div>

              <p className="helper-text">
                Extra charge: <strong>{config ? `£${config.extraChargeAmountGbp} per ${config.extraChargeTimeMin} min` : '—'}</strong>.
                Reducing capacity only removes free slots from the end.
              </p>

              {configError ? <div className="banner banner-error">{configError}</div> : null}
              {configSuccess ? <div className="banner banner-success">Configuration updated successfully.</div> : null}

              <button className="primary-button" type="submit" disabled={configSubmitting}>
                {configSubmitting ? 'Saving…' : 'Save configuration'}
              </button>
            </form>
          )}
        </section>
      </main>
    </div>
  );
}

export default App;






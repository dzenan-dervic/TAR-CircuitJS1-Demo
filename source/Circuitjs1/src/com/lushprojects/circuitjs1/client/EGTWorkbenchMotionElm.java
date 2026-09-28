package com.lushprojects.circuitjs1.client;

class EGTWorkbenchMotionElm extends EGTBewegungsmelderElm {
    double expiresAt;
    double lastSupplyAt = -1;

    EGTWorkbenchMotionElm(int x, int y) { super(x, y); }

    boolean triggerMovement() {
        if (!powered || !app.simIsRunning()) return false;
        timedOn = true;
        expiresAt = sim.t + delay;
        return true;
    }

    boolean triggerAt(int x, int y) { return triggerMovement(); }

    double remaining() { return timedOn ? Math.max(0, expiresAt - sim.t) : 0; }

    void calculateCurrent() {
        EGTStyle.clearPinCurrents(pins);
        double voltage = volts[N_L] - volts[N_N];
        supplyCurrent = voltage / resistance;
        if (Math.abs(voltage) >= nom_v * .35) lastSupplyAt = sim.t;
        // Hold through AC zero crossings; the timer follows simulation time, including pause.
        powered = lastSupplyAt >= 0 && sim.t - lastSupplyAt < .05;
        if (!powered || sim.t >= expiresAt) timedOn = false;
        waitAccum = timedOn ? delay - remaining() : 0;
        contactCurrent = contactClosed() ? (volts[N_L] - volts[N_SW]) / R_ON : 0;
        pins[N_L].current = -(supplyCurrent + contactCurrent);
        pins[N_N].current = supplyCurrent;
        pins[N_SW].current = contactCurrent;
        current = supplyCurrent;
    }

    void reset() {
        super.reset();
        expiresAt = 0;
        lastSupplyAt = -1;
    }
}

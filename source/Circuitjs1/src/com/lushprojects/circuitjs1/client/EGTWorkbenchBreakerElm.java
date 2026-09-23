package com.lushprojects.circuitjs1.client;

/** Manual isolation is separate from the existing breaker's thermal/magnetic latch. */
class EGTWorkbenchBreakerElm extends EGTSicherungElm {
    boolean switchedOn;
    EGTWorkbenchBreakerElm(int x, int y) { super(x, y); }
    boolean contactClosed() { return switchedOn && super.contactClosed(); }
    boolean getConnection(int a, int b) { return switchedOn && super.getConnection(a, b); }
    void stamp() { if (switchedOn) super.stamp(); else holdUntilStamp = true; }
    void stepFinished() { if (switchedOn) super.stepFinished(); }
}

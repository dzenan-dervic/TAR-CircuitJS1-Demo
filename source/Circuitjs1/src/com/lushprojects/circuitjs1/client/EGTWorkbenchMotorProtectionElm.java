package com.lushprojects.circuitjs1.client;

/** The front auxiliary contacts follow the protective switch's mechanical position. */
class EGTWorkbenchMotorProtectionElm extends EGTMotorschutzschalterElm implements EGTSchuetzCoilSource {
    EGTWorkbenchMotorProtectionElm(int x, int y) { super(x, y); }
    public String schuetzDesignation() { return designation; }
    public boolean coilEnergized() { return contactClosed(); }
}

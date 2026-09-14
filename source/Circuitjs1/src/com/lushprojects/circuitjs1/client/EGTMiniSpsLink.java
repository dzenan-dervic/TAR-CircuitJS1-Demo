/*
    TAR-Dervic EGT: Mini-SPS Kopplung Gerät ↔ FUP-Operanden I/Q
*/

package com.lushprojects.circuitjs1.client;

import java.util.Vector;

/**
 * Mini-SPS ↔ FUP. Eine SPS: I1/Q1 ohne Zusatz. Zwei SPS: FUP an A1/A2,
 * Zusatzbeschriftung nur dann. LCD-I = Klemme oder FUP-I (Prozessabbild).
 * FUP-I elektrisch Wired-OR; qOperandHigh = Pegel oder iHigh auf demselben Knoten.
 */
class EGTMiniSpsLink {
    static EGTMiniSpsElm device;
    static int lastStep = -2;

    static int spsCount() {
	Vector<CircuitElm> list = elmList();
	if (list == null)
	    return 0;
	int n = 0;
	for (int i = 0; i < list.size(); i++)
	    if (list.elementAt(i) instanceof EGTMiniSpsElm)
		n++;
	return n;
    }

    static boolean multiSps() { return spsCount() >= 2; }

    static Vector<CircuitElm> elmList() {
	if (CircuitElm.app != null)
	    return CircuitElm.app.elmList;
	return null;
    }

    static EGTMiniSpsElm firstSps() {
	Vector<CircuitElm> list = elmList();
	if (list == null)
	    return null;
	for (int i = 0; i < list.size(); i++) {
	    CircuitElm ce = list.elementAt(i);
	    if (ce instanceof EGTMiniSpsElm)
		return (EGTMiniSpsElm) ce;
	}
	return null;
    }

    static EGTMiniSpsElm findSps(String des) {
	Vector<CircuitElm> list = elmList();
	if (list == null)
	    return null;
	String d = des == null ? "" : des.trim();
	if (d.length() == 0 || !multiSps())
	    return firstSps();
	for (int i = 0; i < list.size(); i++) {
	    CircuitElm ce = list.elementAt(i);
	    if (!(ce instanceof EGTMiniSpsElm))
		continue;
	    EGTMiniSpsElm sps = (EGTMiniSpsElm) ce;
	    String sd = sps.egtDesignation();
	    if (sd != null && sd.trim().equals(d))
		return sps;
	}
	return firstSps();
    }

    /** Anzeige-Kennzeichen für FUP/Q, leer wenn nur eine SPS. */
    static String extraLabel(String opDes) {
	if (!multiSps())
	    return "";
	String d = opDes == null ? "" : opDes.trim();
	if (d.length() > 0)
	    return d;
	EGTMiniSpsElm first = firstSps();
	if (first == null)
	    return "";
	String fd = first.egtDesignation();
	return fd == null ? "" : fd.trim();
    }

    static Vector<String> spsNames() {
	Vector<String> names = new Vector<String>();
	Vector<CircuitElm> list = elmList();
	if (list == null)
	    return names;
	for (int i = 0; i < list.size(); i++) {
	    CircuitElm ce = list.elementAt(i);
	    if (!(ce instanceof EGTMiniSpsElm))
		continue;
	    String sd = ((EGTMiniSpsElm) ce).egtDesignation();
	    if (sd == null)
		sd = "";
	    sd = sd.trim();
	    if (sd.length() == 0)
		sd = "A?";
	    names.add(sd);
	}
	return names;
    }

    static boolean belongsTo(EGTFupOperandElm op, EGTMiniSpsElm sps) {
	if (op == null || sps == null)
	    return false;
	if (!multiSps())
	    return true;
	String d = op.spsDes == null ? "" : op.spsDes.trim();
	if (d.length() == 0)
	    return sps == firstSps();
	String sd = sps.egtDesignation();
	return sd != null && sd.trim().equals(d);
    }

    static String nearestDes(int x, int y) {
	Vector<CircuitElm> list = elmList();
	if (list == null)
	    return "";
	EGTMiniSpsElm best = null;
	double bestD = 1e12;
	for (int i = 0; i < list.size(); i++) {
	    CircuitElm ce = list.elementAt(i);
	    if (!(ce instanceof EGTMiniSpsElm))
		continue;
	    EGTMiniSpsElm sps = (EGTMiniSpsElm) ce;
	    double cx = (sps.x + sps.x2) / 2.0;
	    double cy = (sps.y + sps.y2) / 2.0;
	    double dx = cx - x;
	    double dy = cy - y;
	    double d = dx * dx + dy * dy;
	    if (d < bestD) {
		bestD = d;
		best = sps;
	    }
	}
	if (best == null)
	    return "";
	String sd = best.egtDesignation();
	return sd == null ? "" : sd.trim();
    }

    static void refresh(SimulationManager sim) {
	if (sim == null)
	    return;
	if (sim.timeStepCount == lastStep)
	    return;
	lastStep = sim.timeStepCount;
	device = null;
	if (sim.elmList == null)
	    return;
	Vector<CircuitElm> list = sim.elmList;
	for (int i = 0; i < list.size(); i++) {
	    CircuitElm ce = list.elementAt(i);
	    if (!(ce instanceof EGTMiniSpsElm))
		continue;
	    EGTMiniSpsElm sps = (EGTMiniSpsElm) ce;
	    if (device == null)
		device = sps;
	    sps.senseAcLevels(sim.timeStep);
	    int q = 0;
	    int forcedI = 0;
	    for (int j = 0; j < list.size(); j++) {
		CircuitElm fe = list.elementAt(j);
		if (!(fe instanceof EGTFupOperandElm))
		    continue;
		EGTFupOperandElm op = (EGTFupOperandElm) fe;
		if (!belongsTo(op, sps))
		    continue;
		if (op.isInputOperand()) {
		    if (op.forced)
			forcedI |= 1 << (op.channel - 1);
		} else if (qOperandHigh(list, op))
		    q |= 1 << (op.channel - 1);
	    }
	    sps.applyForcedI(forcedI);
	    sps.applyProgrammedQ(q);
	}
    }

    /** Q = Pegel am Stift oder Wired-OR aller FUP-I auf demselben Knoten. */
    static boolean qOperandHigh(Vector<CircuitElm> list, EGTFupOperandElm qOp) {
	if (qOp.logicHigh())
	    return true;
	if (qOp.nodes == null || qOp.nodes[0] == null)
	    return false;
	CircuitNode n = qOp.nodes[0];
	for (int i = 0; i < list.size(); i++) {
	    CircuitElm ce = list.elementAt(i);
	    if (!(ce instanceof EGTFupOperandElm))
		continue;
	    EGTFupOperandElm op = (EGTFupOperandElm) ce;
	    if (!op.isInputOperand() || op.nodes == null || op.nodes[0] != n)
		continue;
	    if (op.iHigh())
		return true;
	}
	return false;
    }

    static boolean inputHigh(int ch) {
	return inputHigh(ch, "");
    }

    static boolean inputHigh(int ch, String des) {
	if (ch < 1 || ch > EGTMiniSpsElm.N_IN)
	    return false;
	EGTMiniSpsElm sps = findSps(des);
	if (sps == null || !sps.isRun())
	    return false;
	return (sps.getIMask() & (1 << (ch - 1))) != 0;
    }
}

/**
 * 230-V-AC-Pegel ohne 50-Hz-Flattern.
 * Ein: |u| >= 80 V (ein Sample) oder Mittel >= 80 V.
 * Aus: |u| unter 20 V fuer 3 ms am Stueck (nicht Nulldurchgang).
 */
class EGTAcLevelHold {
    static final double TAU_S = 0.012;
    static final double ON_V = 80;
    static final double OPEN_V = 20;
    static final double OPEN_HOLD_S = 0.003;
    /** Hold-Zaehler: ein Solver-Schritt zaehlt hoechstens 0,5 ms (grosse dt). */
    static final double HOLD_DT_MAX = 0.0005;

    double avgAbs;
    boolean on;
    double lowForS;

    void reset() {
	avgAbs = 0;
	on = false;
	lowForS = 0;
    }

    boolean update(double instantV, double dt) {
	if (dt <= 0) {
	    reset();
	    return false;
	}
	double abs = Math.abs(instantV);
	double a = Math.exp(-dt / TAU_S);
	if (a < 0)
	    a = 0;
	if (a > 1)
	    a = 1;
	avgAbs = a * avgAbs + (1 - a) * abs;
	double ht = dt < HOLD_DT_MAX ? dt : HOLD_DT_MAX;
	if (abs < OPEN_V)
	    lowForS += ht;
	else
	    lowForS = 0;
	if (!on) {
	    if (abs >= ON_V || avgAbs >= ON_V) {
		on = true;
		avgAbs = avgAbs < ON_V ? ON_V : avgAbs;
		lowForS = 0;
	    }
	} else if (lowForS >= OPEN_HOLD_S) {
	    on = false;
	    avgAbs = 0;
	    lowForS = 0;
	}
	return on;
    }
}

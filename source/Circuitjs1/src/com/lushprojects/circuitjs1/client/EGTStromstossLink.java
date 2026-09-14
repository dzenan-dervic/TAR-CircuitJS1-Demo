/*
    TAR-Dervic EGT: Stromstoßschalter — Bezeichnung → gerasteter Zustand
*/

package com.lushprojects.circuitjs1.client;

import java.util.HashMap;

/**
 * Kopplung Spule ↔ Kontakte über Betriebsmittelkennzeichen (z. B. E1).
 * Rising Edge der Spule toggelt den Rastzustand (latched).
 */
class EGTStromstossLink {
    static final HashMap<String, Boolean> latched =
	    new HashMap<String, Boolean>();
    static final HashMap<String, Double> lastToggleT =
	    new HashMap<String, Double>();

    static final double R_ON = 0.05;
    static final double R_OFF = 1e8;

    static String normalize(String des) {
	if (des == null)
	    return "E1";
	String t = des.trim().toUpperCase();
	return t.length() == 0 ? "E1" : t;
    }

    static boolean isLatched(String des) {
	Boolean b = latched.get(normalize(des));
	return b != null && b.booleanValue();
    }

    static void setLatched(String des, boolean on) {
	latched.put(normalize(des), Boolean.valueOf(on));
    }

    /** Ein Toggle pro Bezeichnung und Simulationszeitpunkt. */
    static void toggle(String des, double t) {
	String d = normalize(des);
	Double prev = lastToggleT.get(d);
	if (prev != null && prev.doubleValue() == t)
	    return;
	lastToggleT.put(d, Double.valueOf(t));
	setLatched(d, !isLatched(d));
    }

    static void clearDesignation(String des) {
	String d = normalize(des);
	latched.remove(d);
	lastToggleT.remove(d);
    }
}

/** Spulenquelle für Stromstoß (Impuls → Rast). */
interface EGTStromstossSource {
    String stromstossDesignation();
    boolean coilPulseActive();
}

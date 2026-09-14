/*
    TAR-Dervic EGT: Zeitrelais — Bezeichnung → verzögerter Kontaktzustand
*/

package com.lushprojects.circuitjs1.client;

import java.util.HashMap;
import java.util.Vector;

/**
 * Kopplung Spule ↔ Kontakte über Betriebsmittelkennzeichen (z. B. K1).
 * Einschaltverzögert: nach Erregung warten, Abfall sofort.
 * Ausschaltverzögert: Anzug sofort, nach Abfall warten.
 * Kombiniert: Anzug und Abfall getrennt verzögert.
 * Blinkend: solange erregt, Takt tEin / tAus (Fachkunde 2 Hz).
 * Wartezeit = Wanduhr, solange die Simulation läuft (Pause friert).
 */
class EGTZeitrelaisLink {
    static final double R_ON = 0.05;
    static final double R_OFF = 1e8;
    /** Max. Wandzeit pro Solver-Aufruf (Tab im Hintergrund). */
    static final double MAX_WALL_DT = 0.25;
    static final int MODE_ON = 0;
    static final int MODE_OFF = 1;
    static final int MODE_COMBINED = 2;
    static final int MODE_BLINK = 3;
    /** Fachkunde Blatt 6.12: 500 ms Ansprech-, 250 ms Rückfallzeit. */
    static final double DEF_COMBINED_ON = 0.5;
    static final double DEF_COMBINED_OFF = 0.25;
    /** Fachkunde: 2 Hz, Tastverhältnis 1:1. */
    static final double DEF_BLINK = 0.25;

    static class State {
	boolean powered;
	boolean timedOn;
	int mode = MODE_ON;
	double delay = 1;
	double delayOff = DEF_BLINK;
	double waitAccum;
	long lastWallMs;
    }

    static final HashMap<String, State> states =
	    new HashMap<String, State>();

    static String normalize(String des) {
	if (des == null)
	    return "K1";
	String t = des.trim().toUpperCase();
	return t.length() == 0 ? "K1" : t;
    }

    static State get(String des) {
	String d = normalize(des);
	State s = states.get(d);
	if (s == null) {
	    s = new State();
	    states.put(d, s);
	}
	return s;
    }

    static boolean isTimedOn(String des) {
	return get(des).timedOn;
    }

    static boolean isOffDelayMode(String des) {
	return get(des).mode == MODE_OFF;
    }

    static boolean isBlinkMode(String des) {
	return get(des).mode == MODE_BLINK;
    }

    static boolean isCombinedMode(String des) {
	return get(des).mode == MODE_COMBINED;
    }

    static boolean isPowered(String des) {
	return get(des).powered;
    }

    static boolean isWaiting(String des) {
	return isWaitingState(get(des));
    }

    static boolean isWaitingState(State s) {
	if (s.mode == MODE_BLINK)
	    return s.powered;
	if (s.mode == MODE_COMBINED)
	    return s.powered ? !s.timedOn : s.timedOn;
	if (s.mode == MODE_OFF)
	    return !s.powered && s.timedOn;
	return s.powered && !s.timedOn;
    }

    static String statusText(String des) {
	State s = get(des);
	if (s.mode == MODE_BLINK) {
	    if (!s.powered)
		return "abgefallen";
	    return s.timedOn ? "blinkt ein" : "blinkt aus";
	}
	if (isWaitingState(s)) {
	    if (s.mode == MODE_COMBINED)
		return s.powered ? "wartet Ansprech…" : "wartet Rückfall…";
	    return "wartet…";
	}
	return s.timedOn ? "angezogen" : "abgefallen";
    }

    /** Ablaufanzeige neben dem Kontakt, nur während der Verzögerung / des Takts. */
    static String timerLabel(String des) {
	State s = get(des);
	if (!isWaitingState(s))
	    return null;
	double phase = s.delay;
	if (s.mode == MODE_BLINK)
	    phase = s.timedOn ? s.delay : s.delayOff;
	else if (s.mode == MODE_COMBINED && !s.powered)
	    phase = s.delayOff;
	return CircuitElm.getUnitText(s.waitAccum, "s") + " / "
		+ CircuitElm.getUnitText(phase, "s");
    }

    static void applyTimed(State s) {
	if (s.mode == MODE_BLINK)
	    return;
	if (s.mode == MODE_COMBINED) {
	    if (s.powered) {
		if (s.waitAccum >= s.delay)
		    s.timedOn = true;
	    } else if (s.waitAccum >= s.delayOff) {
		s.timedOn = false;
	    }
	    return;
	}
	if (s.mode != MODE_OFF) {
	    if (!s.powered)
		s.timedOn = false;
	    else if (s.waitAccum >= s.delay)
		s.timedOn = true;
	} else {
	    if (s.powered)
		s.timedOn = true;
	    else if (s.waitAccum >= s.delay)
		s.timedOn = false;
	}
    }

    static double clampBlink(double t) {
	if (t < 0.01)
	    return 0.01;
	return t;
    }

    /**
     * Spule meldet Erregung. Zeitbasis: Wanduhr bei laufender Simulation.
     * timedOn nur hier (calculateCurrent), nicht in doStep.
     */
    static void updateFromCoil(String des, boolean poweredNow, int mode,
			       double delayOn, double delayOff) {
	State s = get(des);
	if (delayOn < 0)
	    delayOn = 0;
	if (mode == MODE_BLINK) {
	    delayOn = clampBlink(delayOn);
	    delayOff = clampBlink(delayOff);
	}
	if (delayOff < 0)
	    delayOff = 0;
	int oldMode = s.mode;
	boolean modeChanged = oldMode != mode;
	s.mode = mode;
	s.delay = delayOn;
	s.delayOff = delayOff;
	long now = System.currentTimeMillis();
	boolean running = CircuitElm.app != null
		&& CircuitElm.app.simIsRunning();
	boolean became = s.powered != poweredNow;
	if (modeChanged) {
	    s.waitAccum = 0;
	    s.lastWallMs = now;
	    if (mode == MODE_OFF || mode == MODE_BLINK)
		s.timedOn = poweredNow;
	    else
		s.timedOn = false;
	}
	if (became) {
	    s.powered = poweredNow;
	    s.waitAccum = 0;
	    s.lastWallMs = now;
	    if (mode == MODE_BLINK)
		s.timedOn = poweredNow;
	}
	if (mode == MODE_BLINK) {
	    if (!s.powered) {
		s.timedOn = false;
		s.lastWallMs = now;
		return;
	    }
	    if (running && s.lastWallMs != 0) {
		double dt = (now - s.lastWallMs) / 1000.0;
		if (dt < 0)
		    dt = 0;
		if (dt > MAX_WALL_DT)
		    dt = MAX_WALL_DT;
		s.waitAccum += dt;
		double phase = s.timedOn ? s.delay : s.delayOff;
		if (s.waitAccum >= phase) {
		    s.waitAccum = 0;
		    s.timedOn = !s.timedOn;
		}
	    }
	    s.lastWallMs = now;
	    return;
	}
	if (running && isWaitingState(s) && s.lastWallMs != 0) {
	    double dt = (now - s.lastWallMs) / 1000.0;
	    if (dt < 0)
		dt = 0;
	    if (dt > MAX_WALL_DT)
		dt = MAX_WALL_DT;
	    s.waitAccum += dt;
	}
	s.lastWallMs = now;
	applyTimed(s);
    }

    static void setTimedOn(String des, boolean on) {
	get(des).timedOn = on;
    }

    static void clearDesignation(String des) {
	states.remove(normalize(des));
    }

    static void reset(String des) {
	State s = get(des);
	s.powered = false;
	s.timedOn = false;
	s.waitAccum = 0;
	s.lastWallMs = 0;
    }

    static EGTZeitrelaisSpuleElm findCoil(String des) {
	if (CircuitElm.app == null || CircuitElm.app.elmList == null)
	    return null;
	String d = normalize(des);
	Vector<CircuitElm> list = CircuitElm.app.elmList;
	for (int i = 0; i < list.size(); i++) {
	    CircuitElm ce = list.elementAt(i);
	    if (ce instanceof EGTZeitrelaisSpuleElm) {
		EGTZeitrelaisSpuleElm sp = (EGTZeitrelaisSpuleElm) ce;
		if (normalize(sp.egtDesignation()).equals(d))
		    return sp;
	    }
	}
	return null;
    }

    static Vector findContacts(String des) {
	Vector out = new Vector();
	if (CircuitElm.app == null || CircuitElm.app.elmList == null)
	    return out;
	String d = normalize(des);
	Vector<CircuitElm> list = CircuitElm.app.elmList;
	for (int i = 0; i < list.size(); i++) {
	    CircuitElm ce = list.elementAt(i);
	    if (ce instanceof EGTZeitrelaisKontaktElm) {
		EGTZeitrelaisKontaktElm k = (EGTZeitrelaisKontaktElm) ce;
		if (normalize(k.egtDesignation()).equals(d))
		    out.add(k);
	    }
	}
	return out;
    }
}

/** Spulenquelle für Zeitrelais. */
interface EGTZeitrelaisSource {
    String zeitrelaisDesignation();
    boolean coilEnergized();
}

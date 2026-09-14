/*
    TAR-Dervic EGT: Bezeichnung → angezogen (Leistungsschütz / Hilfskontakte)
*/

package com.lushprojects.circuitjs1.client;

import java.util.HashMap;
import java.util.Vector;

/**
 * Kopplung Spule ↔ Kontakte über Betriebsmittelkennzeichen (z. B. Q1).
 * OR über alle Spulenquellen mit gleichem Tag. Nur EGT-Schütz-Elms.
 */
class EGTSchuetzLink {
    static final HashMap<String, Boolean> energized =
	    new HashMap<String, Boolean>();

    static final double R_ON = 0.05;
    static final double R_OFF = 1e8;

    static boolean pinPair(int n1, int n2, int a, int b) {
	return (n1 == a && n2 == b) || (n1 == b && n2 == a);
    }

    static String normalize(String des) {
	if (des == null)
	    return "Q1";
	String t = des.trim().toUpperCase();
	return t.length() == 0 ? "Q1" : t;
    }

    static void refresh(SimulationManager sim) {
	energized.clear();
	if (sim == null || sim.elmList == null)
	    return;
	Vector<CircuitElm> list = sim.elmList;
	for (int i = 0; i < list.size(); i++) {
	    CircuitElm ce = list.elementAt(i);
	    if (ce instanceof EGTSchuetzCoilSource) {
		EGTSchuetzCoilSource src = (EGTSchuetzCoilSource) ce;
		if (src.coilEnergized())
		    energized.put(normalize(src.schuetzDesignation()),
				  Boolean.TRUE);
	    }
	}
    }

    static boolean isEnergized(String des) {
	Boolean b = energized.get(normalize(des));
	return b != null && b.booleanValue();
    }

    /** Zehner der Hilfsklemme (13 → 1, 21 → 2). 0 = ungültig. */
    static int tensOfTerminal(String lab) {
	if (lab == null)
	    return 0;
	try {
	    int n = Integer.parseInt(lab.trim());
	    if (n < 10)
		return 0;
	    return n / 10;
	} catch (Exception e) {
	    return 0;
	}
    }

    static void markTens(boolean used[], int tens) {
	if (tens >= 1 && tens <= 9)
	    used[tens] = true;
    }

    /**
     * Belegte Kontakt-Zehner derselben Bezeichnung: einzelne Hilfskontakte,
     * eingebautes 13/14 am Leistungsschütz, Hilfsschütz-/Block-Klemmen.
     */
    static void markAuxTens(CircuitElm self, String des, boolean used[]) {
	if (CircuitElm.app == null || CircuitElm.app.elmList == null)
	    return;
	des = normalize(des);
	Vector<CircuitElm> list = CircuitElm.app.elmList;
	for (int i = 0; i < list.size(); i++) {
	    CircuitElm ce = list.elementAt(i);
	    if (ce == self)
		continue;
	    if (ce instanceof EGTSchuetzHilfskontaktElm) {
		EGTSchuetzHilfskontaktElm h = (EGTSchuetzHilfskontaktElm) ce;
		if (normalize(h.designation).equals(des))
		    markTens(used, h.contactNo);
	    } else if (ce instanceof EGTLeistungsschuetzElm) {
		if (normalize(((EGTLeistungsschuetzElm) ce).designation)
			.equals(des))
		    markTens(used, 1);
	    } else if (ce instanceof EGTHilfsschuetzElm) {
		EGTHilfsschuetzElm k = (EGTHilfsschuetzElm) ce;
		if (!normalize(k.designation).equals(des))
		    continue;
		for (int c = 0; c < k.nNo; c++)
		    markTens(used, tensOfTerminal(EGTHilfsschuetzElm.NO_TOP[c]));
		for (int c = 0; c < k.nNc; c++)
		    markTens(used, tensOfTerminal(EGTHilfsschuetzElm.NC_TOP[c]));
	    } else if (ce instanceof EGTHilfsschalterblockElm) {
		EGTHilfsschalterblockElm b = (EGTHilfsschalterblockElm) ce;
		if (!normalize(b.designation).equals(des))
		    continue;
		for (int c = 0; c < b.nNo; c++)
		    markTens(used,
			     tensOfTerminal(EGTHilfsschalterblockElm.NO_TOP[c]));
		for (int c = 0; c < b.nNc; c++)
		    markTens(used,
			     tensOfTerminal(EGTHilfsschalterblockElm.NC_TOP[c]));
	    }
	}
    }

    /** Nächster freier Zehner 1…9 (13/14, 23/24, …). */
    static int nextAuxTens(CircuitElm self, String des) {
	boolean used[] = new boolean[10];
	markAuxTens(self, des, used);
	for (int n = 1; n <= 9; n++) {
	    if (!used[n])
		return n;
	}
	return 9;
    }

    static int nextAuxSpawnX(CircuitElm host, String des) {
	int px = host.snapGrid(host.x2 + 16);
	if (CircuitElm.app == null || CircuitElm.app.elmList == null)
	    return px;
	des = normalize(des);
	Vector<CircuitElm> list = CircuitElm.app.elmList;
	for (int i = 0; i < list.size(); i++) {
	    CircuitElm ce = list.elementAt(i);
	    if (ce == host)
		continue;
	    String d = null;
	    if (ce instanceof EGTSchuetzLeistungskontakteElm)
		d = ((EGTSchuetzLeistungskontakteElm) ce).designation;
	    else if (ce instanceof EGTSchuetzHilfskontaktElm)
		d = ((EGTSchuetzHilfskontaktElm) ce).designation;
	    if (d == null || !normalize(d).equals(des))
		continue;
	    int nx = host.snapGrid(ce.x2 + 16);
	    if (nx > px)
		px = nx;
	}
	return px;
    }

    static CircuitElm designationIfSchuetz(CircuitElm ce) {
	if (ce instanceof EGTSchuetzSpuleElm
		|| ce instanceof EGTLeistungsschuetzElm
		|| ce instanceof EGTHilfsschuetzElm
		|| ce instanceof EGTSchuetzHilfskontaktElm
		|| ce instanceof EGTSchuetzLeistungskontakteElm
		|| ce instanceof EGTHilfsschalterblockElm)
	    return ce;
	return null;
    }

    static String inheritSchuetzDesignation() {
	CircuitElm ref = designationIfSchuetz(CircuitElm.mouseElmRef);
	if (ref instanceof EGTDesignatable)
	    return ((EGTDesignatable) ref).egtDesignation();
	if (CircuitElm.app == null || CircuitElm.app.elmList == null)
	    return uniqueCoilDesignation();
	String found = null;
	int n = 0;
	Vector<CircuitElm> list = CircuitElm.app.elmList;
	for (int i = 0; i < list.size(); i++) {
	    CircuitElm ce = list.elementAt(i);
	    if (!ce.isSelected())
		continue;
	    CircuitElm s = designationIfSchuetz(ce);
	    if (s == null)
		continue;
	    found = ((EGTDesignatable) s).egtDesignation();
	    n++;
	}
	if (n == 1)
	    return found;
	return uniqueCoilDesignation();
    }

    /** Falls genau eine Spulen-Bezeichnung auf dem Blatt liegt. */
    static String uniqueCoilDesignation() {
	if (CircuitElm.app == null || CircuitElm.app.elmList == null)
	    return null;
	String found = null;
	Vector<CircuitElm> list = CircuitElm.app.elmList;
	for (int i = 0; i < list.size(); i++) {
	    CircuitElm ce = list.elementAt(i);
	    if (!(ce instanceof EGTSchuetzCoilSource))
		continue;
	    String d = normalize(
		    ((EGTSchuetzCoilSource) ce).schuetzDesignation());
	    if (found == null)
		found = d;
	    else if (!found.equals(d))
		return null;
	}
	return found;
    }

    /** Neuer Hilfsschließer rechts neben host, gleiche Bezeichnung, nächste Klemme. */
    static void placeHilfskontakt(CircuitElm host, String des) {
	if (CircuitElm.app == null || CircuitElm.app.elmList == null
		|| host == null)
	    return;
	CirSim app = CircuitElm.app;
	app.undoManager.pushUndo();
	CircuitElm ce = CirSim.constructElement("EGTSchuetzHilfskontaktElm",
						nextAuxSpawnX(host, des),
						host.snapGrid(host.y));
	if (!(ce instanceof EGTSchuetzHilfskontaktElm))
	    return;
	EGTSchuetzHilfskontaktElm h = (EGTSchuetzHilfskontaktElm) ce;
	h.setEgtDesignation(des);
	app.elmList.addElement(h);
	h.assignNextContactNo();
	h.draggingDone();
	app.needAnalyze();
	app.undoManager.writeRecoveryToStorage();
	app.unsavedChanges = true;
    }
}

/**
 * Spulenzustand ohne AC-Flattern: exponentielles |I|-Mittel + Hysterese
 * (Vorbild RelayCoilElm.avgCurrent, längere Zeitkonstante für ohmsche Spule).
 */
class EGTSchuetzCoilHold {
    /** ~12 ms — hält über 50-Hz-Nulldurchgang. */
    static final double TAU_S = 0.012;
    static final double ON_FRAC = 0.08;
    static final double OFF_FRAC = 0.025;
    /**
     * Stromkreis offen: |i| ≈ 0 länger als ein 50-Hz-Null (~60 µs).
     * Sonst bleibt avgAbsI nach S1-Auf ~40 ms oben — Kontakte fallen spät.
     */
    static final double OPEN_S = 0.001;
    static final double OPEN_FRAC = 0.02;

    double avgAbsI;
    double lowITime;
    boolean on;

    void reset() {
	avgAbsI = 0;
	lowITime = 0;
	on = false;
    }

    /**
     * @param coilCurrent Momentanstrom der Spule
     * @param iNom Nennstrom (nom_v / R)
     * @param dt Simulationszeitschritt
     * @return true = angezogen
     */
    boolean update(double coilCurrent, double iNom, double dt) {
	if (iNom <= 0 || dt <= 0) {
	    reset();
	    return false;
	}
	double a = Math.exp(-dt / TAU_S);
	if (a < 0)
	    a = 0;
	if (a > 1)
	    a = 1;
	double absI = Math.abs(coilCurrent);
	avgAbsI = a * avgAbsI + (1 - a) * absI;
	if (absI < iNom * OPEN_FRAC)
	    lowITime += dt;
	else
	    lowITime = 0;
	double onTh = iNom * ON_FRAC;
	double offTh = iNom * OFF_FRAC;
	if (!on) {
	    if (avgAbsI >= onTh)
		on = true;
	} else if (avgAbsI < offTh || lowITime >= OPEN_S) {
	    on = false;
	    if (lowITime >= OPEN_S) {
		avgAbsI = 0;
		lowITime = 0;
	    }
	}
	return on;
    }
}

/**
 * Schützspule A1–A2 = CircuitJS {@link Inductor} plus Kupfer-R.
 * Vorbild RelayCoilElm: L nach internem Knoten, R nach A2. Backward-Euler
 * (Schalter). Auto-L so, dass |jωL+R| bei 50 Hz ≈ U²/S (15 VA / 230 V → ~10,8 H).
 */
class EGTSchuetzCoilL {
    static final double F_HZ = 50;
    /** Kupfer + Eisenverluste ≈ 30 % von ωL — τ kurz genug für 50 Hz. */
    static final double R_FRAC = 0.3;

    static final int PRESET_AUTO = 0;
    static final int PRESET_HILF = 1;
    static final int PRESET_LEIST = 2;
    static final int PRESET_FREE = 3;

    Inductor ind;
    double inductance;

    static boolean lCustom(int flags) {
	return (flags & EGTStyle.FLAG_L_CUSTOM) != 0;
    }

    static double defaultHenries(double nomV, double nomVA) {
	if (nomV <= 0)
	    nomV = 230;
	if (nomVA <= 0)
	    nomVA = 15;
	double z = nomV * nomV / nomVA;
	return z / (2 * Math.PI * F_HZ * Math.sqrt(1 + R_FRAC * R_FRAC));
    }

    static int presetIndex(int flags, double nomV, double nomVA) {
	if (lCustom(flags))
	    return PRESET_FREE;
	if (Math.abs(nomV - 230) < 0.5 && Math.abs(nomVA - 8) < 0.05)
	    return PRESET_HILF;
	if (Math.abs(nomV - 230) < 0.5 && Math.abs(nomVA - 12) < 0.05)
	    return PRESET_LEIST;
	return PRESET_AUTO;
    }

    static EditInfo inductanceEditInfo(int flags, double L) {
	if (!lCustom(flags))
	    return null;
	return new EditInfo("Induktivität (H)", L, 0, 0).setPositive();
    }

    static EditInfo modelEditInfo(int flags, double nomV, double nomVA) {
	EditInfo ei = new EditInfo("Spulenmodell", 0, -1, -1);
	ei.choice = new Choice();
	ei.choice.add("Aus Nennwerten (50 Hz)");
	ei.choice.add("Hilfsschütz 230 V / 8 VA");
	ei.choice.add("Leistungsschütz 230 V / 12 VA");
	ei.choice.add("Frei (Henry eingeben)");
	ei.choice.select(presetIndex(flags, nomV, nomVA));
	return ei;
    }

    /**
     * @param pu pu[0]=Nennleistung, pu[1]=Nennspannung
     * @return flags
     */
    static int applyPreset(int idx, double pu[], EGTSchuetzCoilL coil, int flags) {
	if (idx == PRESET_FREE)
	    return flags | EGTStyle.FLAG_L_CUSTOM;
	flags &= ~EGTStyle.FLAG_L_CUSTOM;
	if (idx == PRESET_HILF) {
	    pu[0] = 8;
	    pu[1] = 230;
	} else if (idx == PRESET_LEIST) {
	    pu[0] = 12;
	    pu[1] = 230;
	}
	coil.inductance = 0;
	return flags;
    }

    double copperR() {
	double z = 2 * Math.PI * F_HZ * inductance;
	double r = z * R_FRAC;
	if (r < 20)
	    r = 20;
	return r;
    }

    void setup(SimulationManager sim, double L) {
	if (L <= 0)
	    L = defaultHenries(230, 15);
	inductance = L;
	if (ind == null)
	    ind = new Inductor(sim);
	else
	    ind.sim = sim;
	double i0 = ind.current;
	ind.setup(inductance, i0, Inductor.FLAG_BACK_EULER);
    }

    void reset() {
	if (ind != null)
	    ind.reset();
    }

    void stamp(SimulationManager sim, CircuitNode a1, CircuitNode mid,
	       CircuitNode a2) {
	setup(sim, inductance);
	ind.stamp(a1, mid);
	sim.stampResistor(mid, a2, copperR());
    }

    void startIteration(double vA1, double vMid) {
	if (ind == null)
	    return;
	ind.startIteration(vA1 - vMid);
    }

    void doStep(double vA1, double vMid) {
	if (ind == null)
	    return;
	ind.doStep(vA1 - vMid);
    }

    double calculateCurrent(double vA1, double vMid) {
	if (ind == null)
	    return 0;
	return ind.calculateCurrent(vA1 - vMid);
    }
}

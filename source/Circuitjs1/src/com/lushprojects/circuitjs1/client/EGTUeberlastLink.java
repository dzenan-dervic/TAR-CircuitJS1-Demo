/*
    TAR-Dervic EGT: Überlastrelais — Bezeichnung → ausgelöst
*/

package com.lushprojects.circuitjs1.client;

import java.util.HashMap;
import java.util.Vector;

/**
 * Kopplung Heizleiter ↔ getrennte Hilfskontakte über Kennzeichen (F1).
 * OR über alle Quellen gleicher Bezeichnung.
 */
interface EGTUeberlastSource extends EGTDesignatable {
    boolean ueberlastTripped();
}

class EGTUeberlastLink {
    static final HashMap<String, Boolean> tripped =
	    new HashMap<String, Boolean>();
    static final double R_ON = EGTSchuetzLink.R_ON;

    static String normalize(String des) {
	if (des == null)
	    return "F1";
	String t = des.trim().toUpperCase();
	return t.length() == 0 ? "F1" : t;
    }

    static void refresh(SimulationManager sim) {
	tripped.clear();
	if (sim == null || sim.elmList == null)
	    return;
	Vector<CircuitElm> list = sim.elmList;
	for (int i = 0; i < list.size(); i++) {
	    CircuitElm ce = list.elementAt(i);
	    if (!(ce instanceof EGTUeberlastSource))
		continue;
	    EGTUeberlastSource src = (EGTUeberlastSource) ce;
	    if (src.ueberlastTripped())
		tripped.put(normalize(src.egtDesignation()), Boolean.TRUE);
	}
    }

    static boolean isTripped(String des) {
	Boolean b = tripped.get(normalize(des));
	return b != null && b.booleanValue();
    }

    /** Menü-Kontakt: Kennzeichen der ausgewählten oder einzigen Quelle. */
    static String inheritDesignation() {
	CircuitElm ref = CircuitElm.mouseElmRef;
	if (ref instanceof EGTUeberlastSource)
	    return ((EGTUeberlastSource) ref).egtDesignation();
	if (CircuitElm.app == null || CircuitElm.app.elmList == null)
	    return null;
	String found = null;
	int n = 0;
	Vector<CircuitElm> list = CircuitElm.app.elmList;
	for (int i = 0; i < list.size(); i++) {
	    CircuitElm ce = list.elementAt(i);
	    if (!(ce instanceof EGTUeberlastSource))
		continue;
	    found = ((EGTUeberlastSource) ce).egtDesignation();
	    n++;
	}
	if (n == 1)
	    return found;
	return null;
    }

    static int nextSpawnX(CircuitElm host, String des) {
	int px = host.snapGrid(host.x2 + 16);
	if (CircuitElm.app == null || CircuitElm.app.elmList == null)
	    return px;
	des = normalize(des);
	Vector<CircuitElm> list = CircuitElm.app.elmList;
	for (int i = 0; i < list.size(); i++) {
	    CircuitElm ce = list.elementAt(i);
	    if (ce == host || !(ce instanceof EGTUeberlastKontaktElm))
		continue;
	    if (!normalize(((EGTUeberlastKontaktElm) ce).egtDesignation())
		    .equals(des))
		continue;
	    int x = host.snapGrid(ce.x2 + 16);
	    if (x > px)
		px = x;
	}
	return px;
    }

    static void placeKontakt(CircuitElm host, String des, boolean nc) {
	if (CircuitElm.app == null || CircuitElm.app.elmList == null
		|| host == null)
	    return;
	CirSim app = CircuitElm.app;
	app.undoManager.pushUndo();
	CircuitElm ce = CirSim.constructElement("EGTUeberlastKontaktElm",
						nextSpawnX(host, des),
						host.snapGrid(host.y));
	if (!(ce instanceof EGTUeberlastKontaktElm))
	    return;
	EGTUeberlastKontaktElm k = (EGTUeberlastKontaktElm) ce;
	if (nc)
	    k.flags |= EGTUeberlastKontaktElm.FLAG_NC;
	else
	    k.flags &= ~EGTUeberlastKontaktElm.FLAG_NC;
	k.setEgtDesignation(des);
	app.elmList.addElement(k);
	k.setPoints();
	k.draggingDone();
	app.needAnalyze();
	app.undoManager.writeRecoveryToStorage();
	app.unsavedChanges = true;
    }
}

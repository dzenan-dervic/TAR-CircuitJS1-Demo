package com.lushprojects.circuitjs1.client;

import java.util.Vector;

/**
 * Nächstes freies Kennzeichen beim Platzieren (Q1 → Q2).
 * Nur Maus-Neuablage — Dump/XML/Menü-Probe bleiben unverändert.
 * Zähler pro Elementklasse, damit Spule Q1 und Kontakt Q1 koppeln können.
 */
class EGTDesignation {
    static final int MAX_N = 9999;

    static void assignOnPlace(CircuitElm ce) {
	if (!(ce instanceof EGTDesignatable))
	    return;
	if (ce instanceof EGTSchuetzHilfskontaktElm) {
	    EGTSchuetzHilfskontaktElm h = (EGTSchuetzHilfskontaktElm) ce;
	    String inherited = EGTSchuetzLink.inheritSchuetzDesignation();
	    if (inherited != null)
		h.setEgtDesignation(inherited);
	    h.assignNextContactNo();
	    return;
	}
	if (ce instanceof EGTUeberlastKontaktElm) {
	    EGTUeberlastKontaktElm k = (EGTUeberlastKontaktElm) ce;
	    String inherited = EGTUeberlastLink.inheritDesignation();
	    if (inherited != null)
		k.setEgtDesignation(inherited);
	    return;
	}
	EGTDesignatable d = (EGTDesignatable) ce;
	String cur = d.egtDesignation();
	if (cur == null)
	    return;
	cur = cur.trim();
	if (cur.length() == 0)
	    return;
	d.setEgtDesignation(nextFree(ce, cur));
    }

    static String nextFree(CircuitElm self, String current) {
	String prefix = prefixOf(current);
	if (prefix.length() == 0)
	    return current;
	int start = numberOf(current);
	if (start < 1)
	    start = 1;
	boolean used[] = new boolean[MAX_N + 1];
	markUsed(self, prefix, used);
	int n = start;
	while (n <= MAX_N && used[n])
	    n++;
	if (n > MAX_N)
	    return current;
	return prefix + n;
    }

    static void markUsed(CircuitElm self, String prefix, boolean used[]) {
	Vector<CircuitElm> list = null;
	if (CircuitElm.app != null)
	    list = CircuitElm.app.elmList;
	if (list == null)
	    return;
	Class cls = self.getClass();
	for (int i = 0; i < list.size(); i++) {
	    CircuitElm ce = list.elementAt(i);
	    if (ce == self || ce.getClass() != cls)
		continue;
	    if (!(ce instanceof EGTDesignatable))
		continue;
	    String des = ((EGTDesignatable) ce).egtDesignation();
	    if (des == null || !prefixOf(des).equals(prefix))
		continue;
	    int n = numberOf(des);
	    if (n < 1)
		n = 1;
	    if (n <= MAX_N)
		used[n] = true;
	}
    }

    static String prefixOf(String s) {
	if (s == null)
	    return "";
	s = s.trim();
	int i = s.length();
	while (i > 0) {
	    char c = s.charAt(i - 1);
	    if (c < '0' || c > '9')
		break;
	    i--;
	}
	return s.substring(0, i);
    }

    static int numberOf(String s) {
	if (s == null)
	    return 0;
	s = s.trim();
	int i = s.length();
	while (i > 0) {
	    char c = s.charAt(i - 1);
	    if (c < '0' || c > '9')
		break;
	    i--;
	}
	if (i >= s.length())
	    return 0;
	try {
	    return Integer.parseInt(s.substring(i));
	} catch (Exception e) {
	    return 0;
	}
    }
}

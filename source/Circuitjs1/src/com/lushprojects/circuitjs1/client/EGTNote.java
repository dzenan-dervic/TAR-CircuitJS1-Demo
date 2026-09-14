package com.lushprojects.circuitjs1.client;

import com.lushprojects.circuitjs1.client.util.Locale;

/**
 * Optionale Schüler-Beschriftung unter dem Betriebsmittelkennzeichen (Q2, M1, …).
 * Länge und Steuerzeichen sind begrenzt, damit der Plan nicht aus dem Raster läuft.
 */
class EGTNote {
    static final int MAX_LEN = 20;

    static String clamp(String s) {
	if (s == null)
	    return "";
	StringBuilder sb = new StringBuilder();
	for (int i = 0; i < s.length() && sb.length() < MAX_LEN; i++) {
	    char c = s.charAt(i);
	    if (c == '\n' || c == '\r' || c == '\t')
		c = ' ';
	    if (c < 32)
		continue;
	    sb.append(c);
	}
	int a = 0;
	int b = sb.length();
	while (a < b && sb.charAt(a) == ' ')
	    a++;
	while (b > a && sb.charAt(b - 1) == ' ')
	    b--;
	return sb.substring(a, b);
    }

    static String dumpSuffix(String note) {
	note = clamp(note);
	if (note.length() == 0)
	    return "";
	return " " + CustomLogicModel.escape(note);
    }

    static String readOptional(StringTokenizer st) {
	if (st == null || !st.hasMoreTokens())
	    return "";
	return clamp(CustomLogicModel.unescape(st.nextToken()));
    }

    static void dumpXml(com.google.gwt.xml.client.Element elem, String note) {
	note = clamp(note);
	if (note.length() == 0)
	    return;
	XMLSerializer.dumpAttr(elem, "note", note);
    }

    static String undumpXml(XMLDeserializer xml, String fallback) {
	String def = fallback == null ? "" : fallback;
	return clamp(xml.parseStringAttr("note", def));
    }

    static EditInfo editInfo(String note) {
	EditInfo ei = new EditInfo(Locale.LS("Beschriftung"), clamp(note));
	ei.maxTextLen = MAX_LEN;
	return ei;
    }

    static EditInfo editAt(EGTDesignatable d, int n, int noteIndex) {
	if (d == null || n != noteIndex)
	    return null;
	return editInfo(d.egtNote());
    }

    static int shiftAfter(int n, int noteIndex) {
	if (n > noteIndex)
	    return n - 1;
	return n;
    }

    static boolean applyAt(EGTDesignatable d, int n, int noteIndex,
			   EditInfo ei) {
	if (d == null || n != noteIndex || ei == null || ei.textf == null)
	    return false;
	d.setEgtNote(ei.textf.getText());
	return true;
    }

    static String of(Object elm) {
	if (elm instanceof EGTDesignatable)
	    return clamp(((EGTDesignatable) elm).egtNote());
	return "";
    }
}

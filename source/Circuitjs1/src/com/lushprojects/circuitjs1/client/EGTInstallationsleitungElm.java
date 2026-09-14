/*
    TAR-Dervic EGT extension: Installationsleitung für Übersichtsschaltpläne.
*/

package com.lushprojects.circuitjs1.client;

import com.lushprojects.circuitjs1.client.util.Locale;

/**
 * Neutrale EGT-Leitung für Installationsschaltpläne.
 *
 * Die Aderzahl wird zusammen mit einem schrägen Kennstrich automatisch am
 * längsten Leitungsabschnitt gezeichnet. Sie wird im vorhandenen Feld
 * {@code bezeichnung} gespeichert, damit Text-, XML- und Legacy-Dumps ohne
 * zusätzliche variable Token auskommen.
 */
class EGTInstallationsleitungElm extends EGTLeitungElm {
    static final int DEFAULT_ADERZAHL = 3;

    public EGTInstallationsleitungElm(int xx, int yy) {
	super(xx, yy);
	leiterArt = ROLE_L3;
	bezeichnung = Integer.toString(DEFAULT_ADERZAHL);
	flags &= ~FLAG_SHOW_BEZEICHNUNG;
    }

    public EGTInstallationsleitungElm(int xa, int ya, int xb, int yb, int f,
				     StringTokenizer st) {
	super(xa, ya, xb, yb, f, st);
	leiterArt = ROLE_L3;
	bezeichnung = normalizeAderzahl(bezeichnung);
	flags &= ~FLAG_SHOW_BEZEICHNUNG;
    }

    int getDumpType() { return 478; }
    String getXmlDumpType() { return "EGTInstallationsleitung"; }
    int getShortcut() { return 0; }

    String normalizeAderzahl(String value) {
	try {
	    int n = Integer.parseInt(value);
	    if (n >= 1 && n <= 99)
		return Integer.toString(n);
	} catch (Exception e) {}
	return Integer.toString(DEFAULT_ADERZAHL);
    }

    int aderzahl() {
	return Integer.parseInt(normalizeAderzahl(bezeichnung));
    }

    Color roleColor() {
	return lightGrayColor;
    }

    void doDots(Graphics g) {
	// Übersichtskabel zeigen bewusst keine Stromanimation.
    }

    void draw(Graphics g) {
	super.draw(g);
	drawAderkennzeichnung(g);
    }

    void drawAderkennzeichnung(Graphics g) {
	if (routePoints == null || routePoints.size() < 2)
	    return;
	Point bestA = null;
	Point bestB = null;
	int bestLengthSq = -1;
	for (int i = 0; i < routePoints.size() - 1; i++) {
	    Point a = routePoints.get(i);
	    Point b = routePoints.get(i + 1);
	    int dx = b.x - a.x;
	    int dy = b.y - a.y;
	    int lengthSq = dx * dx + dy * dy;
	    if (lengthSq > bestLengthSq) {
		bestLengthSq = lengthSq;
		bestA = a;
		bestB = b;
	    }
	}
	if (bestA == null || bestB == null)
	    return;

	int mx = (bestA.x + bestB.x) / 2;
	int my = (bestA.y + bestB.y) / 2;
	Color markerColor = needsHighlight() ? selectColor : lightGrayColor;
	g.setColor(markerColor);
	g.setLineWidth(1.5);
	g.drawLine(mx - 8, my + 8, mx + 8, my - 8);
	g.setLineWidth(1.0);
	g.setFont(EGTStyle.pinLabelFont());
	String label = Integer.toString(aderzahl());
	g.drawString(label, mx + 12, my - 8);
	adjustBbox(mx - 10, my - 12, mx + 32, my + 10);
    }

    void getInfo(String arr[]) {
	arr[0] = "EGT-Installationsleitung (Übersicht)";
	arr[1] = "Aderzahl = " + aderzahl();
    }

    public EditInfo getEditInfo(int n) {
	if (n == 0)
	    return new EditInfo(Locale.LS("Aderzahl"), aderzahl(), 1, 99)
		    .setDimensionless();
	return null;
    }

    public void setEditValue(int n, EditInfo ei) {
	if (n == 0) {
	    int value = (int) Math.round(ei.value);
	    if (value < 1)
		value = 1;
	    if (value > 99)
		value = 99;
	    bezeichnung = Integer.toString(value);
	}
    }
}

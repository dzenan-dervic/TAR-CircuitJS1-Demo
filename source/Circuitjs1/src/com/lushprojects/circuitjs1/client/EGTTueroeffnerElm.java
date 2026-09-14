/*
    TAR-Dervic EGT extension: Türöffner L/N (ZH + Übersicht), I203 / INST-10
*/

package com.lushprojects.circuitjs1.client;

import com.lushprojects.circuitjs1.client.util.Locale;

/**
 * EGT Türöffner: ZH-Box mit Spule+Dreieck, L oben / N unten (kein PE).
 * Übersicht: Stem + Symbol. Bei Strom: „angezogen“.
 * Elektrisch: Widerstand L–N (getConnection true). Skalierbar.
 */
class EGTTueroeffnerElm extends ChipElm implements EGTDesignatable {
    static final String[] PIN_LABELS = { "L", "N" };
    static final int MIN_SIZE_X = 3;
    static final int MIN_SIZE_Y = 3;

    String designation = "Y1";
    String note = "";

    public String egtDesignation() { return designation; }
    public void setEgtDesignation(String d) {
	if (d != null)
	    designation = d;
    }
    public String egtNote() { return note; }
    public void setEgtNote(String s) { note = EGTNote.clamp(s); }
    double nom_pow = 10;
    double nom_v = 230;
    double resistance = 230 * 230 / 10.;

    public EGTTueroeffnerElm(int xx, int yy) {
	super(xx, yy);
	setSize(2);
	sizeX = 5;
	sizeY = 5;
	updateResistance();
	layoutPins();
    }

    public EGTTueroeffnerElm(int xa, int ya, int xb, int yb, int f,
			     StringTokenizer st) {
	super(xa, ya, xb, yb, f, st);
	setSize((f & FLAG_SMALL) != 0 ? 1 : 2);
	if (st.hasMoreTokens()) {
	    String t = st.nextToken();
	    try {
		sizeX = Integer.parseInt(t);
		if (st.hasMoreTokens())
		    sizeY = Integer.parseInt(st.nextToken());
		if (st.hasMoreTokens())
		    designation = CustomLogicModel.unescape(st.nextToken());
		if (st.hasMoreTokens())
		    nom_pow = Double.parseDouble(st.nextToken());
		if (st.hasMoreTokens())
		    nom_v = Double.parseDouble(st.nextToken());
	    } catch (Exception e) {
		designation = CustomLogicModel.unescape(t);
	    }
	}
	if (sizeX < MIN_SIZE_X)
	    sizeX = MIN_SIZE_X;
	if (sizeY < MIN_SIZE_Y)
	    sizeY = MIN_SIZE_Y;
	if (nom_pow <= 0)
	    nom_pow = 10;
	if (nom_v <= 0)
	    nom_v = 230;
	updateResistance();
	layoutPins();
	setPoints();
    
	note = EGTNote.readOptional(st);
}

    void updateResistance() {
	resistance = nom_v * nom_v / nom_pow;
	if (resistance < 1)
	    resistance = 1;
    }

    boolean isDigitalChip() { return false; }
    boolean uebersicht() { return (flags & EGTStyle.FLAG_UEBERSICHT) != 0; }

    String getChipName() { return "EGT-Türöffner"; }

    int getPostCount() { return uebersicht() ? 1 : 2; }
    int getVoltageSourceCount() { return 0; }

    void setupPins() {
	if (sizeX < MIN_SIZE_X)
	    sizeX = 5;
	if (sizeY < MIN_SIZE_Y)
	    sizeY = 5;
	pins = new Pin[getPostCount()];
	for (int i = 0; i < pins.length; i++) {
	    pins[i] = new Pin(0, SIDE_N, "");
	    pins[i].output = false;
	}
	placePins();
    }

    void layoutPins() {
	if (pins == null || pins.length != getPostCount())
	    setupPins();
	else
	    placePins();
    }

    void placePins() {
	if (sizeX < MIN_SIZE_X)
	    sizeX = MIN_SIZE_X;
	if (sizeY < MIN_SIZE_Y)
	    sizeY = MIN_SIZE_Y;
	int mid = sizeX / 2;
	if (uebersicht()) {
	    pins[0].pos = mid;
	    pins[0].side0 = SIDE_N;
	    pins[0].side = SIDE_N;
	    pins[0].output = false;
	    return;
	}
	pins[0].pos = mid;
	pins[0].side0 = SIDE_N;
	pins[0].side = SIDE_N;
	pins[0].output = false;
	pins[1].pos = mid;
	pins[1].side0 = SIDE_S;
	pins[1].side = SIDE_S;
	pins[1].output = false;
    }

    void drag(int xx, int yy) {
	xx = snapGrid(xx);
	yy = snapGrid(yy);
	if (cspc2 < 1)
	    setSize(2);
	int dx = Math.abs(xx - x);
	int dy = Math.abs(yy - y);
	sizeX = Math.max(MIN_SIZE_X, (dx + cspc2 / 2) / cspc2);
	sizeY = Math.max(MIN_SIZE_Y, (dy + cspc2 / 2) / cspc2);
	layoutPins();
	x2 = x + sizeX * cspc2;
	y2 = y;
	setPoints();
    }

    boolean creationFailed() {
	return sizeX < MIN_SIZE_X || sizeY < MIN_SIZE_Y;
    }

    boolean getConnection(int n1, int n2) {
	if (uebersicht())
	    return false;
	return (n1 == 0 && n2 == 1) || (n1 == 1 && n2 == 0);
    }

    void stamp() {
	if (uebersicht())
	    return;
	sim.stampResistor(nodes[0], nodes[1], resistance);
    }

    void startIteration() {}
    void execute() {}
    void doStep() {}

    void calculateCurrent() {
	EGTStyle.clearPinCurrents(pins);
	if (uebersicht() || resistance <= 0) {
	    current = 0;
	    return;
	}
	current = (volts[0] - volts[1]) / resistance;
	EGTStyle.pinCurrentThru(pins, 0, 1, current);
    }

    boolean isEnergized() {
	if (uebersicht())
	    return false;
	double iNom = nom_v / resistance;
	return Math.abs(current) > iNom * 0.05;
    }

    void boxBounds(int out[]) {
	int xr = x + cspc2 - cspc;
	int yr = y - cspc;
	out[0] = xr;
	out[1] = yr;
	out[2] = xr + sizeX * cspc2;
	out[3] = yr + sizeY * cspc2;
    }

    void drawChip(Graphics g) {
	g.save();
	int[] b = new int[4];
	boxBounds(b);
	Color stroke = needsHighlight() ? selectColor : lightGrayColor;
	if (uebersicht())
	    drawUebersicht(g, b[0], b[1], b[2], b[3], stroke);
	else
	    drawZh(g, b[0], b[1], b[2], b[3], stroke);
	drawPosts(g);
	g.restore();
    }

    void drawZh(Graphics g, int left, int top, int right, int bottom,
		Color stroke) {
	int w = right - left;
	int h = bottom - top;
	int cx = (left + right) / 2;
	int cy = (top + bottom) / 2;
	int symW = Math.max(28, w * 32 / 100);
	int symH = Math.max(16, h * 22 / 100);
	int sx = cx - symW / 2;
	int sy = cy - symH / 2;
	int coilBottom = sy + symH;

	g.setColor(stroke);
	drawThickPolygon(g, rectPointsX, rectPointsY, 4);

	g.setColor(stroke);
	g.setLineWidth(2.5);
	g.context.setLineCap("butt");
	g.drawLine(cx, pins[0].post.y, cx, sy);
	g.drawLine(cx, coilBottom, cx, pins[1].post.y);

	int triW = EGTStyle.drawDoorOpener(g, sx, sy, symW, symH, stroke);

	g.setColor(whiteColor);
	EGTStyle.drawPinLabelBeside(g, this, pins[0].post.x, pins[0].post.y,
				    PIN_LABELS[0], whiteColor);
	EGTStyle.drawPinLabelBeside(g, this, pins[1].post.x, pins[1].post.y,
				    PIN_LABELS[1], whiteColor);

	int[] ab = EGTStyle.drawHousingAnno(g, this, left, top, right, bottom,
		whiteColor, designation,
		isEnergized() ? "angezogen" : "Ruhe", null);
	adjustBbox(ab[0], ab[1],
		   Math.max(ab[2], sx + symW + triW + 8),
		   Math.max(ab[3], bottom + 20));
	g.setLineWidth(1.0);

	curcount = updateDotCount(current, curcount);
	if (isEnergized()) {
	    drawDots(g, pins[0].post, new Point(cx, sy), curcount);
	    drawDots(g, new Point(cx, coilBottom), pins[1].post, curcount);
	}
    }

    void drawUebersicht(Graphics g, int left, int top, int right, int bottom,
			Color stroke) {
	int cx = (left + right) / 2;
	int cy = (top + bottom) / 2;
	int symW = Math.max(28, Math.min(right - left, bottom - top) * 35 / 100);
	int symH = Math.max(16, symW * 55 / 100);
	int triW = (int) (symH * 0.9);
	int sx = cx - (symW + triW) / 2;
	int sy = cy - symH / 2 + Math.max(2, (bottom - top) / 20);
	int rectCx = sx + symW / 2;

	g.setColor(stroke);
	g.setLineWidth(2.5);
	g.drawLine(pins[0].post.x, pins[0].post.y, rectCx, sy);
	EGTStyle.drawDoorOpener(g, sx, sy, symW, symH, stroke);
	g.setLineWidth(1.0);

	g.setFont(EGTStyle.pinLabelFont());
	g.setColor(whiteColor);
	int tw = EGTStyle.measureDesigNote(g, this, designation);
	EGTStyle.drawDesigAndNote(g, this, designation,
				 rectCx - tw / 2, sy + symH + 16);
	String st = isEnergized() ? "angezogen" : "Ruhe";
	g.drawString(st, sx + symW + triW + 8, sy + symH / 2);
	adjustBbox(left, pins[0].post.y - 8,
		   Math.max(right, sx + symW + triW) + Math.max(tw, 70) + 16,
		   bottom);
    }

    String dump() {
	return super.dump() + " " + sizeX + " " + sizeY + " "
		+ CustomLogicModel.escape(designation) + " " + nom_pow + " "
		+ nom_v + EGTNote.dumpSuffix(note);
    }

    void dumpXml(com.google.gwt.xml.client.Document doc,
		 com.google.gwt.xml.client.Element elem) {
	super.dumpXml(doc, elem);
	XMLSerializer.dumpAttr(elem, "sx", sizeX);
	XMLSerializer.dumpAttr(elem, "sy", sizeY);
	XMLSerializer.dumpAttr(elem, "des", designation);
	EGTNote.dumpXml(elem, note);
	XMLSerializer.dumpAttr(elem, "np", nom_pow);
	XMLSerializer.dumpAttr(elem, "nv", nom_v);
    }

    void undumpXml(XMLDeserializer xml) {
	super.undumpXml(xml);
	sizeX = (int) xml.parseDoubleAttr("sx", sizeX);
	sizeY = (int) xml.parseDoubleAttr("sy", sizeY);
	designation = xml.parseStringAttr("des", designation);
	note = EGTNote.undumpXml(xml, note);
	nom_pow = xml.parseDoubleAttr("np", nom_pow);
	nom_v = xml.parseDoubleAttr("nv", nom_v);
	if (nom_pow <= 0)
	    nom_pow = 10;
	if (nom_v <= 0)
	    nom_v = 230;
	updateResistance();
	layoutPins();
	setPoints();
    }

    int getDumpType() { return 452; }
    String getXmlDumpType() { return "EGTTueroeffner"; }

    void getInfo(String arr[]) {
	arr[0] = getChipName() + " (" + designation + ")"
		+ (uebersicht() ? " [Übersicht]" : " [ZH]");
	if (uebersicht()) {
	    arr[1] = "Anschluss = " + getVoltageText(volts[0]);
	    arr[2] = null;
	} else {
	    arr[1] = isEnergized() ? "angezogen" : "Ruhe";
	    arr[2] = "L = " + getVoltageText(volts[0]);
	    arr[3] = "N = " + getVoltageText(volts[1]);
	    arr[4] = "P = " + getUnitText(getPower(), "W");
	}
    }

    double getPower() {
	if (uebersicht())
	    return 0;
	return current * (volts[0] - volts[1]);
    }

    public EditInfo getChipEditInfo(int n) {
	if (n == 0)
	    return new EditInfo("Bezeichnung", designation);
	if (n == 1)
	    return EditInfo.createCheckbox(Locale.LS("Übersichtsdarstellung"),
					  uebersicht());
	if (n == 2)
	    return new EditInfo("Nennleistung (W)", nom_pow, 0, 0).setPositive();
	if (n == 3)
	    return new EditInfo("Nennspannung (V)", nom_v, 0, 0).setPositive();
	return null;
    }

    public void setChipEditValue(int n, EditInfo ei) {
	if (n == 0)
	    designation = ei.textf.getText();
	if (n == 1) {
	    flags = ei.changeFlag(flags, EGTStyle.FLAG_UEBERSICHT);
	    setupPins();
	    allocNodes();
	    setPoints();
	}
	if (n == 2 && ei.value > 0) {
	    nom_pow = ei.value;
	    updateResistance();
	}
	if (n == 3 && ei.value > 0) {
	    nom_v = ei.value;
	    updateResistance();
	}
    }
}

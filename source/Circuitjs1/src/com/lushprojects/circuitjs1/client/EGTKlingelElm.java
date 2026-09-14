/*
    TAR-Dervic EGT extension: Klingel / Gong L/N (ZH + Übersicht), I203 / INST-10
*/

package com.lushprojects.circuitjs1.client;

import com.lushprojects.circuitjs1.client.util.Locale;

/**
 * EGT Klingel: ZH-Box mit IEC-Klingelsymbol, L/N oben (kein PE).
 * Übersicht: Stem + Klingel. Bei Strom: WLAN-artige Schallbögen.
 * Elektrisch: Widerstand L–N (getConnection true). Skalierbar.
 */
class EGTKlingelElm extends ChipElm implements EGTDesignatable {
    static final String[] PIN_LABELS = { "L", "N" };
    static final int MIN_SIZE_X = 3;
    static final int MIN_SIZE_Y = 2;

    String designation = "K1";
    String note = "";

    public String egtDesignation() { return designation; }
    public void setEgtDesignation(String d) {
	if (d != null)
	    designation = d;
    }
    public String egtNote() { return note; }
    public void setEgtNote(String s) { note = EGTNote.clamp(s); }
    double nom_pow = 8;
    double nom_v = 230;
    double resistance = 230 * 230 / 8.;

    public EGTKlingelElm(int xx, int yy) {
	super(xx, yy);
	setSize(2);
	sizeX = 6;
	sizeY = 5;
	updateResistance();
	layoutPins();
    }

    public EGTKlingelElm(int xa, int ya, int xb, int yb, int f,
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
	    nom_pow = 8;
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

    String getChipName() { return "EGT-Klingel"; }

    int getPostCount() { return uebersicht() ? 1 : 2; }
    int getVoltageSourceCount() { return 0; }

    void setupPins() {
	if (sizeX < MIN_SIZE_X)
	    sizeX = 6;
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
	if (uebersicht()) {
	    pins[0].pos = sizeX / 2;
	    pins[0].side0 = SIDE_N;
	    pins[0].side = SIDE_N;
	    pins[0].output = false;
	    return;
	}
	int inset = Math.max(0, (sizeX - 1) / 5);
	pins[0].pos = inset;
	pins[1].pos = sizeX - 1 - inset;
	if (pins[0].pos >= pins[1].pos) {
	    pins[0].pos = 0;
	    pins[1].pos = Math.max(1, sizeX - 1);
	}
	for (int i = 0; i < 2; i++) {
	    pins[i].side0 = SIDE_N;
	    pins[i].side = SIDE_N;
	    pins[i].output = false;
	}
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

    /** L–N verbunden (wie Widerstand) — sonst Matrix-Trennung / Scheinstrom. */
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

    boolean isRinging() {
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
	int bellR = Math.min(w, h) * 19 / 100;
	if (bellR < 10)
	    bellR = 10;
	int leg = Math.max(6, bellR * 55 / 100);
	int baseY = cy + h / 20;
	int legEnd = baseY + leg;
	int lx = pins[0].post.x;
	int nx = pins[1].post.x;
	int gap = (int) (bellR * 0.38);
	if (gap < 3)
	    gap = 3;
	int legL = cx - gap;
	int legR = cx + gap;

	g.setColor(stroke);
	drawThickPolygon(g, rectPointsX, rectPointsY, 4);

	g.setColor(stroke);
	g.setLineWidth(2.5);
	g.context.setLineCap("butt");
	g.drawLine(lx, pins[0].post.y, lx, legEnd);
	g.drawLine(lx, legEnd, legL, legEnd);
	g.drawLine(nx, pins[1].post.y, nx, legEnd);
	g.drawLine(nx, legEnd, legR, legEnd);

	EGTStyle.drawBell(g, cx, baseY, bellR, leg, stroke);
	if (isRinging())
	    EGTStyle.drawBellSoundWaves(g, cx, baseY, bellR, sim.t * 6.5,
					stroke);

	g.setColor(whiteColor);
	for (int i = 0; i < 2; i++)
	    EGTStyle.drawPinLabelBeside(g, this, pins[i].post.x, pins[i].post.y,
					PIN_LABELS[i], whiteColor);

	int[] ab = EGTStyle.drawHousingAnno(g, this, left, top, right, bottom,
		whiteColor, designation, isRinging() ? "klingelt" : "Ruhe",
		null);
	adjustBbox(ab[0], ab[1], ab[2], ab[3]);
	g.setLineWidth(1.0);

	curcount = updateDotCount(current, curcount);
	if (isRinging()) {
	    drawDots(g, pins[0].post, new Point(lx, legEnd), curcount);
	    drawDots(g, new Point(lx, legEnd), new Point(legL, legEnd),
		     curcount);
	    drawDots(g, new Point(legR, legEnd), new Point(nx, legEnd),
		     -curcount);
	    drawDots(g, new Point(nx, legEnd), pins[1].post, -curcount);
	}
    }

    void drawUebersicht(Graphics g, int left, int top, int right, int bottom,
			Color stroke) {
	int cx = (left + right) / 2;
	int cy = (top + bottom) / 2;
	int bellR = Math.max(10, Math.min(right - left, bottom - top) / 5);
	int leg = Math.max(6, bellR / 2);
	int baseY = cy + Math.max(4, (bottom - top) / 14);
	int stemTop = Math.min(pins[0].post.y + 8, baseY - bellR);

	g.setColor(stroke);
	g.setLineWidth(2.5);
	g.drawLine(pins[0].post.x, pins[0].post.y, cx, stemTop);
	g.drawLine(cx, stemTop, cx, baseY - bellR);
	EGTStyle.drawBell(g, cx, baseY, bellR, leg, stroke);
	if (isRinging())
	    EGTStyle.drawBellSoundWaves(g, cx, baseY, bellR, sim.t * 6.5,
					stroke);
	g.setLineWidth(1.0);

	g.setFont(EGTStyle.pinLabelFont());
	g.setColor(whiteColor);
	int tw = EGTStyle.measureDesigNote(g, this, designation);
	int extra = EGTStyle.drawDesigAndNote(g, this, designation,
					     cx + bellR + 10, baseY + leg + 4);
	String st = isRinging() ? "klingelt" : "Ruhe";
	g.drawString(st, cx + bellR + 10, baseY + leg + 20 + extra);
	adjustBbox(left, pins[0].post.y - 8, right + Math.max(tw, 70) + 24,
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
	    nom_pow = 8;
	if (nom_v <= 0)
	    nom_v = 230;
	updateResistance();
	layoutPins();
	setPoints();
    }

    int getDumpType() { return 451; }
    String getXmlDumpType() { return "EGTKlingel"; }

    void getInfo(String arr[]) {
	arr[0] = getChipName() + " (" + designation + ")"
		+ (uebersicht() ? " [Übersicht]" : " [ZH]");
	if (uebersicht()) {
	    arr[1] = "Anschluss = " + getVoltageText(volts[0]);
	    arr[2] = null;
	} else {
	    arr[1] = isRinging() ? "klingelt" : "Ruhe";
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

/*
    TAR-Dervic EGT: Gleichstrommotor (MOD-MOT-02 / M505), Dump 475
*/

package com.lushprojects.circuitjs1.client;

import com.lushprojects.circuitjs1.client.util.Locale;

/**
 * DIN-Kreis + M + Gleichstrom-Kennzeichen (=). Klemmen +/−.
 * Didaktik: linearer Ankerwiderstand R = Un²/Pn, Polarität = Drehsinn.
 * Kein Upstream-DCMotorElm (Dump 415) — CTMS/Gegen-EMK bleibt dort.
 */
class EGTGleichstrommotorElm extends ChipElm implements EGTDesignatable {
    /**
     * Ungerade Kantenlänge: Pin pos = size/2 liegt auf der Kreisachse.
     * 6×6 setzte die Posts neben die Mitte → schräge Adern.
     */
    static final int SIZE_X = 5;
    static final int SIZE_Y = 5;
    static final double DEF_UN = 24;
    static final double DEF_PN = 50;
    static final double I_RUN = 0.05;
    static final double SPEED_MAX = 8;
    static final double SPEED_TAU = 0.15;
    /**
     * Bogenpfeil: gleiche Zeittaktung wie Strompunkte (currentMult, Slider
     * Stromgeschwindigkeit). SPEED_MAX*dt war ~1 U/s — zu langsam.
     */
    static final double ARROW_RATE = 0.015;
    static final Color COL_ACCENT = new Color(0x4a, 0x9e, 0xff);
    static final Color COL_IDLE = new Color(0x66, 0x66, 0x66);

    String designation = "M1";
    String note = "";
    double nom_v = DEF_UN;
    double nom_p = DEF_PN;
    double resistance = DEF_UN * DEF_UN / DEF_PN;
    double angle = Math.PI / 2;
    double speed;

    public String egtDesignation() { return designation; }
    public void setEgtDesignation(String d) {
	if (d != null)
	    designation = d;
    }
    public String egtNote() { return note; }
    public void setEgtNote(String s) { note = EGTNote.clamp(s); }

    public EGTGleichstrommotorElm(int xx, int yy) {
	super(xx, yy);
	flags &= ~EGTStyle.FLAG_UEBERSICHT;
	setSize(1);
	designation = "M1";
	nom_v = DEF_UN;
	nom_p = DEF_PN;
	updateResistance();
	applySize();
	layoutPins();
	syncEndpoints();
    }

    public EGTGleichstrommotorElm(int xa, int ya, int xb, int yb, int f,
				 StringTokenizer st) {
	super(xa, ya, xb, yb, f, st);
	flags &= ~EGTStyle.FLAG_UEBERSICHT;
	setSize(1);
	if (st.hasMoreTokens()) {
	    String t = st.nextToken();
	    try {
		Integer.parseInt(t);
		if (st.hasMoreTokens())
		    Integer.parseInt(st.nextToken());
		if (st.hasMoreTokens())
		    designation = CustomLogicModel.unescape(st.nextToken());
		if (st.hasMoreTokens())
		    nom_v = Double.parseDouble(st.nextToken());
		if (st.hasMoreTokens())
		    nom_p = Double.parseDouble(st.nextToken());
	    } catch (Exception e) {
		designation = CustomLogicModel.unescape(t);
	    }
	}
	if (nom_v <= 0)
	    nom_v = DEF_UN;
	if (nom_p <= 0)
	    nom_p = DEF_PN;
	updateResistance();
	applySize();
	layoutPins();
	allocNodes();
	syncEndpoints();
    
	note = EGTNote.readOptional(st);
}

    void updateResistance() {
	if (nom_v <= 0)
	    nom_v = DEF_UN;
	if (nom_p <= 0)
	    nom_p = DEF_PN;
	resistance = nom_v * nom_v / nom_p;
	if (resistance < 1)
	    resistance = 1;
    }

    void applySize() {
	sizeX = SIZE_X;
	sizeY = SIZE_Y;
    }

    boolean isDigitalChip() { return false; }
    boolean nonLinear() { return false; }
    boolean isWireEquivalent() { return false; }
    boolean isRemovableWire() { return false; }

    String getChipName() { return "EGT-Gleichstrommotor"; }

    int getPostCount() { return 2; }
    int getVoltageSourceCount() { return 0; }

    void setupPins() {
	applySize();
	pins = new Pin[2];
	pins[0] = new Pin(sizeX / 2, SIDE_N, "+");
	pins[1] = new Pin(sizeX / 2, SIDE_S, "−");
	pins[0].output = false;
	pins[1].output = false;
    }

    void layoutPins() {
	if (pins == null || pins.length != getPostCount())
	    setupPins();
	else {
	    applySize();
	    pins[0].pos = sizeX / 2;
	    pins[0].side0 = SIDE_N;
	    pins[0].side = SIDE_N;
	    pins[0].output = false;
	    pins[0].text = "+";
	    pins[1].pos = sizeX / 2;
	    pins[1].side0 = SIDE_S;
	    pins[1].side = SIDE_S;
	    pins[1].output = false;
	    pins[1].text = "−";
	}
    }

    int boxW() {
	return (isFlippedXY() ? sizeY : sizeX) * cspc2;
    }

    int boxH() {
	return (isFlippedXY() ? sizeX : sizeY) * cspc2;
    }

    void syncEndpoints() {
	if (cspc2 < 1)
	    setSize(1);
	x2 = x + boxW();
	y2 = y + boxH();
	setPoints();
    }

    void setPoints() {
	setSize(1);
	applySize();
	super.setPoints();
	if (cspc < 1)
	    return;
	int xr = x + cspc2 - cspc;
	int yr = y - cspc;
	int xs = boxW();
	int ys = boxH();
	if (isFlippedXY())
	    setBbox(xr - 8, yr - 40, xr + xs + 16, yr + ys + 16);
	else
	    setBbox(xr - 8, yr - 28, xr + xs + 110, yr + ys + 24);
    }

    /**
     * Wie Drehstrommotor: Zugrichtung setzt die Lage.
     * Waagerecht → ChipElm FLAG_FLIP_XY (+ links / − rechts).
     */
    void drag(int xx, int yy) {
	xx = snapGrid(xx);
	yy = snapGrid(yy);
	if (cspc2 < 1)
	    setSize(1);
	applySize();
	if (Math.abs(xx - x) > Math.abs(yy - y))
	    flags |= FLAG_FLIP_XY;
	else
	    flags &= ~FLAG_FLIP_XY;
	x2 = x + boxW();
	y2 = y + boxH();
	setPoints();
    }

    boolean pinsHorizontal() {
	return isFlippedXY();
    }

    boolean creationFailed() {
	syncEndpoints();
	return sizeX < SIZE_X || sizeY < SIZE_Y;
    }

    boolean getConnection(int n1, int n2) {
	return (n1 == 0 && n2 == 1) || (n1 == 1 && n2 == 0);
    }

    void stamp() {
	sim.stampResistor(nodes[0], nodes[1], resistance);
    }

    void startIteration() {}
    void execute() {}
    void doStep() {}

    void calculateCurrent() {
	EGTStyle.clearPinCurrents(pins);
	if (resistance <= 0) {
	    current = 0;
	    return;
	}
	current = (volts[0] - volts[1]) / resistance;
	EGTStyle.pinCurrentThru(pins, 0, 1, current);
    }

    double iNom() {
	return nom_p / nom_v;
    }

    boolean running() {
	return Math.abs(current) > iNom() * I_RUN;
    }

    boolean simRunning() {
	return app != null && app.simIsRunning();
    }

    /** +1 Rechtslauf (Strom + → −), −1 Linkslauf. */
    int directionSign() {
	if (!running())
	    return 0;
	return current >= 0 ? 1 : -1;
    }

    void stepFinished() {
	double target = directionSign() * SPEED_MAX;
	double dt = sim.timeStep;
	if (dt <= 0)
	    dt = 1e-5;
	double a = 1 - Math.exp(-dt / SPEED_TAU);
	speed += (target - speed) * a;
    }

    void reset() {
	super.reset();
	speed = 0;
	angle = Math.PI / 2;
	current = 0;
    }

    String statusText() {
	int d = directionSign();
	if (d > 0)
	    return Locale.LS("Rechtslauf");
	if (d < 0)
	    return Locale.LS("Linkslauf");
	return Locale.LS("Stillstand");
    }

    void boxBounds(int out[]) {
	if (rectPointsX != null && rectPointsY != null
		&& rectPointsX.length >= 4) {
	    int minx = rectPointsX[0], maxx = rectPointsX[0];
	    int miny = rectPointsY[0], maxy = rectPointsY[0];
	    for (int i = 1; i < 4; i++) {
		minx = min(minx, rectPointsX[i]);
		maxx = max(maxx, rectPointsX[i]);
		miny = min(miny, rectPointsY[i]);
		maxy = max(maxy, rectPointsY[i]);
	    }
	    out[0] = minx;
	    out[1] = miny;
	    out[2] = maxx;
	    out[3] = maxy;
	    return;
	}
	int xr = x + cspc2 - cspc;
	int yr = y - cspc;
	out[0] = xr;
	out[1] = yr;
	out[2] = xr + boxW();
	out[3] = yr + boxH();
    }

    Point rimToward(Point post, int cx, int cy, int r) {
	if (isFlippedXY())
	    return new Point(post.x < cx ? cx - r : cx + r, cy);
	return new Point(cx, post.y < cy ? cy - r : cy + r);
    }

    void drawChip(Graphics g) {
	g.save();
	try {
	    int[] b = new int[4];
	    boxBounds(b);
	    Color stroke = needsHighlight() ? selectColor : whiteColor;
	    drawZh(g, b[0], b[1], b[2], b[3], stroke);
	    drawPosts(g);
	} finally {
	    g.restore();
	}
    }

    void drawZh(Graphics g, int left, int top, int right, int bottom,
		Color stroke) {
	int cx = (left + right) / 2;
	int cy = (top + bottom) / 2;
	int w = right - left;
	int h = bottom - top;
	int r = Math.min(w, h) * 36 / 100;
	if (r < 14)
	    r = 14;
	boolean horiz = isFlippedXY();
	if (pins != null && pins.length >= 2
		&& pins[0].post != null && pins[1].post != null) {
	    if (horiz)
		cy = (pins[0].post.y + pins[1].post.y) / 2;
	    else
		cx = (pins[0].post.x + pins[1].post.x) / 2;
	}

	if (pins != null && pins.length >= 2) {
	    Point plus = pins[0].post;
	    Point minus = pins[1].post;
	    Point plusRim = rimToward(plus, cx, cy, r);
	    Point minusRim = rimToward(minus, cx, cy, r);
	    EGTStyle.drawConductor(g, plus.x, plus.y, plusRim.x, plusRim.y,
				   EGTStyle.COL_LIVE_PULSE, 3.5);
	    EGTStyle.drawConductor(g, minusRim.x, minusRim.y, minus.x, minus.y,
				   lightGrayColor, 3.5);
	    pins[0].curcount = updateDotCount(pins[0].current, pins[0].curcount);
	    pins[1].curcount = updateDotCount(pins[1].current, pins[1].curcount);
	    drawDots(g, plus, plusRim, pins[0].curcount);
	    drawDots(g, minusRim, minus, pins[1].curcount);
	    if (horiz) {
		EGTStyle.drawPinLabelToward(g, this, plus.x, plus.y, cx,
					    "+", whiteColor);
		EGTStyle.drawPinLabelToward(g, this, minus.x, minus.y, cx,
					    "−", whiteColor);
	    } else {
		EGTStyle.drawPinLabelBeside(g, this, plus.x, plus.y, "+",
					    whiteColor, true);
		EGTStyle.drawPinLabelBeside(g, this, minus.x, minus.y, "−",
					    whiteColor, true);
	    }
	}

	g.setColor(stroke);
	g.setLineWidth(2.5);
	g.context.setLineCap("butt");
	g.context.beginPath();
	g.context.arc(cx, cy, r, 0, 2 * Math.PI, false);
	g.context.stroke();
	g.setLineWidth(1.0);

	double rr = r * 0.82;
	double span = Math.PI * 0.55;
	int dir = directionSign();
	if (dir != 0) {
	    if (simRunning())
		angle += dir * Math.abs(currentMult) * ARROW_RATE;
	    drawArcArrow(g, cx, cy, rr, angle, span, dir > 0, COL_ACCENT, 2.2);
	} else
	    drawArcIdle(g, cx, cy, rr, -Math.PI / 2, span);

	int labelSize = Math.max(12, (int) Math.round(r * 0.44));
	int subSize = Math.max(10, (int) Math.round(labelSize * 0.68));
	g.setColor(stroke);
	g.context.setTextAlign("center");
	g.context.setTextBaseline("middle");
	g.setFont(new Font("normal", 0, labelSize));
	g.drawString("M", cx, (int) (cy - r * 0.08));
	g.setFont(new Font("normal", 0, subSize));
	g.drawString("=", cx, (int) (cy + r * 0.32));
	g.context.setTextAlign("left");
	g.context.setTextBaseline("alphabetic");

	String stTxt = statusText();
	g.save();
	g.setColor(needsHighlight() ? selectColor : whiteColor);
	g.setFont(EGTStyle.pinLabelFont());
	int tw = (int) g.measureWidth(designation);
	tw = (int) Math.max(tw, g.measureWidth(stTxt));
	g.context.setTextAlign("left");
	int tx;
	if (horiz && pins != null && pins.length >= 2
		&& pins[1].post != null)
	    tx = Math.max(pins[0].post.x, pins[1].post.x) + 12;
	else if (horiz)
	    tx = right + 10;
	else
	    tx = cx + r + 10;
	g.drawString(designation, tx, cy - 8);
	int extra = 0;
	String ntxt = EGTNote.of(this);
	if (ntxt.length() > 0) {
	    g.drawString(ntxt, tx, cy + 6);
	    extra = 14;
	    tw = (int) Math.max(tw, g.measureWidth(ntxt));
	}
	g.drawString(stTxt, tx, cy + 10 + extra);
	adjustBbox(cx - r - 16, top - 8, tx + tw + 8, bottom + 8);
	g.restore();
	g.context.setTextAlign("left");
	g.context.setTextBaseline("alphabetic");
    }

    void drawArcArrow(Graphics g, int cx, int cy, double rr, double aMid,
		      double span, boolean cw, Color color, double w) {
	double a0 = aMid - span / 2;
	double a1 = aMid + span / 2;
	g.setColor(color);
	g.setLineWidth(w);
	g.context.beginPath();
	if (cw)
	    g.context.arc(cx, cy, rr, a0, a1, false);
	else
	    g.context.arc(cx, cy, rr, a1, a0, true);
	g.context.stroke();

	double tipA = cw ? a1 : a0;
	double tang = tipA + (cw ? Math.PI / 2 : -Math.PI / 2);
	double tipX = cx + Math.cos(tipA) * rr;
	double tipY = cy + Math.sin(tipA) * rr;
	double ah = Math.max(7, rr * 0.26);
	double bx = tipX - Math.cos(tang) * ah;
	double by = tipY - Math.sin(tang) * ah;
	double nx = -Math.sin(tang) * ah * 0.45;
	double ny = Math.cos(tang) * ah * 0.45;
	g.context.beginPath();
	g.context.moveTo(tipX, tipY);
	g.context.lineTo(bx + nx, by + ny);
	g.context.lineTo(bx - nx, by - ny);
	g.context.closePath();
	g.context.fill();
	g.setLineWidth(1.0);
    }

    void drawArcIdle(Graphics g, int cx, int cy, double rr, double aMid,
		     double span) {
	g.setColor(COL_IDLE);
	g.setLineWidth(1.8);
	g.setLineDash(5, 4);
	g.context.beginPath();
	g.context.arc(cx, cy, rr, aMid - span / 2, aMid + span / 2, false);
	g.context.stroke();
	g.setLineDash(0, 0);
	g.setLineWidth(1.0);
    }

    String dump() {
	return super.dump() + " " + sizeX + " " + sizeY + " "
		+ CustomLogicModel.escape(designation) + " " + nom_v + " "
		+ nom_p + EGTNote.dumpSuffix(note);
    }

    void dumpXml(com.google.gwt.xml.client.Document doc,
		 com.google.gwt.xml.client.Element elem) {
	super.dumpXml(doc, elem);
	XMLSerializer.dumpAttr(elem, "sx", sizeX);
	XMLSerializer.dumpAttr(elem, "sy", sizeY);
	XMLSerializer.dumpAttr(elem, "des", designation);
	EGTNote.dumpXml(elem, note);
	XMLSerializer.dumpAttr(elem, "un", nom_v);
	XMLSerializer.dumpAttr(elem, "pn", nom_p);
    }

    void undumpXml(XMLDeserializer xml) {
	super.undumpXml(xml);
	flags &= ~EGTStyle.FLAG_UEBERSICHT;
	designation = xml.parseStringAttr("des", designation);
	note = EGTNote.undumpXml(xml, note);
	nom_v = xml.parseDoubleAttr("un", nom_v);
	nom_p = xml.parseDoubleAttr("pn", nom_p);
	if (nom_v <= 0)
	    nom_v = DEF_UN;
	if (nom_p <= 0)
	    nom_p = DEF_PN;
	updateResistance();
	setSize(1);
	applySize();
	layoutPins();
	allocNodes();
	syncEndpoints();
    }

    int getDumpType() { return 475; }
    String getXmlDumpType() { return "EGTGleichstrommotor"; }

    void getInfo(String arr[]) {
	arr[0] = getChipName() + " (" + designation + ")";
	arr[1] = "Un = " + getUnitText(nom_v, "V") + ", Pn = "
		+ getUnitText(nom_p, "W");
	arr[2] = "I = " + getCurrentText(current);
	arr[3] = Locale.LS("Zustand") + " = " + statusText();
	arr[4] = "R = " + getUnitText(resistance, Locale.LS("Ohm"))
		+ " (Un²/Pn)";
    }

    public EditInfo getChipEditInfo(int n) {
	if (n == 0)
	    return new EditInfo("Bezeichnung", designation);
	if (n == 1)
	    return new EditInfo("Nennspannung (V)", nom_v, 0, 0)
		    .setPositive();
	if (n == 2)
	    return new EditInfo("Nennleistung (W)", nom_p, 0, 0)
		    .setPositive();
	return null;
    }

    public void setChipEditValue(int n, EditInfo ei) {
	if (n == 0)
	    designation = ei.textf.getText();
	if (n == 1 && ei.value > 0) {
	    nom_v = ei.value;
	    updateResistance();
	    requestAnalyze();
	}
	if (n == 2 && ei.value > 0) {
	    nom_p = ei.value;
	    updateResistance();
	    requestAnalyze();
	}
    }

    void requestAnalyze() {
	if (CircuitElm.app != null)
	    CircuitElm.app.needAnalyze();
    }
}

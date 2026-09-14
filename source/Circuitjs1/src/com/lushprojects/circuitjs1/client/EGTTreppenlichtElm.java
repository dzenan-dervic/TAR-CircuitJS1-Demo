/*
    TAR-Dervic EGT: Treppenlichtautomat gekoppelt (MOD-REL-02 / SCHUETZ-11), Dump 471
*/

package com.lushprojects.circuitjs1.client;

/**
 * Vierleiter: Spule A1/A2 (Taster–N) + Schließer L–Lampe.
 * Impuls startet/nachtriggert Ausschaltverzögerung; D nur über Edit.
 * Optik Workbench: Rechteck + Schrägstrich, Fallschirm-Schließer, ohne Gehäuse.
 */
class EGTTreppenlichtElm extends ChipElm implements EGTDesignatable {
    static final int DEF_SIZE_X = 9;
    static final int DEF_SIZE_Y = 7;
    static final int MIN_SIZE_Y = DEF_SIZE_Y;
    static final int N_A1 = 0, N_A2 = 1, N_LAMP = 2, N_L = 3;
    static final double DEF_NOM_POW = 5;
    static final double DEF_NOM_V = 230;
    static final double DEF_DELAY = 3;
    static final double R_ON = 0.05;
    static final double MAX_WALL_DT = 0.25;

    String designation = "Q1";
    String note = "";
    double nom_pow = DEF_NOM_POW;
    double nom_v = DEF_NOM_V;
    double resistance = DEF_NOM_V * DEF_NOM_V / DEF_NOM_POW;
    double delay = DEF_DELAY;
    boolean dauerlicht;
    boolean timedOn;
    boolean coilOn;
    double waitAccum;
    long lastWallMs;
    double contactCurrent;
    double contactCurCount;
    EGTSchuetzCoilHold hold = new EGTSchuetzCoilHold();

    public String egtDesignation() { return designation; }
    public void setEgtDesignation(String d) {
	if (d != null)
	    designation = d;
    }
    public String egtNote() { return note; }
    public void setEgtNote(String s) { note = EGTNote.clamp(s); }

    public EGTTreppenlichtElm(int xx, int yy) {
	super(xx, yy);
	flags &= ~EGTStyle.FLAG_UEBERSICHT;
	setSize(1);
	sizeX = DEF_SIZE_X;
	sizeY = DEF_SIZE_Y;
	updateResistance();
	layoutPins();
	syncEndpoints();
    }

    public EGTTreppenlichtElm(int xa, int ya, int xb, int yb, int f,
			     StringTokenizer st) {
	super(xa, ya, xb, yb, f, st);
	flags &= ~EGTStyle.FLAG_UEBERSICHT;
	setSize(1);
	if (st.hasMoreTokens()) {
	    String t = st.nextToken();
	    try {
		sizeX = Integer.parseInt(t);
		if (st.hasMoreTokens())
		    sizeY = Integer.parseInt(st.nextToken());
		if (st.hasMoreTokens())
		    designation = CustomLogicModel.unescape(st.nextToken());
		if (st.hasMoreTokens())
		    delay = Double.parseDouble(st.nextToken());
		if (st.hasMoreTokens())
		    nom_pow = Double.parseDouble(st.nextToken());
		if (st.hasMoreTokens())
		    nom_v = Double.parseDouble(st.nextToken());
		if (st.hasMoreTokens())
		    dauerlicht = Double.parseDouble(st.nextToken()) != 0;
	    } catch (Exception e) {
		designation = CustomLogicModel.unescape(t);
	    }
	}
	sizeX = DEF_SIZE_X;
	sizeY = DEF_SIZE_Y;
	if (delay < 0)
	    delay = 0;
	if (nom_pow <= 0)
	    nom_pow = DEF_NOM_POW;
	if (nom_v <= 0)
	    nom_v = DEF_NOM_V;
	updateResistance();
	if (dauerlicht)
	    timedOn = true;
	layoutPins();
	syncEndpoints();
    
	note = EGTNote.readOptional(st);
}

    void updateResistance() {
	resistance = nom_v * nom_v / nom_pow;
	if (resistance < 1)
	    resistance = 1;
    }

    boolean isDigitalChip() { return false; }
    boolean nonLinear() { return true; }

    String getChipName() { return "EGT-Treppenlichtautomat"; }

    int getPostCount() { return 4; }
    int getVoltageSourceCount() { return 0; }

    void setupPins() {
	sizeX = DEF_SIZE_X;
	sizeY = DEF_SIZE_Y;
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
	sizeX = DEF_SIZE_X;
	if (sizeY < MIN_SIZE_Y)
	    sizeY = MIN_SIZE_Y;
	int[] col = new int[2];
	EGTStyle.evenPinPos(2, sizeX, 1, col);
	pins[N_A1].pos = col[0];
	pins[N_A1].side0 = SIDE_N;
	pins[N_A1].side = SIDE_N;
	pins[N_A1].output = false;
	pins[N_A2].pos = col[0];
	pins[N_A2].side0 = SIDE_S;
	pins[N_A2].side = SIDE_S;
	pins[N_A2].output = false;
	pins[N_LAMP].pos = col[1];
	pins[N_LAMP].side0 = SIDE_N;
	pins[N_LAMP].side = SIDE_N;
	pins[N_LAMP].output = false;
	pins[N_L].pos = col[1];
	pins[N_L].side0 = SIDE_S;
	pins[N_L].side = SIDE_S;
	pins[N_L].output = false;
    }

    void syncEndpoints() {
	if (cspc2 < 1)
	    setSize(1);
	x2 = x + sizeX * cspc2;
	y2 = y + sizeY * cspc2;
	setPoints();
    }

    void setPoints() {
	super.setPoints();
	if (cspc < 1)
	    return;
	int xr = x + cspc2 - cspc;
	int yr = y - cspc;
	int xs = sizeX * cspc2;
	int ys = sizeY * cspc2;
	setBbox(xr - 8, yr - 28, xr + xs + 100, yr + ys + 24);
    }

    boolean getConnection(int n1, int n2) {
	return EGTSchuetzLink.pinPair(n1, n2, N_A1, N_A2);
    }

    boolean getMatrixConnection(int n1, int n2) {
	if (getConnection(n1, n2))
	    return true;
	return EGTSchuetzLink.pinPair(n1, n2, N_LAMP, N_L);
    }

    void stamp() {
	sim.stampResistor(nodes[N_A1], nodes[N_A2], resistance);
	EGTStyle.stampOpenRef(sim, nodes[N_A1]);
	EGTStyle.stampOpenRef(sim, nodes[N_A2]);
	sim.stampNonLinear(nodes[N_LAMP]);
	sim.stampNonLinear(nodes[N_L]);
    }

    void startIteration() {}
    void execute() {}

    double coilPower() {
	return current * (volts[N_A1] - volts[N_A2]);
    }

    double contactPower() {
	if (!contactClosed())
	    return 0;
	return contactCurrent * (volts[N_LAMP] - volts[N_L]);
    }

    double getPower() { return coilPower(); }

    boolean contactClosed() {
	return dauerlicht || timedOn;
    }

    void setDauerlicht(boolean d) {
	dauerlicht = d;
	waitAccum = 0;
	lastWallMs = System.currentTimeMillis();
	if (d)
	    timedOn = true;
	else
	    timedOn = coilOn;
	if (CircuitElm.app != null)
	    CircuitElm.app.needAnalyze();
    }

    boolean currentVisible(double i) {
	double iNom = nom_v / Math.max(resistance, 1);
	return Math.abs(i) > iNom * 0.05;
    }

    void tickTimer() {
	long now = System.currentTimeMillis();
	boolean running = CircuitElm.app != null
		&& CircuitElm.app.simIsRunning();
	if (dauerlicht) {
	    timedOn = true;
	    waitAccum = 0;
	    lastWallMs = now;
	    return;
	}
	if (coilOn) {
	    timedOn = true;
	    waitAccum = 0;
	    lastWallMs = now;
	    return;
	}
	if (timedOn && running && lastWallMs != 0) {
	    double dt = (now - lastWallMs) / 1000.0;
	    if (dt < 0)
		dt = 0;
	    if (dt > MAX_WALL_DT)
		dt = MAX_WALL_DT;
	    waitAccum += dt;
	    if (waitAccum >= delay)
		timedOn = false;
	}
	lastWallMs = now;
    }

    String timerLabel() {
	if (dauerlicht)
	    return "D Dauerbetrieb";
	if (timedOn && !coilOn && delay > 0)
	    return CircuitElm.getUnitText(waitAccum, "s") + " / "
		    + CircuitElm.getUnitText(delay, "s");
	return timedOn ? "T Nachlauf" : "T Tastbetrieb";
    }

    void reset() {
	super.reset();
	hold.reset();
	coilOn = false;
	waitAccum = 0;
	lastWallMs = 0;
	timedOn = dauerlicht;
	contactCurrent = 0;
    }

    void doStep() {
	// Immer Masse-Bezug an beiden Polen (Freeze/Singular, auch bei D
	// ohne verdrahtete Last). Geschlossen zusätzlich R_ON.
	EGTStyle.stampOpenRef(sim, nodes[N_LAMP]);
	EGTStyle.stampOpenRef(sim, nodes[N_L]);
	if (contactClosed())
	    sim.stampResistor(nodes[N_LAMP], nodes[N_L], R_ON);
    }

    void calculateCurrent() {
	EGTStyle.clearPinCurrents(pins);
	if (resistance <= 0) {
	    current = 0;
	    hold.reset();
	    coilOn = false;
	} else {
	    double vCoil = Math.abs(volts[N_A1] - volts[N_A2]);
	    current = (volts[N_A1] - volts[N_A2]) / resistance;
	    double iNom = nom_v / resistance;
	    if (vCoil < nom_v * 0.15) {
		hold.reset();
		coilOn = false;
	    } else {
		coilOn = hold.update(current, iNom, sim.timeStep);
	    }
	}
	tickTimer();
	if (currentVisible(current))
	    EGTStyle.pinCurrentThru(pins, N_A1, N_A2, current);
	if (!contactClosed()) {
	    contactCurrent = 0;
	    return;
	}
	contactCurrent = (volts[N_LAMP] - volts[N_L]) / R_ON;
	if (currentVisible(contactCurrent))
	    EGTStyle.pinCurrentThru(pins, N_LAMP, N_L, contactCurrent);
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
	try {
	    int[] b = new int[4];
	    boxBounds(b);
	    Color stroke = needsHighlight() ? selectColor : lightGrayColor;
	    drawZh(g, b[0], b[1], b[2], b[3], stroke);
	    drawPosts(g);
	} finally {
	    g.restore();
	}
    }

    void drawZh(Graphics g, int left, int top, int right, int bottom,
		Color stroke) {
	int h = bottom - top;
	int topY = top + Math.max(10, h * 22 / 100);
	int botY = bottom - Math.max(10, h * 22 / 100);
	int cy = (topY + botY) / 2;
	int coilCx = pins[N_A1].post.x;
	int axisX = pins[N_LAMP].post.x;
	int coilW = cspc2 * 2;
	int coilH = cspc2;
	int gap = axisX - coilCx;
	if (gap > 24 && coilW > gap - 16)
	    coilW = gap - 16;
	if (coilW < 16)
	    coilW = 16;
	if (coilH > botY - topY - 8)
	    coilH = Math.max(10, botY - topY - 8);

	g.setColor(stroke);
	g.setLineWidth(2.5);
	g.context.setLineCap("butt");
	g.drawLine(coilCx, pins[N_A1].post.y, coilCx, cy - coilH / 2);
	g.drawLine(coilCx, cy + coilH / 2, coilCx, pins[N_A2].post.y);
	EGTStyle.drawCoilRect(g, coilCx, cy, coilW, coilH, false, stroke);

	int u = Math.max(12, (botY - topY) * 30 / 100);
	int contactCx = axisX - (int) Math.round(0.75 * u);
	boolean closed = contactClosed();
	g.setColor(stroke);
	g.setLineWidth(2.5);
	g.drawLine(axisX, pins[N_LAMP].post.y, axisX, topY);
	g.drawLine(axisX, botY, axisX, pins[N_L].post.y);
	EGTStyle.drawZeitContact(g, contactCx, cy, u, false, closed, true,
				 stroke);

	int linkFrom = coilCx + coilW / 2;
	int linkTo = closed ? axisX : axisX - Math.max(2, cspc / 4);
	EGTStyle.drawMechLink(g, linkFrom, linkTo, cy, stroke);

	g.setColor(whiteColor);
	EGTStyle.drawPinLabelBeside(g, this, pins[N_A1].post.x,
				    pins[N_A1].post.y, "A1", whiteColor);
	EGTStyle.drawPinLabelBeside(g, this, pins[N_A2].post.x,
				    pins[N_A2].post.y, "A2", whiteColor);
	EGTStyle.drawLampPinMark(g, this, pins[N_LAMP].post.x,
				 pins[N_LAMP].post.y, whiteColor);
	EGTStyle.drawPinLabelBeside(g, this, pins[N_L].post.x,
				    pins[N_L].post.y, "L", whiteColor);

	g.setFont(EGTStyle.pinLabelFont());
	int labX = axisX + 14;
	int extra = EGTStyle.drawDesigAndNote(g, this, designation, labX, cy - 10);
	g.drawString(timerLabel(), labX, cy + 8 + extra);
	int tw = (int) g.context.measureText(timerLabel()).getWidth();
	int tw2 = EGTStyle.measureDesigNote(g, this, designation);
	adjustBbox(left, top - 28,
		   Math.max(right, labX + Math.max(tw, tw2) + 8),
		   bottom + 20);
	g.setLineWidth(1.0);

	if (currentVisible(current)) {
	    curcount = updateDotCount(current, curcount);
	    EGTStyle.drawDotsAlong(this, g, curcount, new Point[] {
		    pins[N_A1].post,
		    new Point(coilCx, cy - coilH / 2),
		    new Point(coilCx, cy + coilH / 2),
		    pins[N_A2].post
	    });
	} else {
	    curcount = 0;
	}
	if (closed && currentVisible(contactCurrent)) {
	    contactCurCount = updateDotCount(contactCurrent, contactCurCount);
	    EGTStyle.drawDotsAlong(this, g, contactCurCount, new Point[] {
		    pins[N_LAMP].post,
		    new Point(axisX, topY),
		    new Point(axisX, botY),
		    pins[N_L].post
	    });
	} else {
	    contactCurCount = 0;
	}
    }

    String dump() {
	return super.dump() + " " + sizeX + " " + sizeY + " "
		+ CustomLogicModel.escape(designation) + " " + delay + " "
		+ nom_pow + " " + nom_v + " " + (dauerlicht ? 1 : 0) + EGTNote.dumpSuffix(note);
    }

    void dumpXml(com.google.gwt.xml.client.Document doc,
		 com.google.gwt.xml.client.Element elem) {
	super.dumpXml(doc, elem);
	XMLSerializer.dumpAttr(elem, "sx", sizeX);
	XMLSerializer.dumpAttr(elem, "sy", sizeY);
	XMLSerializer.dumpAttr(elem, "des", designation);
	EGTNote.dumpXml(elem, note);
	XMLSerializer.dumpAttr(elem, "del", delay);
	XMLSerializer.dumpAttr(elem, "np", nom_pow);
	XMLSerializer.dumpAttr(elem, "nv", nom_v);
	XMLSerializer.dumpAttr(elem, "dau", dauerlicht ? 1 : 0);
    }

    void undumpXml(XMLDeserializer xml) {
	super.undumpXml(xml);
	flags &= ~EGTStyle.FLAG_UEBERSICHT;
	designation = xml.parseStringAttr("des", designation);
	note = EGTNote.undumpXml(xml, note);
	delay = xml.parseDoubleAttr("del", delay);
	nom_pow = xml.parseDoubleAttr("np", nom_pow);
	nom_v = xml.parseDoubleAttr("nv", nom_v);
	dauerlicht = xml.parseDoubleAttr("dau", 0) != 0;
	if (delay < 0)
	    delay = 0;
	if (nom_pow <= 0)
	    nom_pow = DEF_NOM_POW;
	if (nom_v <= 0)
	    nom_v = DEF_NOM_V;
	sizeX = DEF_SIZE_X;
	sizeY = DEF_SIZE_Y;
	setSize(1);
	updateResistance();
	if (dauerlicht)
	    timedOn = true;
	layoutPins();
	syncEndpoints();
    }

    int getDumpType() { return 471; }
    String getXmlDumpType() { return "EGTTreppenlicht"; }

    void getInfo(String arr[]) {
	arr[0] = getChipName() + " (" + designation + ")";
	arr[1] = timerLabel();
	arr[2] = "Nachlauf = " + getUnitText(delay, "s");
	arr[3] = "Spule I = " + getCurrentText(current)
		+ ", P = " + getUnitText(coilPower(), "W");
	if (contactClosed())
	    arr[4] = "Kontakt I = " + getCurrentText(contactCurrent)
		    + ", P = " + getUnitText(contactPower(), "W");
	else
	    arr[4] = "Kontakt offen";
	arr[5] = "A1 = " + getVoltageText(volts[N_A1])
		+ ", A2 = " + getVoltageText(volts[N_A2]);
	arr[6] = "L = " + getVoltageText(volts[N_L])
		+ ", Lampe = " + getVoltageText(volts[N_LAMP]);
    }

    public EditInfo getChipEditInfo(int n) {
	if (n == 0)
	    return new EditInfo("Bezeichnung", designation);
	if (n == 1)
	    return EditInfo.createCheckbox("Dauerlicht (D)", dauerlicht);
	if (n == 2)
	    return new EditInfo("Nachlaufzeit (s)", delay, 0, 0);
	if (n == 3)
	    return new EditInfo("Nennleistung Spule (W)", nom_pow, 0, 0)
		    .setPositive();
	if (n == 4)
	    return new EditInfo("Nennspannung Spule (V)", nom_v, 0, 0)
		    .setPositive();
	return null;
    }

    public void setChipEditValue(int n, EditInfo ei) {
	if (n == 0 && ei.textf != null)
	    designation = ei.textf.getText();
	if (n == 1 && ei.checkbox != null)
	    setDauerlicht(ei.checkbox.getState());
	if (n == 2) {
	    delay = ei.value;
	    if (delay < 0)
		delay = 0;
	}
	if (n == 3) {
	    nom_pow = ei.value;
	    updateResistance();
	}
	if (n == 4) {
	    nom_v = ei.value;
	    updateResistance();
	}
    }
}

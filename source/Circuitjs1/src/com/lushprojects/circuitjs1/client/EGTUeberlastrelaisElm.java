/*
    TAR-Dervic EGT: thermisches Überlastrelais (MOD-SCH-03 / ANH-07), Dump 472
*/

package com.lushprojects.circuitjs1.client;

import com.google.gwt.user.client.ui.Button;
import com.lushprojects.circuitjs1.client.util.Locale;

/**
 * Drei Heizleiter 1/3/5–2/4/6 bleiben leitend. Auslösung öffnet 95–96 (NC)
 * und schließt 97–98 (NO). Rückstellen nur über Edit-Button.
 */
class EGTUeberlastrelaisElm extends ChipElm implements EGTUeberlastSource {
    static final int DEF_SIZE_X = 11;
    static final int DEF_SIZE_Y = 5;
    static final int N_1 = 0, N_2 = 1, N_3 = 2, N_4 = 3, N_5 = 4, N_6 = 5;
    static final int N_95 = 6, N_96 = 7, N_97 = 8, N_98 = 9;
    static final String[] LABELS = {
	"1", "2", "3", "4", "5", "6", "95", "96", "97", "98"
    };
    static final double DEF_ISET = 4;
    static final double DEF_TRIP_T = 5;
    static final double R_HEAT = 0.05;
    static final double MAX_WALL_DT = 0.25;

    String designation = "F1";
    String note = "";
    double iSet = DEF_ISET;
    double tripTime = DEF_TRIP_T;
    boolean tripped;
    double thermal;
    long lastWallMs;
    double poleCurrent[] = new double[3];
    double poleCurCount[] = new double[3];
    double auxNcCurrent, auxNoCurrent;
    double auxNcCurCount, auxNoCurCount;

    public String egtDesignation() { return designation; }
    public void setEgtDesignation(String d) {
	if (d != null)
	    designation = d;
    }
    public String egtNote() { return note; }
    public void setEgtNote(String s) { note = EGTNote.clamp(s); }
    public boolean ueberlastTripped() { return tripped; }

    public EGTUeberlastrelaisElm(int xx, int yy) {
	super(xx, yy);
	flags &= ~EGTStyle.FLAG_UEBERSICHT;
	setSize(1);
	sizeX = DEF_SIZE_X;
	sizeY = DEF_SIZE_Y;
	layoutPins();
	syncEndpoints();
    }

    public EGTUeberlastrelaisElm(int xa, int ya, int xb, int yb, int f,
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
		    iSet = Double.parseDouble(st.nextToken());
		if (st.hasMoreTokens())
		    tripTime = Double.parseDouble(st.nextToken());
	    } catch (Exception e) {
		designation = CustomLogicModel.unescape(t);
	    }
	}
	sizeX = DEF_SIZE_X;
	sizeY = DEF_SIZE_Y;
	if (iSet <= 0)
	    iSet = DEF_ISET;
	if (tripTime <= 0)
	    tripTime = DEF_TRIP_T;
	layoutPins();
	syncEndpoints();
    
	note = EGTNote.readOptional(st);
}

    boolean isDigitalChip() { return false; }
    boolean nonLinear() { return true; }

    String getChipName() { return "EGT-Thermisches Überlastrelais"; }

    int getPostCount() { return 10; }
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
	sizeY = DEF_SIZE_Y;
	int[] col = new int[5];
	EGTStyle.evenPinPos(5, sizeX, 1, col);
	int[] topIdx = { N_1, N_3, N_5, N_95, N_97 };
	int[] botIdx = { N_2, N_4, N_6, N_96, N_98 };
	for (int i = 0; i < 5; i++) {
	    pins[topIdx[i]].pos = col[i];
	    pins[topIdx[i]].side0 = SIDE_N;
	    pins[topIdx[i]].side = SIDE_N;
	    pins[topIdx[i]].output = false;
	    pins[botIdx[i]].pos = col[i];
	    pins[botIdx[i]].side0 = SIDE_S;
	    pins[botIdx[i]].side = SIDE_S;
	    pins[botIdx[i]].output = false;
	}
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
	return EGTSchuetzLink.pinPair(n1, n2, N_1, N_2)
		|| EGTSchuetzLink.pinPair(n1, n2, N_3, N_4)
		|| EGTSchuetzLink.pinPair(n1, n2, N_5, N_6);
    }

    boolean getMatrixConnection(int n1, int n2) {
	if (getConnection(n1, n2))
	    return true;
	return EGTSchuetzLink.pinPair(n1, n2, N_95, N_96)
		|| EGTSchuetzLink.pinPair(n1, n2, N_97, N_98);
    }

    void stamp() {
	sim.stampResistor(nodes[N_1], nodes[N_2], R_HEAT);
	sim.stampResistor(nodes[N_3], nodes[N_4], R_HEAT);
	sim.stampResistor(nodes[N_5], nodes[N_6], R_HEAT);
	sim.stampNonLinear(nodes[N_95]);
	sim.stampNonLinear(nodes[N_96]);
	sim.stampNonLinear(nodes[N_97]);
	sim.stampNonLinear(nodes[N_98]);
    }

    void startIteration() {}
    void execute() {}

    void reset() {
	super.reset();
	tripped = false;
	thermal = 0;
	lastWallMs = 0;
	auxNcCurrent = auxNoCurrent = 0;
	poleCurrent[0] = poleCurrent[1] = poleCurrent[2] = 0;
    }

    boolean ncClosed() { return !tripped; }
    boolean noClosed() { return tripped; }

    void clearTrip() {
	tripped = false;
	thermal = 0;
	lastWallMs = System.currentTimeMillis();
    }

    void tickThermal() {
	long now = System.currentTimeMillis();
	boolean running = CircuitElm.app != null
		&& CircuitElm.app.simIsRunning();
	double dt = 0;
	if (running && lastWallMs != 0) {
	    dt = (now - lastWallMs) / 1000.0;
	    if (dt < 0)
		dt = 0;
	    if (dt > MAX_WALL_DT)
		dt = MAX_WALL_DT;
	}
	lastWallMs = now;
	if (!running || dt <= 0 || tripped)
	    return;
	double iMax = Math.abs(poleCurrent[0]);
	if (Math.abs(poleCurrent[1]) > iMax)
	    iMax = Math.abs(poleCurrent[1]);
	if (Math.abs(poleCurrent[2]) > iMax)
	    iMax = Math.abs(poleCurrent[2]);
	double r = iMax / Math.max(iSet, 1e-6);
	if (r >= 1.2) {
	    /* Linear: 1,2×Iset → tripTime. Kein I²t, sonst löst Anlauf in <1 s. */
	    thermal += dt / tripTime;
	    if (thermal >= 1) {
		thermal = 1;
		tripped = true;
	    }
	} else if (r < 1.0) {
	    thermal -= dt / Math.max(tripTime * 4, 1);
	    if (thermal < 0)
		thermal = 0;
	}
    }

    void doStep() {
	EGTStyle.stampOpenRef(sim, nodes[N_95]);
	EGTStyle.stampOpenRef(sim, nodes[N_96]);
	EGTStyle.stampOpenRef(sim, nodes[N_97]);
	EGTStyle.stampOpenRef(sim, nodes[N_98]);
	if (ncClosed())
	    sim.stampResistor(nodes[N_95], nodes[N_96], EGTSchuetzLink.R_ON);
	if (noClosed())
	    sim.stampResistor(nodes[N_97], nodes[N_98], EGTSchuetzLink.R_ON);
    }

    void calculateCurrent() {
	EGTStyle.clearPinCurrents(pins);
	poleCurrent[0] = (volts[N_1] - volts[N_2]) / R_HEAT;
	poleCurrent[1] = (volts[N_3] - volts[N_4]) / R_HEAT;
	poleCurrent[2] = (volts[N_5] - volts[N_6]) / R_HEAT;
	tickThermal();
	EGTStyle.pinCurrentThru(pins, N_1, N_2, poleCurrent[0]);
	EGTStyle.pinCurrentThru(pins, N_3, N_4, poleCurrent[1]);
	EGTStyle.pinCurrentThru(pins, N_5, N_6, poleCurrent[2]);
	if (ncClosed()) {
	    auxNcCurrent = (volts[N_95] - volts[N_96]) / EGTSchuetzLink.R_ON;
	    EGTStyle.pinCurrentThru(pins, N_95, N_96, auxNcCurrent);
	} else
	    auxNcCurrent = 0;
	if (noClosed()) {
	    auxNoCurrent = (volts[N_97] - volts[N_98]) / EGTSchuetzLink.R_ON;
	    EGTStyle.pinCurrentThru(pins, N_97, N_98, auxNoCurrent);
	} else
	    auxNoCurrent = 0;
	current = poleCurrent[0];
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
	g.setColor(stroke);
	drawThickPolygon(g, rectPointsX, rectPointsY, 4);

	int h = bottom - top;
	int topY = top + Math.max(12, h * 20 / 100);
	int botY = bottom - Math.max(12, h * 20 / 100);
	int cy = (topY + botY) / 2;
	int[] heatTop = { N_1, N_3, N_5 };
	int[] heatBot = { N_2, N_4, N_6 };
	for (int i = 0; i < 3; i++) {
	    int px = pins[heatTop[i]].post.x;
	    g.setColor(stroke);
	    g.setLineWidth(2.5);
	    g.drawLine(px, pins[heatTop[i]].post.y, px, topY);
	    g.drawLine(px, botY, px, pins[heatBot[i]].post.y);
	    EGTStyle.drawBimetalHeater(g, px, topY, botY, stroke);
	}

	int ncx = pins[N_95].post.x;
	g.setColor(stroke);
	g.setLineWidth(2.5);
	g.drawLine(ncx, pins[N_95].post.y, ncx, topY);
	g.drawLine(ncx, botY, ncx, pins[N_96].post.y);
	EGTStyle.drawNcContact(g, ncx, topY, botY, ncClosed(), stroke);

	int nox = pins[N_97].post.x;
	g.drawLine(nox, pins[N_97].post.y, nox, topY);
	g.drawLine(nox, botY, nox, pins[N_98].post.y);
	EGTStyle.drawNoContact(g, nox, topY, botY, noClosed(), stroke);
	EGTStyle.drawMechLink(g, pins[N_1].post.x, nox, cy, stroke);

	g.setColor(whiteColor);
	for (int i = 0; i < 10; i++)
	    EGTStyle.drawPinLabelBeside(g, this, pins[i].post.x, pins[i].post.y,
					LABELS[i], whiteColor);
	String heatTxt = tripped ? "ausgelöst" : "bereit";
	int[] ab = EGTStyle.drawHousingAnno(g, this, left, top, right, bottom,
		whiteColor, designation, heatTxt, null);
	adjustBbox(ab[0], ab[1], ab[2], ab[3]);
	g.setLineWidth(1.0);

	for (int i = 0; i < 3; i++) {
	    poleCurCount[i] = updateDotCount(poleCurrent[i], poleCurCount[i]);
	    int px = pins[heatTop[i]].post.x;
	    drawDots(g, pins[heatTop[i]].post, new Point(px, topY),
		     poleCurCount[i]);
	    drawDots(g, new Point(px, botY), pins[heatBot[i]].post,
		     poleCurCount[i]);
	}
	if (ncClosed()) {
	    auxNcCurCount = updateDotCount(auxNcCurrent, auxNcCurCount);
	    drawDots(g, pins[N_95].post, new Point(ncx, topY), auxNcCurCount);
	    drawDots(g, new Point(ncx, botY), pins[N_96].post, auxNcCurCount);
	}
	if (noClosed()) {
	    auxNoCurCount = updateDotCount(auxNoCurrent, auxNoCurCount);
	    drawDots(g, pins[N_97].post, new Point(nox, topY), auxNoCurCount);
	    drawDots(g, new Point(nox, botY), pins[N_98].post, auxNoCurCount);
	}
    }

    String dump() {
	return super.dump() + " " + sizeX + " " + sizeY + " "
		+ CustomLogicModel.escape(designation) + " " + iSet + " "
		+ tripTime + EGTNote.dumpSuffix(note);
    }

    void dumpXml(com.google.gwt.xml.client.Document doc,
		 com.google.gwt.xml.client.Element elem) {
	super.dumpXml(doc, elem);
	XMLSerializer.dumpAttr(elem, "sx", sizeX);
	XMLSerializer.dumpAttr(elem, "sy", sizeY);
	XMLSerializer.dumpAttr(elem, "des", designation);
	EGTNote.dumpXml(elem, note);
	XMLSerializer.dumpAttr(elem, "iset", iSet);
	XMLSerializer.dumpAttr(elem, "tt", tripTime);
    }

    void undumpXml(XMLDeserializer xml) {
	super.undumpXml(xml);
	flags &= ~EGTStyle.FLAG_UEBERSICHT;
	designation = xml.parseStringAttr("des", designation);
	note = EGTNote.undumpXml(xml, note);
	iSet = xml.parseDoubleAttr("iset", iSet);
	tripTime = xml.parseDoubleAttr("tt", tripTime);
	if (iSet <= 0)
	    iSet = DEF_ISET;
	if (tripTime <= 0)
	    tripTime = DEF_TRIP_T;
	sizeX = DEF_SIZE_X;
	sizeY = DEF_SIZE_Y;
	setSize(1);
	layoutPins();
	syncEndpoints();
    }

    int getDumpType() { return 472; }
    String getXmlDumpType() { return "EGTUeberlastrelais"; }

    void getInfo(String arr[]) {
	arr[0] = getChipName() + " (" + designation + ")";
	arr[1] = tripped ? "ausgelöst — Reset im Edit" : "bereit";
	arr[2] = "Iset = " + getCurrentText(iSet)
		+ ", t = " + getUnitText(tripTime, "s");
	arr[3] = "I1 = " + getCurrentText(poleCurrent[0])
		+ ", I2 = " + getCurrentText(poleCurrent[1])
		+ ", I3 = " + getCurrentText(poleCurrent[2]);
	arr[4] = "95-96 " + (ncClosed() ? "zu" : "offen")
		+ ", 97-98 " + (noClosed() ? "zu" : "offen");
	arr[5] = "Wärme " + (int) Math.round(thermal * 100) + " %";
    }

    public EditInfo getChipEditInfo(int n) {
	if (n == 0)
	    return new EditInfo("Bezeichnung", designation);
	if (n == 1)
	    return new EditInfo("Einstellstrom Iset (A)", iSet, 0, 0)
		    .setPositive();
	if (n == 2)
	    return new EditInfo("Auslösezeit bei 1,2×Iset (s)", tripTime, 0, 0)
		    .setPositive();
	if (n == 3 && tripped) {
	    EditInfo ei = new EditInfo("", 0, -1, -1);
	    ei.button = new Button(Locale.LS("Auslösung zurücksetzen"));
	    return ei;
	}
	int k = tripped ? n : n + 1;
	if (k == 4) {
	    EditInfo ei = new EditInfo("", 0, -1, -1);
	    ei.button = new Button(Locale.LS("Öffner hinzufügen"));
	    return ei;
	}
	if (k == 5) {
	    EditInfo ei = new EditInfo("", 0, -1, -1);
	    ei.button = new Button(Locale.LS("Schließer hinzufügen"));
	    return ei;
	}
	return null;
    }

    public void setChipEditValue(int n, EditInfo ei) {
	if (n == 0 && ei.textf != null)
	    designation = ei.textf.getText();
	if (n == 1) {
	    iSet = ei.value;
	    if (iSet <= 0)
		iSet = DEF_ISET;
	}
	if (n == 2) {
	    tripTime = ei.value;
	    if (tripTime <= 0)
		tripTime = DEF_TRIP_T;
	}
	if (n == 3 && ei.button != null && tripped) {
	    clearTrip();
	    ei.newDialog = true;
	    return;
	}
	int k = tripped ? n : n + 1;
	if (k == 4 && ei.button != null)
	    EGTUeberlastLink.placeKontakt(this, designation, true);
	if (k == 5 && ei.button != null)
	    EGTUeberlastLink.placeKontakt(this, designation, false);
    }
}

/*
    TAR-Dervic EGT: Sicherung / LS (MOD-SCH-04), Dump 474
*/

package com.lushprojects.circuitjs1.client;

import com.google.gwt.user.client.ui.Button;
import com.lushprojects.circuitjs1.client.util.Locale;

/**
 * Schaltmesser wie Workbench. Elektrisch Leitungsschutzschalter nach
 * IEC 60898-1 (didaktisch gekürzt): thermisch 1,13/1,45×In, magnetisch
 * B/C/D. Auslösung öffnet alle Pole gemeinsam. Reset nur Edit-Button.
 */
class EGTSicherungElm extends ChipElm implements EGTDesignatable {
    static final int SIZE_X_1P = 4;
    /** 7: evenPinPos 1/3/5 — bei 8 wären die Spalten 1/3/6 (L3 weiter). */
    static final int SIZE_X_3P = 7;
    static final int DEF_SIZE_Y = 5;
    static final int[] IN_A = {
	6, 10, 13, 16, 20, 25, 32, 40, 50, 63
    };
    static final int DEF_IN = 16;
    static final int CHAR_B = 0, CHAR_C = 1, CHAR_D = 2;
    /** Magnetisch: obere „muss auslösen“-Grenze IEC 60898-1. */
    static final int[] MAG_MULT = { 5, 10, 20 };
    static final double I1 = 1.13;
    static final double I2 = 1.45;
    static final double THERMAL_T = 3;
    static final double ENV_TAU = 0.05;
    /** Magnetisch: mehrere Frames, nicht ein Solver-Spike / ein Hitch. */
    static final double MAG_HOLD = 0.08;
    static final double MAG_DT_MAX = 0.02;
    static final double MAX_WALL_DT = 0.25;
    /**
     * Geschlossener Pol. Nicht {@code COMPOSITE_CLOSED_R} (1 mΩ):
     * Solver-ΔU ≈ u_L(t) gegen 0 V → ΔU/1 mΩ = 100 kA, Reset unmöglich.
     */
    static final double R_ON = 1;
    /** Nach Reset/Stamp: Schutz aus, bis die neue Matrix gilt. */
    static final int SETTLE_MS = 200;
    static final int TRIP_NONE = 0, TRIP_TH = 1, TRIP_MAG = 2;
    /** Wie Schütz/LS: oben 1/3/5, unten 2/4/6. */
    static final String[] TERM_TOP = { "1", "3", "5" };
    static final String[] TERM_BOT = { "2", "4", "6" };

    String designation = "F1";
    String note = "";
    int poles = 1;
    int inA = DEF_IN;
    int charIdx = CHAR_B;
    boolean tripped;
    int tripReason = TRIP_NONE;
    double thermal;
    double iEnv;
    double magAccum;
    boolean holdUntilStamp = true;
    long stampOkAt;
    long lastWallMs;
    /** Ieff über zwei 50-Hz-Perioden (Simulationszeit), nicht Momentanwert. */
    static final double RMS_WINDOW = 0.04;
    double rmsSum;
    double rmsTime;
    double iShow;
    double poleCurrent[] = new double[3];
    double poleCurCount[] = new double[3];

    public String egtDesignation() { return designation; }
    public void setEgtDesignation(String d) {
	if (d != null)
	    designation = d;
    }
    public String egtNote() { return note; }
    public void setEgtNote(String s) { note = EGTNote.clamp(s); }

    public EGTSicherungElm(int xx, int yy) {
	super(xx, yy);
	flags &= ~EGTStyle.FLAG_UEBERSICHT;
	setSize(1);
	poles = 1;
	inA = DEF_IN;
	charIdx = CHAR_B;
	applySize();
	layoutPins();
	syncEndpoints();
    }

    public EGTSicherungElm(int xa, int ya, int xb, int yb, int f,
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
		    poles = Integer.parseInt(st.nextToken());
		if (st.hasMoreTokens())
		    inA = (int) Math.round(Double.parseDouble(st.nextToken()));
		if (st.hasMoreTokens())
		    charIdx = Integer.parseInt(st.nextToken());
	    } catch (Exception e) {
		designation = CustomLogicModel.unescape(t);
	    }
	}
	clampPoles();
	inA = nearestIn(inA);
	clampChar();
	applySize();
	layoutPins();
	allocNodes();
	syncEndpoints();
    
	note = EGTNote.readOptional(st);
}

    boolean isDigitalChip() { return false; }
    boolean nonLinear() { return false; }
    boolean isWireEquivalent() { return false; }
    boolean isRemovableWire() { return false; }

    String getChipName() { return "EGT-Sicherung"; }

    int getPostCount() { return poles * 2; }
    int getVoltageSourceCount() { return 0; }

    void clampPoles() {
	if (poles != 3)
	    poles = 1;
    }

    void clampChar() {
	if (charIdx < CHAR_B || charIdx > CHAR_D)
	    charIdx = CHAR_B;
    }

    static int nearestIn(int v) {
	int best = IN_A[0];
	int dBest = Math.abs(v - best);
	for (int i = 1; i < IN_A.length; i++) {
	    int d = Math.abs(v - IN_A[i]);
	    if (d < dBest) {
		dBest = d;
		best = IN_A[i];
	    }
	}
	return best;
    }

    int inIndex() {
	for (int i = 0; i < IN_A.length; i++)
	    if (IN_A[i] == inA)
		return i;
	return 3;
    }

    int magMult() {
	clampChar();
	return MAG_MULT[charIdx];
    }

    String charLetter() {
	if (charIdx == CHAR_C)
	    return "C";
	if (charIdx == CHAR_D)
	    return "D";
	return "B";
    }

    String ratingText() {
	return charLetter() + inA;
    }

    String meterText() {
	return "Ieff " + getCurrentText(iShow);
    }

    void applySize() {
	sizeX = poles == 3 ? SIZE_X_3P : SIZE_X_1P;
	sizeY = DEF_SIZE_Y;
    }

    void setupPins() {
	applySize();
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
	else {
	    applySize();
	    placePins();
	}
    }

    void placePins() {
	applySize();
	int[] col = new int[poles];
	EGTStyle.evenPinPos(poles, sizeX, 1, col);
	for (int i = 0; i < poles; i++) {
	    int top = 2 * i;
	    int bot = top + 1;
	    pins[top].pos = col[i];
	    pins[top].side0 = SIDE_N;
	    pins[top].side = SIDE_N;
	    pins[top].output = false;
	    pins[top].text = TERM_TOP[i];
	    pins[bot].pos = col[i];
	    pins[bot].side0 = SIDE_S;
	    pins[bot].side = SIDE_S;
	    pins[bot].output = false;
	    pins[bot].text = TERM_BOT[i];
	}
    }

    void syncEndpoints() {
	if (cspc2 < 1)
	    setSize(1);
	x2 = x + sizeX * cspc2;
	y2 = y + sizeY * cspc2;
	setPoints();
    }

    void applyPoles() {
	clampPoles();
	setupPins();
	allocNodes();
	syncEndpoints();
	if (CircuitElm.app != null)
	    CircuitElm.app.needAnalyze();
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
	if (tripped)
	    return false;
	for (int i = 0; i < poles; i++) {
	    if (EGTSchuetzLink.pinPair(n1, n2, 2 * i, 2 * i + 1))
		return true;
	}
	return false;
    }

    boolean getMatrixConnection(int n1, int n2) {
	return getConnection(n1, n2);
    }

    void stamp() {
	holdUntilStamp = false;
	stampOkAt = System.currentTimeMillis();
	if (tripped)
	    return;
	for (int i = 0; i < poles; i++)
	    sim.stampResistor(nodes[2 * i], nodes[2 * i + 1], R_ON);
    }

    void startIteration() {}
    void execute() {}
    void doStep() {}

    void reset() {
	super.reset();
	clearTrip();
	poleCurrent[0] = poleCurrent[1] = poleCurrent[2] = 0;
	iEnv = 0;
	clearMeter();
    }

    boolean contactClosed() { return !tripped; }

    void clearTrip() {
	tripped = false;
	tripReason = TRIP_NONE;
	thermal = 0;
	iEnv = 0;
	magAccum = 0;
	clearMeter();
	holdUntilStamp = true;
	lastWallMs = System.currentTimeMillis();
	requestAnalyze();
    }

    void requestAnalyze() {
	if (CircuitElm.app != null)
	    CircuitElm.app.needAnalyze();
    }

    void trip(int reason) {
	tripped = true;
	tripReason = reason;
	thermal = 1;
	requestAnalyze();
    }

    /** Anderes Element am Post — offenes Ende zählt nicht als Stromkreis. */
    boolean pinWired(int post) {
	if (nodes == null || post < 0 || post >= nodes.length)
	    return false;
	CircuitNode cn = nodes[post];
	if (cn == null || cn.links == null)
	    return false;
	for (int i = 0; i < cn.links.size(); i++) {
	    CircuitNodeLink ln = cn.links.get(i);
	    if (ln != null && ln.elm != null && ln.elm != this)
		return true;
	}
	return false;
    }

    boolean poleHasCircuit(int pole) {
	return pinWired(2 * pole) && pinWired(2 * pole + 1);
    }

    void clearMeter() {
	rmsSum = 0;
	rmsTime = 0;
	iShow = 0;
    }

    double poleAbsMax() {
	double i = 0;
	for (int k = 0; k < poles; k++) {
	    double ip = Math.abs(poleCurrent[k]);
	    if (ip > i)
		i = ip;
	}
	return i;
    }

    void stepFinished() {
	if (tripped || holdUntilStamp) {
	    clearMeter();
	    return;
	}
	double dt = sim.timeStep;
	if (dt <= 0)
	    return;
	double i = poleAbsMax();
	rmsSum += i * i * dt;
	rmsTime += dt;
	if (rmsTime >= RMS_WINDOW) {
	    iShow = Math.sqrt(rmsSum / rmsTime);
	    rmsSum = 0;
	    rmsTime = 0;
	}
    }

    void tickProtection() {
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
	if (holdUntilStamp || tripped)
	    return;
	if (now - stampOkAt < SETTLE_MS)
	    return;
	if (!running || dt <= 0)
	    return;
	double iMax = Math.abs(poleCurrent[0]);
	for (int i = 1; i < poles; i++) {
	    double ip = Math.abs(poleCurrent[i]);
	    if (ip > iMax)
		iMax = ip;
	}
	double envA = 1 - Math.exp(-dt / ENV_TAU);
	iEnv += (iMax - iEnv) * envA;
	double in = Math.max(inA, 1e-6);
	double rTh = iEnv / in;
	double rMag = iMax / in;
	if (rMag >= magMult()) {
	    double dMag = dt;
	    if (dMag > MAG_DT_MAX)
		dMag = MAG_DT_MAX;
	    magAccum += dMag;
	    if (magAccum >= MAG_HOLD) {
		trip(TRIP_MAG);
		return;
	    }
	} else
	    magAccum = 0;
	if (rTh >= I2) {
	    thermal += dt / THERMAL_T;
	    if (thermal >= 1) {
		trip(TRIP_TH);
		return;
	    }
	} else if (rTh >= I1) {
	    thermal += dt / (THERMAL_T * 8);
	    if (thermal >= 1) {
		trip(TRIP_TH);
		return;
	    }
	} else if (rTh < 1.0) {
	    thermal -= dt / Math.max(THERMAL_T * 4, 1);
	    if (thermal < 0)
		thermal = 0;
	}
    }

    void calculateCurrent() {
	EGTStyle.clearPinCurrents(pins);
	for (int i = 0; i < poles; i++) {
	    int a = 2 * i;
	    int b = a + 1;
	    if (holdUntilStamp || !contactClosed() || !poleHasCircuit(i))
		poleCurrent[i] = 0;
	    else {
		poleCurrent[i] = (volts[a] - volts[b]) / R_ON;
		EGTStyle.pinCurrentThru(pins, a, b, poleCurrent[i]);
	    }
	}
	tickProtection();
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
	int h = bottom - top;
	int topY = top + Math.max(12, h * 20 / 100);
	int botY = bottom - Math.max(12, h * 20 / 100);
	boolean closed = contactClosed();
	for (int i = 0; i < poles; i++) {
	    int a = 2 * i;
	    int b = a + 1;
	    int px = pins[a].post.x;
	    g.setColor(stroke);
	    g.setLineWidth(2.5);
	    g.context.setLineCap("butt");
	    g.drawLine(px, pins[a].post.y, px, topY);
	    g.drawLine(px, botY, px, pins[b].post.y);
	    EGTStyle.drawSicherungPole(g, px, topY, botY, closed, stroke);
	    EGTStyle.drawPinLabelBeside(g, this, pins[a].post.x,
					pins[a].post.y, TERM_TOP[i],
					whiteColor);
	    EGTStyle.drawPinLabelBeside(g, this, pins[b].post.x,
					pins[b].post.y, TERM_BOT[i],
					whiteColor);
	}
	String st = tripped ? "ausgelöst" : "durch";
	int[] ab = EGTStyle.drawHousingAnno(g, this, left, top, right, bottom,
		whiteColor, designation, ratingText(), st,
		meterText());
	adjustBbox(ab[0], ab[1], ab[2], ab[3]);
	g.setLineWidth(1.0);
	if (!closed)
	    return;
	for (int i = 0; i < poles; i++) {
	    int a = 2 * i;
	    int b = a + 1;
	    int px = pins[a].post.x;
	    poleCurCount[i] = updateDotCount(poleCurrent[i], poleCurCount[i]);
	    drawDots(g, pins[a].post, new Point(px, topY), poleCurCount[i]);
	    drawDots(g, new Point(px, botY), pins[b].post, poleCurCount[i]);
	}
    }

    String dump() {
	return super.dump() + " " + sizeX + " " + sizeY + " "
		+ CustomLogicModel.escape(designation) + " " + poles + " "
		+ inA + " " + charIdx + EGTNote.dumpSuffix(note);
    }

    void dumpXml(com.google.gwt.xml.client.Document doc,
		 com.google.gwt.xml.client.Element elem) {
	super.dumpXml(doc, elem);
	XMLSerializer.dumpAttr(elem, "sx", sizeX);
	XMLSerializer.dumpAttr(elem, "sy", sizeY);
	XMLSerializer.dumpAttr(elem, "des", designation);
	EGTNote.dumpXml(elem, note);
	XMLSerializer.dumpAttr(elem, "poles", poles);
	XMLSerializer.dumpAttr(elem, "in", inA);
	XMLSerializer.dumpAttr(elem, "ch", charIdx);
    }

    void undumpXml(XMLDeserializer xml) {
	super.undumpXml(xml);
	flags &= ~EGTStyle.FLAG_UEBERSICHT;
	designation = xml.parseStringAttr("des", designation);
	note = EGTNote.undumpXml(xml, note);
	poles = xml.parseIntAttr("poles", poles);
	inA = (int) Math.round(xml.parseDoubleAttr("in", inA));
	charIdx = xml.parseIntAttr("ch", charIdx);
	clampPoles();
	inA = nearestIn(inA);
	clampChar();
	setSize(1);
	applySize();
	layoutPins();
	allocNodes();
	syncEndpoints();
    }

    int getDumpType() { return 474; }
    String getXmlDumpType() { return "EGTSicherung"; }

    void getInfo(String arr[]) {
	arr[0] = getChipName() + " (" + designation + ")";
	arr[1] = poles + "-polig, " + ratingText();
	if (tripped)
	    arr[2] = tripReason == TRIP_MAG
		    ? "ausgelöst magnetisch — Reset im Edit"
		    : "ausgelöst thermisch — Reset im Edit";
	else
	    arr[2] = "durch";
	arr[3] = "I> = " + magMult() + "×In";
	arr[4] = "Ieff = " + getCurrentText(iShow);
	arr[5] = "Wärme " + (int) Math.round(thermal * 100) + " %";
    }

    public EditInfo getChipEditInfo(int n) {
	if (n == 0)
	    return new EditInfo("Bezeichnung", designation);
	if (n == 1) {
	    EditInfo ei = new EditInfo("Polzahl", 0);
	    ei.choice = new Choice();
	    ei.choice.add(Locale.LS("1-polig"));
	    ei.choice.add(Locale.LS("3-polig"));
	    ei.choice.select(poles == 3 ? 1 : 0);
	    return ei;
	}
	if (n == 2) {
	    EditInfo ei = new EditInfo("Bemessungsstrom In", 0);
	    ei.choice = new Choice();
	    for (int i = 0; i < IN_A.length; i++)
		ei.choice.add(IN_A[i] + " A");
	    ei.choice.select(inIndex());
	    return ei;
	}
	if (n == 3) {
	    EditInfo ei = new EditInfo("Charakteristik", 0);
	    ei.choice = new Choice();
	    ei.choice.add(Locale.LS("B (3–5×In, Wohnen)"));
	    ei.choice.add(Locale.LS("C (5–10×In, gemischt)"));
	    ei.choice.add(Locale.LS("D (10–20×In, Motor)"));
	    ei.choice.select(charIdx);
	    return ei;
	}
	if (n == 4) {
	    EditInfo ei = new EditInfo("", 0, -1, -1);
	    ei.button = new Button(Locale.LS("Auslösung zurücksetzen"));
	    return ei;
	}
	return null;
    }

    public void setChipEditValue(int n, EditInfo ei) {
	if (n == 0 && ei.textf != null)
	    designation = ei.textf.getText();
	if (n == 1 && ei.choice != null) {
	    int next = ei.choice.getSelectedIndex() == 1 ? 3 : 1;
	    if (next != poles) {
		poles = next;
		applyPoles();
		ei.newDialog = true;
	    }
	}
	if (n == 2 && ei.choice != null) {
	    int idx = ei.choice.getSelectedIndex();
	    if (idx >= 0 && idx < IN_A.length)
		inA = IN_A[idx];
	}
	if (n == 3 && ei.choice != null) {
	    charIdx = ei.choice.getSelectedIndex();
	    clampChar();
	}
	if (n == 4 && ei.button != null) {
	    clearTrip();
	    ei.newDialog = true;
	}
    }
}

/*
    TAR-Dervic EGT: IR-Bewegungsmelder (MOD-INST-10 / INST-09), Dump 473
*/

package com.lushprojects.circuitjs1.client;

/**
 * Drei Leiter: L, N, geschaltetes L′. Elektronik L–N. Schließer L–L′ bei
 * Bewegung/Nachlauf. Klick = Bewegung (nachtriggern). Timer nicht in doStep.
 */
class EGTBewegungsmelderElm extends ChipElm implements EGTDesignatable {
    static final int DEF_SIZE_X = 7;
    static final int DEF_SIZE_Y = 6;
    static final int N_L = 0, N_N = 1, N_SW = 2;
    static final double DEF_NOM_POW = 2;
    static final double DEF_NOM_V = 230;
    static final double DEF_DELAY = 3;
    static final double R_ON = 0.05;
    static final double MAX_WALL_DT = 0.25;
    static final double V_SUPPLY_MIN = 50;

    String designation = "B1";
    String note = "";
    double nom_pow = DEF_NOM_POW;
    double nom_v = DEF_NOM_V;
    double resistance = DEF_NOM_V * DEF_NOM_V / DEF_NOM_POW;
    double delay = DEF_DELAY;
    boolean timedOn;
    boolean powered;
    double waitAccum;
    long lastWallMs;
    double supplyCurrent;
    double contactCurrent;
    double supplyCurCount, contactCurCount;

    public String egtDesignation() { return designation; }
    public void setEgtDesignation(String d) {
	if (d != null)
	    designation = d;
    }
    public String egtNote() { return note; }
    public void setEgtNote(String s) { note = EGTNote.clamp(s); }

    public EGTBewegungsmelderElm(int xx, int yy) {
	super(xx, yy);
	flags &= ~EGTStyle.FLAG_UEBERSICHT;
	setSize(1);
	sizeX = DEF_SIZE_X;
	sizeY = DEF_SIZE_Y;
	updateResistance();
	layoutPins();
	syncEndpoints();
    }

    public EGTBewegungsmelderElm(int xa, int ya, int xb, int yb, int f,
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

    String getChipName() { return "EGT-Bewegungsmelder"; }

    int getPostCount() { return 3; }
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
	int[] col = new int[2];
	EGTStyle.evenPinPos(2, sizeX, 1, col);
	pins[N_L].pos = col[0];
	pins[N_L].side0 = SIDE_N;
	pins[N_L].side = SIDE_N;
	pins[N_L].output = false;
	pins[N_N].pos = col[0];
	pins[N_N].side0 = SIDE_S;
	pins[N_N].side = SIDE_S;
	pins[N_N].output = false;
	pins[N_SW].pos = col[1];
	pins[N_SW].side0 = SIDE_N;
	pins[N_SW].side = SIDE_N;
	pins[N_SW].output = false;
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
	return EGTSchuetzLink.pinPair(n1, n2, N_L, N_N);
    }

    boolean getMatrixConnection(int n1, int n2) {
	if (getConnection(n1, n2))
	    return true;
	return EGTSchuetzLink.pinPair(n1, n2, N_L, N_SW);
    }

    void stamp() {
	sim.stampResistor(nodes[N_L], nodes[N_N], resistance);
	EGTStyle.stampOpenRef(sim, nodes[N_L]);
	EGTStyle.stampOpenRef(sim, nodes[N_N]);
	sim.stampNonLinear(nodes[N_L]);
	sim.stampNonLinear(nodes[N_SW]);
    }

    void startIteration() {}
    void execute() {}

    boolean contactClosed() {
	return powered && timedOn;
    }

    boolean triggerAt(int mx, int my) {
	timedOn = true;
	waitAccum = 0;
	lastWallMs = System.currentTimeMillis();
	return true;
    }

    void tickTimer() {
	long now = System.currentTimeMillis();
	boolean running = CircuitElm.app != null
		&& CircuitElm.app.simIsRunning();
	if (!timedOn) {
	    waitAccum = 0;
	    lastWallMs = now;
	    return;
	}
	if (running && lastWallMs != 0) {
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

    void reset() {
	super.reset();
	timedOn = false;
	powered = false;
	waitAccum = 0;
	lastWallMs = 0;
	supplyCurrent = contactCurrent = 0;
    }

    void doStep() {
	EGTStyle.stampOpenRef(sim, nodes[N_L]);
	EGTStyle.stampOpenRef(sim, nodes[N_SW]);
	if (contactClosed())
	    sim.stampResistor(nodes[N_L], nodes[N_SW], R_ON);
    }

    void calculateCurrent() {
	EGTStyle.clearPinCurrents(pins);
	supplyCurrent = (volts[N_L] - volts[N_N]) / resistance;
	powered = Math.abs(volts[N_L] - volts[N_N]) > V_SUPPLY_MIN;
	tickTimer();
	if (contactClosed())
	    contactCurrent = (volts[N_L] - volts[N_SW]) / R_ON;
	else
	    contactCurrent = 0;
	if (pins[N_L] != null)
	    pins[N_L].current = -(supplyCurrent + contactCurrent);
	if (pins[N_N] != null)
	    pins[N_N].current = supplyCurrent;
	if (pins[N_SW] != null)
	    pins[N_SW].current = contactCurrent;
	current = supplyCurrent;
    }

    String timerLabel() {
	if (!powered)
	    return "ohne Versorgung";
	if (timedOn && delay > 0)
	    return CircuitElm.getUnitText(waitAccum, "s") + " / "
		    + CircuitElm.getUnitText(delay, "s");
	return timedOn ? "Nachlauf" : "Ruhe";
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
	    drawUs(g, b[0], b[1], b[2], b[3], stroke);
	    drawPosts(g);
	} finally {
	    g.restore();
	}
    }

    void drawUs(Graphics g, int left, int top, int right, int bottom,
		Color stroke) {
	int lx = pins[N_L].post.x;
	int nx = pins[N_N].post.x;
	int sx = pins[N_SW].post.x;
	int cy = (top + bottom) / 2;
	int hr = Math.max(10, Math.min(right - left, bottom - top) * 22 / 100);
	int dx = (lx + sx) / 2;
	int dy = cy;

	g.setColor(stroke);
	g.setLineWidth(2.5);
	g.context.setLineCap("butt");
	g.drawLine(lx, pins[N_L].post.y, lx, dy);
	g.drawLine(nx, dy, nx, pins[N_N].post.y);
	g.drawLine(lx, dy, dx - hr, dy);
	g.drawLine(dx + hr, dy, sx, dy);
	g.drawLine(sx, dy, sx, pins[N_SW].post.y);

	g.drawLine(dx, dy - hr, dx + hr, dy);
	g.drawLine(dx + hr, dy, dx, dy + hr);
	g.drawLine(dx, dy + hr, dx - hr, dy);
	g.drawLine(dx - hr, dy, dx, dy - hr);
	g.drawLine(dx, dy - hr, dx, dy + hr);

	int topY = dy - Math.max(8, hr / 3);
	int botY = dy + Math.max(10, hr / 2);
	boolean closed = contactClosed();
	EGTStyle.drawNoContact(g, sx, topY, botY, closed, stroke);

	g.setColor(whiteColor);
	EGTStyle.drawPinLabelBeside(g, this, pins[N_L].post.x,
				    pins[N_L].post.y, "L", whiteColor);
	EGTStyle.drawPinLabelBeside(g, this, pins[N_N].post.x,
				    pins[N_N].post.y, "N", whiteColor);
	EGTStyle.drawPinLabelBeside(g, this, pins[N_SW].post.x,
				    pins[N_SW].post.y, "L'", whiteColor);
	g.setFont(EGTStyle.pinLabelFont());
	g.context.setTextAlign("center");
	g.drawString("PIR", dx, dy - hr - 6);
	g.context.setTextAlign("left");
	int[] ab = EGTStyle.drawHousingAnno(g, this, left, top, right, bottom,
		whiteColor, designation, timerLabel(), null);
	adjustBbox(ab[0], ab[1], ab[2], ab[3]);
	g.setLineWidth(1.0);

	supplyCurCount = updateDotCount(supplyCurrent, supplyCurCount);
	drawDots(g, pins[N_L].post, new Point(lx, dy), supplyCurCount);
	drawDots(g, new Point(nx, dy), pins[N_N].post, supplyCurCount);
	if (closed) {
	    contactCurCount = updateDotCount(contactCurrent, contactCurCount);
	    drawDots(g, new Point(dx + hr, dy), pins[N_SW].post,
		     contactCurCount);
	}
    }

    String dump() {
	return super.dump() + " " + sizeX + " " + sizeY + " "
		+ CustomLogicModel.escape(designation) + " " + delay + " "
		+ nom_pow + " " + nom_v + EGTNote.dumpSuffix(note);
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
    }

    void undumpXml(XMLDeserializer xml) {
	super.undumpXml(xml);
	flags &= ~EGTStyle.FLAG_UEBERSICHT;
	designation = xml.parseStringAttr("des", designation);
	note = EGTNote.undumpXml(xml, note);
	delay = xml.parseDoubleAttr("del", delay);
	nom_pow = xml.parseDoubleAttr("np", nom_pow);
	nom_v = xml.parseDoubleAttr("nv", nom_v);
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
	layoutPins();
	syncEndpoints();
    }

    int getDumpType() { return 473; }
    String getXmlDumpType() { return "EGTBewegungsmelder"; }

    void getInfo(String arr[]) {
	arr[0] = getChipName() + " (" + designation + ")";
	arr[1] = timerLabel();
	arr[2] = "Nachlauf = " + getUnitText(delay, "s");
	arr[3] = "L-N I = " + getCurrentText(supplyCurrent);
	arr[4] = contactClosed()
		? "L' I = " + getCurrentText(contactCurrent)
		: "L' offen";
	arr[5] = "L = " + getVoltageText(volts[N_L])
		+ ", N = " + getVoltageText(volts[N_N]);
    }

    public EditInfo getChipEditInfo(int n) {
	if (n == 0)
	    return new EditInfo("Bezeichnung", designation);
	if (n == 1)
	    return new EditInfo("Nachlaufzeit (s)", delay, 0, 0);
	if (n == 2)
	    return new EditInfo("Eigenverbrauch (W)", nom_pow, 0, 0)
		    .setPositive();
	if (n == 3)
	    return new EditInfo("Nennspannung (V)", nom_v, 0, 0)
		    .setPositive();
	return null;
    }

    public void setChipEditValue(int n, EditInfo ei) {
	if (n == 0 && ei.textf != null)
	    designation = ei.textf.getText();
	if (n == 1) {
	    delay = ei.value;
	    if (delay < 0)
		delay = 0;
	}
	if (n == 2) {
	    nom_pow = ei.value;
	    updateResistance();
	}
	if (n == 3) {
	    nom_v = ei.value;
	    updateResistance();
	}
    }
}

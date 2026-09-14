/*
    TAR-Dervic EGT: Stromstoßschalter gekoppelt / zusammen (S305 / MOD-REL-01), Dump 460
*/

package com.lushprojects.circuitjs1.client;

/**
 * Zusammenhängend: Spule A1/A2 (Stufenmarkierung) + n Schließer + m Öffner.
 * Impuls auf Spule toggelt Rastzustand; Kontakte folgen dem Rast.
 */
class EGTStromstossschalterElm extends ChipElm implements EGTStromstossSource, EGTDesignatable {
    static final int MAX_CONTACTS = 4;
    static final int DEF_N_NO = 1;
    static final int DEF_N_NC = 0;
    static final int DEF_SIZE_Y = 7;
    static final int MIN_SIZE_Y = DEF_SIZE_Y;
    static final int N_A1 = 0, N_A2 = 1;

    String designation = "E1";
    String note = "";

    public String egtDesignation() { return designation; }
    public void setEgtDesignation(String d) {
	if (d != null)
	    designation = d;
    }
    public String egtNote() { return note; }
    public void setEgtNote(String s) { note = EGTNote.clamp(s); }
    static final double DEF_NOM_POW = 5;
    static final double DEF_NOM_V = 230;
    int nNo = DEF_N_NO;
    int nNc = DEF_N_NC;
    double nom_pow = DEF_NOM_POW;
    double nom_v = DEF_NOM_V;
    double resistance = DEF_NOM_V * DEF_NOM_V / DEF_NOM_POW;
    boolean coilOn;
    boolean wasCoilOn;
    /** true erst nach Spule-AUS — nächster Rising Edge toggelt (Impuls, nicht Dauer). */
    boolean coilReleased;
    EGTSchuetzCoilHold hold = new EGTSchuetzCoilHold();
    double contactCurrent[] = new double[MAX_CONTACTS * 2];
    double contactCurCount[] = new double[MAX_CONTACTS * 2];

    public EGTStromstossschalterElm(int xx, int yy) {
	super(xx, yy);
	flags &= ~EGTStyle.FLAG_UEBERSICHT;
	setSize(1);
	clampCounts();
	updateSizeX();
	sizeY = DEF_SIZE_Y;
	updateResistance();
	layoutPins();
	syncEndpoints();
    }

    public EGTStromstossschalterElm(int xa, int ya, int xb, int yb, int f,
				    StringTokenizer st) {
	super(xa, ya, xb, yb, f, st);
	flags &= ~EGTStyle.FLAG_UEBERSICHT;
	setSize(1);
	boolean dumpLatched = false;
	if (st.hasMoreTokens()) {
	    String t = st.nextToken();
	    try {
		sizeX = Integer.parseInt(t);
		if (st.hasMoreTokens())
		    sizeY = Integer.parseInt(st.nextToken());
		if (st.hasMoreTokens())
		    designation = CustomLogicModel.unescape(st.nextToken());
		if (st.hasMoreTokens())
		    nNo = (int) Double.parseDouble(st.nextToken());
		if (st.hasMoreTokens())
		    nNc = (int) Double.parseDouble(st.nextToken());
		if (st.hasMoreTokens())
		    nom_pow = Double.parseDouble(st.nextToken());
		if (st.hasMoreTokens())
		    nom_v = Double.parseDouble(st.nextToken());
		if (st.hasMoreTokens())
		    dumpLatched = Double.parseDouble(st.nextToken()) != 0;
	    } catch (Exception e) {
		designation = CustomLogicModel.unescape(t);
	    }
	}
	clampCounts();
	updateSizeX();
	sizeY = DEF_SIZE_Y;
	if (nom_pow <= 0)
	    nom_pow = DEF_NOM_POW;
	if (nom_v <= 0)
	    nom_v = DEF_NOM_V;
	updateResistance();
	EGTStromstossLink.setLatched(designation, dumpLatched);
	layoutPins();
	syncEndpoints();
    
	note = EGTNote.readOptional(st);
}

    void clampCounts() {
	if (nNo < 0)
	    nNo = 0;
	if (nNo > MAX_CONTACTS)
	    nNo = MAX_CONTACTS;
	if (nNc < 0)
	    nNc = 0;
	if (nNc > MAX_CONTACTS)
	    nNc = MAX_CONTACTS;
	if (nNo + nNc < 1)
	    nNo = 1;
    }

    int contactCount() { return nNo + nNc; }

    void updateSizeX() {
	int cols = 1 + contactCount();
	if (cols < 1)
	    cols = 1;
	sizeX = Math.max(5, 2 * cols + 2);
    }

    void updateResistance() {
	resistance = nom_v * nom_v / nom_pow;
	if (resistance < 1)
	    resistance = 1;
    }

    int contactTopPin(int i) { return 2 + i * 2; }
    int contactBotPin(int i) { return 3 + i * 2; }

    boolean contactIsNc(int i) { return i >= nNo; }

    boolean latched() { return EGTStromstossLink.isLatched(designation); }

    boolean contactClosed(int i) {
	return contactIsNc(i) ? !latched() : latched();
    }

    public String stromstossDesignation() { return designation; }
    public boolean coilPulseActive() { return coilOn; }

    boolean isDigitalChip() { return false; }
    boolean nonLinear() { return true; }

    String getChipName() { return "EGT-Stromstoßschalter"; }

    int getPostCount() { return 2 + contactCount() * 2; }
    int getVoltageSourceCount() { return 0; }

    void setupPins() {
	updateSizeX();
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
	updateSizeX();
	if (sizeY < MIN_SIZE_Y)
	    sizeY = MIN_SIZE_Y;
	int cols = 1 + contactCount();
	int[] col = new int[cols];
	EGTStyle.evenPinPos(cols, sizeX, 1, col);
	pins[N_A1].pos = col[0];
	pins[N_A1].side0 = SIDE_N;
	pins[N_A1].side = SIDE_N;
	pins[N_A1].output = false;
	pins[N_A2].pos = col[0];
	pins[N_A2].side0 = SIDE_S;
	pins[N_A2].side = SIDE_S;
	pins[N_A2].output = false;
	for (int i = 0; i < contactCount(); i++) {
	    int c = col[1 + i];
	    int t = contactTopPin(i);
	    int b = contactBotPin(i);
	    pins[t].pos = c;
	    pins[t].side0 = SIDE_N;
	    pins[t].side = SIDE_N;
	    pins[t].output = false;
	    pins[b].pos = c;
	    pins[b].side0 = SIDE_S;
	    pins[b].side = SIDE_S;
	    pins[b].output = false;
	}
    }

    void syncEndpoints() {
	if (cspc2 < 1)
	    setSize(1);
	x2 = x + sizeX * cspc2;
	y2 = y + sizeY * cspc2;
	setPoints();
    }

    void applyContactCounts() {
	clampCounts();
	setupPins();
	allocNodes();
	syncEndpoints();
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
	for (int i = 0; i < contactCount(); i++) {
	    if (EGTSchuetzLink.pinPair(n1, n2, contactTopPin(i),
				      contactBotPin(i)))
		return true;
	}
	return false;
    }

    void stamp() {
	sim.stampResistor(nodes[N_A1], nodes[N_A2], resistance);
	for (int i = 0; i < contactCount(); i++) {
	    sim.stampNonLinear(nodes[contactTopPin(i)]);
	    sim.stampNonLinear(nodes[contactBotPin(i)]);
	}
    }

    void startIteration() {}
    void execute() {}

    void reset() {
	super.reset();
	hold.reset();
	coilOn = false;
	wasCoilOn = false;
	coilReleased = false;
	EGTStromstossLink.setLatched(designation, false);
    }

    void doStep() {
	for (int i = 0; i < contactCount(); i++) {
	    if (!contactClosed(i)) {
		EGTStyle.stampOpenRef(sim, nodes[contactTopPin(i)]);
		EGTStyle.stampOpenRef(sim, nodes[contactBotPin(i)]);
		continue;
	    }
	    sim.stampResistor(nodes[contactTopPin(i)], nodes[contactBotPin(i)],
			      EGTStromstossLink.R_ON);
	}
    }

    void calculateCurrent() {
	EGTStyle.clearPinCurrents(pins);
	if (resistance <= 0) {
	    current = 0;
	    hold.reset();
	    coilOn = false;
	    wasCoilOn = false;
	    coilReleased = true;
	    return;
	}
	double vCoil = Math.abs(volts[N_A1] - volts[N_A2]);
	current = (volts[N_A1] - volts[N_A2]) / resistance;
	double iNom = nom_v / resistance;
	// Unter ~15 % Un: sofort AUS (Hold allein bleibt nach Öffnen oft „hängen“)
	if (vCoil < nom_v * 0.15) {
	    hold.reset();
	    coilOn = false;
	} else {
	    coilOn = hold.update(current, iNom, sim.timeStep);
	}
	if (!coilOn) {
	    coilReleased = true;
	} else if (!wasCoilOn && coilReleased) {
	    // Rising Edge nur nach vorherigem Spule-AUS → EIN/AUS umschalten
	    EGTStromstossLink.toggle(designation, sim.t);
	    coilReleased = false;
	}
	wasCoilOn = coilOn;
	EGTStyle.pinCurrentThru(pins, N_A1, N_A2, current);
	for (int i = 0; i < contactCount(); i++) {
	    if (!contactClosed(i)) {
		contactCurrent[i] = 0;
		continue;
	    }
	    contactCurrent[i] =
		    (volts[contactTopPin(i)] - volts[contactBotPin(i)])
			    / EGTStromstossLink.R_ON;
	    EGTStyle.pinCurrentThru(pins, contactTopPin(i), contactBotPin(i),
				    contactCurrent[i]);
	}
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
	// Kein äußerer Chip-Rahmen — wie Workbench (nur Spulenrechteck + Kontakte)
	int h = bottom - top;
	int topY = top + Math.max(10, h * 22 / 100);
	int botY = bottom - Math.max(10, h * 22 / 100);
	int cy = (topY + botY) / 2;
	int coilCx = pins[N_A1].post.x;
	int maxCoilH = botY - topY;
	int maxCoilW = Math.max(16, (right - left) / 2);
	if (contactCount() > 0) {
	    int gap = pins[contactTopPin(0)].post.x - coilCx;
	    if (gap > 16)
		maxCoilW = Math.min(maxCoilW, gap - 8);
	}
	int[] cs = EGTStyle.impulseCoilSize(maxCoilW, maxCoilH);
	int coilW = cs[0], coilH = cs[1];

	g.setColor(stroke);
	g.setLineWidth(2.5);
	g.context.setLineCap("butt");
	g.drawLine(coilCx, pins[N_A1].post.y, coilCx, cy - coilH / 2);
	g.drawLine(coilCx, cy + coilH / 2, coilCx, pins[N_A2].post.y);
	EGTStyle.drawImpulseCoil(g, coilCx, cy, coilW, coilH, stroke);

	int n = contactCount();
	for (int i = 0; i < n; i++) {
	    int px = pins[contactTopPin(i)].post.x;
	    boolean closed = contactClosed(i);
	    g.setColor(stroke);
	    g.setLineWidth(2.5);
	    g.drawLine(px, pins[contactTopPin(i)].post.y, px, topY);
	    g.drawLine(px, botY, px, pins[contactBotPin(i)].post.y);
	    if (contactIsNc(i))
		EGTStyle.drawNcContact(g, px, topY, botY, closed, stroke);
	    else
		EGTStyle.drawNoContact(g, px, topY, botY, closed, stroke);
	}
	if (n > 0) {
	    int leftC = pins[contactTopPin(0)].post.x;
	    int rightC = pins[contactTopPin(n - 1)].post.x;
	    int linkFrom = coilCx + coilW / 2;
	    // offen: Anbindung leicht vor dem Messer (Workbench)
	    int linkTo = contactClosed(0) ? leftC : leftC - Math.max(2, cspc / 4);
	    EGTStyle.drawMechLink(g, linkFrom, linkTo, cy, stroke);
	    if (n > 1)
		EGTStyle.drawMechLink(g, leftC, rightC, cy, stroke);
	}

	g.setColor(whiteColor);
	EGTStyle.drawPinLabelBeside(g, this, pins[N_A1].post.x, pins[N_A1].post.y,
				    "A1", whiteColor);
	EGTStyle.drawPinLabelBeside(g, this, pins[N_A2].post.x, pins[N_A2].post.y,
				    "A2", whiteColor);
	int labX = coilCx + coilW / 2 + 8;
	if (n > 0)
	    labX = pins[contactTopPin(n - 1)].post.x + 14;
	String cfg = nNo + "S / " + nNc + "Ö";
	String st = latched() ? "EIN (gerastet)" : "AUS";
	if (EGTStyle.chipHasEastWestPins(this)) {
	    int[] ab = EGTStyle.drawHousingAnno(g, this, left, top, right,
		    bottom, whiteColor, designation, cfg, st);
	    adjustBbox(ab[0], ab[1], ab[2], ab[3]);
	} else {
	    g.setFont(EGTStyle.pinLabelFont());
	    g.setColor(whiteColor);
	    int extra = EGTStyle.drawDesigAndNote(g, this, designation,
						 labX, cy - 10);
	    int tw = EGTStyle.measureDesigNote(g, this, designation);
	    g.drawString(cfg, labX, cy + 6 + extra);
	    g.drawString(st, labX, cy + 22 + extra);
	    adjustBbox(left, top - 28,
		       Math.max(right, labX + Math.max(tw, 80)) + 8,
		       bottom + 20);
	}
	g.setLineWidth(1.0);

	if (coilOn || n > 0) {
	    if (coilOn) {
		curcount = updateDotCount(current, curcount);
		drawDots(g, pins[N_A1].post, new Point(coilCx, cy - coilH / 2),
			 curcount);
		drawDots(g, new Point(coilCx, cy + coilH / 2), pins[N_A2].post,
			 curcount);
	    }
	    for (int i = 0; i < n; i++) {
		if (!contactClosed(i))
		    continue;
		contactCurCount[i] = updateDotCount(contactCurrent[i],
						    contactCurCount[i]);
		int px = pins[contactTopPin(i)].post.x;
		drawDots(g, pins[contactTopPin(i)].post, new Point(px, topY),
			 contactCurCount[i]);
		drawDots(g, new Point(px, botY), pins[contactBotPin(i)].post,
			 contactCurCount[i]);
	    }
	}
    }

    String dump() {
	return super.dump() + " " + sizeX + " " + sizeY + " "
		+ CustomLogicModel.escape(designation) + " " + nNo + " " + nNc
		+ " " + nom_pow + " " + nom_v + " " + (latched() ? 1 : 0) + EGTNote.dumpSuffix(note);
    }

    void dumpXml(com.google.gwt.xml.client.Document doc,
		 com.google.gwt.xml.client.Element elem) {
	super.dumpXml(doc, elem);
	XMLSerializer.dumpAttr(elem, "sx", sizeX);
	XMLSerializer.dumpAttr(elem, "sy", sizeY);
	XMLSerializer.dumpAttr(elem, "des", designation);
	EGTNote.dumpXml(elem, note);
	XMLSerializer.dumpAttr(elem, "nno", nNo);
	XMLSerializer.dumpAttr(elem, "nnc", nNc);
	XMLSerializer.dumpAttr(elem, "np", nom_pow);
	XMLSerializer.dumpAttr(elem, "nv", nom_v);
	XMLSerializer.dumpAttr(elem, "lat", latched() ? 1 : 0);
    }

    void undumpXml(XMLDeserializer xml) {
	super.undumpXml(xml);
	flags &= ~EGTStyle.FLAG_UEBERSICHT;
	designation = xml.parseStringAttr("des", designation);
	note = EGTNote.undumpXml(xml, note);
	nNo = (int) xml.parseDoubleAttr("nno", nNo);
	nNc = (int) xml.parseDoubleAttr("nnc", nNc);
	nom_pow = xml.parseDoubleAttr("np", nom_pow);
	nom_v = xml.parseDoubleAttr("nv", nom_v);
	boolean dumpLatched = xml.parseDoubleAttr("lat", 0) != 0;
	if (nom_pow <= 0)
	    nom_pow = DEF_NOM_POW;
	if (nom_v <= 0)
	    nom_v = DEF_NOM_V;
	clampCounts();
	updateSizeX();
	sizeY = DEF_SIZE_Y;
	setSize(1);
	updateResistance();
	EGTStromstossLink.setLatched(designation, dumpLatched);
	layoutPins();
	syncEndpoints();
    }

    int getDumpType() { return 460; }
    String getXmlDumpType() { return "EGTStromstossschalter"; }

    void getInfo(String arr[]) {
	arr[0] = getChipName() + " (" + designation + ")";
	arr[1] = latched() ? "EIN (gerastet)" : "AUS";
	arr[2] = "Kontakte = " + nNo + " Schließer / " + nNc + " Öffner";
	arr[3] = "A1 = " + getVoltageText(volts[N_A1]);
	arr[4] = "A2 = " + getVoltageText(volts[N_A2]);
    }

    public EditInfo getChipEditInfo(int n) {
	if (n == 0)
	    return new EditInfo("Bezeichnung", designation);
	if (n == 1)
	    return new EditInfo("Anzahl Schließer (0–4)", nNo, 0, MAX_CONTACTS)
		    .setDimensionless();
	if (n == 2)
	    return new EditInfo("Anzahl Öffner (0–4)", nNc, 0, MAX_CONTACTS)
		    .setDimensionless();
	if (n == 3)
	    return new EditInfo("Nennleistung Spule (W)", nom_pow, 0, 0)
		    .setPositive();
	if (n == 4)
	    return new EditInfo("Nennspannung Spule (V)", nom_v, 0, 0)
		    .setPositive();
	return null;
    }

    public void setChipEditValue(int n, EditInfo ei) {
	if (n == 0) {
	    String old = designation;
	    designation = ei.textf.getText();
	    if (!EGTStromstossLink.normalize(old)
		    .equals(EGTStromstossLink.normalize(designation))) {
		boolean L = EGTStromstossLink.isLatched(old);
		EGTStromstossLink.clearDesignation(old);
		EGTStromstossLink.setLatched(designation, L);
	    }
	}
	if (n == 1) {
	    nNo = (int) ei.value;
	    applyContactCounts();
	}
	if (n == 2) {
	    nNc = (int) ei.value;
	    applyContactCounts();
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

/*
    TAR-Dervic EGT: Stromstoßschalter-Spule A1/A2 (S305 / MOD-REL-01), Dump 461
*/

package com.lushprojects.circuitjs1.client;

/**
 * Getrennt: Spule mit Stufenmarkierung. Impuls toggelt Rast über EGTStromstossLink.
 */
class EGTStromstossSpuleElm extends ChipElm implements EGTStromstossSource, EGTDesignatable {
    /** Gleich wie EGTSchuetzSpuleElm — Spule/Kontakt DIN-gleiche Rastergröße. */
    static final int DEF_SIZE_X = 3;
    static final int DEF_SIZE_Y = 3;
    static final int MIN_SIZE_X = DEF_SIZE_X;
    static final int MIN_SIZE_Y = DEF_SIZE_Y;

    static final double DEF_NOM_POW = 5;
    static final double DEF_NOM_V = 230;

    String designation = "E1";
    String note = "";

    public String egtDesignation() { return designation; }
    public void setEgtDesignation(String d) {
	if (d != null)
	    designation = d;
    }
    public String egtNote() { return note; }
    public void setEgtNote(String s) { note = EGTNote.clamp(s); }
    double nom_pow = DEF_NOM_POW;
    double nom_v = DEF_NOM_V;
    double resistance = DEF_NOM_V * DEF_NOM_V / DEF_NOM_POW;
    boolean coilOn;
    boolean wasCoilOn;
    /** true erst nach Spule-AUS — nächster Rising Edge toggelt. */
    boolean coilReleased;
    EGTSchuetzCoilHold hold = new EGTSchuetzCoilHold();

    public EGTStromstossSpuleElm(int xx, int yy) {
	super(xx, yy);
	setSize(1);
	sizeX = DEF_SIZE_X;
	sizeY = DEF_SIZE_Y;
	updateResistance();
	layoutPins();
	syncEndpoints();
    }

    public EGTStromstossSpuleElm(int xa, int ya, int xb, int yb, int f,
				 StringTokenizer st) {
	super(xa, ya, xb, yb, f, st);
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
		    nom_pow = Double.parseDouble(st.nextToken());
		if (st.hasMoreTokens())
		    nom_v = Double.parseDouble(st.nextToken());
		if (st.hasMoreTokens())
		    dumpLatched = Double.parseDouble(st.nextToken()) != 0;
	    } catch (Exception e) {
		designation = CustomLogicModel.unescape(t);
	    }
	}
	sizeX = DEF_SIZE_X;
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

    void updateResistance() {
	resistance = nom_v * nom_v / nom_pow;
	if (resistance < 1)
	    resistance = 1;
    }

    boolean latched() { return EGTStromstossLink.isLatched(designation); }

    boolean uebersicht() {
	return (flags & EGTStyle.FLAG_UEBERSICHT) != 0;
    }

    public String stromstossDesignation() { return designation; }
    public boolean coilPulseActive() { return coilOn; }

    boolean isDigitalChip() { return false; }

    String getChipName() { return "EGT-Stromstoß-Spule"; }

    int getPostCount() { return uebersicht() ? 1 : 2; }
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
	if (sizeX < MIN_SIZE_X)
	    sizeX = MIN_SIZE_X;
	if (sizeY < MIN_SIZE_Y)
	    sizeY = MIN_SIZE_Y;
	int mid = sizeX / 2;
	pins[0].pos = mid;
	pins[0].side0 = SIDE_N;
	pins[0].side = SIDE_N;
	pins[0].output = false;
	if (uebersicht())
	    return;
	pins[1].pos = mid;
	pins[1].side0 = SIDE_S;
	pins[1].side = SIDE_S;
	pins[1].output = false;
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

    void reset() {
	super.reset();
	hold.reset();
	coilOn = false;
	wasCoilOn = false;
	coilReleased = false;
	EGTStromstossLink.setLatched(designation, false);
    }

    void calculateCurrent() {
	if (uebersicht()) {
	    current = 0;
	    coilOn = false;
	    return;
	}
	EGTStyle.clearPinCurrents(pins);
	if (resistance <= 0) {
	    current = 0;
	    hold.reset();
	    coilOn = false;
	    wasCoilOn = false;
	    coilReleased = true;
	    return;
	}
	double vCoil = Math.abs(volts[0] - volts[1]);
	current = (volts[0] - volts[1]) / resistance;
	EGTStyle.pinCurrentThru(pins, 0, 1, current);
	double iNom = nom_v / resistance;
	if (vCoil < nom_v * 0.15) {
	    hold.reset();
	    coilOn = false;
	} else {
	    coilOn = hold.update(current, iNom, sim.timeStep);
	}
	if (!coilOn) {
	    coilReleased = true;
	} else if (!wasCoilOn && coilReleased) {
	    EGTStromstossLink.toggle(designation, sim.t);
	    coilReleased = false;
	}
	wasCoilOn = coilOn;
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
	    if (uebersicht()) {
		drawUebersicht(g, b[0], b[1], b[2], b[3], stroke);
		drawPosts(g);
		return;
	    }
	    int left = b[0], top = b[1], right = b[2], bottom = b[3];
	    int h = bottom - top;
	    int w = right - left;
	    int px = pins[0].post.x;
	    int cy = (top + bottom) / 2;
	    int[] cs = EGTStyle.impulseCoilSize(w, h);
	    int coilW = cs[0], coilH = cs[1];

	    g.setColor(stroke);
	    g.setLineWidth(2.5);
	    g.context.setLineCap("butt");
	    g.drawLine(px, pins[0].post.y, px, cy - coilH / 2);
	    g.drawLine(px, cy + coilH / 2, px, pins[1].post.y);
	    EGTStyle.drawImpulseCoil(g, px, cy, coilW, coilH, stroke);

	    g.setColor(whiteColor);
	    EGTStyle.drawPinLabelBeside(g, this, pins[0].post.x, pins[0].post.y,
					"A1", whiteColor);
	    EGTStyle.drawPinLabelBeside(g, this, pins[1].post.x, pins[1].post.y,
					"A2", whiteColor);

	    g.setFont(EGTStyle.pinLabelFont());
	    int[] ab = EGTStyle.drawCoilAnno(g, this, left, top, right, bottom,
		    px + coilW / 2 + 6, cy - 4, whiteColor, designation,
		    latched() ? "EIN (gerastet)" : "AUS");
	    adjustBbox(ab[0], ab[1], ab[2], ab[3]);
	    g.setLineWidth(1.0);

	    if (coilOn) {
		curcount = updateDotCount(current, curcount);
		drawDots(g, pins[0].post, new Point(px, cy - coilH / 2),
			 curcount);
		drawDots(g, new Point(px, cy + coilH / 2), pins[1].post,
			 curcount);
	    }
	    drawPosts(g);
	} finally {
	    g.restore();
	}
    }

    void drawUebersicht(Graphics g, int left, int top, int right, int bottom,
			 Color stroke) {
	int cx = (left + right) / 2;
	int cy = (top + bottom) / 2;
	int side = Math.max(24, Math.min(right - left, bottom - top) * 2 / 3);
	int l = cx - side / 2;
	int t = cy - side / 2;

	g.setColor(stroke);
	g.setLineWidth(2.5);
	g.drawLine(pins[0].post.x, pins[0].post.y, cx, t);
	g.drawRect(l, t, side, side);
	EGTStyle.drawImpulseCoil(g, cx, cy, side * 2 / 3, side / 2, stroke);
	g.setLineWidth(1.0);
	g.setFont(EGTStyle.pinLabelFont());
	g.setColor(whiteColor);
	int tw = EGTStyle.measureDesigNote(g, this, designation);
	EGTStyle.drawDesigAndNote(g, this, designation, l + side + 8, cy + 4);
	adjustBbox(left, pins[0].post.y - 8,
		   l + side + Math.max(tw, 24) + 16, bottom);
    }

    String dump() {
	return super.dump() + " " + sizeX + " " + sizeY + " "
		+ CustomLogicModel.escape(designation) + " " + nom_pow + " "
		+ nom_v + " " + (latched() ? 1 : 0) + EGTNote.dumpSuffix(note);
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
	XMLSerializer.dumpAttr(elem, "lat", latched() ? 1 : 0);
    }

    void undumpXml(XMLDeserializer xml) {
	super.undumpXml(xml);
	sizeX = DEF_SIZE_X;
	sizeY = DEF_SIZE_Y;
	designation = xml.parseStringAttr("des", designation);
	note = EGTNote.undumpXml(xml, note);
	nom_pow = xml.parseDoubleAttr("np", nom_pow);
	nom_v = xml.parseDoubleAttr("nv", nom_v);
	boolean dumpLatched = xml.parseDoubleAttr("lat", 0) != 0;
	if (nom_pow <= 0)
	    nom_pow = DEF_NOM_POW;
	if (nom_v <= 0)
	    nom_v = DEF_NOM_V;
	setSize(1);
	updateResistance();
	EGTStromstossLink.setLatched(designation, dumpLatched);
	layoutPins();
	syncEndpoints();
    }

    int getDumpType() { return 461; }
    String getXmlDumpType() { return "EGTStromstossSpule"; }

    void getInfo(String arr[]) {
	arr[0] = getChipName() + " (" + designation + ")";
	arr[1] = latched() ? "EIN (gerastet)" : "AUS";
	arr[2] = "A1 = " + getVoltageText(volts[0]);
	arr[3] = "A2 = " + getVoltageText(volts[1]);
	arr[4] = "P = " + getUnitText(current * (volts[0] - volts[1]), "W");
    }

    public EditInfo getChipEditInfo(int n) {
	if (n == 0)
	    return new EditInfo("Bezeichnung", designation);
	if (n == 1)
	    return new EditInfo("Nennleistung Spule (W)", nom_pow, 0, 0)
		    .setPositive();
	if (n == 2)
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
	    nom_pow = ei.value;
	    updateResistance();
	}
	if (n == 2) {
	    nom_v = ei.value;
	    updateResistance();
	}
    }
}

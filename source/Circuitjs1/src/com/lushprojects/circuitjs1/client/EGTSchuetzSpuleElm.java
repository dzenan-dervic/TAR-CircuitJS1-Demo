/*
    TAR-Dervic EGT: Leistungsschütz-Spule A1/A2 (S301 / MOD-SCH-01), Dump 454
*/

package com.lushprojects.circuitjs1.client;

import com.google.gwt.user.client.ui.Button;
import com.lushprojects.circuitjs1.client.util.Locale;

/**
 * Schützspule Steuerstrom A1/A2. Setzt EGTSchuetzLink bei sinnvollem Strom.
 */
class EGTSchuetzSpuleElm extends ChipElm implements EGTSchuetzCoilSource, EGTDesignatable {
    /** Kompakte DIN-Spule — feste Größe. */
    static final int DEF_SIZE_X = 3;
    static final int DEF_SIZE_Y = 3;
    static final int MIN_SIZE_X = DEF_SIZE_X;
    static final int MIN_SIZE_Y = DEF_SIZE_Y;

    String designation = "Q1";
    String note = "";

    public String egtDesignation() { return designation; }
    public void setEgtDesignation(String d) {
	if (d != null)
	    designation = d;
    }
    public String egtNote() { return note; }
    public void setEgtNote(String s) { note = EGTNote.clamp(s); }
    double nom_pow = 15;
    double nom_v = 230;
    double resistance = 230 * 230 / 15.;
    boolean energized;
    EGTSchuetzCoilHold hold = new EGTSchuetzCoilHold();
    EGTSchuetzCoilL coilL = new EGTSchuetzCoilL();

    public EGTSchuetzSpuleElm(int xx, int yy) {
	super(xx, yy);
	flags &= ~EGTStyle.FLAG_UEBERSICHT;
	setSize(1);
	sizeX = DEF_SIZE_X;
	sizeY = DEF_SIZE_Y;
	updateResistance();
	layoutPins();
	syncEndpoints();
    }

    public EGTSchuetzSpuleElm(int xa, int ya, int xb, int yb, int f,
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
		    nom_pow = Double.parseDouble(st.nextToken());
		if (st.hasMoreTokens())
		    nom_v = Double.parseDouble(st.nextToken());
		if (st.hasMoreTokens())
		    coilL.inductance = Double.parseDouble(st.nextToken());
	} catch (Exception e) {
		designation = CustomLogicModel.unescape(t);
	    }
	}
	sizeX = DEF_SIZE_X;
	sizeY = DEF_SIZE_Y;
	if (nom_pow <= 0)
	    nom_pow = 15;
	if (nom_v <= 0)
	    nom_v = 230;
	updateResistance();
	layoutPins();
	allocNodes();
	syncEndpoints();
    
	note = EGTNote.readOptional(st);
}

    void updateResistance() {
	resistance = nom_v * nom_v / nom_pow;
	if (resistance < 1)
	    resistance = 1;
	if (!EGTSchuetzCoilL.lCustom(flags) || coilL.inductance <= 0)
	    coilL.inductance = EGTSchuetzCoilL.defaultHenries(nom_v, nom_pow);
    }

    public String schuetzDesignation() { return designation; }
    public boolean coilEnergized() { return energized; }

    boolean isDigitalChip() { return false; }

    String getChipName() { return "EGT-Schütz-Spule"; }

    int getPostCount() { return 2; }
    int getInternalNodeCount() { return 1; }
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

    /** ChipElm.drag — feste Größe, nur Platzieren (kein Size-Ziehen). */

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
	return (n1 == 0 && n2 == 1) || (n1 == 1 && n2 == 0);
    }

    boolean getMatrixConnection(int n1, int n2) {
	return n1 != n2;
    }

    void stamp() {
	coilL.stamp(sim, nodes[0], nodes[2], nodes[1]);
    }

    void startIteration() {
	coilL.startIteration(volts[0], volts[2]);
    }

    void execute() {}

    void doStep() {
	coilL.doStep(volts[0], volts[2]);
    }

    void reset() {
	super.reset();
	hold.reset();
	coilL.reset();
	energized = false;
    }

    void calculateCurrent() {
	EGTStyle.clearPinCurrents(pins);
	if (resistance <= 0) {
	    current = 0;
	    hold.reset();
	    energized = false;
	    return;
	}
	current = coilL.calculateCurrent(volts[0], volts[2]);
	double iNom = nom_pow / nom_v;
	energized = hold.update(current, iNom, sim.timeStep);
	EGTStyle.pinCurrentThru(pins, 0, 1, current);
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
	    int left = b[0], top = b[1], right = b[2], bottom = b[3];
	    int h = bottom - top;
	    int w = right - left;
	    // Achse = Pin (nicht Box-Mitte) — sonst Posts versetzt bei geradem sizeX
	    int px = pins[0].post.x;
	    int cy = (top + bottom) / 2;
	    int[] cs = EGTStyle.standardCoilSize(w, h);
	    int coilW = cs[0], coilH = cs[1];

	    g.setColor(stroke);
	    g.setLineWidth(2.5);
	    g.context.setLineCap("butt");
	    g.drawLine(px, pins[0].post.y, px, cy - coilH / 2);
	    g.drawLine(px, cy + coilH / 2, px, pins[1].post.y);
	    EGTStyle.drawCoilRect(g, px, cy, coilW, coilH, true, stroke);
	    g.setColor(whiteColor);
	    EGTStyle.drawPinLabelBeside(g, this, pins[0].post.x, pins[0].post.y,
					"A1", whiteColor);
	    EGTStyle.drawPinLabelBeside(g, this, pins[1].post.x, pins[1].post.y,
					"A2", whiteColor);

	    g.setFont(EGTStyle.pinLabelFont());
	    int[] ab = EGTStyle.drawCoilAnno(g, this, left, top, right, bottom,
		    px + coilW / 2 + 6, cy - 4, whiteColor, designation,
		    energized ? "angezogen" : "abgefallen");
	    adjustBbox(ab[0], ab[1], ab[2], ab[3]);
	    g.setLineWidth(1.0);

	    if (energized) {
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

    String dump() {
	flags &= ~EGTStyle.FLAG_UEBERSICHT;
	return super.dump() + " " + sizeX + " " + sizeY + " "
		+ CustomLogicModel.escape(designation) + " " + nom_pow + " "
		+ nom_v + " " + coilL.inductance + EGTNote.dumpSuffix(note);
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
	XMLSerializer.dumpAttr(elem, "L", coilL.inductance);
    }

    void undumpXml(XMLDeserializer xml) {
	super.undumpXml(xml);
	sizeX = (int) xml.parseDoubleAttr("sx", sizeX);
	sizeY = (int) xml.parseDoubleAttr("sy", sizeY);
	designation = xml.parseStringAttr("des", designation);
	note = EGTNote.undumpXml(xml, note);
	nom_pow = xml.parseDoubleAttr("np", nom_pow);
	nom_v = xml.parseDoubleAttr("nv", nom_v);
	coilL.inductance = xml.parseDoubleAttr("L", coilL.inductance);
	if (nom_pow <= 0)
	    nom_pow = 15;
	if (nom_v <= 0)
	    nom_v = 230;
	sizeX = DEF_SIZE_X;
	sizeY = DEF_SIZE_Y;
	setSize(1);
	flags &= ~EGTStyle.FLAG_UEBERSICHT;
	updateResistance();
	layoutPins();
	syncEndpoints();
    }

    int getDumpType() { return 454; }
    String getXmlDumpType() { return "EGTSchuetzSpule"; }

    void getInfo(String arr[]) {
	arr[0] = getChipName() + " (" + designation + ")";
	arr[1] = energized ? "angezogen" : "abgefallen";
	arr[2] = "A1 = " + getVoltageText(volts[0]);
	arr[3] = "A2 = " + getVoltageText(volts[1]);
	arr[4] = "P = " + getUnitText(current * (volts[0] - volts[1]), "W");
	arr[5] = "L = " + getUnitText(coilL.inductance, "H")
		+ (EGTSchuetzCoilL.lCustom(flags) ? " (frei)" : " (auto)");
    }

    public EditInfo getChipEditInfo(int n) {
	if (n == 0)
	    return new EditInfo("Bezeichnung", designation);
	if (n == 1)
	    return EGTSchuetzCoilL.modelEditInfo(flags, nom_v, nom_pow);
	if (n == 2)
	    return new EditInfo("Nennleistung (VA)", nom_pow, 0, 0).setPositive();
	if (n == 3)
	    return new EditInfo("Nennspannung (V)", nom_v, 0, 0).setPositive();
	if (n == 4) {
	    EditInfo ei = new EditInfo("", 0, -1, -1);
	    ei.button = new Button(Locale.LS("Leistungskontakte hinzufügen"));
	    return ei;
	}
	if (n == 5) {
	    EditInfo ei = new EditInfo("", 0, -1, -1);
	    ei.button = new Button(Locale.LS("Hilfskontakt hinzufügen"));
	    return ei;
	}
	if (n == 6)
	    return EGTSchuetzCoilL.inductanceEditInfo(flags, coilL.inductance);
	return null;
    }

    public void setChipEditValue(int n, EditInfo ei) {
	if (n == 0)
	    designation = ei.textf.getText();
	if (n == 1 && ei.choice != null) {
	    double pu[] = { nom_pow, nom_v };
	    flags = EGTSchuetzCoilL.applyPreset(ei.choice.getSelectedIndex(),
					       pu, coilL, flags);
	    nom_pow = pu[0];
	    nom_v = pu[1];
	    updateResistance();
	    ei.newDialog = true;
	}
	if (n == 2 && ei.value > 0) {
	    nom_pow = ei.value;
	    if (!EGTSchuetzCoilL.lCustom(flags))
		coilL.inductance = 0;
	    updateResistance();
	}
	if (n == 3 && ei.value > 0) {
	    nom_v = ei.value;
	    if (!EGTSchuetzCoilL.lCustom(flags))
		coilL.inductance = 0;
	    updateResistance();
	}
	if (n == 4)
	    spawnLeistungskontakte();
	if (n == 5)
	    spawnHilfskontakt();
	if (n == 6 && ei.value > 0) {
	    coilL.inductance = ei.value;
	    flags |= EGTStyle.FLAG_L_CUSTOM;
	    updateResistance();
	    ei.newDialog = true;
	}
    }

    boolean hasLeistungskontakte() {
	if (app == null || app.elmList == null)
	    return false;
	String des = EGTSchuetzLink.normalize(designation);
	for (int i = 0; i < app.elmList.size(); i++) {
	    CircuitElm ce = app.elmList.elementAt(i);
	    if (!(ce instanceof EGTSchuetzLeistungskontakteElm))
		continue;
	    if (EGTSchuetzLink.normalize(
		    ((EGTSchuetzLeistungskontakteElm) ce).designation)
		    .equals(des))
		return true;
	}
	return false;
    }

    int nextSpawnX() {
	int px = snapGrid(x2 + 16);
	if (app == null || app.elmList == null)
	    return px;
	String des = EGTSchuetzLink.normalize(designation);
	for (int i = 0; i < app.elmList.size(); i++) {
	    CircuitElm ce = app.elmList.elementAt(i);
	    if (ce == this)
		continue;
	    String d = null;
	    if (ce instanceof EGTSchuetzLeistungskontakteElm)
		d = ((EGTSchuetzLeistungskontakteElm) ce).designation;
	    else if (ce instanceof EGTSchuetzHilfskontaktElm)
		d = ((EGTSchuetzHilfskontaktElm) ce).designation;
	    if (d == null || !EGTSchuetzLink.normalize(d).equals(des))
		continue;
	    int nx = snapGrid(ce.x2 + 16);
	    if (nx > px)
		px = nx;
	}
	return px;
    }

    void spawnLeistungskontakte() {
	if (hasLeistungskontakte())
	    return;
	placeSibling("EGTSchuetzLeistungskontakteElm");
    }

    void spawnHilfskontakt() {
	EGTSchuetzLink.placeHilfskontakt(this, designation);
    }

    void placeSibling(String className) {
	if (app == null || app.elmList == null)
	    return;
	app.undoManager.pushUndo();
	CircuitElm ce = CirSim.constructElement(className, nextSpawnX(),
						snapGrid(y));
	if (ce == null)
	    return;
	if (ce instanceof EGTDesignatable)
	    ((EGTDesignatable) ce).setEgtDesignation(designation);
	app.elmList.addElement(ce);
	ce.draggingDone();
	app.needAnalyze();
	app.undoManager.writeRecoveryToStorage();
	app.unsavedChanges = true;
    }
}

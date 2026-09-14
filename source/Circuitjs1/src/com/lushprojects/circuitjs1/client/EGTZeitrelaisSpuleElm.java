/*
    TAR-Dervic EGT: Zeitrelais-Spule A1/A2 (S303 / MOD-REL-03), Dump 464
*/

package com.lushprojects.circuitjs1.client;

import java.util.Vector;

import com.google.gwt.user.client.Window;
import com.lushprojects.circuitjs1.client.util.Locale;

/**
 * Getrennt: Spule Fachkunde 3×1 (X / gefüllt). Erregung steuert Verzögerung über Link.
 */
class EGTZeitrelaisSpuleElm extends ChipElm implements EGTZeitrelaisSource, EGTDesignatable {
    static final int FLAG_OFF_DELAY = 2;
    static final int FLAG_BLINK = 4;
    static final int FLAG_COMBINED = 8;
    /** Gleich wie Schütz/Stromstoß-Spule. */
    static final int DEF_SIZE_X = 3;
    static final int DEF_SIZE_Y = 3;
    static final int MIN_SIZE_X = DEF_SIZE_X;
    static final int MIN_SIZE_Y = DEF_SIZE_Y;

    String designation = "K1";
    String note = "";

    public String egtDesignation() { return designation; }
    public void setEgtDesignation(String d) {
	if (d != null)
	    designation = d;
    }
    public String egtNote() { return note; }
    public void setEgtNote(String s) { note = EGTNote.clamp(s); }
    double delay = 1;
    double delayOff = EGTZeitrelaisLink.DEF_BLINK;
    double nom_pow = 15;
    double nom_v = 230;
    double resistance = 230 * 230 / 15.;
    boolean coilOn;
    EGTSchuetzCoilHold hold = new EGTSchuetzCoilHold();
    boolean skipTimeWrite;

    public EGTZeitrelaisSpuleElm(int xx, int yy) {
	super(xx, yy);
	setSize(1);
	sizeX = DEF_SIZE_X;
	sizeY = DEF_SIZE_Y;
	updateResistance();
	layoutPins();
	syncEndpoints();
    }

    public EGTZeitrelaisSpuleElm(int xa, int ya, int xb, int yb, int f,
				 StringTokenizer st) {
	super(xa, ya, xb, yb, f, st);
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
		    delayOff = Double.parseDouble(st.nextToken());
	    } catch (Exception e) {
		designation = CustomLogicModel.unescape(t);
	    }
	}
	sizeX = DEF_SIZE_X;
	sizeY = DEF_SIZE_Y;
	if (delay < 0)
	    delay = 0;
	if (delayOff < 0)
	    delayOff = 0;
	if (nom_pow <= 0)
	    nom_pow = 15;
	if (nom_v <= 0)
	    nom_v = 230;
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

    boolean isBlink() { return (flags & FLAG_BLINK) != 0; }
    boolean isCombined() { return (flags & FLAG_COMBINED) != 0; }
    boolean isOffDelay() {
	return !isBlink() && !isCombined()
		&& (flags & FLAG_OFF_DELAY) != 0;
    }

    int delayMode() {
	if (isBlink())
	    return EGTZeitrelaisLink.MODE_BLINK;
	if (isCombined())
	    return EGTZeitrelaisLink.MODE_COMBINED;
	if (isOffDelay())
	    return EGTZeitrelaisLink.MODE_OFF;
	return EGTZeitrelaisLink.MODE_ON;
    }

    void alignDelayMode(int mode) {
	boolean wasBlink = isBlink();
	boolean wasCombined = isCombined();
	flags &= ~(FLAG_OFF_DELAY | FLAG_BLINK | FLAG_COMBINED);
	skipTimeWrite = false;
	if (mode == EGTZeitrelaisLink.MODE_BLINK) {
	    flags |= FLAG_BLINK;
	    if (!wasBlink) {
		delay = EGTZeitrelaisLink.DEF_BLINK;
		delayOff = EGTZeitrelaisLink.DEF_BLINK;
		skipTimeWrite = true;
	    }
	} else if (mode == EGTZeitrelaisLink.MODE_COMBINED) {
	    flags |= FLAG_COMBINED;
	    if (!wasCombined) {
		delay = EGTZeitrelaisLink.DEF_COMBINED_ON;
		delayOff = EGTZeitrelaisLink.DEF_COMBINED_OFF;
		skipTimeWrite = true;
	    }
	} else if (mode == EGTZeitrelaisLink.MODE_OFF)
	    flags |= FLAG_OFF_DELAY;
	setPoints();
    }

    void alignDelayFlag(boolean off) {
	alignDelayMode(off ? EGTZeitrelaisLink.MODE_OFF
			   : EGTZeitrelaisLink.MODE_ON);
    }

    public String zeitrelaisDesignation() { return designation; }
    public boolean coilEnergized() { return coilOn; }

    boolean isDigitalChip() { return false; }

    String getChipName() {
	if (isBlink())
	    return "EGT-Zeitrelais-Spule (Blinkend)";
	if (isCombined())
	    return "EGT-Zeitrelais-Spule (Ansprech/Rückfall)";
	return "EGT-Zeitrelais-Spule (Ein/Aus)";
    }

    int getPostCount() { return 2; }
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

    void stamp() {
	sim.stampResistor(nodes[0], nodes[1], resistance);
    }

    void startIteration() {}
    void execute() {}
    void doStep() {}

    void reset() {
	super.reset();
	hold.reset();
	coilOn = false;
	EGTZeitrelaisLink.reset(designation);
    }

    void calculateCurrent() {
	EGTStyle.clearPinCurrents(pins);
	if (resistance <= 0) {
	    current = 0;
	    hold.reset();
	    coilOn = false;
	    EGTZeitrelaisLink.updateFromCoil(designation, false, delayMode(),
					    delay, delayOff);
	    return;
	}
	current = (volts[0] - volts[1]) / resistance;
	EGTStyle.pinCurrentThru(pins, 0, 1, current);
	double iNom = nom_v / resistance;
	// Nicht |u| < 15 % Un: bei 50 Hz wäre das jeder Nulldurchgang →
	// lastTransition reset, Einschaltverzögerung läuft nie ab.
	coilOn = hold.update(current, iNom, sim.timeStep);
	EGTZeitrelaisLink.updateFromCoil(designation, coilOn, delayMode(),
					 delay, delayOff);
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
	    int px = pins[0].post.x;
	    int cy = (top + bottom) / 2;
	    // Gleiche Rasterhöhe wie Schütz/Stromstoß; Fachkunde 3×1 (u = coilH)
	    int[] cs = EGTStyle.standardCoilSize(w, h);
	    int u = cs[1];
	    int coilTop = cy - u / 2;
	    int coilBot = cy + u / 2;

	    g.setColor(stroke);
	    g.setLineWidth(2.5);
	    g.context.setLineCap("butt");
	    g.drawLine(px, pins[0].post.y, px, coilTop);
	    g.drawLine(px, coilBot, px, pins[1].post.y);
	    EGTStyle.drawZeitCoil(g, px, cy, u, isOffDelay(), isBlink(),
				 isCombined(),
				 stroke, false);

	    // Einheitlich zu Schütz- und Stromstoßspule: Klemmen horizontal
	    // direkt neben den jeweiligen Anschlusspunkten.
	    EGTStyle.drawPinLabelBeside(g, this, pins[0].post.x,
					pins[0].post.y, "A1", whiteColor);
	    EGTStyle.drawPinLabelBeside(g, this, pins[1].post.x,
					pins[1].post.y, "A2", whiteColor);

	    g.setFont(EGTStyle.pinLabelFont());
	    int lx = px + (3 * u) / 2 + 6;
	    int[] ab = EGTStyle.drawCoilAnno(g, this, left, top, right, bottom,
		    lx, cy - 4, whiteColor, designation,
		    isBlink() ? "Blinken (Takt)"
			      : (isCombined() ? "Ansprech + Rückfall"
			      : (isOffDelay() ? "Aus (Rückfall)"
					      : "Ein (Ansprech)")));
	    adjustBbox(px - 2 * u - 4, coilTop - 4, px + u + 4,
		       coilBot + 4);
	    adjustBbox(ab[0], ab[1], ab[2], ab[3]);
	    g.setLineWidth(1.0);

	    if (coilOn) {
		curcount = updateDotCount(current, curcount);
		drawDots(g, pins[0].post, new Point(px, coilTop), curcount);
		drawDots(g, new Point(px, coilBot), pins[1].post, curcount);
	    }
	    drawPosts(g);
	} finally {
	    g.restore();
	}
    }

    String dump() {
	return super.dump() + " " + sizeX + " " + sizeY + " "
		+ CustomLogicModel.escape(designation) + " " + delay + " "
		+ nom_pow + " " + nom_v + " " + delayOff + EGTNote.dumpSuffix(note);
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
	XMLSerializer.dumpAttr(elem, "doff", delayOff);
    }

    void undumpXml(XMLDeserializer xml) {
	super.undumpXml(xml);
	sizeX = DEF_SIZE_X;
	sizeY = DEF_SIZE_Y;
	designation = xml.parseStringAttr("des", designation);
	note = EGTNote.undumpXml(xml, note);
	delay = xml.parseDoubleAttr("del", delay);
	nom_pow = xml.parseDoubleAttr("np", nom_pow);
	nom_v = xml.parseDoubleAttr("nv", nom_v);
	delayOff = xml.parseDoubleAttr("doff", delayOff);
	if (delay < 0)
	    delay = 0;
	if (delayOff < 0)
	    delayOff = 0;
	if (nom_pow <= 0)
	    nom_pow = 15;
	if (nom_v <= 0)
	    nom_v = 230;
	setSize(1);
	updateResistance();
	layoutPins();
	syncEndpoints();
    }

    int getDumpType() { return 464; }
    String getXmlDumpType() { return "EGTZeitrelaisSpule"; }

    void getInfo(String arr[]) {
	arr[0] = getChipName() + " (" + designation + ")";
	arr[1] = EGTZeitrelaisLink.statusText(designation);
	String tl = EGTZeitrelaisLink.timerLabel(designation);
	if (tl != null)
	    arr[2] = tl;
	else if (isBlink())
	    arr[2] = "tEin = " + getUnitText(delay, "s") + ", tAus = "
		    + getUnitText(delayOff, "s");
	else if (isCombined())
	    arr[2] = "tAn = " + getUnitText(delay, "s") + ", tAb = "
		    + getUnitText(delayOff, "s");
	else
	    arr[2] = "T = " + getUnitText(delay, "s");
	arr[3] = "A1 = " + getVoltageText(volts[0]);
	arr[4] = "A2 = " + getVoltageText(volts[1]);
    }

    public EditInfo getChipEditInfo(int n) {
	if (n == 0)
	    return new EditInfo("Bezeichnung", designation);
	if (n == 1) {
	    EditInfo ei = new EditInfo("Verzögerungsart", 0);
	    ei.choice = new Choice();
	    ei.choice.add(Locale.LS("Einschaltverzögert (Ansprech)"));
	    ei.choice.add(Locale.LS("Ausschaltverzögert (Rückfall)"));
	    ei.choice.add(Locale.LS("Ansprech- und rückfallverzögert"));
	    ei.choice.add(Locale.LS("Blinkend (Taktgeber)"));
	    ei.choice.select(delayMode());
	    return ei;
	}
	if (n == 2)
	    return new EditInfo(isBlink() ? "Einschaltdauer (s)"
			 : (isCombined() ? "Ansprechverzögerung (s)"
					 : "Verzögerung (s)"), delay, 0, 0);
	if (n == 3)
	    return new EditInfo(isCombined() ? "Rückfallverzögerung (s)"
					     : "Ausschaltdauer (s)",
				delayOff, 0, 0);
	if (n == 4)
	    return new EditInfo("Nennleistung Spule (W)", nom_pow, 0, 0)
		    .setPositive();
	if (n == 5)
	    return new EditInfo("Nennspannung Spule (V)", nom_v, 0, 0)
		    .setPositive();
	return null;
    }

    public void setChipEditValue(int n, EditInfo ei) {
	if (n == 0) {
	    String old = designation;
	    designation = ei.textf.getText();
	    if (!EGTZeitrelaisLink.normalize(old)
		    .equals(EGTZeitrelaisLink.normalize(designation))) {
		boolean on = EGTZeitrelaisLink.isTimedOn(old);
		EGTZeitrelaisLink.clearDesignation(old);
		EGTZeitrelaisLink.setTimedOn(designation, on);
	    }
	}
	if (n == 1) {
	    int mode = ei.choice.getSelectedIndex();
	    if (mode < 0 || mode > 3)
		mode = 0;
	    alignDelayMode(mode);
	    Vector ks = EGTZeitrelaisLink.findContacts(designation);
	    boolean mismatch = false;
	    for (int i = 0; i < ks.size(); i++) {
		EGTZeitrelaisKontaktElm k =
			(EGTZeitrelaisKontaktElm) ks.elementAt(i);
		if (k.delayMode() != mode) {
		    mismatch = true;
		    k.alignDelayMode(mode);
		}
	    }
	    if (mismatch) {
		String des = EGTZeitrelaisLink.normalize(designation);
		String key;
		if (mode == EGTZeitrelaisLink.MODE_BLINK)
		    key = "Coil NAME is flashing (blink) — contact will be aligned";
		else if (mode == EGTZeitrelaisLink.MODE_COMBINED)
		    key = "Coil NAME is combined-delay — contact will be aligned";
		else if (mode == EGTZeitrelaisLink.MODE_OFF)
		    key = "Coil NAME is off-delay (release) — contact will be aligned";
		else
		    key = "Coil NAME is on-delay (operate) — contact will be aligned";
		Window.alert(Locale.LS(key).replace("NAME", des));
	    }
	    if (CircuitElm.app != null)
		CircuitElm.app.needAnalyze();
	}
	if (n == 2) {
	    if (!skipTimeWrite) {
		delay = ei.value;
		if (delay < 0)
		    delay = 0;
	    }
	}
	if (n == 3) {
	    if (!skipTimeWrite) {
		delayOff = ei.value;
		if (delayOff < 0)
		    delayOff = 0;
	    }
	    skipTimeWrite = false;
	}
	if (n == 4) {
	    nom_pow = ei.value;
	    updateResistance();
	}
	if (n == 5) {
	    nom_v = ei.value;
	    updateResistance();
	}
    }
}

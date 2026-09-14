/*
    TAR-Dervic EGT: Zeitrelais-Kontakt Schließer/Öffner (S303), Dump 465
*/

package com.lushprojects.circuitjs1.client;

import java.util.Vector;

import com.google.gwt.user.client.Window;
import com.lushprojects.circuitjs1.client.util.Locale;

/**
 * Getrennter Kontakt über Bezeichnung. FLAG_NC = Öffner; Fallschirm nach Link-Modus.
 */
class EGTZeitrelaisKontaktElm extends ChipElm implements EGTDesignatable {
    static final int FLAG_NC = 2;
    /** Ausschaltverzögert (Fallschirm). Beim OK an Spule gleicher Bezeichnung angeglichen. */
    static final int FLAG_OFF_DELAY = 4;
    static final int FLAG_BLINK = 8;
    static final int FLAG_COMBINED = 16;
    /** Gleich wie Schütz-Hilfskontakt / Stromstoß-Kontakt. */
    static final int DEF_SIZE_X = 3;
    static final int DEF_SIZE_Y = 4;
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
    double switchCurrent, switchCurCount;

    public EGTZeitrelaisKontaktElm(int xx, int yy) {
	super(xx, yy);
	flags &= ~EGTStyle.FLAG_UEBERSICHT;
	setSize(1);
	sizeX = DEF_SIZE_X;
	sizeY = DEF_SIZE_Y;
	layoutPins();
	syncEndpoints();
    }

    public EGTZeitrelaisKontaktElm(int xa, int ya, int xb, int yb, int f,
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
	    } catch (Exception e) {
		designation = CustomLogicModel.unescape(t);
	    }
	}
	sizeX = DEF_SIZE_X;
	sizeY = DEF_SIZE_Y;
	layoutPins();
	syncEndpoints();
    
	note = EGTNote.readOptional(st);
}

    boolean isDigitalChip() { return false; }
    boolean isNC() { return (flags & FLAG_NC) != 0; }
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
	flags &= ~(FLAG_OFF_DELAY | FLAG_BLINK | FLAG_COMBINED);
	if (mode == EGTZeitrelaisLink.MODE_BLINK)
	    flags |= FLAG_BLINK;
	else if (mode == EGTZeitrelaisLink.MODE_COMBINED)
	    flags |= FLAG_COMBINED;
	else if (mode == EGTZeitrelaisLink.MODE_OFF)
	    flags |= FLAG_OFF_DELAY;
	setPoints();
    }

    void alignDelayFlag(boolean off) {
	alignDelayMode(off ? EGTZeitrelaisLink.MODE_OFF
			   : EGTZeitrelaisLink.MODE_ON);
    }

    boolean nonLinear() { return true; }

    boolean timedOn() { return EGTZeitrelaisLink.isTimedOn(designation); }

    String getChipName() {
	if (isBlink())
	    return "EGT-Zeitrelais-Kontakt (Blinkend)";
	if (isCombined())
	    return "EGT-Zeitrelais-Kontakt (Ansprech/Rückfall)";
	return "EGT-Zeitrelais-Kontakt (Ein/Aus)";
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

    boolean getConnection(int n1, int n2) { return false; }

    boolean getMatrixConnection(int n1, int n2) {
	return EGTSchuetzLink.pinPair(n1, n2, 0, 1);
    }

    void stamp() {
	sim.stampNonLinear(nodes[0]);
	sim.stampNonLinear(nodes[1]);
    }

    boolean contactClosed() {
	return isNC() ? !timedOn() : timedOn();
    }

    void startIteration() {}
    void execute() {}

    void doStep() {
	EGTStyle.stampPotentialFreeContact(sim, this, nodes[0], nodes[1],
		contactClosed(), EGTZeitrelaisLink.R_ON);
    }

    void calculateCurrent() {
	EGTStyle.clearPinCurrents(pins);
	switchCurrent = 0;
	current = 0;
	if (!contactClosed())
	    return;
	switchCurrent = EGTStyle.potentialFreeContactCurrent(this,
		nodes[0], nodes[1], volts[0], volts[1],
		EGTZeitrelaisLink.R_ON);
	current = switchCurrent;
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
	    // Gleiche Stem-Höhe wie Stromstoß-/Hilfskontakt
	    int topY = top + Math.max(10, (bottom - top) * 22 / 100);
	    int botY = bottom - Math.max(10, (bottom - top) * 22 / 100);
	    int cy = (topY + botY) / 2;
	    // Fachkunde-Raster: Klemmenabstand = 5 Rasterfelder.
	    int u = Math.max(10, (botY - topY) / 5);
	    int axisX = pins[0].post.x;
	    int cx = axisX - (int) Math.round(0.75 * u);
	    boolean closed = contactClosed();
	    boolean offDelay = isOffDelay();
	    boolean blink = isBlink();

	    g.setColor(stroke);
	    g.setLineWidth(2.5);
	    g.context.setLineCap("butt");
	    g.drawLine(axisX, pins[0].post.y, axisX, topY);
	    g.drawLine(axisX, botY, axisX, pins[1].post.y);
	    EGTStyle.drawZeitContact(g, cx, cy, u, isNC(), closed, offDelay,
				     blink, isCombined(), stroke);

	    // Wie bei den übrigen EGT-Kontakten: Klemmen horizontal am Post.
	    EGTStyle.drawPinLabelBeside(g, this, pins[0].post.x,
					pins[0].post.y, "7", whiteColor);
	    EGTStyle.drawPinLabelBeside(g, this, pins[1].post.x,
					pins[1].post.y, "8", whiteColor);

	    int[] ab = EGTStyle.drawHousingAnno(g, this, left, top, right,
		    bottom, whiteColor, designation,
		    blink ? "Blinken"
			  : (isCombined() ? "Ansprech + Rückfall"
					  : (offDelay ? "Aus" : "Ein")),
		    EGTZeitrelaisLink.timerLabel(designation));
	    adjustBbox(Math.min(ab[0], cx - 2 * u) - 4, ab[1], ab[2], ab[3]);
	    g.setLineWidth(1.0);

	    if (closed && Math.abs(switchCurrent) >= EGTStyle.I_DOT_MIN) {
		switchCurCount = updateDotCount(switchCurrent, switchCurCount);
		drawDots(g, pins[0].post, new Point(axisX, topY),
			 switchCurCount);
		drawDots(g, new Point(axisX, botY), pins[1].post,
			 switchCurCount);
	    }
	    drawPosts(g);
	} finally {
	    g.restore();
	}
    }

    String dump() {
	return super.dump() + " " + sizeX + " " + sizeY + " "
		+ CustomLogicModel.escape(designation) + EGTNote.dumpSuffix(note);
    }

    void dumpXml(com.google.gwt.xml.client.Document doc,
		 com.google.gwt.xml.client.Element elem) {
	super.dumpXml(doc, elem);
	XMLSerializer.dumpAttr(elem, "sx", sizeX);
	XMLSerializer.dumpAttr(elem, "sy", sizeY);
	XMLSerializer.dumpAttr(elem, "des", designation);
	EGTNote.dumpXml(elem, note);
    }

    void undumpXml(XMLDeserializer xml) {
	super.undumpXml(xml);
	flags &= ~EGTStyle.FLAG_UEBERSICHT;
	designation = xml.parseStringAttr("des", designation);
	note = EGTNote.undumpXml(xml, note);
	sizeX = DEF_SIZE_X;
	sizeY = DEF_SIZE_Y;
	setSize(1);
	layoutPins();
	syncEndpoints();
    }

    int getDumpType() { return 465; }
    String getXmlDumpType() { return "EGTZeitrelaisKontakt"; }

    void getInfo(String arr[]) {
	arr[0] = getChipName() + " (" + designation + ")";
	arr[1] = contactClosed() ? "geschlossen" : "offen";
	arr[2] = EGTZeitrelaisLink.statusText(designation);
	String tl = EGTZeitrelaisLink.timerLabel(designation);
	if (tl != null)
	    arr[3] = tl;
	else
	    arr[3] = "V = " + getVoltageText(volts[0] - volts[1]);
	arr[4] = "I = " + getCurrentText(switchCurrent);
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
	    return EGTStyle.kontaktartInfo(isNC());
	return null;
    }

    public void setChipEditValue(int n, EditInfo ei) {
	if (n == 0)
	    designation = ei.textf.getText();
	if (n == 1) {
	    int mode = ei.choice.getSelectedIndex();
	    if (mode < 0 || mode > 3)
		mode = 0;
	    alignDelayMode(mode);
	    EGTZeitrelaisSpuleElm coil =
		    EGTZeitrelaisLink.findCoil(designation);
	    if (coil != null && coil.delayMode() != mode) {
		String des = EGTZeitrelaisLink.normalize(designation);
		String key;
		if (mode == EGTZeitrelaisLink.MODE_BLINK)
		    key = "Contact NAME is flashing (blink) — coil will be aligned";
		else if (mode == EGTZeitrelaisLink.MODE_COMBINED)
		    key = "Contact NAME is combined-delay — coil will be aligned";
		else if (mode == EGTZeitrelaisLink.MODE_OFF)
		    key = "Contact NAME is off-delay (release) — coil will be aligned";
		else
		    key = "Contact NAME is on-delay (operate) — coil will be aligned";
		Window.alert(Locale.LS(key).replace("NAME", des));
		coil.alignDelayMode(mode);
		Vector ks = EGTZeitrelaisLink.findContacts(designation);
		for (int i = 0; i < ks.size(); i++) {
		    EGTZeitrelaisKontaktElm k =
			    (EGTZeitrelaisKontaktElm) ks.elementAt(i);
		    if (k != this && k.delayMode() != mode)
			k.alignDelayMode(mode);
		}
		if (CircuitElm.app != null)
		    CircuitElm.app.needAnalyze();
	    }
	}
	if (n == 2) {
	    flags = EGTStyle.applyKontaktart(flags, FLAG_NC, ei);
	    setPoints();
	}
    }
}

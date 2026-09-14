/*
    TAR-Dervic EGT: FUP-Operanden I1…I8 / Q1…Q8 (LOGO-Adressen), Dump 467 / 468
    Optik: weißes Quadrat, I/Q innen, Adresse oben — Fachkunde / Soft Comfort.
*/

package com.lushprojects.circuitjs1.client;

import java.util.Vector;

import com.google.gwt.xml.client.Document;
import com.google.gwt.xml.client.Element;
import com.lushprojects.circuitjs1.client.util.Locale;

/**
 * Brücke Mini-SPS-Klemme ↔ FUP. Keine 230-V-Leitung — Logikpegel.
 * FUP-I: Thevenin/Wired-OR, keine ideale Quelle 0/5 V gegen Masse
 * (sonst Kurzschluss wenn mehrere I auf ein Q). Siehe Handoff 2026-08-13.
 */
abstract class EGTFupOperandElm extends ChipElm {
    static final int DEF_SIZE = 2;
    static final int MAX_CH = 8;
    /** Sichtbarer FUP-Anschluss zwischen Kasten und elektrischem Post. */
    static final int FUP_LEAD = 16;

    static final Color COL_HI = new Color(0x2e, 0xcc, 0x71);

    int channel = 1;
    /** FUP-I: Klick-Zwang 1, ODER mit Mini-SPS-Klemme. */
    boolean forced;
    /** FUP-I: true = Taster (tastend), false = Schalter (rastend). */
    boolean momentary;
    /** Mini-SPS-Kennzeichen (A1). Leer = erste SPS. Zusatztext nur bei ≥2 SPS. */
    String spsDes = "";

    EGTFupOperandElm(int xx, int yy) {
	super(xx, yy);
	setSize(1);
	sizeX = DEF_SIZE;
	sizeY = DEF_SIZE;
	setupPins();
	syncEndpoints();
	spsDes = EGTMiniSpsLink.nearestDes(xx, yy);
	channel = nextFreeChannel();
    }

    EGTFupOperandElm(int xa, int ya, int xb, int yb, int f,
		     StringTokenizer st) {
	super(xa, ya, xb, yb, f, st);
	setSize(1);
	if (st.hasMoreTokens()) {
	    try {
		channel = Integer.parseInt(st.nextToken());
		if (st.hasMoreTokens())
		    forced = Integer.parseInt(st.nextToken()) != 0;
		if (st.hasMoreTokens())
		    momentary = Integer.parseInt(st.nextToken()) != 0;
		if (st.hasMoreTokens())
		    spsDes = CustomLogicModel.unescape(st.nextToken());
	    } catch (Exception e) {
	    }
	}
	clampChannel();
	sizeX = DEF_SIZE;
	sizeY = DEF_SIZE;
	setupPins();
	syncEndpoints();
    }

    abstract boolean isInputOperand();

    void clampChannel() {
	if (channel < 1)
	    channel = 1;
	if (channel > MAX_CH)
	    channel = MAX_CH;
    }

    /** Nächste freie I-/Q-Adresse 1…8 derselben Mini-SPS. Dump-/XML-Ctor nicht. */
    int nextFreeChannel() {
	boolean used[] = new boolean[MAX_CH + 1];
	String selfTag = spsDes == null ? "" : spsDes.trim();
	Vector<CircuitElm> list = null;
	if (app != null)
	    list = app.elmList;
	else if (sim != null)
	    list = sim.elmList;
	if (list != null) {
	    for (int i = 0; i < list.size(); i++) {
		CircuitElm ce = list.elementAt(i);
		if (!(ce instanceof EGTFupOperandElm) || ce == this)
		    continue;
		EGTFupOperandElm op = (EGTFupOperandElm) ce;
		if (op.isInputOperand() != isInputOperand())
		    continue;
		String tag = op.spsDes == null ? "" : op.spsDes.trim();
		if (!tag.equals(selfTag))
		    continue;
		int ch = op.channel;
		if (ch >= 1 && ch <= MAX_CH)
		    used[ch] = true;
	    }
	}
	for (int ch = 1; ch <= MAX_CH; ch++)
	    if (!used[ch])
		return ch;
	return MAX_CH;
    }

    String addrText() {
	return (isInputOperand() ? "I" : "Q") + channel;
    }

    boolean useSmallGrid() { return true; }
    boolean isDigitalChip() { return true; }

    String getChipName() {
	return isInputOperand()
		? Locale.LS("EGT-FUP-Eingang")
		: Locale.LS("EGT-FUP-Ausgang");
    }

    int getPostCount() { return 1; }
    /** Keine starre Quelle gegen Masse — sonst Kurzschluss, wenn mehrere I auf ein Q gehen. */
    int getVoltageSourceCount() { return 0; }
    boolean nonLinear() { return isInputOperand(); }

    void stamp() {
	if (nodes == null || nodes[0] == null)
	    return;
	if (isInputOperand())
	    sim.stampNonLinear(nodes[0]);
	else
	    /* Unverdrahtetes FUP-Q sonst schwebend: logicHigh kann 1 lesen
	       und den Mini-SPS-Schließer ohne Programm schließen. */
	    sim.stampResistor(nodes[0], CircuitNode.ground, 1e6);
    }

    void setupPins() {
	sizeX = DEF_SIZE;
	sizeY = DEF_SIZE;
	pins = new Pin[1];
	if (isInputOperand()) {
	    pins[0] = new Pin(0, SIDE_E, "");
	    pins[0].output = true;
	    pins[0].state = false;
	} else {
	    pins[0] = new Pin(0, SIDE_W, "");
	    pins[0].output = false;
	    pins[0].state = false;
	}
    }

    void syncEndpoints() {
	if (cspc2 < 1)
	    setSize(1);
	x2 = x + sizeX * cspc2;
	y2 = y + sizeY * cspc2;
	setPoints();
    }

    void drag(int xx, int yy) {
	xx = snapGrid(xx);
	yy = snapGrid(yy);
	if (cspc2 < 1)
	    setSize(1);
	sizeX = DEF_SIZE;
	sizeY = DEF_SIZE;
	syncEndpoints();
    }

    void draggingDone() {
	syncEndpoints();
    }

    /**
     * Ein-Post-Chip ist kein Draht: sonst verschiebt splitAt beim Platzieren
     * eines zweiten Operanden auf derselben Reihe den bestehenden (Q „verschwindet“).
     */
    int getLeadPost(int px, int py) {
	return -1;
    }

    boolean creationFailed() {
	return sizeX < DEF_SIZE || sizeY < DEF_SIZE;
    }

    void setPoints() {
	super.setPoints();
	if (pins == null || cspc2 < 1)
	    return;
	int w = sizeX * cspc2;
	int h = sizeY * cspc2;
	int left = x;
	int top = y;
	/*
	 * Der elektrische Post bleibt an seiner bisherigen Koordinate. Damit
	 * bleiben auch alte gespeicherte FUP-Schaltungen verdrahtet. Nur der
	 * gezeichnete Kasten rückt um den Anschlussstutzen nach innen.
	 */
	int lead = Math.max(FUP_LEAD, cspc2);
	int visualLeft = left + (isInputOperand() ? -lead : lead);
	int visualRight = left + w + (isInputOperand() ? -lead : lead);
	rectPointsX = new int[] { visualLeft, visualRight, visualRight, visualLeft };
	rectPointsY = new int[] { top, top, top + h, top + h };
	int cy = snapGrid(top + h / 2);
	int px = isInputOperand() ? snapGrid(left + w) : snapGrid(left);
	pins[0].stub = new Point(isInputOperand() ? visualRight : visualLeft, cy);
	pins[0].post = new Point(px, cy);
	setBbox(Math.min(visualLeft, px) - 4, top - Math.max(14, 10 * csize),
		Math.max(visualRight, px) + 4, top + h + 4);
    }

    boolean logicHigh() {
	return volts[0] > getThreshold();
    }

    boolean iHigh() {
	return forced || EGTMiniSpsLink.inputHigh(channel, spsDes);
    }

    void clickDown() {
	if (!isInputOperand())
	    return;
	if (momentary)
	    forced = true;
	else
	    forced = !forced;
    }

    void clickUp() {
	if (!isInputOperand() || !momentary)
	    return;
	forced = false;
    }

    boolean hitBody(int mx, int my) {
	if (rectPointsX == null)
	    return false;
	int xr = rectPointsX[0];
	int yr = rectPointsY[0];
	int xs = rectPointsX[2] - xr;
	int ys = rectPointsY[2] - yr;
	return mx >= xr && mx <= xr + xs && my >= yr && my <= yr + ys;
    }

    void startIteration() {
	EGTMiniSpsLink.refresh(sim);
	if (isInputOperand())
	    pins[0].value = iHigh();
	else
	    pins[0].value = volts[0] > getThreshold();
    }

    void execute() {}

    void doStep() {
	if (!isInputOperand() || nodes == null || nodes[0] == null)
	    return;
	boolean hi = iHigh();
	pins[0].value = hi;
	// Thevenin: High 10 Ohm auf 5 V, Low 1 MOhm auf 0 V. Keine VoltageSource.
	double r = hi ? 10 : 1e6;
	double v = hi ? highVoltage : 0;
	sim.stampResistor(nodes[0], CircuitNode.ground, r);
	sim.stampCurrentSource(CircuitNode.ground, nodes[0], v / r);
    }

    void drawChip(Graphics g) {
	g.save();
	try {
	    if (rectPointsX == null)
		setPoints();
	    int xr = rectPointsX[0];
	    int yr = rectPointsY[0];
	    int xs = rectPointsX[2] - xr;
	    int ys = rectPointsY[2] - yr;
	    int cx = xr + xs / 2;
	    int cy = yr + ys / 2;

	    if (pins[0].stub.x != pins[0].post.x
		    || pins[0].stub.y != pins[0].post.y) {
		boolean hi = isInputOperand() ? iHigh() : logicHigh();
		g.setColor(hi ? COL_HI : Color.gray);
		drawThickLine(g, pins[0].stub, pins[0].post);
	    }

	    boolean hi = isInputOperand() ? iHigh() : logicHigh();
	    g.setColor(Color.white);
	    g.fillRect(xr, yr, xs, ys);
	    Color edge = needsHighlight() ? selectColor
		    : (hi ? COL_HI : Color.black);
	    g.setColor(edge);
	    g.setLineWidth(hi ? 2.2 : 1.6);
	    g.drawRect(xr, yr, xs, ys);

	    String letter = isInputOperand() ? "I" : "Q";
	    int fs = Math.max(12, 11 * csize);
	    g.setFont(new Font("bold", 0, fs));
	    g.setColor(hi ? COL_HI : Color.black);
	    int tw = (int) g.context.measureText(letter).getWidth();
	    g.drawString(letter, cx - tw / 2, cy + fs / 3);

	    String addr = addrText();
	    String extra = EGTMiniSpsLink.extraLabel(spsDes);
	    int afs = Math.max(10, 8 * csize);
	    g.setFont(new Font("normal", 0, afs));
	    g.setColor(needsHighlight() ? selectColor : whiteColor);
	    int atw = (int) g.context.measureText(addr).getWidth();
	    g.drawString(addr, cx - atw / 2, yr - 4);
	    if (extra.length() > 0) {
		g.setFont(EGTStyle.pinLabelFont());
		int etw = (int) g.context.measureText(extra).getWidth();
		g.drawString(extra, cx - etw / 2, yr - 4 - afs - 2);
		adjustBbox(xr - 4, yr - 4 - afs - 16, xr + xs + 4, yr + ys + 4);
	    }

	    drawPost(g, pins[0].post);
	    g.setLineWidth(1.0);
	} finally {
	    g.restore();
	}
    }

    String dump() {
	String d = super.dump() + " " + channel + " " + (forced ? 1 : 0)
		+ " " + (momentary ? 1 : 0);
	if (spsDes != null && spsDes.trim().length() > 0)
	    d += " " + CustomLogicModel.escape(spsDes.trim());
	return d;
    }

    void dumpXml(Document doc, Element elem) {
	super.dumpXml(doc, elem);
	XMLSerializer.dumpAttr(elem, "ch", channel);
	if (forced)
	    XMLSerializer.dumpAttr(elem, "frc", 1);
	if (momentary)
	    XMLSerializer.dumpAttr(elem, "tas", 1);
	if (spsDes != null && spsDes.length() > 0)
	    XMLSerializer.dumpAttr(elem, "sps", spsDes);
    }

    void undumpXml(XMLDeserializer xml) {
	super.undumpXml(xml);
	channel = xml.parseIntAttr("ch", 1);
	forced = xml.parseIntAttr("frc", 0) != 0;
	momentary = xml.parseIntAttr("tas", 0) != 0;
	spsDes = xml.parseStringAttr("sps", spsDes);
	clampChannel();
	setSize(1);
	sizeX = DEF_SIZE;
	sizeY = DEF_SIZE;
	setupPins();
	allocNodes();
	syncEndpoints();
    }

    void getInfo(String arr[]) {
	arr[0] = getChipName() + " " + addrText();
	String extra = EGTMiniSpsLink.extraLabel(spsDes);
	if (extra.length() > 0)
	    arr[0] += " (" + extra + ")";
	if (isInputOperand()) {
	    arr[1] = iHigh() ? "1" : "0";
	    if (forced)
		arr[1] += momentary ? " (Taster)" : " (Schalter)";
	    else if (EGTMiniSpsLink.inputHigh(channel, spsDes))
		arr[1] += " (Klemme)";
	} else {
	    arr[1] = pins[0].value ? "1" : "0";
	}
	arr[2] = "V = " + getVoltageText(volts[0]);
    }

    /** Kein „High-Pegel“-Feld — FUP bleibt 5 V wie die Gatter. */
    public EditInfo getEditInfo(int n) {
	return getChipEditInfo(n);
    }

    public void setEditValue(int n, EditInfo ei) {
	setChipEditValue(n, ei);
    }

    public EditInfo getChipEditInfo(int n) {
	if (n == 0) {
	    EditInfo ei = new EditInfo("Adresse", 0, -1, -1);
	    ei.choice = new Choice();
	    String p = isInputOperand() ? "I" : "Q";
	    for (int i = 1; i <= MAX_CH; i++)
		ei.choice.add(p + i);
	    ei.choice.select(channel - 1);
	    return ei;
	}
	if (n == 1 && isInputOperand()) {
	    EditInfo ei = new EditInfo(Locale.LS("Klick"), 0, -1, -1);
	    ei.choice = new Choice();
	    ei.choice.add(Locale.LS("Schalter (rastend)"));
	    ei.choice.add(Locale.LS("Taster (tastend)"));
	    ei.choice.select(momentary ? 1 : 0);
	    return ei;
	}
	int spsN = isInputOperand() ? 2 : 1;
	if (n == spsN && EGTMiniSpsLink.multiSps()) {
	    Vector<String> names = EGTMiniSpsLink.spsNames();
	    if (names.size() < 2)
		return null;
	    EditInfo ei = new EditInfo("Mini-SPS", 0, -1, -1);
	    ei.choice = new Choice();
	    int sel = 0;
	    String cur = spsDes == null ? "" : spsDes.trim();
	    if (cur.length() == 0 && names.size() > 0)
		cur = names.elementAt(0);
	    for (int i = 0; i < names.size(); i++) {
		ei.choice.add(names.elementAt(i));
		if (names.elementAt(i).equals(cur))
		    sel = i;
	    }
	    ei.choice.select(sel);
	    return ei;
	}
	return null;
    }

    public void setChipEditValue(int n, EditInfo ei) {
	if (n == 0 && ei.choice != null) {
	    channel = ei.choice.getSelectedIndex() + 1;
	    clampChannel();
	}
	if (n == 1 && isInputOperand() && ei.choice != null)
	    momentary = ei.choice.getSelectedIndex() == 1;
	int spsN = isInputOperand() ? 2 : 1;
	if (n == spsN && ei.choice != null && EGTMiniSpsLink.multiSps()) {
	    Vector<String> names = EGTMiniSpsLink.spsNames();
	    int i = ei.choice.getSelectedIndex();
	    if (i >= 0 && i < names.size())
		spsDes = names.elementAt(i);
	}
    }
}

class EGTFupEingangElm extends EGTFupOperandElm {
    public EGTFupEingangElm(int xx, int yy) { super(xx, yy); }
    public EGTFupEingangElm(int xa, int ya, int xb, int yb, int f,
			    StringTokenizer st) {
	super(xa, ya, xb, yb, f, st);
    }
    boolean isInputOperand() { return true; }
    int getDumpType() { return 467; }
    String getXmlDumpType() { return "EGTFupEingang"; }
}

class EGTFupAusgangElm extends EGTFupOperandElm {
    public EGTFupAusgangElm(int xx, int yy) { super(xx, yy); }
    public EGTFupAusgangElm(int xa, int ya, int xb, int yb, int f,
			    StringTokenizer st) {
	super(xa, ya, xb, yb, f, st);
    }
    boolean isInputOperand() { return false; }
    int getDumpType() { return 468; }
    String getXmlDumpType() { return "EGTFupAusgang"; }
}

/*
    TAR-Dervic EGT: IEC-FUP-Zeitbausteine TON / TOF, L403 / LOGIK-09
*/

package com.lushprojects.circuitjs1.client;

import com.google.gwt.xml.client.Document;
import com.google.gwt.xml.client.Element;
import com.lushprojects.circuitjs1.client.util.Locale;

/**
 * Gemeinsame Basis der FUP-Zeitbausteine. Die Zeitbasis ist bewusst die
 * Wanduhr bei laufender Simulation, wie bei den EGT-Zeitrelais; sie ist aber
 * nicht mit deren Spulen-/Kontaktkopplung verbunden.
 */
abstract class EGTFupZeitElm extends ChipElm {
    static final int DEF_SX = 4;
    static final int DEF_SY = 4;
    static final double DEF_DELAY = 1;
    static final double MAX_WALL_DT = 0.25;
    static final Color COL_HI = new Color(0x2e, 0xcc, 0x71);
    static final Color COL_LO = new Color(0x88, 0x88, 0x88);

    double delay = DEF_DELAY;
    double elapsed;
    long lastWallMs;

    EGTFupZeitElm(int xx, int yy) {
	super(xx, yy);
	initGeometry();
    }

    EGTFupZeitElm(int xa, int ya, int xb, int yb, int f,
		   StringTokenizer st) {
	super(xa, ya, xb, yb, f, st);
	if (st.hasMoreTokens()) {
	    try {
		delay = Double.parseDouble(st.nextToken());
	    } catch (Exception e) {
	    }
	}
	clampDelay();
	initGeometry();
    }

    abstract boolean isOnDelay();

    void initGeometry() {
	setSize(1);
	sizeX = DEF_SX;
	sizeY = DEF_SY;
	setupPins();
	syncEndpoints();
    }

    void clampDelay() {
	if (delay < 0)
	    delay = 0;
    }

    boolean useSmallGrid() { return true; }
    boolean isDigitalChip() { return true; }

    String getChipName() {
	return isOnDelay() ? Locale.LS("TON (FUP)") : Locale.LS("TOF (FUP)");
    }

    int getPostCount() { return 2; }
    int getVoltageSourceCount() { return 1; }

    void setupPins() {
	pins = new Pin[2];
        // I/Q liegen mittig auf der jeweiligen Seite des IEC-Blocks.
        pins[0] = new Pin(1, SIDE_W, "I");
        pins[1] = new Pin(1, SIDE_E, "Q");
	pins[1].output = pins[1].state = true;
    }

    void syncEndpoints() {
	if (cspc2 < 1)
	    setSize(1);
	x2 = x + sizeX * cspc2;
	y2 = y + sizeY * cspc2;
	setPoints();
    }

    /**
     * IEC-Kasten nach innen, I/Q als Stutzen. Posts bleiben auf der
     * ChipElm-Rasterzeile von Pin(1), sonst liegen 16-Raster-Leitungen
     * 8 px neben dem Anschluss.
     */
    void setPoints() {
	super.setPoints();
	if (pins == null || rectPointsX == null || rectPointsY == null)
	    return;
	int lead = Math.max(8, cspc);
	int left = rectPointsX[0] + lead;
	int right = rectPointsX[2] - lead;
	if (right <= left)
	    return;
	rectPointsX = new int[] { left, right, right, left };
	for (int i = 0; i < pins.length; i++) {
	    Pin p = pins[i];
	    if (p.side == SIDE_W)
		p.stub = new Point(left, p.post.y);
	    else if (p.side == SIDE_E)
		p.stub = new Point(right, p.post.y);
	}
	setBbox(Math.min(left, pins[0].post.x) - 4, rectPointsY[0] - 4,
		Math.max(right, pins[1].post.x) + 4, rectPointsY[2] + 4);
    }

    boolean timingActive() {
	if (pins == null || pins.length < 2)
	    return false;
	return isOnDelay()
		? pins[0].value && !pins[1].value
		: !pins[0].value && pins[1].value;
    }

    /** Wie Zeitrelais-Kontakt: Ablauf nur während der Verzögerung. */
    String timerLabel() {
	if (!timingActive())
	    return null;
	return CircuitElm.getUnitText(elapsed, "s") + " / "
		+ CircuitElm.getUnitText(delay, "s");
    }

    void drag(int xx, int yy) {
	syncEndpoints();
    }

    void draggingDone() {
	syncEndpoints();
    }

    void reset() {
	super.reset();
	elapsed = 0;
	lastWallMs = 0;
	if (pins != null && pins.length > 1) {
	    pins[1].value = false;
	    volts[1] = 0;
	}
    }

    /** Nur die bei laufender Simulation vergangene Wanduhrzeit aufsummieren. */
    void addWallTime(boolean timing) {
	long now = System.currentTimeMillis();
	boolean running = CircuitElm.app != null && CircuitElm.app.simIsRunning();
	if (timing && running && lastWallMs != 0) {
	    double dt = (now - lastWallMs) / 1000.0;
	    if (dt < 0)
		dt = 0;
	    if (dt > MAX_WALL_DT)
		dt = MAX_WALL_DT;
	    elapsed += dt;
	}
	lastWallMs = now;
    }

    void execute() {
	boolean input = pins[0].value;
	boolean output = pins[1].value;
	if (isOnDelay()) {
	    if (!input) {
		elapsed = 0;
		output = false;
		addWallTime(false);
	    } else {
		addWallTime(!output);
		if (elapsed >= delay)
		    output = true;
	    }
	} else {
	    if (input) {
		elapsed = 0;
		output = true;
		addWallTime(false);
	    } else if (output) {
		addWallTime(true);
		if (elapsed >= delay)
		    output = false;
	    } else {
		elapsed = 0;
		addWallTime(false);
	    }
	}
	writeOutput(1, output);
    }

    /**
     * TON/TOF übertragen ausschließlich einen digitalen FUP-Zustand. Die
     * normale WireElm-Stromermittlung fragt diese Methode nur für ihre
     * gelben Strompunkte ab. Ein Rückgabewert von 0 blendet deshalb allein
     * diese missverständliche Stromanimation aus; Knoten, Spannung und
     * Solver-Verhalten bleiben unverändert.
     */
    double getCurrentIntoNode(int n) {
	return 0;
    }

    void drawChip(Graphics g) {
	g.save();
	int x0 = Math.min(rectPointsX[0], rectPointsX[2]);
	int y0 = Math.min(rectPointsY[0], rectPointsY[1]);
	int x1 = Math.max(rectPointsX[0], rectPointsX[2]);
	int y1 = Math.max(rectPointsY[0], rectPointsY[2]);
	int bw = Math.max(1, x1 - x0);
	int bh = Math.max(1, y1 - y0);

	for (int i = 0; i < pins.length; i++) {
	    Pin p = pins[i];
	    boolean hi = p.output ? p.value : volts[i] > getThreshold();
	    g.setColor(hi ? COL_HI : COL_LO);
	    drawThickLine(g, p.post, p.stub);
	}

	Color ink = needsHighlight() ? selectColor : whiteColor;
	g.setColor(ink);
	drawThickPolygon(g, rectPointsX, rectPointsY, 4);

	int infoSize = Math.max(8, Math.min(9, bh / 8));
	int waveTop = y0 + 4;
	int waveBot = y1 - infoSize - 4;
	drawLogoWaveform(g, x0 + 4, waveTop, x1 - 4, waveBot, isOnDelay(), ink);

	g.setColor(ink);
	g.setFont(new Font("normal", 0, infoSize));
	long tenths = Math.round(delay * 10);
	String delayText = "T=" + (tenths / 10) + "," + Math.abs(tenths % 10) + "s";
	int dw = (int) g.context.measureText(delayText).getWidth();
	g.drawString(delayText, (x0 + x1 - dw) / 2, y1 - 3);
	String tl = timerLabel();
	if (tl != null) {
	    g.setFont(EGTStyle.pinLabelFont());
	    int tw = (int) g.context.measureText(tl).getWidth();
	    int ty = y1 + 14;
	    g.drawString(tl, (x0 + x1 - tw) / 2, ty);
	    adjustBbox(Math.min(x0, pins[0].post.x) - 4, y0 - 4,
		       Math.max(x1, pins[1].post.x) + 4, ty + 6);
	}
	drawPosts(g);
	g.restore();
    }

    /**
     * LOGO-Zeitdiagramm ohne Füllung.
     * TON: oben langer Trigger, unten Startstrich dann verzögerter Impuls.
     * TOF: oben kurzer Trigger, unten langer Impuls, T-Strich an der Fallflanke.
     */
    void drawLogoWaveform(Graphics g, int left, int top, int right, int bot,
			  boolean onDelay, Color ink) {
	int w = right - left;
	int h = bot - top;
	if (w < 16 || h < 14)
	    return;
	int gap = Math.max(4, h / 7);
	int topHi = top + 1;
	int topLo = top + h * 42 / 100;
	int botHi = topLo + gap;
	int botLo = bot - 1;
	int tick = Math.max(4, (botLo - botHi) / 3);
	g.setColor(ink);
	g.setLineWidth(2.2);
	g.context.setLineCap("butt");
	g.context.setLineJoin("miter");
	if (onDelay) {
	    int inRise = left + w * 16 / 100;
	    int inFall = left + w * 84 / 100;
	    int delayedRise = left + w * 58 / 100;
	    int delayedFall = left + w * 74 / 100;
	    int down = 8;
	    drawPulse(g, left, topLo, inRise, topHi, inFall, right);
	    g.drawLine(inRise, topLo + down, inRise, topLo + tick + down);
	    g.context.beginPath();
	    g.context.moveTo(left, botLo);
	    g.context.lineTo(inRise, botLo);
	    g.context.lineTo(inRise, botLo - tick);
	    g.context.lineTo(inRise, botLo);
	    g.context.lineTo(delayedRise, botLo);
	    g.context.lineTo(delayedRise, botHi);
	    g.context.lineTo(delayedFall, botHi);
	    g.context.lineTo(delayedFall, botLo);
	    g.context.lineTo(right, botLo);
	    g.context.stroke();
	} else {
	    int trgRise = left + w * 18 / 100;
	    int trgFall = left + w * 36 / 100;
	    int qFall = left + w * 87 / 100;
	    int amp = botLo - botHi;
	    int hang = Math.max(4, amp * 45 / 100);
	    int up = Math.max(4, amp * 37 / 100);
	    if (hang + up + 2 > amp) {
		hang = amp / 2 - 1;
		up = amp / 2 - 1;
	    }
	    drawPulse(g, left, topLo, trgRise, topHi, trgFall, right);
	    drawPulse(g, left, botLo, trgRise, botHi, qFall, right);
	    g.drawLine(trgFall, botHi, trgFall, botHi + hang);
	    g.drawLine(trgFall, botLo - up, trgFall, botLo);
	}
	g.setLineWidth(1.0);
    }

    void drawPulse(Graphics g, int x0, int yLo, int xRise, int yHi, int xFall,
		   int x1) {
	g.context.beginPath();
	g.context.moveTo(x0, yLo);
	g.context.lineTo(xRise, yLo);
	g.context.lineTo(xRise, yHi);
	g.context.lineTo(xFall, yHi);
	g.context.lineTo(xFall, yLo);
	g.context.lineTo(x1, yLo);
	g.context.stroke();
    }

    String dump() {
	return super.dump() + " " + delay;
    }

    void dumpXml(Document doc, Element elem) {
	super.dumpXml(doc, elem);
	XMLSerializer.dumpAttr(elem, "delay", delay);
    }

    void undumpXml(XMLDeserializer xml) {
	super.undumpXml(xml);
	delay = xml.parseDoubleAttr("delay", delay);
	clampDelay();
	initGeometry();
    }

    public EditInfo getChipEditInfo(int n) {
	if (n == 0)
	    return new EditInfo(Locale.LS("Zeit T (s)"), delay, 0, 0).setNonNegative();
	return super.getChipEditInfo(n);
    }

    public void setChipEditValue(int n, EditInfo ei) {
	if (n == 0) {
	    delay = ei.value;
	    clampDelay();
	    elapsed = 0;
	    lastWallMs = 0;
	    return;
	}
	super.setChipEditValue(n, ei);
    }

    void getInfo(String arr[]) {
	arr[0] = getChipName();
	arr[1] = "I = " + (pins[0].value ? "1" : "0");
	arr[2] = "Q = " + (pins[1].value ? "1" : "0");
	arr[3] = "T = " + CircuitElm.getUnitText(delay, "s");
	String tl = timerLabel();
	if (tl != null)
	    arr[4] = tl;
    }
}

/** Einschaltverzögerung: I=1 startet T, anschließend Q=1. */
class EGTFupTonElm extends EGTFupZeitElm {
    public EGTFupTonElm(int xx, int yy) { super(xx, yy); }
    public EGTFupTonElm(int xa, int ya, int xb, int yb, int f,
			 StringTokenizer st) { super(xa, ya, xb, yb, f, st); }
    boolean isOnDelay() { return true; }
    int getDumpType() { return 476; }
    String getXmlDumpType() { return "EGTFupTon"; }
}

/** Ausschaltverzögerung: I=1 setzt Q sofort, nach I=0 läuft T ab. */
class EGTFupTofElm extends EGTFupZeitElm {
    public EGTFupTofElm(int xx, int yy) { super(xx, yy); }
    public EGTFupTofElm(int xa, int ya, int xb, int yb, int f,
			 StringTokenizer st) { super(xa, ya, xb, yb, f, st); }
    boolean isOnDelay() { return false; }
    int getDumpType() { return 477; }
    String getXmlDumpType() { return "EGTFupTof"; }
}

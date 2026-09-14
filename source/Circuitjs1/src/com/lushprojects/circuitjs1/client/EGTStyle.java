/*
    TAR-Dervic EGT extension: shared colours / PE zebra / overview symbol helpers
*/

package com.lushprojects.circuitjs1.client;

import com.lushprojects.circuitjs1.client.util.Locale;

/** Shared EGT drawing helpers (conductor colours, PE zebra, overview circle). */
class EGTStyle {
    static final Color COL_L1 = new Color(0x8B, 0x45, 0x13);
    static final Color COL_L2 = new Color(0x0a, 0x0a, 0x0a);
    static final Color COL_L3 = new Color(0x80, 0x80, 0x80);
    static final Color COL_N = new Color(0x1E, 0x90, 0xFF);
    static final Color COL_PE_G = new Color(0x22, 0x8B, 0x22);
    static final Color COL_PE_Y = new Color(0xFF, 0xD7, 0x00);
    /** Workbench Arbeitsblatt-Rot (L ohne Phasenfarbe). */
    static final Color COL_RED_SHEET = new Color(0xc0, 0x39, 0x2b);
    /** Gleichspannungsquelle Plus-Ader (Workbench #C41E3A). */
    static final Color COL_DC_PLUS = new Color(0xC4, 0x1E, 0x3A);
    /** Helle Kontur für dunkle Leiter (L2) auf schwarzem Canvas. */
    static final Color COL_OUTLINE = new Color(0xea, 0xea, 0xea);
    /** Wenn klassische Spannungsfarbe rot wäre → EGT-Leitung kurz so. */
    static final Color COL_LIVE_PULSE = Color.red;

    /** ChipElm FLAG_* uses bits 0 and 10–13; keep EGT view flag clear of those. */
    static final int FLAG_UEBERSICHT = 1 << 14;
    /** Taster: Edit-Option Leuchttaster (kein eigenes Element). */
    static final int FLAG_LEUCHT = 1 << 15;
    /** Taster: Öffner (Ruhe geschlossen, Drücken öffnet). Default = Schließer. */
    static final int FLAG_TASTER_NC = 1 << 18;
    /** Leuchte: Edit-Option Meldeleuchte (Ring, kein eigenes Element). */
    static final int FLAG_MELD = 1 << 16;
    /** Schützspule: L fest (sonst aus Nenn-U/S bei 50 Hz). */
    static final int FLAG_L_CUSTOM = 1 << 17;

    /** Klemmenbeschriftung — überall gleich, Vorbild Motor U1. */
    static final int PIN_LABEL_SIZE = 11;
    static final int PIN_LABEL_PAD = 5;
    /**
     * Offener Kontakt: Bezug gegen Masse, kein sichtbarer Strom
     * (Mini-SPS-Q, 230 V / 1e12 ≈ 0,2 nA).
     */
    static final double R_OPEN_REF = 1e12;
    /** Unter 0,1 mA: Open-Ref / Solver-Rauschen, kein Laststrom (Spule/Lampe). */
    static final double I_DOT_MIN = 1e-4;

    static void stampOpenRef(SimulationManager sim, CircuitNode n) {
	if (sim == null || n == null)
	    return;
	sim.stampResistor(n, CircuitNode.ground, R_OPEN_REF);
    }

    /** Anderes Element am selben Knoten (Leitung, Last) — nicht nur dieser Kontakt. */
    static boolean poleHasOtherElm(CircuitElm self, CircuitNode n) {
	if (self == null || n == null || n.links == null)
	    return false;
	for (int i = 0; i < n.links.size(); i++) {
	    CircuitNodeLink l = n.links.get(i);
	    if (l != null && l.elm != null && l.elm != self)
		return true;
	}
	return false;
    }

    static boolean bothPolesExternallyConnected(CircuitElm self,
						CircuitNode a, CircuitNode b) {
	return poleHasOtherElm(self, a) && poleHasOtherElm(self, b);
    }

    /**
     * Potenzialfreier Kontakt: {@code R_ON} nur im geschlossenen Lastkreis.
     * Offen: Open-Ref nur an <em>freien</em> Polen. Ein Pol an L plus Open-Ref
     * wäre L nach Masse und gelbe Würfel auf F3/S1 ohne Last.
     */
    static void stampPotentialFreeContact(SimulationManager sim,
	    CircuitElm self, CircuitNode a, CircuitNode b,
	    boolean closed, double rOn) {
	if (sim == null)
	    return;
	if (closed && rOn > 0
		&& bothPolesExternallyConnected(self, a, b)) {
	    sim.stampResistor(a, b, rOn);
	    return;
	}
	if (!poleHasOtherElm(self, a))
	    stampOpenRef(sim, a);
	if (!poleHasOtherElm(self, b))
	    stampOpenRef(sim, b);
    }

    /** Anzeigestrom: 0 ohne Lastkreis (sonst (V)/R_ON = Geisterstrom). */
    static double potentialFreeContactCurrent(CircuitElm self,
	    CircuitNode a, CircuitNode b, double va, double vb, double rOn) {
	if (rOn <= 0 || !bothPolesExternallyConnected(self, a, b))
	    return 0;
	double i = (va - vb) / rOn;
	if (Math.abs(i) < I_DOT_MIN)
	    return 0;
	return i;
    }

    static Font pinLabelFont() {
	return new Font("normal", 0, PIN_LABEL_SIZE);
    }

    /**
     * Text rechts neben dem Post, vertikal mittig (Leitung überdeckt ihn nicht).
     * {@code right=false}: links vom Post (Klemmen nach Osten).
     */
    static void drawPinLabelBeside(Graphics g, CircuitElm elm,
				   int postX, int postY, String text,
				   Color color) {
	drawPinLabelBeside(g, elm, postX, postY, text, color, true);
    }

    /** Klemmentext neben der Zuleitung, Richtung Motormitte, nicht auf der Ader. */
    static void drawPinLabelToward(Graphics g, CircuitElm elm,
				   int postX, int postY, int towardX,
				   String text, Color color) {
	drawPinLabelBeside(g, elm, postX, postY - 9, text, color,
			   towardX > postX);
    }

    static void drawPinLabelBeside(Graphics g, CircuitElm elm,
				   int postX, int postY, String text,
				   Color color, boolean right) {
	if (text == null || text.length() == 0)
	    return;
	g.setFont(pinLabelFont());
	g.setColor(color);
	g.context.setTextAlign("left");
	g.context.setTextBaseline("middle");
	int tw = (int) g.context.measureText(text).getWidth();
	int lx = right ? postX + PIN_LABEL_PAD : postX - PIN_LABEL_PAD - tw;
	int ly = postY;
	g.drawString(text, lx, ly);
	g.context.setTextBaseline("alphabetic");
	if (elm != null)
	    elm.adjustBbox(lx, ly - 7, lx + tw, ly + 7);
    }

    /** Options „EGT-Leitungspuls“ — nur EGT-*Elm / EGTStyle, nicht getVoltageColor. */
    static boolean egtLivePulseEnabled() {
	return CircuitElm.app != null && CircuitElm.app.menus != null
		&& CircuitElm.app.menus.egtLivePulseCheckItem != null
		&& CircuitElm.app.menus.egtLivePulseCheckItem.getState();
    }

    static boolean voltageDisplayOn() {
	return CircuitElm.app != null && CircuitElm.app.menus != null
		&& CircuitElm.app.menus.voltsCheckItem.getState();
    }

    /**
     * true nur wenn Upstream-colorScale für diesen Momentanspannungswert
     * sichtbar rot wäre (positiv). Bei Grün/Grau/Negativ → false.
     */
    static boolean isClassicVoltageRed(double volts) {
	if (Double.isNaN(volts))
	    volts = 0;
	double vr = CircuitElm.voltageRange;
	if (vr <= 0)
	    vr = 5;
	int n = CircuitElm.colorScaleCount;
	Color[] scale = CircuitElm.colorScale;
	if (scale == null || n < 2)
	    return volts > 1;
	int c = (int) ((volts + vr) * (n - 1) / (vr * 2));
	if (c < 0)
	    c = 0;
	if (c >= n)
	    c = n - 1;
	Color col = scale[c];
	int r = col.getRed(), g = col.getGreen(), b = col.getBlue();
	return r >= 160 && r > g + 30 && r > b + 30;
    }

    /** DIN-Farbe behalten; nur bei klassischem Rot → Puls-Rot. */
    static Color liveOrBase(Color base, double voltsInstant) {
	if (!egtLivePulseEnabled() || !voltageDisplayOn())
	    return base;
	if (isClassicVoltageRed(voltsInstant))
	    return COL_LIVE_PULSE;
	return base;
    }

    /** Leiter oder PE-Zebra; Rot nur wenn Momentanspannung „klassisch rot“. */
    static void drawLiveConductor(Graphics g, int x1, int y1, int x2, int y2,
				  Color base, double voltsInstant, double width,
				  boolean peZebra) {
	Color ref = base != null ? base : COL_PE_G;
	if (liveOrBase(ref, voltsInstant) == COL_LIVE_PULSE) {
	    drawConductor(g, x1, y1, x2, y2, COL_LIVE_PULSE, width);
	    return;
	}
	if (peZebra)
	    drawPeZebra(g, x1, y1, x2, y2, width);
	else
	    drawConductor(g, x1, y1, x2, y2, base, width);
    }

    static void drawConductor(Graphics g, int x1, int y1, int x2, int y2,
			      Color color, double width) {
	// dunkle Leiter (L2): dünner heller Rand, dann Farbe — sonst unsichtbar
	if (color.getRed() + color.getGreen() + color.getBlue() < 80) {
	    g.setColor(COL_OUTLINE);
	    g.setLineWidth(width + 1.0);
	    g.drawLine(x1, y1, x2, y2);
	}
	g.setColor(color);
	g.setLineWidth(width);
	g.drawLine(x1, y1, x2, y2);
	g.setLineWidth(1.0);
    }

    /** I von Pin a nach b. Strom in den Knoten aus dem Element (ChipElm-Leitungen). */
    static void pinCurrentThru(ChipElm.Pin[] pins, int a, int b, double i) {
	if (pins == null)
	    return;
	if (a >= 0 && a < pins.length && pins[a] != null)
	    pins[a].current = -i;
	if (b >= 0 && b < pins.length && pins[b] != null)
	    pins[b].current = i;
    }

    static void clearPinCurrents(ChipElm.Pin[] pins) {
	if (pins == null)
	    return;
	for (int i = 0; i < pins.length; i++)
	    if (pins[i] != null)
		pins[i].current = 0;
    }

    /** Strompunkte entlang eines Polylinienzugs (Schaltstrecke, nicht Sehne). */
    static void drawDotsAlong(CircuitElm elm, Graphics g, double pos,
			      Point[] pts) {
	if (elm == null || pts == null || pts.length < 2)
	    return;
	double cc = pos;
	for (int i = 0; i < pts.length - 1; i++) {
	    Point a = pts[i];
	    Point b = pts[i + 1];
	    if (a == null || b == null)
		continue;
	    elm.drawDots(g, a, b, cc);
	    double dx = b.x - a.x;
	    double dy = b.y - a.y;
	    cc = elm.addCurCount(cc, Math.sqrt(dx * dx + dy * dy));
	}
    }

    /** PE zebra (workbench variant D). */
    static void drawPeZebra(Graphics g, int x1, int y1, int x2, int y2, double width) {
	double dx = x2 - x1;
	double dy = y2 - y1;
	double len = Math.sqrt(dx * dx + dy * dy);
	if (len < 1)
	    return;
	double ux = dx / len;
	double uy = dy / len;
	int seg = 7;
	g.setLineWidth(width);
	for (int s = 0, i = 0; s < len; s += seg, i++) {
	    double e = Math.min(s + seg, len);
	    g.setColor((i % 2 == 0) ? COL_PE_G : COL_PE_Y);
	    g.drawLine((int) (x1 + ux * s), (int) (y1 + uy * s),
		       (int) (x1 + ux * e), (int) (y1 + uy * e));
	}
	g.setLineWidth(1.0);
    }

    /** Overview: circle; mark "~" (sine) or "3~". */
    static void drawOverviewSource(Graphics g, CircuitElm elm,
				   int cx, int cy, int r,
				   boolean threePhase, String designation,
				   Color stroke) {
	g.setColor(stroke);
	CircuitElm.drawThickCircle(g, cx, cy, r);
	g.setFont(new Font("normal", 0, Math.max(10, r / 2)));
	if (threePhase) {
	    String t = "3~";
	    int tw = (int) g.context.measureText(t).getWidth();
	    g.drawString(t, cx - tw / 2, cy + 4);
	} else {
	    // Sinus-Näherung
	    g.setLineWidth(2.0);
	    g.context.beginPath();
	    double a = r * 0.55;
	    for (int i = 0; i <= 24; i++) {
		double t = (i / 24.0) * Math.PI * 2;
		double x = cx - a + (i / 24.0) * 2 * a;
		double y = cy - Math.sin(t) * a * 0.55;
		if (i == 0)
		    g.context.moveTo(x, y);
		else
		    g.context.lineTo(x, y);
	    }
	    g.context.stroke();
	    g.setLineWidth(1.0);
	}
	if (designation != null && designation.length() > 0) {
	    /* Über dem Kreis — Klemmen der Quelle liegen rechts (SIDE_E). */
	    g.setFont(pinLabelFont());
	    int tw = (int) g.context.measureText(designation).getWidth();
	    g.drawString(designation, cx - tw / 2, cy - r - 6);
	    String note = EGTNote.of(elm);
	    if (note.length() > 0) {
		int nw = (int) g.context.measureText(note).getWidth();
		g.drawString(note, cx - nw / 2, cy - r + 8);
	    }
	}
    }

    /** Nach Chip-Drehung (FLIP_XY) liegen N/S-Klemmen links/rechts. */
    static boolean chipHasEastWestPins(ChipElm elm) {
	if (elm == null || elm.pins == null)
	    return false;
	int n = elm.getPostCount();
	for (int i = 0; i < n; i++) {
	    if (elm.pins[i] == null)
		continue;
	    int s = elm.pins[i].side;
	    if (s == ChipElm.SIDE_E || s == ChipElm.SIDE_W)
		return true;
	}
	return false;
    }

    /**
     * Gehäuse-Bezeichnung/Status. Klemmen oben/unten → rechts;
     * Klemmen links/rechts → über dem Kasten. Immer save/restore.
     * @return [x1,y1,x2,y2] für {@code adjustBbox}
     */
    static int[] drawHousingAnno(Graphics g, ChipElm elm,
				 int left, int top, int right, int bottom,
				 Color color, String line1, String line2,
				 String line3) {
	return drawHousingAnno(g, elm, left, top, right, bottom,
			       color, line1, line2, line3, null);
    }

    static int[] drawHousingAnno(Graphics g, ChipElm elm,
				 int left, int top, int right, int bottom,
				 Color color, String line1, String line2,
				 String line3, String line4) {
	String note = EGTNote.of(elm);
	String[] raw = note.length() > 0
	    ? new String[] { line1, note, line2, line3, line4 }
	    : new String[] { line1, line2, line3, line4 };
	int n = 0;
	for (int i = 0; i < raw.length; i++) {
	    if (raw[i] != null && raw[i].length() > 0)
		n++;
	}
	String[] lines = new String[n];
	int k = 0;
	for (int i = 0; i < raw.length; i++) {
	    if (raw[i] != null && raw[i].length() > 0)
		lines[k++] = raw[i];
	}
	boolean above = chipHasEastWestPins(elm);
	g.save();
	try {
	    g.setColor(color);
	    int maxW = 0;
	    for (int i = 0; i < n; i++) {
		g.setFont(pinLabelFont());
		int w = (int) g.context.measureText(lines[i]).getWidth();
		if (w > maxW)
		    maxW = w;
	    }
	    if (above) {
		g.context.setTextAlign("center");
		int cx = (left + right) / 2;
		int y0 = top - 6 - 14 * Math.max(n - 1, 0);
		for (int i = 0; i < n; i++) {
		    g.setFont(pinLabelFont());
		    g.drawString(lines[i], cx, y0 + i * 14);
		}
		int half = maxW / 2 + 8;
		return new int[] { Math.min(left, cx - half), y0 - 16,
				   Math.max(right, cx + half), bottom };
	    }
	    g.context.setTextAlign("left");
	    int tx = right + 8;
	    int[] ys;
	    if (n <= 1)
		ys = new int[] { top + 14 };
	    else if (n == 2)
		ys = new int[] { top + 14, bottom - 6 };
	    else if (n == 3)
		ys = new int[] { top + 14, top + 30, bottom - 6 };
	    else if (n == 4)
		ys = new int[] { top + 14, top + 28, bottom - 6,
				 bottom + 12 };
	    else
		ys = new int[] { top + 14, top + 28, top + 42, bottom - 6,
				 bottom + 12 };
	    for (int i = 0; i < n; i++) {
		g.setFont(pinLabelFont());
		g.drawString(lines[i], tx, ys[i]);
	    }
	    int yBot = n >= 4 ? bottom + 28 : bottom + 20;
	    return new int[] { left, top - 28, tx + maxW + 16, yBot };
	} finally {
	    g.restore();
	    g.context.setTextAlign("left");
	    g.context.setTextBaseline("alphabetic");
	}
    }

    /**
     * Spulen-Annotation neben der Spule, bei Seitenklemmen über dem Gehäuse.
     * @return [x1,y1,x2,y2] für {@code adjustBbox}
     */
    static int[] drawCoilAnno(Graphics g, ChipElm elm,
			      int left, int top, int right, int bottom,
			      int lx, int ly, Color color,
			      String line1, String line2) {
	if (chipHasEastWestPins(elm))
	    return drawHousingAnno(g, elm, left, top, right, bottom,
				   color, line1, line2, null);
	String note = EGTNote.of(elm);
	g.save();
	try {
	    g.setColor(color);
	    g.context.setTextAlign("left");
	    g.setFont(pinLabelFont());
	    int tw = 0;
	    int y = ly;
	    if (line1 != null) {
		g.drawString(line1, lx, y);
		tw = (int) g.context.measureText(line1).getWidth();
		y += 16;
	    }
	    if (note.length() > 0) {
		g.drawString(note, lx, y);
		int wn = (int) g.context.measureText(note).getWidth();
		if (wn > tw)
		    tw = wn;
		y += 16;
	    }
	    if (line2 != null) {
		g.drawString(line2, lx, y);
		int w2 = (int) g.context.measureText(line2).getWidth();
		if (w2 > tw)
		    tw = w2;
	    }
	    return new int[] { left, top - 22,
			       Math.max(right, lx + Math.max(tw, 70)) + 12,
			       bottom + (note.length() > 0 ? 32 : 18) };
	} finally {
	    g.restore();
	    g.context.setTextAlign("left");
	    g.context.setTextBaseline("alphabetic");
	}
    }

    /**
     * Kennzeichen plus optionale Schüler-Beschriftung darunter.
     * @return zusätzliche Zeilenhöhe (0 oder 14)
     */
    static int drawDesigAndNote(Graphics g, CircuitElm elm,
				String designation, int x, int y) {
	if (designation != null && designation.length() > 0)
	    g.drawString(designation, x, y);
	String note = EGTNote.of(elm);
	if (note.length() == 0)
	    return 0;
	g.drawString(note, x, y + 14);
	return 14;
    }

    static int measureDesigNote(Graphics g, CircuitElm elm,
				String designation) {
	int w = 0;
	if (designation != null && designation.length() > 0)
	    w = (int) g.context.measureText(designation).getWidth();
	String note = EGTNote.of(elm);
	if (note.length() > 0) {
	    int nw = (int) g.context.measureText(note).getWidth();
	    if (nw > w)
		w = nw;
	}
	return w;
    }

    /** Kontaktbogen ∩ (Öffnung nach unten); Scheitel bei yPeak. */
    static void drawOpenDownU(Graphics g, int cx, int yPeak, int r) {
	if (r < 4)
	    r = 4;
	double midY = yPeak + r;
	g.context.beginPath();
	for (int i = 0; i <= 24; i++) {
	    double a = Math.PI + (i / 24.0) * Math.PI;
	    double x = cx + r * Math.cos(a);
	    double y = midY + r * Math.sin(a);
	    if (i == 0)
		g.context.moveTo(x, y);
	    else
		g.context.lineTo(x, y);
	}
	g.context.stroke();
    }

    /**
     * Rastende Betätigung (IEC): Stiel mit V-Knick + Endquerstrich (T).
     * mx,my = Ansatz am Messermittelpunkt; len nach links.
     */
    static void drawRastBetaetigung(Graphics g, int mx, int my, int len,
				    boolean dashed, Color stroke) {
	if (len < 12)
	    len = 12;
	int x0 = mx;
	int x1 = mx - (len * 38) / 100;
	int xK = mx - (len * 58) / 100;
	int x2 = mx - len;
	g.setColor(stroke);
	g.setLineWidth(2.0);
	if (dashed)
	    g.setLineDash(5, 3);
	g.drawLine(x0, my, x1, my);
	g.drawLine(x1, my, xK, my + 6);
	g.drawLine(xK, my + 6, xK - (len * 12) / 100, my);
	g.drawLine(xK - (len * 12) / 100, my, x2 + 4, my);
	g.setLineDash(0, 0);
	g.drawLine(x2 + 4, my - 7, x2 + 4, my + 7);
	g.setLineWidth(1.0);
    }

    /**
     * Tastende Betätigung: gestrichelter Stiel + T-Ende, kein V-Knick.
     * mx,my = Ansatz am Messermittelpunkt; len nach links.
     */
    static void drawTastBetaetigung(Graphics g, int mx, int my, int len,
				    Color stroke) {
	if (len < 12)
	    len = 12;
	int x2 = mx - len;
	g.setColor(stroke);
	g.setLineWidth(2.0);
	g.setLineDash(5, 3);
	g.drawLine(mx, my, x2 + 4, my);
	g.setLineDash(0, 0);
	g.drawLine(x2 + 4, my - 7, x2 + 4, my + 7);
	g.setLineWidth(1.0);
    }

    /** Kleines Leuchtensymbol (Kreis+X) rechts neben dem Post. */
    static void drawLampPinMark(Graphics g, CircuitElm elm,
			       int postX, int postY, Color stroke) {
	int r = 6;
	int cx = postX + PIN_LABEL_PAD + r;
	int cy = postY;
	g.setColor(stroke);
	g.setLineWidth(1.6);
	g.context.beginPath();
	g.context.arc(cx, cy, r, 0, 2 * Math.PI);
	g.context.stroke();
	drawLampX(g, cx, cy, r, stroke);
	if (elm != null)
	    elm.adjustBbox(cx - r - 1, cy - r - 1, cx + r + 1, cy + r + 1);
    }

    /** Lampenkreuz (X) im Kreis — Leuchte / Leuchttaster. */
    static void drawLampX(Graphics g, int cx, int cy, int r, Color stroke) {
	int d = (int) (r * 0.7);
	if (d < 4)
	    d = 4;
	g.setColor(stroke);
	g.setLineWidth(2.0);
	g.drawLine(cx - d, cy - d, cx + d, cy + d);
	g.drawLine(cx + d, cy - d, cx - d, cy + d);
	g.setLineWidth(1.0);
    }

    /**
     * Klingel/Gong IEC: Kuppel (Halbkreis oben) + Grundlinie + zwei Beinchen.
     * baseY = Unterkante der Kuppel.
     */
    static void drawBell(Graphics g, int cx, int baseY, int radius, int legLen,
			 Color stroke) {
	if (radius < 6)
	    radius = 6;
	if (legLen < 4)
	    legLen = 4;
	g.setColor(stroke);
	g.setLineWidth(2.5);
	g.context.setLineCap("butt");
	g.context.beginPath();
	g.context.arc(cx, baseY, radius, Math.PI, 0, false);
	g.context.stroke();
	g.drawLine(cx - radius, baseY, cx + radius, baseY);
	int gap = (int) (radius * 0.38);
	if (gap < 3)
	    gap = 3;
	g.drawLine(cx - gap, baseY, cx - gap, baseY + legLen);
	g.drawLine(cx + gap, baseY, cx + gap, baseY + legLen);
	g.setLineWidth(1.0);
    }

    /**
     * Schallbögen (WLAN-artig) rechts oben — phase aus sim.t für Puls.
     */
    static void drawBellSoundWaves(Graphics g, int cx, int baseY, int radius,
				   double phase, Color stroke) {
	if (radius < 6)
	    radius = 6;
	int ox = cx + (int) (radius * 0.25);
	int oy = baseY - (int) (radius * 0.65);
	g.context.setLineCap("round");
	for (int i = 0; i < 3; i++) {
	    double pulse = 0.45 + 0.55 * (0.5 + 0.5
		    * Math.sin(phase * 2.2 - i * 0.85));
	    double r = radius * (0.55 + i * 0.42)
		    + Math.sin(phase * 1.6 + i) * 1.5;
	    g.context.setGlobalAlpha(0.25 + 0.7 * pulse);
	    g.setColor(stroke);
	    g.setLineWidth(2.2);
	    g.context.beginPath();
	    g.context.arc(ox, oy, r, -Math.PI * 0.62, -Math.PI * 0.08, false);
	    g.context.stroke();
	}
	g.context.setGlobalAlpha(1.0);
	g.context.setLineCap("butt");
	g.setLineWidth(1.0);
    }

    /**
     * Türöffner IEC: Spulenrechteck mit ~, rechts rechtwinkliges Dreieck.
     * x,y = links oben am Rechteck. Rückgabe triW (Gesamtbreite = w + triW).
     */
    static int drawDoorOpener(Graphics g, int x, int y, int w, int h,
			      Color stroke) {
	if (w < 12)
	    w = 12;
	if (h < 8)
	    h = 8;
	int triW = (int) (h * 0.9);
	if (triW < 6)
	    triW = 6;
	g.setColor(stroke);
	g.setLineWidth(2.5);
	g.context.setLineCap("butt");
	g.drawRect(x, y, w, h);
	int padX = (int) (w * 0.14);
	if (padX < 2)
	    padX = 2;
	double midY = y + h / 2.0;
	double amp = h * 0.22;
	g.context.beginPath();
	for (int i = 0; i <= 24; i++) {
	    double t = (i / 24.0) * Math.PI * 2;
	    double px = x + padX + (i / 24.0) * (w - 2 * padX);
	    double py = midY - Math.sin(t) * amp;
	    if (i == 0)
		g.context.moveTo(px, py);
	    else
		g.context.lineTo(px, py);
	}
	g.context.stroke();
	int rx = x + w;
	int by = y + h;
	g.drawLine(rx, y, rx + triW, by);
	g.drawLine(rx + triW, by, rx, by);
	g.setLineWidth(1.0);
	return triW;
    }

    static final Color COL_CONTACT_OPEN = new Color(0xff, 0xb3, 0x47);

    /**
     * Gleichmäßige Chip-Pin-Positionen 0..sizeX-1 mit Randabstand {@code inset}.
     * {@code out.length >= n}.
     */
    static void evenPinPos(int n, int sizeX, int inset, int[] out) {
	if (n <= 0 || out == null)
	    return;
	if (sizeX < 1)
	    sizeX = 1;
	if (inset < 0)
	    inset = 0;
	while (2 * inset >= sizeX && inset > 0)
	    inset--;
	int first = inset;
	int last = sizeX - 1 - inset;
	if (last < first)
	    last = first;
	for (int i = 0; i < n; i++) {
	    if (n == 1)
		out[i] = (first + last) / 2;
	    else
		out[i] = first + i * (last - first) / (n - 1);
	}
    }

    /** Spulenrechteck (Leistungsschütz: plain ohne Schrägstrich). */
    static void drawCoilRect(Graphics g, int cx, int cy, int w, int h,
			     boolean plain, Color stroke) {
	if (w < 8)
	    w = 8;
	if (h < 6)
	    h = 6;
	g.setColor(stroke);
	g.setLineWidth(2.5);
	g.context.setLineCap("butt");
	g.drawRect(cx - w / 2, cy - h / 2, w, h);
	if (!plain) {
	    int inset = Math.min(w, h) * 22 / 100;
	    g.drawLine(cx - w / 2 + inset, cy + h / 2 - inset,
		       cx + w / 2 - inset, cy - h / 2 + inset);
	}
	g.setLineWidth(1.0);
    }

    /**
     * Einheitliche Schütz-Spulengröße aus Chip-Box.
     * @return int[]{ coilW, coilH }
     */
    static int[] standardCoilSize(int boxW, int boxH) {
	int coilH = Math.max(10, boxH * 22 / 100);
	int coilW = Math.max(coilH + 6, boxW * 48 / 100);
	return new int[] { coilW, coilH };
    }

    /**
     * Stromstoß-Spulenrechteck quer (breiter als hoch), nicht schmal zusammengedrückt.
     * Schütz bleibt bei {@link #standardCoilSize}.
     * @return int[]{ coilW, coilH }
     */
    static int[] impulseCoilSize(int maxW, int maxH) {
	int h = Math.max(16, maxH * 38 / 100);
	if (maxH > 8 && h > maxH - 8)
	    h = Math.max(14, maxH - 8);
	int w = Math.max(h + 10, h * 3 / 2);
	if (maxW >= 14 && w > maxW)
	    w = maxW;
	if (w < 14)
	    w = Math.min(14, Math.max(10, maxW));
	return new int[] { w, h };
    }

    /**
     * Stromstoß-Spule: Rechteck + Stufenmarkierung (Workbench).
     */
    static void drawImpulseCoil(Graphics g, int cx, int cy, int w, int h,
				Color stroke) {
	if (h < 12)
	    h = 12;
	if (w < 10)
	    w = 10;
	g.setColor(stroke);
	g.setLineWidth(2.5);
	g.context.setLineCap("butt");
	g.drawRect(cx - w / 2, cy - h / 2, w, h);
	int arm = Math.max(3, (int) Math.round(w * 0.28));
	int rise = Math.max(3, (int) Math.round(h * 0.28));
	int midY = cy + (int) Math.round(h * 0.08);
	int highY = midY - rise;
	int topIn = cy - h / 2 + 2;
	int botIn = cy + h / 2 - 2;
	if (highY < topIn)
	    highY = topIn;
	if (midY > botIn)
	    midY = botIn;
	if (midY <= highY)
	    highY = midY - Math.max(2, h / 4);
	g.setLineWidth(2.2);
	g.drawLine(cx - arm, midY, cx, midY);
	g.drawLine(cx, midY, cx, highY);
	g.drawLine(cx, highY, cx + arm, highY);
	g.setLineWidth(1.0);
    }

    /** Schließer NO vertikal: Festkontakt oben, Drehpunkt unten. */
    static void drawNoContact(Graphics g, int cx, int topY, int bottomY,
			      boolean closed, Color stroke) {
	drawNoContact(g, cx, topY, bottomY, closed, stroke, true);
    }

    /**
     * Schließer NO. {@code tBar=false}: nur Stamm/Klinge, kein Querstrich
     * (Leistungsschütz 13/14).
     */
    static void drawNoContact(Graphics g, int cx, int topY, int bottomY,
			      boolean closed, Color stroke, boolean tBar) {
	int openDx = Math.max(10, (bottomY - topY) * 28 / 100);
	int tipX = closed ? cx : cx - openDx;
	int tipY = closed ? topY : topY + Math.max(3, (bottomY - topY) / 12);
	g.setColor(stroke);
	g.setLineWidth(2.0);
	g.context.setLineCap("butt");
	if (tBar)
	    g.drawLine(cx - 5, topY, cx + 5, topY);
	g.setColor(closed ? stroke : COL_CONTACT_OPEN);
	g.setLineWidth(2.5);
	g.drawLine(cx, bottomY, tipX, tipY);
	g.setLineWidth(1.0);
    }

    /** Schütz-Schließer mit Bauch (hook) links am Stamm. */
    static void drawContactorNoContact(Graphics g, int cx, int topY, int bottomY,
				       boolean closed, Color stroke) {
	int bellyR = Math.max(4, (bottomY - topY) * 12 / 100);
	int tipY = topY + Math.max(8, (bottomY - topY) * 22 / 100);
	int bellyCy = tipY - bellyR;
	g.setColor(stroke);
	g.setLineWidth(2.5);
	g.context.setLineCap("butt");
	g.drawLine(cx, topY, cx, tipY);
	g.context.beginPath();
	g.context.arc(cx, bellyCy, bellyR, Math.PI / 2, -Math.PI / 2, false);
	g.context.closePath();
	g.context.fill();
	g.context.stroke();
	int openDx = Math.max(10, (bottomY - topY) * 28 / 100);
	int tipX = closed ? cx - bellyR / 5 : cx - openDx;
	int bladeY = closed ? tipY : tipY + Math.max(2, (bottomY - topY) / 16);
	g.setColor(closed ? stroke : COL_CONTACT_OPEN);
	g.drawLine(cx, bottomY, tipX, bladeY);
	g.setLineWidth(1.0);
    }

    /** Kurzer oberer Querstrich nach rechts; Arm ~18° von der Senkrechten. */
    static int ncHook(int span) {
	if (span < 8)
	    span = 8;
	return Math.max(8, span * 32 / 100);
    }

    static int ncTipX(int cx, int topY, int bottomY, boolean closed) {
	int hook = ncHook(bottomY - topY);
	return closed ? cx + hook
		      : cx + hook + Math.max(6, (bottomY - topY) * 12 / 100);
    }

    static int ncTipY(int topY, int bottomY, boolean closed) {
	return closed ? topY
		      : topY + Math.max(3, (bottomY - topY) / 12);
    }

    /**
     * Öffner NC, Schulbuch/Taster: Haken nach rechts, Arm nach oben rechts
     * an die Hakenkuppe. closed=true = leitend.
     */
    static void drawNcContact(Graphics g, int cx, int topY, int bottomY,
			      boolean closed, Color stroke) {
	int hook = ncHook(bottomY - topY);
	int tipX = ncTipX(cx, topY, bottomY, closed);
	int tipY = ncTipY(topY, bottomY, closed);
	g.setColor(stroke);
	g.setLineWidth(2.5);
	g.context.setLineCap("butt");
	g.drawLine(cx, topY, cx + hook, topY);
	g.drawLine(cx, bottomY, tipX, tipY);
	g.setLineWidth(1.0);
    }

    /**
     * Bimetall-Heizleiter (Überlastrelais): Rechteck mit Rechteckwelle,
     * Fachbild 1/3/5–2/4/6.
     */
    static void drawBimetalHeater(Graphics g, int cx, int y1, int y2,
				  Color stroke) {
	int h = y2 - y1;
	if (h < 8)
	    return;
	int amp = Math.max(4, h * 16 / 100);
	int boxL = cx - amp - 2;
	int boxW = 2 * amp + 4;
	g.setColor(stroke);
	g.setLineWidth(1.4);
	g.drawRect(boxL, y1, boxW, h);
	g.setLineWidth(2.0);
	g.context.setLineCap("butt");
	g.context.beginPath();
	g.context.moveTo(cx, y1);
	int segs = 6;
	int sign = 1;
	for (int i = 0; i < segs; i++) {
	    int ya = y1 + i * h / segs;
	    int yb = (i == segs - 1) ? y2 : y1 + (i + 1) * h / segs;
	    g.context.lineTo(cx + sign * amp, ya);
	    g.context.lineTo(cx + sign * amp, yb);
	    sign = -sign;
	}
	g.context.lineTo(cx, y2);
	g.context.stroke();
	g.setLineWidth(1.0);
    }

    /** Gestrichelte mechanische Kopplung horizontal. */
    static void drawMechLink(Graphics g, int x1, int x2, int y, Color stroke) {
	g.setColor(stroke);
	g.setLineWidth(1.8);
	g.setLineDash(7, 5);
	g.drawLine(x1, y, x2, y);
	g.setLineDash(0, 0);
	g.setLineWidth(1.0);
    }

    /**
     * Zeitrelais-Spule 1:1 Workbench/Fachkunde: Raster {@code u}, Rechteck 3×1,
     * linkes Feld X (Ansprech), gefüllt (Rückfall), X/gefüllt
     * (kombiniert) oder leer (Blinken). Die A1/A2-Achse liegt wie in der
     * Fachkunde bei x=2·u ab linker Rechteckkante.
     * Zuleitungen optional ({@code drawLeads=false} wenn Caller Posts selbst
     * anbindet — Schütz-Größe).
     */
    static void drawZeitCoil(Graphics g, int cx, int cy, int u,
			     boolean offDelay, Color stroke, boolean drawLeads) {
	drawZeitCoil(g, cx, cy, u, offDelay, false, false, stroke,
		     drawLeads);
    }

    static void drawZeitCoil(Graphics g, int cx, int cy, int u,
			     boolean offDelay, boolean blink, Color stroke,
			     boolean drawLeads) {
	drawZeitCoil(g, cx, cy, u, offDelay, blink, false, stroke,
		     drawLeads);
    }

    static void drawZeitCoil(Graphics g, int cx, int cy, int u,
			     boolean offDelay, boolean blink,
			     boolean combined, Color stroke,
			     boolean drawLeads) {
	if (u < 8)
	    u = 8;
	int w = 3 * u;
	int h = 1 * u;
	// cx ist die Anschlussachse; rechts davon bleibt ein Rasterfeld.
	int x = cx - 2 * u;
	int y = cy - h / 2;
	g.setColor(stroke);
	g.setLineWidth(2.5);
	g.context.setLineCap("butt");
	g.drawRect(x, y, w, h);
	g.drawLine(x + u, y, x + u, y + h);
	if (combined) {
	    int mid = y + h / 2;
	    int pad = Math.max(2, (int) Math.round(u * 0.18));
	    g.fillRect(x + 2, mid, Math.max(1, u - 4),
		       Math.max(1, y + h - mid - 2));
	    g.drawLine(x + pad, y + pad, x + u - pad, mid - pad);
	    g.drawLine(x + u - pad, y + pad, x + pad, mid - pad);
	} else if (!blink) {
	    if (offDelay) {
		g.fillRect(x + 2, y + 2, Math.max(1, u - 4),
			   Math.max(1, h - 4));
	    } else {
		int pad = Math.max(2, (int) Math.round(u * 0.18));
		g.drawLine(x + pad, y + pad, x + u - pad, y + h - pad);
		g.drawLine(x + u - pad, y + pad, x + pad, y + h - pad);
	    }
	}
	if (drawLeads) {
	    g.drawLine(cx, y - u, cx, y);
	    g.drawLine(cx, y + h, cx, y + h + u);
	}
	g.setLineWidth(1.0);
    }

    /** Kompatibilität: mit Zuleitungen ±u. */
    static void drawZeitCoil(Graphics g, int cx, int cy, int u,
			     boolean offDelay, Color stroke) {
	drawZeitCoil(g, cx, cy, u, offDelay, stroke, true);
    }

    /**
     * Zeitrelais-Kontakt Workbench: Fallschirm (Pilz) + zwei klar getrennte Arme.
     * Blinkend: S-Kurve statt Fallschirm (Fachkunde).
     * {@code u} = Rasterfeld; Achse bei {@code cx + 0.75·u}.
     */
    static void drawZeitContact(Graphics g, int cx, int cy, int u,
				boolean nc, boolean closed, boolean offDelay,
				Color stroke) {
	drawZeitContact(g, cx, cy, u, nc, closed, offDelay, false, false,
			stroke);
    }

    static void drawZeitContact(Graphics g, int cx, int cy, int u,
				boolean nc, boolean closed, boolean offDelay,
				boolean blink, Color stroke) {
	drawZeitContact(g, cx, cy, u, nc, closed, offDelay, blink, false,
			stroke);
    }

    static void drawZeitContact(Graphics g, int cx, int cy, int u,
				boolean nc, boolean closed, boolean offDelay,
				boolean blink, boolean combined, Color stroke) {
	if (u < 10)
	    u = 10;
	int axisX = cx + (int) Math.round(0.75 * u);
	int topY = cy - (int) Math.round(2.5 * u);
	int botY = cy + (int) Math.round(2.5 * u);
	int tipY = cy - (int) Math.round(1.05 * u);
	int kneeY = cy + (int) Math.round(0.95 * u);
	int tipX;
	int bladeTipY;
	if (nc) {
	    tipX = ncTipX(axisX, tipY, kneeY, closed);
	    bladeTipY = ncTipY(tipY, kneeY, closed);
	} else {
	    tipX = closed ? axisX
			  : axisX - Math.max(8, (int) Math.round(0.85 * u));
	    bladeTipY = tipY;
	}
	g.setColor(stroke);
	g.setLineWidth(2.2);
	g.context.setLineCap("butt");
	g.context.setLineJoin("miter");
	if (!nc && closed) {
	    g.drawLine(axisX, topY, axisX, botY);
	} else {
	    g.drawLine(axisX, topY, axisX, tipY);
	    g.drawLine(axisX, kneeY, axisX, botY);
	    if (nc)
		drawNcContact(g, axisX, tipY, kneeY, closed, stroke);
	    else {
		g.setLineWidth(2.4);
		g.drawLine(axisX, kneeY, tipX, bladeTipY);
	    }
	}

	double den = bladeTipY - kneeY;
	if (Math.abs(den) < 0.001)
	    den = 1;
	double armAt = (!nc && closed) ? axisX
		: axisX + ((cy - kneeY) / den) * (tipX - axisX);

	if (blink) {
	    // Fachkunde: senkrechte S-Kurve mit waagerechter Mittellinie zum Messer.
	    double r = Math.max(5, 0.42 * u);
	    double sx = armAt - 1.08 * u;
	    g.setLineWidth(2.0);
	    g.drawLine((int) Math.round(sx - r), cy,
		       (int) Math.round(armAt), cy);
	    g.context.beginPath();
	    g.context.arc(sx, cy - r, r, -Math.PI / 2, Math.PI / 2, false);
	    g.context.arc(sx, cy + r, r, -Math.PI / 2, Math.PI / 2, true);
	    g.context.stroke();
	    g.setLineWidth(1.0);
	    return;
	}

	double gap = Math.max(5, 0.36 * u);
	double rad = Math.max(8, 0.78 * u);
	double y1 = cy - gap / 2;
	double y2 = cy + gap / 2;
	boolean onDelay = !offDelay;
	double armY1 = (!nc && closed) ? axisX
		: axisX + ((y1 - kneeY) / den) * (tipX - axisX);
	double armY2 = (!nc && closed) ? axisX
		: axisX + ((y2 - kneeY) / den) * (tipX - axisX);
	double halfGap = gap / 2;
	if (halfGap >= rad)
	    halfGap = rad * 0.85;
	double chord = Math.sqrt(Math.max(rad * rad - halfGap * halfGap, 0));
	if (combined) {
	    // Fachkunde-Zeichen `)(`: zwei Halbkreise berühren sich in der
	    // Mitte, statt sich zu einem Kreis zu überlagern. Von diesem
	    // Berührpunkt laufen beide Zeitlinien bis zum Messer.
	    double junctionX = armAt - 1.55 * u;
	    double leftCupX = junctionX - rad;
	    double rightCupX = junctionX + rad;
	    g.setLineWidth(2.0);
	    g.context.beginPath();
	    g.context.arc(leftCupX, cy, rad, -Math.PI / 2,
			  Math.PI / 2, false);
	    g.context.stroke();
	    g.context.beginPath();
	    g.context.arc(rightCupX, cy, rad, -Math.PI / 2,
			  Math.PI / 2, true);
	    g.context.stroke();
	    g.drawLine((int) Math.round(junctionX), (int) Math.round(y1),
		       (int) Math.round(armY1),
		       (int) Math.round(y1));
	    g.drawLine((int) Math.round(junctionX), (int) Math.round(y2),
		       (int) Math.round(armY2),
		       (int) Math.round(y2));
	    g.setLineWidth(1.0);
	    return;
	}

	double cupX = onDelay ? armAt - 1.35 * u : armAt - 1.95 * u;
	double startX = onDelay ? cupX - chord : cupX + chord;
	g.setLineWidth(2.0);
	g.drawLine((int) Math.round(startX), (int) Math.round(y1),
		   (int) Math.round(armY1), (int) Math.round(y1));
	g.drawLine((int) Math.round(startX), (int) Math.round(y2),
		   (int) Math.round(armY2), (int) Math.round(y2));
	g.context.beginPath();
	g.context.arc(cupX, cy, rad, -Math.PI / 2, Math.PI / 2, onDelay);
	g.context.stroke();
	g.setLineWidth(1.0);
    }

    /** Achse X des Zeitrelais-Kontakts (für Dots/Zuleitungen). */
    static int zeitContactAxisX(int cx, int u) {
	return cx + (int) Math.round(0.75 * Math.max(8, u));
    }

    static int zeitContactTopY(int cy, int u) {
	return cy - (int) Math.round(2.5 * Math.max(8, u));
    }

    static int zeitContactBotY(int cy, int u) {
	return cy + (int) Math.round(2.5 * Math.max(8, u));
    }

    /**
     * Sicherung/LS Workbench: Pfeil setzt am Hebel an und zeigt nach links.
     */
    static void drawSicherungTripArrow(Graphics g, int x, int y, int h,
				      Color stroke) {
	int len = Math.max(12, h * 40 / 100);
	int head = Math.max(6, h * 18 / 100);
	int spread = (int) Math.round(head * 0.48);
	if (spread < 2)
	    spread = 2;
	g.setColor(stroke);
	g.setLineWidth(2);
	g.context.setLineCap("butt");
	int x0 = x - 2;
	int x1 = x0 - len;
	g.drawLine(x0, y, x1, y);
	g.drawLine(x1, y, x1 + head, y - spread);
	g.drawLine(x1, y, x1 + head, y + spread);
	g.setLineWidth(1.0);
    }

    /** Schaltmesser: Festkontakt oben, Drehpunkt unten, ausgelöst nach links. */
    static void drawSicherungPole(Graphics g, int cx, int topY, int botY,
				 boolean closed, Color stroke) {
	int h = botY - topY;
	if (h < 8)
	    h = 8;
	int openDx = Math.max(10, h * 28 / 100);
	int tipX = closed ? cx : cx - openDx;
	int tipY = closed ? topY : topY + Math.max(3, h / 12);
	g.setColor(stroke);
	g.setLineWidth(2);
	g.context.setLineCap("butt");
	int bar = Math.max(5, h * 8 / 100);
	g.drawLine(cx - bar, topY, cx + bar, topY);
	g.setLineWidth(2.5);
	g.drawLine(cx, botY, tipX, tipY);
	int ax = cx + (tipX - cx) * 82 / 100;
	int ay = botY + (tipY - botY) * 82 / 100;
	drawSicherungTripArrow(g, ax, ay, h, stroke);
	g.setLineWidth(1.0);
    }

    /** Edit-Dialog Kontaktart (Schließer/Öffner), Vorbild Zeitrelais-Kontakt. */
    static EditInfo kontaktartInfo(boolean nc) {
	EditInfo ei = new EditInfo("Kontaktart", 0);
	ei.choice = new Choice();
	ei.choice.add(Locale.LS("Schließer"));
	ei.choice.add(Locale.LS("Öffner"));
	ei.choice.select(nc ? 1 : 0);
	return ei;
    }

    static int applyKontaktart(int flags, int flagNc, EditInfo ei) {
	if (ei == null || ei.choice == null)
	    return flags;
	if (ei.choice.getSelectedIndex() == 1)
	    return flags | flagNc;
	return flags & ~flagNc;
    }
}

/*
    TAR-Dervic EGT: Drehstrom-Asynchronmotor V2 (M501–M504 / MOD-MOT-01), Dump 459
*/

package com.lushprojects.circuitjs1.client;

import com.google.gwt.xml.client.Document;
import com.google.gwt.xml.client.Element;
import com.lushprojects.circuitjs1.client.util.Locale;

/**
 * EGT-Drehstrommotor: Physik von ThreePhaseMotorElm, Optik Workbench-Variante A.
 * Didaktik M502: fehlende Versorgung stoppt; kein Zweiphasen-Weiterlauf.
 */
class EGTDrehstrommotorElm extends ThreePhaseMotorElm implements EGTDesignatable {
    static final Color COL_ACCENT = new Color(0x4a, 0x9e, 0xff);
    static final Color COL_IDLE = new Color(0x66, 0x66, 0x66);
    static final double SPEED_ON = 0.25;
    static final double SPEED_OFF = 0.05;
    static final double PHASE_I_MIN = 0.15;
    /** Relativ zum stärksten Leiterstrom — Rest/Kopplung zählt nicht als Phase. */
    static final double PHASE_I_REL = 0.3;
    static final double PHASE_TAU = 0.04;
    /** Simzeit: Peak-Fenster gegen 50-Hz-Nulldurchgang (Erkennung). */
    static final double LIVE_HOLD = 0.05;
    /**
     * Simzeit bis Anzeige Phasenausfall/Pfeil-weg (synchron).
     * Default-Speed ≈ Echtzeit → ~0,1 s; Slider links → länger.
     */
    static final double FAIL_DISPLAY_DELAY = 0.05;
    static final double COAST_BRAKE = 15;
    /** Max. Spannungsdifferenz U2/V2/W2 für Sternbrücke (V, Hüllkurve). */
    static final double STAR_SPREAD_MAX = 25;
    static final double DEF_J = 0.02;
    static final double DEF_B = 0.08;
    /** Bezug = Upstream-Default; Schüler-kW skaliert R/L/b. */
    static final double P_REF_KW = 1.5;
    static final double RAW_RS = .435;
    static final double RAW_RR = .816;
    static final double RAW_LS = .0294;
    static final double RAW_LR = .0297;
    static final double RAW_LM = .0287;
    /** Schulformel I = P / (√3 · 400 V · η · cos φ). */
    static final double U_N_LL = 400;
    static final double DEF_ETA = 0.80;
    static final double DEF_COSPHI = 0.80;
    static final double ETA_MIN = 0.50;
    static final double ETA_MAX = 0.98;
    static final double P_MIN_KW = .12;
    static final double P_MAX_KW = 15;

    String designation = "M1";
    String note = "";
    double ratedKw = P_REF_KW;
    double eta = DEF_ETA;
    boolean showAdvanced;
    boolean skipAdvancedThisApply;

    public String egtDesignation() { return designation; }
    public void setEgtDesignation(String d) {
	if (d != null)
	    designation = d;
    }
    public String egtNote() { return note; }
    public void setEgtNote(String s) { note = EGTNote.clamp(s); }
    double envUV, envVW, envWU;
    double envIu, envIv, envIw;
    double lastPhaseT = -1;
    double lastLiveU = -1e9, lastLiveV = -1e9, lastLiveW = -1e9;
    double phaseFaultSinceT = -1;
    int lastDirSign;
    boolean dirLatched;

    public EGTDrehstrommotorElm(int xx, int yy) {
	super(xx, yy);
	designation = "M1";
	ratedKw = P_REF_KW;
	eta = DEF_ETA;
	J = DEF_J;
	b = DEF_B;
	applyRatedPower();
    }

    public EGTDrehstrommotorElm(int xa, int ya, int xb, int yb, int f,
				StringTokenizer st) {
	super(xa, ya, xb, yb, f, st);
	if (st.hasMoreTokens())
	    designation = CustomLogicModel.unescape(st.nextToken());
	if (designation == null || designation.length() == 0)
	    designation = "M1";
	if (st.hasMoreTokens()) {
	    try {
		ratedKw = new Double(st.nextToken()).doubleValue();
	    } catch (Exception e) {
		ratedKw = 0;
	    }
	}
	if (st.hasMoreTokens()) {
	    try {
		eta = parseEtaToken(new Double(st.nextToken()).doubleValue());
	    } catch (Exception e) {
		eta = DEF_ETA;
	    }
	}
	if (ratedKw < P_MIN_KW)
	    inferRatedKw();
	eta = clampEta(eta);
	applyDidacticMech();
    
	note = EGTNote.readOptional(st);
}

    /** Lehrer-Demo: Upstream-J=1 nicht übernehmen. b folgt Nennleistung. */
    void applyDidacticMech() {
	if (J > 0.1)
	    J = DEF_J;
    }

    double clampRatedKw(double p) {
	if (p < P_MIN_KW)
	    return P_MIN_KW;
	if (p > P_MAX_KW)
	    return P_MAX_KW;
	return p;
    }

    static double clampEta(double e) {
	if (e < ETA_MIN)
	    return ETA_MIN;
	if (e > ETA_MAX)
	    return ETA_MAX;
	return e;
    }

    /** 80 oder 0,80 → 0,80. */
    static double parseEtaToken(double v) {
	if (v > 1.5)
	    v = v / 100;
	return clampEta(v);
    }

    /** ωL des Rohmodells vs. Schulstrom bei 1,5 kW / η=0,8 / cos φ=0,8. */
    static double zScale() {
	double iModel = 230.0 / (2 * Math.PI * 50 * RAW_LS);
	double iSchool = P_REF_KW * 1000
		/ (Math.sqrt(3) * U_N_LL * DEF_ETA * DEF_COSPHI);
	return iModel / iSchool;
    }

    double schoolCurrentA() {
	return ratedKw * 1000
		/ (Math.sqrt(3) * U_N_LL * clampEta(eta) * DEF_COSPHI);
    }

    /** Stromskala: mehr kW oder schlechteres η → mehr Strom. */
    double powerScale() {
	double s = (ratedKw / P_REF_KW) * (DEF_ETA / clampEta(eta));
	if (s < 0.02)
	    return 0.02;
	return s;
    }

    void inferRatedKw() {
	if (Rs > 1e-9)
	    ratedKw = P_REF_KW * (RAW_RS / Rs);
	ratedKw = clampRatedKw(ratedKw);
    }

    /**
     * kW und η → R/L so, dass Leiterstrom ≈ P/(√3·400·η·cos φ).
     * Rohmodell ohne Z-Faktor zieht ~25 A, nicht Schulstrom.
     */
    void applyRatedPower() {
	ratedKw = clampRatedKw(ratedKw);
	eta = clampEta(eta);
	double s = powerScale();
	double z = zScale();
	Rs = RAW_RS * z / s;
	Rr = RAW_RR * z / s;
	Ls = RAW_LS * z / s;
	Lr = RAW_LR * z / s;
	Lm = RAW_LM * z / s;
	b = DEF_B * s / z;
	if (b < 0.004)
	    b = 0.004;
	J = DEF_J;
    }

    /** Alte kW-Skala (Ls ≈ RAW/s) auf Schulstrom heben. */
    void maybeMigrateSchoolScale() {
	double sP = ratedKw / P_REF_KW;
	if (sP < 0.02)
	    sP = 0.02;
	double lsRaw = RAW_LS / sP;
	double iModel = 230.0 / (2 * Math.PI * 50 * RAW_LS);
	double z08 = iModel
		/ (P_REF_KW * 1000 / (Math.sqrt(3) * U_N_LL * 0.8));
	double ls08 = RAW_LS * z08 / sP;
	if (Ls > 1e-9 && (relClose(Ls, lsRaw) || relClose(Ls, ls08)))
	    applyRatedPower();
    }

    static boolean relClose(double a, double b) {
	return Math.abs(a - b) / b < 0.25;
    }

    /**
     * Zuleitungen parallel zur Elementachse; Enden knapp außerhalb des Kreises.
     * Waagerecht: jede Zeile gerade (gleiche Querlage links/rechts);
     * rechts elektrisch W2 / U2 / V2 (Nodes 5 / 1 / 3).
     */
    void clampLeadsToRim() {
	if (posts == null || leads == null || motorCenter == null)
	    return;
	double rim = cr + 2;
	double L = dn;
	if (L < 1)
	    L = 1;
	boolean horiz = Math.abs(dx) >= Math.abs(dy);
	int q = horiz ? 1 : -1;
	int cx = motorCenter.x;
	int cy = motorCenter.y;
	int i;
	for (i = 0; i != 3; i++) {
	    double g0 = -q * 32.0 * (i - 1);
	    double g1 = horiz ? g0 : (q * 32.0 * (i - 1));
	    int right = i * 2 + 1;
	    if (horiz)
		right = (i == 0) ? 5 : ((i == 1) ? 1 : 3);
	    if (Math.abs(g0) >= rim)
		g0 = Math.copySign(rim - 1, g0);
	    if (Math.abs(g1) >= rim)
		g1 = Math.copySign(rim - 1, g1);
	    double disc0 = rim * rim - g0 * g0;
	    double disc1 = rim * rim - g1 * g1;
	    double df0 = (disc0 <= 0) ? 0 : Math.sqrt(disc0) / L;
	    double df1 = (disc1 <= 0) ? 0 : Math.sqrt(disc1) / L;
	    double f0 = 0.5 - df0;
	    double f1 = 0.5 + df1;
	    if (f0 < 0)
		f0 = 0;
	    if (f1 > 1)
		f1 = 1;
	    interpPoint(point1, point2, leads[i * 2], f0, g0);
	    interpPoint(point1, point2, leads[right], f1, g1);
	    if (horiz)
		interpPoint(point1, point2, posts[right], 1, g1);
	    pushLeadOutside(leads[i * 2], cx, cy, rim);
	    pushLeadOutside(leads[right], cx, cy, rim);
	}
    }

    void pushLeadOutside(Point lead, int cx, int cy, double rim) {
	double vx = lead.x - cx;
	double vy = lead.y - cy;
	double dist = Math.sqrt(vx * vx + vy * vy);
	if (dist < 1e-6) {
	    lead.x = cx;
	    lead.y = (int) Math.round(cy - rim);
	    return;
	}
	if (dist < rim) {
	    lead.x = (int) Math.round(cx + vx / dist * rim);
	    lead.y = (int) Math.round(cy + vy / dist * rim);
	}
    }

    void setPoints() {
	super.setPoints();
	clampLeadsToRim();
    }

    int getDumpType() { return 459; }

    String getXmlDumpType() { return "EGTDrehstrommotor"; }

    String dump() {
	return super.dump() + " " + Rs + " " + Rr + " " + Ls + " " + Lr + " "
		+ Lm + " " + b + " " + J + " "
		+ CustomLogicModel.escape(designation) + " " + ratedKw
		+ " " + eta + EGTNote.dumpSuffix(note);
    }

    void dumpXml(Document doc, Element elem) {
	super.dumpXml(doc, elem);
	XMLSerializer.dumpAttr(elem, "designation", designation);
	EGTNote.dumpXml(elem, note);
	XMLSerializer.dumpAttr(elem, "ratedKw", ratedKw);
	XMLSerializer.dumpAttr(elem, "eta", eta);
    }

    void undumpXml(XMLDeserializer xml) {
	super.undumpXml(xml);
	String d = xml.parseStringAttr("designation", designation);
	if (d != null && d.length() > 0)
	    designation = d;
	note = EGTNote.undumpXml(xml, note);
	double p = xml.parseDoubleAttr("ratedKw", 0);
	if (p >= P_MIN_KW)
	    ratedKw = clampRatedKw(p);
	else
	    inferRatedKw();
	double e = xml.parseDoubleAttr("eta", 0);
	if (e > 0)
	    eta = parseEtaToken(e);
	else
	    eta = DEF_ETA;
	applyDidacticMech();
    }

    /** True, wenn U1/V1/W1 noch an ein anderes Element angebunden ist. */
    boolean terminalHasExternal(int post) {
	if (nodes == null || post < 0 || post >= nodes.length)
	    return false;
	CircuitNode cn = nodes[post];
	if (cn == null || cn.links == null)
	    return false;
	int i;
	for (i = 0; i != cn.links.size(); i++) {
	    CircuitNodeLink ln = cn.links.get(i);
	    if (ln != null && ln.elm != null && ln.elm != this)
		return true;
	}
	return false;
    }

    void updatePhaseEnvelope() {
	double uv = Math.abs(volts[0] - volts[2]);
	double vw = Math.abs(volts[2] - volts[4]);
	double wu = Math.abs(volts[4] - volts[0]);
	/* getCurrentIntoNode = Statorzweigstrom (≈ Leiterstrom); bei offener
	   Klemme → 0. Peak-Hold nur für Anzeige; Live-Zählung über LIVE_HOLD. */
	double iu = Math.abs(getCurrentIntoNode(0));
	double iv = Math.abs(getCurrentIntoNode(2));
	double iw = Math.abs(getCurrentIntoNode(4));

	double t = (sim != null) ? sim.t : 0;
	double dt = (lastPhaseT < 0) ? 0 : (t - lastPhaseT);
	if (dt < 0)
	    dt = 0;
	if (dt > 0.2)
	    dt = 0.2;
	lastPhaseT = t;
	double decay = (dt <= 0) ? 1 : Math.exp(-dt / PHASE_TAU);
	envUV = (uv > envUV) ? uv : envUV * decay;
	envVW = (vw > envVW) ? vw : envVW * decay;
	envWU = (wu > envWU) ? wu : envWU * decay;
	envIu = (iu > envIu) ? iu : envIu * decay;
	envIv = (iv > envIv) ? iv : envIv * decay;
	envIw = (iw > envIw) ? iw : envIw * decay;

	double iMax = iu;
	if (iv > iMax)
	    iMax = iv;
	if (iw > iMax)
	    iMax = iw;
	double iGate = PHASE_I_MIN;
	if (iMax > PHASE_I_MIN * 2)
	    iGate = Math.max(PHASE_I_MIN, iMax * PHASE_I_REL);
	if (terminalHasExternal(0) && iu >= iGate)
	    lastLiveU = t;
	if (terminalHasExternal(2) && iv >= iGate)
	    lastLiveV = t;
	if (terminalHasExternal(4) && iw >= iGate)
	    lastLiveW = t;
    }

    boolean phaseLive(int post, double lastLive, double t) {
	/* Kein Sofort-Kill bei Draht weg — nur LIVE_HOLD (Simzeit → Speed-Slider). */
	return (t - lastLive) <= LIVE_HOLD;
    }

    /** Anzahl gespeister Außenleiter U1/V1/W1 (roh, für Auslaufbremse). */
    int livePhaseCount() {
	double t = (sim != null) ? sim.t : 0;
	int n = 0;
	if (phaseLive(0, lastLiveU, t))
	    n++;
	if (phaseLive(2, lastLiveV, t))
	    n++;
	if (phaseLive(4, lastLiveW, t))
	    n++;
	return n;
    }

    boolean threePhaseSupplied() {
	return livePhaseCount() >= 3;
    }

    /** Rohfehler erkannt, Anzeige aber erst nach FAIL_DISPLAY_DELAY (Simzeit). */
    boolean faultDisplayed() {
	double t = (sim != null) ? sim.t : 0;
	if (threePhaseSupplied()) {
	    phaseFaultSinceT = -1;
	    return false;
	}
	if (phaseFaultSinceT < 0)
	    phaseFaultSinceT = t;
	return (t - phaseFaultSinceT) >= FAIL_DISPLAY_DELAY;
    }

    boolean starNodesShared() {
	if (nodes == null || nodes.length < 6)
	    return false;
	return nodes[1] != null && nodes[1] == nodes[3] && nodes[3] == nodes[5];
    }

    boolean deltaNodesShared() {
	if (nodes == null || nodes.length < 6)
	    return false;
	return nodes[0] != null && nodes[0] == nodes[5]
		&& nodes[2] != null && nodes[2] == nodes[1]
		&& nodes[4] != null && nodes[4] == nodes[3];
    }

    boolean hasCurrent() {
	return (envIu + envIv + envIw) > PHASE_I_MIN;
    }

    /** Momentane Spreizung U2/V2/W2 (kein Peak-Hold — sonst hängt die Anzeige). */
    double starSpreadNow() {
	if (volts == null || volts.length < 6)
	    return 1e9;
	double s = Math.abs(volts[1] - volts[3]);
	double a = Math.abs(volts[3] - volts[5]);
	if (a > s)
	    s = a;
	a = Math.abs(volts[5] - volts[1]);
	if (a > s)
	    s = a;
	return s;
    }

    /** Momentane Spreizung der Δ-Paare U1–W2, V1–U2, W1–V2. */
    double deltaSpreadNow() {
	if (volts == null || volts.length < 6)
	    return 1e9;
	double s = Math.abs(volts[0] - volts[5]);
	double a = Math.abs(volts[2] - volts[1]);
	if (a > s)
	    s = a;
	a = Math.abs(volts[4] - volts[3]);
	if (a > s)
	    s = a;
	return s;
    }

    /** Stern: Drahtbrücke (gleicher Knoten) oder Schütz — sofort, ohne Hüllkurve. */
    boolean starLikely() {
	if (starNodesShared())
	    return true;
	return starSpreadNow() < STAR_SPREAD_MAX && hasCurrent();
    }

    boolean deltaLikely() {
	if (deltaNodesShared())
	    return true;
	return deltaSpreadNow() < STAR_SPREAD_MAX && hasCurrent();
    }

    void startIteration() {
	maybeMigrateSchoolScale();
	applyDidacticMech();
	updatePhaseEnvelope();
	super.startIteration();
	if (!threePhaseSupplied() && sim != null) {
	    double d = Math.exp(-COAST_BRAKE * sim.timeStep);
	    speed *= d;
	    filteredSpeed *= d;
	    if (Math.abs(speed) < 1e-3)
		speed = 0;
	    if (Math.abs(filteredSpeed) < 1e-3)
		filteredSpeed = 0;
	}
    }

    /**
     * +1 Rechtslauf, −1 Linkslauf, 0 kein Laufpfeil.
     * Synchron mit statusText(): erst nach faultDisplayed() weg.
     */
    int directionSign() {
	if (faultDisplayed()) {
	    dirLatched = false;
	    lastDirSign = 0;
	    return 0;
	}
	if (!threePhaseSupplied())
	    return lastDirSign;
	double fs = filteredSpeed;
	double mag = Math.abs(fs);
	if (!dirLatched) {
	    if (mag >= SPEED_ON)
		dirLatched = true;
	} else if (mag < SPEED_OFF) {
	    dirLatched = false;
	}
	if (!dirLatched) {
	    lastDirSign = 0;
	    return 0;
	}
	lastDirSign = (fs >= 0) ? 1 : -1;
	return lastDirSign;
    }

    /** Primärer Status — synchron mit directionSign() / faultDisplayed(). */
    String statusText() {
	if (faultDisplayed()) {
	    int n = livePhaseCount();
	    if (n == 0)
		return Locale.LS("Keine Versorgung");
	    return Locale.LS("Phasenausfall");
	}
	int s = directionSign();
	if (s > 0)
	    return Locale.LS("Rechtslauf");
	if (s < 0)
	    return Locale.LS("Linkslauf");
	return Locale.LS("Stillstand");
    }

    String wiringHintText() {
	if (deltaLikely())
	    return Locale.LS("Dreieck");
	if (starLikely())
	    return Locale.LS("Stern");
	return Locale.LS("offen");
    }

    void drawArcArrow(Graphics g, int cx, int cy, double rr, double aMid,
		      double span, boolean cw, Color color, double w) {
	double a0 = aMid - span / 2;
	double a1 = aMid + span / 2;
	g.setColor(color);
	g.setLineWidth(w);
	g.context.beginPath();
	if (cw)
	    g.context.arc(cx, cy, rr, a0, a1, false);
	else
	    g.context.arc(cx, cy, rr, a1, a0, true);
	g.context.stroke();

	double tipA = cw ? a1 : a0;
	double tang = tipA + (cw ? Math.PI / 2 : -Math.PI / 2);
	double tipX = cx + Math.cos(tipA) * rr;
	double tipY = cy + Math.sin(tipA) * rr;
	double ah = Math.max(7, rr * 0.26);
	double bx = tipX - Math.cos(tang) * ah;
	double by = tipY - Math.sin(tang) * ah;
	double nx = -Math.sin(tang) * ah * 0.45;
	double ny = Math.cos(tang) * ah * 0.45;
	g.context.beginPath();
	g.context.moveTo(tipX, tipY);
	g.context.lineTo(bx + nx, by + ny);
	g.context.lineTo(bx - nx, by - ny);
	g.context.closePath();
	g.context.fill();
	g.setLineWidth(1.0);
    }

    void drawArcIdle(Graphics g, int cx, int cy, double rr, double aMid,
		     double span) {
	g.setColor(COL_IDLE);
	g.setLineWidth(1.8);
	g.setLineDash(5, 4);
	g.context.beginPath();
	g.context.arc(cx, cy, rr, aMid - span / 2, aMid + span / 2, false);
	g.context.stroke();
	g.setLineDash(0, 0);
	g.setLineWidth(1.0);
    }

    void draw(Graphics g) {
	setBbox(point1, point2, cr);
	clampLeadsToRim();
	updatePhaseEnvelope();

	int i;
	for (i = 0; i != 6; i++) {
	    setVoltageColor(g, volts[i]);
	    drawThickLine(g, posts[i], leads[i]);
	}
	for (i = 0; i != 3; i++) {
	    curcounts[i] = updateDotCount(coilCurrents[i], curcounts[i]);
	    drawDots(g, posts[i * 2], leads[i * 2], curcounts[i]);
	    drawDots(g, leads[i * 2 + 1], posts[i * 2 + 1], curcounts[i]);
	}

	int cx = motorCenter.x;
	int cy = motorCenter.y;
	double r = cr;
	/* Laufbogen (blau) innen; Stillstand-Bogen ebenfalls innen. */
	double rr = r * 0.82;
	setBbox(point1, point2, cr);
	Color stroke = needsHighlight() ? selectColor : whiteColor;

	g.setColor(stroke);
	g.setLineWidth(2.5);
	g.context.setLineCap("butt");
	g.context.beginPath();
	g.context.arc(cx, cy, r, 0, 2 * Math.PI, false);
	g.context.stroke();
	g.setLineWidth(1.0);

	int dir = directionSign();
	double span = Math.PI * 0.55;
	if (dir != 0)
	    drawArcArrow(g, cx, cy, rr, angle, span, dir > 0, COL_ACCENT, 2.2);
	else
	    drawArcIdle(g, cx, cy, rr, -Math.PI / 2, span);

	int labelSize = Math.max(12, (int) Math.round(r * 0.44));
	int subSize = Math.max(10, (int) Math.round(labelSize * 0.68));
	g.setColor(stroke);
	g.context.setTextAlign("center");
	g.context.setTextBaseline("middle");
	g.setFont(new Font("normal", 0, labelSize));
	g.drawString("M", cx, (int) (cy - r * 0.08));
	g.setFont(new Font("normal", 0, subSize));
	g.drawString("3~", cx, (int) (cy + r * 0.32));
	g.context.setTextAlign("left");
	g.context.setTextBaseline("alphabetic");

	drawPosts(g);

	g.setColor(needsHighlight() ? selectColor : whiteColor);
	g.save();
	if (Math.abs(dy) > Math.abs(dx)) {
	    for (i = 0; i != 3; i++) {
		EGTStyle.drawPinLabelBeside(g, this, posts[i * 2].x,
			posts[i * 2].y, "UVW".substring(i, i + 1) + "1",
			whiteColor, true);
		EGTStyle.drawPinLabelBeside(g, this, posts[i * 2 + 1].x,
			posts[i * 2 + 1].y, "UVW".substring(i, i + 1) + "2",
			whiteColor, true);
	    }
	} else {
	    /* Waagerecht: gerade Zeilen; rechts W2/U2/V2. */
	    for (i = 0; i != 3; i++) {
		EGTStyle.drawPinLabelToward(g, this, posts[i * 2].x,
			posts[i * 2].y, cx, "UVW".substring(i, i + 1) + "1",
			whiteColor);
		EGTStyle.drawPinLabelToward(g, this, posts[i * 2 + 1].x,
			posts[i * 2 + 1].y, cx,
			"UVW".substring(i, i + 1) + "2", whiteColor);
	    }
	}
	g.restore();

	String stTxt = statusText();
	String wireTxt = wiringHintText();
	g.save();
	g.setColor(needsHighlight() ? selectColor : whiteColor);
	g.setFont(EGTStyle.pinLabelFont());
	int tw = (int) g.measureWidth(designation);
	tw = (int) Math.max(tw, g.measureWidth(stTxt));
	tw = (int) Math.max(tw, g.measureWidth(wireTxt));
	String ntxt = EGTNote.of(this);
	if (ntxt.length() > 0)
	    tw = (int) Math.max(tw, g.measureWidth(ntxt));
	/* Senkrecht: Text rechts (Klemmen oben/unten).
	   Waagerecht: Text über dem Kreis (Klemmen links/rechts frei). */
	if (Math.abs(dy) > Math.abs(dx)) {
	    g.context.setTextAlign("left");
	    int tx = cx + (int) r + 10;
	    int extra = EGTStyle.drawDesigAndNote(g, this, designation,
						 tx, cy - 14);
	    g.drawString(stTxt, tx, cy + 2 + extra);
	    g.drawString(wireTxt, tx, cy + 16 + extra);
	    adjustBbox(cx - (int) r - 4, cy - (int) r - 4,
		       tx + tw + 8, cy + (int) r + 8 + extra);
	} else {
	    g.context.setTextAlign("center");
	    /* Kreisoberkante frei lassen — letzte Zeile nicht auf dem Rand. */
	    int ty = cy - (int) r - 24;
	    int extra = EGTStyle.drawDesigAndNote(g, this, designation,
						 cx, ty - 26);
	    g.drawString(stTxt, cx, ty - 10 + extra);
	    g.drawString(wireTxt, cx, ty + 6 + extra);
	    adjustBbox(cx - tw / 2 - 4, ty - 42 - extra,
		       cx + tw / 2 + 4, cy + (int) r + 8);
	}
	g.restore();
	g.context.setTextAlign("left");
	g.context.setTextBaseline("alphabetic");

	filteredSpeed = filteredSpeed * .98 + speed * .02;
    }

    void getInfo(String arr[]) {
	arr[0] = Locale.LS("EGT-Drehstrommotor");
	getBasicInfo(arr);
	arr[3] = Locale.LS("Zustand") + " = " + statusText();
	arr[4] = Locale.LS("Klemmung") + " = " + wiringHintText();
	arr[5] = Locale.LS("Nennleistung") + " = "
		+ CircuitElm.getUnitText(ratedKw * 1000, Locale.LS("W"))
		+ ", η = " + (int) Math.round(clampEta(eta) * 100) + " %"
		+ ", In ≈ "
		+ CircuitElm.getCurrentText(schoolCurrentA());
	double iLine = envIu;
	if (envIv > iLine)
	    iLine = envIv;
	if (envIw > iLine)
	    iLine = envIw;
	arr[6] = Locale.LS("Leiterstrom") + " = "
		+ CircuitElm.getCurrentText(iLine);
	double rpm = 60 * Math.abs(filteredSpeed) / (2 * Math.PI);
	arr[7] = Locale.LS("speed") + " = "
		+ CircuitElm.getUnitText(rpm, Locale.LS("RPM"));
    }

    public EditInfo getEditInfo(int n) {
	EditInfo noteEi = EGTNote.editAt(this, n, 1);
	if (noteEi != null)
	    return noteEi;
	n = EGTNote.shiftAfter(n, 1);
	if (n == 0)
	    return new EditInfo("Bezeichnung", designation);
	if (n == 1)
	    return new EditInfo("Nennleistung (kW)", ratedKw, 0, 0)
		    .setPositive();
	if (n == 2)
	    return new EditInfo("Wirkungsgrad (%)", clampEta(eta) * 100, 0, 0)
		    .setPositive();
	if (n == 3)
	    return EditInfo.createCheckbox(Locale.LS("Erweiterte Funktionen"),
					  showAdvanced);
	if (!showAdvanced)
	    return null;
	if (n == 4)
	    return new EditInfo("Statorinduktivität (H)", Ls, 0, 0);
	if (n == 5)
	    return new EditInfo("Rotorinduktivität (H)", Lr, 0, 0);
	if (n == 6)
	    return new EditInfo("Kopplungsfaktor",
			Lm / Math.sqrt(Ls * Lr), 0, 0).setDimensionless();
	if (n == 7)
	    return new EditInfo("Statorwiderstand (Ohm)", Rs, 0, 0);
	if (n == 8)
	    return new EditInfo("Rotorwiderstand (Ohm)", Rr, 0, 0);
	if (n == 9)
	    return new EditInfo("Reibungskoeffizient (Nms/rad)", b, 0, 0);
	if (n == 10)
	    return new EditInfo("Trägheitsmoment (kg·m²)", J, 0, 0);
	return null;
    }

    public void setEditValue(int n, EditInfo ei) {
	if (EGTNote.applyAt(this, n, 1, ei))
	    return;
	n = EGTNote.shiftAfter(n, 1);
	if (n == 0) {
	    skipAdvancedThisApply = false;
	    designation = ei.textf.getText();
	    if (designation == null || designation.length() == 0)
		designation = "M1";
	    return;
	}
	if (n == 1 && ei.value > 0) {
	    double p = clampRatedKw(ei.value);
	    if (Math.abs(p - ratedKw) > 1e-6) {
		ratedKw = p;
		applyRatedPower();
		skipAdvancedThisApply = true;
	    }
	    return;
	}
	if (n == 2 && ei.value > 0) {
	    double e = parseEtaToken(ei.value);
	    if (Math.abs(e - eta) > 1e-4) {
		eta = e;
		applyRatedPower();
		skipAdvancedThisApply = true;
	    }
	    return;
	}
	if (n == 3 && ei.checkbox != null) {
	    showAdvanced = ei.checkbox.getState();
	    ei.newDialog = true;
	    return;
	}
	if (skipAdvancedThisApply)
	    return;
	if (ei.value > 0 && n == 4)
	    Ls = ei.value;
	if (ei.value > 0 && n == 5)
	    Lr = ei.value;
	if (ei.value > 0 && ei.value < 1 && n == 6)
	    Lm = ei.value * Math.sqrt(Ls * Lr);
	if (ei.value > 0 && n == 7)
	    Rs = ei.value;
	if (ei.value > 0 && n == 8)
	    Rr = ei.value;
	if (n == 9)
	    b = ei.value;
	if (ei.value > 0 && n == 10)
	    J = ei.value;
    }
}

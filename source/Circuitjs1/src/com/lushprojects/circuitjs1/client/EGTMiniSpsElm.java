/*
    TAR-Dervic EGT: Mini-SPS LOGO! Geräteansicht (MOD-LOG-01 / L404), Dump 466
    Optik 1:1 Symbol-Workbench drawMiniSpsDevice; Q = echte Schließer 1–2.
*/

package com.lushprojects.circuitjs1.client;

import com.google.gwt.user.client.ui.Button;
import com.lushprojects.circuitjs1.client.util.Locale;

/**
 * Mini-SPS Geräteansicht: Eingänge L1/N/I1…I8 oben, Ausgänge Qn (1/2) unten.
 * Varianten 8I/4Q und 8I/8Q. L1/N = Versorgung; I = 230-V-Pegel.
 * qMask kommt von FUP-Operanden Q. RUN/STOP-Tasten = Betriebsart.
 */
class EGTMiniSpsElm extends ChipElm implements EGTDesignatable {
    static final int N_IN = 8;
    static final int N_TOP = 2 + N_IN; // L1, N, I1…I8
    static final int VARIANT_4Q = 0;
    static final int VARIANT_8Q = 1;
    /** Höher als Workbench-rows=7: Platz für Eingänge + Ausgänge ohne Q/1/2-Überlappung. */
    static final int DEF_SIZE_Y = 11;
    static final int SIZE_X_4Q = 12;
    static final int SIZE_X_8Q = 16;
    static final double R_ON = EGTSchuetzLink.R_ON;
    /** Interne Versorgung ~3 W bei 230 V. Ohne N folgt N an L1, |u|≈0. */
    static final double R_SUPPLY = 230 * 230 / 3.0;

    int variant = VARIANT_4Q;
    int qMask = 0;
    int iMask = 0;
    /** LCD-I: Klemme oder FUP-I-Klick (Prozessabbild). */
    int lcdIMask = 0;
    boolean supplyOk;
    boolean run;
    /** Bedientaste RUN/STOP; Default RUN. Versorgung L1/N bleibt Pflicht. */
    boolean runEnable = true;
    double qCurrent[];
    double qCurCount[];
    EGTAcLevelHold supplyHold = new EGTAcLevelHold();
    EGTAcLevelHold iHold[] = new EGTAcLevelHold[N_IN];
    /** IEC 81346: A = allgemeines Betriebsmittel / SPS. Q1–Q8 bleiben Ausgänge. */
    String designation = "A1";
    String note = "";

    public String egtDesignation() { return designation; }
    public void setEgtDesignation(String d) {
	if (d != null)
	    designation = d;
    }
    public String egtNote() { return note; }
    public void setEgtNote(String s) { note = EGTNote.clamp(s); }

    public EGTMiniSpsElm(int xx, int yy) {
	super(xx, yy);
	setSize(2);
	applyVariantSize();
	layoutPins();
	syncEndpoints();
	allocQArrays();
	allocIHolds();
    }

    public EGTMiniSpsElm(int xa, int ya, int xb, int yb, int f,
			 StringTokenizer st) {
	super(xa, ya, xb, yb, f, st);
	setSize(2);
	if (st.hasMoreTokens()) {
	    try {
		sizeX = Integer.parseInt(st.nextToken());
		if (st.hasMoreTokens())
		    sizeY = Integer.parseInt(st.nextToken());
		if (st.hasMoreTokens())
		    variant = Integer.parseInt(st.nextToken());
		if (st.hasMoreTokens())
		    qMask = Integer.parseInt(st.nextToken());
		if (st.hasMoreTokens())
		    runEnable = Integer.parseInt(st.nextToken()) != 0;
		if (st.hasMoreTokens())
		    designation = CustomLogicModel.unescape(st.nextToken());
	    } catch (Exception e) {
	    }
	}
	if (variant != VARIANT_8Q)
	    variant = VARIANT_4Q;
	applyVariantSize();
	layoutPins();
	syncEndpoints();
	allocQArrays();
	allocIHolds();
    
	note = EGTNote.readOptional(st);
}

    void allocIHolds() {
	for (int i = 0; i < N_IN; i++) {
	    if (iHold[i] == null)
		iHold[i] = new EGTAcLevelHold();
	}
    }

    int getIMask() { return iMask; }
    int getLcdIMask() { return lcdIMask; }
    boolean isRun() { return run; }

    void applyForcedI(int bits) {
	lcdIMask = iMask;
	if (supplyOk)
	    lcdIMask |= bits & ((1 << N_IN) - 1);
    }

    void applyProgrammedQ(int bits) {
	if (!run)
	    bits = 0;
	qMask = bits & ((1 << nOut()) - 1);
    }

    /**
     * I-Klemme High bei 230 V gegen N; Versorgung nur bei geschlossenem L1–N.
     */
    void senseAcLevels(double dt) {
	allocIHolds();
	if (volts == null || volts.length < N_TOP) {
	    run = false;
	    supplyOk = false;
	    iMask = 0;
	    lcdIMask = 0;
	    return;
	}
	double vN = volts[1];
	supplyOk = supplyHold.update(volts[0] - vN, dt);
	run = supplyOk && runEnable;
	iMask = 0;
	lcdIMask = 0;
	if (!supplyOk) {
	    for (int i = 0; i < N_IN; i++)
		iHold[i].reset();
	    return;
	}
	for (int i = 0; i < N_IN; i++) {
	    if (iHold[i].update(volts[2 + i] - vN, dt))
		iMask |= 1 << i;
	}
	lcdIMask = iMask;
    }

    void allocQArrays() {
	int n = nOut();
	qCurrent = new double[n];
	qCurCount = new double[n];
    }

    int nOut() { return variant == VARIANT_8Q ? 8 : 4; }

    void applyVariantSize() {
	sizeX = variant == VARIANT_8Q ? SIZE_X_8Q : SIZE_X_4Q;
	sizeY = DEF_SIZE_Y;
    }

    boolean isDigitalChip() { return false; }
    String getChipName() { return "EGT-Mini-SPS"; }

    int getPostCount() { return N_TOP + 2 * nOut(); }
    int getVoltageSourceCount() { return 0; }

    int qPin1(int qi) { return N_TOP + qi * 2; }
    int qPin2(int qi) { return N_TOP + qi * 2 + 1; }

    boolean qClosed(int qi) {
	return (qMask & (1 << qi)) != 0;
    }

    void setQClosed(int qi, boolean on) {
	if (on)
	    qMask |= (1 << qi);
	else
	    qMask &= ~(1 << qi);
    }

    void toggleQ(int qi) {
	if (qi < 0 || qi >= nOut())
	    return;
	setQClosed(qi, !qClosed(qi));
    }

    void setupPins() {
	applyVariantSize();
	pins = new Pin[getPostCount()];
	String[] topLabs = new String[N_TOP];
	topLabs[0] = "L1";
	topLabs[1] = "N";
	for (int i = 1; i <= N_IN; i++)
	    topLabs[1 + i] = "I" + i;
	for (int i = 0; i < N_TOP; i++) {
	    int pos;
	    if (i < 2)
		pos = i;
	    else
		pos = 3 + (i - 2);
	    pins[i] = new Pin(clampPos(pos), SIDE_N, topLabs[i]);
	    pins[i].output = false;
	    pins[i].state = false;
	}
	int nOut = nOut();
	for (int qi = 0; qi < nOut; qi++) {
	    double slot = (sizeX - 1) / (double) nOut;
	    double cx = (qi + 0.5) * slot;
	    double dx = Math.max(0.4, slot * 0.28);
	    int p1 = clampPos((int) Math.round(cx - dx));
	    int p2 = clampPos((int) Math.round(cx + dx));
	    if (p2 <= p1)
		p2 = Math.min(sizeX - 1, p1 + 1);
	    pins[qPin1(qi)] = new Pin(p1, SIDE_S, "1");
	    pins[qPin2(qi)] = new Pin(p2, SIDE_S, "2");
	    pins[qPin1(qi)].output = false;
	    pins[qPin2(qi)].output = false;
	    pins[qPin1(qi)].state = false;
	    pins[qPin2(qi)].state = false;
	}
    }

    int clampPos(int p) {
	if (p < 0)
	    return 0;
	if (p > sizeX - 1)
	    return sizeX - 1;
	return p;
    }

    void layoutPins() {
	if (pins == null || pins.length != getPostCount())
	    setupPins();
	else {
	    // refresh sides/pos after variant change
	    setupPins();
	}
    }

    void syncEndpoints() {
	if (cspc2 < 1)
	    setSize(2);
	x2 = x + sizeX * cspc2;
	y2 = y + sizeY * cspc2;
	setPoints();
    }

    void drag(int xx, int yy) {
	xx = snapGrid(xx);
	yy = snapGrid(yy);
	if (cspc2 < 1)
	    setSize(2);
	applyVariantSize();
	layoutPins();
	x2 = x + sizeX * cspc2;
	y2 = y + sizeY * cspc2;
	setPoints();
    }

    boolean creationFailed() {
	return sizeX < SIZE_X_4Q || sizeY < DEF_SIZE_Y;
    }

    int getLeadPost(int px, int py) {
	return -1;
    }

    /** STOP links, RUN rechts — gleiche Geometrie wie drawChip. */
    void keypadStopRunRects(int stop[], int runBtn[]) {
	int[] b = new int[4];
	boxBounds(b);
	int left = b[0], top = b[1], right = b[2];
	int bodyW = right - left;
	int bodyH = b[3] - top;
	int[] bands = new int[3];
	bandLayout(bodyH, bands);
	int uiBand = bands[2];
	int yUi = top + bands[0];
	int uiPadX = (int) (bodyW * 0.03);
	int uiPadY = (int) (uiBand * 0.14);
	int uiInnerY = yUi + uiPadY;
	int uiInnerH = uiBand - 2 * uiPadY;
	int dispW = (int) (bodyW * 0.55);
	int keyX = left + uiPadX + dispW + (int) (uiPadX * 1.2);
	int keyW = right - uiPadX - keyX;
	int botH = Math.max(8, uiInnerH * 17 / 100);
	int halfW = keyW * 38 / 100;
	int gapB = keyW * 8 / 100;
	int botY = uiInnerY + uiInnerH - botH;
	int x0 = keyX + keyW * 8 / 100;
	stop[0] = x0;
	stop[1] = botY;
	stop[2] = halfW;
	stop[3] = botH;
	runBtn[0] = x0 + halfW + gapB;
	runBtn[1] = botY;
	runBtn[2] = halfW;
	runBtn[3] = botH;
    }

    boolean hitRect(int mx, int my, int r[]) {
	return mx >= r[0] && my >= r[1] && mx <= r[0] + r[2] && my <= r[1] + r[3];
    }

    boolean toggleAt(int mx, int my) {
	int[] stop = new int[4];
	int[] runBtn = new int[4];
	keypadStopRunRects(stop, runBtn);
	if (hitRect(mx, my, stop)) {
	    runEnable = false;
	    applyProgrammedQ(0);
	    return true;
	}
	if (hitRect(mx, my, runBtn)) {
	    runEnable = true;
	    return true;
	}
	return false;
    }

    void setPinXY(int n, int px, int py) {
	int gx = snapGrid(px);
	int gy = snapGrid(py);
	pins[n].post = new Point(gx, gy);
	pins[n].stub = new Point(gx, gy);
    }

    /** Erster und letzter Punkt fest, dazwischen gleiche Rasterlücken (CSS space-between). */
    int gridLerp(int first, int last, int i, int n) {
	if (n <= 1)
	    return snapGrid(first);
	return snapGrid(first + (int) ((last - first) * i / (double) (n - 1)));
    }

    void setPoints() {
	super.setPoints();
	if (cspc < 1 || pins == null)
	    return;
	int[] b = new int[4];
	boxBounds(b);
	int left = b[0], top = b[1], right = b[2], bottom = b[3];
	int gs = cspc;
	int pad = Math.max(gs, (int) ((right - left) * 0.035));
	int ty = snapGrid(top);
	int lnPitch = Math.max(gs, cspc2);
	int l1 = snapGrid(left + pad + gs);
	setPinXY(0, l1, ty);
	setPinXY(1, l1 + lnPitch, ty);
	int iFirst = snapGrid(l1 + 3 * lnPitch);
	int iLast = snapGrid(right - pad);
	int iPitch = ((iLast - iFirst) / (N_IN - 1) / gs) * gs;
	if (iPitch < gs)
	    iPitch = gs;
	for (int i = 0; i < N_IN; i++)
	    setPinXY(2 + i, iFirst + i * iPitch, ty);
	int nOut = nOut();
	int dxQ = gs;
	int by = snapGrid(bottom);
	if (nOut > 4) {
	    int qPad = Math.max(gs, (int) ((right - left) * 0.04));
	    int qFirst = snapGrid(left + qPad + dxQ);
	    int qLast = snapGrid(right - qPad - dxQ);
	    for (int qi = 0; qi < nOut; qi++) {
		int qx = gridLerp(qFirst, qLast, qi, nOut);
		setPinXY(qPin1(qi), qx - dxQ, by);
		setPinXY(qPin2(qi), qx + dxQ, by);
	    }
	} else {
	    int innerL = left + gs;
	    int innerW = right - gs - innerL;
	    for (int qi = 0; qi < nOut; qi++) {
		int qx = snapGrid(innerL + (int) (innerW * (qi + 0.5) / nOut));
		setPinXY(qPin1(qi), qx - dxQ, by);
		setPinXY(qPin2(qi), qx + dxQ, by);
	    }
	}
	setBbox(left - 4, top - 6, right + 4, bottom + 6);
    }

    void boxBounds(int out[]) {
	int xr = x + cspc2 - cspc;
	int yr = y - cspc;
	out[0] = xr;
	out[1] = yr;
	out[2] = xr + sizeX * cspc2;
	out[3] = yr + sizeY * cspc2;
    }

    boolean getConnection(int n1, int n2) {
	if ((n1 == 0 && n2 == 1) || (n1 == 1 && n2 == 0))
	    return true;
	int nOut = nOut();
	for (int qi = 0; qi < nOut; qi++) {
	    if (!qClosed(qi))
		continue;
	    int a = qPin1(qi), b = qPin2(qi);
	    if ((n1 == a && n2 == b) || (n1 == b && n2 == a))
		return true;
	}
	return false;
    }

    /**
     * Q-Schließer sind potentialfrei (1–2). getConnection folgt dem Kontakt.
     * getMatrixConnection bleibt für das Paar, damit R_ON beim Anzug in
     * derselben Matrix liegt (sonst LabeledNode 0 V trotz qMask).
     */
    boolean getMatrixConnection(int n1, int n2) {
	if ((n1 == 0 && n2 == 1) || (n1 == 1 && n2 == 0))
	    return true;
	int nOut = nOut();
	for (int qi = 0; qi < nOut; qi++) {
	    int a = qPin1(qi), b = qPin2(qi);
	    if ((n1 == a && n2 == b) || (n1 == b && n2 == a))
		return true;
	}
	return false;
    }

    boolean nonLinear() { return true; }

    void stamp() {
	sim.stampResistor(nodes[0], nodes[1], R_SUPPLY);
	int nOut = nOut();
	for (int qi = 0; qi < nOut; qi++) {
	    sim.stampNonLinear(nodes[qPin1(qi)]);
	    sim.stampNonLinear(nodes[qPin2(qi)]);
	}
    }

    void startIteration() {
	EGTMiniSpsLink.refresh(sim);
    }
    void execute() {}

    void reset() {
	super.reset();
	iMask = 0;
	lcdIMask = 0;
	supplyOk = false;
	run = false;
	supplyHold.reset();
	allocIHolds();
	for (int i = 0; i < N_IN; i++)
	    iHold[i].reset();
    }

    void doStep() {
	int nOut = nOut();
	for (int qi = 0; qi < nOut; qi++) {
	    if (qClosed(qi))
		sim.stampResistor(nodes[qPin1(qi)], nodes[qPin2(qi)], R_ON);
	    else {
		EGTStyle.stampOpenRef(sim, nodes[qPin1(qi)]);
		EGTStyle.stampOpenRef(sim, nodes[qPin2(qi)]);
	    }
	}
    }

    void calculateCurrent() {
	int nOut = nOut();
	double iSup = (volts[0] - volts[1]) / R_SUPPLY;
	current = iSup;
	if (pins != null && pins.length > 1) {
	    pins[0].current = -iSup;
	    pins[1].current = iSup;
	    for (int i = 2; i < N_TOP && i < pins.length; i++)
		pins[i].current = 0;
	}
	for (int qi = 0; qi < nOut; qi++) {
	    if (!qClosed(qi))
		qCurrent[qi] = 0;
	    else
		qCurrent[qi] = (volts[qPin1(qi)] - volts[qPin2(qi)]) / R_ON;
	    if (pins != null) {
		int a = qPin1(qi), b = qPin2(qi);
		if (a < pins.length)
		    pins[a].current = -qCurrent[qi];
		if (b < pins.length)
		    pins[b].current = qCurrent[qi];
	    }
	}
    }

    void bandLayout(int bodyH, int out[]) {
	int topBand = Math.max((int) (bodyH * 0.18), cspc2 * 2);
	int outBand = Math.max((int) (cspc2 * 2.6), (int) (bodyH * 0.34));
	int uiBand = bodyH - topBand - outBand;
	if (uiBand < cspc2 * 2) {
	    outBand = Math.max(cspc2 * 2, bodyH * 30 / 100);
	    topBand = Math.max(cspc2 * 2, bodyH / 5);
	    uiBand = bodyH - topBand - outBand;
	}
	out[0] = topBand;
	out[1] = outBand;
	out[2] = uiBand;
    }

    void drawChip(Graphics g) {
	g.save();
	try {
	    int[] b = new int[4];
	    boxBounds(b);
	    int left = b[0], top = b[1], right = b[2], bottom = b[3];
	    int bodyW = right - left;
	    int bodyH = bottom - top;
	    Color stroke = needsHighlight() ? selectColor : lightGrayColor;
	    Color labelCol = whiteColor;

	    int[] bands = new int[3];
	    bandLayout(bodyH, bands);
	    int topBand = bands[0], outBand = bands[1], uiBand = bands[2];
	    int yUi = top + topBand;
	    int yOut = top + topBand + uiBand;

	    // Gehäuse
	    g.setColor(new Color(12, 12, 12));
	    g.fillRect(left, top, bodyW, bodyH);
	    g.setColor(stroke);
	    g.setLineWidth(2.4);
	    g.drawRect(left, top, bodyW, bodyH);
	    g.setLineWidth(1.35);
	    g.drawLine(left, yUi, right, yUi);
	    g.drawLine(left, yOut, right, yOut);

	    // Eingänge: Schrauben auf Oberkante, Labels darunter
	    int fsTop = Math.max(9, 8 * csize);
	    int labTopY = top + Math.max(fsTop + 6, cspc2 * 42 / 100);
	    int infoY = Math.min(yUi - 4, labTopY + fsTop + 4);
	    if (infoY <= labTopY + 2)
		infoY = labTopY;
	    g.setFont(new Font("normal", 0, fsTop));
	    g.setColor(labelCol);
	    for (int i = 0; i < N_TOP; i++) {
		Point p = pins[i].post;
		String t = pins[i].text;
		int tw = (int) g.context.measureText(t).getWidth();
		g.drawString(t, p.x - tw / 2, labTopY);
	    }
	    if (infoY > labTopY + 2) {
		int twL1 = (int) g.context.measureText(pins[0].text).getWidth();
		int acLeft = pins[0].post.x - twL1 / 2 -8;
		int midIn = (pins[2].post.x + pins[N_TOP - 1].post.x) / 2;
		String ac = "AC 230 V";
		String ein = "Eingänge " + N_IN + " × AC";
		String extraIn = EGTMiniSpsLink.extraLabel(designation);
		if (extraIn.length() > 0)
		    ein = "Eingänge " + extraIn + " · " + N_IN + " × AC";
		int twEin = (int) g.context.measureText(ein).getWidth();
		g.drawString(ac, acLeft, infoY);
		g.drawString(ein, midIn - twEin / 2, infoY);
	    }

	    // UI: Display + Tasten
	    int uiPadX = (int) (bodyW * 0.03);
	    int uiPadY = (int) (uiBand * 0.14);
	    int uiInnerY = yUi + uiPadY;
	    int uiInnerH = uiBand - 2 * uiPadY;
	    int dispW = (int) (bodyW * 0.55);
	    int dispX = left + uiPadX;
	    int keyX = dispX + dispW + (int) (uiPadX * 1.2);
	    int keyW = right - uiPadX - keyX;

	    g.setColor(new Color(46, 154, 58));
	    g.fillRect(dispX, uiInnerY, dispW, uiInnerH);
	    g.setColor(stroke);
	    g.setLineWidth(1.5);
	    g.drawRect(dispX, uiInnerY, dispW, uiInnerH);
	    drawLcdStatus(g, dispX, uiInnerY, dispW, uiInnerH);

	    int escH = Math.max(8, uiInnerH * 17 / 100);
	    int botH = Math.max(8, uiInnerH * 17 / 100);
	    int midTop = uiInnerY + escH + uiInnerH * 7 / 100;
	    int midBot = uiInnerY + uiInnerH - botH - uiInnerH * 7 / 100;
	    int midH = midBot - midTop;
	    int midCy = midTop + midH / 2;
	    int midCx = keyX + keyW / 2;

	    g.setColor(new Color(91, 158, 201));
	    g.setLineWidth(1.15);
	    g.fillRect(keyX + keyW * 8 / 100, uiInnerY, keyW * 84 / 100, escH);
	    g.setColor(stroke);
	    g.drawRect(keyX + keyW * 8 / 100, uiInnerY, keyW * 84 / 100, escH);
	    if (designation != null && designation.length() > 0) {
		g.setFont(EGTStyle.pinLabelFont());
		g.setColor(whiteColor);
		g.context.save();
		g.context.setTextAlign("center");
		g.context.setTextBaseline("middle");
		g.drawString(designation,
			     keyX + keyW / 2, uiInnerY + escH / 2);
		g.context.restore();
	    }

	    int arm = (int) (Math.min(keyW, midH) * 0.26);
	    int gap = Math.max(5, arm * 70 / 100);
	    g.setColor(new Color(192, 57, 43));
	    fillTri(g, midCx, midCy - gap - arm, midCx - arm * 62 / 100,
		    midCy - gap, midCx + arm * 62 / 100, midCy - gap);
	    fillTri(g, midCx, midCy + gap + arm, midCx - arm * 62 / 100,
		    midCy + gap, midCx + arm * 62 / 100, midCy + gap);
	    fillTri(g, midCx - gap - arm, midCy, midCx - gap,
		    midCy - arm * 62 / 100, midCx - gap, midCy + arm * 62 / 100);
	    fillTri(g, midCx + gap + arm, midCy, midCx + gap,
		    midCy - arm * 62 / 100, midCx + gap, midCy + arm * 62 / 100);

	    int[] stopR = new int[4];
	    int[] runR = new int[4];
	    keypadStopRunRects(stopR, runR);
	    Color idleKey = new Color(91, 158, 201);
	    Color stopOn = new Color(192, 57, 43);
	    Color runOn = new Color(46, 154, 58);
	    drawModeKey(g, stopR, "STOP", !runEnable, stopOn, idleKey, stroke);
	    drawModeKey(g, runR, "RUN", runEnable, runOn, idleKey, stroke);

	    // Ausgänge — Fachkunde-Schließer (Knick + ~16°-Arm), kurze Stiele
	    int outHeaderH = Math.max(18, outBand * 22 / 100);
	    int fsAus = Math.max(10, 9 * csize);
	    g.setFont(new Font("normal", 0, fsAus));
	    g.setColor(labelCol);
	    String aus = "Ausgänge";
	    String extra = EGTMiniSpsLink.extraLabel(designation);
	    if (extra.length() > 0)
		aus = "Ausgänge " + extra;
	    int twAus = (int) g.context.measureText(aus).getWidth();
	    g.drawString(aus, left + bodyW / 2 - twAus / 2,
			 yOut + Math.max(fsAus, outHeaderH * 55 / 100));
	    g.setColor(stroke);
	    g.setLineWidth(1.25);
	    g.drawLine(left, yOut + outHeaderH, right, yOut + outHeaderH);

	    int nOut = nOut();
	    int contentH = outBand - outHeaderH;
	    int stemH = Math.max(12, Math.min(cspc2 * 32 / 100, contentH * 42 / 100));
	    int span = Math.abs(pins[qPin2(0)].post.x - pins[qPin1(0)].post.x);
	    if (span < cspc)
		span = 2 * cspc;
	    // Fachkunde: kurzer Knick, lange flache Schräge, Spitze über Stub
	    int elbowQ = Math.max(3, span * 10 / 100);
	    int stubW = Math.max(5, span * 18 / 100);
	    int gapQ = Math.max(2, span * 5 / 100);

	    for (int qi = 0; qi < nOut; qi++) {
		int t1x = pins[qPin1(qi)].post.x;
		int t2x = pins[qPin2(qi)].post.x;
		int qx = (t1x + t2x) / 2;
		int termY = pins[qPin1(qi)].post.y;
		boolean closed = qClosed(qi);
		String extraQ = EGTMiniSpsLink.extraLabel(designation);
		drawLogoOutputChannel(g, qx, "Q" + (qi + 1), extraQ, t1x, t2x,
				      termY, stemH, elbowQ, stubW, gapQ, closed,
				      stroke);
		if (closed) {
		    qCurCount[qi] = updateDotCount(qCurrent[qi], qCurCount[qi]);
		    int contactY = termY - stemH;
		    Point p1 = pins[qPin1(qi)].post;
		    Point p2 = pins[qPin2(qi)].post;
		    Point c1 = new Point(t1x, contactY);
		    Point c2 = new Point(t2x, contactY);
		    drawDots(g, p1, c1, qCurCount[qi]);
		    drawDots(g, c1, c2, qCurCount[qi]);
		    drawDots(g, c2, p2, qCurCount[qi]);
		}
	    }

	    drawPosts(g);
	    int screwR = Math.max(4, (int) (cspc2 * 0.13));
	    for (int i = 0; i < N_TOP; i++)
		drawScrewTerminal(g, pins[i].post.x, pins[i].post.y, screwR);
	    for (int qi = 0; qi < nOut; qi++) {
		drawScrewTerminal(g, pins[qPin1(qi)].post.x,
				  pins[qPin1(qi)].post.y, screwR);
		drawScrewTerminal(g, pins[qPin2(qi)].post.x,
				  pins[qPin2(qi)].post.y, screwR);
	    }
	    g.setLineWidth(1.0);
	} finally {
	    g.restore();
	}
    }

    void drawLcdStatus(Graphics g, int x, int y, int w, int h) {
	int pad = Math.max(3, w * 3 / 100);
	Color on = new Color(220, 255, 180);
	Color off = new Color(18, 72, 32);
	Color txt = new Color(12, 40, 14);
	Color dim = new Color(8, 48, 18);
	int barH = Math.max(13, h * 26 / 100);
	g.setColor(run ? on : dim);
	g.fillRect(x + 1, y + 1, w - 2, barH - 1);
	int fsBar = Math.max(10, Math.min(15, barH * 72 / 100));
	g.setFont(new Font("bold", 0, fsBar));
	g.setColor(run ? txt : on);
	g.context.save();
	g.context.setTextAlign("center");
	g.context.setTextBaseline("middle");
	g.drawString(run ? "RUN" : "STOP", x + w / 2, y + barH / 2);
	g.context.restore();

	int restY = y + barH + 1;
	int restH = h - barH - pad;
	int rowH = Math.max(10, restH / 2 - 1);
	int innerX = x + pad;
	int innerW = w - 2 * pad;
	drawLcdBitRow(g, innerX, restY, innerW, rowH, "I", N_IN, lcdIMask,
		      on, off, txt);
	drawLcdBitRow(g, innerX, restY + rowH + 1, innerW, rowH, "Q", nOut(),
		      qMask, on, off, txt);
    }

    void drawLcdBitRow(Graphics g, int x, int y, int w, int h, String lab,
		       int n, int mask, Color on, Color off, Color txt) {
	int fsLab = Math.max(8, Math.min(12, h * 55 / 100));
	g.setFont(new Font("bold", 0, fsLab));
	g.setColor(txt);
	g.context.save();
	g.context.setTextAlign("left");
	g.context.setTextBaseline("middle");
	g.drawString(lab, x, y + h / 2);
	int labW = Math.max(12, (int) g.context.measureText(lab).getWidth() + 5);
	int gap = 2;
	int box = Math.max(8, Math.min(h - 2, (w - labW - gap * N_IN) / N_IN));
	int by = y + (h - box) / 2;
	int fsN = Math.max(6, Math.min(10, box * 62 / 100));
	g.setFont(new Font("bold", 0, fsN));
	g.context.setTextAlign("center");
	for (int i = 0; i < n; i++) {
	    boolean hi = (mask & (1 << i)) != 0;
	    int bx = x + labW + i * (box + gap);
	    g.setColor(hi ? on : off);
	    g.fillRect(bx, by, box, box);
	    g.setColor(hi ? txt : new Color(70, 130, 80));
	    g.drawRect(bx, by, box, box);
	    g.drawString(String.valueOf(i + 1), bx + box / 2, by + box / 2);
	}
	g.context.restore();
    }

    void fillTri(Graphics g, int x1, int y1, int x2, int y2, int x3, int y3) {
	g.context.beginPath();
	g.context.moveTo(x1, y1);
	g.context.lineTo(x2, y2);
	g.context.lineTo(x3, y3);
	g.context.closePath();
	g.context.fill();
    }

    void drawModeKey(Graphics g, int r[], String lab, boolean on,
		     Color onCol, Color offCol, Color stroke) {
	g.setColor(on ? onCol : offCol);
	g.setLineWidth(1.15);
	g.fillRect(r[0], r[1], r[2], r[3]);
	g.setColor(stroke);
	g.drawRect(r[0], r[1], r[2], r[3]);
	int fs = Math.max(7, Math.min(11, r[3] * 70 / 100));
	g.setFont(new Font("bold", 0, fs));
	g.setColor(whiteColor);
	g.context.save();
	g.context.setTextAlign("center");
	g.context.setTextBaseline("middle");
	g.drawString(lab, r[0] + r[2] / 2, r[1] + r[3] / 2);
	g.context.restore();
    }

    void drawScrewTerminal(Graphics g, int cx, int cy, int r) {
	g.setColor(new Color(196, 120, 58));
	g.fillOval(cx - r, cy - r, 2 * r, 2 * r);
	g.setColor(lightGrayColor);
	g.setLineWidth(1.15);
	g.context.beginPath();
	g.context.arc(cx, cy, r, 0, 2 * Math.PI);
	g.context.stroke();
	g.setColor(new Color(26, 26, 26));
	int d = Math.max(2, (r * 5 + 5) / 10);
	g.setLineWidth(1.2);
	g.context.setLineCap("butt");
	g.drawLine(cx - d, cy - d, cx + d, cy + d);
    }

    /**
     * Fachkunde-Schließer (LOGO-Anschlussplan):
     * Stiel → kurzer Knick → eine gerade Schräge (~17°), Spitze über dem Stub an 2.
     */
    void drawLogoOutputChannel(Graphics g, int qx, String qLabel, String extra,
			       int t1x, int t2x, int termY, int stemH, int elbow,
			       int stubW, int gap, boolean closed,
			       Color stroke) {
	int contactY = termY - stemH;
	int stubLeft = t2x - stubW;
	int bladeTipX = stubLeft - gap;
	int run = bladeTipX - (t1x + elbow);
	if (run < 8)
	    run = 8;
	int rise = (int) Math.round(run * 0.30);
	int bladeTipY = contactY - rise;
	int labY = contactY - Math.max(3, cspc2 * 8 / 100);
	int sideOff = Math.max(6, (int) (cspc2 * 0.16));

	Color qCol = closed ? new Color(111, 207, 151) : whiteColor;
	boolean compactQ = nOut() >= 8;
	int fsQ = compactQ ? 11 : Math.max(11, 10 * csize);
	g.setFont(new Font("normal", 0, fsQ));
	g.setColor(qCol);
	int tw = (int) g.context.measureText(qLabel).getWidth();
	int qLabY = bladeTipY - Math.max(compactQ ? 7 : 9,
					 cspc2 * (compactQ ? 16 : 24) / 100);
	g.drawString(qLabel, qx - tw / 2, qLabY);
	if (extra != null && extra.length() > 0) {
	    g.setFont(EGTStyle.pinLabelFont());
	    int etw = (int) g.context.measureText(extra).getWidth();
	    g.drawString(extra, qx - etw / 2, qLabY - fsQ - 1);
	}

	g.setColor(closed ? qCol : stroke);
	g.setLineWidth(closed ? 2.4 : 2.1);
	g.context.setLineCap("butt");
	g.context.setLineJoin("miter");
	g.drawLine(t1x, termY, t1x, contactY);
	g.drawLine(t2x, termY, t2x, contactY);
	if (closed) {
	    g.context.beginPath();
	    g.context.moveTo(t1x, contactY);
	    g.context.lineTo(t2x, contactY);
	    g.context.stroke();
	} else {
	    g.context.beginPath();
	    g.context.moveTo(t1x, contactY);
	    g.context.lineTo(t1x + elbow, contactY);
	    g.context.lineTo(bladeTipX, bladeTipY);
	    g.context.stroke();
	    g.drawLine(stubLeft, contactY, t2x, contactY);
	}

	g.setFont(new Font(compactQ ? "normal" : "bold", 0,
			   compactQ ? 10 : Math.max(10, 9 * csize)));
	g.setColor(whiteColor);
	int tw1 = (int) g.context.measureText("1").getWidth();
	int tw2 = (int) g.context.measureText("2").getWidth();
	g.drawString("1", t1x - sideOff - tw1 / 2, labY);
	g.drawString("2", t2x + sideOff - tw2 / 2, labY);
    }

    String dump() {
	return super.dump() + " " + sizeX + " " + sizeY + " " + variant + " "
		+ qMask + " " + (runEnable ? 1 : 0) + " "
		+ CustomLogicModel.escape(designation) + EGTNote.dumpSuffix(note);
    }

    void dumpXml(com.google.gwt.xml.client.Document doc,
		 com.google.gwt.xml.client.Element elem) {
	super.dumpXml(doc, elem);
	XMLSerializer.dumpAttr(elem, "sx", sizeX);
	XMLSerializer.dumpAttr(elem, "sy", sizeY);
	XMLSerializer.dumpAttr(elem, "var", variant);
	XMLSerializer.dumpAttr(elem, "qm", qMask);
	XMLSerializer.dumpAttr(elem, "re", runEnable ? 1 : 0);
	XMLSerializer.dumpAttr(elem, "des", designation);
	EGTNote.dumpXml(elem, note);
    }

    void undumpXml(XMLDeserializer xml) {
	super.undumpXml(xml);
	variant = xml.parseIntAttr("var", VARIANT_4Q);
	if (variant != VARIANT_8Q)
	    variant = VARIANT_4Q;
	qMask = xml.parseIntAttr("qm", 0);
	runEnable = xml.parseIntAttr("re", 1) != 0;
	designation = xml.parseStringAttr("des", designation);
	note = EGTNote.undumpXml(xml, note);
	setSize(2);
	applyVariantSize();
	layoutPins();
	allocNodes();
	syncEndpoints();
	allocQArrays();
	allocIHolds();
    }

    int getDumpType() { return 466; }
    String getXmlDumpType() { return "EGTMiniSps"; }

    void getInfo(String arr[]) {
	arr[0] = getChipName() + " (" + designation + ")"
		+ (variant == VARIANT_8Q ? " 8I/8Q" : " 8I/4Q");
	arr[1] = run ? "RUN" : (runEnable ? "STOP (L1/N)" : "STOP (Taste)");
	arr[2] = "I=" + bitString(iMask, N_IN) + "  Q=" + bitString(qMask, nOut());
    }

    String bitString(int mask, int n) {
	char c[] = new char[n];
	for (int i = 0; i < n; i++)
	    c[i] = ((mask & (1 << i)) != 0) ? '1' : '0';
	return new String(c);
    }

    public EditInfo getChipEditInfo(int n) {
	if (n == 0)
	    return new EditInfo("Bezeichnung", designation);
	if (n == 1) {
	    EditInfo ei = new EditInfo("Gerätevariante", 0, -1, -1);
	    ei.choice = new Choice();
	    ei.choice.add("8I / 4Q — Fachkunde (Standard)");
	    ei.choice.add("8I / 8Q — erweiterte Ausgänge");
	    ei.choice.select(variant == VARIANT_8Q ? 1 : 0);
	    return ei;
	}
	if (n == 2) {
	    EditInfo ei = new EditInfo("", 0, -1, -1);
	    ei.button = new Button(Locale.LS("FUP-Operanden anlegen"));
	    return ei;
	}
	return null;
    }

    public void setChipEditValue(int n, EditInfo ei) {
	if (n == 0 && ei.textf != null) {
	    designation = ei.textf.getText();
	    return;
	}
	if (n == 1 && ei.choice != null) {
	    int v = ei.choice.getSelectedIndex() == 1 ? VARIANT_8Q : VARIANT_4Q;
	    if (v != variant) {
		variant = v;
		applyVariantSize();
		layoutPins();
		allocNodes();
		syncEndpoints();
		allocQArrays();
		qMask &= (1 << nOut()) - 1;
	    }
	    return;
	}
	if (n == 2)
	    spawnFupOperands();
    }

    /** I1…I8 links, Q1…nOut() rechts unter dem Gehäuse. Nur fehlende Adressen.
     *  Steht schon eine I- oder Q-Spalte, fehlende Kanäle in dieselbe Spalte. */
    void spawnFupOperands() {
	if (app == null || app.elmList == null)
	    return;
	app.undoManager.pushUndo();
	int pitch = 48;
	int box = EGTFupOperandElm.DEF_SIZE * 16;
	int below = snapGrid(y2 + 32);
	int iX = snapGrid(x);
	int iY0 = below;
	int qX = snapGrid(x2 - box);
	int qY0 = below;
	EGTFupOperandElm iRef = firstFupOperand(true);
	if (iRef != null) {
	    iX = iRef.x;
	    iY0 = iRef.y - (iRef.channel - 1) * pitch;
	}
	EGTFupOperandElm qRef = firstFupOperand(false);
	if (qRef != null) {
	    qX = qRef.x;
	    qY0 = qRef.y - (qRef.channel - 1) * pitch;
	}
	int added = 0;
	for (int ch = 1; ch <= N_IN; ch++) {
	    if (fupOperandExists(true, ch))
		continue;
	    if (placeFupOperand(true, ch, iX, iY0 + (ch - 1) * pitch))
		added++;
	}
	int nQ = nOut();
	for (int ch = 1; ch <= nQ; ch++) {
	    if (fupOperandExists(false, ch))
		continue;
	    if (placeFupOperand(false, ch, qX, qY0 + (ch - 1) * pitch))
		added++;
	}
	if (added == 0)
	    return;
	app.needAnalyze();
	app.undoManager.writeRecoveryToStorage();
	app.unsavedChanges = true;
    }

    EGTFupOperandElm firstFupOperand(boolean input) {
	if (app == null || app.elmList == null)
	    return null;
	for (int i = 0; i < app.elmList.size(); i++) {
	    CircuitElm ce = app.elmList.elementAt(i);
	    if (!(ce instanceof EGTFupOperandElm))
		continue;
	    EGTFupOperandElm op = (EGTFupOperandElm) ce;
	    if (op.isInputOperand() == input && fupMatchesThis(op))
		return op;
	}
	return null;
    }

    boolean fupMatchesThis(EGTFupOperandElm op) {
	String d = designation == null ? "" : designation.trim();
	String od = op.spsDes == null ? "" : op.spsDes.trim();
	if (!EGTMiniSpsLink.multiSps())
	    return true;
	if (od.length() == 0)
	    return this == EGTMiniSpsLink.firstSps();
	return od.equals(d);
    }

    boolean fupOperandExists(boolean input, int ch) {
	if (app == null || app.elmList == null)
	    return false;
	for (int i = 0; i < app.elmList.size(); i++) {
	    CircuitElm ce = app.elmList.elementAt(i);
	    if (!(ce instanceof EGTFupOperandElm))
		continue;
	    EGTFupOperandElm op = (EGTFupOperandElm) ce;
	    if (op.isInputOperand() == input && op.channel == ch
		    && fupMatchesThis(op))
		return true;
	}
	return false;
    }

    boolean placeFupOperand(boolean input, int ch, int px, int py) {
	String name = input ? "EGTFupEingangElm" : "EGTFupAusgangElm";
	CircuitElm ce = CirSim.constructElement(name, snapGrid(px), snapGrid(py));
	if (!(ce instanceof EGTFupOperandElm))
	    return false;
	EGTFupOperandElm op = (EGTFupOperandElm) ce;
	op.channel = ch;
	op.spsDes = designation == null ? "" : designation.trim();
	op.clampChannel();
	op.syncEndpoints();
	app.elmList.addElement(op);
	op.draggingDone();
	return true;
    }
}

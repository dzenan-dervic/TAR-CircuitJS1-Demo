/*
    TAR-Dervic EGT: dreipoliger Motorschutz-/Leistungsschalter, Dump 481.
    Symbol nach der Unterrichtsvorlage mit gekoppelten Hauptkontakten,
    Betätigungsmechanik und Überstromauslöser I >.
*/

package com.lushprojects.circuitjs1.client;

import com.google.gwt.user.client.ui.Button;
import com.lushprojects.circuitjs1.client.util.Locale;

/**
 * Dreipoliger Motorschutzschalter. Alle drei Außenleiter werden gemeinsam
 * manuell oder durch die thermisch/magnetische Überstromauslösung getrennt.
 */
class EGTMotorschutzschalterElm extends ChipElm implements EGTDesignatable {
    static final int SIZE_X = 8;
    static final int SIZE_Y = 8;
    static final double DEF_ISET = 32;
    static final double DEF_TRIP_TIME = 5;
    static final double DEF_MAG_MULT = 8;
    static final double R_ON = .05;
    static final double RMS_WINDOW = .04;
    // A magnetic release is effectively instantaneous.  A very short
    // simulation-time hold suppresses one-step numerical spikes without
    // making an AC short circuit miss the threshold at every zero crossing.
    static final double MAG_HOLD = .001;
    static final int TRIP_NONE = 0;
    static final int TRIP_THERMAL = 1;
    static final int TRIP_MAGNETIC = 2;
    static final String[] TOP_LABELS = { "1", "3", "5" };
    static final String[] BOTTOM_LABELS = { "2", "4", "6" };

    String designation = "Q1";
    String note = "";
    double iSet = DEF_ISET;
    double tripTime = DEF_TRIP_TIME;
    double magneticMultiple = DEF_MAG_MULT;
    boolean switchedOn = true;
    boolean tripped;
    int tripReason = TRIP_NONE;
    double thermal;
    double rmsSum[] = new double[3];
    double rmsTime;
    double poleRms[] = new double[3];
    double iRms;
    double magneticAccum;
    double poleCurrent[] = new double[3];
    double poleCurCount[] = new double[3];
    boolean holdUntilStamp = true;

    public String egtDesignation() { return designation; }
    public void setEgtDesignation(String d) {
        if (d != null && d.length() > 0)
            designation = d;
    }
    public String egtNote() { return note; }
    public void setEgtNote(String s) { note = EGTNote.clamp(s); }

    public EGTMotorschutzschalterElm(int xx, int yy) {
        super(xx, yy);
        flags &= ~EGTStyle.FLAG_UEBERSICHT;
        setSize(1);
        applySize();
        layoutPins();
        syncEndpoints();
    }

    public EGTMotorschutzschalterElm(int xa, int ya, int xb, int yb, int f,
                                     StringTokenizer st) {
        super(xa, ya, xb, yb, f, st);
        flags &= ~EGTStyle.FLAG_UEBERSICHT;
        setSize(1);
        if (st.hasMoreTokens()) {
            try {
                Integer.parseInt(st.nextToken());
                if (st.hasMoreTokens()) Integer.parseInt(st.nextToken());
                if (st.hasMoreTokens()) designation = CustomLogicModel.unescape(st.nextToken());
                if (st.hasMoreTokens()) iSet = Double.parseDouble(st.nextToken());
                if (st.hasMoreTokens()) tripTime = Double.parseDouble(st.nextToken());
                if (st.hasMoreTokens()) magneticMultiple = Double.parseDouble(st.nextToken());
                if (st.hasMoreTokens()) switchedOn = Boolean.parseBoolean(st.nextToken());
            } catch (Exception e) {
                // Fehlende optionale Werte behalten die Unterrichts-Defaults.
            }
        }
        sanitizeParameters();
        applySize();
        layoutPins();
        allocNodes();
        syncEndpoints();
    
        note = EGTNote.readOptional(st);
}

    void sanitizeParameters() {
        if (designation == null || designation.length() == 0) designation = "Q1";
        if (iSet <= 0) iSet = DEF_ISET;
        if (tripTime <= 0) tripTime = DEF_TRIP_TIME;
        if (magneticMultiple < 2) magneticMultiple = DEF_MAG_MULT;
    }

    boolean isDigitalChip() { return false; }
    boolean nonLinear() { return false; }
    boolean isWireEquivalent() { return false; }
    boolean isRemovableWire() { return false; }
    String getChipName() { return "EGT-Motorschutzschalter (3-polig)"; }
    int getPostCount() { return 6; }
    int getVoltageSourceCount() { return 0; }

    void applySize() {
        sizeX = SIZE_X;
        sizeY = SIZE_Y;
    }

    void setupPins() {
        applySize();
        pins = new Pin[6];
        for (int i = 0; i < pins.length; i++) {
            pins[i] = new Pin(0, SIDE_N, "");
            pins[i].output = false;
        }
        placePins();
    }

    void layoutPins() {
        if (pins == null || pins.length != 6)
            setupPins();
        else
            placePins();
    }

    void placePins() {
        applySize();
        // Equal integer-grid spacing; interpolating 1..6 rounded the
        // middle column and produced unequal gaps of two and three cells.
        int[] cols = { 1, 4, 7 };
        for (int i = 0; i < 3; i++) {
            int a = 2 * i;
            int b = a + 1;
            pins[a].pos = cols[i];
            pins[a].side0 = SIDE_N;
            pins[a].side = SIDE_N;
            pins[a].text = "";
            pins[a].output = false;
            pins[b].pos = cols[i];
            pins[b].side0 = SIDE_S;
            pins[b].side = SIDE_S;
            pins[b].text = "";
            pins[b].output = false;
        }
    }

    void syncEndpoints() {
        if (cspc2 < 1) setSize(1);
        x2 = x + sizeX * cspc2;
        y2 = y + sizeY * cspc2;
        setPoints();
    }

    boolean creationFailed() {
        syncEndpoints();
        return sizeX < SIZE_X || sizeY < SIZE_Y;
    }

    boolean contactClosed() { return switchedOn && !tripped; }

    boolean getConnection(int n1, int n2) {
        if (!contactClosed()) return false;
        for (int i = 0; i < 3; i++)
            if (EGTSchuetzLink.pinPair(n1, n2, 2 * i, 2 * i + 1))
                return true;
        return false;
    }

    boolean getMatrixConnection(int n1, int n2) {
        return getConnection(n1, n2);
    }

    void stamp() {
        holdUntilStamp = false;
        if (!contactClosed()) return;
        for (int i = 0; i < 3; i++)
            sim.stampResistor(nodes[2 * i], nodes[2 * i + 1], R_ON);
    }

    void startIteration() {}
    void execute() {}
    void doStep() {}

    void requestAnalyze() {
        if (CircuitElm.app != null)
            CircuitElm.app.needAnalyze();
    }

    void clearTrip() {
        tripped = false;
        tripReason = TRIP_NONE;
        thermal = 0;
        magneticAccum = 0;
        clearMeter();
        holdUntilStamp = true;
        requestAnalyze();
    }

    void trip(int reason) {
        if (tripped) return;
        tripped = true;
        tripReason = reason;
        thermal = 1;
        holdUntilStamp = true;
        requestAnalyze();
    }

    void clearMeter() {
        rmsSum[0] = rmsSum[1] = rmsSum[2] = 0;
        poleRms[0] = poleRms[1] = poleRms[2] = 0;
        rmsTime = 0;
        iRms = 0;
    }

    void reset() {
        super.reset();
        tripped = false;
        tripReason = TRIP_NONE;
        thermal = 0;
        magneticAccum = 0;
        poleCurrent[0] = poleCurrent[1] = poleCurrent[2] = 0;
        poleCurCount[0] = poleCurCount[1] = poleCurCount[2] = 0;
        clearMeter();
        holdUntilStamp = true;
    }

    boolean pinWired(int post) {
        if (nodes == null || post < 0 || post >= nodes.length) return false;
        CircuitNode cn = nodes[post];
        if (cn == null || cn.links == null) return false;
        for (int i = 0; i < cn.links.size(); i++) {
            CircuitNodeLink link = cn.links.get(i);
            if (link != null && link.elm != null && link.elm != this)
                return true;
        }
        return false;
    }

    boolean poleHasCircuit(int pole) {
        return pinWired(2 * pole) && pinWired(2 * pole + 1);
    }

    double poleAbsMax() {
        double result = 0;
        for (int i = 0; i < 3; i++)
            result = Math.max(result, Math.abs(poleCurrent[i]));
        return result;
    }

    void calculateCurrent() {
        EGTStyle.clearPinCurrents(pins);
        for (int i = 0; i < 3; i++) {
            int a = 2 * i;
            int b = a + 1;
            if (holdUntilStamp || !contactClosed() || !poleHasCircuit(i))
                poleCurrent[i] = 0;
            else {
                poleCurrent[i] = (volts[a] - volts[b]) / R_ON;
                EGTStyle.pinCurrentThru(pins, a, b, poleCurrent[i]);
            }
        }
        current = poleCurrent[0];
    }

    void stepFinished() {
        if (holdUntilStamp || !contactClosed()) {
            clearMeter();
            magneticAccum = 0;
            return;
        }
        double dt = sim.timeStep;
        if (dt <= 0) return;

        // Measure every pole separately.  Taking the largest instantaneous
        // value of a three-phase system would overestimate its RMS current.
        for (int i = 0; i < 3; i++) {
            if (!poleHasCircuit(i)) continue;
            double ip = poleCurrent[i];
            rmsSum[i] += ip * ip * dt;
        }
        rmsTime += dt;
        if (rmsTime >= RMS_WINDOW) {
            iRms = 0;
            for (int i = 0; i < 3; i++) {
                poleRms[i] = Math.sqrt(rmsSum[i] / rmsTime);
                iRms = Math.max(iRms, poleRms[i]);
                rmsSum[i] = 0;
            }
            rmsTime = 0;
        }

        // Magnetic short-circuit release uses simulation time so its result
        // is independent of CPU speed and the UI refresh rate.
        double peakRatio = poleAbsMax() / Math.max(iSet, 1e-6);
        if (peakRatio >= magneticMultiple) {
            magneticAccum += dt;
            if (magneticAccum >= MAG_HOLD) {
                trip(TRIP_MAGNETIC);
                return;
            }
        } else {
            magneticAccum = 0;
        }

        // Thermal I²t model, also based on simulation time.  This makes the
        // trip independent of browser frame rate and machine performance.
        double thermalRatio = iRms / Math.max(iSet, 1e-6);
        if (thermalRatio >= 1.2) {
            double severity = thermalRatio * thermalRatio;
            thermal += dt * severity / (tripTime * 1.2 * 1.2);
            if (thermal >= 1) trip(TRIP_THERMAL);
        } else if (thermalRatio < 1.0) {
            thermal -= dt / Math.max(tripTime * 4, 1);
            if (thermal < 0) thermal = 0;
        }
    }

    void boxBounds(int out[]) {
        int minX = rectPointsX[0], maxX = rectPointsX[0];
        int minY = rectPointsY[0], maxY = rectPointsY[0];
        for (int i = 1; i < 4; i++) {
            minX = Math.min(minX, rectPointsX[i]);
            maxX = Math.max(maxX, rectPointsX[i]);
            minY = Math.min(minY, rectPointsY[i]);
            maxY = Math.max(maxY, rectPointsY[i]);
        }
        out[0] = minX; out[1] = minY; out[2] = maxX; out[3] = maxY;
    }

    void drawChip(Graphics g) {
        g.save();
        try {
            int[] box = new int[4];
            boxBounds(box);
            Color stroke = needsHighlight() ? selectColor : lightGrayColor;
            drawReferenceSymbol(g, box[0], box[1], box[2], box[3], stroke);
            drawPosts(g);
        } finally {
            g.restore();
        }
    }

    void drawReferenceSymbol(Graphics g, int left, int top, int right,
                             int bottom, Color stroke) {
        int firstX = pins[0].post.x;
        int lastX = pins[4].post.x;
        int h = bottom - top;
        int contactY = top + h * 23 / 100;
        int bodyTop = top + h * 43 / 100;
        int bodyBottom = bottom - h * 10 / 100;
        int bodyLeft = firstX - 15;
        int bodyRight = lastX + 15;
        int dividerY = (bodyTop + bodyBottom) / 2;
        boolean closed = contactClosed();

        g.setColor(stroke);
        g.setLineWidth(2.5);
        g.context.setLineCap("butt");

        // Drei Hauptkontakte. Nur die Messer bewegen sich; die Schutz-
        // und Auslöseeinheit darunter bleibt ein ruhiger, leerer Block.
        for (int i = 0; i < 3; i++) {
            int a = 2 * i;
            int b = a + 1;
            int px = pins[a].post.x;
            g.drawLine(px, pins[a].post.y, px, contactY - 16);
            if (closed)
                g.drawLine(px, contactY - 16, px, bodyTop);
            else {
                int bladeOverhang = 12;
                int bladeSlope = 12;
                g.drawLine(px - bladeSlope, contactY - bladeOverhang,
                        px, bodyTop - 8);
                g.drawLine(px, bodyTop - 8, px, bodyTop);
            }
            g.drawLine(px, bodyBottom, px, pins[b].post.y);
        }

        // Zweigeteilte Schutz-/Auslöseeinheit.
        g.drawRect(bodyLeft, bodyTop, bodyRight - bodyLeft,
                bodyBottom - bodyTop);
        g.drawLine(bodyLeft, dividerY, bodyRight, dividerY);

        int cx = (bodyLeft + bodyRight) / 2;
        int upperCy = (bodyTop + dividerY) / 2;
        // Kompaktes, senkrechtes S-Auslösesymbol entsprechend der
        // markierten Fachkunde-Vorlage.
        int tripLeft = cx - 4;
        int tripRight = cx + 9;
        int tripTop = upperCy - 10;
        int tripUpper = upperCy - 5;
        int tripLower = upperCy + 5;
        int tripBottom = upperCy + 10;
        g.drawLine(tripLeft, tripTop, tripLeft, tripUpper);
        g.drawLine(tripLeft, tripUpper, tripRight, tripUpper);
        g.drawLine(tripRight, tripUpper, tripRight, tripLower);
        g.drawLine(tripRight, tripLower, tripLeft, tripLower);
        g.drawLine(tripLeft, tripLower, tripLeft, tripBottom);

        g.setFont(new Font("italic", 0, 19));
        g.context.setTextAlign("center");
        g.context.setTextBaseline("middle");
        g.drawString("I >", cx, (dividerY + bodyBottom) / 2);

        // Betätigungsmechanik nach Vorlage: Quadrat mit einfachem Kreuz.
        int actuatorX = bodyLeft - 39;
        int actuatorY = contactY;
        int actuatorSize = 20;
        g.setLineWidth(2.2);
        g.drawLine(actuatorX - 28, actuatorY, actuatorX + 10, actuatorY);
        g.drawLine(actuatorX - 28, actuatorY - 6,
                actuatorX - 28, actuatorY + 6);
        g.drawRect(actuatorX - 10, actuatorY - 10,
                actuatorSize, actuatorSize);
        g.drawLine(actuatorX, actuatorY - 15,
                actuatorX, actuatorY + 15);

        // Gestrichelte gemeinsame Betätigung läuft ausschließlich durch
        // die drei beweglichen Kontaktmesser, wie in der Fachkunde-Vorlage.
        g.setLineDash(12, 5);
        g.drawLine(actuatorX + 10, contactY, lastX + 2, contactY);

        // Unterhalb des Rasterknopfs nur die gestrichelte Mechanikachse.
        g.drawLine(actuatorX, actuatorY + 10,
                actuatorX, bodyBottom - 12);
        g.drawLine(actuatorX, bodyTop + 10, bodyLeft, bodyTop + 10);
        g.drawLine(actuatorX, bodyBottom - 12,
                bodyLeft, bodyBottom - 12);
        g.setLineDash(0, 0);

        // Kleine, außerhalb liegende Bedien-/Messangaben; das eigentliche
        // Schaltzeichen bleibt unverändert und frei lesbar.
        int textX = bodyRight + 15;
        g.context.setTextAlign("left");
        g.context.setTextBaseline("alphabetic");
        g.setFont(EGTStyle.pinLabelFont());
        g.setColor(whiteColor);
        int extra = EGTStyle.drawDesigAndNote(g, this, designation,
                textX, bodyTop + 12);
        g.drawString(getShortUnitText(iSet, "A"), textX, bodyTop + 28 + extra);
        g.setColor(tripped ? Color.red : (switchedOn ? lightGrayColor : COL_GRAY));
        g.drawString(stateText(), textX, bodyTop + 44 + extra);

        // Klemmenbezeichnungen bewusst klein, damit die Form der Vorlage
        // dominiert und die Verdrahtung trotzdem eindeutig bleibt.
        g.setFont(new Font("normal", 0, 9));
        g.context.setTextAlign("left");
        g.context.setTextBaseline("middle");
        g.setColor(whiteColor);
        for (int i = 0; i < 3; i++) {
            int a = 2 * i;
            int b = a + 1;
            g.drawString(TOP_LABELS[i], pins[a].post.x + 6,
                    pins[a].post.y - 1);
            g.drawString(BOTTOM_LABELS[i], pins[b].post.x + 6,
                    pins[b].post.y + 1);
        }

        if (closed) {
            for (int i = 0; i < 3; i++) {
                int a = 2 * i;
                int b = a + 1;
                poleCurCount[i] = updateDotCount(poleCurrent[i], poleCurCount[i]);
                drawDots(g, pins[a].post, new Point(pins[a].post.x, contactY - 7),
                        poleCurCount[i]);
                drawDots(g, new Point(pins[b].post.x, bodyTop), pins[b].post,
                        poleCurCount[i]);
            }
        }

        int textW = (int) Math.max(EGTStyle.measureDesigNote(g, this, designation),
                Math.max(g.measureWidth(getShortUnitText(iSet, "A")),
                        g.measureWidth(stateText())));
        adjustBbox(actuatorX - 31, top - 20,
                Math.max(right, textX + textW + 8), bottom + 18);
        g.setLineWidth(1);
    }

    static final Color COL_GRAY = new Color(0x77, 0x77, 0x77);

    String stateText() {
        if (tripped) return Locale.LS("ausgelöst");
        return switchedOn ? Locale.LS("EIN") : Locale.LS("AUS");
    }

    String dump() {
        return super.dump() + " " + sizeX + " " + sizeY + " "
                + CustomLogicModel.escape(designation) + " " + iSet + " "
                + tripTime + " " + magneticMultiple + " " + switchedOn + EGTNote.dumpSuffix(note);
    }

    void dumpXml(com.google.gwt.xml.client.Document doc,
                 com.google.gwt.xml.client.Element elem) {
        super.dumpXml(doc, elem);
        XMLSerializer.dumpAttr(elem, "sx", sizeX);
        XMLSerializer.dumpAttr(elem, "sy", sizeY);
        XMLSerializer.dumpAttr(elem, "des", designation);
        EGTNote.dumpXml(elem, note);
        XMLSerializer.dumpAttr(elem, "iset", iSet);
        XMLSerializer.dumpAttr(elem, "tt", tripTime);
        XMLSerializer.dumpAttr(elem, "mag", magneticMultiple);
        XMLSerializer.dumpAttr(elem, "on", switchedOn);
    }

    void undumpXml(XMLDeserializer xml) {
        super.undumpXml(xml);
        flags &= ~EGTStyle.FLAG_UEBERSICHT;
        designation = xml.parseStringAttr("des", designation);
        note = EGTNote.undumpXml(xml, note);
        iSet = xml.parseDoubleAttr("iset", iSet);
        tripTime = xml.parseDoubleAttr("tt", tripTime);
        magneticMultiple = xml.parseDoubleAttr("mag", magneticMultiple);
        switchedOn = xml.parseBooleanAttr("on", switchedOn);
        sanitizeParameters();
        setSize(1);
        applySize();
        layoutPins();
        allocNodes();
        syncEndpoints();
    }

    int getDumpType() { return 481; }
    String getXmlDumpType() { return "EGTMotorschutzschalter"; }

    void getInfo(String arr[]) {
        arr[0] = getChipName() + " (" + designation + ")";
        arr[1] = stateText() + ", Iset = " + getCurrentText(iSet);
        arr[2] = "Ieff(max) = " + getCurrentText(iRms);
        arr[3] = "I1 = " + getCurrentText(poleCurrent[0])
                + ", I2 = " + getCurrentText(poleCurrent[1])
                + ", I3 = " + getCurrentText(poleCurrent[2]);
        arr[4] = "I> = " + getUnitText(magneticMultiple, "×Iset");
        arr[5] = "Wärme = " + (int) Math.round(thermal * 100) + " %";
        if (tripped)
            arr[6] = tripReason == TRIP_MAGNETIC
                    ? "magnetisch ausgelöst" : "thermisch ausgelöst";
    }

    public EditInfo getChipEditInfo(int n) {
        if (n == 0) return new EditInfo("Bezeichnung", designation);
        if (n == 1) {
            EditInfo ei = new EditInfo("Schaltzustand", 0);
            ei.choice = new Choice();
            ei.choice.add(Locale.LS("EIN"));
            ei.choice.add(Locale.LS("AUS"));
            ei.choice.select(switchedOn ? 0 : 1);
            return ei;
        }
        if (n == 2)
            return new EditInfo("Einstellstrom Iset (A)", iSet, 0, 0).setPositive();
        if (n == 3)
            return new EditInfo("Thermische Auslösezeit (s)", tripTime, 0, 0).setPositive();
        if (n == 4)
            return new EditInfo("Magnetische Auslösung (×Iset)",
                    magneticMultiple, 0, 0).setPositive();
        if (n == 5 && tripped) {
            EditInfo ei = new EditInfo("", 0, -1, -1);
            ei.button = new Button(Locale.LS("Auslösung zurücksetzen"));
            return ei;
        }
        return null;
    }

    public void setChipEditValue(int n, EditInfo ei) {
        if (n == 0 && ei.textf != null) designation = ei.textf.getText();
        if (n == 1 && ei.choice != null) {
            boolean next = ei.choice.getSelectedIndex() == 0;
            if (next != switchedOn) {
                switchedOn = next;
                holdUntilStamp = true;
                requestAnalyze();
            }
        }
        if (n == 2) iSet = ei.value;
        if (n == 3) tripTime = ei.value;
        if (n == 4) magneticMultiple = ei.value;
        if (n == 5 && ei.button != null && tripped) {
            clearTrip();
            ei.newDialog = true;
        }
        sanitizeParameters();
    }

    double getCurrentIntoNode(int n) {
        int pole = n / 2;
        return (n % 2) == 1 ? poleCurrent[pole] : -poleCurrent[pole];
    }
}

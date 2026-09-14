/*
    TAR-Dervic EGT: vereinfachter Kondensatormotor (MOD-MOT-03 / M505), Dump 480.
    Zweipoliges Schuelermodell mit internem Betriebskondensator.
*/

package com.lushprojects.circuitjs1.client;

import com.lushprojects.circuitjs1.client.util.Locale;

/**
 * Didaktisch vereinfachter Einphasen-Kondensatormotor. Nach aussen werden nur
 * L und N verdrahtet. Hauptwicklung, Hilfswicklung und C_B bleiben als
 * getrennte interne Modellzweige erhalten; die Drehrichtung wird im Dialog
 * gewaehlt. Die Klemmbrettvariante mit U1/U2/Z1/Z2 ist Dump 479.
 */
class EGTKondensatormotorKompaktElm extends ChipElm implements EGTDesignatable {
    static final int SIZE_X = 7;
    static final int SIZE_Y = 7;
    static final double DEF_UN = 230;
    static final double DEF_PN = 250;
    static final double DEF_CAP_UF = 12;
    static final double DEF_RU = 35;
    static final double DEF_RZ = 55;
    static final double DEF_LU = .18;
    static final double DEF_LZ = .24;
    static final double DEF_J = .12;
    static final double DEF_B = .65;
    static final double NOMINAL_FREQUENCY = 50;
    static final double CURRENT_MIN = .025;
    static final double SPEED_LIMIT = 8;
    static final double ARROW_RATE = .015;
    static final Color COL_ACCENT = new Color(0x4a, 0x9e, 0xff);
    static final Color COL_IDLE = new Color(0x66, 0x66, 0x66);

    String designation = "M1";
    String note = "";
    int direction = 1;
    double capacitanceUf = DEF_CAP_UF;
    double nominalVoltage = DEF_UN;
    double nominalPower = DEF_PN;
    double rMain = DEF_RU;
    double rAux = DEF_RZ;
    double lMain = DEF_LU;
    double lAux = DEF_LZ;
    double inertia = DEF_J;
    double damping = DEF_B;

    // Nur eine Oberflaechenoption: Die erweiterten Modellwerte bleiben
    // gespeichert, werden Schuelern aber standardmaessig nicht gezeigt.
    boolean showAdvanced;

    // Solver-Begleitmodelle der beiden internen Zweige:
    // L -- R_U -- L_U -- N
    // L -- R_Z -- L_Z -- C_B -- N
    // Die Norton-Form vermeidet zusaetzliche schwebende Netzknoten.
    double mainCompResistance;
    double auxCompResistance;
    double mainSourceValue;
    double auxSourceValue;

    double mainCurrent;
    double auxCurrent;
    double capacitorVoltage;
    double mainEnvelope;
    double auxEnvelope;
    double voltageSquaredEnvelope;
    double speed;
    double angle = Math.PI / 2;

    public String egtDesignation() { return designation; }
    public void setEgtDesignation(String d) {
        if (d != null)
            designation = d;
    }
    public String egtNote() { return note; }
    public void setEgtNote(String s) { note = EGTNote.clamp(s); }

    public EGTKondensatormotorKompaktElm(int xx, int yy) {
        super(xx, yy);
        setSize(1);
        applySize();
        layoutPins();
        syncEndpoints();
    }

    public EGTKondensatormotorKompaktElm(int xa, int ya, int xb, int yb,
                                         int f, StringTokenizer st) {
        super(xa, ya, xb, yb, f, st);
        readDump(st);
        setSize(1);
        applySize();
        layoutPins();
        allocNodes();
        syncEndpoints();
    }

    void readDump(StringTokenizer st) {
        try {
            if (st.hasMoreTokens()) Integer.parseInt(st.nextToken());
            if (st.hasMoreTokens()) Integer.parseInt(st.nextToken());
            if (st.hasMoreTokens()) designation = CustomLogicModel.unescape(st.nextToken());
            if (st.hasMoreTokens()) direction = Integer.parseInt(st.nextToken());
            if (st.hasMoreTokens()) capacitanceUf = Double.parseDouble(st.nextToken());
            if (st.hasMoreTokens()) nominalVoltage = Double.parseDouble(st.nextToken());
            if (st.hasMoreTokens()) nominalPower = Double.parseDouble(st.nextToken());
            if (st.hasMoreTokens()) rMain = Double.parseDouble(st.nextToken());
            if (st.hasMoreTokens()) rAux = Double.parseDouble(st.nextToken());
            if (st.hasMoreTokens()) lMain = Double.parseDouble(st.nextToken());
            if (st.hasMoreTokens()) lAux = Double.parseDouble(st.nextToken());
            if (st.hasMoreTokens()) inertia = Double.parseDouble(st.nextToken());
            if (st.hasMoreTokens()) damping = Double.parseDouble(st.nextToken());
            note = EGTNote.readOptional(st);
        } catch (Exception e) {
            // Fehlende optionale Werte behalten die Unterrichts-Defaults.
        }
        sanitizeParameters();
    }

    void sanitizeParameters() {
        direction = direction < 0 ? -1 : 1;
        if (capacitanceUf <= 0) capacitanceUf = DEF_CAP_UF;
        if (nominalVoltage <= 0) nominalVoltage = DEF_UN;
        if (nominalPower <= 0) nominalPower = DEF_PN;
        if (rMain <= 0) rMain = DEF_RU;
        if (rAux <= 0) rAux = DEF_RZ;
        if (lMain <= 0) lMain = DEF_LU;
        if (lAux <= 0) lAux = DEF_LZ;
        if (inertia <= 0) inertia = DEF_J;
        if (damping <= 0) damping = DEF_B;
    }

    void applySize() {
        sizeX = SIZE_X;
        sizeY = SIZE_Y;
    }

    boolean isDigitalChip() { return false; }
    boolean nonLinear() { return false; }
    boolean isWireEquivalent() { return false; }
    boolean isRemovableWire() { return false; }
    String getChipName() { return "EGT-Kondensatormotor (vereinfacht)"; }
    int getPostCount() { return 2; }
    int getInternalNodeCount() { return 0; }
    int getVoltageSourceCount() { return 0; }

    void setupPins() {
        applySize();
        pins = new Pin[2];
        pins[0] = new Pin(sizeX / 2, SIDE_N, "L");
        pins[1] = new Pin(sizeX / 2, SIDE_S, "N");
        pins[0].output = false;
        pins[1].output = false;
    }

    void layoutPins() {
        if (pins == null || pins.length != 2)
            setupPins();
        else {
            applySize();
            pins[0].pos = sizeX / 2;
            pins[0].side0 = SIDE_N;
            pins[0].side = SIDE_N;
            pins[0].text = "L";
            pins[0].output = false;
            pins[1].pos = sizeX / 2;
            pins[1].side0 = SIDE_S;
            pins[1].side = SIDE_S;
            pins[1].text = "N";
            pins[1].output = false;
        }
    }

    int boxW() { return (isFlippedXY() ? sizeY : sizeX) * cspc2; }
    int boxH() { return (isFlippedXY() ? sizeX : sizeY) * cspc2; }

    void syncEndpoints() {
        if (cspc2 < 1)
            setSize(1);
        x2 = x + boxW();
        y2 = y + boxH();
        setPoints();
    }

    void drag(int xx, int yy) {
        xx = snapGrid(xx);
        yy = snapGrid(yy);
        if (Math.abs(xx - x) > Math.abs(yy - y))
            flags |= FLAG_FLIP_XY;
        else
            flags &= ~FLAG_FLIP_XY;
        applySize();
        x2 = x + boxW();
        y2 = y + boxH();
        setPoints();
    }

    boolean creationFailed() {
        syncEndpoints();
        return sizeX < SIZE_X || sizeY < SIZE_Y;
    }

    boolean getConnection(int n1, int n2) {
        return (n1 == 0 && n2 == 1) || (n1 == 1 && n2 == 0);
    }

    double capacitance() { return capacitanceUf * 1e-6; }

    void stamp() {
        // Rueckwaerts-Euler-Begleitmodelle. C_B ist im Hilfszweig nicht nur
        // gezeichnet, sondern Bestandteil der gestempelten RLC-Gleichung.
        double dt = sim.timeStep;
        mainCompResistance = rMain + lMain / dt;
        auxCompResistance = rAux + lAux / dt + dt / capacitance();
        sim.stampResistor(nodes[0], nodes[1], mainCompResistance);
        sim.stampResistor(nodes[0], nodes[1], auxCompResistance);
        sim.stampRightSide(nodes[0]);
        sim.stampRightSide(nodes[1]);
    }

    void startIteration() {
        double dt = sim.timeStep;
        mainSourceValue = (lMain / dt * mainCurrent) / mainCompResistance;
        auxSourceValue = (lAux / dt * auxCurrent - capacitorVoltage)
                / auxCompResistance;
    }

    void doStep() {
        sim.stampCurrentSource(nodes[0], nodes[1], mainSourceValue);
        sim.stampCurrentSource(nodes[0], nodes[1], auxSourceValue);
    }

    void calculateCurrent() {
        double voltage = volts[0] - volts[1];
        if (mainCompResistance > 0)
            mainCurrent = voltage / mainCompResistance + mainSourceValue;
        if (auxCompResistance > 0)
            auxCurrent = voltage / auxCompResistance + auxSourceValue;
        current = mainCurrent + auxCurrent;
        EGTStyle.clearPinCurrents(pins);
        EGTStyle.pinCurrentThru(pins, 0, 1, current);
    }

    void stepFinished() {
        double dt = sim.timeStep;
        if (dt <= 0)
            return;
        double voltage = volts[0] - volts[1];

        // Die elektrischen Groessen stammen direkt aus den gestempelten
        // RL-/RLC-Zweigen des CircuitJS-Solvers.
        calculateCurrent();
        capacitorVoltage += dt * auxCurrent / capacitance();

        double blend = Math.min(1, dt * 35);
        mainEnvelope += (Math.abs(mainCurrent) - mainEnvelope) * blend;
        auxEnvelope += (Math.abs(auxCurrent) - auxEnvelope) * blend;
        voltageSquaredEnvelope += (voltage * voltage - voltageSquaredEnvelope) * blend;

        double target = hasRotatingField() ? direction * SPEED_LIMIT : 0;
        double mechanicalTau = Math.max(.08, inertia / damping);
        double speedBlend = 1 - Math.exp(-dt / mechanicalTau);
        speed += (target - speed) * speedBlend;
        if (Math.abs(speed) < .002 && target == 0)
            speed = 0;
        angle += speed * dt;
    }

    boolean hasRotatingField() {
        double vrms = Math.sqrt(Math.max(0, voltageSquaredEnvelope));
        return vrms > nominalVoltage * .25
                && mainEnvelope > CURRENT_MIN && auxEnvelope > CURRENT_MIN;
    }

    int directionSign() {
        if (!hasRotatingField() || Math.abs(speed) < .03)
            return 0;
        return direction;
    }

    String statusText() {
        int d = directionSign();
        if (d > 0) return Locale.LS("Rechtslauf");
        if (d < 0) return Locale.LS("Linkslauf");
        return Locale.LS("Stillstand");
    }

    void boxBounds(int out[]) {
        int minx = rectPointsX[0], maxx = rectPointsX[0];
        int miny = rectPointsY[0], maxy = rectPointsY[0];
        for (int i = 1; i < 4; i++) {
            minx = min(minx, rectPointsX[i]);
            maxx = max(maxx, rectPointsX[i]);
            miny = min(miny, rectPointsY[i]);
            maxy = max(maxy, rectPointsY[i]);
        }
        out[0] = minx; out[1] = miny; out[2] = maxx; out[3] = maxy;
    }

    Point axisPoint(double cx, double cy, double ux, double uy, double along,
                    double px, double py, double across) {
        return new Point((int) Math.round(cx + ux * along + px * across),
                (int) Math.round(cy + uy * along + py * across));
    }

    void drawChip(Graphics g) {
        g.save();
        try {
            Point a = pins[0].post;
            Point b = pins[1].post;
            double dx = b.x - a.x, dy = b.y - a.y;
            double len = Math.max(1, Math.sqrt(dx * dx + dy * dy));
            double ux = dx / len, uy = dy / len;
            double px = uy, py = -ux;
            int cx = (a.x + b.x) / 2;
            int cy = (a.y + b.y) / 2;
            int r = (int) Math.max(24, Math.min(32, len * .22));
            double branchOffset = r + 17;
            Color stroke = needsHighlight() ? selectColor : whiteColor;

            // Hauptzweig durch das Motorgehaeuse.
            Point rimA = axisPoint(cx, cy, ux, uy, -r, px, py, 0);
            Point rimB = axisPoint(cx, cy, ux, uy, r, px, py, 0);
            setVoltageColor(g, volts[0]);
            drawThickLine(g, a, rimA, 3);
            setVoltageColor(g, volts[1]);
            drawThickLine(g, rimB, b, 3);
            pins[0].curcount = updateDotCount(pins[0].current, pins[0].curcount);
            pins[1].curcount = updateDotCount(pins[1].current, pins[1].curcount);
            drawDots(g, a, rimA, pins[0].curcount);
            drawDots(g, rimB, b, pins[1].curcount);

            // Sichtbarer interner Hilfszweig, rechtwinklig zum Hauptzweig.
            Point bt = axisPoint(cx, cy, ux, uy, -len / 2, px, py, branchOffset);
            Point bb = axisPoint(cx, cy, ux, uy, len / 2, px, py, branchOffset);
            double capMid = -r * .72;
            Point ca = axisPoint(cx, cy, ux, uy, capMid - 5, px, py, branchOffset);
            Point cb = axisPoint(cx, cy, ux, uy, capMid + 5, px, py, branchOffset);
            g.setColor(lightGrayColor);
            g.setLineWidth(2);
            drawThickLine(g, a, bt, 2);
            drawThickLine(g, bt, ca, 2);
            drawThickLine(g, cb, bb, 2);
            drawThickLine(g, bb, b, 2);
            Point ca1 = axisPoint(ca.x, ca.y, px, py, -9, ux, uy, 0);
            Point ca2 = axisPoint(ca.x, ca.y, px, py, 9, ux, uy, 0);
            Point cb1 = axisPoint(cb.x, cb.y, px, py, -9, ux, uy, 0);
            Point cb2 = axisPoint(cb.x, cb.y, px, py, 9, ux, uy, 0);
            drawThickLine(g, ca1, ca2, 3);
            drawThickLine(g, cb1, cb2, 3);

            g.setColor(stroke);
            g.setLineWidth(3);
            g.context.beginPath();
            g.context.arc(cx, cy, r, 0, 2 * Math.PI, false);
            g.context.stroke();
            g.setLineWidth(1);

            int d = directionSign();
            if (d != 0) {
                if (app != null && app.simIsRunning())
                    angle += d * Math.abs(currentMult) * ARROW_RATE;
                drawArcArrow(g, cx, cy, r * .80, angle, d > 0);
            } else {
                g.setColor(COL_IDLE);
                g.setLineWidth(1.6);
                g.setLineDash(5, 4);
                g.context.beginPath();
                g.context.arc(cx, cy, r * .80, -2.25, -.90, false);
                g.context.stroke();
                g.setLineDash(0, 0);
                g.setLineWidth(1);
            }

            g.setColor(stroke);
            g.context.setTextAlign("center");
            g.context.setTextBaseline("middle");
            g.setFont(new Font("normal", 0, Math.max(15, r * 48 / 100)));
            g.drawString("M", cx, cy - r * 10 / 100);
            g.setFont(new Font("normal", 0, Math.max(10, r * 29 / 100)));
            g.drawString("1~", cx, cy + r * 30 / 100);
            g.setFont(EGTStyle.pinLabelFont());
            int capLabelX = (ca.x + cb.x) / 2 + (int) (px * 29)
                    - (int) (ux * 3);
            int capLabelY = (ca.y + cb.y) / 2 + (int) (py * 29)
                    - (int) (uy * 3);
            g.drawString("C_B", capLabelX, capLabelY);
            g.setFont(new Font("normal", 0, 9));
            g.setColor(lightGrayColor);
            g.drawString(Locale.LS("intern"), capLabelX, capLabelY + 11);
            g.context.setTextAlign("left");
            g.context.setTextBaseline("alphabetic");

            EGTStyle.drawPinLabelToward(g, this, a.x, a.y, cx, "L", whiteColor);
            EGTStyle.drawPinLabelToward(g, this, b.x, b.y, cx, "N", whiteColor);
            drawPosts(g);

            String status = statusText();
            int tx, ty;
            if (Math.abs(dy) >= Math.abs(dx)) {
                // Abstand zwischen sichtbarem C_B-Zweig und Textblock:
                // Bezeichnung und Betriebszustand duerfen nicht wie ein
                // gemeinsames Symbol wirken.
                tx = cx + (int) (px * (branchOffset + 56));
                ty = cy;
            } else {
                tx = Math.max(a.x, b.x) + 14;
                ty = cy;
            }
            g.setColor(stroke);
            int extra = EGTStyle.drawDesigAndNote(g, this, designation, tx, ty - 7);
            g.setColor(d == 0 ? lightGrayColor : COL_ACCENT);
            g.drawString(status, tx, ty + 12 + extra);

            int[] bounds = new int[4];
            boxBounds(bounds);
            int textWidth = (int) Math.max(EGTStyle.measureDesigNote(g, this, designation),
                    g.measureWidth(status));
            adjustBbox(Math.min(bounds[0], cx - r - 12),
                    Math.min(bounds[1] - 24, bt.y - 14),
                    Math.max(bounds[2], tx + textWidth + 10),
                    Math.max(bounds[3] + 8, bb.y + 14));
        } finally {
            g.restore();
        }
    }

    void drawArcArrow(Graphics g, int cx, int cy, double rr, double mid,
                      boolean clockwise) {
        double span = Math.PI * .55;
        double start = mid - span / 2;
        double end = mid + span / 2;
        g.setColor(COL_ACCENT);
        g.setLineWidth(2.2);
        g.context.beginPath();
        if (clockwise)
            g.context.arc(cx, cy, rr, start, end, false);
        else
            g.context.arc(cx, cy, rr, end, start, true);
        g.context.stroke();
        double tipAngle = clockwise ? end : start;
        double tangent = tipAngle + (clockwise ? Math.PI / 2 : -Math.PI / 2);
        double tx = cx + Math.cos(tipAngle) * rr;
        double ty = cy + Math.sin(tipAngle) * rr;
        double ah = Math.max(7, rr * .26);
        double bx = tx - Math.cos(tangent) * ah;
        double by = ty - Math.sin(tangent) * ah;
        double nx = -Math.sin(tangent) * ah * .45;
        double ny = Math.cos(tangent) * ah * .45;
        g.context.beginPath();
        g.context.moveTo(tx, ty);
        g.context.lineTo(bx + nx, by + ny);
        g.context.lineTo(bx - nx, by - ny);
        g.context.closePath();
        g.context.fill();
        g.setLineWidth(1);
    }

    void reset() {
        super.reset();
        mainCurrent = auxCurrent = capacitorVoltage = 0;
        mainSourceValue = auxSourceValue = 0;
        mainCompResistance = auxCompResistance = 0;
        mainEnvelope = auxEnvelope = voltageSquaredEnvelope = 0;
        speed = 0;
        angle = Math.PI / 2;
    }

    String dump() {
        return super.dump() + " " + sizeX + " " + sizeY + " "
                + CustomLogicModel.escape(designation) + " " + direction + " "
                + capacitanceUf + " " + nominalVoltage + " " + nominalPower
                + " " + rMain + " " + rAux + " " + lMain + " " + lAux
                + " " + inertia + " " + damping + EGTNote.dumpSuffix(note);
    }

    void dumpXml(com.google.gwt.xml.client.Document doc,
                 com.google.gwt.xml.client.Element elem) {
        super.dumpXml(doc, elem);
        XMLSerializer.dumpAttr(elem, "sx", sizeX);
        XMLSerializer.dumpAttr(elem, "sy", sizeY);
        XMLSerializer.dumpAttr(elem, "des", designation);
        EGTNote.dumpXml(elem, note);
        XMLSerializer.dumpAttr(elem, "dir", direction);
        XMLSerializer.dumpAttr(elem, "cb", capacitanceUf);
        XMLSerializer.dumpAttr(elem, "un", nominalVoltage);
        XMLSerializer.dumpAttr(elem, "pn", nominalPower);
        XMLSerializer.dumpAttr(elem, "ru", rMain);
        XMLSerializer.dumpAttr(elem, "rz", rAux);
        XMLSerializer.dumpAttr(elem, "lu", lMain);
        XMLSerializer.dumpAttr(elem, "lz", lAux);
        XMLSerializer.dumpAttr(elem, "j", inertia);
        XMLSerializer.dumpAttr(elem, "b", damping);
    }

    void undumpXml(XMLDeserializer xml) {
        super.undumpXml(xml);
        designation = xml.parseStringAttr("des", designation);
        note = EGTNote.undumpXml(xml, note);
        direction = xml.parseIntAttr("dir", direction);
        capacitanceUf = xml.parseDoubleAttr("cb", capacitanceUf);
        nominalVoltage = xml.parseDoubleAttr("un", nominalVoltage);
        nominalPower = xml.parseDoubleAttr("pn", nominalPower);
        rMain = xml.parseDoubleAttr("ru", rMain);
        rAux = xml.parseDoubleAttr("rz", rAux);
        lMain = xml.parseDoubleAttr("lu", lMain);
        lAux = xml.parseDoubleAttr("lz", lAux);
        inertia = xml.parseDoubleAttr("j", inertia);
        damping = xml.parseDoubleAttr("b", damping);
        sanitizeParameters();
        setSize(1);
        applySize();
        layoutPins();
        allocNodes();
        syncEndpoints();
    }

    int getDumpType() { return 480; }
    String getXmlDumpType() { return "EGTKondensatormotorKompakt"; }

    void getInfo(String arr[]) {
        arr[0] = getChipName() + " (" + designation + ")";
        arr[1] = "Un = " + getUnitText(nominalVoltage, "V") + ", Pn = "
                + getUnitText(nominalPower, "W");
        arr[2] = "C_B = " + getUnitText(capacitanceUf, "µF");
        arr[3] = "U = " + getVoltageText(volts[0] - volts[1])
                + ", I = " + getCurrentText(current);
        arr[4] = "I_H = " + getCurrentText(mainCurrent)
                + ", I_A = " + getCurrentText(auxCurrent);
        arr[5] = Locale.LS("Zustand") + " = " + statusText();
        arr[6] = "L/N; Hauptwicklung, Hilfswicklung und C_B intern";
    }

    public EditInfo getChipEditInfo(int n) {
        if (n == 0) return new EditInfo("Bezeichnung", designation);
        if (n == 1) {
            EditInfo ei = new EditInfo("Drehrichtung", 0, -1, -1);
            ei.choice = new Choice();
            ei.choice.add(Locale.LS("Rechtslauf"));
            ei.choice.add(Locale.LS("Linkslauf"));
            ei.choice.select(direction > 0 ? 0 : 1);
            return ei;
        }
        if (n == 2) return new EditInfo("Betriebskondensator C_B (µF)", capacitanceUf, 0, 0).setPositive();
        if (n == 3) return new EditInfo("Nennspannung (V)", nominalVoltage, 0, 0).setPositive();
        if (n == 4) return new EditInfo("Nennleistung (W)", nominalPower, 0, 0).setPositive();
        if (n == 5) {
            EditInfo ei = EditInfo.createCheckbox(
                    Locale.LS("Erweiterte Motorparameter"), showAdvanced);
            ei.newDialog = true;
            return ei;
        }
        if (!showAdvanced) return null;
        if (n == 6) return new EditInfo("Hauptwicklung R (Ohm)", rMain, 0, 0).setPositive();
        if (n == 7) return new EditInfo("Hilfswicklung R (Ohm)", rAux, 0, 0).setPositive();
        if (n == 8) return new EditInfo("Hauptwicklung L (H)", lMain, 0, 0).setPositive();
        if (n == 9) return new EditInfo("Hilfswicklung L (H)", lAux, 0, 0).setPositive();
        if (n == 10) return new EditInfo("Trägheit J", inertia, 0, 0).setPositive();
        if (n == 11) return new EditInfo("Reibung b", damping, 0, 0).setPositive();
        return null;
    }

    public void setChipEditValue(int n, EditInfo ei) {
        if (n == 0) designation = ei.textf.getText();
        if (n == 1) direction = ei.choice.getSelectedIndex() == 0 ? 1 : -1;
        if (n == 2) capacitanceUf = ei.value;
        if (n == 3) nominalVoltage = ei.value;
        if (n == 4) nominalPower = ei.value;
        if (n == 5 && ei.checkbox != null) {
            showAdvanced = ei.checkbox.getState();
            ei.newDialog = true;
            return;
        }
        if (n == 6) rMain = ei.value;
        if (n == 7) rAux = ei.value;
        if (n == 8) lMain = ei.value;
        if (n == 9) lAux = ei.value;
        if (n == 10) inertia = ei.value;
        if (n == 11) damping = ei.value;
        sanitizeParameters();
        if (app != null)
            app.needAnalyze();
    }
}

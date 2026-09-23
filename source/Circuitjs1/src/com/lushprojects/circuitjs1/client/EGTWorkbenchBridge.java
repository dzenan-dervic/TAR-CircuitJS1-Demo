package com.lushprojects.circuitjs1.client;

import java.util.ArrayList;
import java.util.HashMap;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * Additive JS-Bridge for the HTML contact-workshop. CircuitJS remains the only
 * electrical solver. Layout coordinates never become nodes.
 */
class EGTWorkbenchBridge {
    static final String VERSION = "1";
    static final int SCHEMA = 1;
    static final int ORIGIN_X = 48;
    static final int ORIGIN_Y = 80;
    static final int STEP_X = 240;
    static final int STEP_Y = 176;
    static final int COLS = 5;
    static final double COIL_V = 24;
    static final double COIL_P = 4;
    static final double MOTOR_V = 24;
    static final double MOTOR_P = 50;

    static class Term {
	CircuitElm elm;
	int post;
	Term(CircuitElm e, int p) {
	    elm = e;
	    post = p;
	}
    }

    CirSim app;
    int loadedRevision;
    int placeCol, placeRow;
    HashMap<String, Term> terminals;
    HashMap<String, EGTTasterElm> tasters;
    HashMap<String, EGTTasterElm> tastersNc;
    HashMap<String, EGTTasterElm[]> selectors;
    HashMap<String, EGTLeuchteElm> lamps;
    HashMap<String, EGTLeistungsschuetzElm> contactors;
    HashMap<String, EGTSchuetzSpuleElm> relays;
    HashMap<String, EGTZeitrelaisSpuleElm> timers;
    ArrayList<CircuitElm> commonFrom;
    ArrayList<CircuitElm> commonTo;
    HashMap<String, EGTSchuetzHilfskontaktElm> auxContacts;
    HashMap<String, EGTGleichstrommotorElm> motors;
    HashMap<String, EGTDrehstrommotorElm> acMotors;
    HashMap<String, EGTWechselschalterElm> driveSensors;
    HashMap<String, EGTGleichspannungsquelleElm> psus;
    HashMap<String, Double> psuSetpoint;
    HashMap<String, EGTDrehstromquelleElm> mains;
    HashMap<String, Double> mainsSetpoint;
    HashMap<String, WireElm> wireElms;
    HashMap<String, EGTWorkbenchMotorProtectionElm> motorProtectors;
    HashMap<String, EGTWorkbenchBreakerElm> breakers;
    boolean supplyOn = true;

    EGTWorkbenchBridge(CirSim app) {
	this.app = app;
	resetMaps();
    }

    String version() { return VERSION; }

    void resetMaps() {
        motorProtectors = new HashMap<String, EGTWorkbenchMotorProtectionElm>();
        breakers = new HashMap<String, EGTWorkbenchBreakerElm>();
	terminals = new HashMap<String, Term>();
	tasters = new HashMap<String, EGTTasterElm>();
	tastersNc = new HashMap<String, EGTTasterElm>();
	selectors = new HashMap<String, EGTTasterElm[]>();
	lamps = new HashMap<String, EGTLeuchteElm>();
	contactors = new HashMap<String, EGTLeistungsschuetzElm>();
        relays = new HashMap<String, EGTSchuetzSpuleElm>();
        timers = new HashMap<String, EGTZeitrelaisSpuleElm>();
	commonFrom = new ArrayList<CircuitElm>();
	commonTo = new ArrayList<CircuitElm>();
	auxContacts = new HashMap<String, EGTSchuetzHilfskontaktElm>();
	motors = new HashMap<String, EGTGleichstrommotorElm>();
	acMotors = new HashMap<String, EGTDrehstrommotorElm>();
	driveSensors = new HashMap<String, EGTWechselschalterElm>();
	psus = new HashMap<String, EGTGleichspannungsquelleElm>();
	psuSetpoint = new HashMap<String, Double>();
	mains = new HashMap<String, EGTDrehstromquelleElm>();
	mainsSetpoint = new HashMap<String, Double>();
	wireElms = new HashMap<String, WireElm>();
	placeCol = 0;
	placeRow = 0;
    }

    String loadProject(JavaScriptObject project) {
	if (project == null)
	    return fail("Kein Projekt");
	int schema = (int) getNum(project, "schema", 0);
	if (schema != SCHEMA)
	    return fail("Unbekanntes Schema " + schema);
	JavaScriptObject components = getObj(project, "components");
	JavaScriptObject wires = getObj(project, "wires");
	if (components == null)
	    return fail("components fehlt");
	if (wires == null)
	    return fail("wires fehlt");

	String check = validate(project, components, wires);
	if (check != null)
	    return fail(check);

	boolean wantRun = getBool(project, "running", false);
	boolean wantSupply = getBool(project, "supplyOn", true);
	int revision = (int) getNum(project, "revision", 0);
	String previous = app.dumpCircuit();
	boolean wasRunning = app.simIsRunning();
	app.setSimRunning(false);
	try {
	    build(components, wires, wantSupply);
	    loadedRevision = revision;
	    supplyOn = wantSupply;
	    CircuitElm.voltageRange = 24;
	    app.ui.setLayoutLocked(true);
	    app.loader.finishReadCircuit(CircuitLoader.RC_NO_CENTER);
	    app.sim.analyzeCircuit();
	    EGTSchuetzLink.refresh(app.sim);
	    enableRealtime();
	    updateVoltageRange();
	    if (wantRun)
		app.setSimRunning(true);
	    return ok();
	} catch (Exception e) {
	    try {
		if (previous != null)
		    app.loader.readCircuit(previous, CircuitLoader.RC_NO_CENTER);
	    } catch (Exception ignored) {}
	    app.setSimRunning(wasRunning);
	    return fail(e.getMessage() == null ? "Import fehlgeschlagen" : e.getMessage());
	}
    }

    boolean setPressed(String id, boolean pressed) {
	if (id == null)
	    return false;
	EGTTasterElm no = tasters.get(id);
	EGTTasterElm nc = tastersNc.get(id);
	if (no == null && nc == null)
	    return false;
	if (no != null)
	    no.setWorkbenchPressed(pressed);
	if (nc != null)
	    nc.setWorkbenchPressed(pressed);
	app.needAnalyze();
	app.repaint();
	return true;
    }

    void setRunning(boolean running) {
	app.setSimRunning(running);
    }

    boolean setSelector(String id, int position) {
        EGTTasterElm[] pair = selectors.get(id);
        if (pair == null || position < -1 || position > 1) return false;
        pair[0].setWorkbenchPressed(false);
        pair[1].setWorkbenchPressed(false);
        if (position != 0) pair[position < 0 ? 0 : 1].setWorkbenchPressed(true);
        app.needAnalyze();
        app.repaint();
        return true;
    }

    boolean setProtection(String id, String action) {
        if (!"on".equals(action) && !"off".equals(action) && !"reset".equals(action) && !"test".equals(action)) return false;
        EGTWorkbenchMotorProtectionElm m = motorProtectors.get(id);
        EGTWorkbenchBreakerElm b = breakers.get(id);
        if (m == null && b == null) return false;
        if (m != null) {
            if ("off".equals(action)) m.switchedOn = false;
            if ("on".equals(action) && !m.tripped) m.switchedOn = true;
            if ("reset".equals(action)) { m.switchedOn = false; m.clearTrip(); }
            if ("test".equals(action)) m.trip(EGTMotorschutzschalterElm.TRIP_THERMAL);
        }
        if (b != null) {
            if ("off".equals(action)) b.switchedOn = false;
            if ("on".equals(action) && !b.tripped) b.switchedOn = true;
            if ("reset".equals(action)) { b.switchedOn = false; b.clearTrip(); }
            if ("test".equals(action)) b.trip(EGTSicherungElm.TRIP_TH);
        }
        EGTSchuetzLink.refresh(app.sim);
        app.needAnalyze(); app.repaint();
        return true;
    }

    boolean setDrivePosition(String id, double position) {
        if (!driveSensors.containsKey(id + ".B1")) return false;
        boolean[] active = { position <= .03, Math.abs(position - .5) <= .035, position >= .97 };
        boolean changed = false;
        for (int i = 0; i < 3; i++) {
            EGTWechselschalterElm sensor = driveSensors.get(id + ".B" + (i + 1));
            int next = active[i] ? 1 : 0;
            if (sensor.position != next) {
                sensor.position = next;
                sensor.setPoints();
                changed = true;
            }
        }
        if (changed) { app.needAnalyze(); app.repaint(); }
        return true;
    }

    String resetSimulation(JavaScriptObject project) {
	if (project == null)
	    return fail("Kein Aufbau zum Zurücksetzen");
	return loadProject(resetProjectCopy(project));
    }

    private native JavaScriptObject resetProjectCopy(JavaScriptObject project) /*-{
	var copy = JSON.parse(JSON.stringify(project));
	copy.running = false;
	return copy;
    }-*/;

    void enableRealtime() {
	app.ui.setEgtRealtimeAvailable(true);
	app.ui.setEgtRealtimeMode(true);
    }

    void setSupplyOn(boolean on) {
	supplyOn = on;
	for (String id : psus.keySet()) {
	    EGTGleichspannungsquelleElm g = psus.get(id);
	    g.voltage = on ? setpointOf(id) : 0;
	}
	for (String id : mains.keySet()) {
	    EGTDrehstromquelleElm g = mains.get(id);
	    g.voltageRMS = on ? lineToPhase(mainsLineOf(id)) : 0;
	}
	updateVoltageRange();
	app.needAnalyze();
	app.repaint();
    }

    boolean setPsuVoltage(String id, double v) {
	if (id == null)
	    return false;
	if (v < 1)
	    v = 1;
	if (psus.containsKey(id)) {
	    psuSetpoint.put(id, Double.valueOf(v));
	    if (supplyOn)
		psus.get(id).voltage = v;
	    updateVoltageRange();
	    app.needAnalyze();
	    app.repaint();
	    return true;
	}
	if (mains.containsKey(id)) {
	    mainsSetpoint.put(id, Double.valueOf(v));
	    if (supplyOn)
		mains.get(id).voltageRMS = lineToPhase(v);
	    updateVoltageRange();
	    app.needAnalyze();
	    app.repaint();
	    return true;
	}
	return false;
    }

    boolean setRatings(String id, double nomV, double nomP) {
	if (id == null)
	    return false;
	if (nomV < 1)
	    nomV = 1;
	if (nomP <= 0)
	    nomP = 1;
	EGTLeistungsschuetzElm k = contactors.get(id);
	if (k != null) {
	    k.nom_v = nomV;
	    k.nom_pow = nomP;
	    k.updateResistance();
	    app.needAnalyze();
	    app.repaint();
	    return true;
	}
        EGTZeitrelaisSpuleElm timer = timers.get(id);
        if (timer != null) {
            timer.nom_v = nomV; timer.nom_pow = nomP; timer.updateResistance();
            app.needAnalyze(); app.repaint(); return true;
        }
        EGTSchuetzSpuleElm relay = relays.get(id);
        if (relay != null) {
            relay.nom_v = nomV;
            relay.nom_pow = nomP;
            relay.updateResistance();
            app.needAnalyze();
            app.repaint();
            return true;
        }
	EGTGleichstrommotorElm m = motors.get(id);
	if (m != null) {
	    m.nom_v = nomV;
	    m.nom_p = nomP;
	    m.updateResistance();
	    app.needAnalyze();
	    app.repaint();
	    return true;
	}
	EGTLeuchteElm light = lamps.get(id);
	if (light != null) {
	    light.nom_v = nomV;
	    light.nom_pow = nomP;
	    light.updateResistance();
	    app.needAnalyze();
	    app.repaint();
	    return true;
	}
	return false;
    }

    boolean setAcMotorRatings(String id, double kw, double eta) {
	if (id == null || !acMotors.containsKey(id))
	    return false;
	EGTDrehstrommotorElm m = acMotors.get(id);
	if (kw < EGTDrehstrommotorElm.P_MIN_KW)
	    kw = EGTDrehstrommotorElm.P_MIN_KW;
	if (kw > EGTDrehstrommotorElm.P_MAX_KW)
	    kw = EGTDrehstrommotorElm.P_MAX_KW;
	m.ratedKw = kw;
	m.eta = EGTDrehstrommotorElm.parseEtaToken(eta);
	m.applyRatedPower();
	app.needAnalyze();
	app.repaint();
	return true;
    }

    private double setpointOf(String id) {
	Double v = psuSetpoint.get(id);
	return v == null ? 24 : v.doubleValue();
    }

    private double mainsLineOf(String id) {
	Double v = mainsSetpoint.get(id);
	return v == null ? 400 : v.doubleValue();
    }

    /** U_LL → U_LN. 400 V 3~ ergibt 230 V gegen N. */
    private static double lineToPhase(double uLL) {
	return uLL / Math.sqrt(3);
    }

    private void updateVoltageRange() {
	double max = 24;
	for (String id : psuSetpoint.keySet()) {
	    double v = setpointOf(id);
	    if (v > max)
		max = v;
	}
	for (String id : mainsSetpoint.keySet()) {
	    double v = mainsLineOf(id);
	    if (v > max)
		max = v;
	}
	CircuitElm.voltageRange = supplyOn ? max : 24;
    }

    private double supplyCurrent(EGTGleichspannungsquelleElm g) {
	if (g == null || g.pins == null || g.pins.length == 0)
	    return 0;
	return g.pins[0].current;
    }

    private double mainsCurrent(EGTDrehstromquelleElm g) {
	if (g == null || g.pins == null || g.pins.length < 3)
	    return 0;
	double max = 0;
	for (int i = 0; i < 3; i++) {
	    double abs = Math.abs(g.pins[i].current);
	    if (abs > max)
		max = abs;
	}
	return max;
    }

    private double acLineCurrent(EGTDrehstrommotorElm m) {
	if (m == null)
	    return 0;
	double i = m.envIu;
	if (m.envIv > i)
	    i = m.envIv;
	if (m.envIw > i)
	    i = m.envIw;
	return i;
    }

    private void protectionSnapshot(StringBuffer sb, String id, boolean on, boolean tripped, int reason, double current, double thermal) {
        sb.append('"').append(esc(id)).append("\":{\"on\":").append(on);
        sb.append(",\"tripped\":").append(tripped).append(",\"reason\":").append(reason);
        sb.append(",\"current\":").append(jsonNum(current)).append(",\"thermal\":").append(jsonNum(thermal)).append('}');
    }

    String readSnapshot() {
	StringBuffer sb = new StringBuffer();
	sb.append("{\"ok\":true,\"version\":\"").append(VERSION);
	sb.append("\",\"revision\":").append(loadedRevision);
	sb.append(",\"time\":").append(app.sim.t);
	sb.append(",\"running\":").append(app.simIsRunning());
	sb.append(",\"currentMult\":").append(jsonNum(CircuitElm.currentMult));
	sb.append(",\"supplyOn\":").append(supplyOn);
	sb.append(",\"fault\":");
	if (app.stopMessage == null) {
	    sb.append("null");
	} else {
	    sb.append("{\"code\":\"SIMULATION_STOPPED\",\"message\":\"");
	    sb.append(esc(app.stopMessage)).append("\",\"componentIds\":[");
	    HashMap<String, Boolean> reported = new HashMap<String, Boolean>();
	    boolean firstFault = true;
	    for (String key : terminals.keySet()) {
		if (terminals.get(key).elm != app.stopElm)
		    continue;
		String id = key.substring(0, key.indexOf('.'));
		if (reported.containsKey(id))
		    continue;
		reported.put(id, Boolean.TRUE);
		if (!firstFault)
		    sb.append(',');
		firstFault = false;
		sb.append('"').append(esc(id)).append('"');
	    }
	    sb.append("]}");
	}
        sb.append(",\"protections\":{");
        boolean firstProtection = true;
        for (String id : motorProtectors.keySet()) {
            EGTWorkbenchMotorProtectionElm m = motorProtectors.get(id);
            if (!firstProtection) sb.append(','); firstProtection = false;
            protectionSnapshot(sb, id, m.switchedOn, m.tripped, m.tripReason, m.iRms, m.thermal);
        }
        for (String id : breakers.keySet()) {
            EGTWorkbenchBreakerElm b = breakers.get(id);
            if (!firstProtection) sb.append(','); firstProtection = false;
            protectionSnapshot(sb, id, b.switchedOn, b.tripped, b.tripReason, b.iShow, b.thermal);
        }
        sb.append('}');
	sb.append(",\"coils\":{");
	boolean first = true;
	for (String id : contactors.keySet()) {
	    EGTLeistungsschuetzElm k = contactors.get(id);
	    if (!first)
		sb.append(',');
	    first = false;
	    sb.append('"').append(esc(id)).append("\":{");
	    sb.append("\"energized\":").append(k.coilEnergized());
	    sb.append(",\"a1\":").append(jsonNum(k.getVoltageJS(EGTLeistungsschuetzElm.N_A1)));
	    sb.append(",\"a2\":").append(jsonNum(k.getVoltageJS(EGTLeistungsschuetzElm.N_A2)));
	    sb.append(",\"current\":").append(jsonNum(k.getCurrent()));
	    sb.append('}');
	}
        for (String id : relays.keySet()) {
            EGTSchuetzSpuleElm k = relays.get(id);
            if (!first) sb.append(',');
            first = false;
            sb.append('"').append(esc(id)).append("\":{");
            sb.append("\"energized\":").append(k.coilEnergized());
            sb.append(",\"a1\":").append(jsonNum(k.getVoltageJS(0)));
            sb.append(",\"a2\":").append(jsonNum(k.getVoltageJS(1)));
            sb.append(",\"current\":").append(jsonNum(k.getCurrent()));
            sb.append('}');
        }
        for (String id : timers.keySet()) {
            if (!first) sb.append(','); first = false;
            EGTZeitrelaisSpuleElm timer = timers.get(id);
            sb.append('"').append(esc(id)).append("\":{\"energized\":");
            sb.append(EGTZeitrelaisLink.isTimedOn(timer.designation)).append('}');
        }
        sb.append("},\"timers\":{"); first = true;
        for (String id : timers.keySet()) {
            if (!first) sb.append(','); first = false;
            EGTZeitrelaisLink.State state = EGTZeitrelaisLink.get(timers.get(id).designation);
            boolean waiting = EGTZeitrelaisLink.isWaitingState(state);
            double phase = state.mode == EGTZeitrelaisLink.MODE_BLINK ? (state.timedOn ? state.delay : state.delayOff)
                : state.mode == EGTZeitrelaisLink.MODE_COMBINED && !state.powered ? state.delayOff : state.delay;
            sb.append('"').append(esc(id)).append("\":{\"powered\":").append(state.powered);
            sb.append(",\"on\":").append(state.timedOn).append(",\"waiting\":").append(waiting);
            sb.append(",\"remaining\":").append(jsonNum(waiting ? Math.max(0, phase - state.waitAccum) : 0));
            sb.append(",\"duration\":").append(jsonNum(phase)).append('}');
        }
	sb.append("},\"contacts\":{");
	first = true;
	for (String id : contactors.keySet()) {
	    EGTLeistungsschuetzElm k = contactors.get(id);
	    boolean on = k.coilEnergized();
	    first = contactPair(sb, first, id + ".1", on);
	    first = contactPair(sb, first, id + ".3", on);
	    first = contactPair(sb, first, id + ".5", on);
	    first = contactPair(sb, first, id + ".13", on);
	}
	for (String key : auxContacts.keySet()) {
	    EGTSchuetzHilfskontaktElm c = auxContacts.get(key);
	    first = contactPair(sb, first, key, c.contactClosed());
	}
        for (String id : selectors.keySet()) {
            EGTTasterElm[] pair = selectors.get(id);
            first = contactPair(sb, first, id + ".13", pair[0].position == 0);
            first = contactPair(sb, first, id + ".23", pair[1].position == 0);
        }
        for (String key : driveSensors.keySet()) {
            boolean active = driveSensors.get(key).position == 1;
            first = contactPair(sb, first, key + "_12", !active);
            first = contactPair(sb, first, key + "_14", active);
        }
	sb.append("},\"terminals\":{");
	first = true;
	for (String key : terminals.keySet()) {
	    Term t = terminals.get(key);
	    if (!first)
		sb.append(',');
	    first = false;
	    sb.append('"').append(esc(key)).append("\":");
	    sb.append(jsonNum(t.elm.getVoltageJS(t.post)));
	}
	sb.append("},\"wireCurrents\":{");
	first = true;
	for (String key : wireElms.keySet()) {
	    WireElm w = wireElms.get(key);
	    if (w == null)
		continue;
	    if (!first)
		sb.append(',');
	    first = false;
	    sb.append('"').append(esc(key)).append("\":").append(jsonNum(w.getCurrent()));
	}
	sb.append("},\"motors\":{");
	first = true;
	for (String id : motors.keySet()) {
	    EGTGleichstrommotorElm m = motors.get(id);
	    if (!first)
		sb.append(',');
	    first = false;
	    double i = m.getCurrent();
	    sb.append('"').append(esc(id)).append("\":{");
	    sb.append("\"current\":").append(jsonNum(i));
	    sb.append(",\"running\":").append(Math.abs(i) >= EGTGleichstrommotorElm.I_RUN);
	    sb.append(",\"reverse\":").append(i < -EGTGleichstrommotorElm.I_RUN);
	    sb.append(",\"kind\":\"dc\"");
	    sb.append('}');
	}
	for (String id : acMotors.keySet()) {
	    EGTDrehstrommotorElm m = acMotors.get(id);
	    if (!first)
		sb.append(',');
	    first = false;
	    int dir = m.directionSign();
	    sb.append('"').append(esc(id)).append("\":{");
	    sb.append("\"kind\":\"ac3\"");
	    sb.append(",\"current\":").append(jsonNum(acLineCurrent(m)));
	    sb.append(",\"running\":").append(dir != 0);
	    sb.append(",\"reverse\":").append(dir < 0);
	    sb.append(",\"wiring\":\"").append(esc(m.wiringHintText())).append('"');
	    sb.append(",\"status\":\"").append(esc(m.statusText())).append('"');
	    sb.append('}');
	}
	sb.append("},\"supplies\":{");
	first = true;
	for (String id : psus.keySet()) {
	    EGTGleichspannungsquelleElm g = psus.get(id);
	    if (!first)
		sb.append(',');
	    first = false;
	    sb.append('"').append(esc(id)).append("\":{");
	    sb.append("\"kind\":\"dc\"");
	    sb.append(",\"set\":").append(jsonNum(setpointOf(id)));
	    sb.append(",\"voltage\":").append(jsonNum(g.voltage));
	    sb.append(",\"current\":").append(jsonNum(supplyCurrent(g)));
	    sb.append('}');
	}
	for (String id : mains.keySet()) {
	    EGTDrehstromquelleElm g = mains.get(id);
	    if (!first)
		sb.append(',');
	    first = false;
	    double uLL = mainsLineOf(id);
	    sb.append('"').append(esc(id)).append("\":{");
	    sb.append("\"kind\":\"ac400\"");
	    sb.append(",\"set\":").append(jsonNum(uLL));
	    sb.append(",\"voltage\":").append(jsonNum(g.voltageRMS));
	    sb.append(",\"uLL\":").append(jsonNum(uLL));
	    sb.append(",\"current\":").append(jsonNum(mainsCurrent(g)));
	    sb.append('}');
	}
	sb.append("},\"lamps\":{");
	first = true;
	for (String id : lamps.keySet()) {
	    EGTLeuchteElm p = lamps.get(id);
	    if (!first)
		sb.append(',');
	    first = false;
	    sb.append('"').append(esc(id)).append("\":{\"on\":").append(p.isLit()).append('}');
	}
	sb.append("}}");
	return sb.toString();
    }

    private boolean contactPair(StringBuffer sb, boolean first, String key, boolean closed) {
	if (!first)
	    sb.append(',');
	sb.append('"').append(esc(key)).append("\":").append(closed);
	return false;
    }

    private String validate(JavaScriptObject project, JavaScriptObject components,
			    JavaScriptObject wires) {
	HashMap<String, String> types = new HashMap<String, String>();
	HashMap<String, String> parents = new HashMap<String, String>();
	int n = getLen(components);
	for (int i = 0; i < n; i++) {
	    JavaScriptObject c = getAt(components, i);
	    if (c == null)
		return "Leeres Bauteil";
	    String id = getStr(c, "id", "");
	    String type = getStr(c, "type", "");
	    if (id.length() == 0)
		return "Bauteil ohne id";
	    if (types.containsKey(id))
		return "Doppelte id " + id;
	    if (!validType(type))
		return "Unbekannter Typ " + type;
            if ("relay".equals(type)) {
                JavaScriptObject config = getObj(c, "relayContacts");
                if (config == null || getLen(config) < 1 || getLen(config) > 4) return id + ": 1 bis 4 Kontakte erforderlich";
                HashMap<Integer, Boolean> numbers = new HashMap<Integer, Boolean>();
                for (int j = 0; j < getLen(config); j++) {
                    JavaScriptObject contact = getAt(config, j);
                    double number = getNum(contact, "number", 0);
                    String kind = getStr(contact, "kind", "");
                    if (number < 1 || number > 9 || number != (int)number || numbers.containsKey((int)number)
                            || !("NO".equals(kind) || "NC".equals(kind))) return id + ": Kontaktart oder Kontaktnummer ungültig";
                    numbers.put((int)number, Boolean.TRUE);
                }
            }
	    types.put(id, type);
	    if ("auxiliary".equals(type)) {
		String parent = getStr(c, "parentId", getStr(c, "parent", ""));
		if (parent.length() == 0)
		    return id + " ohne parentId";
		parents.put(id, parent);
	    }
	}
	for (String auxId : parents.keySet()) {
	    String parent = parents.get(auxId);
	    if (!"contactor".equals(types.get(parent)))
		return auxId + " braucht ein Leistungsschütz";
	}
	HashMap<String, Boolean> seenWire = new HashMap<String, Boolean>();
	int nw = getLen(wires);
	for (int i = 0; i < nw; i++) {
	    JavaScriptObject w = getAt(wires, i);
	    if (w == null)
		return "Leere Leitung";
	    String a = normalizeTerm(getStr(w, "a", ""));
	    String b = normalizeTerm(getStr(w, "b", ""));
	    if (a.length() == 0 || b.length() == 0)
		return "Leitung ohne Klemmen";
	    if (a.equals(b))
		return "Leitung auf derselben Klemme " + a;
	    String ta = termOwner(a);
	    String tb = termOwner(b);
	    if (!types.containsKey(ta))
		return "Unbekannte Klemme " + a;
	    if (!types.containsKey(tb))
		return "Unbekannte Klemme " + b;
	    String key = a.compareTo(b) < 0 ? a + "|" + b : b + "|" + a;
	    if (seenWire.containsKey(key))
		return "Doppelte Leitung " + a + "–" + b;
	    seenWire.put(key, Boolean.TRUE);
	}
	return null;
    }

    private void build(JavaScriptObject components, JavaScriptObject wires, boolean wantSupply) {
	resetMaps();
	app.loader.clearCircuit();
	int n = getLen(components);
	for (int i = 0; i < n; i++)
	    addComponent(getAt(components, i), wantSupply);
	for (String key : terminals.keySet()) {
	    Term t = terminals.get(key);
	    t.elm.setPoints();
	}
	for (int i = 0; i < commonFrom.size(); i++) {
	    CircuitElm a = commonFrom.get(i), b = commonTo.get(i);
	    a.setPoints();
	    b.setPoints();
	    Point pa = a.getPost(0), pb = b.getPost(0);
	    if (pa == null || pb == null || (pa.x == pb.x && pa.y == pb.y))
		continue;
	    WireElm link = new WireElm(pa.x, pa.y, pb.x, pb.y, 0, new StringTokenizer(""));
	    link.setPoints();
	    addElm(link);
	}
	int nw = getLen(wires);
	for (int i = 0; i < nw; i++) {
	    JavaScriptObject w = getAt(wires, i);
	    addWire(normalizeTerm(getStr(w, "a", "")), normalizeTerm(getStr(w, "b", "")));
	}
    }

    private void addComponent(JavaScriptObject c, boolean wantSupply) {
	String id = getStr(c, "id", "");
	String type = getStr(c, "type", "");
	String designation = getStr(c, "designation", id);
        if ("motorprotection".equals(type)) {
            EGTWorkbenchMotorProtectionElm m = new EGTWorkbenchMotorProtectionElm(nextX(), nextY());
            m.designation = designation;
            m.iSet = Math.max(.1, Math.min(100, getNum(c, "ratedCurrent", 2.8)));
            m.magneticMultiple = 13;
            m.tripTime = 5;
            m.switchedOn = getBool(c, "protectionOn", false);
            m.tripped = getBool(c, "protectionTripped", false);
            m.tripReason = (int)getNum(c, "protectionReason", 0);
            m.syncEndpoints(); addElm(m); motorProtectors.put(id, m);
            for (int i = 0; i < 6; i++) bind(id + "." + (i + 1), m, i);
            bump(); addAux(id, designation, 1, false); addAux(id, designation, 2, true);
            return;
        }
        if ("breaker".equals(type)) {
            EGTWorkbenchBreakerElm b = new EGTWorkbenchBreakerElm(nextX(), nextY());
            b.designation = designation;
            b.poles = (int)getNum(c, "poles", 1) == 3 ? 3 : 1;
            b.inA = EGTSicherungElm.nearestIn((int)getNum(c, "ratedCurrent", 16));
            String curve = getStr(c, "characteristic", "B");
            b.charIdx = "D".equals(curve) ? 2 : "C".equals(curve) ? 1 : 0;
            b.applySize(); b.layoutPins(); b.allocNodes(); b.syncEndpoints();
            b.switchedOn = getBool(c, "protectionOn", false);
            b.tripped = getBool(c, "protectionTripped", false);
            b.tripReason = (int)getNum(c, "protectionReason", 0);
            addElm(b); breakers.put(id, b);
            for (int i = 0; i < b.poles * 2; i++) bind(id + "." + (i + 1), b, i);
            bump(); return;
        }
        if ("timer".equals(type)) {
            EGTZeitrelaisSpuleElm timer = new EGTZeitrelaisSpuleElm(nextX(), nextY());
            timer.setEgtDesignation(designation);
            int mode = Math.max(0, Math.min(3, (int)getNum(c, "timerMode", 0)));
            timer.alignDelayMode(mode);
            timer.delay = Math.max(.01, Math.min(3600, getNum(c, "delayOn", 3)));
            timer.delayOff = Math.max(.01, Math.min(3600, getNum(c, "delayOff", 3)));
            timer.nom_v = Math.max(1, getNum(c, "nomV", 24));
            timer.nom_pow = Math.max(.1, getNum(c, "nomP", 2));
            timer.updateResistance(); timer.syncEndpoints();
            EGTZeitrelaisLink.clearDesignation(designation);
            addElm(timer); timers.put(id, timer);
            bind(id + ".A1", timer, 0); bind(id + ".A2", timer, 1); bump();
            EGTZeitrelaisKontaktElm no = new EGTZeitrelaisKontaktElm(nextX(), nextY());
            no.setEgtDesignation(designation); no.alignDelayMode(mode);
            no.syncEndpoints(); addElm(no); bump();
            EGTZeitrelaisKontaktElm nc = new EGTZeitrelaisKontaktElm(nextX(), nextY());
            nc.setEgtDesignation(designation); nc.alignDelayMode(mode);
            nc.flags |= EGTZeitrelaisKontaktElm.FLAG_NC;
            nc.syncEndpoints(); addElm(nc); bump();
            bind(id + ".15", no, 0);
            bind(id + ".18", no, 1);
            bind(id + ".16", nc, 1);
            commonFrom.add(no);
            commonTo.add(nc);
            return;
        }
        if ("relay".equals(type)) {
            EGTSchuetzSpuleElm k = new EGTSchuetzSpuleElm(nextX(), nextY());
            k.setEgtDesignation(designation);
            k.nom_v = getNum(c, "nomV", COIL_V);
            k.nom_pow = getNum(c, "nomP", COIL_P);
            k.updateResistance();
            k.syncEndpoints();
            addElm(k);
            relays.put(id, k);
            bind(id + ".A1", k, 0);
            bind(id + ".A2", k, 1);
            bump();
            JavaScriptObject config = getObj(c, "relayContacts");
            for (int i = 0; i < getLen(config); i++) {
                JavaScriptObject contact = getAt(config, i);
                addAux(id, designation, (int)getNum(contact, "number", i + 1), "NC".equals(getStr(contact, "kind", "NO")));
            }
            return;
        }
	if ("contactor".equals(type)) {
	    EGTLeistungsschuetzElm k = new EGTLeistungsschuetzElm(nextX(), nextY());
	    k.setEgtDesignation(designation.length() == 0 ? id : designation);
	    k.nom_v = getNum(c, "nomV", COIL_V);
	    k.nom_pow = getNum(c, "nomP", COIL_P);
	    k.updateResistance();
	    k.syncEndpoints();
	    addElm(k);
	    contactors.put(id, k);
	    bind(id + ".A1", k, EGTLeistungsschuetzElm.N_A1);
	    bind(id + ".A2", k, EGTLeistungsschuetzElm.N_A2);
	    bind(id + ".1", k, EGTLeistungsschuetzElm.N_1);
	    bind(id + ".2", k, EGTLeistungsschuetzElm.N_2);
	    bind(id + ".3", k, EGTLeistungsschuetzElm.N_3);
	    bind(id + ".4", k, EGTLeistungsschuetzElm.N_4);
	    bind(id + ".5", k, EGTLeistungsschuetzElm.N_5);
	    bind(id + ".6", k, EGTLeistungsschuetzElm.N_6);
	    bind(id + ".13", k, EGTLeistungsschuetzElm.N_13);
	    bind(id + ".14", k, EGTLeistungsschuetzElm.N_14);
	    bump();
	    return;
	}
	if ("auxiliary".equals(type)) {
	    String parent = getStr(c, "parentId", getStr(c, "parent", designation));
	    addAux(id, parent, 5, false);
	    addAux(id, parent, 6, true);
	    addAux(id, parent, 7, true);
	    addAux(id, parent, 8, false);
	    return;
	}
	if ("psu".equals(type)) {
	    EGTGleichspannungsquelleElm g = new EGTGleichspannungsquelleElm(nextX(), nextY());
            g.setEgtDesignation(designation.length() == 0 ? id : designation);
	    double set = getNum(c, "voltage", 24);
	    if (set < 1)
		set = 1;
	    g.voltage = wantSupply ? set : 0;
	    g.setPoints();
	    addElm(g);
	    psus.put(id, g);
	    psuSetpoint.put(id, Double.valueOf(set));
	    bind(id + ".+24", g, 0);
	    bind(id + ".0V", g, 1);
	    bump();
	    return;
	}
	if ("mains".equals(type)) {
	    EGTDrehstromquelleElm g = new EGTDrehstromquelleElm(nextX(), nextY());
	    g.setEgtDesignation(designation.length() == 0 ? id : designation);
	    double uLL = getNum(c, "voltage", 400);
	    if (uLL < 1)
		uLL = 1;
	    g.voltageRMS = wantSupply ? lineToPhase(uLL) : 0;
	    g.frequency = 50;
	    g.setPoints();
	    addElm(g);
	    mains.put(id, g);
	    mainsSetpoint.put(id, Double.valueOf(uLL));
	    bind(id + ".L1", g, 0);
	    bind(id + ".L2", g, 1);
	    bind(id + ".L3", g, 2);
	    bind(id + ".N", g, 3);
	    bind(id + ".PE", g, 4);
	    bump();
	    return;
	}
        if ("selector".equals(type)) {
            EGTTasterElm[] pair = new EGTTasterElm[2];
            for (int i = 0; i < 2; i++) {
                pair[i] = new EGTTasterElm(nextX(), nextY());
                pair[i].setEgtDesignation(designation + (i == 0 ? " A" : " B"));
                addElm(pair[i]);
                bind(id + (i == 0 ? ".13" : ".23"), pair[i], 0);
                bind(id + (i == 0 ? ".14" : ".24"), pair[i], 1);
                bump();
            }
            selectors.put(id, pair);
            setSelector(id, (int)getNum(c, "selection", 0));
            return;
        }
	if ("start".equals(type) || "stop".equals(type) || "emergency".equals(type)) {
	    addTasterStation(id, designation.length() == 0 ? id : designation, !"emergency".equals(type));
	    return;
	}
	if ("terminal".equals(type)) {
	    int poles = (int) getNum(c, "poles", 6);
	    if (poles < 1)
		poles = 1;
	    if (poles > 12)
		poles = 12;
	    int span = (int) getNum(c, "span", 1);
	    if (span != 2 && span != 4)
		span = 1;
	    for (int start = 1; start <= poles; start += span) {
		int count = Math.min(span, poles - start + 1);
		String[] keys = new String[count * 2];
		for (int i = 0; i < count; i++) {
		    keys[i * 2] = id + "." + (start + i) + "o";
		    keys[i * 2 + 1] = id + "." + (start + i) + "u";
		}
		bindCommon(keys);
	    }
	    return;
	}
	if ("rail".equals(type)) {
	    int screws = (int) getNum(c, "screws", 5);
	    if (screws < 2)
		screws = 2;
	    if (screws > 8)
		screws = 8;
	    String[] bars = { "+24", "0V", "PE" };
	    for (int b = 0; b < bars.length; b++) {
		String[] keys = new String[screws];
		for (int i = 1; i <= screws; i++)
		    keys[i - 1] = id + "." + bars[b] + "." + i;
		bindCommon(keys);
	    }
	    return;
	}
        if ("lamp".equals(type) || "pilot".equals(type)) {
            boolean pilot = "pilot".equals(type);
            EGTLeuchteElm light = pilot ? new EGTMeldeleuchteElm(nextX(), nextY()) : new EGTLeuchteElm(nextX(), nextY());
            light.setEgtDesignation(designation);
            light.nom_v = getNum(c, "nomV", 24);
            light.nom_pow = getNum(c, "nomP", pilot ? 1.5 : 10);
            light.updateResistance();
            addElm(light);
            lamps.put(id, light);
            bind(id + (pilot ? ".X1" : ".L"), light, 0);
            bind(id + (pilot ? ".X2" : ".N"), light, light.lampReturnPin());
            if (!pilot) bind(id + ".PE", light, 1);
            bump();
            return;
        }
	if ("motor".equals(type) || "lineardrive".equals(type)) {
	    EGTGleichstrommotorElm m = new EGTGleichstrommotorElm(nextX(), nextY());
	    m.setEgtDesignation(designation.length() == 0 ? id : designation);
	    m.nom_v = getNum(c, "nomV", MOTOR_V);
	    m.nom_p = getNum(c, "nomP", MOTOR_P);
	    m.updateResistance();
	    m.syncEndpoints();
	    addElm(m);
	    motors.put(id, m);
	    bind(id + ".+", m, 0);
	    bind(id + ".−", m, 1);
	    bind(id + ".-", m, 1);
	    bump();
            if ("lineardrive".equals(type)) {
                for (int i = 1; i <= 3; i++) {
                    String sensorId = id + ".B" + i;
                    EGTWechselschalterElm sensor = new EGTWechselschalterElm(nextX(), nextY());
                    sensor.setEgtDesignation(designation + " B" + i);
                    addElm(sensor);
                    driveSensors.put(sensorId, sensor);
                    bind(sensorId + "_11", sensor, 2);
                    bind(sensorId + "_12", sensor, 0);
                    bind(sensorId + "_14", sensor, 1);
                    bump();
                }
                // Unidentified sensor sockets remain independent passive test points.
                String[] spare = { "PE", "24V", "0V", "B4", "B5", "B6", "B7" };
                for (String pin : spare) {
                    OutputElm socket = new OutputElm(nextX(), nextY());
                    socket.x2 = socket.x + 32;
                    addElm(socket);
                    bind(id + "." + pin, socket, 0);
                    bump();
                }
                setDrivePosition(id, getNum(c, "slide", .5));
            }
	    return;
	}
	if ("acmotor".equals(type)) {
	    EGTDrehstrommotorElm m = new EGTDrehstrommotorElm(nextX(), nextY());
	    m.x2 = m.x + 128;
	    m.y2 = m.y;
	    m.setEgtDesignation(designation.length() == 0 ? id : designation);
	    double kw = getNum(c, "ratedKw", EGTDrehstrommotorElm.P_REF_KW);
	    double eta = getNum(c, "eta", EGTDrehstrommotorElm.DEF_ETA);
	    if (kw < EGTDrehstrommotorElm.P_MIN_KW)
		kw = EGTDrehstrommotorElm.P_MIN_KW;
	    if (kw > EGTDrehstrommotorElm.P_MAX_KW)
		kw = EGTDrehstrommotorElm.P_MAX_KW;
	    m.ratedKw = kw;
	    m.eta = EGTDrehstrommotorElm.parseEtaToken(eta);
	    m.applyRatedPower();
	    m.setPoints();
	    addElm(m);
	    acMotors.put(id, m);
	    bind(id + ".U1", m, 0);
	    bind(id + ".U2", m, 1);
	    bind(id + ".V1", m, 2);
	    bind(id + ".V2", m, 3);
	    bind(id + ".W1", m, 4);
	    bind(id + ".W2", m, 5);
	    bump();
	    OutputElm pe = new OutputElm(nextX(), nextY());
	    pe.x2 = pe.x + 32;
	    addElm(pe);
	    bind(id + ".PE", pe, 0);
	    bump();
	    return;
	}
    }

    private void addTasterStation(String id, String designation, boolean withLamp) {
	EGTTasterElm no = new EGTTasterElm(nextX(), nextY());
	no.setEgtDesignation(designation);
	no.setPoints();
	addElm(no);
	tasters.put(id, no);
	bind(id + ".13", no, 0);
	bind(id + ".14", no, 1);
	bump();

	EGTTasterElm nc = new EGTTasterElm(nextX(), nextY());
	nc.setEgtDesignation(designation);
	nc.flags |= EGTStyle.FLAG_TASTER_NC;
	nc.setPoints();
	addElm(nc);
	tastersNc.put(id, nc);
	bind(id + ".21", nc, 0);
	bind(id + ".22", nc, 1);
	bump();
	if (!withLamp) return;

	EGTMeldeleuchteElm p = new EGTMeldeleuchteElm(nextX(), nextY());
	p.setEgtDesignation(lampName(id));
	p.nom_v = 24;
	p.nom_pow = 1.5;
	p.updateResistance();
	p.setPoints();
	addElm(p);
	lamps.put(id, p);
	bind(id + ".X1", p, 0);
	bind(id + ".X2", p, 1);
	bump();
    }

    private String lampName(String id) {
	StringBuffer d = new StringBuffer();
	for (int i = 0; i < id.length(); i++) {
	    char c = id.charAt(i);
	    if (c >= '0' && c <= '9')
		d.append(c);
	}
	return d.length() == 0 ? "P1" : "P" + d.toString();
    }

    private void addAux(String htmlId, String designation, int contactNo, boolean nc) {
	EGTSchuetzHilfskontaktElm c = new EGTSchuetzHilfskontaktElm(nextX(), nextY());
	c.setEgtDesignation(designation);
	c.contactNo = contactNo;
	if (nc)
	    c.flags |= EGTSchuetzHilfskontaktElm.FLAG_NC;
	else
	    c.flags &= ~EGTSchuetzHilfskontaktElm.FLAG_NC;
	c.clampContactNo();
	c.syncEndpoints();
	addElm(c);
	String a = htmlId + "." + c.labTop();
	String b = htmlId + "." + c.labBot();
	bind(a, c, 0);
	bind(b, c, 1);
	auxContacts.put(a, c);
	bump();
    }

    private void addWire(String a, String b) {
	Term ta = terminals.get(a);
	Term tb = terminals.get(b);
	if (ta == null || tb == null)
	    throw new RuntimeException("Klemme fehlt: " + a + " / " + b);
	ta.elm.setPoints();
	tb.elm.setPoints();
	Point pa = ta.elm.getPost(ta.post);
	Point pb = tb.elm.getPost(tb.post);
	if (pa == null || pb == null)
	    throw new RuntimeException("Post fehlt: " + a + " / " + b);
	if (pa.x == pb.x && pa.y == pb.y)
	    return;
	WireElm w = new WireElm(pa.x, pa.y, pb.x, pb.y, 0, new StringTokenizer(""));
	w.setPoints();
	addElm(w);
	wireElms.put(a + "|" + b, w);
    }

    private void addElm(CircuitElm elm) {
	elm.setPoints();
	app.elmList.addElement(elm);
    }

    private void bind(String key, CircuitElm elm, int post) {
	terminals.put(key, new Term(elm, post));
    }

    /** Mehrere Schrauben, ein Knoten. OutputElm trägt den Post, ohne Last. */
    private void bindCommon(String[] keys) {
	OutputElm node = new OutputElm(nextX(), nextY());
	node.x2 = node.x + 32;
	addElm(node);
	for (int i = 0; i < keys.length; i++)
	    bind(keys[i], node, 0);
	bump();
    }

    private int nextX() { return ORIGIN_X + placeCol * STEP_X; }
    private int nextY() { return ORIGIN_Y + placeRow * STEP_Y; }

    private void bump() {
	placeCol++;
	if (placeCol >= COLS) {
	    placeCol = 0;
	    placeRow++;
	}
    }

    private static boolean validType(String type) {
	return "timer".equals(type) || "motorprotection".equals(type) || "breaker".equals(type) || "relay".equals(type) || "contactor".equals(type) || "auxiliary".equals(type)
		|| "psu".equals(type) || "mains".equals(type) || "start".equals(type)
		|| "stop".equals(type) || "motor".equals(type) || "acmotor".equals(type) || "lineardrive".equals(type)
                || "lamp".equals(type) || "pilot".equals(type) || "emergency".equals(type) || "selector".equals(type)
		|| "terminal".equals(type) || "rail".equals(type);
    }

    private static String termOwner(String key) {
	int d = key.indexOf('.');
	return d < 0 ? key : key.substring(0, d);
    }

    private static String normalizeTerm(String key) {
	if (key == null)
	    return "";
	if (key.endsWith(".-"))
	    return key.substring(0, key.length() - 2) + ".−";
	return key;
    }

    private String ok() {
	return "{\"ok\":true,\"version\":\"" + VERSION + "\",\"revision\":"
		+ loadedRevision + "}";
    }

    private String fail(String error) {
	return "{\"ok\":false,\"error\":\"" + esc(error) + "\"}";
    }

    private static String esc(String s) {
	if (s == null)
	    return "";
	return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    private static String jsonNum(double v) {
	if (Double.isNaN(v) || Double.isInfinite(v))
	    return "null";
	return String.valueOf(v);
    }

    private native JavaScriptObject getObj(JavaScriptObject o, String k) /*-{
	var v = o[k];
	return (v && typeof v === 'object') ? v : null;
    }-*/;

    private native String getStr(JavaScriptObject o, String k, String d) /*-{
	var v = o[k];
	if (v == null || v === undefined)
	    return d;
	return String(v);
    }-*/;

    private native double getNum(JavaScriptObject o, String k, double d) /*-{
	var v = o[k];
	if (typeof v === 'number' && isFinite(v))
	    return v;
	if (typeof v === 'string' && v !== '' && isFinite(+v))
	    return +v;
	return d;
    }-*/;

    private native boolean getBool(JavaScriptObject o, String k, boolean d) /*-{
	var v = o[k];
	if (typeof v === 'boolean')
	    return v;
	return d;
    }-*/;

    private native int getLen(JavaScriptObject a) /*-{
	return a && a.length ? a.length : 0;
    }-*/;

    private native JavaScriptObject getAt(JavaScriptObject a, int i) /*-{
	return a[i];
    }-*/;
}

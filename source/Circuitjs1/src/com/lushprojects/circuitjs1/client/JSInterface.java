package com.lushprojects.circuitjs1.client;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;

public class JSInterface {

    CirSim app;
    EGTWorkbenchBridge workbench;

    JSInterface(CirSim app) {
	this.app = app;
	workbench = new EGTWorkbenchBridge(app);
    }

    void setExtVoltage(String name, double v) {
	int i;
	for (i = 0; i != app.elmList.size(); i++) {
	    CircuitElm ce = app.getElm(i);
	    if (ce instanceof ExtVoltageElm) {
		ExtVoltageElm eve = (ExtVoltageElm) ce;
		if (eve.getName().equals(name))
		    eve.setVoltage(v);
	    } else if (ce instanceof EGTMiniSpsElm) {
		((EGTMiniSpsElm) ce).setHmiInputVoltage(name, v);
	    }
	}
    }

    native JsArray<JavaScriptObject> getJSArray() /*-{ return []; }-*/;

    JsArray<JavaScriptObject> getJSElements() {
	int i;
	JsArray<JavaScriptObject> arr = getJSArray();
	for (i = 0; i != app.elmList.size(); i++) {
	    CircuitElm ce = app.getElm(i);
	    ce.addJSMethods();
	    arr.push(ce.getJavaScriptObject());
	}
	return arr;
    }

    double getLabeledNodeVoltage(String name) {
	for (int i = 0; i != app.elmList.size(); i++) {
	    CircuitElm ce = app.getElm(i);
	    if (!(ce instanceof EGTMiniSpsElm))
		continue;
	    double voltage = ((EGTMiniSpsElm) ce).getHmiOutputVoltage(name);
	    if (!Double.isNaN(voltage))
		return voltage;
	}
	return app.sim.getLabeledNodeVoltage(name);
    }

    // Delegate methods for JSNI access
    void setSimRunning(boolean run) { app.setSimRunning(run); }
    boolean simIsRunning() { return app.simIsRunning(); }
    void doExportAsSVGFromAPI() { app.imageExporter.doExportAsSVGFromAPI(); }
    String dumpCircuit() { return app.dumpCircuit(); }
    void importCircuitFromText(String t, boolean s) { app.importCircuitFromText(t, s); }
    String workbenchVersion() { return EGTWorkbenchBridge.VERSION; }
    String workbenchLoadProject(JavaScriptObject project) { return workbench.loadProject(project); }
    boolean workbenchSetPressed(String id, boolean pressed) { return workbench.setPressed(id, pressed); }
    boolean workbenchSetProtection(String id, String action) { return workbench.setProtection(id, action); }
    boolean workbenchSetSelector(String id, int position) { return workbench.setSelector(id, position); }
    boolean workbenchSetDrivePosition(String id, double position) { return workbench.setDrivePosition(id, position); }
    void workbenchSetRunning(boolean running) { workbench.setRunning(running); }
    String workbenchResetSimulation(JavaScriptObject project) { return workbench.resetSimulation(project); }
    void workbenchSetSupplyOn(boolean on) { workbench.setSupplyOn(on); }
    boolean workbenchSetPsuVoltage(String id, double v) { return workbench.setPsuVoltage(id, v); }
    boolean workbenchSetRatings(String id, double nomV, double nomP) { return workbench.setRatings(id, nomV, nomP); }
    boolean workbenchSetAcMotorRatings(String id, double kw, double eta) { return workbench.setAcMotorRatings(id, kw, eta); }
    void workbenchSetRealtime(boolean on) {
	app.ui.setEgtRealtimeAvailable(true);
	app.ui.setEgtRealtimeMode(on);
    }
    String workbenchReadSnapshot() { return workbench.readSnapshot(); }
    double getTime() { return app.sim.t; }
    double getTimeStep() { return app.sim.timeStep; }
    void setTimeStep(double ts) { app.sim.timeStep = ts; }
    double getMaxTimeStep() { return app.sim.maxTimeStep; }
    void setMaxTimeStep(double ts) { app.sim.maxTimeStep = app.sim.timeStep = ts; }

    native void setupJSInterface() /*-{
	var that = this;
	$wnd.CircuitJS1 = {
	    setSimRunning: $entry(function(run) { that.@com.lushprojects.circuitjs1.client.JSInterface::setSimRunning(Z)(run); } ),
	    getTime: $entry(function() { return that.@com.lushprojects.circuitjs1.client.JSInterface::getTime()(); } ),
	    getTimeStep: $entry(function() { return that.@com.lushprojects.circuitjs1.client.JSInterface::getTimeStep()(); } ),
	    setTimeStep: $entry(function(ts) { that.@com.lushprojects.circuitjs1.client.JSInterface::setTimeStep(D)(ts); } ), // don't use this, see #843
	    getMaxTimeStep: $entry(function() { return that.@com.lushprojects.circuitjs1.client.JSInterface::getMaxTimeStep()(); } ),
	    setMaxTimeStep: $entry(function(ts) { that.@com.lushprojects.circuitjs1.client.JSInterface::setMaxTimeStep(D)(ts); } ),
	    isRunning: $entry(function() { return that.@com.lushprojects.circuitjs1.client.JSInterface::simIsRunning()(); } ),
	    getNodeVoltage: $entry(function(n) { return that.@com.lushprojects.circuitjs1.client.JSInterface::getLabeledNodeVoltage(Ljava/lang/String;)(n); } ),
	    setExtVoltage: $entry(function(n, v) { that.@com.lushprojects.circuitjs1.client.JSInterface::setExtVoltage(Ljava/lang/String;D)(n, v); } ),
	    getElements: $entry(function() { return that.@com.lushprojects.circuitjs1.client.JSInterface::getJSElements()(); } ),
	    getCircuitAsSVG: $entry(function() { return that.@com.lushprojects.circuitjs1.client.JSInterface::doExportAsSVGFromAPI()(); } ),
	    exportCircuit: $entry(function() { return that.@com.lushprojects.circuitjs1.client.JSInterface::dumpCircuit()(); } ),
	    importCircuit: $entry(function(circuit, subcircuitsOnly) { return that.@com.lushprojects.circuitjs1.client.JSInterface::importCircuitFromText(Ljava/lang/String;Z)(circuit, subcircuitsOnly); }),
	    workbench: {
		version: $entry(function() { return that.@com.lushprojects.circuitjs1.client.JSInterface::workbenchVersion()(); }),
		loadProject: $entry(function(project) {
		    if (typeof project === 'string') {
			try { project = JSON.parse(project); } catch (e) {
			    return JSON.stringify({ok:false,error:String(e)});
			}
		    }
		    return that.@com.lushprojects.circuitjs1.client.JSInterface::workbenchLoadProject(Lcom/google/gwt/core/client/JavaScriptObject;)(project);
		}),
		setPressed: $entry(function(id, pressed) { return that.@com.lushprojects.circuitjs1.client.JSInterface::workbenchSetPressed(Ljava/lang/String;Z)(id, !!pressed); }),
                setProtection: $entry(function(id, action) { return that.@com.lushprojects.circuitjs1.client.JSInterface::workbenchSetProtection(Ljava/lang/String;Ljava/lang/String;)(id, action); }),
                setSelector: $entry(function(id, position) { return that.@com.lushprojects.circuitjs1.client.JSInterface::workbenchSetSelector(Ljava/lang/String;I)(id, position|0); }),
                setDrivePosition: $entry(function(id, position) { return that.@com.lushprojects.circuitjs1.client.JSInterface::workbenchSetDrivePosition(Ljava/lang/String;D)(id, +position); }),
		setRunning: $entry(function(running) { that.@com.lushprojects.circuitjs1.client.JSInterface::workbenchSetRunning(Z)(!!running); }),
		resetSimulation: $entry(function(project) { return that.@com.lushprojects.circuitjs1.client.JSInterface::workbenchResetSimulation(Lcom/google/gwt/core/client/JavaScriptObject;)(project); }),
		setSupplyOn: $entry(function(on) { that.@com.lushprojects.circuitjs1.client.JSInterface::workbenchSetSupplyOn(Z)(!!on); }),
		setPsuVoltage: $entry(function(id, v) { return that.@com.lushprojects.circuitjs1.client.JSInterface::workbenchSetPsuVoltage(Ljava/lang/String;D)(id, +v); }),
		setRatings: $entry(function(id, nv, np) { return that.@com.lushprojects.circuitjs1.client.JSInterface::workbenchSetRatings(Ljava/lang/String;DD)(id, +nv, +np); }),
		setAcMotorRatings: $entry(function(id, kw, eta) { return that.@com.lushprojects.circuitjs1.client.JSInterface::workbenchSetAcMotorRatings(Ljava/lang/String;DD)(id, +kw, +eta); }),
		setRealtime: $entry(function(on) { that.@com.lushprojects.circuitjs1.client.JSInterface::workbenchSetRealtime(Z)(!!on); }),
		readSnapshot: $entry(function() { return that.@com.lushprojects.circuitjs1.client.JSInterface::workbenchReadSnapshot()(); })
	    }
	};
	var hook = $wnd.oncircuitjsloaded;
	if (hook)
	    hook($wnd.CircuitJS1);
    }-*/;

    native void callUpdateHook() /*-{
	var hook = $wnd.CircuitJS1.onupdate;
	if (hook)
	    hook($wnd.CircuitJS1);
    }-*/;

    native void callAnalyzeHook() /*-{
	var hook = $wnd.CircuitJS1.onanalyze;
	if (hook)
	    hook($wnd.CircuitJS1);
    }-*/;

    native void callTimeStepHook() /*-{
	var hook = $wnd.CircuitJS1.ontimestep;
	if (hook)
	    hook($wnd.CircuitJS1);
    }-*/;

    native void callSVGRenderedHook(String svgData) /*-{
	var hook = $wnd.CircuitJS1.onsvgrendered;
	if (hook)
	    hook($wnd.CircuitJS1, svgData);
    }-*/;
}

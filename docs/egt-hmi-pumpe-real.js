(function (root) {
  "use strict";
  function PumpController() { this.api = null; this.snapshot = null; this.processTime = null; this.wiring = {}; }
  PumpController.prototype.attach = function (sim) {
    this.sim = sim;
    this.api = sim && sim.pump || null;
    this.realtimeReady = false;
    this.snapshot = null;
    this.processTime = null;
    this.wiring = {};
    return !!this.api;
  };
  PumpController.prototype.read = function (level, wellHasWater) {
    if (!this.api) return this.snapshot = null;
    try {
      this.api.setProcessState({ level: level, wellHasWater: wellHasWater });
      this.snapshot = this.api.readSnapshot();
      if (!this.realtimeReady && this.device("sps:A1").present && this.sim.workbench && typeof this.sim.workbench.setRealtime === "function") {
        this.sim.workbench.setRealtime(true);
        this.realtimeReady = true;
      }
    } catch (e) { this.snapshot = null; }
    return this.snapshot;
  };
  PumpController.prototype.device = function (id) {
    var item = this.snapshot && this.snapshot.devices[id];
    return item && item.present ? item : {};
  };
  PumpController.prototype.action = function (id, action) {
    return !!(this.api && this.api.setDeviceAction(id, action));
  };
  PumpController.prototype.pumping = function () {
    return !!(this.snapshot && this.snapshot.running && this.device("motor:M1").running);
  };
  PumpController.prototype.processDelta = function () {
    var time = this.snapshot && this.snapshot.time;
    var elapsed = this.processTime == null || time == null ? 0 : time - this.processTime;
    this.processTime = time;
    return this.snapshot && this.snapshot.running && elapsed > 0 ? Math.min(0.08, elapsed) : 0;
  };
  PumpController.prototype.reset = function () {
    if (this.api) this.api.reset();
    this.snapshot = null;
    this.processTime = null;
  };
  PumpController.prototype.releaseButtons = function () {
    this.action("button:S4", "release"); this.action("button:S5", "release");
  };
  PumpController.prototype.render = function (doc, level, dry) {
    var self = this;
    function el(id) { return doc.getElementById(id); }
    function text(id, value) { var node = el(id); if (node && node.textContent !== value) node.textContent = value; }
    function on(id, value, cls) { if (el(id)) el(id).classList.toggle(cls || "on", !!value); }
    var plc = this.device("sps:A1"), coil = this.device("coil:Q1"), contacts = this.device("contacts:Q1");
    var motor = this.device("motor:M1"), protection = this.device("protection:F1");
    var errors = this.snapshot ? this.snapshot.errors : ["Pumpenverbindung fehlt"];
    var supplied = !!plc.supplied, moving = this.pumping(), stopped = !!this.device("estop:S0").pressed;
    var inputs = plc.inputs || [], outputs = plc.outputs || [];
    if (!plc.present) this.wiring = {};
    ["inputDevices", "outputDevices"].forEach(function (key) {
      var rows = plc[key];
      if (plc.present && Array.isArray(rows) && rows.length === 8 && rows.every(Array.isArray)) {
        self.wiring[key] = rows.map(function (row) { return row.slice(); });
      }
    });
    for (var i = 1; i <= 8; i++) {
      var names=this.wiring.inputDevices&&this.wiring.inputDevices[i-1], loads=this.wiring.outputDevices&&this.wiring.outputDevices[i-1];
      var inputLabel=!plc.present?"SPS A1 fehlt":!names?"Zuordnung wird ermittelt":names.length?names.join("\n"):"Nicht zugeordnet";
      var outputLabel=!plc.present?"SPS A1 fehlt":i>plc.outputCount?"Nicht vorhanden (8/4)":!loads?"Zuordnung wird ermittelt":loads.length?loads.join("\n"):"Nicht zugeordnet";
      text("inputFn"+i,inputLabel);text("outputFn"+i,outputLabel);
      el("inputFn"+i).title=inputLabel.replace(/\n/g, ", ");el("outputFn"+i).title=outputLabel.replace(/\n/g, ", ");
      ["I", "Q"].forEach(function (kind) {
        var dot = el("led" + kind + i), active = kind === "I" ? inputs[i - 1] : outputs[i - 1];
        if (dot) { dot.className = "dot" + (active ? " on-green" : ""); dot.setAttribute("aria-label", kind + i + (active ? ": EIN" : ": AUS")); }
      });
    }
    on("contactor", coil.energized, "energized"); on("contactor", contacts.closed, "closed");
    el("contactor").setAttribute("aria-label", "Schütz Q1: " + (coil.present ? (coil.energized ? "angezogen" : "abgefallen") : "fehlt")
      + ", " + (contacts.present ? (contacts.closed ? "Leistungskontakte geschlossen" : "Leistungskontakte offen") : "Leistungskontakte fehlen"));
    [["B1", "sensorB1Card", "Minimum"], ["B2", "sensorB2Card", "Maximum"], ["B3", "wellCap", "Brunnen"]].forEach(function (row) {
      var sensor = self.device("sensor:" + row[0]);
      var channels = [];
      (self.wiring.inputDevices || []).forEach(function (names, index) {
        if (names.some(function (name) { return name.indexOf(row[0] + " ") === 0; })) channels.push("I" + (index + 1));
      });
      var assignment = !plc.present ? "SPS A1 fehlt" : !self.wiring.inputDevices ? "Zuordnung wird ermittelt"
        : channels.length ? "SPS-Eingang: " + channels.join(", ") : "Kein SPS-Eingang zugeordnet";
      text("sensorInput" + row[0], channels.length ? channels.join(", ") : "—");
      el("sensorInput" + row[0]).title = row[0] + ": " + assignment;
      var actuated = !!(sensor.present && sensor.actuated);
      on("sensor" + row[0], actuated, "actuated");
      on(row[1], actuated, "actuated");
      var description = row[0] + " " + row[2] + ": " + (!sensor.present ? "Sensor fehlt" : actuated ? "betätigt" : "nicht betätigt") + " · " + assignment;
      el(row[1]).setAttribute("aria-label", description);
      el(row[1]).setAttribute("title", description);
    });
    on("tbP1", this.device("lamp:P1").on); on("tbP2", this.device("lamp:P2").on);
    [ ["mlQ2", "P3", "on-yellow"], ["mlQ3", "P4", "on-green"], ["mlQ5", "P5", "on-yellow"] ].forEach(function (row) {
      if (el(row[0])) el(row[0]).className = "ml" + (self.device("lamp:" + row[1]).on ? " " + row[2] : "");
    });
    on("alarm", this.device("horn:H1").on);
    on("btnEin", this.device("button:S4").pressed, "active");
    on("btnAus", this.device("button:S5").pressed, "active");
    on("btnNotAus", stopped, "active");
    el("btnNotAus").setAttribute("aria-pressed", String(stopped));
    el("btnNotAusRelease").disabled = !this.device("estop:S0").present || !stopped;
    el("modeAuto").checked = !!this.device("selector:S7").closed;
    el("swF1").checked = !!protection.tripped;
    el("swI4").checked = !!this.device("sensor:B2").handTest;
    el("swI5").checked = !!this.device("sensor:B3").handTest;
    el("pumpBindingErrors").hidden = !errors.length;
    text("pumpBindingErrors", errors.join("; "));
    var message = !this.snapshot ? "Pumpenverbindung fehlt – Seite neu laden" : errors.length ? "Bauteilzuordnung prüfen"
      : !supplied ? "SPS A1 ohne Versorgung" : !plc.run ? "SPS A1: STOP"
      : protection.tripped ? "Motorschutz F1 ausgelöst" : !protection.closed ? "Motorschutz F1 ausgeschaltet"
      : stopped ? "Not-Aus S0 betätigt" : moving && dry ? "Trockenlauf – B3 und I5 prüfen"
      : level >= 100 ? "Überlauf – B2, I4 und FUP prüfen"
      : outputs[0] && !coil.energized ? "A1.Q1 EIN – Schützspule Q1 nicht angezogen"
      : coil.energized && !motor.running ? "Q1 angezogen – Leistungskreis / Motor prüfen"
      : "Elektrische Anlage verbunden";
    text("sysMsg", message);
    el("sysMsg").className = "v" + (errors.length || protection.tripped || stopped || moving && dry || level >= 100 ? " state-fault" : "");
    text("anlageState", level >= 100 ? "Überlauf" : moving ? (dry ? "Trockenlauf" : "Pumpe fördert")
      : !supplied ? "Spannungslos" : "Stillstand");
    el("anlageState").className = "v" + (level >= 100 || moving && dry ? " state-fault" : moving ? " state-ok" : "");
    text("driveDir", moving ? (dry ? "Trockenlauf" : "Fördert") : "Stillstand");
    el("driveDir").className = "pump-dir" + (moving ? (dry ? " hot" : " run") : "");
  };
  root.EGTPumpController = PumpController;
})(window);

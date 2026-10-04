(function (root) {
  "use strict";
  function PumpController() { this.api = null; this.snapshot = null; this.processTime = null; }
  PumpController.prototype.attach = function (sim) {
    this.api = sim && sim.pump || null;
    this.snapshot = null;
    this.processTime = null;
    return !!this.api;
  };
  PumpController.prototype.read = function (level, wellHasWater) {
    if (!this.api) return this.snapshot = null;
    try {
      this.api.setProcessState({ level: level, wellHasWater: wellHasWater });
      this.snapshot = this.api.readSnapshot();
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
    function text(id, value) { if (el(id)) el(id).textContent = value; }
    function on(id, value, cls) { if (el(id)) el(id).classList.toggle(cls || "on", !!value); }
    var plc = this.device("sps:A1"), coil = this.device("coil:Q1"), contacts = this.device("contacts:Q1");
    var motor = this.device("motor:M1"), protection = this.device("protection:F1");
    var errors = this.snapshot ? this.snapshot.errors : ["Pumpenverbindung fehlt"];
    var supplied = !!plc.supplied, moving = this.pumping(), stopped = !!this.device("estop:S0").pressed;
    var inputs = plc.inputs || [], outputs = plc.outputs || [];
    for (var i = 1; i <= 8; i++) {
      ["I", "Q"].forEach(function (kind) {
        var dot = el("led" + kind + i), active = kind === "I" ? inputs[i - 1] : outputs[i - 1];
        if (dot) { dot.className = "dot" + (active ? " on-green" : ""); dot.setAttribute("aria-label", kind + i + (active ? ": EIN" : ": AUS")); }
      });
    }
    on("contactor", coil.energized, "energized"); on("contactor", contacts.closed, "closed");
    el("contactor").setAttribute("aria-label", "Schütz Q1: " + (coil.present ? (coil.energized ? "angezogen" : "abgefallen") : "fehlt")
      + ", " + (contacts.present ? (contacts.closed ? "Leistungskontakte geschlossen" : "Leistungskontakte offen") : "Leistungskontakte fehlen"));
    [["B1", "sensorMinState", 3], ["B2", "sensorMaxState", 4], ["B3", "sensorWellState", 5]].forEach(function (row) {
      var sensor = self.device("sensor:" + row[0]);
      text(row[1], !sensor.present ? row[0] + ": Sensor fehlt" : row[0] + ": Kontakt " + (sensor.closed ? "geschlossen" : "offen")
        + " · I" + row[2] + ": " + (inputs[row[2] - 1] ? "EIN" : "AUS") + (sensor.handTest ? " · Handtest" : ""));
    });
    on("sensorB1", this.device("sensor:B1").actuated, "actuated");
    on("sensorB2", this.device("sensor:B2").actuated, "actuated");
    on("sensorB3", this.device("sensor:B3").actuated, "actuated");
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

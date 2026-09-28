package com.lushprojects.circuitjs1.client;

class EGTWorkbenchPotElm extends PotElm {
    double setting = .5;
    boolean configured;

    EGTWorkbenchPotElm(int x, int y) { super(x, y); }

    void setPosition(double value) {
        setting = Math.max(0, Math.min(1, value));
        configured = true;
        slider.setValue((int)Math.round(setting * 100));
        setPoints();
    }

    void setPoints() {
        super.setPoints();
        if (configured) position = setting;
    }

    public void execute() {
        setting = slider.getValue() / 100.0;
        configured = true;
        super.execute();
    }

    void stamp() {
        // A small contact resistance keeps the matrix finite at either end stop.
        resistance1 = Math.max(.001, maxResistance * position);
        resistance2 = Math.max(.001, maxResistance * (1 - position));
        sim.stampResistor(nodes[0], nodes[2], resistance1);
        sim.stampResistor(nodes[2], nodes[1], resistance2);
    }
}

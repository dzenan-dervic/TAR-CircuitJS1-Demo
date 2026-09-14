package com.lushprojects.circuitjs1.client;

/** EGT-Bauteil mit Betriebsmittelkennzeichen (Q1, K1, S1, …). */
interface EGTDesignatable {
    String egtDesignation();
    void setEgtDesignation(String d);
    /** Freie Schüler-Zeile unter dem Kennzeichen; leer = nicht zeichnen. */
    String egtNote();
    void setEgtNote(String n);
}

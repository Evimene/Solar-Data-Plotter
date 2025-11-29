// SolarDataPoint.java
package com.example.solardataplotter.model;

import javafx.beans.property.*;

public class SolarDataPoint {
    private final StringProperty time;
    private final DoubleProperty solarRadiation;
    private final DoubleProperty vMono;
    private final DoubleProperty vPoly;
    private final DoubleProperty iMono;
    private final DoubleProperty iPoly;
    private final DoubleProperty pMono;
    private final DoubleProperty pPoly;
    private final DoubleProperty effMono;
    private final DoubleProperty effPoly;
    private final DoubleProperty rh;
    private final DoubleProperty panelTempMono;
    private final DoubleProperty panelTempPoly;
    private final DoubleProperty ambientTemp;
    private final DoubleProperty windSpeed;

    public SolarDataPoint() {
        this.time = new SimpleStringProperty("");
        this.solarRadiation = new SimpleDoubleProperty(0.0);
        this.vMono = new SimpleDoubleProperty(0.0);
        this.vPoly = new SimpleDoubleProperty(0.0);
        this.iMono = new SimpleDoubleProperty(0.0);
        this.iPoly = new SimpleDoubleProperty(0.0);
        this.pMono = new SimpleDoubleProperty(0.0);
        this.pPoly = new SimpleDoubleProperty(0.0);
        this.effMono = new SimpleDoubleProperty(0.0);
        this.effPoly = new SimpleDoubleProperty(0.0);
        this.rh = new SimpleDoubleProperty(0.0);
        this.panelTempMono = new SimpleDoubleProperty(0.0);
        this.panelTempPoly = new SimpleDoubleProperty(0.0);
        this.ambientTemp = new SimpleDoubleProperty(0.0);
        this.windSpeed = new SimpleDoubleProperty(0.0);
    }

    // Getters and Setters
    public String getTime() { return time.get(); }
    public void setTime(String time) { this.time.set(time); }
    public StringProperty timeProperty() { return time; }

    public double getSolarRadiation() { return solarRadiation.get(); }
    public void setSolarRadiation(double solarRadiation) { this.solarRadiation.set(solarRadiation); }
    public DoubleProperty solarRadiationProperty() { return solarRadiation; }

    public double getVMono() { return vMono.get(); }
    public void setVMono(double vMono) { this.vMono.set(vMono); }
    public DoubleProperty vMonoProperty() { return vMono; }

    public double getVPoly() { return vPoly.get(); }
    public void setVPoly(double vPoly) { this.vPoly.set(vPoly); }
    public DoubleProperty vPolyProperty() { return vPoly; }

    public double getIMono() { return iMono.get(); }
    public void setIMono(double iMono) { this.iMono.set(iMono); }
    public DoubleProperty iMonoProperty() { return iMono; }

    public double getIPoly() { return iPoly.get(); }
    public void setIPoly(double iPoly) { this.iPoly.set(iPoly); }
    public DoubleProperty iPolyProperty() { return iPoly; }

    public double getPMono() { return pMono.get(); }
    public void setPMono(double pMono) { this.pMono.set(pMono); }
    public DoubleProperty pMonoProperty() { return pMono; }

    public double getPPoly() { return pPoly.get(); }
    public void setPPoly(double pPoly) { this.pPoly.set(pPoly); }
    public DoubleProperty pPolyProperty() { return pPoly; }

    public double getEffMono() { return effMono.get(); }
    public void setEffMono(double effMono) { this.effMono.set(effMono); }
    public DoubleProperty effMonoProperty() { return effMono; }

    public double getEffPoly() { return effPoly.get(); }
    public void setEffPoly(double effPoly) { this.effPoly.set(effPoly); }
    public DoubleProperty effPolyProperty() { return effPoly; }

    public double getRh() { return rh.get(); }
    public void setRh(double rh) { this.rh.set(rh); }
    public DoubleProperty rhProperty() { return rh; }

    public double getPanelTempMono() { return panelTempMono.get(); }
    public void setPanelTempMono(double panelTempMono) { this.panelTempMono.set(panelTempMono); }
    public DoubleProperty panelTempMonoProperty() { return panelTempMono; }

    public double getPanelTempPoly() { return panelTempPoly.get(); }
    public void setPanelTempPoly(double panelTempPoly) { this.panelTempPoly.set(panelTempPoly); }
    public DoubleProperty panelTempPolyProperty() { return panelTempPoly; }

    public double getAmbientTemp() { return ambientTemp.get(); }
    public void setAmbientTemp(double ambientTemp) { this.ambientTemp.set(ambientTemp); }
    public DoubleProperty ambientTempProperty() { return ambientTemp; }

    public double getWindSpeed() { return windSpeed.get(); }
    public void setWindSpeed(double windSpeed) { this.windSpeed.set(windSpeed); }
    public DoubleProperty windSpeedProperty() { return windSpeed; }
}
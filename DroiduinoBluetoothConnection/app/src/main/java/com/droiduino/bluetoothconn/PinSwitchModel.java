package com.droiduino.bluetoothconn;

public class PinSwitchModel {

    private String name;
    private int pinNumber;
    private String message;
    private boolean isChecked = false;
    private boolean isEnabled = true;

    public PinSwitchModel(){}

    public PinSwitchModel(String name, int pinNumber){
        this.name = name;
        this.pinNumber = pinNumber;
    }

    public PinSwitchModel(int pinNumber){
        this.name = "Pin";
        this.pinNumber = pinNumber;
    }

    public String getName(){ return name; }

    public int getPinNumber(){ return pinNumber; }

    public void setMessage(String value){ message = value; };

    public String getMessage(){ return message; };

    public void setIsChecked(boolean value){ isChecked = value; };

    public boolean getIsChecked(){ return isChecked; };

    public void setIsEnabled(boolean value){ isEnabled = value; };

    public boolean getIsEnabled(){ return isEnabled; };
}
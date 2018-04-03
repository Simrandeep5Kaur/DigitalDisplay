package com.kmsg.digitaldisplay.data;

/**
 * Created by ADMIN on 27-Nov-17.
 * STB model
 */

public class STBData {

    private String stbId;
    private String address;
    private String locality;
    private String city;
    private String state;
    private String l1;
    private String l1PhoneNo;

    public STBData(String stbId, String address, String locality, String city, String state, String l1, String l1PhoneNo) {
        this.stbId = stbId;
        this.address = address;
        this.locality = locality;
        this.city = city;
        this.state = state;
        this.l1 = l1;
        this.l1PhoneNo = l1PhoneNo;
    }

    public String getStbId() {
        return stbId;
    }

    public String getAddress() {
        return address;
    }

    public String getLocality() {
        return locality;
    }

    public String getCity() {
        return city;
    }

    public String getState() {
        return state;
    }

    public String getL1() {
        return l1;
    }

    public String getL1PhoneNo() {
        return l1PhoneNo;
    }
}
package com.omni.omnimatics_maps;

/**
 * Created by Kit on 28/10/2017.
 */

public class Model {

    private final String speed, date, lastseen, address, carplate;
    private final boolean engine;
    private final double longitude, latitude;
    private final int id;


    public Model(Boolean eng, String spd, String dt, String lstseen, String add, String cp, double la, double lg, int id) {
        this.engine = eng;
        this.speed = spd;
        this.date = dt;
        this.lastseen = lstseen;
        this.address = add;
        this.carplate = cp;
        this.longitude = lg;
        this.latitude = la;
        this.id = id;
    }

    public Boolean get_eng(){
        return engine;
    }

    public String get_spd(){
        return speed;
    }

    public String get_date(){
        return date;
    }

    public String get_lstseen(){
        return lastseen;
    }

    public String get_address(){
        return address;
    }

    public String get_carplate(){
        return carplate;
    }

    public Double get_Lon(){
        return longitude;
    }

    public Double get_Lat(){
        return latitude;
    }

    public int get_id(){
        return id;
    }

}

package com.omni.omnimatics_maps;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.PeriodicSync;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import okhttp3.FormBody;
import okhttp3.RequestBody;

import static android.text.format.DateUtils.getRelativeTimeSpanString;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {


    private GoogleMap mMap;
    private LinearLayout popupview;
    private boolean clicked = false;
    private static final String TAG_ENG = "engine";
    private static final String TAG_SPD = "speed";
    private static final String TAG_date = "date";
    private static final String TAG_last = "seen";
    private static final String TAG_add = "addr";
    private static final String TAG_plate = "plate";
    private static final String TAG_la = "lati";
    private static final String TAG_lon = "long";
    private static final String TAG_mark = "mark";
    private ArrayList<HashMap<String, String>> tempList;
    Map<Marker, Model> markerMap;
    private TextView lbl_eng, lbl_spd, lbl_date, lbl_lastseen, lbl_address;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        popupview = (LinearLayout)findViewById(R.id.popupview);

        lbl_eng = (TextView)findViewById(R.id.txt_engine_state);
        lbl_spd = (TextView)findViewById(R.id.txt_speed);
        lbl_date = (TextView)findViewById(R.id.txt_date);
        lbl_lastseen = (TextView)findViewById(R.id.txt_last_seen);
        lbl_address = (TextView)findViewById(R.id.txt_address);

        markerMap = new HashMap<>();
        tempList = new ArrayList<>();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        if(isNetworkAvailable.connection(getApplicationContext()))
        {
            new Load_vehicles().execute();

            Intent intent = new Intent(getApplicationContext(), SyncService.class);
            startService(intent);
        }else {
            Toast.makeText(getBaseContext(), getResources().getString(R.string.no_int), Toast.LENGTH_SHORT).show();
        }


    }


    @Override
    public void onMapReady(GoogleMap googleMap) {


        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {

            Marker lastClicked = null;
            int prev_id = 0;
            String prev_carplate = "";

            @Override
            public boolean onMarkerClick(Marker marker) {

                if (lastClicked!=null){
                    lastClicked.setIcon(gen_bmp(true, prev_carplate));
                }

                Model selected_vehicle = markerMap.get(marker);
                marker.setIcon(gen_bmp(false, selected_vehicle.get_carplate()));

                if (selected_vehicle.get_eng()){
                    lbl_eng.setText(getResources().getString(R.string.eng_on));
                    lbl_eng.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorGreen));
                }else {
                    lbl_eng.setText(getResources().getString(R.string.eng_off));
                    lbl_eng.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorRed));
                }

                String lati = String.valueOf(selected_vehicle.get_Lat());
                String longi = String.valueOf(selected_vehicle.get_Lon());

                lbl_spd.setText(selected_vehicle.get_spd());
                lbl_date.setText(selected_vehicle.get_date());
                lbl_lastseen.setText(selected_vehicle.get_lstseen());
                lbl_address.setText("Latitude: " + lati + ", Longitude: " + longi);


                if(clicked ){
                    if(prev_id == selected_vehicle.get_id()){
                        popupview.setVisibility(View.GONE);
                        marker.setIcon(gen_bmp(true, selected_vehicle.get_carplate()));
                        clicked = false;
                    }else {
                        prev_id = selected_vehicle.get_id();
                        prev_carplate = selected_vehicle.get_carplate();
                    }

                }else {
                    prev_id = selected_vehicle.get_id();
                    prev_carplate = selected_vehicle.get_carplate();
                    popupview.setVisibility(View.VISIBLE);
                    clicked = true;
                }

                lastClicked = marker;
                return true;
            }
        });


    }


    public void addMarker(Model Vehicles) {

        if (null != mMap) {
            Marker hotelMarker = mMap.addMarker(new MarkerOptions()
                    .position(new LatLng(Vehicles.get_Lat(), Vehicles.get_Lon()))
                    .title(Vehicles.get_carplate())
                    .icon(gen_bmp(true, Vehicles.get_carplate()))
                    .anchor(0.5f, 0.8f)); //set the anchor position % of the marker image

            markerMap.put(hotelMarker, Vehicles);
        }
    }

    public BitmapDescriptor gen_bmp(boolean grey, String carplate){

        //draw the icon in bitmam
        Bitmap.Config conf = Bitmap.Config.ARGB_8888;
        Bitmap bmp = Bitmap.createBitmap(125, 180, conf);
        Paint color = new Paint();

        if(grey){
            color.setColor(ContextCompat.getColor(getApplicationContext(), R.color.colorGrey));
        }else {
            color.setColor(ContextCompat.getColor(getApplicationContext(), R.color.colorYellow));
        }

        //set text and background color
        Paint color2 = new Paint();
        color2.setTextSize(25);
        color2.setTextAlign(Paint.Align.CENTER);
        color2.setFakeBoldText(true);
        color2.setColor(Color.WHITE);

        //draw the icon, then the background and then lastly text
        Canvas canvas1 = new Canvas(bmp);
        if(grey){
            canvas1.drawBitmap(BitmapFactory.decodeResource(getResources(),
                    R.mipmap.grey_pin1), 0,0, color);
        }else {
            canvas1.drawBitmap(BitmapFactory.decodeResource(getResources(),
                    R.mipmap.yellow_pin), 0,0, color);
        }

        canvas1.drawRect(0,140,125,205,color);
        canvas1.drawText(carplate, 63, 170, color2);

        return BitmapDescriptorFactory.fromBitmap(bmp);
    }

    public class Load_vehicles extends AsyncTask<Void, Void, Boolean> {
        List<String> list = new ArrayList<>();
        Dialog progress;
        String vehicles_data;
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progress = ProgressDialog.show(MapsActivity.this,
                    "Loading", "Please wait...");
        }

        @Override
        protected Boolean doInBackground(Void... arg0) {

            GetJSON client2 = new GetJSON();
            RequestBody formBody = new FormBody.Builder()
                    .build();

            try {

                String url_vehicles = "https://screen-lock-mark-i.firebaseio.com/vehicles.json";
                vehicles_data = client2.getJSONRequest(url_vehicles, formBody);

                list = collect_vehicle(vehicles_data);

            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("OKHTTP LOAD VEHICLE ERROR : " + e);
            } catch (JSONException | ParseException e) {
                e.printStackTrace();
            }

            return null;

        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            progress.dismiss();

            populate_markers();
        }
    }

    public List<String> collect_vehicle(String jsondata) throws JSONException, ParseException {

        List<String> list = new ArrayList<>();

        JSONObject object = new JSONObject(jsondata);
        Iterator<String> iterator = object.keys();

        markerMap.clear();
        tempList.clear();
        //// TODO: 27/10/2017 populating marker to the maps, custom markers
        //// TODO: 28/10/2017 populating all markers to the maps, get last seen time
        //// TODO: 29/10/2017 get last seen time, change marker color when clicked, get real address, check why populated map too near
        //// TODO: 29/10/2017 get last seen time, get real address (http://maps.googleapis.com/maps/api/geocode/json?latlng=3.124123,101.5&sensor=true)
        //// TODO: 29/10/2017 get breadcrumbs trip path, rest polling, temporary use latitude & longitude instead of real address


        while(iterator.hasNext()) {
            String currentKey = iterator.next();
            //System.out.println("DATA patched:" + currentKey);

            list.add(currentKey);

            String json_data = object.getString(currentKey);
            JSONObject object2 = new JSONObject(json_data); //to get the object for each vehicle
            JSONArray coordinates_List = (JSONArray) object2.get("coordinates");
            String car_plate = object2.getString("plate");

            SimpleDateFormat dformat = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
            dformat.setTimeZone(TimeZone.getTimeZone("UTC"));

            SimpleDateFormat dformat2 = new SimpleDateFormat( "MMM' 'd', 'yyyy", Locale.US);
            dformat2.setTimeZone(TimeZone.getTimeZone("UTC"));

            for(int i = 0; i < coordinates_List.length(); i++){

                JSONObject coor_Obj = coordinates_List.getJSONObject(i);
                String engine = coor_Obj.getString("engine");
                String longitude = coor_Obj.getString("longitude");
                String latitude = coor_Obj.getString("latitude");
                String speed = coor_Obj.getString("speed");
                String time = coor_Obj.getString("time");

                Date date = dformat.parse(time);
                String formattedDate = dformat2.format(date);
                //need check time zone
                CharSequence timePassedString = getRelativeTimeSpanString (date.getTime(), System.currentTimeMillis(), DateUtils.SECOND_IN_MILLIS);
                //System.out.println("time now:"+System.currentTimeMillis());

                HashMap<String, String> all_det = new HashMap<>();

                all_det.put(TAG_ENG, engine);
                all_det.put(TAG_plate,car_plate);
                all_det.put(TAG_SPD,speed);
                all_det.put(TAG_add,"");
                all_det.put(TAG_date, formattedDate);
                all_det.put(TAG_last, String.valueOf(timePassedString));
                all_det.put(TAG_la,latitude);
                all_det.put(TAG_lon,longitude);

                String last_pos;
                if(coordinates_List.length()-1 == i){
                    last_pos = "1";
                }else {
                    last_pos = "0";
                }

                all_det.put(TAG_mark,last_pos); // detect which is the last pos for marker placing

                tempList.add(all_det);


            }

        }
        return list;
    }

    public void populate_markers(){

        Double l_latitude = -34.00, l_longitute = 151.00;
        for(int i=0; i<tempList.size(); i++ ){

            String temp_eng = tempList.get(i).get(TAG_ENG);
            String temp_plate = tempList.get(i).get(TAG_plate);
            String temp_date = tempList.get(i).get(TAG_date);
            String temp_last = tempList.get(i).get(TAG_last);
            String temp_speed = tempList.get(i).get(TAG_SPD);
            String temp_address = tempList.get(i).get(TAG_add);
            String last_pos = tempList.get(i).get(TAG_mark);
            Double temp_la = Double.parseDouble(tempList.get(i).get(TAG_la));
            Double temp_long = Double.parseDouble(tempList.get(i).get(TAG_lon));

            Boolean engine;
            engine = temp_eng.equals("1");

            //// TODO: 30/10/2017  need to add function to draw breadcrumbs line here
            
            if(last_pos.equals("1")) {
                Model vehicle_details = new Model(engine, temp_speed, temp_date, temp_last, temp_address, temp_plate, temp_la, temp_long, i);
                addMarker(vehicle_details);
            }

            l_latitude = temp_la;
            l_longitute = temp_long;
        }

        LatLng last_marker = new LatLng(l_latitude, l_longitute);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(last_marker,8f));
    }

    @Override
    public void onResume() {
        super.onResume();

        // Register mMessageReceiver to receive messages.
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter("sync"));
    }

    // handler for received Intents for the "my-event" event
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Extract data included in the Intent
            String message = intent.getStringExtra("sync_now");
            if(message.equals("ok") && isNetworkAvailable.connection(getApplicationContext())){
                
                //// TODO: 30/10/2017 Code below able to refresh data every constant timing passed, but need remove the progress dialog and camera zoom afterwards. 
                //new Load_vehicles().execute();
                

            }
        }
    };

    @Override
    protected void onPause() {
        // Unregister since the activity is not visible
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        super.onPause();
    }


}

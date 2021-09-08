package com.example.searchnearby;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.AsyncQueryHandler;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    Spinner spinner;
    Button btn;
    SupportMapFragment supportMapFragment;
    GoogleMap map;
    FusedLocationProviderClient client;
    double latitude=0,longitude=0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        spinner = findViewById(R.id.spinner);
        btn = findViewById(R.id.button);
        supportMapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

        String[] placeType = {"atm", "bank", "hospital", "movie_theater", "restaurant"};
        String[] placeName = {"ATM", "Bank", "Hospital", "Movie Theater", "Restaurant"};


        spinner.setAdapter(new ArrayAdapter<>(MainActivity.this, R.layout.support_simple_spinner_dropdown_item, placeName));

        client = LocationServices.getFusedLocationProviderClient(this);

        //checking permissions
        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED) {
            getCurrentLocation();
        }
        else{
            ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},1);
        }

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int i=spinner.getSelectedItemPosition();


                String url = "https://maps.googleapis.com/maps/api/..." + "?location=" + latitude + "," + latitude + "&radius=5000" + "&type=" + placeType[i] + "&sensor=true" + "&key=" + getResources().getString(R.string.google_map_key);
                new PlaceTask().execute(url);
            }
        });

    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        Task<Location> task = client.getLastLocation();
        task.addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if(location!=null){
        latitude=location.getLatitude();
        longitude=location.getLongitude();

        //syncing to current location
                    supportMapFragment.getMapAsync(new OnMapReadyCallback() {
                        @Override
                        public void onMapReady(GoogleMap googleMap) {
                            map=googleMap;

                            //zooming to current location

                            map.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude,longitude),10));
                        }
                    });
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
        if(permissions.length>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED){
            getCurrentLocation();
        }
        }
    }

    private class PlaceTask extends AsyncTask<String,Integer,String> {


        @Override
        protected String doInBackground(String... strings) {
            try {
                String data=downloadUrl(strings[0]);
                return data;
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {

            //execute parser task
            new ParserTask().execute();
        }
    }

    private String downloadUrl(String string) throws IOException {
        URL url=new URL(string);
        HttpURLConnection connection= (HttpURLConnection) url.openConnection();
        connection.connect();
        //connection is made
        InputStream stream=connection.getInputStream();
        BufferedReader reader=new BufferedReader(new InputStreamReader(stream));

        StringBuilder builder=new StringBuilder();

        String line="";
        while((line=reader.readLine())!=null){
            builder.append(line);
        }

        String data=builder.toString();
        reader.close();
        return data;
    }


    private class ParserTask extends AsyncTask<String,Integer, List<HashMap<String,String>>> {
        @Override
        protected List<HashMap<String, String>> doInBackground(String... strings) {
            JsonParser jsonParser=new JsonParser();
            List<HashMap<String,String>> list=new ArrayList<>();
            JSONObject object;
            try {
            object=new JSONObject(strings[0]);
            //parse json
                list=jsonParser.parseResult(object);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return list;
        }

        @Override
        protected void onPostExecute(List<HashMap<String, String>> maps) {

            map.clear();

            for(int i=0;i<maps.size();i++){
                HashMap<String,String> hmap=maps.get(i);
                double lat= Double.parseDouble(hmap.get("lat"));
                double lng= Double.parseDouble(hmap.get("lng"));
                String name=hmap.get("name");
                LatLng latLng=new LatLng(lat,lng);
                // mark the location

                MarkerOptions options=new MarkerOptions();
                options.position(latLng);
                options.title(name);

                map.addMarker(options);
            }

        }
    }
}
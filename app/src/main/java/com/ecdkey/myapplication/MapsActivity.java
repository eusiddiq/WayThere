package com.ecdkey.myapplication;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.akexorcist.googledirection.DirectionCallback;
import com.akexorcist.googledirection.GoogleDirection;
import com.akexorcist.googledirection.constant.Unit;
import com.akexorcist.googledirection.model.Direction;
import com.akexorcist.googledirection.model.Info;
import com.akexorcist.googledirection.model.Leg;
import com.akexorcist.googledirection.model.Route;
import com.akexorcist.googledirection.util.DirectionConverter;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.kwabenaberko.openweathermaplib.Units;
import com.kwabenaberko.openweathermaplib.implementation.OpenWeatherMapHelper;
import com.kwabenaberko.openweathermaplib.models.currentweather.CurrentWeather;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.io.IOException;
import java.util.Map;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.OnConnectionFailedListener {

    private static final LatLngBounds LAT_LNG_BOUNDS = new LatLngBounds(new LatLng(-40, -168), new LatLng(71, 136));

    private GoogleMap mMap;

    private Map<Integer, Polyline> allPolylines = new LinkedHashMap<>();
    private Map<Integer, Route> allRoutes = new LinkedHashMap<>();
    private Map<Integer, ArrayList<Marker>> markers = new LinkedHashMap<>();
    private Map<Integer, ArrayList<String>> weatherDesc = new LinkedHashMap<>();
    private Map<Integer, ArrayList<Double>> weatherTemp = new LinkedHashMap<>();
    private Map<Integer, ArrayList<String>> weatherCity = new LinkedHashMap<>();
    private Map<Integer, ArrayList<String>> weatherIcon = new LinkedHashMap<>();

    private double startLat;
    private double startLon;
    private double destLat;
    private double destLon;
    private LatLng startingPoint;
    private LatLng endingPoint;

    private Button viewList;

    private AutoCompleteTextView origin;
    private AutoCompleteTextView destination;

    private ImageView findRoute;

    private PlaceAutocompleteAdapter placeAutocompleteAdapter;
    private GoogleApiClient mGoogleApiClient;

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);

        }

        viewList =  findViewById(R.id.viewList);
        viewList.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(!weatherIcon.isEmpty())
                {
                    Bundle bundle = new Bundle();
                    bundle.putSerializable("weatherDesc", (Serializable) weatherDesc);
                    bundle.putSerializable("weatherTemp", (Serializable) weatherTemp);
                    bundle.putSerializable("weatherIcon", (Serializable) weatherIcon);
                    bundle.putSerializable("weatherCity", (Serializable) weatherCity);
                    Intent activityChangeIntent = new Intent(MapsActivity.this, ListViewActivity.class);
                    activityChangeIntent.putExtras(bundle);

                    MapsActivity.this.startActivity(activityChangeIntent);
                }
            }
        });

        origin = findViewById(R.id.origin);
        destination = findViewById(R.id.destination);
        findRoute = findViewById(R.id.find);

        mGoogleApiClient = new GoogleApiClient
                .Builder(this)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .enableAutoManage(this, this)
                .build();

        placeAutocompleteAdapter = new PlaceAutocompleteAdapter(this, mGoogleApiClient, LAT_LNG_BOUNDS, null);

        origin.setAdapter(placeAutocompleteAdapter);
        destination.setAdapter(placeAutocompleteAdapter);

        origin.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                InputMethodManager in = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                in.hideSoftInputFromWindow(view.getApplicationWindowToken(), 0);
            }
        });

        destination.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                InputMethodManager in = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                in.hideSoftInputFromWindow(view.getApplicationWindowToken(), 0);
            }
        });

        findRoute.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                geoLocate();
                drawRoute();
            }
        });
    }

    private void geoLocate() {
        String start = origin.getText().toString();
        String end = destination.getText().toString();

        if (start.isEmpty()) {
            Toast.makeText(this, "Please enter a starting address!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (end.isEmpty()) {
            Toast.makeText(this, "Please enter a destination address!", Toast.LENGTH_SHORT).show();
            return;
        } else {
            try {
                startLat = geoLocateLat(start);
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                startLon = geoLocateLon(start);
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                destLat = geoLocateLat(end);
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                destLon = geoLocateLon(end);
            } catch (IOException e) {
                e.printStackTrace();
            }
            startingPoint = new LatLng(startLat, startLon);
            endingPoint = new LatLng(destLat, destLon);
        }

    }

    public double geoLocateLat(String loc) throws IOException {
        Geocoder gc = new Geocoder(this);
        List<Address> list = gc.getFromLocationName(loc, 1);
        double val1 = list.get(0).getLatitude();

        return val1;
    }

    public double geoLocateLon(String loc) throws IOException {
        Geocoder gc = new Geocoder(this);
        List<Address> list = gc.getFromLocationName(loc, 1);
        double val1 = list.get(0).getLongitude();

        return val1;
    }

    public void getWeather(double lat, double lon, final int i){
        final LatLng location = new LatLng(lat, lon);
        OpenWeatherMapHelper helper = new OpenWeatherMapHelper();
        helper.setApiKey("b84c7d5441b754b375a42b4a9568957e");
        helper.setUnits(Units.IMPERIAL);
        helper.getCurrentWeatherByGeoCoordinates(lat, lon, new OpenWeatherMapHelper.CurrentWeatherCallback() {
            @Override
            public void onSuccess(CurrentWeather currentWeather) {
                String icon = currentWeather.getWeatherArray().get(0).getIcon();
                weatherDesc.get(i).add(currentWeather.getWeatherArray().get(0).getDescription());
                weatherCity.get(i).add(currentWeather.getName());
                weatherIcon.get(i).add(icon);
                weatherTemp.get(i).add(currentWeather.getMain().getTemp());
                if(icon.equals("01d") || icon.equals("01n"))
                {
                    Marker m = mMap.addMarker(new MarkerOptions()
                            .title(currentWeather.getName() + ", " + currentWeather.getSys().getCountry() + ": " + currentWeather.getWeatherArray().get(0).getDescription() + ", " + currentWeather.getMain().getTemp() + "°F")
                            .position(location)
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.clear_sky))
                            .visible(false)
                    );
                    markers.get(i).add(m);
                }
                else if(icon.equals("02d") || icon.equals("02n"))
                {
                    Marker m = mMap.addMarker(new MarkerOptions()
                            .title(currentWeather.getName() + ", " + currentWeather.getSys().getCountry() + ": " + currentWeather.getWeatherArray().get(0).getDescription() + ", " + currentWeather.getMain().getTemp() + "°F")
                            .position(location)
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.few_clouds))
                            .visible(false)
                    );
                    markers.get(i).add(m);
                }
                else if(icon.equals("03d") || icon.equals("03n"))
                {
                    Marker m = mMap.addMarker(new MarkerOptions()
                            .title(currentWeather.getName() + ", " + currentWeather.getSys().getCountry() + ": " + currentWeather.getWeatherArray().get(0).getDescription() + ", " + currentWeather.getMain().getTemp() + "°F")
                            .position(location)
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.scattered_clouds))
                            .visible(false)
                    );
                    markers.get(i).add(m);
                }
                else if(icon.equals("04d") || icon.equals("04n"))
                {
                    Marker m = mMap.addMarker(new MarkerOptions()
                            .title(currentWeather.getName() + ", " + currentWeather.getSys().getCountry() + ": " + currentWeather.getWeatherArray().get(0).getDescription() + ", " + currentWeather.getMain().getTemp() + "°F")
                            .position(location)
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.broken_clouds))
                            .visible(false)
                    );
                    markers.get(i).add(m);
                }
                else if(icon.equals("09d") || icon.equals("09n"))
                {
                    Marker m = mMap.addMarker(new MarkerOptions()
                            .title(currentWeather.getName() + ", " + currentWeather.getSys().getCountry() + ": " + currentWeather.getWeatherArray().get(0).getDescription() + ", " + currentWeather.getMain().getTemp() + "°F")
                            .position(location)
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.shower_rain))
                            .visible(false)
                    );
                    markers.get(i).add(m);
                }
                else if(icon.equals("10d") || icon.equals("10n"))
                {
                    Marker m = mMap.addMarker(new MarkerOptions()
                            .title(currentWeather.getName() + ", " + currentWeather.getSys().getCountry() + ": " + currentWeather.getWeatherArray().get(0).getDescription() + ", " + currentWeather.getMain().getTemp() + "°F")
                            .position(location)
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.shower_rain))
                            .visible(false)
                    );
                    markers.get(i).add(m);
                }
                else if(icon.equals("11d") || icon.equals("11n"))
                {
                    Marker m = mMap.addMarker(new MarkerOptions()
                            .title(currentWeather.getName() + ", " + currentWeather.getSys().getCountry() + ": " + currentWeather.getWeatherArray().get(0).getDescription() + ", " + currentWeather.getMain().getTemp() + "°F")
                            .position(location)
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.thunderstorm))
                            .visible(false)
                    );
                    markers.get(i).add(m);
                }
                else if(icon.equals("13d") || icon.equals("13n"))
                {
                    Marker m = mMap.addMarker(new MarkerOptions()
                            .title(currentWeather.getName() + ", " + currentWeather.getSys().getCountry() + ": " + currentWeather.getWeatherArray().get(0).getDescription() + ", " + currentWeather.getMain().getTemp() + "°F")
                            .position(location)
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.snow))
                            .visible(false)
                    );
                    markers.get(i).add(m);
                }
                else if(icon.equals("50d") || icon.equals("50n"))
                {
                    Marker m = mMap.addMarker(new MarkerOptions()
                            .title(currentWeather.getName() + ", " + currentWeather.getSys().getCountry() + ": " + currentWeather.getWeatherArray().get(0).getDescription() + ", " + currentWeather.getMain().getTemp() + "°F")
                            .position(location)
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.windy))
                            .visible(false)
                    );
                    markers.get(i).add(m);
                }
                /*Log.v(TAG,
                        "Coordinates: " + currentWeather.getCoord().getLat() + ", "+currentWeather.getCoord().getLat() +"\n"
                                +"Max Temperature: " + currentWeather.getMain().getTempMax()+"\n"
                                +"Wind Speed: " + currentWeather.getWind().getSpeed() + "\n"
                );*/
            }

            @Override
            public void onFailure(Throwable throwable) {
            }
        });
    }

    public void drawRoute() {
        mMap.clear();
        allRoutes.clear();
        allPolylines.clear();
        markers.clear();
        weatherDesc.clear();
        weatherCity.clear();
        weatherIcon.clear();
        weatherTemp.clear();

        String serverKey = "AIzaSyDgotRGFD9CcKFqV9s1R1AgiKIUMqO8R3I";
        GoogleDirection.withServerKey(serverKey)
                .from(startingPoint)
                .to(endingPoint)
                .alternativeRoute(true)
                .unit(Unit.IMPERIAL)
                .execute(new DirectionCallback() {
                    @Override
                    public void onDirectionSuccess(Direction direction, String rawBody) {
                        if (direction.isOK()) {
                            int routes = direction.getRouteList().size();
                            for(int i = 0; i < routes; i++)
                            {
                                markers.put(i, new ArrayList<Marker>());
                                weatherDesc.put(i, new ArrayList<String>());
                                weatherCity.put(i, new ArrayList<String>());
                                weatherIcon.put(i, new ArrayList<String>());
                                weatherTemp.put(i, new ArrayList<Double>());

                                Route route = direction.getRouteList().get(i);
                                Leg leg = route.getLegList().get(0);
                                Info distanceInfo = leg.getDistance();
                                Info durationInfo = leg.getDuration();
                                ArrayList<LatLng> directionPositionList = leg.getDirectionPoint();

                                for(int z = 500; z < directionPositionList.size(); z += 500)
                                {
                                    if(!(z >= directionPositionList.size() - 550)) {
                                        getWeather(directionPositionList.get(z).latitude, directionPositionList.get(z).longitude, i);
                                    }
                                }
                                final PolylineOptions polylineOptions = DirectionConverter.createPolyline(getApplicationContext(), directionPositionList, 5, Color.RED);
                                polylineOptions.clickable(true);
                                mMap.addMarker(new MarkerOptions()
                                        .title(origin.getText().toString())
                                        .position(startingPoint)
                                );
                                mMap.addMarker(new MarkerOptions()
                                        .title(destination.getText().toString() + ", Duration: " + durationInfo.getText())
                                        .position(endingPoint)
                                );
                                Polyline p = mMap.addPolyline(polylineOptions);
                                allPolylines.put(i, p);
                                allRoutes.put(i, route);
                            }
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(startingPoint, 7));
                        } else {
                            // Do something
                        }

                    }

                    @Override
                    public void onDirectionFailure(Throwable t) {
                        // Do something here
                    }
                });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        mMap = googleMap;
        try{
            boolean success = googleMap.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(
                            this, R.raw.mapstyle));

                    if(!success){

                    }
        }catch(Resources.NotFoundException e){

        }

        mMap.getUiSettings().setZoomControlsEnabled(true);

        if ((ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
            mMap.setMyLocationEnabled(true);
        }
        mMap.setOnPolylineClickListener(new GoogleMap.OnPolylineClickListener() {
            @Override
            public void onPolylineClick(Polyline polyline) {
                for(int i = 0; i < markers.size(); i++)
                {
                    for(int z = 0; z < markers.get(i).size(); z++)
                    {
                        markers.get(i).get(z).setVisible(false);
                    }
                    allPolylines.get(i).setColor(Color.RED);
                    allPolylines.get(i).setZIndex(0);
                }
                for(int i = 0; i < allPolylines.size(); i++)
                {
                    if(polyline.getPoints().equals(allPolylines.get(i).getPoints()))
                    {
                        for(int z = 0; z < markers.get(i).size(); z++)
                        {
                            markers.get(i).get(z).setVisible(true);
                        }
                    }
                }
                polyline.setColor(Color.GREEN);
                polyline.setZIndex(1);
            }
        });

    }
}

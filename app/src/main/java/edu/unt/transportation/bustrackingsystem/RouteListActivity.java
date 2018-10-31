package edu.unt.transportation.bustrackingsystem;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.ahmadrosid.lib.drawroutemap.DrawMarker;
import com.ahmadrosid.lib.drawroutemap.DrawRouteMaps;
import com.akexorcist.googledirection.DirectionCallback;
import com.akexorcist.googledirection.GoogleDirection;
import com.akexorcist.googledirection.constant.AvoidType;
import com.akexorcist.googledirection.model.Direction;
import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.Query;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.maps.android.PolyUtil;

import org.w3c.dom.Document;

import java.lang.annotation.Documented;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.unt.transportation.bustrackingsystem.firebase.BusStopListener;
import edu.unt.transportation.bustrackingsystem.firebase.BusStopReceiver;
import edu.unt.transportation.bustrackingsystem.firebase.VehicleChangeListener;
import edu.unt.transportation.bustrackingsystem.firebase.VehicleMapChangeReceiver;
import edu.unt.transportation.bustrackingsystem.model.BusRoute;
import edu.unt.transportation.bustrackingsystem.model.BusStop;
import edu.unt.transportation.bustrackingsystem.model.StopSchedule;
import edu.unt.transportation.bustrackingsystem.model.Vehicle;
import edu.unt.transportation.bustrackingsystem.network.ApiServices;
import edu.unt.transportation.bustrackingsystem.network.InitLibrary;
import edu.unt.transportation.bustrackingsystem.response.Distance;
import edu.unt.transportation.bustrackingsystem.response.Duration;
import edu.unt.transportation.bustrackingsystem.response.LegsItem;
import edu.unt.transportation.bustrackingsystem.response.ResponseRoute;
import edu.unt.transportation.bustrackingsystem.util.GeneralUtil;
import edu.unt.transportation.bustrackingsystem.util.PermissionUtils;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static edu.unt.transportation.bustrackingsystem.R.drawable.bus;
import static edu.unt.transportation.bustrackingsystem.util.GeneralUtil.getDayStringForToday;

/**
 * Created by Gil Wasserman on 10/15/2016.
 * This is the main RouteList Activity.  The view will show the user a list of all Bus Routes
 * currently available
 * and allow them to select a route to view it on TrackerMapActivity.
 *
 * This is the main launcher class for the app, so it will also contain a Driver Sign-In option
 * item to allow
 * navigating to the Driver Sign-In screen where the driver will be assigned a bus and route
 */

public class RouteListActivity extends AppCompatActivity implements OnMapReadyCallback,
        ActivityCompat.OnRequestPermissionsResultCallback, VehicleChangeListener, BusStopListener
{
    //constant for root node used in this activity
    private static final String ROOT_ROUTE = "stops";
    private static final String FIREBASE_URL = "https://bus-tracking-da9f3.firebaseio.com/";
    private ListView routeList; //ListView showing list of routes in UI
    private RouteAdapter routeAdapter;  //Adapter of routes to be linked with routeList
    private Map<String, BusRoute> busRouteMap; //Map of RouteIDs to BusRoute objects
    private BusStop selectedRoute; //The bus route selected by the user
    private DatabaseReference mDatabase;  //reference to the root node of the Firebase database
    private ArrayAdapter<String> schdulestopadapter;
    private List<String> scheduledStopsList = new ArrayList<String>();
    private CheckBox mStopCheckBox;
    private CheckBox mScheduleCheckBox;
    private static final String TAG = RouteListActivity.class.getName();
    private Map<String, List<String>> scheduleListMap;
    private List<String> scheduleList;
    private ArrayAdapter scheduleListAdapter;
    private LinearLayout scheduleListLayout;

    private Spinner spinner1;

    private Spinner spinner2;

    private GoogleMap mMap;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private List<Vehicle> vehicleList = new ArrayList<>();
    private Map<String, Marker> markerMap;
    private List<BusStop> busStopList = new ArrayList<>();
    private List<StopSchedule> stopSchedules = new ArrayList<>();
    private BusStopReceiver busStopReceiver;
    private List<Marker> mBusStopMarkerList = new ArrayList<Marker>();
    private List<Marker> makerdirec = new ArrayList<Marker>();
    private List<Polyline> mBusStopPolyList = new ArrayList<Polyline>();
    private VehicleMapChangeReceiver vehicleMapChangeReceiver;
    private List<String> lokasilist = new ArrayList<String>();
    private List<String> tujuanlist = new ArrayList<String>();
    private ArrayAdapter<String> lokasiAdapter = null;
    private ArrayAdapter<String> tujuanAdapter = null;
    private Marker marker;
    private Map<String, BusStop> lokasi = null;
    private Map<String, String> routed = new HashMap<>();
    private Firebase mFirebase = null;
    private String lokasiID = null;
    private String tekan;
    private Button find;

    private TextView tv1;
    private TextView tv2;

    private String API_KEY = "AIzaSyBe9P-itehQgH5BG7ox5vizpv5E1iGLMhg";
    private LatLng awal = new LatLng(-8.594616,116.145411);
    private LatLng tujuan = new LatLng(-8.5869073,116.0921869);

    private ArrayList<LatLng> ab = new ArrayList<>();
    private LatLng a = null;
    private LatLng b = null;
    // Firebase database

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.maps_fix);
        setTitle(R.string.title_route_list);
        // Set up toolbar to be used in activity
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        //Sets the context of the Firebase database to the current activity
        Firebase.setAndroidContext(this);
        busRouteMap = new HashMap<>();
        scheduleListLayout = (LinearLayout) findViewById(R.id.scheduleListLayout1);
        //Initialize mDatabase value and add activity as ChildEvent Listener
        mDatabase = FirebaseDatabase.getInstance().getReference();
        scheduleList = new ArrayList<>();
        scheduleListMap = new HashMap<>();
        markerMap = new HashMap<>();
        spinner1 =   (Spinner) findViewById(R.id.spinner_fix1);
        spinner2 =   (Spinner) findViewById(R.id.spinner_fix2);
        tv1 = (TextView) findViewById(R.id.waktu);
        tv2 = (TextView) findViewById(R.id.jarak);
        mStopCheckBox = (CheckBox)findViewById(R.id.stopCheckbox1);
        mScheduleCheckBox = (CheckBox)findViewById(R.id.scheduleCheckbox1);
        scheduleListAdapter = new ArrayAdapter<>(this,
                R.layout.schedule_listview, scheduleList);
        final ListView listView = (ListView) findViewById(R.id.scheduleList1);
        listView.setAdapter(scheduleListAdapter);
        scheduleListLayout = (LinearLayout) findViewById(R.id.scheduleListLayout1);
        final Spinner spinner = (Spinner) findViewById(R.id.busStopSpinner1);
        // Create an ArrayAdapter using the string array and a default spinner layout
        schdulestopadapter = new ArrayAdapter<>(this, R.layout.spinner_item, scheduledStopsList);
        // Specify the layout to use when the list of choices appears
        schdulestopadapter.setDropDownViewResource(android.R.layout
                .simple_spinner_dropdown_item);
        // Apply the scheduleListAdapter to the spinner

        spinner.setAdapter(schdulestopadapter);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
            {

                tekan = (String) parent.getItemAtPosition(position);
                Log.e(TAG,"pesannya tekan"+tekan);
                List<String> scheduleStringList = scheduleListMap.get(tekan);
                scheduleList.clear();
                if (scheduleStringList != null)
                {
                    scheduleListAdapter.clear();
                    scheduleListAdapter.addAll(scheduleStringList);
                }
                else
                {
                    Log.e(TAG, "while taking the spinner found that scheduleStringList is null");
                }
                scheduleListAdapter.notifyDataSetChanged();
//                navigateToMap();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent)
            {
                Log.w(TAG, "Spinner: Nothing is selected");
            }
        });
        lokasiAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item, lokasilist);
        tujuanAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item, tujuanlist);
        lokasi = new HashMap<String,BusStop>();
        Firebase.setAndroidContext(this);
        mFirebase = new Firebase(FIREBASE_URL);
        mFirebase.child("/stops/").addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(com.firebase.client.DataSnapshot dataSnapshot, String s) {
                BusStop lokal = dataSnapshot.getValue(BusStop.class);
                lokasi.put(dataSnapshot.getKey(), lokal);
                LatLng as = new LatLng(lokal.getLongitude(),lokal.getLatitude());
                ab.add(as);
                lokasiAdapter.add(lokal.getStopName());
                lokasiAdapter.notifyDataSetChanged();
                busStopList.add(lokal);
//                schdulestopadapter.add(lokal.getStopName());
            }

            @Override
            public void onChildChanged(com.firebase.client.DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(com.firebase.client.DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(com.firebase.client.DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {

            }
        });

        addItemsSpinner1();
        addItemsSpinner2();
//        routeID = getSelectedRoute().getStopID();
        //Initialize routeList component with routeAdapter and set current class listeners
        SupportMapFragment mapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.trackerMap1);
        mapFragment.getMapAsync(this);

        markerMap = new HashMap<>();

        DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();
        busStopReceiver = new BusStopReceiver();
        /**
         * Set the routeID, whose vehicleMap we want to listen to and register for the listener
         */
        vehicleMapChangeReceiver = new VehicleMapChangeReceiver();

        find = (Button) findViewById(R.id.find);
        find.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(a!=null&&b!=null){
                    mMap.clear();
                    actionRoute();
                }
            }
        });

        Log.d(TAG,"lotab"+a+" "+b);
    }

    private void addItemsSpinner1(){
        spinner1.setVisibility(View.VISIBLE);
        spinner1.setPrompt("Pilih lokasi");
        lokasiAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner1.setAdapter(lokasiAdapter);
        spinner1.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                lokasiID = adapterView.getItemAtPosition(i).toString();
                a = ab.get(i);
                Log.d(TAG,"lotab"+a);

            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }

    private void addItemsSpinner2(){
        spinner2.setVisibility(View.VISIBLE);
        spinner2.setPrompt("Pilih tujuan");
        tujuanAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner2.setAdapter(lokasiAdapter);
        spinner2.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                b = ab.get(i);
                Log.d(TAG,"lotab"+b);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }

    @Override
    public void onStart(){
        super.onStart();
    }

    private void enableMyLocation()
    {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission
                .ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED)
        {
            // Permission to access the location is missing.
            PermissionUtils.requestPermission(this, LOCATION_PERMISSION_REQUEST_CODE,
                    android.Manifest.permission.ACCESS_FINE_LOCATION, true);
        }
        else if (mMap != null)
        {
            // Access to the location has been granted to the app.
            mMap.setMyLocationEnabled(true);
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.action_signIn:
                startActivity(new Intent(this, SignInActivity.class));
                return true;

            case R.id.action_aboutUs:
                startActivity(new Intent(this, About_Us.class));
                return true;

            case R.id.action_likeUs:
                GeneralUtil.OpenFacebookPage(this);
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }

    //getter and setter method for selectedRoute

    //Create an intent and start the TrackerMapActivity
    //This should pass the currently selected route ID to the TrackerMapActivity

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        enableMyLocation();
        LatLngBounds bounds = new LatLngBounds.Builder().include(awal).include(tujuan).build();
        Point size = new Point();
        getWindowManager().getDefaultDisplay().getSize(size);
        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds,size.x,250,30));

    }

    @Override
    public void onVehicleChanged(Vehicle vehicle) {
        LatLng vehiclePosition = new LatLng(vehicle.getLatitude(), vehicle
                .getLongitude());
        if (vehicleList.contains(vehicle)) {
            // Replace the old vehicle object with the new one
            vehicleList.set(vehicleList.indexOf(vehicle), vehicle);
            /**
             * If the vehicle is already present on the UI and in the vehicleList,
             * most probably the position is updated, so we update the marker position
             */
            markerMap.get(vehicle.getVehicleID()).setPosition(vehiclePosition);
        } else {
            /**
             * Perform following operations if new vehicle is added on the route
             */
            vehicleList.add(vehicle);
            Marker newVehicleMarker = mMap.addMarker(new MarkerOptions()
                    .icon(BitmapDescriptorFactory.fromResource(bus))
                    .title(vehicle.getVehicleID())
                    .position(vehiclePosition)
                    .flat(true));
            markerMap.put(vehicle.getVehicleID(), newVehicleMarker);
        }
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }

    public void onScheduleToggled(View view)
    {
        if (mScheduleCheckBox.isChecked())
        {
//            loadSchedule();

            tampildata();
            scheduleListLayout.setVisibility(View.VISIBLE);
        }
        else
            scheduleListLayout.setVisibility(View.GONE);
    }
    /**
     * This method is invoked when the user clicks on show stops checkbox on the user interface
     *
     * @param view View of the checkbox which is being toggled
     */
    public void onStopToggled(View view)
    {

        if (mStopCheckBox.isChecked())
        {
            for (BusStop busStop : busStopList)
            {
                LatLng busStopLocation = new LatLng(busStop.getLongitude(),busStop.getLatitude());
                mBusStopMarkerList.add(mMap.addMarker(new MarkerOptions()
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory
                                .HUE_AZURE))
                        .position(busStopLocation)
                        .title(busStop.getStopName())
                        .flat(true)));

            }
        }
        else
        {
            for (Marker marker : mBusStopMarkerList)
            {
                marker.remove();
            }
        }

    }


    private void tampildata(){
        scheduledStopsList.clear();
        for (BusStop busStop : busStopList)
        {
            Map<String, List<StopSchedule>> scheduleMap = busStop.getRouteSchedule();
            Log.d(TAG,"pesannya takan"+tekan);
            if (scheduleMap != null)
            {
                scheduledStopsList.add(busStop.getStopName());
                schdulestopadapter.notifyDataSetChanged();
                for(StopSchedule stopSchedule: stopSchedules) {
                    List<String> listn = stopSchedule.getTimingsList();
                    Log.e(TAG, "pesan error" + listn.toString());
                }
                List<StopSchedule> stopScheduleList = scheduleMap.get(tekan);
                try{
                    if (stopScheduleList != null)
                    {
                        for (StopSchedule stopSchedule : stopScheduleList)
                        {
                            Log.d(TAG,"schedule"+stopSchedule.getTimingsList().toString());
                            scheduleListAdapter.addAll(stopSchedule.getTimingsList());
                            if(stopSchedule.getDayOfWeek().equalsIgnoreCase(getDayStringForToday()))
                                scheduleListMap.put(busStop.getStopName(), stopSchedule.getTimingsList());
                        }
                    }
                    else
                        Log.e(TAG, "stopScheduleList is null");
                }catch (Exception e){
                    Log.e(TAG,e.getMessage());
                }

            }
        }

        scheduleListAdapter.notifyDataSetChanged();
    }

    public List<BusStop> getBusStopList()
    {
        return busStopList;
    }

    /**
     * Get the list of vehicles available on the currently selected route
     *
     * @return List of Vehicles
     */
    public List<Vehicle> getVehicleList()
    {
        return vehicleList;
    }

    public List<Marker> getmBusStopMarkerList() {
        return mBusStopMarkerList;
    }

    public Map<String, Marker> getMarkerMap() {
        return markerMap;
    }

    public List<String> getScheduledStopsList() {
        return scheduledStopsList;
    }

    public Map<String, List<String>> getScheduleListMap() {
        return scheduleListMap;
    }

    public String getTekan(){
        return tekan;
    }

    public void setTekan(String tekan){
        this.tekan = tekan;
    }

    @Override
    public void onBusStopAdded(BusStop busStop) {
        if (!busStopList.contains(busStop))
            busStopList.add(busStop);
    }

    private void actionRoute(){
        String awalx = a.latitude+","+a.longitude;
        String tujuanx = b.latitude+","+b.longitude;
        ApiServices api = InitLibrary.getInstance();

        Call<ResponseRoute> routeCall = api.request_route(awalx,tujuanx,API_KEY);
        routeCall.enqueue(new Callback<ResponseRoute>() {
            @Override
            public void onResponse(Call<ResponseRoute> call, Response<ResponseRoute> response) {
                if(response.isSuccess()){
                    try {
                        ResponseRoute data = response.body();

                        LegsItem datalegs = data.getRoutes().get(0).getLegs().get(0);

                        String polyline = data.getRoutes().get(0).getOverviewPolyline().getPoints();

                        List<LatLng> decode= PolyUtil.decode(polyline);

                        mMap.addPolyline(new PolylineOptions().addAll(decode).width(5).color(Color.RED)).setGeodesic(true);

                        Distance distance = datalegs.getDistance();
                        Duration duration = datalegs.getDuration();

                        tv1.setText("jarak "+distance.getText()+" ("+distance.getValue()+"m)");
                        tv2.setText("waktu "+duration.getText()+" ("+duration.getValue()+"s)");

                        BusStop busStop = new BusStop();
                        makerdirec.add(mMap.addMarker(new MarkerOptions()
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory
                                        .HUE_AZURE))
                                .position(a)
                                .title(busStop.getStopName())
                                .flat(true)));
                        makerdirec.add(mMap.addMarker(new MarkerOptions()
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory
                                        .HUE_AZURE))
                                .position(b)
                                .title(busStop.getStopName())
                                .flat(true)));

                    }catch (Exception e){
                        Log.e(TAG,"tracking"+e.getMessage());
                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseRoute> call, Throwable t) {

            }
        });
    }

}

package com.wikijourney.wikijourney.views;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.acg.lib.ACGResourceAccessException;
import com.acg.lib.impl.UpdateLocationACG;
import com.acg.lib.model.Location;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.MySSLSocketFactory;
import com.wikijourney.wikijourney.GlobalState;
import com.wikijourney.wikijourney.R;
import com.wikijourney.wikijourney.functions.CustomInfoWindow;
import com.wikijourney.wikijourney.functions.Map;
import com.wikijourney.wikijourney.functions.POI;
import com.wikijourney.wikijourney.functions.UI;
import cz.msebera.android.httpclient.Header;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.api.IMapController;
import org.osmdroid.bonuspack.overlays.Marker;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Overlay;
import sparta.checkers.quals.Sink;
import sparta.checkers.quals.Source;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static sparta.checkers.quals.FlowPermissionString.DISPLAY;
import static sparta.checkers.quals.FlowPermissionString.INTERNET;
import static sparta.checkers.quals.FlowPermissionString.WRITE_LOGS;

public class MapFragment extends Fragment {

    private GlobalState gs;

    // Variables for API
    private @Sink("INTERNET(api.wikijourney.eu)") String language = "en";
    private @Sink("INTERNET(api.wikijourney.eu)") double paramRange;
    private @Sink("INTERNET(api.wikijourney.eu)") int paramMaxPoi;
    private @Sink("INTERNET(api.wikijourney.eu)") String paramPlace;
    private @Sink({}) int paramMethod; //Could be around or place, depends on which button was clicked.

    //Now the variables we are going to use for the rest of the program.
    private @Sink(DISPLAY) MapView map;
    private @Source("ACG(location)") GeoPoint userLocation;
    private boolean isUserLocatedOnce = false;
    private @Sink(DISPLAY) Marker userLocationMarker;

    private Snackbar locatingSnackbar;
    private Snackbar downloadSnackbar;

    private @Sink({}) boolean isVisibleToUser = false;
    private @Source({}) UpdateLocationACG updateLocationACG;


    public MapFragment() {
        // Required empty public constructor
    }

    public void setUpdateLocationACG(@Source({}) UpdateLocationACG updateLocationACG) {
        this.updateLocationACG = updateLocationACG;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        gs = (GlobalState) getActivity().getApplication();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_map, container, false);

        // These lines initialize the map settings
        map = (MapView) view.findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setBuiltInZoomControls(true);
        map.setMultiTouchControls(true);
        map.setTilesScaledToDpi(true);
        IMapController mapController = map.getController();
        mapController.setZoom(16);

        userLocationMarker = new Marker(map);

        Locale defaultLocale = (/*@Sink("INTERNET(api.wikijourney.eu)")*/ Locale) Locale.getDefault();
        language = defaultLocale.getLanguage();

        // We get the Bundle values
        Bundle args = getArguments();

        try {
            paramMaxPoi = args.getInt(HomeFragment.EXTRA_OPTIONS[0]);
        } catch (Exception e) {
            paramMaxPoi = getResources().getInteger(R.integer.default_maxPOI);
        }
        try {
            paramRange = args.getDouble(HomeFragment.EXTRA_OPTIONS[1]);
        } catch (Exception e) {
            paramRange = getResources().getInteger(R.integer.default_range);
        }
        try {
            paramPlace = args.getString(HomeFragment.EXTRA_OPTIONS[2]);
        } catch (Exception e) {
            paramPlace = "null"; // Place value
        }
        try {
            paramMethod = args.getInt(HomeFragment.EXTRA_OPTIONS[3]);
        } catch (Exception e) {
            paramMethod = HomeFragment.METHOD_AROUND;
        }

        if (paramMethod == HomeFragment.METHOD_AROUND) {
            locateUser();

        } else if(paramMethod == HomeFragment.METHOD_PLACE) {
            drawMap(paramPlace);
        }

        setUserVisibleHint(true);

        // Inflate ACG
        FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
        transaction.add(R.id.update_location_acg_fragment_id, updateLocationACG).commit();

        return view;
    }

    private void locateUser() {
        // Display a Snackbar while the phone locates the user, so he doesn't think the app crashed
        if (getActivity().findViewById(R.id.fragment_container) != null) {
            locatingSnackbar = Snackbar.make(getActivity().findViewById(R.id.fragment_container), R.string.snackbar_locating, Snackbar.LENGTH_INDEFINITE);
            locatingSnackbar.show();
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        // Stop the Geolocation if the user leaves the MapFragment early
        updateLocationACG.onDetach();
        if (locatingSnackbar != null) {
            locatingSnackbar.dismiss();
        }
        if (downloadSnackbar != null) {
            downloadSnackbar.dismiss();
        }
        setUserVisibleHint(false);
    }

    private void drawCurrentLocation(double latitude, double longitude) {

        IMapController mapController = map.getController();

        // This starts the map at the desired point
        userLocation = new GeoPoint(latitude, longitude);
        if (!isUserLocatedOnce) {
            mapController.setCenter(userLocation);
        }

        // Now we add a marker using osmBonusPack

        userLocationMarker.setPosition(userLocation);
        userLocationMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        @Sink(DISPLAY) List</*@Sink(DISPLAY)*/ Overlay> overlays = (/*@Sink(DISPLAY)*/ List</*@Sink(DISPLAY)*/ Overlay>) map.getOverlays();
        overlays.add(userLocationMarker);

        // And we have to use this to refresh the map
        // map.invalidate();

        // We can change some properties of the marker (don't forget to refresh the map !!)
        userLocationMarker.setInfoWindow(new CustomInfoWindow(map));
        Drawable icon = null;
        if (getActivity() != null) {
            icon = ContextCompat.getDrawable(getActivity(), R.drawable.ic_place);
        }
        try {
            userLocationMarker.setIcon(icon);
            userLocationMarker.setTitle(getString(R.string.you_are_here));
            map.invalidate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void drawMap() {
        // We get the POI around the user with WikiJourney API
        String url;
        url = gs.API_URL + "long=" + userLocation.getLongitude() + "&lat=" + userLocation.getLatitude()
                + "&maxPOI=" + paramMaxPoi + "&range=" + paramRange + "&lg=" + language;

        // Check if the Internet is up
        final ConnectivityManager connMgr = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        // These are needed for the download library and the methods called later
        final Context context = this.getActivity();
        final MapFragment mapFragment = this;

        if (networkInfo != null && networkInfo.isConnected()) {
            // Show a Snackbar while we wait for WikiJourney server, so the user doesn't think the app crashed
            if (getActivity().findViewById(R.id.fragment_container) != null) {
                downloadSnackbar = Snackbar.make(getActivity().findViewById(R.id.fragment_container), R.string.snackbar_downloading, Snackbar.LENGTH_INDEFINITE);
                downloadSnackbar.show();
            }
            new DownloadWjApi(url, HomeFragment.METHOD_AROUND, context, mapFragment).invoke(false);

        } else {
            UI.openPopUp(mapFragment.getActivity(), getResources().getString(R.string.error_activate_internet_title), getResources().getString(R.string.error_activate_internet));
        }
    }
    private void drawMap(String paramPlace) {
        // We get the POI around the user with WikiJourney API
        String url;
        String encodedPlace = "";
        try { // https://stackoverflow.com/a/10786112/3641865
            encodedPlace = (/*@Sink("INTERNET(api.wikijourney.eu)")*/ String) URLEncoder.encode(paramPlace, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        url = gs.API_URL + "place=" + encodedPlace + "&maxPOI=" + paramMaxPoi + "&range="
                + paramRange + "&lg=" + language;

        // Check if the Internet is up
        final ConnectivityManager connMgr = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        // These are needed for the download library and the methods called later
        final Context context = this.getActivity();
        final MapFragment mapFragment = this;

        if (networkInfo != null && networkInfo.isConnected()) {
            // Show a Snackbar while we wait for WikiJourney server, so the user doesn't think the app crashed
            if (getActivity().findViewById(R.id.fragment_container) != null) {
                downloadSnackbar = Snackbar.make(getActivity().findViewById(R.id.fragment_container), R.string.snackbar_downloading, Snackbar.LENGTH_INDEFINITE);
                downloadSnackbar.show();
            }
            new DownloadWjApi(url, HomeFragment.METHOD_PLACE, context, mapFragment).invoke(false);

        } else {
            UI.openPopUp(mapFragment.getActivity(), getResources().getString(R.string.error_activate_internet_title), getResources().getString(R.string.error_activate_internet));
        }
    }

    private class DownloadWjApi {
        private @Sink({}) final Context context;
        private @Sink({}) final MapFragment mapFragment;
        private @Sink("INTERNET(api.wikijourney.eu)") String url;
        private @Sink({}) final int paramMethod;

        public DownloadWjApi(@Sink("INTERNET(api.wikijourney.eu)") String url, int paramMethod, @Sink({}) Context context, @Sink({}) MapFragment mapFragment) {
            this.url = url;
            this.context = context;
            this.mapFragment = mapFragment;
            this.paramMethod = paramMethod;
        }

        public void invoke(boolean useSelfSignedSSL) {
            // Download from the WJ API
            AsyncHttpClient client = new AsyncHttpClient();
            if (useSelfSignedSSL) {
                try { // We add the certificate chain, because the intermediate cert issued by Let's Encrypt isn't in default KeyStore
                    KeyStore trustStore = MySSLSocketFactory.getKeystoreOfCA(getResources().openRawResource(R.raw.fullchain));
                    MySSLSocketFactory sf = new MySSLSocketFactory(trustStore);
                    client.setSSLSocketFactory(sf);
                }
                catch (Exception e) {
                    // Empty catch, what should we put here?
                }
            }
            client.setTimeout(30_000); // Set timeout to 30s, the server may be slow...
            client.get(context, url, new JsonHttpResponseHandler() {
                @Override
                public void onSuccess(@Source(INTERNET) int statusCode, @Source(INTERNET) Header /*@Source(INTERNET)*/ [] headers, @Source(INTERNET) JSONObject response) {
                    if (downloadSnackbar != null) {
                        downloadSnackbar.dismiss();
                    }
                    @Sink(DISPLAY) ArrayList</*@Sink(DISPLAY)*/ POI> poiArrayList;
                    boolean errorOccurred = true;
                    String errorMessage = null;
                    boolean isPoiAround = false;
                    // We check if the server answered correctly and if there is any POI around
                    try {
                        errorOccurred = response.getJSONObject("err_check").getBoolean("value");
                        if (errorOccurred) {
                            errorMessage = response.getJSONObject("err_check").getString("err_msg");
                        } else {
                            errorOccurred = false;
                        }

                        if (response.getJSONObject("poi").getInt("nb_poi") == 0) {
                            isPoiAround = false;
                        } else {
                            isPoiAround = true;
                        }
                    } catch (JSONException e) {
                        errorOccurred = true;
                        e.printStackTrace();
                    }
                    if (errorOccurred) {
                        UI.openPopUp(mapFragment.getActivity(), getResources().getString(R.string.error_download_api_response_title), errorMessage);
                    } else if (!isPoiAround) {
                        UI.openPopUp(mapFragment.getActivity(), getResources().getString(R.string.error_no_poi_around_title),
                                getResources().getString(R.string.error_no_poi_around));
                    } else {
                        if (paramMethod == HomeFragment.METHOD_PLACE) {
                            JSONObject placeLocationJson = null;
                            double placeLat = 0;
                            double placeLong = 0;
                            try {
                                placeLocationJson = response.getJSONObject("user_location");
                                placeLat = placeLocationJson.getDouble("latitude");
                                placeLong = placeLocationJson.getDouble("longitude");
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            drawCurrentLocation(placeLat, placeLong);
                        }

                        poiArrayList = (/*@Sink(DISPLAY)*/ ArrayList</*@Sink(DISPLAY)*/ POI>) POI.parseApiJson(response, paramMethod, context);
                        if (poiArrayList != null && poiArrayList.size() != 0) {
                            Map.drawPOI(mapFragment, poiArrayList);
                        } else {
                            UI.openPopUp(context, getResources().getString(R.string.error_no_poi_around_title), getResources().getString(R.string.error_no_poi_around));
                        }
                    }
                }

                @Override
                public void onProgress(@Sink(WRITE_LOGS) long bytesWritten, @Sink(WRITE_LOGS) long totalSize) {
                    Log.d("progress", "Downloading " + bytesWritten + " of " + totalSize);
                }

                @Override
                public void onFailure(@Source(INTERNET) int statusCode, @Source(INTERNET) Header /*@Source(INTERNET)*/ [] headers, Throwable throwable, @Source(INTERNET) JSONObject errorResponse) {
                    try {
                        Log.e("Error", errorResponse.toString());
                    } catch (Exception e) {
                        Log.e("Error", "Error while downloading the API response");
                    }
                    finally {
                        UI.openPopUp(mapFragment.getActivity(), getResources().getString(R.string.error_download_api_response_title), getResources().getString(R.string.error_download_api_response));
                        if (downloadSnackbar != null) {
                            downloadSnackbar.dismiss();
                        }
                    }
                }

                @Override
                public void onRetry(@Sink(WRITE_LOGS) int retryNo) {
                    Log.e("Error", "Retrying for the " + retryNo + " time");
                    super.onRetry(retryNo);
                }
            });
        }
    }

    public void drawCurrentLocation() {
        if (isVisibleToUser) {
            // Once located, download the info from the API and display the map
            if (locatingSnackbar != null) {
                locatingSnackbar.dismiss();
            }

            try {
                Location location = updateLocationACG.getResource();

                drawCurrentLocation(location.getLatitude(), location.getLongitude());
                if (!isUserLocatedOnce) {
                    isUserLocatedOnce = true;
                    drawMap();
                }
            } catch (ACGResourceAccessException e) {
                // ignore, since the original app doesn't care
            }
        }
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        this.isVisibleToUser = isVisibleToUser;
        this.isUserLocatedOnce = false;
    }
}
package com.wikijourney.wikijourney;

import android.app.Application;
import com.wikijourney.wikijourney.functions.POI;
import sparta.checkers.quals.PolySinkR;
import sparta.checkers.quals.PolySourceR;
import sparta.checkers.quals.Sink;

import java.util.ArrayList;

/**
 * This class should hold all variables useful for the whole app.
 * For now, it contains the list of POIs.
 * http://stackoverflow.com/questions/708012/how-to-declare-global-variables-in-android?answertab=votes#tab-top
 * Created by Thomas on 16/10/15.
 */
public class GlobalState extends Application {
    public static final @Sink("INTERNET(api.wikijourney.eu)") String API_URL = "http://api.wikijourney.eu/?";

    private @PolySourceR @PolySinkR ArrayList</*@PolySourceR @PolySinkR*/ POI> poiList;

    @Override
    public void onCreate() {
        super.onCreate();
        // Initialize the singletons so their instances
        // are bound to the application process.
        poiList = new ArrayList<>();
    }

    public @PolySourceR @PolySinkR ArrayList</*@PolySourceR @PolySinkR*/ POI> getPoiList() {
        return poiList;
    }

    public void setPoiList(@PolySourceR @PolySinkR ArrayList<POI> poiList) {
        this.poiList = poiList;
    }
}
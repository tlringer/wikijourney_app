package com.wikijourney.wikijourney;

import com.acg.lib.listeners.ResourceAvailabilityListener;
import com.wikijourney.wikijourney.views.MapFragment;
import sparta.checkers.quals.Source;

/**
 * Listener for the ACG
 */
public class LocationACGListener implements ResourceAvailabilityListener {

    private @Source({}) MapFragment mapFragment;

    public void setMapFragment(@Source({}) MapFragment mapFragment) {
        this.mapFragment = mapFragment;
    }

    @Override
    public void onResourceUnavailable() {
        // Do nothing, like the original app
    }

    @Override
    public void onResourceReady() {
        mapFragment.drawCurrentLocation();
    }
}

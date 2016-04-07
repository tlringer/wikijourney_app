package com.wikijourney.wikijourney;

import com.acg.lib.listeners.ResourceAvailabilityListener;
import com.wikijourney.wikijourney.views.MapFragment;

/**
 * Listener for the ACG
 */
public class LocationACGListener implements ResourceAvailabilityListener {

    private MapFragment mapFragment;

    public void setMapFragment(MapFragment mapFragment) {
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

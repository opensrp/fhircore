package org.smartregister.fhircore.geowidget.shadows;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.AttributeSet;

import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMapOptions;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadows.ShadowViewGroup;

@Implements(MapView.class)
public class ShadowMapView extends ShadowViewGroup {

    @Implementation
    public void __constructor__(@NonNull Context context, @Nullable AttributeSet attrs) {
        // Do nothing
    }

    @Implementation
    public void onCreate(@Nullable Bundle savedInstanceState) {
        //Do nothing
    }

    @Implementation
    public void getMapAsync(final @NonNull OnMapReadyCallback callback) {
        //Do nothing
    }

    @Implementation
    public void onStart() {
        //Do nothing
    }

    @Implementation
    protected void initialize(@NonNull final Context context, @NonNull final MapboxMapOptions options) {
        // Do nothing
    }

}

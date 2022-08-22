package org.smartregister.fhircore.geowidget.shadows;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadows.ShadowViewGroup;

import io.ona.kujaku.helpers.MapboxLocationComponentWrapper;
import io.ona.kujaku.layers.KujakuLayer;
import io.ona.kujaku.views.KujakuMapView;

/**
 * This shadow is used to test some methods that can be invoked directly and do not need any of the ui
 * or other code to be called
 *
 * Created by Ephraim Kigamba - ekigamba@ona.io on 14/03/2019
 */
@Implements(KujakuMapView.class)
public class ShadowKujakuMapView extends ShadowMapView {

    @Implementation
    public void __constructor__(@NonNull Context context, @Nullable AttributeSet attrs) {
        // Do nothing
    }

    @Implementation
    public void __constructor__(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        // Do nothing
    }


    @Implementation
    public MapboxLocationComponentWrapper getMapboxLocationComponentWrapper() {
        return new MapboxLocationComponentWrapper();
    }

    @Implementation
    public void showCurrentLocationBtn(boolean isVisible) {
        //Do nothing
    }

    @Implementation
    public void onDestroy() {
        //Do nothing
    }

    @Implementation
    public void onResume() {
        //Do nothing
    }

    @Implementation
    public void enableAddPoint(boolean canAddPoint) {
        //Do nothing
    }

    @Implementation
    public void addLayer(@NonNull KujakuLayer kujakuLayer) {
        //Do nothing
    }

}

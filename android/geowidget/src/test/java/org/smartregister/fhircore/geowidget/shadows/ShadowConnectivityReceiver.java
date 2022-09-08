package org.smartregister.fhircore.geowidget.shadows;

import android.content.Context;

import com.mapbox.mapboxsdk.net.ConnectivityReceiver;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 28/12/2017.
 */

@Implements(ConnectivityReceiver.class)
public class ShadowConnectivityReceiver {

    @Implementation
    public static synchronized ConnectivityReceiver instance(Context context) {
        return null;
    }
}

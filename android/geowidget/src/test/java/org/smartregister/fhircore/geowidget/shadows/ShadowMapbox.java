package org.smartregister.fhircore.geowidget.shadows;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.net.ConnectivityReceiver;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 28/12/2017.
 */

@Implements(Mapbox.class)
public class ShadowMapbox {

    private static String lastMethodCall = null;
    private static List<Object> lastMethodParams = null;

    @Implementation
    public static synchronized Mapbox getInstance(@NonNull Context context, @Nullable String accessToken) {

        if ("YES".equals(lastMethodCall)) {
            lastMethodCall = "getInstance";
            lastMethodParams = new ArrayList<>();
            lastMethodParams.add(context);
            lastMethodParams.add(accessToken);
        }

        return null;
    }

    public static void allowMethodRecording() {
        lastMethodCall = "YES";
    }

    /**
     * Once this method is called, it resets
     * @return
     */
    public static Map<String, List<Object>> getLastMethodCall() {
        String methodCall = lastMethodCall;
        List<Object> params = lastMethodParams;

        lastMethodCall = null;
        lastMethodParams = null;

        HashMap<String, List<Object>> map = new HashMap<>();
        map.put(methodCall, params);

        return map;
    }
}

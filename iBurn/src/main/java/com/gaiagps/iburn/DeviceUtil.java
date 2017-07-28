package com.gaiagps.iburn;

import android.os.Build;
import android.support.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Convenience class for inspecting Device properties that might affect application logic
 * Created by dbro on 9/30/15.
 */
public class DeviceUtil {

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({
            S4,
            S5,
            S6,
            S7
    })
    public @interface Device {}

    public static final String S4 = "SAMSUNG GT-I9500";
    public static final String S5 = "SAMSUNG SM-G900";
    public static final String S6 = "SAMSUNG SM-G92";
    public static final String S7 = "SAMSUNG SM-G93";

    public static boolean isDevice(@Device String deviceType) {
        // We use "startsWith" because Galaxy models differ per carrier
        // e.g: Verizon S6 might carry model "SM-G920V"
        return getDeviceName().startsWith(deviceType);
    }

    /**
     * @return a consistent uppercase String containing the device manufacturer and model,
     * separated by a single space.
     * e.g: "SAMSUNG SM-G920A"
     */
    public static String getDeviceName() {
        String manufacturer = Build.MANUFACTURER.toUpperCase();
        String model = Build.MODEL.toUpperCase();

        if (model.startsWith(manufacturer + "-")) {
            // e.g: manufacturer: "SAMSUNG", model: "SAMSUNG-SM-G920A" -> "SAMSUNG SM-G920A"
            return manufacturer + " " + model.replace(manufacturer + "-", "");
        } else if (model.startsWith(manufacturer)) {
            // e.g: manufacturer: "SAMSUNG", model: "SAMSUNG SM-G920A" -> "SAMSUNG SM-G920A"
            return model;
        } else {
            // e.g: manufacturer: "SAMSUNG", model: "SM-G920A" -> "SAMSUNG SM-G920A"
            return manufacturer + " " + model;
        }
    }
}
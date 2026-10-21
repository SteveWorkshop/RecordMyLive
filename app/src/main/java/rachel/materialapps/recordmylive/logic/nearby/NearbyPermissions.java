package rachel.materialapps.recordmylive.logic.nearby;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.content.ContextCompat;
import java.util.Arrays;

public final class NearbyPermissions {
    private NearbyPermissions() {}
    public static String[] required() {
        if (Build.VERSION.SDK_INT >= 33) return new String[] {
                Manifest.permission.BLUETOOTH_ADVERTISE, Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.NEARBY_WIFI_DEVICES };
        return new String[] { Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION };
    }
    public static boolean granted(Context context) {
        for (String permission : required()) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED)
                return false;
        }
        return true;
    }
    public static boolean notificationGranted(Context context) {
        return Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context,
                Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
    }
    public static String[] forStart() {
        String[] nearby = required();
        if (Build.VERSION.SDK_INT < 33) return nearby;
        String[] requested = Arrays.copyOf(nearby, nearby.length + 1);
        requested[nearby.length] = Manifest.permission.POST_NOTIFICATIONS;
        return requested;
    }
}

package rachel.materialapps.recordmylive;

import android.app.Application;
import android.app.Activity;
import android.os.Bundle;
import android.content.Context;

import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.color.DynamicColors;
import com.google.android.material.color.DynamicColorsOptions;

import rachel.materialapps.recordmylive.ui.ThemeSettings;
import rachel.materialapps.recordmylive.util.GlobalExceptionHandler;

public class BaseApplication  extends Application {
    private static volatile Context context;

    public static synchronized Context getApplication()
    {
        return context;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        context=getApplicationContext();
        AppCompatDelegate.setDefaultNightMode(ThemeSettings.getNightMode(this));
        DynamicColors.applyToActivitiesIfAvailable(this, new DynamicColorsOptions.Builder()
                .setPrecondition((activity, theme) -> ThemeSettings.getColorIndex(activity) == 0)
                .build());
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override public void onActivityPreCreated(Activity activity, Bundle state) {
                ThemeSettings.applyColorOverlay(activity);
            }
            @Override public void onActivityCreated(Activity activity, Bundle state) { }
            @Override public void onActivityStarted(Activity activity) { }
            @Override public void onActivityResumed(Activity activity) { }
            @Override public void onActivityPaused(Activity activity) { }
            @Override public void onActivityStopped(Activity activity) { }
            @Override public void onActivitySaveInstanceState(Activity activity, Bundle state) { }
            @Override public void onActivityDestroyed(Activity activity) { }
        });

        Thread.setDefaultUncaughtExceptionHandler(
                new GlobalExceptionHandler(this));
    }
}

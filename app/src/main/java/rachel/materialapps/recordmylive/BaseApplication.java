package rachel.materialapps.recordmylive;

import android.app.Application;
import android.content.Context;

import com.google.android.material.color.DynamicColors;

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
        DynamicColors.applyToActivitiesIfAvailable(this);

        Thread.setDefaultUncaughtExceptionHandler(
                new GlobalExceptionHandler(this));
    }
}

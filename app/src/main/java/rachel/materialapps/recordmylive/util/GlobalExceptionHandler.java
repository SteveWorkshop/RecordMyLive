package rachel.materialapps.recordmylive.util;

import android.content.Context;
import android.content.Intent;
import android.os.Process;

import androidx.annotation.NonNull;

import rachel.materialapps.recordmylive.ui.CrashActivity;

public class GlobalExceptionHandler implements Thread.UncaughtExceptionHandler{

    private static final long EXIT_DELAY_MS = 500L;

    private final Context context;

    private final Thread.UncaughtExceptionHandler defaultHandler;

    public GlobalExceptionHandler(Context context) {
        this.context = context;
        this.defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
    }
    @Override
    public void uncaughtException(@NonNull Thread t, @NonNull Throwable e) {
        try {

            // 生成 Crash 信息
            CrashReport report = DeviceInfoUtil.fromThrowable(context, t, e);
            // 保存日志
            CrashStorage.save(context, report);

            // 启动 CrashActivity
            launchCrashActivity();

            // 给 Activity 一点启动时间
            try {
                Thread.sleep(EXIT_DELAY_MS);
            } catch (InterruptedException ignored) {
            }

        } catch (Throwable ignored) {
            defaultHandler.uncaughtException(t, e);
        } finally {
            Process.killProcess(Process.myPid());
            System.exit(10);

        }


    }

    private void launchCrashActivity() {

        Intent intent = new Intent(context, CrashActivity.class);

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        context.startActivity(intent);
    }
}

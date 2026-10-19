package rachel.materialapps.recordmylive.ui;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.EditText;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import rachel.materialapps.recordmylive.R;
import rachel.materialapps.recordmylive.util.CrashStorage;

public class CrashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_crash);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        String report = CrashStorage.readText(this);
        TextView deviceInfo = findViewById(R.id.tvDevice);
        EditText crashLog = findViewById(R.id.etCrash);
        deviceInfo.setText(report.isEmpty() ? "未找到崩溃报告" : report);
        crashLog.setText(report);
    }
}

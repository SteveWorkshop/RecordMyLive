package rachel.materialapps.recordmylive.ui;

import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import rachel.materialapps.recordmylive.R;
import rachel.materialapps.recordmylive.util.CrashStorage;

public class CrashActivity extends AppCompatActivity {
    private EditText crashLog;
    private final ActivityResultLauncher<String> saveReport = registerForActivityResult(
            new ActivityResultContracts.CreateDocument("text/plain"), uri -> {
                if (uri == null) return;
                try (OutputStream stream = getContentResolver().openOutputStream(uri)) {
                    if (stream == null) throw new IOException("No output stream");
                    stream.write(getReport().getBytes(StandardCharsets.UTF_8));
                    Toast.makeText(this, R.string.crash_saved, Toast.LENGTH_SHORT).show();
                } catch (IOException | SecurityException e) {
                    Toast.makeText(this, R.string.crash_save_failed, Toast.LENGTH_SHORT).show();
                }
            });

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
        crashLog = findViewById(R.id.etCrash);
        deviceInfo.setText(report.isEmpty() ? "未找到崩溃报告" : report);
        crashLog.setText(report);

        Button copy = findViewById(R.id.btnCopy);
        Button save = findViewById(R.id.btnSave);
        Button mail = findViewById(R.id.btnMail);
        boolean hasReport = !report.isEmpty();
        copy.setEnabled(hasReport);
        save.setEnabled(hasReport);
        mail.setEnabled(hasReport);
        copy.setOnClickListener(v -> {
            ClipboardManager clipboard = getSystemService(ClipboardManager.class);
            clipboard.setPrimaryClip(ClipData.newPlainText(getString(R.string.crash_report), getReport()));
            Toast.makeText(this, R.string.crash_copied, Toast.LENGTH_SHORT).show();
        });
        save.setOnClickListener(v -> saveReport.launch("crash-report.txt"));
        mail.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_SENDTO,
                    Uri.fromParts("mailto", getString(R.string.crash_email_address), null));
            intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.crash_email_subject));
            intent.putExtra(Intent.EXTRA_TEXT, getReport());
            try {
                startActivity(intent);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(this, R.string.crash_no_email_app, Toast.LENGTH_SHORT).show();
            }
        });
        findViewById(R.id.btnExit).setOnClickListener(v -> finishAffinity());
    }

    private String getReport() {
        return crashLog.getText().toString();
    }
}

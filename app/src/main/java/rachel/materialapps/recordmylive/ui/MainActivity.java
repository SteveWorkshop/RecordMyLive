package rachel.materialapps.recordmylive.ui;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.content.Intent;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import lombok.Getter;
import lombok.Setter;
import rachel.materialapps.recordmylive.R;
import rachel.materialapps.recordmylive.databinding.ActivityMainBinding;
import rachel.materialapps.recordmylive.logic.nearby.NearbyDiscoveryService;
import rachel.materialapps.recordmylive.ui.pages.home.AddConcertFragment;

public class MainActivity extends AppCompatActivity {

    @Getter
    @Setter
    private NavHostFragment navHostFragment;

    @Getter
    @Setter
    private NavController navController;

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        //setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.my_nav_host_fragment);
        navController = navHostFragment.getNavController();
        binding.navigationRail.setOnItemSelectedListener(item->{
            int id = item.getItemId();
            if(id==R.id.home_page){
                navController.navigate(R.id.homeFragment);
            }
            else if(id==R.id.timeline_page){
                navController.navigate(R.id.timeLineFragment);
            }
            else if(id==R.id.nearby_page){
                navController.navigate(R.id.nearByFragment);
            }
            else if(id==R.id.statistic_page){
                navController.navigate(R.id.statisticsFragment);
            }
            else if(id==R.id.my_page){
                navController.navigate(R.id.myFragment);
            }
            return true;
        });
        openNearbyFromNotification(getIntent());
        // Navigation restores its own back stack after a configuration change.
        if (savedInstanceState == null) openSharedConcert(getIntent());
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        openNearbyFromNotification(intent);
        openSharedConcert(intent);
    }

    private void openSharedConcert(Intent intent) {
        if (intent == null || !Intent.ACTION_SEND.equals(intent.getAction())
                || !"text/plain".equalsIgnoreCase(intent.getType())) return;
        CharSequence sharedText = intent.getCharSequenceExtra(Intent.EXTRA_TEXT);
        if (sharedText == null || sharedText.toString().trim().isEmpty()) return;
        Bundle args = new Bundle();
        args.putString(AddConcertFragment.ARG_SHARED_CONCERT_TEXT, sharedText.toString());
        navController.navigate(R.id.addConcertFragment, args);
        // The Fragment arguments now own the share; do not replay it on activity recreation.
        intent.removeExtra(Intent.EXTRA_TEXT);
    }

    private void openNearbyFromNotification(Intent intent) {
        if (intent != null && intent.getBooleanExtra(NearbyDiscoveryService.EXTRA_OPEN_NEARBY, false)) {
            intent.removeExtra(NearbyDiscoveryService.EXTRA_OPEN_NEARBY);
            binding.navigationRail.setSelectedItemId(R.id.nearby_page);
        }
    }
}

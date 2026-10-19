package rachel.materialapps.recordmylive.ui;

import android.annotation.SuppressLint;
import android.os.Bundle;

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
            else if(id==R.id.statistic_page){
                navController.navigate(R.id.statisticsFragment);
            }
            else if(id==R.id.my_page){
                navController.navigate(R.id.myFragment);
            }
            return true;
        });
    }
}
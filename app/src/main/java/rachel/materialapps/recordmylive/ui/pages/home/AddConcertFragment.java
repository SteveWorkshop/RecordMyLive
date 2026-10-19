package rachel.materialapps.recordmylive.ui.pages.home;

import androidx.lifecycle.ViewModelProvider;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.timepicker.MaterialTimePicker;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import lombok.Getter;
import lombok.Setter;
import rachel.materialapps.recordmylive.R;
import rachel.materialapps.recordmylive.databinding.FragmentAddConcertBinding;
import rachel.materialapps.recordmylive.logic.constants.AppConstants;
import rachel.materialapps.recordmylive.logic.constants.ConcertStatus;
import rachel.materialapps.recordmylive.logic.constants.ConcertType;
import rachel.materialapps.recordmylive.logic.constants.PurchaseChannel;
import rachel.materialapps.recordmylive.logic.entity.ConcertEntity;
import rachel.materialapps.recordmylive.logic.entity.SeatInfo;

public class AddConcertFragment extends Fragment {

    @Getter
    @Setter
    private NavHostFragment navHostFragment;

    @Getter
    @Setter
    private NavController navController;


    private FragmentAddConcertBinding binding;

    private AddConcertViewModel mViewModel;


    private LocalDate selectedDate;

    private LocalTime selectedTime;

    private ConcertType selectedConcertType;

    public static AddConcertFragment newInstance() {
        return new AddConcertFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding=FragmentAddConcertBinding.inflate(inflater,container,false);

        //binding.toolbar.inflateMenu(R.menu.menu_save);

        binding.layoutBasicInfo.edtDate.setOnClickListener(v->{
            showDatePicker();
        });

        binding.layoutBasicInfo.edtTime.setOnClickListener(v -> {

            showTimePicker();

        });

        ConcertType[] values = ConcertType.values();

        String[] names = new String[values.length];

        for (int i = 0; i < values.length; i++) {
            names[i] = values[i].getDisplayName();
        }

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(
                        requireContext(),
                        android.R.layout.simple_list_item_1,
                        names
                );

        binding.layoutBasicInfo.actType.setAdapter(adapter);

        binding.layoutBasicInfo.actType.setOnItemClickListener(
                (parent, view, position, id) ->
                        selectedConcertType = values[position]
        );

        View root=binding.getRoot();

        return root;//inflater.inflate(R.layout.fragment_add_concert, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initToolbar();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = new ViewModelProvider(this).get(AddConcertViewModel.class);
        // TODO: Use the ViewModel
        navHostFragment = (NavHostFragment) getActivity().getSupportFragmentManager().findFragmentById(R.id.my_nav_host_fragment);
        navController = navHostFragment.getNavController();

        binding.toolbar.setNavigationOnClickListener(v->{
            navController.navigateUp();
        });
    }

    private void showDatePicker(){
        MaterialDatePicker<Long> picker =
                MaterialDatePicker.Builder.datePicker()
                        .setTitleText("选择演出日期")
                        .build();

        picker.addOnPositiveButtonClickListener(selection -> {

            selectedDate = Instant.ofEpochMilli(selection)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();

            binding.layoutBasicInfo.edtDate.setText(
                    selectedDate.format(DateTimeFormatter.ISO_DATE)
            );

        });

        picker.show(getParentFragmentManager(), "datePicker");
    }

    private void showTimePicker(){
        MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setHour(19)
                .setMinute(30)
                .build();

        picker.addOnPositiveButtonClickListener(v -> {

            selectedTime = LocalTime.of(
                    picker.getHour(),
                    picker.getMinute()
            );

            binding.layoutBasicInfo.edtTime.setText(
                    selectedTime.toString()
            );

        });

        picker.show(getParentFragmentManager(), "timePicker");
    }


    private boolean validateInput() {

        boolean valid = true;

        if (binding.layoutBasicInfo.edtName.getText().toString().trim().isEmpty()) {

            binding.layoutBasicInfo.layoutName.setError("请输入演出名称");

            valid = false;

        } else {

            binding.layoutBasicInfo.layoutName.setError(null);

        }

        if (selectedDate == null) {

            binding.layoutBasicInfo.layoutDate.setError("请选择日期");

            valid = false;

        } else {

            binding.layoutBasicInfo.layoutDate.setError(null);

        }

        if (binding.layoutLocation.edtCity.getText().toString().trim().isEmpty()) {

            binding.layoutLocation.layoutCity.setError("请输入城市");

            valid = false;

        } else {

            binding.layoutLocation.layoutCity.setError(null);

        }

        return valid;
    }

    private void initToolbar() {

        binding.toolbar.setNavigationOnClickListener(v ->
                NavHostFragment.findNavController(this).navigateUp()
        );

        binding.toolbar.setOnMenuItemClickListener(item -> {

            if (item.getItemId() == R.id.action_save) {
                if (!mViewModel.canAddConcert(selectedDate)) {

                    Snackbar.make(binding.getRoot(), "当天最多只能记录"+ AppConstants.MAX_CONCERT_PER_DAY +"场演出", Snackbar.LENGTH_LONG).show();
                    return false;
                }


                saveConcert();
                Toast.makeText(getContext(), "保存成功！", Toast.LENGTH_SHORT).show();
                return true;
            }

            return false;
        });
    }

    private ConcertEntity buildConcert(){
        ConcertEntity concert = ConcertEntity.createEmpty();
        concert.setName(binding.layoutBasicInfo.edtName.getText().toString().trim());
        concert.setType(selectedConcertType);
        concert.setDate(selectedDate);
        concert.setTime(selectedTime);
        concert.setCity(binding.layoutLocation.edtCity.getText().toString().trim());
        concert.setVenue(binding.layoutLocation.edtVenue.getText().toString().trim());

        // 第一阶段默认值
        concert.setArtists(new ArrayList<>());
        concert.setPurchaseChannel(PurchaseChannel.OTHER);
        concert.setStatus(ConcertStatus.PLANNED);
        concert.setPrice(0);
        concert.setFavorite(false);
        concert.setTicketCollected(false);
        concert.setSeat(new SeatInfo());
        concert.setNote(null);
        return concert;
    }

    private void saveConcert(){
        if(validateInput()){
            ConcertEntity concert = buildConcert();
            mViewModel.saveConcert(concert);
            NavHostFragment.findNavController(this).navigateUp();
        }

    }
}
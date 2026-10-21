package rachel.materialapps.recordmylive.ui.pages.timeline;

import androidx.lifecycle.ViewModelProvider;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.Collections;
import java.util.List;

import rachel.materialapps.recordmylive.R;
import rachel.materialapps.recordmylive.databinding.FragmentTimeLineBinding;
import rachel.materialapps.recordmylive.logic.entity.ConcertEntity;
import rachel.materialapps.recordmylive.ui.components.ConcertDeleteDialog;

public class TimeLineFragment extends Fragment {

    private FragmentTimeLineBinding binding;

    private TimeLineViewModel mViewModel;

    private TimeLineAdapter adapter;
    private AlertDialog deleteDialog;

    public static TimeLineFragment newInstance() {
        return new TimeLineFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentTimeLineBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewModel = new ViewModelProvider(this).get(TimeLineViewModel.class);
        initRecyclerView();
        mViewModel.getTimelineItems().observe(getViewLifecycleOwner(), this::renderTimeline);
    }

    private void initRecyclerView() {
        adapter = new TimeLineAdapter(this::openDetail, this::confirmDelete);
        binding.rvTimeline.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvTimeline.setAdapter(adapter);
    }

    private void renderTimeline(List<TimeLineItem> items) {
        List<TimeLineItem> timeline = items == null ? Collections.emptyList() : items;
        FragmentTimeLineBinding currentBinding = binding;
        TimeLineAdapter currentAdapter = adapter;
        currentAdapter.submitList(timeline, () -> {
            // 异步差分完成时，旧页面的 View 可能已经销毁或重建。
            if (binding != currentBinding || adapter != currentAdapter) {
                return;
            }
            boolean empty = currentAdapter.getCurrentList().isEmpty();
            binding.rvTimeline.setVisibility(empty ? View.GONE : View.VISIBLE);
            binding.emptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
        });
    }

    private void openDetail(ConcertEntity concert) {
        Bundle args = new Bundle();
        args.putLong("concertId", concert.getId());
        NavHostFragment.findNavController(this).navigate(R.id.detailFragment, args);
    }

    private void confirmDelete(ConcertEntity concert) {
        if (deleteDialog != null && deleteDialog.isShowing()) return;
        deleteDialog = ConcertDeleteDialog.show(requireContext(), concert,
                callback -> mViewModel.deleteConcert(concert, callback));
    }

    @Override
    public void onDestroyView() {
        if (deleteDialog != null) {
            deleteDialog.dismiss();
            deleteDialog = null;
        }
        binding.rvTimeline.setAdapter(null);
        adapter = null;
        binding = null;
        super.onDestroyView();
    }
}

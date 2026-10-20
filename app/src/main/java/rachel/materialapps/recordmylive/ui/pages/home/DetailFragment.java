package rachel.materialapps.recordmylive.ui.pages.home;

import androidx.lifecycle.ViewModelProvider;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;

import rachel.materialapps.recordmylive.R;
import rachel.materialapps.recordmylive.databinding.FragmentDetailBinding;
import rachel.materialapps.recordmylive.logic.entity.ConcertEntity;
import rachel.materialapps.recordmylive.logic.entity.ConcertSongEntity;
import rachel.materialapps.recordmylive.logic.entity.dto.ConcertWithSongs;
import rachel.materialapps.recordmylive.logic.util.TicketPrice;

public class DetailFragment extends Fragment {

    private FragmentDetailBinding binding;

    private DetailViewModel mViewModel;

    private long concertId;

    public static DetailFragment newInstance() {
        return new DetailFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentDetailBinding.inflate(inflater, container, false);
        View root=binding.getRoot();
        return root;
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.toolbar.setNavigationOnClickListener(v ->
                NavHostFragment.findNavController(this).navigateUp()
        );
        binding.toolbar.inflateMenu(R.menu.menu_detail);
        binding.toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_edit) {
                Bundle args = new Bundle();
                args.putLong("concertId", concertId);
                NavHostFragment.findNavController(this).navigate(R.id.action_detail_to_edit, args);
                return true;
            }
            return false;
        });
        concertId = requireArguments().getLong("concertId");
        mViewModel = new ViewModelProvider(this).get(DetailViewModel.class);
        binding.recyclerSongs.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerSongs.setNestedScrollingEnabled(false);
        binding.recyclerSongs.setAdapter(new SongAdapter());
        mViewModel.getDetail().observe(getViewLifecycleOwner(), this::bindDetail);
        mViewModel.loadConcert(concertId);
    }

    private void bindDetail(ConcertWithSongs detail) {
        if (binding == null || detail == null || detail.concert == null) {
            return;
        }
        ConcertEntity concert = detail.concert;
        binding.tvName.setText(concert.getName());
        binding.chipType.setText(concert.getType() == null
                ? "未设置类型" : concert.getType().getDisplayName());

        String time = concert.getDate() == null ? "日期待定" : concert.getDate().toString();
        time += concert.getTime() == null ? " 时间待定" : " " + concert.getTime();
        binding.tvTime.setText(time);

        Integer durationMinutes = concert.getDurationMinutes();
        if (durationMinutes != null && durationMinutes > 0) {
            binding.tvDuration.setText("演出时长：" + formatDuration(durationMinutes));
            binding.tvDuration.setVisibility(View.VISIBLE);
        } else {
            binding.tvDuration.setVisibility(View.GONE);
        }

        String location = concert.getCity() == null ? "" : concert.getCity();
        if (concert.getVenue() != null && !concert.getVenue().isEmpty()) {
            location += (location.isEmpty() ? "" : " · ") + concert.getVenue();
        }
        binding.tvCityVenue.setText(location.isEmpty() ? "未设置地点" : location);

        List<String> seatParts = new ArrayList<>();
        if (concert.getSeat() != null) {
            addSeatPart(seatParts, concert.getSeat().getSection(), "区");
            addSeatPart(seatParts, concert.getSeat().getRow(), "排");
            addSeatPart(seatParts, concert.getSeat().getNumber(), "座");
        }
        binding.tvSeat.setText(seatParts.isEmpty()
                ? "未设置座位" : android.text.TextUtils.join(" ", seatParts));
        if (concert.getFaceValuePriceCents() == concert.getPaidPriceCents()) {
            binding.tvTicketPrice.setText("票面价与实付价一致 · ¥"
                    + TicketPrice.formatCentsAsYuan(concert.getPaidPriceCents()));
        } else {
            binding.tvTicketPrice.setText("票面价 ¥"
                    + TicketPrice.formatCentsAsYuan(concert.getFaceValuePriceCents())
                    + "    实付价 ¥"
                    + TicketPrice.formatCentsAsYuan(concert.getPaidPriceCents()));
        }

        binding.chipArtists.removeAllViews();
        if (concert.getArtists() != null) {
            for (String artist : concert.getArtists()) {
                Chip chip = new Chip(requireContext());
                chip.setText(artist);
                chip.setCheckable(false);
                chip.setClickable(false);
                binding.chipArtists.addView(chip);
            }
        }

        String cover = concert.getCoverPath();
        // 按封面控件尺寸异步解码，避免进入详情时在主线程读取大图。
        Glide.with(this)
                .load(cover == null || cover.trim().isEmpty() ? null : Uri.parse(cover))
                .placeholder(R.drawable.baseline_imagesmode_24px)
                .fallback(R.drawable.baseline_imagesmode_24px)
                .error(R.drawable.baseline_imagesmode_24px)
                .centerCrop()
                .dontAnimate()
                .into(binding.imgCover);
        binding.tvNote.setText(concert.getNote() == null || concert.getNote().isEmpty()
                ? "无备注" : concert.getNote());
        List<ConcertSongEntity> songs = detail.songs == null ? new ArrayList<>() : detail.songs;
        ((SongAdapter) binding.recyclerSongs.getAdapter()).submitList(songs);
        binding.tvSongCount.setText(songs.size() + " 首");
        binding.tvSongsEmpty.setVisibility(songs.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void addSeatPart(List<String> parts, String value, String suffix) {
        if (value != null && !value.trim().isEmpty()) {
            parts.add(value + suffix);
        }
    }

    private String formatDuration(int totalMinutes) {
        int hours = totalMinutes / 60;
        int minutes = totalMinutes % 60;
        if (hours == 0) {
            return minutes + "分钟";
        }
        if (minutes == 0) {
            return hours + "小时";
        }
        return hours + "小时" + minutes + "分钟";
    }

    @Override
    public void onDestroyView() {
        Glide.with(this).clear(binding.imgCover);
        super.onDestroyView();
        binding = null;
    }

    private static class SongAdapter extends RecyclerView.Adapter<SongAdapter.SongViewHolder> {
        private List<ConcertSongEntity> songs = new ArrayList<>();

        void submitList(List<ConcertSongEntity> songs) {
            this.songs = songs;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public SongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View row = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_song_detail, parent, false);
            return new SongViewHolder(row);
        }

        @Override
        public void onBindViewHolder(@NonNull SongViewHolder holder, int position) {
            ConcertSongEntity song = songs.get(position);
            holder.number.setText(String.valueOf(position + 1));
            holder.name.setText(song.getName());
            holder.encore.setVisibility(song.isEncore() ? View.VISIBLE : View.GONE);
        }

        @Override
        public int getItemCount() {
            return songs.size();
        }

        private static class SongViewHolder extends RecyclerView.ViewHolder {
            final TextView number;
            final TextView name;
            final Chip encore;

            SongViewHolder(@NonNull View row) {
                super(row);
                number = row.findViewById(R.id.tvSongNumber);
                name = row.findViewById(R.id.tvSongName);
                encore = row.findViewById(R.id.chipEncore);
            }
        }
    }

}

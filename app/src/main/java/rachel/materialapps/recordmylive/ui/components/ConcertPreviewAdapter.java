package rachel.materialapps.recordmylive.ui.components;

import android.net.Uri;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.PopupMenu;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.ListAdapter;

import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

import rachel.materialapps.recordmylive.R;
import rachel.materialapps.recordmylive.databinding.ItemConcertBinding;
import rachel.materialapps.recordmylive.logic.entity.ConcertEntity;

public class ConcertPreviewAdapter extends
        ListAdapter<ConcertEntity, ConcertPreviewAdapter.ViewHolder> {

    private final Consumer<ConcertEntity> clickListener;
    private final Consumer<ConcertEntity> deleteListener;

    public ConcertPreviewAdapter(
            Consumer<ConcertEntity> clickListener,
            Consumer<ConcertEntity> deleteListener
    ) {
        super(DIFF_CALLBACK);
        this.clickListener = clickListener;
        this.deleteListener = deleteListener;
    }

    private static final DiffUtil.ItemCallback<ConcertEntity> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<ConcertEntity>() {

                @Override
                public boolean areItemsTheSame(
                        @NonNull ConcertEntity oldItem,
                        @NonNull ConcertEntity newItem
                ) {
                    return oldItem.getId() == newItem.getId();
                }

                @Override
                public boolean areContentsTheSame(
                        @NonNull ConcertEntity oldItem,
                        @NonNull ConcertEntity newItem
                ) {
                    return oldItem.equals(newItem);
                }
            };

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemConcertBinding binding;

        ViewHolder(ItemConcertBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(
                ConcertEntity concert,
                Consumer<ConcertEntity> clickListener,
                Consumer<ConcertEntity> deleteListener
        ) {

            String timeText;

            if (concert.getTime() == null) {
                timeText = concert.getDate().toString() +" "+"时间待定";
            } else {
                timeText = concert.getDate()
                        + " "
                        + concert.getTime().format(
                        DateTimeFormatter.ofPattern("HH:mm")
                );
            }

            binding.tvTime.setText(timeText);

            binding.tvVenue.setText(
                    concert.getCity() + " · " + concert.getVenue()
            );

            binding.tvName.setText(concert.getName());

            binding.tvVenue.setText(
                    concert.getCity() + " · " + concert.getVenue()
            );

            if (concert.getArtists() == null ||
                    concert.getArtists().isEmpty()) {

                binding.tvArtist.setText("暂未设置歌手");

            } else {

                binding.tvArtist.setText(
                        TextUtils.join(" / ", concert.getArtists())
                );

            }

            if (concert.getCoverPath() != null) {

                binding.imgCover.setImageURI(
                        Uri.parse(concert.getCoverPath())
                );

            } else {

                binding.imgCover.setImageResource(
                        R.drawable.baseline_imagesmode_24px
                );

            }



            binding.getRoot().setOnClickListener(v -> {

                if (clickListener != null) {
                    clickListener.accept(concert);
                }

            });

            binding.btnMore.setOnClickListener(v -> {

                PopupMenu menu = new PopupMenu(
                        v.getContext(),
                        binding.btnMore
                );

                menu.inflate(R.menu.menu_concert_item);

                menu.setOnMenuItemClickListener(item -> {

                    int itemId = item.getItemId();
                    if(itemId==R.id.action_edit){
                        //todo: 编辑
                        return true;
                    }
                    else if(itemId==R.id.action_delete){
                        deleteListener.accept(concert);
                        return true;
                    }
                    else{
                        return true;
                    }
                });

                menu.show();

            });

        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {

        ItemConcertBinding binding =
                ItemConcertBinding.inflate(
                        LayoutInflater.from(parent.getContext()),
                        parent,
                        false
                );

        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(
            @NonNull ViewHolder holder,
            int position
    ) {

        holder.bind(
                getItem(position),
                clickListener,
                deleteListener
        );

    }
}
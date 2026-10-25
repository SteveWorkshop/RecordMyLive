package rachel.materialapps.recordmylive.ui.pages.timeline;

import android.net.Uri;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.time.format.DateTimeFormatter;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.function.Consumer;

import rachel.materialapps.recordmylive.R;
import rachel.materialapps.recordmylive.databinding.ItemTimelineConcertBinding;
import rachel.materialapps.recordmylive.databinding.ItemTimelineMonthBinding;
import rachel.materialapps.recordmylive.logic.entity.ConcertEntity;

public class TimeLineAdapter extends ListAdapter<TimeLineItem, RecyclerView.ViewHolder> {
    private static final int TYPE_MONTH = 0;
    private static final int TYPE_CONCERT = 1;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy年M月d日");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private final Consumer<ConcertEntity> clickListener;
    private final Consumer<ConcertEntity> deleteListener;

    public TimeLineAdapter(Consumer<ConcertEntity> clickListener,
                           Consumer<ConcertEntity> deleteListener) {
        super(DIFF_CALLBACK);
        this.clickListener = clickListener;
        this.deleteListener = deleteListener;
        setStateRestorationPolicy(StateRestorationPolicy.PREVENT_WHEN_EMPTY);
    }

    private static final DiffUtil.ItemCallback<TimeLineItem> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<TimeLineItem>() {
                @Override
                public boolean areItemsTheSame(@NonNull TimeLineItem oldItem,
                                               @NonNull TimeLineItem newItem) {
                    if (oldItem instanceof TimeLineItem.MonthHeader
                            && newItem instanceof TimeLineItem.MonthHeader) {
                        return ((TimeLineItem.MonthHeader) oldItem).getMonth().equals(
                                ((TimeLineItem.MonthHeader) newItem).getMonth());
                    }
                    if (oldItem instanceof TimeLineItem.ConcertItem
                            && newItem instanceof TimeLineItem.ConcertItem) {
                        return ((TimeLineItem.ConcertItem) oldItem).getConcert().getId()
                                == ((TimeLineItem.ConcertItem) newItem).getConcert().getId();
                    }
                    return false;
                }

                @Override
                public boolean areContentsTheSame(@NonNull TimeLineItem oldItem,
                                                  @NonNull TimeLineItem newItem) {
                    if (oldItem instanceof TimeLineItem.MonthHeader
                            && newItem instanceof TimeLineItem.MonthHeader) {
                        return areItemsTheSame(oldItem, newItem);
                    }
                    if (oldItem instanceof TimeLineItem.ConcertItem
                            && newItem instanceof TimeLineItem.ConcertItem) {
                        return ((TimeLineItem.ConcertItem) oldItem).getConcert().equals(
                                ((TimeLineItem.ConcertItem) newItem).getConcert());
                    }
                    return false;
                }
            };

    @Override
    public int getItemViewType(int position) {
        return getItem(position) instanceof TimeLineItem.MonthHeader ? TYPE_MONTH : TYPE_CONCERT;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_MONTH) {
            return new MonthViewHolder(ItemTimelineMonthBinding.inflate(inflater, parent, false));
        }
        return new ConcertViewHolder(ItemTimelineConcertBinding.inflate(inflater, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        TimeLineItem item = getItem(position);
        if (holder instanceof MonthViewHolder) {
            ((MonthViewHolder) holder).binding.tvMonth.setText(
                    ((TimeLineItem.MonthHeader) item).getTitle());
        } else {
            ((ConcertViewHolder) holder).bind(
                    ((TimeLineItem.ConcertItem) item).getConcert(), clickListener, deleteListener);
        }
    }

    @Override
    public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
        if (holder instanceof ConcertViewHolder) {
            ConcertViewHolder concertHolder = (ConcertViewHolder) holder;
            Glide.with(concertHolder.binding.imgCover).clear(concertHolder.binding.imgCover);
            concertHolder.binding.imgCover.setImageDrawable(null);
            concertHolder.binding.getRoot().setOnClickListener(null);
            concertHolder.binding.btnDelete.setOnClickListener(null);
        }
        super.onViewRecycled(holder);
    }

    private static class MonthViewHolder extends RecyclerView.ViewHolder {
        final ItemTimelineMonthBinding binding;

        MonthViewHolder(ItemTimelineMonthBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    private static class ConcertViewHolder extends RecyclerView.ViewHolder {
        final ItemTimelineConcertBinding binding;

        ConcertViewHolder(ItemTimelineConcertBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(ConcertEntity concert, Consumer<ConcertEntity> clickListener,
                  Consumer<ConcertEntity> deleteListener) {
            binding.btnDelete.setOnClickListener(v -> deleteListener.accept(concert));
            binding.tvName.setText(concert.getName());
            String time = concert.getTime() == null
                    ? binding.getRoot().getContext().getString(R.string.timeline_time_pending)
                    : concert.getTime().format(TIME_FORMAT);
            binding.tvDateTime.setText(binding.getRoot().getContext().getString(
                    R.string.timeline_date_time, concert.getDate().format(DATE_FORMAT), time));
            binding.tvCity.setText(concert.getCity());

            LocalDate concertDate = concert.getDate();
            long days = ChronoUnit.DAYS.between(LocalDate.now(), concertDate);
            if (days > 0) {
                binding.tvRelativeTime.setText(
                        binding.getRoot().getContext().getString(R.string.timeline_days_left, days));
                binding.tvRelativeTime.setTextColor(
                        resolveThemeColor(binding.tvRelativeTime, androidx.appcompat.R.attr.colorPrimary));
            } else if (days == 0) {
                binding.tvRelativeTime.setText(R.string.timeline_today);
                binding.tvRelativeTime.setTextColor(
                        resolveThemeColor(binding.tvRelativeTime, androidx.appcompat.R.attr.colorPrimary));
            } else {
                binding.tvRelativeTime.setText(R.string.timeline_ended);
                binding.tvRelativeTime.setTextColor(
                        resolveThemeColor(binding.tvRelativeTime, com.google.android.material.R.attr.colorOnSurfaceVariant));
            }

            String cover = concert.getCoverPath();
            // Glide 按 ImageView 的实际尺寸异步解码并缓存，绑定新条目时替换旧请求。
            Glide.with(binding.imgCover)
                    .load(cover == null || cover.trim().isEmpty() ? null : Uri.parse(cover))
                    .placeholder(R.drawable.baseline_imagesmode_24px)
                    .fallback(R.drawable.baseline_imagesmode_24px)
                    .error(R.drawable.baseline_imagesmode_24px)
                    .centerCrop()
                    .dontAnimate()
                    .into(binding.imgCover);
            binding.getRoot().setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.accept(concert);
                }
            });
        }

        private int resolveThemeColor(android.widget.TextView view, int attribute) {
            TypedValue value = new TypedValue();
            if (view.getContext().getTheme().resolveAttribute(attribute, value, true)) {
                return value.resourceId != 0
                        ? view.getContext().getColor(value.resourceId) : value.data;
            }
            return view.getCurrentTextColor();
        }
    }
}

package rachel.materialapps.recordmylive.ui.pages.my;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.MotionEvent;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import rachel.materialapps.recordmylive.R;
import rachel.materialapps.recordmylive.databinding.FragmentFavoriteArtistsBinding;
import rachel.materialapps.recordmylive.databinding.ItemFavoriteArtistBinding;
import rachel.materialapps.recordmylive.logic.entity.FavoriteArtistEntity;
import rachel.materialapps.recordmylive.logic.repository.FavoriteArtistRepository;

public class FavoriteArtistsFragment extends Fragment {
    private FragmentFavoriteArtistsBinding binding;
    private FavoriteArtistsViewModel viewModel;
    private AlertDialog deleteDialog;
    private ItemTouchHelper touchHelper;
    private boolean dragging;
    private boolean moved;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentFavoriteArtistsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(FavoriteArtistsViewModel.class);
        binding.toolbar.setNavigationOnClickListener(v -> NavHostFragment.findNavController(this).navigateUp());
        binding.addArtist.setOnClickListener(v -> openEditor(0));
        ArtistAdapter adapter = new ArtistAdapter(this::confirmRemove, artist -> openEditor(artist.getId()));
        adapter.toggleNearby = (artist, selected) -> viewModel.selectForNearbyMatch(artist.getId(), selected);
        adapter.startDrag = holder -> {
            if (!Boolean.TRUE.equals(viewModel.getBusy().getValue())) touchHelper.startDrag(holder);
        };
        adapter.saveOrder = () -> viewModel.reorder(adapter.ids());
        binding.artistList.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.artistList.setAdapter(adapter);
        touchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
            @Override public boolean isLongPressDragEnabled() { return false; }
            @Override public int getMovementFlags(@NonNull RecyclerView recycler, @NonNull RecyclerView.ViewHolder holder) {
                return Boolean.TRUE.equals(viewModel.getBusy().getValue()) ? 0 : super.getMovementFlags(recycler, holder);
            }
            @Override public void onSelectedChanged(@Nullable RecyclerView.ViewHolder holder, int state) {
                if (state == ItemTouchHelper.ACTION_STATE_DRAG) dragging = true;
                super.onSelectedChanged(holder, state);
            }
            @Override public boolean onMove(@NonNull RecyclerView recycler,
                    @NonNull RecyclerView.ViewHolder from, @NonNull RecyclerView.ViewHolder to) {
                boolean changed = adapter.move(from.getBindingAdapterPosition(), to.getBindingAdapterPosition());
                moved |= changed;
                return changed;
            }
            @Override public void onSwiped(@NonNull RecyclerView.ViewHolder holder, int direction) {}
            @Override public void clearView(@NonNull RecyclerView recycler, @NonNull RecyclerView.ViewHolder holder) {
                super.clearView(recycler, holder);
                dragging = false;
                if (moved) {
                    moved = false;
                    viewModel.reorder(adapter.ids());
                } else if (!Boolean.TRUE.equals(viewModel.getBusy().getValue())) {
                    adapter.submitList(viewModel.getArtists().getValue());
                }
            }
        });
        touchHelper.attachToRecyclerView(binding.artistList);
        viewModel.getArtists().observe(getViewLifecycleOwner(), artists -> {
            List<FavoriteArtistEntity> items = artists == null ? Collections.emptyList() : artists;
            binding.empty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
            binding.artistCount.setText(getString(R.string.favorite_artist_count, items.size()));
            int nearbySelected = 0;
            for (FavoriteArtistEntity artist : items) if (artist.isNearbyMatchSelected()) nearbySelected++;
            binding.nearbySelectionCount.setText(getString(R.string.favorite_artists_nearby_count, nearbySelected));
            adapter.nearbySelectionCount = nearbySelected;
            if (!dragging && !Boolean.TRUE.equals(viewModel.getBusy().getValue())) adapter.submitList(items);
        });
        viewModel.getBusy().observe(getViewLifecycleOwner(), busy -> {
            binding.addArtist.setEnabled(!Boolean.TRUE.equals(busy));
            adapter.enabled = !Boolean.TRUE.equals(busy);
            if (!Boolean.TRUE.equals(busy) && !dragging) adapter.submitList(viewModel.getArtists().getValue());
        });
        viewModel.getResult().observe(getViewLifecycleOwner(), result -> {
            if (result == null) return;
            viewModel.consumeResult();
            switch (result) {
                case ERROR:
                    Toast.makeText(requireContext(), R.string.favorite_artist_error, Toast.LENGTH_SHORT).show();
                    break;
                case NEARBY_SELECTION_LIMIT:
                    Toast.makeText(requireContext(), R.string.favorite_artists_nearby_limit, Toast.LENGTH_SHORT).show();
                    break;
                default:
                    break;
            }
        });
    }

    private void openEditor(long id) {
        if (dragging || Boolean.TRUE.equals(viewModel.getBusy().getValue())) return;
        Bundle args = new Bundle();
        args.putLong("artistId", id);
        NavHostFragment.findNavController(this).navigate(R.id.artistEditorFragment, args);
    }

    private void confirmRemove(FavoriteArtistEntity artist) {
        if (dragging || Boolean.TRUE.equals(viewModel.getBusy().getValue())
                || (deleteDialog != null && deleteDialog.isShowing())) return;
        deleteDialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.favorite_artist_remove)
                .setMessage(getString(R.string.favorite_artist_remove_message, artist.getName()))
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.favorite_artist_remove,
                        (dialog, which) -> viewModel.delete(artist.getId()))
                .show();
    }

    @Override
    public void onDestroyView() {
        if (deleteDialog != null) {
            deleteDialog.dismiss();
            deleteDialog = null;
        }
        if (moved) {
            moved = false;
            viewModel.reorder(((ArtistAdapter) binding.artistList.getAdapter()).ids());
        }
        touchHelper.attachToRecyclerView(null);
        touchHelper = null;
        dragging = false;
        moved = false;
        binding.artistList.setAdapter(null);
        super.onDestroyView();
        binding = null;
    }

    private static class ArtistAdapter extends RecyclerView.Adapter<ArtistAdapter.Holder> {
        private final List<FavoriteArtistEntity> items = new java.util.ArrayList<>();
        private Consumer<Holder> startDrag;
        private Runnable saveOrder;
        private boolean enabled = true;
        private int nearbySelectionCount;
        private Consumer2 toggleNearby;
        private final Consumer<FavoriteArtistEntity> remove;
        private final Consumer<FavoriteArtistEntity> edit;

        ArtistAdapter(Consumer<FavoriteArtistEntity> remove, Consumer<FavoriteArtistEntity> edit) {
            setHasStableIds(true);
            this.remove = remove;
            this.edit = edit;
        }

        void submitList(List<FavoriteArtistEntity> artists) {
            items.clear();
            if (artists != null) items.addAll(artists);
            notifyDataSetChanged();
        }
        @Override public int getItemCount() { return items.size(); }
        @Override public long getItemId(int position) { return items.get(position).getId(); }
        List<Long> ids() {
            List<Long> ids = new java.util.ArrayList<>();
            for (FavoriteArtistEntity artist : items) ids.add(artist.getId());
            return ids;
        }
        boolean move(int from, int to) {
            if (!enabled || from < 0 || to < 0 || from >= items.size() || to >= items.size() || from == to) return false;
            items.add(to, items.remove(from));
            notifyItemMoved(from, to);
            return true;
        }
        boolean accessibleMove(Holder holder, int delta) {
            int from = holder.getBindingAdapterPosition();
            if (!move(from, from + delta)) return false;
            saveOrder.run();
            return true;
        }

        @NonNull @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(ItemFavoriteArtistBinding.inflate(
                    LayoutInflater.from(parent.getContext()), parent, false));
        }

        @Override public void onBindViewHolder(@NonNull Holder holder, int position) {
            FavoriteArtistEntity artist = items.get(position);
            holder.binding.dragHandle.setOnTouchListener((v, event) -> {
                if (event.getActionMasked() == MotionEvent.ACTION_DOWN && enabled) startDrag.accept(holder);
                return true;
            });
            androidx.core.view.ViewCompat.replaceAccessibilityAction(holder.binding.dragHandle,
                    androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_SCROLL_BACKWARD,
                    holder.itemView.getContext().getString(R.string.favorite_artist_move_up),
                    (v, args) -> accessibleMove(holder, -1));
            androidx.core.view.ViewCompat.replaceAccessibilityAction(holder.binding.dragHandle,
                    androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_SCROLL_FORWARD,
                    holder.itemView.getContext().getString(R.string.favorite_artist_move_down),
                    (v, args) -> accessibleMove(holder, 1));
            holder.binding.artistName.setText(artist.getName());
            holder.binding.nearbyMatchSelected.setContentDescription(holder.itemView.getContext().getString(
                    R.string.favorite_artist_nearby_selection, artist.getName()));
            holder.binding.nearbyMatchSelected.setOnCheckedChangeListener(null);
            holder.binding.nearbyMatchSelected.setTag(artist.getId());
            holder.binding.nearbyMatchSelected.setChecked(artist.isNearbyMatchSelected());
            holder.binding.nearbyMatchSelected.setEnabled(enabled &&
                    (artist.isNearbyMatchSelected() || nearbySelectionCount < FavoriteArtistRepository.MAX_NEARBY_MATCH_ARTISTS));
            holder.binding.nearbyMatchSelected.setOnCheckedChangeListener((button, selected) -> {
                if (enabled && toggleNearby != null) toggleNearby.accept(artist, selected);
            });
            rachel.materialapps.recordmylive.logic.util.ArtistImages.show(holder.binding.artistImage, artist.getImageFile());
            holder.itemView.setOnClickListener(v -> edit.accept(artist));
            holder.binding.removeArtist.setContentDescription(holder.itemView.getContext()
                    .getString(R.string.favorite_artist_remove_description, artist.getName()));
            holder.binding.removeArtist.setOnClickListener(v -> remove.accept(artist));
        }

        static class Holder extends RecyclerView.ViewHolder {
            final ItemFavoriteArtistBinding binding;
            Holder(ItemFavoriteArtistBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }
    }

    @FunctionalInterface private interface Consumer2 {
        void accept(FavoriteArtistEntity artist, boolean selected);
    }
}

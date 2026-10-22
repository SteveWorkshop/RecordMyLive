package rachel.materialapps.recordmylive.ui.pages.nearby;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Intent;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.graphics.drawable.GradientDrawable;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.material.color.MaterialColors;
import rachel.materialapps.recordmylive.R;
import rachel.materialapps.recordmylive.databinding.FragmentNearByBinding;
import rachel.materialapps.recordmylive.databinding.ItemNearbyPersonBinding;
import rachel.materialapps.recordmylive.logic.nearby.NearbyPermissions;
import rachel.materialapps.recordmylive.logic.nearby.NearbySession;
import rachel.materialapps.recordmylive.logic.nearby.NearbyMatchNotifications;
import rachel.materialapps.recordmylive.logic.nearby.NearbyDiscovery;
import rachel.materialapps.recordmylive.logic.repository.ProfileRepository;
import rachel.materialapps.recordmylive.logic.repository.FavoriteArtistRepository;
import rachel.materialapps.recordmylive.logic.util.ProfileImages;

public class NearByFragment extends Fragment {

    private FragmentNearByBinding binding;
    private NearByViewModel model;
    private ProfileRepository profile;
    private boolean permissionPending;
    private boolean refreshingAlerts;
    private final ActivityResultLauncher<String> notificationPermission = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), granted -> {
                if (binding != null) {
                    refreshAlerts();
                    NearbyDiscovery.get(requireContext()).refreshState();
                }
            });
    private final ActivityResultLauncher<String[]> permissions = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                boolean requestedHere = permissionPending;
                permissionPending = false;
                if (!requestedHere || binding == null
                        || !getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED)) return;
                if (NearbyPermissions.granted(requireContext())) startAfterPermissions();
                else model.showIssue(NearbySession.Issue.PERMISSION);
            });

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentNearByBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        model = new ViewModelProvider(this).get(NearByViewModel.class);
        profile = new ProfileRepository(requireContext());
        new FavoriteArtistRepository(requireContext()).getAll().observe(getViewLifecycleOwner(),
                favorites -> NearbyDiscovery.get(requireContext()).preloadFavorites(favorites));
        PeopleAdapter adapter = new PeopleAdapter(peer -> {
            if (getChildFragmentManager().findFragmentByTag("shared_artists") != null) return;
            SharedArtistsDialog dialog = new SharedArtistsDialog();
            Bundle args = new Bundle();
            args.putString("peer", peer.id);
            dialog.setArguments(args);
            dialog.show(getChildFragmentManager(), "shared_artists");
        });
        binding.nearbyPeople.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.nearbyPeople.setAdapter(adapter);
        binding.nearbyHelp.setOnClickListener(this::showNearbyHelp);
        binding.nearbyToggle.setOnClickListener(v -> {
            NearbySession.Snapshot state = model.getState().getValue();
            if (state != null && (state.stage == NearbySession.Stage.STARTING || state.stage == NearbySession.Stage.ACTIVE))
                model.stop();
            else requestStart();
        });
        binding.nearbyEditProfile.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.profileEditorFragment));
        binding.nearbySettings.setOnClickListener(v -> openSettings());
        NearbyMatchNotifications.createChannel(requireContext());
        refreshAlerts();
        binding.nearbyMatchAlerts.setOnCheckedChangeListener((button, enabled) -> {
            if (refreshingAlerts) return;
            NearbyMatchNotifications.setEnabled(requireContext(), enabled);
            if (enabled && Build.VERSION.SDK_INT >= 33 && !NearbyPermissions.notificationGranted(requireContext()))
                notificationPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS);
            refreshAlerts();
        });
        binding.nearbyAlertSettings.setOnClickListener(v ->
                startActivity(NearbyMatchNotifications.settingsIntent(requireContext())));
        model.getState().observe(getViewLifecycleOwner(), state -> {
            boolean running = state.stage == NearbySession.Stage.STARTING || state.stage == NearbySession.Stage.ACTIVE;
            refreshProfile();
            binding.nearbyToggle.setText(running ? R.string.nearby_stop : R.string.nearby_start);
            binding.nearbyToggle.setEnabled(!permissionPending && state.stage != NearbySession.Stage.STOPPING);
            binding.nearbyProgress.setVisibility(state.stage == NearbySession.Stage.STARTING
                    || state.stage == NearbySession.Stage.STOPPING ? View.VISIBLE : View.GONE);
            binding.nearbyStatus.setText(statusText(state));
            binding.nearbySettings.setVisibility(state.issue == NearbySession.Issue.PERMISSION
                    || state.issue == NearbySession.Issue.BLUETOOTH || state.issue == NearbySession.Issue.LOCATION ? View.VISIBLE : View.GONE);
            binding.nearbyCount.setText(getString(R.string.nearby_count, state.peers.size()));
            binding.nearbyEmpty.setVisibility(state.peers.isEmpty() ? View.VISIBLE : View.GONE);
            binding.nearbyEmpty.setText(running ? R.string.nearby_empty_searching : R.string.nearby_empty);
            adapter.submitList(state.peers);
        });
    }

    private void requestStart() {
        if (permissionPending) return;
        if (!ProfileRepository.isValidNickname(profile.getNickname())) {
            model.showIssue(NearbySession.Issue.NAME_REQUIRED);
            return;
        }
        if (GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(requireContext()) != ConnectionResult.SUCCESS) {
            model.showIssue(NearbySession.Issue.SERVICES);
            return;
        }
        if (NearbyPermissions.granted(requireContext()) && NearbyPermissions.notificationGranted(requireContext())) startAfterPermissions();
        else {
            permissionPending = true;
            binding.nearbyToggle.setEnabled(false);
            permissions.launch(NearbyPermissions.forStart());
        }
    }

    private void startAfterPermissions() {
        if (!ProfileRepository.isValidNickname(profile.getNickname())) {
            model.showIssue(NearbySession.Issue.NAME_REQUIRED);
            return;
        }
        if (!NearbyPermissions.granted(requireContext())) {
            model.showIssue(NearbySession.Issue.PERMISSION);
            return;
        }
        try {
            BluetoothManager bluetooth = requireContext().getSystemService(BluetoothManager.class);
            BluetoothAdapter adapter = bluetooth == null ? null : bluetooth.getAdapter();
            if (adapter == null || !adapter.isEnabled()) {
                model.showIssue(NearbySession.Issue.BLUETOOTH);
                return;
            }
            if (Build.VERSION.SDK_INT <= 32) {
                LocationManager location = requireContext().getSystemService(LocationManager.class);
                if (location == null || !location.isLocationEnabled()) {
                    model.showIssue(NearbySession.Issue.LOCATION);
                    return;
                }
            }
            model.start();
        } catch (SecurityException e) { model.showIssue(NearbySession.Issue.PERMISSION); }
    }

    private int statusText(NearbySession.Snapshot state) {
        switch (state.issue) {
            case NAME_REQUIRED: return R.string.nearby_name_required;
            case PERMISSION: return R.string.nearby_permission_denied;
            case SERVICES: return R.string.nearby_services_unavailable;
            case BLUETOOTH: return R.string.nearby_bluetooth_off;
            case LOCATION: return R.string.nearby_location_off;
            case START_FAILED: return R.string.nearby_start_failed;
            default:
                switch (state.stage) {
                    case STARTING: return R.string.nearby_starting;
                    case ACTIVE: return R.string.nearby_active;
                    case STOPPING: return R.string.nearby_stopping;
                    default: return R.string.nearby_idle;
                }
        }
    }

    private void openSettings() {
        NearbySession.Snapshot state = model.getState().getValue();
        String action = state != null && state.issue == NearbySession.Issue.BLUETOOTH ? Settings.ACTION_BLUETOOTH_SETTINGS
                : state != null && state.issue == NearbySession.Issue.LOCATION ? Settings.ACTION_LOCATION_SOURCE_SETTINGS
                : Settings.ACTION_APPLICATION_DETAILS_SETTINGS;
        Intent intent = new Intent(action);
        if (Settings.ACTION_APPLICATION_DETAILS_SETTINGS.equals(action)) intent.setData(Uri.parse("package:" + requireContext().getPackageName()));
        if (intent.resolveActivity(requireContext().getPackageManager()) != null) startActivity(intent);
    }

    @Override public void onResume() {
        super.onResume();
        refreshProfile();
        refreshAlerts();
        NearbyDiscovery.get(requireContext()).refreshState();
        NearbySession.Snapshot state = model.getState().getValue();
        binding.nearbyToggle.setEnabled(!permissionPending && (state == null || state.stage != NearbySession.Stage.STOPPING));
    }

    @Override public void onStop() {
        permissionPending = false;
        model.clearIssue();
        super.onStop();
    }

    private void refreshProfile() {
        String nickname = model.getNickname(profile.getNickname());
        binding.nearbyNickname.setText(nickname.isEmpty() ? getString(R.string.profile_unset) : nickname);
        ProfileImages.show(binding.nearbyAvatar, ProfileImages.file(requireContext(), profile.getAvatarFile()));
    }

    private void showNearbyHelp(View anchor) {
        int horizontalPadding = dp(16);
        int maxWidth = Math.min(dp(320), getResources().getDisplayMetrics().widthPixels - dp(48));
        TextView message = new TextView(requireContext());
        message.setText(R.string.nearby_visibility_hint);
        message.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium);
        message.setTextColor(MaterialColors.getColor(anchor, com.google.android.material.R.attr.colorOnSurface));
        message.setPadding(horizontalPadding, dp(12), horizontalPadding, dp(12));
        GradientDrawable background = new GradientDrawable();
        background.setColor(MaterialColors.getColor(anchor, com.google.android.material.R.attr.colorSurfaceContainerHigh));
        background.setCornerRadius(dp(8));
        LinearLayout content = new LinearLayout(requireContext());
        content.setPadding(dp(4), dp(4), dp(4), dp(4));
        content.addView(message, new LinearLayout.LayoutParams(maxWidth, ViewGroup.LayoutParams.WRAP_CONTENT));
        PopupWindow popup = new PopupWindow(content, maxWidth + dp(8), ViewGroup.LayoutParams.WRAP_CONTENT, true);
        popup.setBackgroundDrawable(background);
        popup.setOutsideTouchable(true);
        popup.setElevation(dp(8));
        popup.showAsDropDown(anchor, 0, 0, android.view.Gravity.END);
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void refreshAlerts() {
        boolean enabled = NearbyMatchNotifications.enabled(requireContext());
        boolean blocked = enabled && !NearbyMatchNotifications.available(requireContext());
        refreshingAlerts = true;
        binding.nearbyMatchAlerts.setChecked(enabled);
        refreshingAlerts = false;
        binding.nearbyAlertHint.setText(blocked ? R.string.nearby_alert_blocked : R.string.nearby_alert_hint);
        binding.nearbyAlertSettings.setVisibility(blocked ? View.VISIBLE : View.GONE);
    }

    @Override public void onDestroyView() {
        binding.nearbyPeople.setAdapter(null);
        super.onDestroyView();
        binding = null;
    }

    private static final class PeopleAdapter extends ListAdapter<NearbySession.Peer, PersonHolder> {
        private final java.util.function.Consumer<NearbySession.Peer> click;
        PeopleAdapter(java.util.function.Consumer<NearbySession.Peer> click) {
            super(new DiffUtil.ItemCallback<NearbySession.Peer>() {
                @Override public boolean areItemsTheSame(@NonNull NearbySession.Peer a, @NonNull NearbySession.Peer b) { return a.id.equals(b.id); }
                @Override public boolean areContentsTheSame(@NonNull NearbySession.Peer a, @NonNull NearbySession.Peer b) {
                    return a.nickname.equals(b.nickname) && a.matched == b.matched && a.matchFailed == b.matchFailed && a.commonArtists.equals(b.commonArtists);
                }
            });
            this.click = click;
        }
        @NonNull @Override public PersonHolder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
            return new PersonHolder(ItemNearbyPersonBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        }
        @Override public void onBindViewHolder(@NonNull PersonHolder holder, int position) {
            NearbySession.Peer peer = getItem(position);
            holder.binding.nearbyPersonName.setText(peer.nickname);
            holder.binding.nearbyPersonMatch.setText(peer.matchFailed ? holder.itemView.getContext().getString(R.string.nearby_match_failed)
                    : !peer.matched ? holder.itemView.getContext().getString(R.string.nearby_match_pending)
                    : peer.commonArtists.isEmpty() ? holder.itemView.getContext().getString(R.string.nearby_match_empty)
                    : holder.itemView.getContext().getString(R.string.nearby_match_count, peer.commonArtists.size()));
            boolean clickable = peer.matched && !peer.commonArtists.isEmpty();
            holder.itemView.setOnClickListener(clickable ? v -> click.accept(peer) : null);
            holder.itemView.setClickable(clickable);
            holder.itemView.setFocusable(clickable);
            holder.itemView.setBackgroundResource(clickable ? android.R.drawable.list_selector_background : 0);
        }
    }
    private static final class PersonHolder extends RecyclerView.ViewHolder {
        final ItemNearbyPersonBinding binding;
        PersonHolder(ItemNearbyPersonBinding binding) { super(binding.getRoot()); this.binding = binding; }
    }

    public static class SharedArtistsDialog extends androidx.fragment.app.DialogFragment {
        @NonNull @Override public android.app.Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
            android.content.Context context = requireContext();
            android.widget.LinearLayout content = new android.widget.LinearLayout(context);
            content.setOrientation(android.widget.LinearLayout.VERTICAL);
            android.widget.TextView note = new android.widget.TextView(context);
            int padding = Math.round(24 * getResources().getDisplayMetrics().density);
            note.setPadding(padding, padding / 2, padding, padding / 2);
            note.setText(R.string.nearby_match_note);
            content.addView(note);
            RecyclerView list = new RecyclerView(context);
            list.setLayoutManager(new LinearLayoutManager(context));
            SharedArtistAdapter adapter = new SharedArtistAdapter();
            list.setAdapter(adapter);
            int height = Math.round(Math.min(360, getResources().getConfiguration().screenHeightDp * .45f)
                    * getResources().getDisplayMetrics().density);
            content.addView(list, new android.widget.LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height));
            androidx.appcompat.app.AlertDialog dialog = new com.google.android.material.dialog.MaterialAlertDialogBuilder(context)
                    .setTitle(R.string.nearby_title).setView(content)
                    .setPositiveButton(R.string.nearby_match_close, null).create();
            rachel.materialapps.recordmylive.logic.nearby.NearbyDiscovery.get(context).getState().observe(this, state -> {
                NearbySession.Peer peer = null;
                for (NearbySession.Peer candidate : state.peers)
                    if (candidate.id.equals(requireArguments().getString("peer"))) peer = candidate;
                if (peer == null || !peer.matched) { dismiss(); return; }
                dialog.setTitle(getString(R.string.nearby_match_title, peer.nickname));
                note.setText(peer.commonArtists.isEmpty() ? R.string.nearby_match_empty : R.string.nearby_match_note);
                adapter.submitList(peer.commonArtists);
            });
            return dialog;
        }
    }

    private static class SharedArtistAdapter extends ListAdapter<NearbySession.Artist, SharedArtistHolder> {
        SharedArtistAdapter() {
            super(new DiffUtil.ItemCallback<NearbySession.Artist>() {
                @Override public boolean areItemsTheSame(@NonNull NearbySession.Artist a, @NonNull NearbySession.Artist b) { return a.key.equals(b.key); }
                @Override public boolean areContentsTheSame(@NonNull NearbySession.Artist a, @NonNull NearbySession.Artist b) {
                    return a.name.equals(b.name) && java.util.Objects.equals(a.imageFile, b.imageFile);
                }
            });
        }
        @NonNull @Override public SharedArtistHolder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
            return new SharedArtistHolder(rachel.materialapps.recordmylive.databinding.ItemNearbyArtistBinding.inflate(
                    LayoutInflater.from(parent.getContext()), parent, false));
        }
        @Override public void onBindViewHolder(@NonNull SharedArtistHolder holder, int position) {
            NearbySession.Artist artist = getItem(position);
            holder.binding.sharedArtistName.setText(artist.name);
            rachel.materialapps.recordmylive.logic.util.ArtistImages.show(holder.binding.sharedArtistImage, artist.imageFile);
        }
    }
    private static class SharedArtistHolder extends RecyclerView.ViewHolder {
        final rachel.materialapps.recordmylive.databinding.ItemNearbyArtistBinding binding;
        SharedArtistHolder(rachel.materialapps.recordmylive.databinding.ItemNearbyArtistBinding binding) {
            super(binding.getRoot()); this.binding = binding;
        }
    }
}

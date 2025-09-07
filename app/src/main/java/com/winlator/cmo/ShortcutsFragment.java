package com.winlator.cmo;

import static androidx.core.content.ContextCompat.getSystemService;

import static com.winlator.cmo.MainActivity.PACKAGE_NAME;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.drawable.Icon;
import android.icu.lang.UCharacter;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.winlator.cmo.container.Container;
import com.winlator.cmo.container.ContainerManager;
import com.winlator.cmo.container.Shortcut;
import com.winlator.cmo.contentdialog.ContentDialog;
import com.winlator.cmo.contentdialog.ShortcutSettingsDialog;
import com.winlator.cmo.core.AppUtils;
import com.winlator.cmo.core.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ShortcutsFragment extends Fragment {
    private RecyclerView recyclerView;
    private TextView emptyTextView;
    private ContainerManager manager;
    private ShortcutSettingsDialog currentDialog;
    private ShortcutsAdapter adapter;
    private SharedPreferences prefs;
    private MenuItem sortItem;
    private final String[] sortTypeText = {"Name", "Container Id", "Path", "Playtime", "Play Count", "Last Play Date"};
    private final String[] prefsText = {"cur_sort_type", "last_view_type", "playtime_stats", "cur_grid_type", "cur_list_type"};
    private String searchText;
    private Container shortcutContainer;
    public int curSortType = 0;
    public int curGridType = 0;
    public int curListType = 0;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.shortcuts_fragment, container, false);
        recyclerView = rootView.findViewById(R.id.RecyclerView);
        emptyTextView = rootView.findViewById(R.id.TVEmptyText);

        prefs = requireContext().getSharedPreferences("ShortcutsPref", Context.MODE_PRIVATE);

        curSortType = prefs.getInt(prefsText[0], 0);
        curGridType = prefs.getInt(prefsText[3], 0);
        curListType = prefs.getInt(prefsText[4], 1); //fix default value

        setRecyclerLayoutManager(curGridType, curListType);

        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        manager = new ContainerManager(getContext());
        loadShortcutsList(curSortType);
        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(R.string.shortcuts);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        // Clear any existing menu items to prevent duplication
        menu.clear();
        menuInflater.inflate(R.menu.shortcuts_menu, menu);
        sortItem = menu.findItem(R.id.sort_shortcuts);
        sortItem.setTitle(sortTypeText[curSortType]);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            setRecyclerLayoutManager(curGridType, curListType);
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            setRecyclerLayoutManager(curGridType, curListType);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) { //WARNING TRASH CODE...
        SharedPreferences.Editor prefEditor = prefs.edit();
        switch (menuItem.getItemId()) { //xd
            case R.id.add_shortcuts -> {
                // Use the ContainerManager to get the list of containers
                ContainerManager containerManager = new ContainerManager(getContext());
                ArrayList<Container> containers = containerManager.getContainers();

                // Show a container selection dialog
                adapter.showContainerSelectionDialog(containers, selectedContainer -> {
                        shortcutContainer = selectedContainer;
                        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                        intent.setType("*/*");
                        intent.addCategory(Intent.CATEGORY_OPENABLE);
                        startActivityForResult(intent, 7777);
                });
                return true;
            }
            case R.id.search_shortcut -> {
                final EditText taskEditText = new EditText(getContext());
                taskEditText.setText(searchText);
                AlertDialog dialog = new AlertDialog.Builder(getContext())
                        .setTitle("Search shortcuts by Name")
                        .setMessage("Type Name of shortcut in field")
                        .setView(taskEditText)
                        .setPositiveButton("SEARCH", (dialog1, which) -> {
                            searchText = String.valueOf(taskEditText.getText());
                            loadShortcutsList(6);
                        })
                        .setNegativeButton("Cancel", null)
                        .create();
                dialog.show();
                taskEditText.setSelection(0);
            }
            case R.id.sort_by_name -> curSortType = 0;
            case R.id.sort_by_con_id -> curSortType = 1;
            case R.id.sort_by_path -> curSortType = 2;
            case R.id.sort_by_playtime -> curSortType = 3;
            case R.id.sort_by_play_count -> curSortType = 4;
            case R.id.sort_by_play_date -> curSortType = 5;
            case R.id.layout_grid_small -> {
                curGridType = 1;
                curListType = 0;
            }
            case R.id.layout_grid_big -> {
                curGridType = 2;
                curListType = 0;
            }
            case R.id.layout_list_small -> {
                curGridType = 0;
                curListType = 1;
            }
            case R.id.layout_list_big -> {
                curGridType = 0;
                curListType = 2;
            }
            default -> {
                return super.onOptionsItemSelected(menuItem);
            }
        }

        prefEditor.putInt(prefsText[0], curSortType); // lol
        prefEditor.apply();
        loadShortcutsList(curSortType);
        sortItem.setTitle(sortTypeText[curSortType]);

        prefEditor.putInt(prefsText[3], curGridType);
        prefEditor.apply();
        prefEditor.putInt(prefsText[4], curListType);
        prefEditor.apply();
        setRecyclerLayoutManager(curGridType, curListType);
        recyclerView.setAdapter(adapter);
        adapter.setGrid(curGridType > 0);

        return true;
    }

    public void loadShortcutsList(int typeSort) {
        ArrayList<Shortcut> shortcuts = manager.loadShortcuts();
        SharedPreferences playtime_prefs = getContext().getSharedPreferences("playtime_stats", Context.MODE_PRIVATE);

        switch (typeSort) {
            case 0 ->
                shortcuts.sort(Comparator.comparing(s -> s.name));
            case 1 ->
                shortcuts.sort(Comparator.comparing(s -> s.container.id));
            case 2 ->
                shortcuts.sort(Comparator.comparing(s -> s.path));
            case 3 ->
                shortcuts.sort((s1, s2) -> Long.compare(
                        playtime_prefs.getLong(s2.path + "_playtime", 0),
                        playtime_prefs.getLong(s1.path + "_playtime", 0)
                ));
            case 4 ->
                shortcuts.sort((s1, s2) -> Integer.compare(
                        playtime_prefs.getInt(s2.path + "_play_count", 0),
                        playtime_prefs.getInt(s1.path + "_play_count", 0)
                ));
            case 5 ->
                shortcuts.sort((s1, s2) -> Long.compare(
                        playtime_prefs.getLong(s2.path + "_play_date", 0),
                        playtime_prefs.getLong(s1.path + "_play_date", 0)
                ));
            case 6 -> {
                shortcuts.sort((s1, s2) -> {
                    String name1 = s1.name.toLowerCase();
                    String name2 = s2.name.toLowerCase();
                    String search = searchText.toLowerCase();

                    int idx1 = name1.indexOf(search);
                    int idx2 = name2.indexOf(search);

                    if (idx1 == -1 && idx2 != -1) return 1;
                    if (idx1 != -1 && idx2 == -1) return -1;
                    if (idx1 == -1 && idx2 == -1) return name1.compareTo(name2);

                    if (idx1 != idx2) return Integer.compare(idx1, idx2);

                    return name1.compareTo(name2);
                });
            }
        }

        shortcuts.removeIf(shortcut -> shortcut == null || shortcut.file.getName().isEmpty());
        if (adapter == null) {
            adapter = new ShortcutsAdapter(shortcuts, curGridType > 0);
            recyclerView.setAdapter(adapter);
        } else {
            adapter.setGrid(curGridType > 0);
            adapter.setData(shortcuts);
        }
        emptyTextView.setVisibility(shortcuts.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void setRecyclerLayoutManager(int gridType, int listType) {
        int orientation = this.getResources().getConfiguration().orientation;
        switch (gridType) {
            case 1 ->  {
                recyclerView.setLayoutManager(new GridLayoutManager(getContext(), orientation == Configuration.ORIENTATION_PORTRAIT ? 5 : 7));
            } //5 = portrait, 7 landscape
            case 2 -> {
                recyclerView.setLayoutManager(new GridLayoutManager(getContext(), orientation == Configuration.ORIENTATION_PORTRAIT ? 3 : 5));
            } //3 = portrait, 5 landscape
        }

        switch (listType) {
            case 1, 2 -> {
                recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            }
        }
    }

    private void changeListSizes(int curListType, ShortcutsAdapter.ListViewHolder vh) {
        ImageView shortcutIcon = vh.imageView;
        TextView shortcutName = vh.title;
        TextView shortcutPath = vh.path;
        TextView shortcutContainer = vh.subtitle;
        ViewGroup.LayoutParams params = shortcutIcon.getLayoutParams();
        if (curListType == 1) {
            params.width = 128;
            params.height = 128;
            shortcutName.setTextSize(14);
            shortcutPath.setTextSize(12);
            shortcutContainer.setTextSize(12);
        } else {
            params.width = 192;
            params.height = 192;
            shortcutName.setTextSize(22);
            shortcutPath.setTextSize(14);
            shortcutContainer.setTextSize(14);
        }
        shortcutIcon.setLayoutParams(params);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1337 && resultCode == Activity.RESULT_OK && data != null) {
            Uri selectedImageUri = data.getData();

            if (currentDialog != null) {
                currentDialog.onIconSelected(selectedImageUri);
            }
        }

        if (requestCode == 7777 && resultCode == Activity.RESULT_OK && data != null) {
            Uri selectedFile = data.getData();
            String selectedFilePath = selectedFile.getPath().toLowerCase();
            //AppUtils.showToast(getContext(), selectedFilePath);
            if (selectedFilePath.endsWith(".exe") && selectedFilePath.contains("/document/")) {
                if (shortcutContainer != null) {
                    String driveLetter = null;

                    if (selectedFilePath.contains("primary:"))
                        driveLetter = "D:";

                    if (selectedFilePath.contains("/data/user/0/" + PACKAGE_NAME + "/files/imagefs/"))
                        driveLetter = "Z:";

                    if (driveLetter == null) {
                        AppUtils.showToast(getContext(), "Wrong path! Can't detect drive!");
                        return;
                    }

                    String fileName = queryName(getContext().getContentResolver(), selectedFile);
                    String fileNameOutExe = fileName.substring(0, fileName.length() - 4); // -.exe
                    String pathWOutDocument = selectedFilePath;

                    if (pathWOutDocument.startsWith("/document/primary:")) {
                        pathWOutDocument = pathWOutDocument.replaceFirst("/document/primary:", "");
                    } else if (pathWOutDocument.startsWith("/document/")) {
                        pathWOutDocument = pathWOutDocument.replaceFirst("/document/", "");
                    }

                    if (driveLetter.equals("Z:"))
                        pathWOutDocument = pathWOutDocument.replaceFirst("/data/user/0/" + PACKAGE_NAME + "/files/imagefs/", "");

                    if (driveLetter.equals("D:"))
                        pathWOutDocument = pathWOutDocument.replaceFirst("download/", "");

                    String execPath = pathWOutDocument.replace("/", "\\\\\\\\");
                    execPath = execPath.replace(" ", "\\\\ ");
                    String shortcutDesktop =
                                    "[Desktop Entry]\n" +
                                    "Name=" + fileNameOutExe + "\n" +
                                    "Exec=env WINEPREFIX=\"/data/user/0/" + PACKAGE_NAME + "/files/imagefs/home/xuser/.wine/dosdevices/z:/home/xuser/.wine\" wine " + driveLetter + "\\\\\\\\" + execPath /*+ fileName*/ + "\n" +
                                    "Type=Application\n" +
                                    "StartupNotify=true\n" +
                                    "Path=/data/user/0/" + PACKAGE_NAME + "/files/imagefs/home/xuser/.wine/dosdevices/" + driveLetter.toLowerCase()  + "/" + pathWOutDocument.replaceFirst(fileName.toLowerCase(), "") + "\n" +
                                    "Icon=MAKE_BIONIC_GREAT_AGAIN\n" +
                                    "StartupWMClass=" + fileName;

                    File desktopFile = new File(shortcutContainer.getDesktopDir(), fileNameOutExe + ".desktop");

                    //AppUtils.showToast(getContext(), "Path: " + driveLetter + selectedFilePath.substring(0, pathWOutDocument.length() - fileName.length()) + "\n"
                    //        + "Exec: " + driveLetter + execPath + fileName);

                    try (FileWriter writer = new FileWriter(desktopFile)) {
                        writer.write(shortcutDesktop);
                    } catch (IOException e) {
                        Log.e("ShortcutsFragment", e.toString());
                        AppUtils.showToast(getContext(), "Error occured while adding shortcut!");
                        return;
                    }

                    loadShortcutsList(curSortType);
                    AppUtils.showToast(getContext(), "Shortcut created for Container: " + shortcutContainer.name);
                }
            } else {
                AppUtils.showToast(getContext(), "Wrong file type! U need choose .exe file!");
            }
        }
    }

    private String queryName(ContentResolver resolver, Uri uri) { //https://stackoverflow.com/questions/5568874/how-to-extract-the-file-name-from-uri-returned-from-intent-action-get-content
        String[] projection = new String[] { OpenableColumns.DISPLAY_NAME };
        Cursor returnCursor =
                resolver.query(uri, projection, null, null, null);
        assert returnCursor != null;
        returnCursor.moveToFirst();
        String name = returnCursor.getString(0);
        returnCursor.close();
        return name;
    }

    private class ShortcutsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private final List<Shortcut> data;
        private boolean isGrid;

        public ShortcutsAdapter(List<Shortcut> data, boolean isGrid) {
            this.data = new ArrayList<>(data);
            this.isGrid = isGrid;
        }

        public void setData(List<Shortcut> newData) {
            data.clear();
            data.addAll(newData);
            notifyDataSetChanged();
        }

        public void setGrid(boolean isGrid) {
            this.isGrid = isGrid;
            notifyDataSetChanged();
        }

        @Override
        public int getItemViewType(int position) {
            return isGrid ? 1 : 0;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == 1) { // GRID
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.shortcut_grid_item, parent, false);
                return new GridViewHolder(view);
            } else { // LIST
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.shortcut_list_item, parent, false);
                return new ListViewHolder(view);
            }
        }

        @Override
        public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
            if (holder instanceof ListViewHolder vh) {
                vh.menuButton.setOnClickListener(null);
                vh.innerArea.setOnClickListener(null);
            } else if (holder instanceof GridViewHolder vh) {
                vh.itemView.setOnClickListener(null);
                vh.itemView.setOnLongClickListener(null);
            }
            super.onViewRecycled(holder);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            final Shortcut item = data.get(position);

            if (holder instanceof ListViewHolder vh) {
                if (item.icon != null) vh.imageView.setImageBitmap(item.icon);
                vh.title.setText(item.name);
                vh.path.setText(item.path);
                vh.subtitle.setText(item.container.getName());
                vh.menuButton.setOnClickListener(v -> showListItemMenu(v, item));
                vh.innerArea.setOnClickListener(v -> runFromShortcut(item));
                changeListSizes(curListType, vh);
            } else if (holder instanceof GridViewHolder vh) {
                if (item.icon != null) vh.imageView.setImageBitmap(item.icon);
                vh.title.setText(item.name);
                vh.container_name.setText(item.container.name);
                vh.itemView.setOnClickListener(v -> runFromShortcut(item));
                vh.itemView.setOnLongClickListener(v -> {
                    showListItemMenu(v, item);
                    return true;
                });
            }
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        private static class ListViewHolder extends RecyclerView.ViewHolder {
            private final ImageButton menuButton;
            private final ImageView imageView;
            private final TextView title;
            private final TextView path;
            private final TextView subtitle;
            private final View innerArea;

            private ListViewHolder(View view) {
                super(view);
                this.imageView = view.findViewById(R.id.ImageView);
                this.title = view.findViewById(R.id.TVTitle);
                this.path = view.findViewById(R.id.TVPath);
                this.subtitle = view.findViewById(R.id.TVSubtitle);
                this.menuButton = view.findViewById(R.id.BTMenu);
                this.innerArea = view.findViewById(R.id.LLInnerArea);
            }
        }

        private static class GridViewHolder extends RecyclerView.ViewHolder {
            private final ImageView imageView;
            private final TextView title;
            private final TextView container_name;

            private GridViewHolder(View view) {
                super(view);
                this.imageView = view.findViewById(R.id.ImageView);
                this.title = view.findViewById(R.id.TVTitle);
                this.container_name = view.findViewById(R.id.TVSubtitle);
            }
        }

        private void showListItemMenu(View anchorView, final Shortcut shortcut) {
            final Context context = getContext();
            PopupMenu listItemMenu = new PopupMenu(context, anchorView);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) listItemMenu.setForceShowIcon(true);
            listItemMenu.setGravity(Gravity.CENTER | Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
            listItemMenu.inflate(R.menu.shortcut_popup_menu);
            listItemMenu.setOnMenuItemClickListener((menuItem) -> {
                int itemId = menuItem.getItemId();
                switch (itemId) {
                    case R.id.shortcut_settings -> {
                        currentDialog = new ShortcutSettingsDialog(ShortcutsFragment.this, shortcut);
                        currentDialog.show();
                    }
                    case R.id.shortcut_launch_container -> {
                        Activity activity = getActivity();
                        if (!XrActivity.isEnabled(getContext())) {
                            Intent intent = new Intent(activity, XServerDisplayActivity.class);
                            intent.putExtra("container_id", shortcut.container.id);
                            requireActivity().startActivity(intent);
                        } else XrActivity.openIntent(getActivity(), shortcut.container.id, null);
                    }
                    case R.id.shortcut_remove ->
                            ContentDialog.confirm(context, R.string.do_you_want_to_remove_this_shortcut, () -> {
                                if (shortcut.file.delete()) {
                                    disableShortcutOnScreen(requireContext(), shortcut);
                                    loadShortcutsList(curSortType);
                                    AppUtils.showToast(context, "Shortcut removed successfully.");
                                } else {
                                    AppUtils.showToast(context, "Failed to remove the shortcut. Please try again.");
                                }
                            });
                    case R.id.shortcut_clone_to_container -> {
                        // Use the ContainerManager to get the list of containers
                        ContainerManager containerManager = new ContainerManager(context);
                        ArrayList<Container> containers = containerManager.getContainers();

                        // Show a container selection dialog
                        showContainerSelectionDialog(containers, new OnContainerSelectedListener() {
                            @Override
                            public void onContainerSelected(Container selectedContainer) {
                                // Use the selected container to clone the shortcut
                                if (shortcut.cloneToContainer(selectedContainer)) {
                                    AppUtils.showToast(context, "Shortcut cloned successfully.");
                                    loadShortcutsList(curSortType); // Reload the shortcuts to show the cloned one
                                } else {
                                    AppUtils.showToast(context, "Failed to clone shortcut.");
                                }
                            }
                        });
                    }
                    case R.id.shortcut_add_to_home_screen -> {
                        if (shortcut.getExtra("uuid").isEmpty())
                            shortcut.genUUID();
                        addShortcutToScreen(shortcut);
                    }
                    case R.id.shortcut_export_to_frontend -> exportShortcutToFrontend(shortcut);
                    case R.id.shortcut_properties -> showShortcutProperties(shortcut);
                }
                return true;
            });
            listItemMenu.show();
        }

        // Define the listener interface for selecting a container
        public interface OnContainerSelectedListener {
            void onContainerSelected(Container container);
        }

        public void showContainerSelectionDialog(ArrayList<Container> containers, OnContainerSelectedListener listener) {
            // Create an AlertDialog to show the list of containers
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle("Select a container");

            // Create an array of container names to display
            String[] containerNames = new String[containers.size()];
            for (int i = 0; i < containers.size(); i++) {
                containerNames[i] = containers.get(i).getName();
            }

            // Set up the list in the dialog
            builder.setItems(containerNames, (dialog, which) -> {
                // Call the listener when a container is selected
                listener.onContainerSelected(containers.get(which));
            });

            // Show the dialog
            builder.show();
        }

        private void runFromShortcut(Shortcut shortcut) {
            Activity activity = getActivity();
            if (!XrActivity.isEnabled(getContext())) {
                Intent intent = new Intent(activity, XServerDisplayActivity.class);
                intent.putExtra("container_id", shortcut.container.id);
                intent.putExtra("shortcut_path", shortcut.file.getPath());
                intent.putExtra("shortcut_name", shortcut.name); // Add this line to pass the shortcut name
                // Check if the shortcut has the disableXinput value; if not, default to false.
                String disableXinputValue = shortcut.getExtra("disableXinput", "0"); // Get value from shortcut or use "0" (false) by default
                intent.putExtra("disableXinput", disableXinputValue); // Use the actual value from the shortcut
                activity.startActivity(intent);
            } else XrActivity.openIntent(activity, shortcut.container.id, shortcut.file.getPath());
        }

        private void exportShortcutToFrontend(Shortcut shortcut) {
            // Check for a custom frontend export path in shared preferences
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
            String uriString = sharedPreferences.getString("frontend_export_uri", null);

            File frontendDir;

            if (uriString != null) {
                // If custom URI is set, use it
                Uri folderUri = Uri.parse(uriString);
                DocumentFile pickedDir = DocumentFile.fromTreeUri(getContext(), folderUri);

                if (pickedDir == null || !pickedDir.canWrite()) {
                    Toast.makeText(getContext(), "Cannot write to the selected folder", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Convert DocumentFile to a File object for further processing
                frontendDir = new File(FileUtils.getFilePathFromUri(getContext(), folderUri));
            } else {
                // Default to Downloads\Winlator\Frontend if no custom URI is set
                frontendDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Winlator/Frontend");
                if (!frontendDir.exists() && !frontendDir.mkdirs()) {
                    Toast.makeText(getContext(), "Failed to create default directory", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            // Check for FRONTEND_INSTRUCTIONS.txt
            File instructionsFile = new File(frontendDir, "FRONTEND_INSTRUCTIONS.txt");
            if (!instructionsFile.exists()) {
                try (FileWriter writer = new FileWriter(instructionsFile, false)) {
                    writer.write("Instructions for adding Winlator shortcuts to Frontends (WIP):\n\n");
                    writer.write("Daijisho:\n\n");
                    writer.write("1. Open Daijisho\n");
                    writer.write("2. Navigate to the Settings tab.\n");
                    writer.write("3. Navigate to Settings\\Library\n");
                    writer.write("4. Select, Import from Pegasus\n");
                    writer.write("5. Add the metadata.pegasus.txt file located in this directory (Downloads\\Winlator\\Frontend)\n");
                    writer.write("6. Set the Sync path to Downloads\\Winlator\\Frontend\n");
                    writer.write("7. Start your game!\n\n");
                    writer.write("Beacon:\n\n");
                    writer.write("1. Navigate to Settings\n");
                    writer.write("2. Click the + Icon\n");
                    writer.write("3. Set the following values:\n\n");
                    writer.write("Platform Type: Custom\n");
                    writer.write("Name: Windows (or Winlator, whatever you prefer)\n");
                    writer.write("Short name: windows\n");
                    writer.write("Player app: Select Winlator.CMOD (or whichever fork you are using that has adopted this code)\n");
                    writer.write("ROMs folder: Use Android FilePicker to select the Downloads\\Winlator\\Frontend directory\n");
                    writer.write("Expand Advanced:\n");
                    writer.write("File handling: Default\n");
                    writer.write("Use custom launch: True\n");
                    writer.write("am start command: am start -n " + getContext().getPackageName() + "/" + PACKAGE_NAME + ".XServerDisplayActivity -e shortcut_path {file_path}\n\n");
                    writer.write("4. Click Save\n");
                    writer.write("5. Scan the folder for your game\n");
                    writer.write("6. Launch your game!\n");
                    writer.flush();
                    Log.d("ShortcutsFragment", "FRONTEND_INSTRUCTIONS.txt created successfully.");
                } catch (IOException e) {
                    Log.e("ShortcutsFragment", "Failed to create FRONTEND_INSTRUCTIONS.txt", e);
                }
            }

            // Check for metadata.pegasus.txt
            File metadataFile = new File(frontendDir, "metadata.pegasus.txt");
            try (FileWriter writer = new FileWriter(metadataFile, false)) {
                writer.write("collection: Windows\n");
                writer.write("shortname: windows\n");
                writer.write("extensions: desktop\n");
                writer.write("launch: am start\n");
                writer.write("  -n " + getContext().getPackageName() + "/.XServerDisplayActivity\n");
                writer.write("  -e shortcut_path {file.path}\n");
                writer.write("  --activity-clear-task\n");
                writer.write("  --activity-clear-top\n");
                writer.write("  --activity-no-history\n");
                writer.flush();
                Log.d("ShortcutsFragment", "metadata.pegasus.txt created or updated successfully.");
            } catch (IOException e) {
                Log.e("ShortcutsFragment", "Failed to create or update metadata.pegasus.txt", e);
            }

            // Create the export file in the Frontend directory
            File exportFile = new File(frontendDir, shortcut.file.getName());

            boolean fileExists = exportFile.exists();
            boolean containerIdFound = false;

            try {
                List<String> lines = new ArrayList<>();

                // Read the original file or existing file if it exists
                try (BufferedReader reader = new BufferedReader(new FileReader(shortcut.file))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("container_id:")) {
                            // Replace the existing container_id line
                            lines.add("container_id:" + shortcut.container.id);
                            containerIdFound = true;
                        } else {
                            lines.add(line);
                        }
                    }
                }

                // If no container_id was found, add it
                if (!containerIdFound) {
                    lines.add("container_id:" + shortcut.container.id);
                }

                // Write the contents to the export file
                try (FileWriter writer = new FileWriter(exportFile, false)) {
                    for (String line : lines) {
                        writer.write(line + "\n");
                    }
                    writer.flush();
                }

                Log.d("ShortcutsFragment", "Shortcut exported successfully to " + exportFile.getPath());

                // Determine the toast message
                String message;
                if (fileExists) {
                    message = "Frontend Shortcut Updated at " + exportFile.getPath();
                } else {
                    message = "Frontend Shortcut Exported to " + exportFile.getPath();
                }

                // Show a toast message to the user
                AppUtils.showToast(getContext(), message);
            } catch (IOException e) {
                Log.e("ShortcutsFragment", "Failed to export shortcut", e);
                AppUtils.showToast(getContext(), "Failed to export shortcut");
            }
        }

        private void showShortcutProperties(Shortcut shortcut) {
            SharedPreferences playtimePrefs = getContext().getSharedPreferences("playtime_stats", Context.MODE_PRIVATE);

            String playtimeKey = shortcut.path + "_playtime";
            String playCountKey = shortcut.path + "_play_count";

            long totalPlaytime = playtimePrefs.getLong(playtimeKey, 0);
            int playCount = playtimePrefs.getInt(playCountKey, 0);

            // Convert playtime to human-readable format
            long seconds = (totalPlaytime / 1000) % 60;
            long minutes = (totalPlaytime / (1000 * 60)) % 60;
            long hours = (totalPlaytime / (1000 * 60 * 60)) % 24;
            long days = (totalPlaytime / (1000 * 60 * 60 * 24));

            String playtimeFormatted = String.format("%dd %02dh %02dm %02ds", days, hours, minutes, seconds);

            // Create the properties dialog
            ContentDialog dialog = new ContentDialog(getContext(), R.layout.shortcut_properties_dialog);
            dialog.setTitle("Properties");

            TextView playCountTextView = dialog.findViewById(R.id.play_count);
            TextView playtimeTextView = dialog.findViewById(R.id.playtime);

            playCountTextView.setText("Number of times played: " + playCount);
            playtimeTextView.setText("Playtime: " + playtimeFormatted);

            Button resetPropertiesButton = dialog.findViewById(R.id.reset_properties);

            resetPropertiesButton.setOnClickListener(v -> {
                playtimePrefs.edit().remove(playtimeKey).remove(playCountKey).apply();
                Toast.makeText(getContext(), "Properties reset successfully.", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });
            dialog.show();
        }
    }

    private ShortcutInfo buildScreenShortCut(String shortLabel, String longLabel, int containerId, String shortcutPath, Icon icon, String uuid) {
        Intent intent = new Intent(getActivity(), XServerDisplayActivity.class);
        intent.setAction(Intent.ACTION_VIEW);
        intent.putExtra("container_id", containerId);
        intent.putExtra("shortcut_path", shortcutPath);

        return new ShortcutInfo.Builder(getActivity(), uuid)
                .setShortLabel(shortLabel)
                .setLongLabel(longLabel)
                .setIcon(icon)
                .setIntent(intent)
                .build();
    }

    private void addShortcutToScreen(Shortcut shortcut) {
        ShortcutManager shortcutManager = getSystemService(requireContext(), ShortcutManager.class);
        if (shortcutManager != null && shortcutManager.isRequestPinShortcutSupported())
            shortcutManager.requestPinShortcut(buildScreenShortCut(shortcut.name, shortcut.name, shortcut.container.id,
                    shortcut.file.getPath(), Icon.createWithBitmap(shortcut.icon), shortcut.getExtra("uuid")), null);
    }

    public static void disableShortcutOnScreen(Context context, Shortcut shortcut) {
        ShortcutManager shortcutManager = getSystemService(context, ShortcutManager.class);
        try {
            shortcutManager.disableShortcuts(Collections.singletonList(shortcut.getExtra("uuid")),
                    context.getString(R.string.shortcut_not_available));
        } catch (Exception e) {
            Log.e("ShortcutsFragment", e.toString());
        }
    }

    public void updateShortcutOnScreen(String shortLabel, String longLabel, int containerId, String shortcutPath, Icon icon, String uuid) {
        ShortcutManager shortcutManager = getSystemService(requireContext(), ShortcutManager.class);
        try {
            for (ShortcutInfo shortcutInfo : shortcutManager.getPinnedShortcuts()) {
                if (shortcutInfo.getId().equals(uuid)) {
                    shortcutManager.updateShortcuts(Collections.singletonList(
                            buildScreenShortCut(shortLabel, longLabel, containerId, shortcutPath, icon, uuid)));
                    break;
                }
            }
        } catch (Exception e) {
            Log.e("ShortcutsFragment", e.toString());
        }
    }
}

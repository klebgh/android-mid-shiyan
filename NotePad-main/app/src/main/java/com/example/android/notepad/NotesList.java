/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.notepad;

import com.example.android.notepad.NotePad;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.ListActivity;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import java.util.Random;


/**
 * Displays a list of notes. Will display notes from the {@link Uri}
 * provided in the incoming Intent if there is one, otherwise it defaults to displaying the
 * contents of the {@link NotePadProvider}.
 *
 * NOTE: Notice that the provider operations in this Activity are taking place on the UI thread.
 * This is not a good practice. It is only done here to make the code more readable. A real
 * application should use the {@link android.content.AsyncQueryHandler} or
 * {@link android.os.AsyncTask} object to perform operations asynchronously on a separate thread.
 */
public class NotesList extends ListActivity {

    // For logging and debugging
    private static final String TAG = "NotesList";

    private static final int SEARCH_REQUEST_CODE = 1;

    private boolean isDarkTheme = true; // 初始为夜间模式

    /**
     * The columns needed by the cursor adapter
     */
    private static final String[] PROJECTION = new String[]{
            NotePad.Notes._ID, // 0
            NotePad.Notes.COLUMN_NAME_TITLE, // 1
            NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE, // 2 - 新增修改时间字段
            NotePad.Notes.COLUMN_NAME_NOTE,

    };

    public static String[] getProjection() {
        return PROJECTION;
    }

    /**
     * The index of the title column
     *
     */
    // 更新列索引常量
    private static final int COLUMN_INDEX_ID = 0;
    private static final int COLUMN_INDEX_TITLE = 1;
    private static final int COLUMN_INDEX_MODIFICATION_DATE = 2; // 新增修改时间列索引

    private static final int COLUMN_INDEX_NOTE = 3;

    /**
     * onCreate is called when Android starts this Activity from scratch.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // The user does not need to hold down the key to use menu shortcuts.
        setDefaultKeyMode(DEFAULT_KEYS_SHORTCUT);


        /* If no data is given in the Intent that started this Activity, then this Activity
         * was started when the intent filter matched a MAIN action. We should use the default
         * provider URI.
         */
        // Gets the intent that started this Activity.
        Intent intent = getIntent();

        // If there is no data associated with the Intent, sets the data to the default URI, which
        // accesses a list of notes.
        if (intent.getData() == null) {
            intent.setData(NotePad.Notes.CONTENT_URI);
        }

        /*
         * Sets the callback for context menu activation for the ListView. The listener is set
         * to be this Activity. The effect is that context menus are enabled for items in the
         * ListView, and the context menu is handled by a method in NotesList.
         */
        getListView().setOnCreateContextMenuListener(this);

        /* Performs a managed query. The Activity handles closing and requerying the cursor
         * when needed.
         *
         * Please see the introductory note about performing provider operations on the UI thread.
         */
        Cursor cursor = managedQuery(
                getIntent().getData(),            // Use the default content URI for the provider.
                PROJECTION,                       // Return the note ID and title for each note.
                null,                             // No where clause, return all records.
                null,                             // No where clause, therefore no where column values.
                NotePad.Notes.DEFAULT_SORT_ORDER  // Use the default sort order.
        );

        /*
         * The following two arrays create a "map" between columns in the cursor and view IDs
         * for items in the ListView. Each element in the dataColumns array represents
         * a column name; each element in the viewID array represents the ID of a View.
         * The SimpleCursorAdapter maps them in ascending order to determine where each column
         * value will appear in the ListView.
         */

        // The names of the cursor columns to display in the view, initialized to the title column
        String[] dataColumns = {
                NotePad.Notes.COLUMN_NAME_TITLE,
                NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE,
                NotePad.Notes.COLUMN_NAME_NOTE,
                NotePad.Notes._ID
        };

        // The view IDs that will display the cursor columns, initialized to the TextView in
        // noteslist_item.xml
        int[] viewIDs = {
                android.R.id.text1,
                R.id.note_modification_date,
                R.id.note_content_preview,
                R.id.category_icon
        };


        // Creates the backing adapter for the ListView.
        SimpleCursorAdapter adapter
                = new SimpleCursorAdapter(
                this,                             // The Context for the ListView
                R.layout.noteslist_item,          // Points to the XML for a list item
                cursor,                           // The cursor to get items from
                dataColumns,
                viewIDs
        );

        // Sets the ListView's adapter to be the cursor adapter that was just created.
        setListAdapter(adapter);

        // 添加 ViewBinder 来格式化时间戳显示
        // 添加 ViewBinder 来格式化时间戳显示
        adapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
            @Override
            public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
                if (columnIndex == COLUMN_INDEX_MODIFICATION_DATE) {
                    long modificationTime = cursor.getLong(columnIndex);
                    TextView dateView = (TextView) view;
                    // 将时间戳格式化为易读的日期格式
                    String formattedDate = formatDateTime(modificationTime);
                    dateView.setText(formattedDate);
                    // 设置文字颜色
                    dateView.setTextColor(ThemeManager.getSecondaryTextColor(NotesList.this));
                    return true;
                }
                // 为标题也设置颜色
                else if (columnIndex == COLUMN_INDEX_TITLE) {
                    TextView titleView = (TextView) view;
                    titleView.setTextColor(ThemeManager.getTextColor(NotesList.this));
                    // 返回 false 让系统继续处理文本内容
                    return false;
                }
                else if(columnIndex == COLUMN_INDEX_NOTE){
                    String content = cursor.getString(columnIndex);
                    TextView previewView = (TextView) view;

                    // 处理内容：如果太长则截断
                    if (content != null && content.length() > 100) {
                        content = content.substring(0, 100) + "...";
                    }

                    // 设置预览文本
                    previewView.setText(content != null ? content : "");

                    // 根据主题设置文字颜色
                    previewView.setTextColor(ThemeManager.getSecondaryTextColor(NotesList.this));
                    return true;
                }
                // 处理分类图标
                else if (view.getId() == R.id.category_icon) {
                    ImageView categoryIcon = (ImageView) view;

                    // 随机选择一个图标
                    int[] icons = {R.drawable.ic_work, R.drawable.ic_study, R.drawable.ic_life, R.drawable.ic_undefined};
                    int randomIcon = icons[new Random().nextInt(icons.length)];

                    categoryIcon.setImageResource(randomIcon);
                    return true;
                }
                return false;
            }
        });

        // 设置自定义 ActionBar
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            // 隐藏默认标题
            actionBar.setDisplayShowTitleEnabled(false);
            // 启用自定义布局
            actionBar.setDisplayShowCustomEnabled(true);
            // 加载自定义布局
            View customView = LayoutInflater.from(this).inflate(R.layout.custom_action_bar, null);
            actionBar.setCustomView(customView);
        }
        applyTheme();
    }
    // 在 NotesList 类中添加
    private int getCategoryIcon(String category) {
        switch (category) {
            case "work":
                return R.drawable.ic_work;
            case "study":
                return R.drawable.ic_study;
            case "life":
                return R.drawable.ic_life;
            default:
                return R.drawable.ic_undefined;
        }
    }


    // 添加日期格式化方法
    private String formatDateTime(long time) {
        // 创建 SimpleDateFormat 实例
        java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm");

        // 设置时区为 UTC+8 (北京时间)
        formatter.setTimeZone(java.util.TimeZone.getTimeZone("GMT+8"));

        // 格式化时间
        return formatter.format(new java.util.Date(time));
    }

    /**
     * Called when the user clicks the device's Menu button the first time for
     * this Activity. Android passes in a Menu object that is populated with items.
     * <p>
     * Sets up a menu that provides the Insert option plus a list of alternative actions for
     * this Activity. Other applications that want to handle notes can "register" themselves in
     * Android by providing an intent filter that includes the category ALTERNATIVE and the
     * mimeTYpe NotePad.Notes.CONTENT_TYPE. If they do this, the code in onCreateOptionsMenu()
     * will add the Activity that contains the intent filter to its list of options. In effect,
     * the menu will offer the user other applications that can handle notes.
     *
     * @param menu A Menu object, to which menu items should be added.
     * @return True, always. The menu should be displayed.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.list_options_menu, menu);

        MenuItem themeItem = menu.findItem(R.id.menu_theme);
        if (isDarkTheme) {
            themeItem.setIcon(R.drawable.ic_menu_night); // 夜间模式图标
        } else {
            themeItem.setIcon(R.drawable.ic_menu_morning); // 白天模式图标
        }

        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        // The paste menu item is enabled if there is data on the clipboard.
        ClipboardManager clipboard = (ClipboardManager)
                getSystemService(Context.CLIPBOARD_SERVICE);


        MenuItem mPasteItem = menu.findItem(R.id.menu_paste);

        // If the clipboard contains an item, enables the Paste option on the menu.
        if (clipboard.hasPrimaryClip()) {
            mPasteItem.setEnabled(true);
        } else {
            // If the clipboard is empty, disables the menu's Paste option.
            mPasteItem.setEnabled(false);
        }

        // Gets the number of notes currently being displayed.
        final boolean haveItems = getListAdapter().getCount() > 0;

        // If there are any notes in the list (which implies that one of
        // them is selected), then we need to generate the actions that
        // can be performed on the current selection.  This will be a combination
        // of our own specific actions along with any extensions that can be
        // found.
        if (haveItems) {

            // This is the selected item.
            Uri uri = ContentUris.withAppendedId(getIntent().getData(), getSelectedItemId());

            // Creates an array of Intents with one element. This will be used to send an Intent
            // based on the selected menu item.
            Intent[] specifics = new Intent[1];

            // Sets the Intent in the array to be an EDIT action on the URI of the selected note.
            specifics[0] = new Intent(Intent.ACTION_EDIT, uri);

            // Creates an array of menu items with one element. This will contain the EDIT option.
            MenuItem[] items = new MenuItem[1];

            // Creates an Intent with no specific action, using the URI of the selected note.
            Intent intent = new Intent(null, uri);

            /* Adds the category ALTERNATIVE to the Intent, with the note ID URI as its
             * data. This prepares the Intent as a place to group alternative options in the
             * menu.
             */
            intent.addCategory(Intent.CATEGORY_ALTERNATIVE);

            /*
             * Add alternatives to the menu
             */
            menu.addIntentOptions(
                    Menu.CATEGORY_ALTERNATIVE,  // Add the Intents as options in the alternatives group.
                    Menu.NONE,                  // A unique item ID is not required.
                    Menu.NONE,                  // The alternatives don't need to be in order.
                    null,                       // The caller's name is not excluded from the group.
                    specifics,                  // These specific options must appear first.
                    intent,                     // These Intent objects map to the options in specifics.
                    Menu.NONE,                  // No flags are required.
                    items                       // The menu items generated from the specifics-to-
                    // Intents mapping
            );
            // If the Edit menu item exists, adds shortcuts for it.
            if (items[0] != null) {

                // Sets the Edit menu item shortcut to numeric "1", letter "e"
                items[0].setShortcut('1', 'e');
            }
        } else {
            // If the list is empty, removes any existing alternative actions from the menu
            menu.removeGroup(Menu.CATEGORY_ALTERNATIVE);
        }

        // Displays the menu
        return true;
    }

    /**
     * This method is called when the user selects an option from the menu, but no item
     * in the list is selected. If the option was INSERT, then a new Intent is sent out with action
     * ACTION_INSERT. The data from the incoming Intent is put into the new Intent. In effect,
     * this triggers the NoteEditor activity in the NotePad application.
     * <p>
     * If the item was not INSERT, then most likely it was an alternative option from another
     * application. The parent method is called to process the item.
     *
     * @param item The menu item that was selected by the user
     * @return True, if the INSERT menu item was selected; otherwise, the result of calling
     * the parent method.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_add) {
            startActivity(new Intent(Intent.ACTION_INSERT, getIntent().getData()));
            return true;
        } else if (item.getItemId() == R.id.menu_paste) {
            startActivity(new Intent(Intent.ACTION_PASTE, getIntent().getData()));
            return true;
        } else if (item.getItemId() == R.id.menu_search) {
            startActivityForResult(new Intent(this, NoteSearch.class), SEARCH_REQUEST_CODE);
            return true;
        } else if (item.getItemId() == R.id.menu_theme) {
            // 切换主题状态
            isDarkTheme = !isDarkTheme;

            // 更新图标
            if (isDarkTheme) {
                item.setIcon(R.drawable.ic_menu_night);
            } else {
                item.setIcon(R.drawable.ic_menu_morning);
            }

            // 应用主题到列表和菜单栏
            applyTheme();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // 添加主题应用方法
    private void applyTheme() {


        // 处理列表内容区域主题（已实现的部分）
        getListView().setBackgroundColor(isDarkTheme ? Color.BLACK : Color.WHITE);

        // 处理ActionBar（菜单栏）主题
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            // 设置ActionBar背景色
            actionBar.setBackgroundDrawable(new ColorDrawable(
                    isDarkTheme ? Color.DKGRAY : Color.LTGRAY));

            // 设置ActionBar标题颜色
            TextView titleView = (TextView) findViewById(R.id.action_bar_title);
            if (titleView != null) {
                titleView.setTextColor(isDarkTheme ? Color.WHITE : Color.BLACK);
            }
        }
        ThemeManager.saveThemeMode(this, isDarkTheme);
        // 通知列表刷新以应用新主题
        ((SimpleCursorAdapter) getListAdapter()).notifyDataSetChanged();
    }

    private void toggleTheme() {
        isDarkTheme = !isDarkTheme;

        // 更新 UI 主题
        if (isDarkTheme) {
            setDarkTheme();
        } else {
            setLightTheme();
        }

        // 刷新列表适配器以应用新样式
        SimpleCursorAdapter adapter = (SimpleCursorAdapter) getListAdapter();
        adapter.notifyDataSetChanged();
    }

    private void setDarkTheme() {
        getWindow().getDecorView().setBackgroundColor(0xFF000000); // 黑色背景
        ListView listView = getListView();
        listView.setBackgroundColor(0xFF000000);
        listView.setCacheColorHint(0xFF000000);
    }

    private void setLightTheme() {
        getWindow().getDecorView().setBackgroundColor(0xFFFFFFFF); // 白色背景
        ListView listView = getListView();
        listView.setBackgroundColor(0xFFFFFFFF);
        listView.setCacheColorHint(0xFFFFFFFF);
    }


    // 添加处理搜索结果的方法
    // NotesList.java
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SEARCH_REQUEST_CODE && resultCode == RESULT_OK) {
            String searchQuery = data.getStringExtra("search_query");
            int searchMode = data.getIntExtra("search_mode", 0);

            // ✅ 正确刷新列表
            refreshListWithSearch(searchQuery, searchMode);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    // 根据搜索条件刷新列表
    private void refreshListWithSearch(String query, int mode) {
        String selection = null;
        String[] selectionArgs = null;

        if (mode == 0) {
            selection = NotePad.Notes.COLUMN_NAME_TITLE + " LIKE ?";
        } else {
            selection = NotePad.Notes.COLUMN_NAME_NOTE + " LIKE ?";
        }

        selectionArgs = new String[]{"%" + query + "%"};

        Cursor cursor = getContentResolver().query(
                getIntent().getData(),
                PROJECTION,
                selection,
                selectionArgs,
                NotePad.Notes.DEFAULT_SORT_ORDER
        );

        ((SimpleCursorAdapter) getListAdapter()).changeCursor(cursor);
    }


    /**
     * This method is called when the user context-clicks a note in the list. NotesList registers
     * itself as the handler for context menus in its ListView (this is done in onCreate()).
     * <p>
     * The only available options are COPY and DELETE.
     * <p>
     * Context-click is equivalent to long-press.
     *
     * @param menu     A ContexMenu object to which items should be added.
     * @param view     The View for which the context menu is being constructed.
     * @param menuInfo Data associated with view.
     * @throws ClassCastException
     */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {

        // The data from the menu item.
        AdapterView.AdapterContextMenuInfo info;

        // Tries to get the position of the item in the ListView that was long-pressed.
        try {
            // Casts the incoming data object into the type for AdapterView objects.
            info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        } catch (ClassCastException e) {
            // If the menu object can't be cast, logs an error.
            Log.e(TAG, "bad menuInfo", e);
            return;
        }

        /*
         * Gets the data associated with the item at the selected position. getItem() returns
         * whatever the backing adapter of the ListView has associated with the item. In NotesList,
         * the adapter associated all of the data for a note with its list item. As a result,
         * getItem() returns that data as a Cursor.
         */
        Cursor cursor = (Cursor) getListAdapter().getItem(info.position);

        // If the cursor is empty, then for some reason the adapter can't get the data from the
        // provider, so returns null to the caller.
        if (cursor == null) {
            // For some reason the requested item isn't available, do nothing
            return;
        }

        // Inflate menu from XML resource
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.list_context_menu, menu);

        // Sets the menu header to be the title of the selected note.
        menu.setHeaderTitle(cursor.getString(COLUMN_INDEX_TITLE));

        // Append to the
        // menu items for any other activities that can do stuff with it
        // as well.  This does a query on the system for any activities that
        // implement the ALTERNATIVE_ACTION for our data, adding a menu item
        // for each one that is found.
        Intent intent = new Intent(null, Uri.withAppendedPath(getIntent().getData(),
                Integer.toString((int) info.id)));
        intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        menu.addIntentOptions(Menu.CATEGORY_ALTERNATIVE, 0, 0,
                new ComponentName(this, NotesList.class), null, intent, 0, null);
    }

    /**
     * This method is called when the user selects an item from the context menu
     * (see onCreateContextMenu()). The only menu items that are actually handled are DELETE and
     * COPY. Anything else is an alternative option, for which default handling should be done.
     *
     * @param item The selected menu item
     * @return True if the menu item was DELETE, and no default processing is need, otherwise false,
     * which triggers the default handling of the item.
     * @throws ClassCastException
     */
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        // The data from the menu item.
        AdapterView.AdapterContextMenuInfo info;

        /*
         * Gets the extra info from the menu item. When an note in the Notes list is long-pressed, a
         * context menu appears. The menu items for the menu automatically get the data
         * associated with the note that was long-pressed. The data comes from the provider that
         * backs the list.
         *
         * The note's data is passed to the context menu creation routine in a ContextMenuInfo
         * object.
         *
         * When one of the context menu items is clicked, the same data is passed, along with the
         * note ID, to onContextItemSelected() via the item parameter.
         */
        try {
            // Casts the data object in the item into the type for AdapterView objects.
            info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        } catch (ClassCastException e) {

            // If the object can't be cast, logs an error
            Log.e(TAG, "bad menuInfo", e);

            // Triggers default processing of the menu item.
            return false;
        }
        // Appends the selected note's ID to the URI sent with the incoming Intent.
        Uri noteUri = ContentUris.withAppendedId(getIntent().getData(), info.id);

        /*
         * Gets the menu item's ID and compares it to known actions.
         */
        int id = item.getItemId();
        if (id == R.id.context_open) {
            // Launch activity to view/edit the currently selected item
            startActivity(new Intent(Intent.ACTION_EDIT, noteUri));
            return true;
        } else if (id == R.id.context_copy) { //BEGIN_INCLUDE(copy)
            // Gets a handle to the clipboard service.
            ClipboardManager clipboard = (ClipboardManager)
                    getSystemService(Context.CLIPBOARD_SERVICE);

            // Copies the notes URI to the clipboard. In effect, this copies the note itself
            clipboard.setPrimaryClip(ClipData.newUri(   // new clipboard item holding a URI
                    getContentResolver(),               // resolver to retrieve URI info
                    "Note",                             // label for the clip
                    noteUri));                          // the URI

            // Returns to the caller and skips further processing.
            return true;
            //END_INCLUDE(copy)
        } else if (id == R.id.context_delete) {
            // Deletes the note from the provider by passing in a URI in note ID format.
            // Please see the introductory note about performing provider operations on the
            // UI thread.
            getContentResolver().delete(
                    noteUri,  // The URI of the provider
                    null,     // No where clause is needed, since only a single note ID is being
                    // passed in.
                    null      // No where clause is used, so no where arguments are needed.
            );

            // Returns to the caller and skips further processing.
            return true;
        }
        return super.onContextItemSelected(item);
    }

    /**
     * This method is called when the user clicks a note in the displayed list.
     * <p>
     * This method handles incoming actions of either PICK (get data from the provider) or
     * GET_CONTENT (get or create data). If the incoming action is EDIT, this method sends a
     * new Intent to start NoteEditor.
     *
     * @param l        The ListView that contains the clicked item
     * @param v        The View of the individual item
     * @param position The position of v in the displayed list
     * @param id       The row ID of the clicked item
     */
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {

        // Constructs a new URI from the incoming URI and the row ID
        Uri uri = ContentUris.withAppendedId(getIntent().getData(), id);

        // Gets the action from the incoming Intent
        String action = getIntent().getAction();

        // Handles requests for note data
        if (Intent.ACTION_PICK.equals(action) || Intent.ACTION_GET_CONTENT.equals(action)) {

            // Sets the result to return to the component that called this Activity. The
            // result contains the new URI
            setResult(RESULT_OK, new Intent().setData(uri));
        } else {

            // Sends out an Intent to start an Activity that can handle ACTION_EDIT. The
            // Intent's data is the note ID URI. The effect is to call NoteEdit.
            startActivity(new Intent(Intent.ACTION_EDIT, uri));
        }
    }
}
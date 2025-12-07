package com.example.android.notepad;

import android.app.ActionBar;
import android.app.ListActivity;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Arrays;

// SearchResultsActivity.java
public class SearchResultsActivity extends ListActivity {
    private Cursor mCursor;
    private SimpleCursorAdapter mAdapter;

    private static final int COLUMN_INDEX_TITLE = 1;
    private static final int COLUMN_INDEX_MODIFICATION_DATE = 2;
    private static final int COLUMN_INDEX_NOTE = 3;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search_results_layout);



        String query = getIntent().getStringExtra("search_query");
        int searchMode = getIntent().getIntExtra("search_mode", 0);

        if (query == null || query.trim().isEmpty()) {
            Toast.makeText(this, "请输入搜索内容", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String selection = null;
        String[] selectionArgs = null;

        if (searchMode == 0) { // 按标题搜索
            selection = NotePad.Notes.COLUMN_NAME_TITLE + " LIKE ?";
        } else { // 按内容搜索
            selection = NotePad.Notes.COLUMN_NAME_NOTE + " LIKE ?";
        }

        selectionArgs = new String[]{"%" + query.trim() + "%"};

        String[] projection = NotesList.getProjection();


        mCursor = getContentResolver().query(
                NotePad.Notes.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                NotePad.Notes.DEFAULT_SORT_ORDER
        );

        if (mCursor == null || mCursor.getCount() == 0) {
            Log.d("SearchResultsActivity", "No results found");
            Toast.makeText(this, "未找到相关笔记", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 设置适配器 - 修改这里以包含内容预览字段
        String[] dataColumns = {
                NotePad.Notes.COLUMN_NAME_TITLE,
                NotePad.Notes.COLUMN_NAME_NOTE,        // 添加内容字段
                NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE
        };

        int[] viewIDs = {
                android.R.id.text1,
                R.id.note_content_preview,            // 内容预览
                R.id.note_modification_date           // 修改时间
        };

        mAdapter = new SimpleCursorAdapter(
                this,
                R.layout.noteslist_item,              // 使用相同的布局
                mCursor,
                dataColumns,
                viewIDs
        );

        // 添加 ViewBinder 来处理内容预览
        mAdapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
            @Override
            public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
                // 处理修改时间
                if (columnIndex == COLUMN_INDEX_MODIFICATION_DATE) {
                    long modificationTime = cursor.getLong(columnIndex);
                    TextView dateView = (TextView) view;
                    String formattedDate = formatDateTime(modificationTime);
                    dateView.setText(formattedDate);
                    dateView.setTextColor(ThemeManager.getSecondaryTextColor(SearchResultsActivity.this));
                    return true;
                }
                // 处理标题
                else if (columnIndex == COLUMN_INDEX_TITLE) {
                    TextView titleView = (TextView) view;
                    titleView.setTextColor(ThemeManager.getTextColor(SearchResultsActivity.this));
                    return false; // 返回 false 让系统继续处理文本内容
                }
                // 处理内容预览
                else if (columnIndex == COLUMN_INDEX_NOTE) {
                    String content = cursor.getString(columnIndex);
                    TextView previewView = (TextView) view;

                    // 处理内容：如果太长则截断
                    if (content != null && content.length() > 100) {
                        content = content.substring(0, 100) + "...";
                    }

                    // 设置预览文本
                    previewView.setText(content != null ? content : "");

                    // 根据主题设置文字颜色
                    previewView.setTextColor(ThemeManager.getSecondaryTextColor(SearchResultsActivity.this));
                    return true;
                }
                return false;
            }
        });

        setListAdapter(mAdapter);
        // 应用主题
        applyTheme();
    }


    private void applyTheme() {
        // 应用 Activity 背景
        ThemeManager.applyActivityTheme(this, getWindow().getDecorView());

        // 应用 ListView 背景
        ThemeManager.applyListViewTheme(this, getListView());

        // 应用 ActionBar 主题（如果有）
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            ThemeManager.applyActionBarTheme(this, actionBar);
        }

        // 刷新适配器以更新文字颜色
        if (mAdapter != null) {
            mAdapter.notifyDataSetChanged();
        }
    }
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        // 跳转到编辑界面
        Uri noteUri = ContentUris.withAppendedId(NotePad.Notes.CONTENT_URI, id);
        Intent intent = new Intent(Intent.ACTION_EDIT, noteUri);
        startActivity(intent);
    }

    private String formatDateTime(long time) {
        java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm");
        formatter.setTimeZone(java.util.TimeZone.getTimeZone("GMT+8"));
        return formatter.format(new java.util.Date(time));
    }


    @Override
    protected void onDestroy() {
        if (mCursor != null && !mCursor.isClosed()) {
            mCursor.close();
        }
        super.onDestroy();
    }

}


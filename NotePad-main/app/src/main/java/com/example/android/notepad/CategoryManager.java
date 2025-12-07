package com.example.android.notepad;

import android.content.ContentUris;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

public class CategoryManager {
    private static final String TAG = "CategoryManager";
    private static final String PREFS_NAME = "note_categories";
    private static final String KEY_CATEGORY_PREFIX = "category_";

    private static Map<String, String> categoryCache = new HashMap<>();

    // 保存笔记的分类
    public static void saveNoteCategory(Context context, long noteId, String category) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String key = KEY_CATEGORY_PREFIX + noteId;

        prefs.edit().putString(key, category).apply();
        categoryCache.put(key, category);

        Log.d(TAG, "Saved category " + category + " for note " + noteId);
    }

    // 获取笔记的分类
    public static String getNoteCategory(Context context, long noteId) {
        String key = KEY_CATEGORY_PREFIX + noteId;

        // 先从缓存中查找
        if (categoryCache.containsKey(key)) {
            return categoryCache.get(key);
        }

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String category = prefs.getString(key, CategoryConstants.CATEGORY_UNDEFINED);

        // 放入缓存
        categoryCache.put(key, category);

        return category;
    }

    // 获取笔记的分类（通过Uri）
    public static String getNoteCategory(Context context, Uri noteUri) {
        try {
            long noteId = ContentUris.parseId(noteUri);
            return getNoteCategory(context, noteId);
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse note ID from URI: " + noteUri, e);
            return CategoryConstants.CATEGORY_UNDEFINED;
        }
    }

    // 删除笔记的分类信息
    public static void deleteNoteCategory(Context context, long noteId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String key = KEY_CATEGORY_PREFIX + noteId;

        prefs.edit().remove(key).apply();
        categoryCache.remove(key);

        Log.d(TAG, "Deleted category for note " + noteId);
    }

    // 清空所有分类缓存
    public static void clearCache() {
        categoryCache.clear();
    }
}
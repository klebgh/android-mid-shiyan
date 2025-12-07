// ThemeManager.java
package com.example.android.notepad;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.app.ActionBar;
import android.graphics.drawable.ColorDrawable;

public class ThemeManager {
    private static final String PREF_NAME = "theme_pref";
    private static final String KEY_THEME_MODE = "is_dark_theme";

    // 保存主题状态
    public static void saveThemeMode(Context context, boolean isDarkTheme) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_THEME_MODE, isDarkTheme).apply();
    }

    // 获取主题状态
    public static boolean isDarkTheme(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_THEME_MODE, true); // 默认夜间模式
    }

    // 应用主题到 Activity 背景
    public static void applyActivityTheme(Context context, View rootView) {
        boolean isDarkTheme = isDarkTheme(context);
        if (rootView != null) {
            rootView.setBackgroundColor(isDarkTheme ? Color.BLACK : Color.WHITE);
        }
    }

    // 应用主题到 ListView
    public static void applyListViewTheme(Context context, ListView listView) {
        boolean isDarkTheme = isDarkTheme(context);
        if (listView != null) {
            listView.setBackgroundColor(isDarkTheme ? Color.BLACK : Color.WHITE);
        }
    }

    // 应用主题到 EditText
    public static void applyEditTextTheme(Context context, android.widget.EditText editText) {
        boolean isDarkTheme = isDarkTheme(context);
        if (editText != null) {
            editText.setBackgroundColor(isDarkTheme ? Color.DKGRAY : Color.WHITE);
            editText.setTextColor(isDarkTheme ? Color.WHITE : Color.BLACK);
            editText.setHintTextColor(isDarkTheme ? Color.LTGRAY : Color.DKGRAY);
        }
    }

    // 应用主题到 Button
    public static void applyButtonTheme(Context context, android.widget.Button button) {
        boolean isDarkTheme = isDarkTheme(context);
        if (button != null) {
            button.setTextColor(isDarkTheme ? Color.WHITE : Color.BLACK);
            // 可以根据需要设置按钮背景
        }
    }

    // 应用主题到 ActionBar
    public static void applyActionBarTheme(Context context, ActionBar actionBar) {
        boolean isDarkTheme = isDarkTheme(context);
        if (actionBar != null) {
            actionBar.setBackgroundDrawable(new ColorDrawable(
                    isDarkTheme ? Color.DKGRAY : Color.LTGRAY));

            // 如果使用自定义 ActionBar 布局
            View customView = actionBar.getCustomView();
            if (customView != null) {
                TextView titleView = customView.findViewById(R.id.action_bar_title);
                if (titleView != null) {
                    titleView.setTextColor(isDarkTheme ? Color.WHITE : Color.BLACK);
                }
            }
        }
    }

    // 应用主题到 LinearLayout
    public static void applyLinearLayoutTheme(Context context, LinearLayout layout) {
        boolean isDarkTheme = isDarkTheme(context);
        if (layout != null) {
            layout.setBackgroundColor(isDarkTheme ? Color.BLACK : Color.WHITE);
        }
    }
    // ThemeManager.java - 添加新的方法
    public static void applyTextViewTheme(Context context, TextView textView) {
        boolean isDarkTheme = isDarkTheme(context);
        if (textView != null) {
            textView.setTextColor(isDarkTheme ? Color.WHITE : Color.BLACK);
        }
    }

    public static int getTextColor(Context context) {
        return isDarkTheme(context) ? Color.WHITE : Color.BLACK;
    }

    public static int getSecondaryTextColor(Context context) {
        return isDarkTheme(context) ? Color.LTGRAY : Color.DKGRAY;
    }

    // 在 ThemeManager.java 中添加
    public static int getAccentColor(Context context) {
        // 返回主题强调色，这里使用系统默认的 accent color
        // 可以根据需要自定义颜色
        return isDarkTheme(context) ? Color.parseColor("#BB86FC") : Color.parseColor("#6200EE");
    }

    public static void applySearchBarTheme(Context context, View searchBar) {
        boolean isDarkTheme = isDarkTheme(context);
        if (searchBar != null) {
            searchBar.setBackgroundColor(isDarkTheme ? Color.DKGRAY : Color.WHITE);
        }
    }


}

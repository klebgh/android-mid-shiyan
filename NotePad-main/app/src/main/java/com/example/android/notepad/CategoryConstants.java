package com.example.android.notepad;

public class CategoryConstants {
    public static final String CATEGORY_WORK = "work";
    public static final String CATEGORY_STUDY = "study";
    public static final String CATEGORY_LIFE = "life";
    public static final String CATEGORY_UNDEFINED = "undefined";

    public static final String[] ALL_CATEGORIES = {
            CATEGORY_WORK,
            CATEGORY_STUDY,
            CATEGORY_LIFE,
            CATEGORY_UNDEFINED
    };

    public static final String[] DISPLAY_CATEGORIES = {
            "工作", "学习", "生活", "未定义"
    };

    public static int getCategoryIcon(String category) {
        switch (category) {
            case CATEGORY_WORK:
                return R.drawable.ic_work;
            case CATEGORY_STUDY:
                return R.drawable.ic_study;
            case CATEGORY_LIFE:
                return R.drawable.ic_life;
            default:
                return R.drawable.ic_undefined;
        }
    }

    public static String getDisplayName(String category) {
        switch (category) {
            case CATEGORY_WORK:
                return "工作";
            case CATEGORY_STUDY:
                return "学习";
            case CATEGORY_LIFE:
                return "生活";
            default:
                return "未定义";
        }
    }
}
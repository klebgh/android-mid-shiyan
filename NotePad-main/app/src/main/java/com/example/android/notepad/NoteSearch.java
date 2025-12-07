// NoteSearch.java
package com.example.android.notepad;

import android.Manifest;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class NoteSearch extends Activity {
    private static final int REQUEST_AUDIO_PERMISSION = 100; // 添加请求码
    private EditText mSearchEditText;
    private ImageView mClearIcon;
    private RadioGroup mSearchModeGroup;
    private TextView mHintText;
    private static final int SEARCH_BY_TITLE = 0;
    private static final int SEARCH_BY_CONTENT = 1;
    private int mCurrentSearchMode = SEARCH_BY_TITLE;
    private VoiceSearchHelper voiceSearchHelper;
    private ImageView voiceBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.note_search);
        voiceBtn = (ImageView) findViewById(R.id.voice_btn);

        // 语音按钮点击事件
        voiceBtn.setOnClickListener(v -> {
            // 如果是模拟器，显示测试对话框
            if (isRunningOnEmulator()) {
                showEmulatorInputDialog();
            } else {
                startVoiceSearch();
            }
        });


        // 初始化语音搜索助手
        voiceSearchHelper = new VoiceSearchHelper(this, new VoiceSearchHelper.OnVoiceResultListener() {
            @Override
            public void onVoiceResult(String result) {
                runOnUiThread(() -> {
                    mSearchEditText.setText(result);
                    // 可选：自动执行搜索
                    // performSearch();
                });
            }

            @Override
            public void onVoiceError(int errorCode, String errorMessage) {

            }

            @Override
            public void onVoiceError(int errorCode) {
                // 处理错误
            }
        });
        // 初始化视图
        mSearchEditText = (EditText) findViewById(R.id.edit_search);
        mClearIcon = (ImageView) findViewById(R.id.clear_btn);
        mSearchModeGroup = (RadioGroup) findViewById(R.id.search_mode_group);
        mHintText = (TextView) findViewById(R.id.tv_hint);
        Button btnSearch = (Button) findViewById(R.id.btn_search);

        // 设置初始提示
        mSearchEditText.setHint("请输入标题关键词");

        // 清除按钮点击事件
        mClearIcon.setOnClickListener(v -> {
            mSearchEditText.setText("");
            mHintText.setVisibility(View.GONE);
        });

        // 搜索框文本变化监听
        mSearchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // 显示/隐藏清除按钮
                mClearIcon.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);

                // 显示提示信息
                mHintText.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // 搜索按钮点击事件
        btnSearch.setOnClickListener(v -> {
            performSearch();
        });

        // 搜索模式切换监听
        mSearchModeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radio_title) {
                mCurrentSearchMode = SEARCH_BY_TITLE;
                mSearchEditText.setHint("请输入标题关键词");
            } else if (checkedId == R.id.radio_content) {
                mCurrentSearchMode = SEARCH_BY_CONTENT;
                mSearchEditText.setHint("请输入内容关键词");
            }
        });

        // 键盘搜索按钮监听
        mSearchEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                performSearch();
                return true;
            }
            return false;
        });

        // 设置焦点并弹出键盘
        mSearchEditText.requestFocus();

        // 应用主题
        applyTheme();
    }

    private boolean isRunningOnEmulator() {
        return Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for")
                || Build.MANUFACTURER.contains("Genymotion");
    }

    private void showEmulatorInputDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("模拟器语音输入测试");
        builder.setMessage("模拟器语音识别可能无法工作，请输入测试文本：");

        final EditText input = new EditText(this);
        input.setHint("输入测试文本");
        builder.setView(input);

        builder.setPositiveButton("确定", (dialog, which) -> {
            String text = input.getText().toString();
            if (!text.isEmpty()) {
                mSearchEditText.setText(text);
            }
        });

        builder.setNegativeButton("取消", null);
        builder.show();
    }
    private void startVoiceSearch() {
        // 检查权限
        if (checkAudioPermission()) {
            voiceSearchHelper.startListening();
        } else {
            requestAudioPermission();
        }
    }

    private boolean checkAudioPermission() {
        return ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestAudioPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.RECORD_AUDIO},
                REQUEST_AUDIO_PERMISSION);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_AUDIO_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                voiceSearchHelper.startListening();
            } else {
                Toast.makeText(this, "需要录音权限才能使用语音搜索", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (voiceSearchHelper != null) {
            voiceSearchHelper.destroy();
        }
    }

    // 搜索方法
    private void performSearch() {
        String query = mSearchEditText.getText().toString().trim();
        if (!query.isEmpty()) {
            Intent intent = new Intent(this, SearchResultsActivity.class);
            intent.putExtra("search_query", query);
            intent.putExtra("search_mode", mCurrentSearchMode);
            startActivity(intent);
            // finish(); // 可选：是否关闭搜索页面
        } else {
            Toast.makeText(this, "请输入搜索内容", Toast.LENGTH_SHORT).show();
        }
    }

    // 应用主题
    private void applyTheme() {
        // 应用 Activity 背景
        ThemeManager.applyActivityTheme(this, getWindow().getDecorView());

        // 应用 ActionBar 主题（如果有）
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            ThemeManager.applyActionBarTheme(this, actionBar);
        }

        // 应用 EditText 主题
        if (mSearchEditText != null) {
            ThemeManager.applyEditTextTheme(this, mSearchEditText);
            // 设置提示文字颜色
            mSearchEditText.setHintTextColor(ThemeManager.getSecondaryTextColor(this));
        }

        // 应用 Button 主题
        Button btnSearch = (Button) findViewById(R.id.btn_search);
        if (btnSearch != null) {
            btnSearch.setTextColor(Color.WHITE);
        }

        // 应用提示文本颜色
        if (mHintText != null) {
            mHintText.setTextColor(ThemeManager.getSecondaryTextColor(this));
        }

        RadioButton radioTitle = (RadioButton) findViewById(R.id.radio_title);
        RadioButton radioContent = (RadioButton) findViewById(R.id.radio_content);
        if (radioTitle != null) {
            radioTitle.setTextColor(ThemeManager.getTextColor(this));
        }
        if (radioContent != null) {
            radioContent.setTextColor(ThemeManager.getTextColor(this));
        }
        // 设置根布局的背景
        View rootView = findViewById(R.id.root_layout);
        if (rootView != null) {
            boolean isDarkTheme = ThemeManager.isDarkTheme(this);
            rootView.setBackgroundColor(isDarkTheme ? Color.BLACK : Color.WHITE);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.search_options_menu, menu);
        return true;
    }
}
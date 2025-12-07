# NotePad
项目来自于官方早期的数据库操作的基本教程。
更多的解析参考[Android Sample--NotePad解析](https://blog.csdn.net/llfjfz/article/details/67638499)

# **基本要求**

## 	NoteList界面中笔记条目增加时间戳显示

1.在noteslist中添加新的投影常量并更新索引常量

```java

private static final String[] PROJECTION = new String[] {
        NotePad.Notes._ID, // 0
        NotePad.Notes.COLUMN_NAME_TITLE, // 1
        NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE // 2 - 新增修改时间字段
};

private static final int COLUMN_INDEX_TITLE = 1;
private static final int COLUMN_INDEX_MODIFICATION_DATE = 2; // 新增修改时间列索引

```

2.在viewIDs和dataColumns添加时间字段

3.在列表项布局中添加日期标签

```java
 <!-- 修改时间 -->
        <TextView android:id="@+id/note_modification_date"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:textAppearance="?android:attr/textAppearanceSmall"
            android:textColor="?android:attr/textColorSecondary"
            android:singleLine="true" />
```

4.添加 ViewBinder 来格式化时间戳显示,添加日期格式化方法

```java
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
            return true;
        }
        return false;
    }
});

```

```java

private String formatDateTime(long time) {
    java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm");

    formatter.setTimeZone(java.util.TimeZone.getTimeZone("GMT+8"));

    return formatter.format(new java.util.Date(time));
}

```

![屏幕截图 2025-12-01 225959](https://github.com/klebgh/android-mid-shiyan/blob/main/NotePad-main/image/%E5%B1%8F%E5%B9%95%E6%88%AA%E5%9B%BE%202025-12-01%20225959.png?raw=true)

## 	添加笔记查询功能（根据标题或内容查询）

1.在list_options_menu添加搜索图标，点击后进入搜索界面

```java
<item android:id="@+id/menu_search"
        android:title="@string/menu_search"
        android:icon="@drawable/ic_menu_search"
        android:showAsAction="always"/>
```

2.创建搜索界面note_search.xml,搜索结果界面search_results_layout.xml



![屏幕截图 2025-12-02 142806](https://github.com/klebgh/android-mid-shiyan/blob/main/NotePad-main/image/%E5%B1%8F%E5%B9%95%E6%88%AA%E5%9B%BE%202025-12-02%20142806.png?raw=true)

这里因为后面在ui优化时优化了搜索界面布局，无法展示初始搜索界面代码

```java
<!-- res/layout/search_results_layout.xml -->
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical">

    <ListView
        android:id="@android:id/list"
        android:layout_width="match_parent"
        android:layout_height="match_parent" />

</LinearLayout>
   
```

3.创建NoteSearch.java,实现搜索界面相关逻辑

在`onCreate`方法中初始化搜索界面布局，包括设置搜索框初始提示文字、为搜索框请求焦点并弹出键盘、应用主题样式；同时设置以下监听事件：

- 搜索文本变化监听：根据输入内容是否为空，动态显示 / 隐藏清除按钮和提示文本；
- 搜索模式（标题 / 内容）切换监听：根据选择更新搜索框提示文字；
- 清除按钮点击监听：点击时清空输入框内容并隐藏提示文本；
- 搜索按钮点击监听及键盘搜索键 / 回车键监听：触发搜索操作。

在用户点击搜索按钮或按下键盘搜索键 / 回车键时，调用`performSearch`方法：

- 校验输入：若关键词为空，提示 “请输入搜索内容”；
- 传递参数：通过`Intent`将关键词（`search_query`）和搜索模式（`search_mode`）传递给`SearchResultsActivity`。

```java
private void performSearch() {
    String query = mSearchEditText.getText().toString().trim();
    if (!query.isEmpty()) {
        Intent intent = new Intent(this, SearchResultsActivity.class);
        intent.putExtra("search_query", query);
        intent.putExtra("search_mode", mCurrentSearchMode);
        startActivity(intent);
    } else {
        Toast.makeText(this, "请输入搜索内容", Toast.LENGTH_SHORT).show();
    }
}
```

4.创建SearchResultsActivity.java,实现搜索结果界面相关逻辑

在onCreate方法中加载搜索结果布局，通过Intent获取搜索参数：

- `search_query`：用户输入的搜索关键词；

- `search_mode`：搜索模式（0 表示按标题搜索，1 表示按内容搜索）。

  ```java
  String query = getIntent().getStringExtra("search_query");
  int searchMode = getIntent().getIntExtra("search_mode", 0);
  ```

  

若关键词为空或空白，提示 “请输入搜索内容” 并关闭当前活动:

```java
if (query == null || query.trim().isEmpty()) {
    Toast.makeText(this, "请输入搜索内容", Toast.LENGTH_SHORT).show();
    finish(); // 关闭活动
    return;
}
```

根据搜索模式（标题 / 内容）构建 `selection`（查询条件）和 `selectionArgs`（参数），使用 `LIKE` 实现模糊搜索:

```java
String selection = null;
if (searchMode == 0) { // 按标题搜索
    selection = NotePad.Notes.COLUMN_NAME_TITLE + " LIKE ?";
} else { // 按内容搜索
    selection = NotePad.Notes.COLUMN_NAME_NOTE + " LIKE ?";
}
// 模糊匹配参数：关键词前后加%
String[] selectionArgs = new String[]{"%" + query.trim() + "%"};
```

通过 `ContentResolver` 调用`NotePadProvider`查询数据，复用 `NotesList` 中的投影

若查询无结果（游标为空或计数为 0），提示 “未找到相关笔记” 并关闭活动：

```java
if (mCursor == null || mCursor.getCount() == 0) {
    Log.d("SearchResultsActivity", "No results found");
    Toast.makeText(this, "未找到相关笔记", Toast.LENGTH_SHORT).show();
    finish();
    return;
}
```

接下来加载列表项布局视图，复用列表项布局

点击某条搜索结果时，通过笔记 ID 构建对应的内容 URI，启动 `NoteEditor` 活动进行编辑：

```java
@Override
protected void onListItemClick(ListView l, View v, int position, long id) {
    // 构建笔记 URI（ID 拼接到底层 URI）
    Uri noteUri = ContentUris.withAppendedId(NotePad.Notes.CONTENT_URI, id);
    Intent intent = new Intent(Intent.ACTION_EDIT, noteUri); // 启动编辑界面
    startActivity(intent);
}
```



# 附加功能

## 	UI美化

一.主题设置

在列表菜单布局中添加主题图标，点击会触发主题切换

主题切换通过`ThemeManager`工具类统一管理，支持动态切换深色 / 浅色主题，并适配所有界面元素。

1. 核心实现：ThemeManager 类

负责主题状态的保存、获取及 UI 元素的主题适配，关键方法如下：

```java
// 保存/获取主题状态（使用SharedPreferences持久化）
public static void saveThemeMode(Context context, boolean isDarkTheme) {
    SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    prefs.edit().putBoolean(KEY_THEME_MODE, isDarkTheme).apply();
}
public static boolean isDarkTheme(Context context) {
    return prefs.getBoolean(KEY_THEME_MODE, true); // 默认深色主题
}

// 主题适配方法（针对不同UI元素）
public static void applyActivityTheme(Context context, View rootView) {
    // Activity背景色：深色为黑色，浅色为白色
    rootView.setBackgroundColor(isDarkTheme(context) ? Color.BLACK : Color.WHITE);
}
public static void applyEditTextTheme(Context context, EditText editText) {
    // 输入框背景、文字色、提示色适配
    editText.setBackgroundColor(isDarkTheme(context) ? Color.DKGRAY : Color.WHITE);
    editText.setTextColor(isDarkTheme(context) ? Color.WHITE : Color.BLACK);
}
public static int getTextColor(Context context) {
    // 主文字色：深色主题白色，浅色主题黑色
    return isDarkTheme(context) ? Color.WHITE : Color.BLACK;
}
public static int getSecondaryTextColor(Context context) {
    // 次要文字色：深色主题浅灰，浅色主题深灰
    return isDarkTheme(context) ? Color.LTGRAY : Color.DKGRAY;
}
```

2. 各界面的主题应用

所有 Activity 通过调用`applyTheme()`方法应用主题，确保界面元素颜色与当前主题一致：

- **NotesList（笔记列表页）**：在`onCreate`和`onResume`中调用，适配列表项文字色、背景色等；
- **NoteSearch（搜索页）**：在`onCreate`中调用，适配搜索框、按钮、提示文字颜色；
- **SearchResultsActivity（搜索结果页）**：在`onCreate`中调用，适配结果列表背景和文字色；
- **NoteEditor（编辑页）**：在`onCreate`和`onResume`中调用，适配标题、内容输入框和分类选择器。

3.效果展示

![屏幕截图 2025-12-06 231420](https://github.com/klebgh/android-mid-shiyan/blob/main/NotePad-main/image/%E5%B1%8F%E5%B9%95%E6%88%AA%E5%9B%BE%202025-12-06%20231420.png?raw=true)

![屏幕截图 2025-12-06 231538](https://github.com/klebgh/android-mid-shiyan/blob/main/NotePad-main/image/%E5%B1%8F%E5%B9%95%E6%88%AA%E5%9B%BE%202025-12-06%20231538.png?raw=true)

二、列表项布局添加内容预览

在笔记列表（NotesList）和搜索结果（SearchResultsActivity）中，列表项新增内容预览区域，优化信息展示。

1. 布局文件：noteslist_item.xml

新增`note_content_preview` TextView 用于展示内容预览，位于标题下方、修改时间上方：

```java
<!-- 内容预览 -->
<TextView android:id="@+id/note_content_preview"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:textAppearance="?android:attr/textAppearanceSmall"
    android:singleLine="true"
    android:maxLines="2"
    android:ellipsize="end"  <!-- 内容过长时显示省略号 -->
    android:textSize="14sp" />
```

2. Java 逻辑适配（NotesList 和 SearchResultsActivity）

通过`SimpleCursorAdapter`的`ViewBinder`处理内容预览的显示逻辑（截断长内容、适配主题颜色）：

**NotesList 中的实现**：

```java
adapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
    @Override
    public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
        // 处理内容预览
        if (columnIndex == COLUMN_INDEX_NOTE) {
            String content = cursor.getString(columnIndex);
            TextView previewView = (TextView) view;
            // 内容过长时截断（保留前100字）
            if (content != null && content.length() > 100) {
                content = content.substring(0, 100) + "...";
            }
            previewView.setText(content != null ? content : "");
            // 适配主题颜色（次要文字色）
            previewView.setTextColor(ThemeManager.getSecondaryTextColor(NotesList.this));
            return true;
        }
        return false;
    }
});
```

SearchResultsActivity 的实现与 NotesList 逻辑一致,这里不做展示

三、搜索界面布局和编辑界面布局优化

1.布局文件：note_search.xml

搜索界面从简单的 “搜索框 + 按钮” 优化为包含搜索区域（带图标）、搜索模式切换、提示信息的完整布局，提升交互体验。

优化后的布局结构如下：

```java
<LinearLayout  <!-- 根布局 -->
    android:id="@+id/root_layout"
    android:orientation="vertical"
    android:padding="16dp">

    <!-- 搜索框区域（带图标和交互按钮） -->
    <RelativeLayout
        android:background="@drawable/search_bar_background">  <!-- 搜索框背景 -->
        <ImageView  <!-- 搜索图标 -->
            android:id="@+id/search_icon"
            android:src="@android:drawable/ic_menu_search"/>
        <ImageView  <!-- 语音搜索按钮 -->
            android:id="@+id/voice_btn"
            android:src="@drawable/ic_mic"/>
        <ImageView  <!-- 清除按钮（输入内容时显示） -->
            android:id="@+id/clear_btn"
            android:src="@android:drawable/ic_menu_close_clear_cancel"
            android:visibility="gone"/>
        <EditText  <!-- 搜索输入框 -->
            android:id="@+id/edit_search"
            android:hint="请输入关键词"
            android:imeOptions="actionSearch"/>  <!-- 键盘搜索键触发 -->
    </RelativeLayout>

    <!-- 搜索模式切换（标题/内容） -->
    <RadioGroup
        android:id="@+id/search_mode_group"
        android:orientation="horizontal">
        <RadioButton android:id="@+id/radio_title" android:text="按标题搜索"/>
        <RadioButton android:id="@+id/radio_content" android:text="按内容搜索"/>
    </RadioGroup>

    <!-- 搜索按钮 -->
    <Button
        android:id="@+id/btn_search"
        android:text="搜索"
        android:backgroundTint="?android:attr/colorAccent"/>

    <!-- 提示信息（输入内容时显示） -->
    <TextView
        android:id="@+id/tv_hint"
        android:visibility="gone"/>
</LinearLayout>
```

![屏幕截图 2025-12-07 153657](https://github.com/klebgh/android-mid-shiyan/blob/main/NotePad-main/image/%E5%B1%8F%E5%B9%95%E6%88%AA%E5%9B%BE%202025-12-07%20153657.png?raw=true)

Java 逻辑适配（NoteSearch 类）

在`onCreate`中初始化布局元素并设置交互逻辑：

```java
@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.note_search);

    // 初始化视图
    mSearchEditText = findViewById(R.id.edit_search);
    mClearIcon = findViewById(R.id.clear_btn);
    mSearchModeGroup = findViewById(R.id.search_mode_group);
    mHintText = findViewById(R.id.tv_hint);
    Button btnSearch = findViewById(R.id.btn_search);

    // 清除按钮点击事件：清空输入框
    mClearIcon.setOnClickListener(v -> {
        mSearchEditText.setText("");
        mHintText.setVisibility(View.GONE);
    });

    // 文本变化监听：显示/隐藏清除按钮和提示
    mSearchEditText.addTextChangedListener(new TextWatcher() {
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            mClearIcon.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
            mHintText.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
        }
    });

    // 搜索模式切换：更新输入框提示文字
    mSearchModeGroup.setOnCheckedChangeListener((group, checkedId) -> {
        if (checkedId == R.id.radio_title) {
            mSearchEditText.setHint("请输入标题关键词");
        } else {
            mSearchEditText.setHint("请输入内容关键词");
        }
    });

    // 搜索触发：按钮点击或键盘搜索键
    btnSearch.setOnClickListener(v -> performSearch());
    mSearchEditText.setOnEditorActionListener((v, actionId, event) -> {
        if (actionId == EditorInfo.IME_ACTION_SEARCH) {
            performSearch();
            return true;
        }
        return false;
    });
}
```

2.布局文件：note_editor.xml

编辑界面从 “仅内容输入” 优化为 “标题 + 分类 + 内容” 的三段式布局，提升笔记结构化程度。

新增标题编辑框:

```
<!-- 标题编辑框 -->
<EditText
    android:id="@+id/title"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:hint="标题"
    android:inputType="text"
    android:maxLines="1"  <!-- 限制单行标题 -->
    android:textSize="18sp"
    android:textStyle="bold"/>  <!-- 标题加粗突出显示 -->
```

Java 逻辑适配（NoteEditor 类）

在编辑页中添加标题的加载、保存和主题适配逻辑：

```java
public class NoteEditor extends Activity {
    private EditText mTitleText;  // 标题编辑框
    private EditText mContentText;  // 内容编辑框

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.note_editor);

        // 初始化标题和内容编辑框
        mTitleText = findViewById(R.id.title);
        mContentText = findViewById(R.id.note);

        // 加载笔记数据（从数据库游标中读取标题和内容）
        if (mCursor != null) {
            mCursor.moveToFirst();
            // 读取标题并设置到编辑框
            String title = mCursor.getString(mCursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_TITLE));
            mTitleText.setText(title);
            // 读取内容并设置到编辑框
            String content = mCursor.getString(mCursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_NOTE));
            mContentText.setText(content);
        }

        // 应用主题（标题和内容编辑框颜色适配）
        applyTheme();
    }

    private void applyTheme() {
        // 标题编辑框主题适配
        ThemeManager.applyEditTextTheme(this, mTitleText);
        // 内容编辑框主题适配
        ThemeManager.applyEditTextTheme(this, mContentText);
    }

    // 保存笔记时，同步保存标题和内容
    private void saveNote() {
        ContentValues values = new ContentValues();
        values.put(NotePad.Notes.COLUMN_NAME_TITLE, mTitleText.getText().toString());  // 保存标题
        values.put(NotePad.Notes.COLUMN_NAME_NOTE, mContentText.getText().toString());  // 保存内容
        getContentResolver().update(mUri, values, null, null);
    }
}
```

## 	语音搜索

语音搜索功能的实现主要涉及布局文件、Activity 逻辑处理及语音识别工具类三个部分，以下是详细说明：

一、在`note_search.xml`中添加语音搜索按钮

在布局中通过`ImageView`定义了语音搜索按钮，用于触发语音搜索功能，作为用户触发语音搜索的入口

```java
<!-- 语音按钮 -->
<ImageView
    android:id="@+id/voice_btn"
    android:layout_width="24dp"
    android:layout_height="24dp"
    android:layout_toRightOf="@+id/search_icon"  <!-- 位于搜索图标右侧 -->
    android:layout_centerVertical="true"
    android:layout_marginLeft="8dp"
    android:src="@drawable/ic_mic"  <!-- 麦克风图标 -->
    android:contentDescription="语音搜索"  <!-- 辅助功能描述 -->
    android:background="?android:attr/selectableItemBackground"  <!-- 点击反馈效果 -->
    android:clickable="true"  <!-- 允许点击 -->
    app:tint="?android:attr/textColorSecondary" />  <!-- 图标颜色适配主题 -->
```

二、通过创建语音搜索工具类（`VoiceSearchHelper.java`）实现封装语音识别逻辑

该类封装了 Android 系统语音识别 API（`SpeechRecognizer`）的调用，处理语音监听、结果回调及错误处理，核心功能如下：

1. 核心属性与接口

- `SpeechRecognizer`：系统语音识别核心类，用于处理语音输入和识别。
- `OnVoiceResultListener`：回调接口，用于将识别结果或错误信息传递给调用者（如`NoteSearch`）。
- `isEmulator`：检测当前设备是否为模拟器。

2. 初始化与配置

```java
private void initSpeechRecognizer() {
    if (SpeechRecognizer.isRecognitionAvailable(context)) {  // 检查设备是否支持语音识别
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onResults(Bundle results) {
                // 处理识别结果：取第一个匹配度最高的结果
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    listener.onVoiceResult(matches.get(0));  // 回调给调用者
                }
            }

            @Override
            public void onError(int error) {
                // 处理错误：转换错误码为可读信息
                String errorMsg = getErrorMsg(error);
                listener.onVoiceError(error, errorMsg);  // 回调错误信息
            }

            // 其他生命周期方法（如开始说话、结束说话等）
        });
    } else {
        // 设备不支持语音识别时提示
        Toast.makeText(context, "设备不支持语音识别", Toast.LENGTH_LONG).show();
    }
}
```

3. 关键方法

- `startListening()`：启动语音识别，配置识别参数（语言模型、提示信息等）：
- `stopListening()`/`cancel()`/`destroy()`：管理语音识别的生命周期，避免资源泄漏。
- `getErrorMsg()`：将系统返回的错误码（如网络错误、无匹配结果）转换为用户可读的提示信息。

```java
public void startListening() {
    Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
    intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
    intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());  // 使用系统默认语言
    intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "请说出搜索内容");  // 提示用户
    speechRecognizer.startListening(intent);  // 开始监听语音
}
 public void stopListening() {
        if (speechRecognizer != null) {
            speechRecognizer.stopListening();
            Log.d(TAG, "停止语音监听");
        }
    }

    public void cancel() {
        if (speechRecognizer != null) {
            speechRecognizer.cancel();
            Log.d(TAG, "取消语音监听");
        }
    }

    public void destroy() {
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
            Log.d(TAG, "销毁语音识别器");
        }
    }

    private String getErrorMsg(int errorCode) {
        switch (errorCode) {
            case SpeechRecognizer.ERROR_AUDIO:
                return "音频错误 - 请检查麦克风";
            case SpeechRecognizer.ERROR_CLIENT:
                return "客户端错误 - 请重启应用";
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                return "权限不足 - 请授予录音权限";
            case SpeechRecognizer.ERROR_NETWORK:
                return "网络错误 - 请检查网络连接";
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                return "网络超时 - 请检查网络连接";
            case SpeechRecognizer.ERROR_NO_MATCH:
                return "未识别到语音 - 请重试";
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                return "识别器忙 - 请稍后重试";
            case SpeechRecognizer.ERROR_SERVER:
                return "服务器错误 - 请稍后重试";
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                return "未检测到语音 - 请说话";
            default:
                return "未知错误 (错误码: " + errorCode + ")";
        }
    }
```

三、在`NoteSearch`中实现触发与处理语音搜索相关逻辑

该 Activity 作为语音搜索的调用者，负责绑定 UI 按钮事件、处理权限请求及接收识别结果，核心流程如下：

1. 绑定语音按钮与点击事件

```java
voiceBtn = findViewById(R.id.voice_btn);
voiceBtn.setOnClickListener(v -> {
    if (isRunningOnEmulator()) {  // 模拟器适配：显示输入对话框（替代语音输入）
        showEmulatorInputDialog();
    } else {
        startVoiceSearch();  // 真实设备启动语音搜索
    }
});
```

2. 初始化语音助手并处理结果

```java
// 初始化VoiceSearchHelper，实现回调接口
voiceSearchHelper = new VoiceSearchHelper(this, new VoiceSearchHelper.OnVoiceResultListener() {
    @Override
    public void onVoiceResult(String result) {
        runOnUiThread(() -> mSearchEditText.setText(result));  // 将识别结果设置到输入框
    }

    @Override
    public void onVoiceError(int errorCode, String errorMessage) {
        // 处理错误（如显示Toast）
    }
});
```

3. 权限处理（语音识别需要录音权限）

```java
private void startVoiceSearch() {
    if (checkAudioPermission()) {  // 检查是否有录音权限
        voiceSearchHelper.startListening();  // 有权限则启动识别
    } else {
        requestAudioPermission();  // 无权限则请求
    }
}

// 请求录音权限
private void requestAudioPermission() {
    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_AUDIO_PERMISSION);
}

// 权限回调处理
@Override
public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    if (requestCode == REQUEST_AUDIO_PERMISSION && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
        voiceSearchHelper.startListening();  // 权限授予后启动识别
    } else {
        Toast.makeText(this, "需要录音权限才能使用语音搜索", Toast.LENGTH_SHORT).show();
    }
}
```

4. 资源释放

在 Activity 销毁时释放语音识别资源，避免内存泄漏：

```java
@Override
protected void onDestroy() {
    super.onDestroy();
    if (voiceSearchHelper != null) {
        voiceSearchHelper.destroy();  // 销毁语音识别器
    }
}
```

## 	笔记分类

笔记分类的实现比较简单，只在列表项左侧添加了对应分类的图片，在编辑界面中添加了下拉的分类选择框

 一、修改数据库：为`note_pad.db`的`notes`表添加`category`字段，用于存储笔记分类信息

1.在**NotePad.java**  添加分类字段定义：public static final String COLUMN_NAME_CATEGORY = "category";

2.在**NotePadProvider.java** 修改数据库创建和更新：

```java
// 在DatabaseHelper的onCreate方法中更新表结构
@Override
public void onCreate(SQLiteDatabase db) {
    db.execSQL("CREATE TABLE " + NotePad.Notes.TABLE_NAME + " ("
            + NotePad.Notes._ID + " INTEGER PRIMARY KEY,"
            + NotePad.Notes.COLUMN_NAME_TITLE + " TEXT,"
            + NotePad.Notes.COLUMN_NAME_NOTE + " TEXT,"
            + NotePad.Notes.COLUMN_NAME_CREATE_DATE + " INTEGER,"
            + NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE + " INTEGER,"
            + NotePad.Notes.COLUMN_NAME_CATEGORY + " TEXT DEFAULT '" + NotePad.Notes.DEFAULT_CATEGORY + "'" 
            + ");");
}

@Override
public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    if (oldVersion < 3) { // 假设当前版本为3，新增分类字段
        db.execSQL("ALTER TABLE " + NotePad.Notes.TABLE_NAME + 
                  " ADD COLUMN " + NotePad.Notes.COLUMN_NAME_CATEGORY + " TEXT DEFAULT '" + NotePad.Notes.DEFAULT_CATEGORY + "'");
    }
}
```

对数据库的修改很少，只添加了category字段，该字段只有工作，学习，生活，未定义这四个值，默认值未定义

二、分类常量定义（`CategoryConstants.java`）

定义分类常量，便于统一管理分类标识、显示名和图标，避免硬编码

```java
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
```

三、编辑界面：分类选择功能

在标题与内容编辑框之间添加分类下拉选择框，支持选择 “工作”“学习”“生活”，默认 “未定义”。

1. 布局文件（category_spinner_item.xml note_editor.xml）

   ```java
   <?xml version="1.0" encoding="utf-8"?>
   <TextView xmlns:android="http://schemas.android.com/apk/res/android"
       android:id="@android:id/text1"
       android:layout_width="match_parent"
       android:layout_height="wrap_content"
       android:textSize="16sp"
       android:padding="8dp"
       android:textColor="@android:color/black" />
   ```

```java
 <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:gravity="center_vertical"
        android:layout_marginBottom="12dp"
        android:paddingVertical="8dp">

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="分类:"
            android:textSize="16sp"
            android:layout_marginEnd="12dp" />

        <Spinner
            android:id="@+id/category_spinner"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1" />
    </LinearLayout>
```

![屏幕截图 2025-12-07 181127](https://github.com/klebgh/android-mid-shiyan/blob/main/NotePad-main/image/%E5%B1%8F%E5%B9%95%E6%88%AA%E5%9B%BE%202025-12-07%20181127.png?raw=true)

2. 逻辑处理（`NoteEditor.java`）

实现了以下逻辑:初始化下拉框→监听选择→加载已有分类→保存时写入数据库。

```java
// 初始化分类选择器
private Spinner mCategorySpinner;
private String mCurrentCategory = CategoryConstants.CATEGORY_UNDEFINED; // 默认未定义
private static final String[] CATEGORIES = {"工作", "学习", "生活"}; 
private static final String[] CATEGORY_VALUES = { 
    CategoryConstants.CATEGORY_WORK,
    CategoryConstants.CATEGORY_STUDY,
    CategoryConstants.CATEGORY_LIFE
};

@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.note_editor);

    mCategorySpinner = findViewById(R.id.category_spinner);
    ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
            android.R.layout.simple_spinner_item, CATEGORIES);
    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    mCategorySpinner.setAdapter(adapter);

    // 监听选择事件，更新当前分类
    mCategorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            mCurrentCategory = CATEGORY_VALUES[position];
        }
        @Override
        public void onNothingSelected(AdapterView<?> parent) {
            mCurrentCategory = CategoryConstants.CATEGORY_UNDEFINED;
        }
    });

    if (mState == STATE_EDIT) {
        loadNoteCategory();
    }
}

// 加载已有笔记的分类并设置到Spinner
private void loadNoteCategory() {
    String[] projection = {NotePad.Notes.COLUMN_NAME_CATEGORY};
    Cursor cursor = getContentResolver().query(mUri, projection, null, null, null);
    if (cursor != null && cursor.moveToFirst()) {
        String category = cursor.getString(0);
        cursor.close();
        // 根据分类标识设置Spinner选中项
        for (int i = 0; i < CATEGORY_VALUES.length; i++) {
            if (CATEGORY_VALUES[i].equals(category)) {
                mCategorySpinner.setSelection(i);
                return;
            }
        }

        mCurrentCategory = CategoryConstants.CATEGORY_UNDEFINED;
    }
}

private void saveNote() {
    ContentValues values = new ContentValues();
    values.put(NotePad.Notes.COLUMN_NAME_TITLE, mTitleText.getText().toString());
    values.put(NotePad.Notes.COLUMN_NAME_NOTE, mContentText.getText().toString());
    values.put(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE, System.currentTimeMillis());
    values.put(NotePad.Notes.COLUMN_NAME_CATEGORY, mCurrentCategory); // 新增：保存分类

    if (mState == STATE_EDIT) {
        getContentResolver().update(mUri, values, null, null);
    } else {
        values.put(NotePad.Notes.COLUMN_NAME_CREATE_DATE, System.currentTimeMillis());
        getContentResolver().insert(mUri, values);
    }
}
```

四、列表界面：分类图标显示

在笔记列表项左侧显示分类图标（普通列表和搜索结果列表通用）。

1. 布局文件（`noteslist_item.xml`）

```java
<!-- 分类图标（左侧显示） -->
<ImageView
    android:id="@+id/category_icon"
    android:layout_width="32dp"
    android:layout_height="32dp"
    android:layout_marginEnd="12dp"
    android:scaleType="centerInside" />
```

![屏幕截图 2025-12-07 180926](https://github.com/klebgh/android-mid-shiyan/blob/main/NotePad-main/image/%E5%B1%8F%E5%B9%95%E6%88%AA%E5%9B%BE%202025-12-07%20180926.png?raw=true)

2. 逻辑处理（`NotesList.java`）

更新PROJECTION，dataColumns和viewId,修改列表适配器，根据笔记分类显示对应图标

```java
// 在NotesList的onCreate方法中，适配器设置ViewBinder时修改
adapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
    @Override
    public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
        // ... 其他字段处理（标题、时间等）

        // 处理分类图标（根据数据库的category字段）
        if (view.getId() == R.id.category_icon) {
            ImageView categoryIcon = (ImageView) view;
            // 从Cursor获取当前笔记的category字段（需在PROJECTION中添加该字段）
            String category = cursor.getString(cursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_CATEGORY));
            // 根据分类获取对应图标
            int iconRes = CategoryConstants.getCategoryIcon(category);
            categoryIcon.setImageResource(iconRes);
            return true;
        }
        return false;
    }
});

```

五、搜索结果界面：分类图标显示

搜索结果列表与普通列表共用`noteslist_item.xml`布局，因此只需在搜索结果的适配器中复用上述`ViewBinder`逻辑，确保查询时包含`category`字段并设置图标即可。

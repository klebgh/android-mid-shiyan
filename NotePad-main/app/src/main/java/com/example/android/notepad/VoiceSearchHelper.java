package com.example.android.notepad;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Locale;

public class VoiceSearchHelper {
    private static final String TAG = "VoiceSearchHelper";
    private SpeechRecognizer speechRecognizer;
    private Context context;
    private OnVoiceResultListener listener;
    private boolean isEmulator = false;

    public interface OnVoiceResultListener {
        void onVoiceResult(String result);
        void onVoiceError(int errorCode, String errorMessage);

        void onVoiceError(int errorCode);
    }

    public VoiceSearchHelper(Context context, OnVoiceResultListener listener) {
        this.context = context;
        this.listener = listener;
        detectEmulator();
        initSpeechRecognizer();
    }

    private void detectEmulator() {
        // 检测是否是模拟器
        isEmulator = Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for")
                || Build.MANUFACTURER.contains("Genymotion")
                || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || "google_sdk".equals(Build.PRODUCT);
    }

    private void initSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
            speechRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override
                public void onReadyForSpeech(Bundle params) {
                    Log.d(TAG, "准备接收语音");
                }

                @Override
                public void onBeginningOfSpeech() {
                    Log.d(TAG, "开始说话");
                }

                @Override
                public void onRmsChanged(float rmsdB) {
                    // 音量变化
                }

                @Override
                public void onBufferReceived(byte[] buffer) {}

                @Override
                public void onEndOfSpeech() {
                    Log.d(TAG, "结束说话");
                }

                @Override
                public void onError(int error) {
                    String errorMsg = getErrorMsg(error);
                    Log.e(TAG, "语音识别错误: " + error + " - " + errorMsg);

                    // 如果是模拟器，提供更友好的提示
                    if (isEmulator) {
                        errorMsg += " (模拟器上语音识别可能无法正常工作，请使用真实设备)";
                    }

                    Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show();
                    if (listener != null) {
                        listener.onVoiceError(error, errorMsg);
                    }
                }

                @Override
                public void onResults(Bundle results) {
                    ArrayList<String> matches = results.getStringArrayList(
                            SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null && !matches.isEmpty()) {
                        String bestResult = matches.get(0);
                        Log.d(TAG, "语音识别结果: " + bestResult);
                        if (listener != null) {
                            listener.onVoiceResult(bestResult);
                        }
                    } else {
                        Log.w(TAG, "语音识别返回空结果");
                        if (listener != null) {
                            listener.onVoiceError(SpeechRecognizer.ERROR_NO_MATCH, "未识别到语音");
                        }
                    }
                }

                @Override
                public void onPartialResults(Bundle partialResults) {
                    // 部分结果，可用于实时显示
                    ArrayList<String> partialMatches = partialResults.getStringArrayList(
                            SpeechRecognizer.RESULTS_RECOGNITION);
                    if (partialMatches != null && !partialMatches.isEmpty()) {
                        Log.d(TAG, "部分识别结果: " + partialMatches.get(0));
                    }
                }

                @Override
                public void onEvent(int eventType, Bundle params) {}
            });
        } else {
            String errorMsg = "设备不支持语音识别";
            if (isEmulator) {
                errorMsg += " - 模拟器可能缺少Google语音服务";
            }
            Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show();
            Log.e(TAG, errorMsg);
        }
    }

    public void startListening() {
        if (speechRecognizer == null) {
            Toast.makeText(context, "语音识别不可用", Toast.LENGTH_SHORT).show();
            return;
        }

        // 如果是模拟器，给出提示
        if (isEmulator) {
            Toast.makeText(context,
                    "模拟器语音识别可能无法正常工作，建议使用真实设备",
                    Toast.LENGTH_LONG).show();
        }

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "请说出搜索内容");
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);

        // 添加更多配置以提高兼容性
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        intent.putExtra("android.speech.extra.GET_AUDIO_FORMAT", "audio/AMR");
        intent.putExtra("android.speech.extra.GET_AUDIO", true);

        try {
            speechRecognizer.startListening(intent);
            Log.d(TAG, "开始语音监听");
        } catch (SecurityException e) {
            Toast.makeText(context, "请授予录音权限", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "权限异常: " + e.getMessage());
        } catch (Exception e) {
            Toast.makeText(context, "语音识别启动失败", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "启动异常: " + e.getMessage());
        }
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

    public boolean isEmulator() {
        return isEmulator;
    }
}
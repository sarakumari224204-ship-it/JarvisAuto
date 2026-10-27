package com.jarvis.auto;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private TextView tvCommand;
    private Button btnSpeak;
    private SpeechRecognizer speechRecognizer;
    private TextToSpeech textToSpeech;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setGravity(android.view.Gravity.CENTER);
        layout.setPadding(40, 40, 40, 40);

        TextView tvTitle = new TextView(this);
        tvTitle.setText("JARVIS AI CONTROLLER");
        tvTitle.setTextSize(22);
        tvTitle.setGravity(android.view.Gravity.CENTER);
        layout.addView(tvTitle);

        tvCommand = new TextView(this);
        tvCommand.setText("Boliye: 'kya kar sakte ho', 'scroll down', 'youtube kholo'");
        tvCommand.setTextSize(16);
        tvCommand.setPadding(0, 30, 0, 50);
        tvCommand.setGravity(android.view.Gravity.CENTER);
        layout.addView(tvCommand);

        btnSpeak = new Button(this);
        btnSpeak.setText("VOICE COMMAND DEIN");
        layout.addView(btnSpeak);

        setContentView(layout);

        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.setLanguage(new Locale("hi", "IN"));
            }
        });

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 1);
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle params) { tvCommand.setText("Sun raha hoon, boliye..."); }
            @Override public void onBeginningOfSpeech() {}
            @Override public void onRmsChanged(float rmsdB) {}
            @Override public void onBufferReceived(byte[] buffer) {}
            @Override public void onEndOfSpeech() {}
            @Override public void onError(int error) { tvCommand.setText("Aawaz samajh nahi aayi, dobara dabayein."); }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    executeJarvisCommand(matches.get(0));
                }
            }

            @Override public void onPartialResults(Bundle partialResults) {}
            @Override public void onEvent(int eventType, Bundle params) {}
        });

        btnSpeak.setOnClickListener(v -> startListening());
    }

    private void startListening() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN");
        speechRecognizer.startListening(intent);
    }

    private void executeJarvisCommand(String command) {
        String cmd = command.toLowerCase();
        String reply;
        AutoTouchService service = AutoTouchService.getInstance();

        if (cmd.contains("kya kar sakte ho") || cmd.contains("tum kya")) {
            reply = "Main aapki screen scroll kar sakta hoon, touch kar sakta hoon aur apps launch kar sakta hoon.";
            speak(reply);
        } else if (cmd.contains("scroll down") || cmd.contains("niche scroll")) {
            reply = "Neeche scroll kar raha hoon.";
            speak(reply);
            minimizeAndExecute(() -> {
                if (service != null) service.scrollDown();
            });
        } else if (cmd.contains("scroll up") || cmd.contains("upar scroll")) {
            reply = "Upar scroll kar raha hoon.";
            speak(reply);
            minimizeAndExecute(() -> {
                if (service != null) service.scrollUp();
            });
        } else if (cmd.contains("youtube")) {
            reply = "YouTube open kar raha hoon.";
            speak(reply);
            Intent launchIntent = getPackageManager().getLaunchIntentForPackage("com.google.android.youtube");
            if (launchIntent != null) startActivity(launchIntent);
        } else if (cmd.contains("touch") || cmd.contains("click")) {
            reply = "Screen center par touch kar raha hoon.";
            speak(reply);
            minimizeAndExecute(() -> {
                if (service != null) service.clickAt(540, 1100);
            });
        } else {
            reply = "Aapne kaha: " + command;
            speak(reply);
        }

        tvCommand.setText("Aap: " + command + "\n\nJarvis: " + reply);
    }

    private void minimizeAndExecute(Runnable action) {
        Intent startMain = new Intent(Intent.ACTION_MAIN);
        startMain.addCategory(Intent.CATEGORY_HOME);
        startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(startMain);
        handler.postDelayed(action, 1200);
    }

    private void speak(String text) {
        if (textToSpeech != null) {
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    @Override
    protected void onDestroy() {
        if (speechRecognizer != null) speechRecognizer.destroy();
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroy();
    }
}

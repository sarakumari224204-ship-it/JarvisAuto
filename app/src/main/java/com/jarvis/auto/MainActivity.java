package com.jarvis.auto;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setGravity(android.view.Gravity.CENTER);
        layout.setPadding(40, 40, 40, 40);

        TextView tvTitle = new TextView(this);
        tvTitle.setText("JARVIS CONTROLLER");
        tvTitle.setTextSize(24);
        tvTitle.setGravity(android.view.Gravity.CENTER);
        layout.addView(tvTitle);

        tvCommand = new TextView(this);
        tvCommand.setText("Jarvis bolne aur sunne ke liye tayyar hai.");
        tvCommand.setTextSize(16);
        tvCommand.setPadding(0, 30, 0, 50);
        tvCommand.setGravity(android.view.Gravity.CENTER);
        layout.addView(tvCommand);

        btnSpeak = new Button(this);
        btnSpeak.setText("VOICE COMMAND DEIN");
        layout.addView(btnSpeak);

        setContentView(layout);

        // Text-To-Speech setup
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
            @Override public void onError(int error) { tvCommand.setText("Aawaz samajh nahi aayi, dobara boliye."); }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    String userSaid = matches.get(0);
                    processCommand(userSaid);
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

    private void processCommand(String text) {
        String lower = text.toLowerCase();
        String reply;

        if (lower.contains("kya kya kar sakte ho") || lower.contains("kya kar sakte ho")) {
            reply = "Main aapke phone ki screen control kar sakta hoon, apps open kar sakta hoon aur aawaz se command follow kar sakta hoon.";
        } else if (lower.contains("kaise ho")) {
            reply = "Main badhiya hoon. Aap bataiye kya madad karoon?";
        } else if (lower.contains("khol") || lower.contains("open")) {
            reply = "Thik hai, command execute kar raha hoon.";
            if (AutoTouchService.getInstance() != null) {
                AutoTouchService.getInstance().clickScreen(500, 500);
            }
        } else {
            reply = "Aapne kaha: " + text + ". Main iska reply tayyar kar raha hoon.";
        }

        tvCommand.setText("Aap: " + text + "\n\nJarvis: " + reply);
        speakOut(reply);
    }

    private void speakOut(String speechText) {
        if (textToSpeech != null) {
            textToSpeech.speak(speechText, TextToSpeech.QUEUE_FLUSH, null, null);
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

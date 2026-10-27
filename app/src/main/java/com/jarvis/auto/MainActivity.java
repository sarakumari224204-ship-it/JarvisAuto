package com.jarvis.auto;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "JarvisSettings";
    private static final String KEY_GEMINI_API = "gemini_api_key";

    private EditText etApiKey;
    private TextView tvCommand;
    private Button btnSpeak;
    private Button btnSaveKey;
    private SpeechRecognizer speechRecognizer;
    private TextToSpeech textToSpeech;
    private SharedPreferences prefs;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(40, 40, 40, 40);

        TextView tvTitle = new TextView(this);
        tvTitle.setText("JARVIS AI CONTROLLER");
        tvTitle.setTextSize(22);
        tvTitle.setGravity(Gravity.CENTER);
        layout.addView(tvTitle);

        etApiKey = new EditText(this);
        etApiKey.setHint("Yahan apni Gemini API Key daalein");
        etApiKey.setText(prefs.getString(KEY_GEMINI_API, ""));
        layout.addView(etApiKey);

        btnSaveKey = new Button(this);
        btnSaveKey.setText("SAVE API KEY");
        layout.addView(btnSaveKey);

        tvCommand = new TextView(this);
        tvCommand.setText("API Key save karein aur 'VOICE COMMAND DEIN' dabayein.");
        tvCommand.setTextSize(16);
        tvCommand.setPadding(0, 30, 0, 40);
        tvCommand.setGravity(Gravity.CENTER);
        layout.addView(tvCommand);

        btnSpeak = new Button(this);
        btnSpeak.setText("VOICE COMMAND DEIN");
        layout.addView(btnSpeak);

        setContentView(layout);

        btnSaveKey.setOnClickListener(v -> {
            String enteredKey = etApiKey.getText().toString().trim();
            if (!enteredKey.isEmpty()) {
                prefs.edit().putString(KEY_GEMINI_API, enteredKey).apply();
                Toast.makeText(this, "API Key Saved Successfully!", Toast.LENGTH_SHORT).show();
                tvCommand.setText("API Key set ho gayi! Ab command dein.");
            } else {
                Toast.makeText(this, "Kripya valid API key daalein!", Toast.LENGTH_SHORT).show();
            }
        });

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
                    String userText = matches.get(0);
                    tvCommand.setText("Aap: " + userText + "\n\nJarvis soch raha hai...");
                    callGeminiAI(userText);
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

    private void callGeminiAI(String prompt) {
        executor.execute(() -> {
            String apiKey = prefs.getString(KEY_GEMINI_API, "").trim();
            String aiResponse;

            if (apiKey.isEmpty()) {
                aiResponse = "Kripya pehle app mein apni Gemini API Key save karein.";
            } else {
                try {
                    String systemPrompt = "Aap Jarvis hain. User sawal pooche toh short Hindi reply dein. User query: " + prompt;
                    String urlStr = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + apiKey;
                    URL url = new URL(urlStr);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                    conn.setDoOutput(true);

                    JSONObject textPart = new JSONObject().put("text", systemPrompt);
                    JSONObject parts = new JSONObject().put("parts", new JSONArray().put(textPart));
                    JSONObject body = new JSONObject().put("contents", new JSONArray().put(parts));

                    OutputStream os = conn.getOutputStream();
                    os.write(body.toString().getBytes("UTF-8"));
                    os.close();

                    int responseCode = conn.getResponseCode();
                    if (responseCode == 200) {
                        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = in.readLine()) != null) sb.append(line);
                        in.close();

                        JSONObject json = new JSONObject(sb.toString());
                        aiResponse = json.getJSONArray("candidates")
                                .getJSONObject(0)
                                .getJSONObject("content")
                                .getJSONArray("parts")
                                .getJSONObject(0)
                                .getString("text");
                    } else {
                        aiResponse = fallbackLogic(prompt);
                    }
                } catch (Exception e) {
                    aiResponse = fallbackLogic(prompt);
                }
            }

            final String replyText = aiResponse;
            mainHandler.post(() -> {
                tvCommand.setText("Aap: " + prompt + "\n\nJarvis: " + replyText);
                speak(replyText);
                handleScreenActions(prompt);
            });
        });
    }

    private String fallbackLogic(String text) {
        String lower = text.toLowerCase();
        if (lower.contains("kya kar sakte ho") || lower.contains("kaun ho")) {
            return "Main aapka Jarvis hoon. Main phone screen control kar sakta hoon, scroll kar sakta hoon aur baatcheet kar sakta hoon.";
        } else if (lower.contains("kaise ho")) {
            return "Main badhiya hoon. Aap bataiye kya hukam hai?";
        } else if (lower.contains("scroll down") || lower.contains("niche")) {
            return "Neeche scroll kar raha hoon.";
        } else if (lower.contains("scroll up") || lower.contains("upar")) {
            return "Upar scroll kar raha hoon.";
        } else if (lower.contains("youtube")) {
            return "YouTube open kar raha hoon.";
        } else {
            return "Command samajh li gayi hai, koshish kar raha hoon.";
        }
    }

    private void handleScreenActions(String prompt) {
        String lower = prompt.toLowerCase();
        AutoTouchService service = AutoTouchService.getInstance();

        if (lower.contains("scroll down") || lower.contains("niche")) {
            minimizeAndExecute(() -> { if (service != null) service.scrollDown(); });
        } else if (lower.contains("scroll up") || lower.contains("upar")) {
            minimizeAndExecute(() -> { if (service != null) service.scrollUp(); });
        } else if (lower.contains("youtube")) {
            Intent launchIntent = getPackageManager().getLaunchIntentForPackage("com.google.android.youtube");
            if (launchIntent != null) startActivity(launchIntent);
        } else if (lower.contains("click") || lower.contains("touch")) {
            minimizeAndExecute(() -> { if (service != null) service.clickAt(540, 1100); });
        }
    }

    private void minimizeAndExecute(Runnable action) {
        Intent startMain = new Intent(Intent.ACTION_MAIN);
        startMain.addCategory(Intent.CATEGORY_HOME);
        startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(startMain);
        mainHandler.postDelayed(action, 1200);
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
        executor.shutdown();
        super.onDestroy();
    }
}

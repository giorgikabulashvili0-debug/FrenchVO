package com.georgeslebatoon.frenchvo;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends Activity implements TextToSpeech.OnInitListener {
    private TextToSpeech tts;
    private EditText textInput;
    private Spinner voiceSpinner;
    private SeekBar speedSeek;
    private TextView speedLabel;
    private TextView statusText;
    private Button previewButton;
    private Button saveButton;
    private final List<Voice> frenchVoices = new ArrayList<>();
    private float speechRate = 1.0f;
    private File pendingOutputFile;
    private String pendingDisplayName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textInput = findViewById(R.id.textInput);
        voiceSpinner = findViewById(R.id.voiceSpinner);
        speedSeek = findViewById(R.id.speedSeek);
        speedLabel = findViewById(R.id.speedLabel);
        statusText = findViewById(R.id.statusText);
        previewButton = findViewById(R.id.previewButton);
        saveButton = findViewById(R.id.saveButton);
        Button stopButton = findViewById(R.id.stopButton);

        previewButton.setEnabled(false);
        saveButton.setEnabled(false);

        speedSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                speechRate = 0.70f + (progress / 100f);
                speedLabel.setText(String.format(Locale.US, "Vitesse : %.2f×", speechRate));
                if (tts != null) tts.setSpeechRate(speechRate);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        previewButton.setOnClickListener(v -> preview());
        saveButton.setOnClickListener(v -> synthesizeAndSave());
        stopButton.setOnClickListener(v -> {
            if (tts != null) tts.stop();
            pendingOutputFile = null;
            previewButton.setEnabled(true);
            saveButton.setEnabled(true);
            statusText.setText("Arrêté.");
        });

        tts = new TextToSpeech(this, this);
    }

    @Override
    public void onInit(int status) {
        if (status != TextToSpeech.SUCCESS) {
            statusText.setText("Impossible d'initialiser le moteur TTS.");
            return;
        }

        tts.setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build());
        tts.setLanguage(Locale.FRANCE);
        tts.setSpeechRate(speechRate);
        loadFrenchVoices();

        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override public void onStart(String utteranceId) {
                runOnUiThread(() -> statusText.setText(
                        utteranceId.startsWith("SAVE_") ? "Génération du fichier…" : "Lecture…"));
            }

            @Override public void onDone(String utteranceId) {
                if (utteranceId.startsWith("SAVE_") && pendingOutputFile != null) {
                    saveToDownloads(pendingOutputFile, pendingDisplayName);
                } else {
                    runOnUiThread(() -> statusText.setText("Terminé."));
                }
            }

            @Override public void onError(String utteranceId) {
                runOnUiThread(() -> {
                    statusText.setText("Erreur pendant la synthèse.");
                    previewButton.setEnabled(true);
                    saveButton.setEnabled(true);
                });
            }
        });

        previewButton.setEnabled(true);
        saveButton.setEnabled(true);
        statusText.setText("Prêt. Les voix disponibles dépendent de ton moteur TTS Android.");
    }

    private void loadFrenchVoices() {
        frenchVoices.clear();
        List<String> labels = new ArrayList<>();
        if (tts.getVoices() != null) {
            for (Voice voice : tts.getVoices()) {
                Locale locale = voice.getLocale();
                if (locale != null && "fr".equals(locale.getLanguage())) {
                    frenchVoices.add(voice);
                    String network = voice.isNetworkConnectionRequired() ? " • en ligne" : " • hors ligne";
                    labels.add(locale.toLanguageTag() + " — " + voice.getName() + network);
                }
            }
        }

        if (labels.isEmpty()) {
            labels.add("Français (voix par défaut Android)");
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, labels);
        voiceSpinner.setAdapter(adapter);
    }

    private void applySelectedVoice() {
        int pos = voiceSpinner.getSelectedItemPosition();
        if (!frenchVoices.isEmpty() && pos >= 0 && pos < frenchVoices.size()) {
            tts.setVoice(frenchVoices.get(pos));
        } else {
            tts.setLanguage(Locale.FRANCE);
        }
        tts.setSpeechRate(speechRate);
    }

    private String getTextOrWarn() {
        String text = textInput.getText().toString().trim();
        if (text.isEmpty()) {
            Toast.makeText(this, "Ajoute un texte français.", Toast.LENGTH_SHORT).show();
            return null;
        }
        if (text.length() > TextToSpeech.getMaxSpeechInputLength()) {
            Toast.makeText(this, "Texte trop long : maximum " + TextToSpeech.getMaxSpeechInputLength() + " caractères par génération dans V1.", Toast.LENGTH_LONG).show();
            return null;
        }
        return text;
    }

    private void preview() {
        String text = getTextOrWarn();
        if (text == null) return;
        applySelectedVoice();
        tts.stop();
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "PREVIEW_" + System.currentTimeMillis());
    }

    private void synthesizeAndSave() {
        String text = getTextOrWarn();
        if (text == null) return;
        applySelectedVoice();
        tts.stop();

        previewButton.setEnabled(false);
        saveButton.setEnabled(false);
        String stamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        pendingDisplayName = "FrenchVO_" + stamp + ".wav";
        pendingOutputFile = new File(getCacheDir(), pendingDisplayName);
        if (pendingOutputFile.exists()) pendingOutputFile.delete();

        int result = tts.synthesizeToFile(text, null, pendingOutputFile,
                "SAVE_" + System.currentTimeMillis());
        if (result == TextToSpeech.ERROR) {
            statusText.setText("Impossible de lancer la génération.");
            previewButton.setEnabled(true);
            saveButton.setEnabled(true);
        }
    }

    private void saveToDownloads(File source, String displayName) {
        try {
            ContentResolver resolver = getContentResolver();
            ContentValues values = new ContentValues();
            values.put(MediaStore.Downloads.DISPLAY_NAME, displayName);
            values.put(MediaStore.Downloads.MIME_TYPE, "audio/wav");
            values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/FrenchVO");
            values.put(MediaStore.Downloads.IS_PENDING, 1);

            Uri uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (uri == null) throw new Exception("MediaStore insert failed");

            try (FileInputStream in = new FileInputStream(source);
                 OutputStream out = resolver.openOutputStream(uri)) {
                if (out == null) throw new Exception("Output stream unavailable");
                byte[] buffer = new byte[8192];
                int len;
                while ((len = in.read(buffer)) > 0) out.write(buffer, 0, len);
            }

            values.clear();
            values.put(MediaStore.Downloads.IS_PENDING, 0);
            resolver.update(uri, values, null, null);

            runOnUiThread(() -> {
                previewButton.setEnabled(true);
                saveButton.setEnabled(true);
                statusText.setText("Enregistré : Downloads/FrenchVO/" + displayName);
                Toast.makeText(this, "VO enregistré dans Téléchargements/FrenchVO", Toast.LENGTH_LONG).show();
            });
        } catch (Exception e) {
            runOnUiThread(() -> {
                statusText.setText("Erreur d'enregistrement : " + e.getMessage());
                previewButton.setEnabled(true);
                saveButton.setEnabled(true);
            });
        }
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }
}

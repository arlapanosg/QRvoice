package gr.qrvoice.app;

import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;

import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanIntentResult;
import com.journeyapps.barcodescanner.ScanOptions;

import java.util.Locale;

public class MainActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {
    private TextToSpeech textToSpeech;
    private boolean ttsReady = false;
    private TextView resultText;

    private final ActivityResultLauncher<ScanOptions> scanner =
            registerForActivityResult(new ScanContract(), this::handleScanResult);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        resultText = findViewById(R.id.resultText);
        Button scanButton = findViewById(R.id.scanButton);
        Button repeatButton = findViewById(R.id.repeatButton);

        textToSpeech = new TextToSpeech(this, this);
        scanButton.setOnClickListener(v -> startScanner());
        repeatButton.setOnClickListener(v -> speak(resultText.getText().toString()));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (resultText != null && resultText.getText().toString().isEmpty()) {
            resultText.postDelayed(this::startScanner, 350);
        }
    }

    private void startScanner() {
        ScanOptions options = new ScanOptions();
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
        options.setPrompt("Τοποθετήστε το QR μέσα στο πλαίσιο");
        options.setBeepEnabled(true);
        options.setOrientationLocked(false);
        options.setBarcodeImageEnabled(false);
        scanner.launch(options);
    }

    private void handleScanResult(ScanIntentResult result) {
        if (result.getContents() == null) {
            Toast.makeText(this, "Η σάρωση ακυρώθηκε", Toast.LENGTH_SHORT).show();
            return;
        }

        String text = result.getContents().trim();
        if (text.regionMatches(true, 0, "SPEAK:", 0, 6)) {
            text = text.substring(6).trim();
        }

        resultText.setText(text);
        speak(text);
    }

    private void speak(String text) {
        if (text == null || text.trim().isEmpty()) {
            Toast.makeText(this, "Το QR δεν περιέχει κείμενο", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!ttsReady) {
            Toast.makeText(this, "Η φωνή δεν είναι ακόμη έτοιμη", Toast.LENGTH_SHORT).show();
            return;
        }
        textToSpeech.stop();
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, "qrvoice-utterance");
    }

    @Override
    public void onInit(int status) {
        if (status != TextToSpeech.SUCCESS) {
            Toast.makeText(this, "Δεν βρέθηκε διαθέσιμη υπηρεσία εκφώνησης", Toast.LENGTH_LONG).show();
            return;
        }

        int languageResult = textToSpeech.setLanguage(new Locale("el", "GR"));
        if (languageResult == TextToSpeech.LANG_MISSING_DATA ||
                languageResult == TextToSpeech.LANG_NOT_SUPPORTED) {
            Toast.makeText(this,
                    "Δεν είναι εγκατεστημένη ελληνική φωνή στις ρυθμίσεις Text-to-Speech",
                    Toast.LENGTH_LONG).show();
            return;
        }

        textToSpeech.setSpeechRate(0.95f);
        ttsReady = true;
    }

    @Override
    protected void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroy();
    }
}

package com.example.makani

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.*
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.util.*

class VoiceNoteFragment : Fragment() {

    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var speechIntent: Intent
    private lateinit var database: DatabaseReference

    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var noteEditText: EditText

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_voice_note, container, false)

        startButton = view.findViewById(R.id.startRecordingButton)
        stopButton = view.findViewById(R.id.stopRecordingButton)
        noteEditText = view.findViewById(R.id.voiceResultText)

        database = FirebaseDatabase.getInstance().getReference("voice_notes")

        // طلب صلاحية المايك
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.RECORD_AUDIO), 1)
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(requireContext())
        speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-JO")
        }
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                matches?.let {
                    val currentText = noteEditText.text.toString()
                    noteEditText.setText("$currentText ${it[0]} ")
                    saveNoteToFirebase()
                }
            }
            override fun onError(error: Int) {
                Toast.makeText(requireContext(), "⚠️ حدث خطأ أثناء التسجيل", Toast.LENGTH_SHORT).show()
            }
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        startButton.setOnClickListener {
            speechRecognizer.startListening(speechIntent)
            startButton.isEnabled = false
            stopButton.isEnabled = true
        }

        stopButton.setOnClickListener {
            speechRecognizer.stopListening()
            startButton.isEnabled = true
            stopButton.isEnabled = false
        }

        return view
    }

    private fun saveNoteToFirebase() {
        val noteText = noteEditText.text.toString().trim()
        if (noteText.isNotEmpty()) {
            val noteId = database.push().key ?: return
            database.child(noteId).setValue(noteText)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Savedة", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Fail to save note", Toast.LENGTH_SHORT).show()
                }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer.destroy()
    }
}

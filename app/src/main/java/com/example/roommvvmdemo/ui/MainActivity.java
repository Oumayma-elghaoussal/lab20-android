package com.example.roommvvmdemo.ui;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.roommvvmdemo.R;
import com.example.roommvvmdemo.data.local.Note;
import com.example.roommvvmdemo.viewmodel.NoteViewModel;

public class MainActivity extends AppCompatActivity {

    private NoteViewModel noteViewModel;
    private EditText editTextTitle;
    private EditText editTextDescription;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editTextTitle = findViewById(R.id.edit_text_title);
        editTextDescription = findViewById(R.id.edit_text_description);
        Button buttonAddNote = findViewById(R.id.button_add_note);
        Button buttonDeleteAll = findViewById(R.id.button_delete_all);

        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);

        NoteAdapter adapter = new NoteAdapter();
        recyclerView.setAdapter(adapter);

        noteViewModel = new ViewModelProvider(this).get(NoteViewModel.class);
        noteViewModel.getAllNotes().observe(this, adapter::setNotes);

        buttonAddNote.setOnClickListener(v -> {
            String title = editTextTitle.getText().toString().trim();
            String description = editTextDescription.getText().toString().trim();

            if (title.isEmpty() || description.isEmpty()) {
                Toast.makeText(this, "Veuillez entrer un titre et une description", Toast.LENGTH_SHORT).show();
                return;
            }

            Note note = new Note(title, description);
            noteViewModel.insert(note);

            editTextTitle.setText("");
            editTextDescription.setText("");
            Toast.makeText(this, "Note ajoutée", Toast.LENGTH_SHORT).show();
        });

        buttonDeleteAll.setOnClickListener(v -> {
            noteViewModel.deleteAllNotes();
            Toast.makeText(this, "Toutes les notes ont été supprimées", Toast.LENGTH_SHORT).show();
        });

        adapter.setOnItemLongClickListener(note -> {
            noteViewModel.delete(note);
            Toast.makeText(this, "Note supprimée", Toast.LENGTH_SHORT).show();
        });
    }
}

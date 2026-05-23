package com.example.hellotoast;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

/**
 * LAB 18 — MainActivity : ViewModel + LiveData vs variable classique.
 *
 * Démontre :
 *   1) Variable classique → remise à 0 après rotation (Activity re-créée)
 *   2) ViewModel + LiveData → compteur intact après rotation
 *   3) postValue() depuis un background thread
 *   4) Informations lifecycle (nombre de créations, instance ViewModel)
 *
 * Concepts internes :
 *   - LifecycleOwner : l'Activity elle-même (this)
 *   - Observer : lambda dans observe() — appelé seulement quand l'Activity est STARTED/RESUMED
 *   - ViewModelStore : conteneur interne qui garde le ViewModel vivant entre les re-créations
 *   - ViewModelProvider : récupère ou crée le ViewModel depuis le ViewModelStore
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    // Compteur de créations d'Activity (pour prouver la re-création à la rotation)
    private static int activityCreateCount = 0;

    // ── Variable classique (PERDUE à la rotation) ──
    private int classicCount = 0;

    // ── ViewModel (SURVIT à la rotation) ──
    private CounterViewModel viewModel;

    // Vues
    private TextView tvClassicCount, tvViewModelCount, tvInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        activityCreateCount++;
        Log.i(TAG, "═══ onCreate() — Activity créée " + activityCreateCount + " fois ═══");

        // Binding
        tvClassicCount   = findViewById(R.id.tvClassicCount);
        tvViewModelCount = findViewById(R.id.tvViewModelCount);
        tvInfo           = findViewById(R.id.tvInfo);

        Button btnClassicInc  = findViewById(R.id.btnClassicIncrement);
        Button btnIncrement   = findViewById(R.id.btnIncrement);
        Button btnDecrement   = findViewById(R.id.btnDecrement);
        Button btnReset       = findViewById(R.id.btnReset);
        Button btnAddBg       = findViewById(R.id.btnAddBackground);

        // ══════════════════════════════════════════
        //  Obtenir le ViewModel
        // ══════════════════════════════════════════
        // ViewModelProvider cherche dans le ViewModelStore :
        //   - 1er appel → crée une nouvelle instance
        //   - Après rotation → retourne la MÊME instance
        viewModel = new ViewModelProvider(this).get(CounterViewModel.class);

        // ══════════════════════════════════════════
        //  Observer le LiveData
        // ══════════════════════════════════════════
        // observe(LifecycleOwner, Observer)
        //   - LifecycleOwner = this (l'Activity)
        //   - L'Observer est appelé SEULEMENT quand l'Activity est active (STARTED/RESUMED)
        //   - Pas besoin de désenregistrer manuellement → lifecycle-aware
        viewModel.getCount().observe(this, count -> {
            tvViewModelCount.setText(String.valueOf(count));
            Log.d(TAG, "LiveData observe → UI mise à jour : " + count);
        });

        // ══════════════════════════════════════════
        //  Section 1 : Variable classique
        // ══════════════════════════════════════════
        tvClassicCount.setText(String.valueOf(classicCount)); // toujours 0 après rotation

        btnClassicInc.setOnClickListener(v -> {
            classicCount++;
            tvClassicCount.setText(String.valueOf(classicCount));
            Log.d(TAG, "classicCount = " + classicCount);
        });

        // ══════════════════════════════════════════
        //  Section 2 : ViewModel + LiveData
        // ══════════════════════════════════════════
        btnIncrement.setOnClickListener(v -> viewModel.increment());

        btnDecrement.setOnClickListener(v -> viewModel.decrement());

        btnReset.setOnClickListener(v -> {
            viewModel.reset();
            Toast.makeText(this, "Compteur réinitialisé", Toast.LENGTH_SHORT).show();
        });

        // postValue() depuis un background thread
        btnAddBg.setOnClickListener(v -> {
            Toast.makeText(this, "Ajout de 10 en background…", Toast.LENGTH_SHORT).show();
            viewModel.addFromBackground();
        });

        // ══════════════════════════════════════════
        //  Section 3 : Informations lifecycle
        // ══════════════════════════════════════════
        updateInfo();
    }

    private void updateInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("Activity.onCreate() appelé : ").append(activityCreateCount).append(" fois\n");
        sb.append("ViewModel instance #").append(viewModel.getInstanceId()).append("\n");
        sb.append("→ Si le numéro d'instance ne change pas\n");
        sb.append("  après rotation, le ViewModel a survécu !\n\n");
        sb.append("classicCount = ").append(classicCount).append(" (toujours 0 après rotation)\n");
        sb.append("viewModel count = ").append(viewModel.getCount().getValue()).append(" (intact !)\n\n");
        sb.append("─── Concepts ───\n");
        sb.append("• LifecycleOwner = this (Activity)\n");
        sb.append("• ViewModelStore garde le VM vivant\n");
        sb.append("• Observer reçoit les updates auto\n");
        sb.append("• setValue = main thread\n");
        sb.append("• postValue = any thread\n");
        tvInfo.setText(sb.toString());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy() — isFinishing=" + isFinishing());
        // Si isFinishing() == true → l'utilisateur a quitté (back), ViewModel sera détruit
        // Si isFinishing() == false → rotation, ViewModel SURVIT
    }
}

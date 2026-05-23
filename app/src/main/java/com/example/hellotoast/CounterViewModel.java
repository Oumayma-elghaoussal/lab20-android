package com.example.hellotoast;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

/**
 * LAB 18 — CounterViewModel.
 *
 * ViewModel survit aux changements de configuration (rotation, thème, etc.)
 * car il est stocké dans le ViewModelStore de l'Activity.
 *
 * Cycle de vie :
 *   Activity.onCreate()  →  ViewModelProvider.get(CounterViewModel.class)
 *                            ↓
 *                  1er appel → constructeur exécuté
 *                  2e+ appel → même instance réutilisée
 *                            ↓
 *   Activity.onDestroy() (rotation) → ViewModel SURVIT
 *   Activity.finish()    (vraie fin) → onCleared() appelé, ViewModel détruit
 *
 * MutableLiveData vs LiveData :
 *   - MutableLiveData : lecture + écriture (privé au ViewModel)
 *   - LiveData        : lecture seule (exposé à l'Activity via getter)
 *   → l'Activity ne peut PAS modifier la valeur directement (encapsulation)
 *
 * setValue() vs postValue() :
 *   - setValue()  : MAIN thread uniquement (synchrone, immédiat)
 *   - postValue() : depuis N'IMPORTE quel thread (asynchrone, posté au main thread)
 */
public class CounterViewModel extends ViewModel {

    private static final String TAG = "CounterViewModel";

    // MutableLiveData = lecture + écriture (privé)
    private final MutableLiveData<Integer> count = new MutableLiveData<>(0);

    // Compteur de créations (pour prouver que le ViewModel est réutilisé)
    private final int instanceId;
    private static int instanceCounter = 0;

    public CounterViewModel() {
        instanceId = ++instanceCounter;
        Log.i(TAG, "═══ CounterViewModel CRÉÉ (instance #" + instanceId + ") ═══");
    }

    // ── API publique ──────────────────────────────

    /** LiveData = lecture seule (exposé à l'Activity). */
    public LiveData<Integer> getCount() {
        return count;
    }

    /** Incrémenter de 1 (main thread → setValue). */
    public void increment() {
        Integer current = count.getValue();
        count.setValue((current != null ? current : 0) + 1);
        Log.d(TAG, "increment → " + count.getValue());
    }

    /** Décrémenter de 1 (main thread → setValue). */
    public void decrement() {
        Integer current = count.getValue();
        count.setValue((current != null ? current : 0) - 1);
        Log.d(TAG, "decrement → " + count.getValue());
    }

    /** Réinitialiser à 0. */
    public void reset() {
        count.setValue(0);
        Log.d(TAG, "reset → 0");
    }

    /**
     * Ajouter 10 depuis un background thread.
     * Utilise postValue() au lieu de setValue() car on est hors du main thread.
     */
    public void addFromBackground() {
        new Thread(() -> {
            try {
                // Simuler un travail réseau/DB
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            Integer current = count.getValue();
            int newVal = (current != null ? current : 0) + 10;

            // postValue() = thread-safe (posté au main thread)
            count.postValue(newVal);
            Log.d(TAG, "addFromBackground (postValue) → " + newVal
                    + " [thread=" + Thread.currentThread().getName() + "]");
        }).start();
    }

    /** Retourne l'ID d'instance (pour debug). */
    public int getInstanceId() {
        return instanceId;
    }

    // ── Nettoyage ─────────────────────────────────

    /**
     * Appelé quand l'Activity est définitivement détruite (finish()).
     * Libérer les ressources ici (annuler des requêtes, fermer des streams...).
     */
    @Override
    protected void onCleared() {
        super.onCleared();
        Log.i(TAG, "═══ onCleared() — ViewModel #" + instanceId + " DÉTRUIT ═══");
    }
}

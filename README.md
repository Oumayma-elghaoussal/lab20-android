# LAB 18 — ViewModel et LiveData en Android

## Objectifs

| # | Objectif |
|---|----------|
| 1 | Comprendre pourquoi une variable classique est **perdue à chaque rotation** |
| 2 | Voir la limite de `onSaveInstanceState()` (ancienne méthode) |
| 3 | Maîtriser **ViewModel** (survit à la destruction/re-création de l'Activity) |
| 4 | Maîtriser **LiveData** (lifecycle-aware, met à jour l'UI quand l'Activity est active) |
| 5 | Découvrir les concepts internes : `LifecycleOwner`, `Observer`, `ViewModelStore` |
| 6 | Comprendre `MutableLiveData` vs `LiveData`, `setValue` vs `postValue` |
| 7 | Tester des scénarios réels (rotation, thème, background thread) |

---

## Architecture

```
app/src/main/java/com/example/hellotoast/
├── CounterViewModel.java   ← ViewModel + MutableLiveData
└── MainActivity.java       ← UI + observe LiveData + variable classique

app/src/main/res/
├── layout/activity_main.xml
└── values/ (strings, colors, themes)
```

---

## Le problème : pourquoi la variable classique est perdue ?

Quand l'écran tourne, Android **détruit** et **re-crée** l'Activity :

```
[Rotation]
Activity.onDestroy() → Activity re-créée → onCreate()
                                            ↓
                                    toutes les variables
                                    sont réinitialisées !
```

```java
// PROBLÈME : classicCount revient à 0 après rotation
private int classicCount = 0;
```

---

## La solution : ViewModel

Le **ViewModel** est stocké dans un `ViewModelStore` qui survit à la re-création :

```
Activity #1          Activity #2 (après rotation)
    ↓                      ↓
ViewModelStore ←──────── ViewModelStore (MÊME instance)
    ↓                      ↓
CounterViewModel ←──── CounterViewModel (MÊME instance, count intact)
```

```java
// Le ViewModel est obtenu via ViewModelProvider
viewModel = new ViewModelProvider(this).get(CounterViewModel.class);
// 1er appel → crée le ViewModel
// Après rotation → retourne la MÊME instance
```

---

## LiveData : mise à jour automatique de l'UI

**LiveData** est **lifecycle-aware** : l'Observer n'est appelé que si l'Activity est active.

```java
// Dans le ViewModel (privé = écriture)
private final MutableLiveData<Integer> count = new MutableLiveData<>(0);

// Exposé en lecture seule
public LiveData<Integer> getCount() { return count; }

// Dans l'Activity (observe)
viewModel.getCount().observe(this, count -> {
    tvCounter.setText(String.valueOf(count));
});
// → PAS besoin de désenregistrer manuellement !
```

---

## MutableLiveData vs LiveData

| | `MutableLiveData` | `LiveData` |
|---|---|---|
| **Accès** | Lecture + Écriture | Lecture seule |
| **Utilisé par** | Le ViewModel (interne) | L'Activity (via getter) |
| **Méthodes** | `setValue()`, `postValue()` | `observe()`, `getValue()` |

**Pourquoi ?** → **Encapsulation** : l'Activity ne peut pas modifier la valeur directement.

---

## setValue() vs postValue()

| | `setValue()` | `postValue()` |
|---|---|---|
| **Thread** | Main thread UNIQUEMENT | N'importe quel thread |
| **Timing** | Synchrone (immédiat) | Asynchrone (posté au main) |
| **Usage** | Clic bouton, UI event | Réseau, BDD, worker thread |

```java
// Main thread
count.setValue(42);

// Background thread
new Thread(() -> {
    // travail long...
    count.postValue(42);  // thread-safe
}).start();
```

---

## Concepts internes

### LifecycleOwner
L'Activity implémente `LifecycleOwner`. Quand on écrit `observe(this, ...)`, le `this` permet à LiveData de savoir si l'Activity est active.

### ViewModelStore
Conteneur interne qui garde les ViewModel vivants entre les re-créations d'Activity. Détruit seulement quand `isFinishing() == true`.

### Observer
Lambda passée à `observe()`. Appelée **uniquement** quand :
- L'Activity est en état `STARTED` ou `RESUMED`
- La valeur du LiveData a changé

### onCleared()
Méthode appelée quand le ViewModel est **définitivement détruit** (Activity finish). Idéal pour libérer des ressources.

---

## Dépendances

```groovy
dependencies {
    implementation 'androidx.lifecycle:lifecycle-viewmodel:2.7.0'
    implementation 'androidx.lifecycle:lifecycle-livedata:2.7.0'
}
```

---

## Tests à faire

### Test 1 : Rotation
1. Incrémentez le compteur **classique** 5 fois → affiche `5`
2. Incrémentez le compteur **ViewModel** 15 fois → affiche `15`
3. **Tournez l'écran** (Ctrl+F11 sur émulateur)
4. **Résultat :** classique = `0` ❌ | ViewModel = `15` ✅

### Test 2 : Changement de thème
1. Incrémentez → rotation → changement sombre/clair
2. Le compteur ViewModel reste intact

### Test 3 : Background thread (postValue)
1. Cliquez "**+ 10 (Background Thread)**"
2. Après 500ms le compteur augmente de 10
3. Vérifiez dans Logcat : `[thread=Thread-X]`

### Test 4 : Sans LiveData
1. Commentez la ligne `viewModel.getCount().observe(...)` dans MainActivity
2. Cliquez Incrémenter → l'UI ne se met **plus** à jour automatiquement
3. → Prouve que c'est LiveData qui pousse les updates

### Test 5 : Logcat
Filtrez par `CounterViewModel` ou `MainActivity` :

```
I/CounterViewModel: ═══ CounterViewModel CRÉÉ (instance #1) ═══
I/MainActivity: ═══ onCreate() — Activity créée 1 fois ═══
D/CounterViewModel: increment → 1
D/MainActivity: LiveData observe → UI mise à jour : 1
[ROTATION]
I/MainActivity: onDestroy() — isFinishing=false
I/MainActivity: ═══ onCreate() — Activity créée 2 fois ═══
D/MainActivity: LiveData observe → UI mise à jour : 1   ← INTACT !
```
> ⚠️ Notez que `CounterViewModel CRÉÉ` n'apparaît qu'**une seule fois** !

---

## Comparatif : anciennes méthodes vs ViewModel

| Méthode | Survit rotation | Données complexes | Lifecycle-aware |
|---|---|---|---|
| Variable classique | ❌ | ✅ | ❌ |
| `onSaveInstanceState()` | ✅ | ❌ (Bundle limité) | ❌ |
| **ViewModel + LiveData** | **✅** | **✅** | **✅** |

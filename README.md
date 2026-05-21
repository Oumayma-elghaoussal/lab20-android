# LAB 12 — GPS et Map (Google Maps Activity)

## 📋 Objectifs

- Afficher une **Google Map** dans l'application
- Demander la **permission de localisation** à l'exécution
- Écouter les changements de position via **Network** et **GPS** providers
- Ajouter un **marker** à chaque nouvelle position détectée
- Si le GPS est désactivé : afficher une **boîte de dialogue** pour l'activer
- **Zoomer** automatiquement sur la position courante

---

## 🏗️ Architecture du projet

```
app/src/main/
├── AndroidManifest.xml               # Permissions + API Key Google Maps
├── java/com/example/hellotoast/
│   └── MapsActivity.java             # Activité Maps + LocationListener
└── res/
    ├── layout/
    │   └── activity_maps.xml          # SupportMapFragment + info card
    ├── drawable/
    │   └── ic_launcher_foreground.xml
    └── values/
        ├── strings.xml
        ├── colors.xml
        └── themes.xml
```

---

## 🔑 Configuration de la clé API Google Maps

### Étape 1 — Obtenir une clé API

1. Aller sur [Google Cloud Console](https://console.cloud.google.com/)
2. Créer un projet ou en sélectionner un existant
3. Activer l'API **Maps SDK for Android**
4. Aller dans **APIs & Services → Credentials**
5. Cliquer sur **Create Credentials → API Key**
6. Copier la clé

### Étape 2 — Insérer la clé dans le projet

Ouvrir `AndroidManifest.xml` et remplacer `YOUR_API_KEY_HERE` :

```xml
<meta-data
    android:name="com.google.android.geo.API_KEY"
    android:value="VOTRE_CLE_API_ICI" />
```

> ⚠️ **Sans clé API valide, la carte affichera un écran gris/vide.**

---

## 📱 Fonctionnalités

| Fonctionnalité | Description |
|----------------|-------------|
| **Google Map** | Carte plein écran avec `SupportMapFragment` |
| **Permission runtime** | Demande `ACCESS_FINE_LOCATION` + `ACCESS_COARSE_LOCATION` |
| **GPS Provider** | Écoute les positions du capteur GPS (marker 🔴 rouge) |
| **Network Provider** | Écoute les positions réseau/WiFi (marker 🔵 bleu) |
| **Dialog GPS** | Si GPS désactivé → popup proposant d'ouvrir les paramètres |
| **Zoom auto** | `animateCamera` zoom niveau 16 sur chaque nouvelle position |
| **Info panel** | Card flottante en bas : numéro position, lat/lng, provider |

---

## 🔧 Technologies utilisées

| Composant | Technologie |
|-----------|-------------|
| Carte | Google Maps SDK (`play-services-maps:18.2.0`) |
| Localisation | `LocationManager` (GPS_PROVIDER + NETWORK_PROVIDER) |
| UI | Material Components (MaterialCardView) |
| Permissions | Runtime permissions (Android 6+) |

---

## 🔒 Permissions Android

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

---

## 🚀 Comment exécuter

1. **Configurer la clé API** dans `AndroidManifest.xml` (voir ci-dessus)
2. Ouvrir le projet dans **Android Studio**
3. **Sync Gradle** → **Run**
4. Sur l'émulateur : aller dans **Extended controls (⋯) → Location** pour simuler une position GPS
5. L'application :
   - Demande la permission de localisation
   - Affiche la carte Google Maps
   - Place un marker rouge (GPS) ou bleu (Network) à chaque position
   - Zoome automatiquement sur la dernière position

---

## 🧪 Tester sur l'émulateur

Pour simuler des positions GPS sur l'émulateur Android :

1. Lancer l'app sur l'émulateur
2. Ouvrir les **Extended Controls** (bouton `⋯` dans la barre de l'émulateur)
3. Aller dans l'onglet **Location**
4. Entrer des coordonnées (ex: `33.9716, -6.8498` pour Rabat)
5. Cliquer sur **Set Location**
6. Un marker apparaît sur la carte et l'info panel se met à jour

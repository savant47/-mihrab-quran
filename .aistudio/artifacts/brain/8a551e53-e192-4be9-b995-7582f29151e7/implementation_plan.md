# محرابي (Mihrabi) - Waqf Quranic Tracker & Offline Companion

A 100% Free, Ad-Free, Waqf (لوجه الله) Quranic Tracker & Offline AI Companion Android Application built with Jetpack Compose, Material Design 3, Firebase Cloud Firestore with offline caching, Google Authentication via Credential Manager API, and an instant On-Device AI Engine.

> [!IMPORTANT]
> **User Choices & Master Architect Directives**:
> 1. **100% Offline AI Engine**: Zero API calls or external LLM server costs. Uses an embedded instant On-Device Quranic Knowledge & Reflection Engine (`OnDeviceAiManager`) with MediaPipe GenAI integration for authentic concise Tafsir, Asbab al-Nuzul, and customized parent/family Duas.
> 2. **Master Admin Control Panel (savant47@gmail.com)**: Exclusive admin dashboard for `savant47@gmail.com` with global read access to observe user counts, active Khatmas, daily readers, and detailed reading progress.
> 3. **Firebase Firestore Sync with Offline Persistence**: Seamless real-time sync when connected, with full offline caching when disconnected.
> 4. **Visual Style & Sharing**: Classic Islamic aesthetic with dark/light themes, Amiri Arabic typography, and daily reflection card sharing with parent Duas.

---

## 1. Overview & Core System Architecture

```
┌────────────────────────────────────────────────────────────────────────┐
│                        Google Sign-In Gate                             │
│                  (Jetpack Credential Manager API)                      │
└──────────────────────────────────┬─────────────────────────────────────┘
                                   │
                                   ▼
┌────────────────────────────────────────────────────────────────────────┐
│                        Role-Based Access Control                       │
│     Is Email == "savant47@gmail.com"? ──► Assign role = "admin"        │
└─────────────────┬──────────────────────────────────┬───────────────────┘
                  │                                  │
                  ▼                                  ▼
┌──────────────────────────────────┐┌──────────────────────────────────┐
│   User Main Dashboard Screen     ││     Admin Control Dashboard      │
│  - Circular Progress (604 pages) ││   (Exclusive: savant47@gmail.com) │
│  - Metrics (Rub's & Pace)        ││  - Total Users & Khatma Counts   │
│  - Stop Point Input Dialog       ││  - Live Users Progress Table     │
│  - Instant Offline AI Reflection ││  - Search & Filter by Progress   │
│  - Share Reflection Card         │└──────────────────────────────────┘
└─────────────────┬────────────────┘
                  │
                  ▼
┌────────────────────────────────────────────────────────────────────────┐
│                     OnDeviceAiManager (100% Offline)                   │
│   - Ingests read verses/pages                                         │
│   - Synthesizes authentic Tafsir, Asbab al-Nuzul, & Parent Dua         │
│   - MediaPipe GenAI Tasks LlmInference / Offline Embedded Knowledge   │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Cloud Data Strategy & Security Schema

### Collection `users` (`users/{uid}`)
- `uid`: String (Document ID & PK)
- `email`: String
- `displayName`: String
- `role`: String ("user" | "admin") -> set to "admin" if `email.equals("savant47@gmail.com", ignoreCase = true)`
- `currentKhatmaNumber`: Int (e.g. 1)
- `currentSurah`: String (e.g. "البقرة")
- `currentAyah`: Int (e.g. 255)
- `currentPage`: Int (1 to 604)
- `completedRubCount`: Int (0 to 240)
- `progressPercentage`: Double (0.00 to 100.00)
- `lastActiveDate`: Timestamp (@ServerTimestamp)

### Subcollection `reading_logs` (`users/{uid}/reading_logs/{logId}`)
- `logId`: String
- `date`: Timestamp
- `surah`: String
- `ayah`: Int
- `pagesReadToday`: Int
- `reflectionSummary`: Map / String (Tafsir, Asbab al-Nuzul, Parents Dua)

### Firestore Security Rules (`firestore.rules`)
```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    // Master Admin Helper Function
    function isMasterAdmin() {
      return request.auth != null && 
             request.auth.token.email.lower() == 'savant47@gmail.com';
    }
    
    // User Profile Rules
    match /users/{userId} {
      allow read: if request.auth != null && (request.auth.uid == userId || isMasterAdmin());
      allow create, update: if request.auth != null && request.auth.uid == userId;
      allow delete: if request.auth != null && request.auth.uid == userId;
      
      // User Reading Logs Subcollection Rules
      match /reading_logs/{logId} {
        allow read: if request.auth != null && (request.auth.uid == userId || isMasterAdmin());
        allow write: if request.auth != null && request.auth.uid == userId;
      }
    }
  }
}
```

---

## 3. Mathematical & Offline AI Engine Specifications

### 1. Mathematical Accuracy Invariants
- Total Pages: `604` (Madinah Mus-haf)
- Total Rub's: `240` (60 Hizbs / 30 Ajza')
- Base Pace: `1 Rub'` (~2.5 pages) per day
- Percentage Formula: `(currentPage / 604.0) * 100.0`
- Remaining Pages: `604 - currentPage`
- Remaining Rub's: `240 - completedRubCount`
- Projected Completion: `CurrentDate + (Remaining Pages / Actual Daily Pace)`

### 2. OnDeviceAiManager (Offline AI Engine)
- Zero server/API dependency.
- Generates 3 authentic sections:
  1. **التفسير الجامع والمختصر**: Core themes & clear explanation of the read section.
  2. **أسباب النزول**: Authentic occasions of revelation from classical sources (Ibn Kathir, Al-Tabari, Al-Qurtubi).
  3. **دعاء البر بالوالدين**: Dedicated Dua for parents and family.

---

## 4. Execution Plan & Implementation Steps

### Phase 1: Environment & Dependencies
1. Provision Firebase Firestore backend using `set_up_firebase` tool (`requestedProducts: ["firestore"]`).
2. Add MediaPipe Tasks GenAI dependency (`com.google.mediapipe:tasks-genai`) to `app/build.gradle.kts`.
3. Install Quranic local font `amiri` using `font-util install "Amiri" --target app/src/main/res/font`.
4. Create `QuranDataHelper` containing all 114 Surahs, page ranges, Hizb/Rub' mappings.

### Phase 2: Security Rules & Test Gate
1. Write `firebase-blueprint.json` and `firestore.rules`.
2. Configure `firebase.json` for Firestore emulator.
3. Author `firestore.test.js` JavaScript rules unit tests (`node --test firestore.test.js`).
4. Author `UserRepositoryRuleTest.kt` Robolectric unit tests.
5. Deploy security rules to Firebase backend via `deploy_firebase` tool.

### Phase 3: Core Logic & Repositories
1. `OnDeviceAiManager.kt`: Implement local offline reflection and prompt processing engine using MediaPipe LlmInference & offline Quranic knowledge synthesizer.
2. `AuthRepository.kt`: Implement Credential Manager Google Sign-In with auto-admin assignment for `savant47@gmail.com`.
3. `KhatmaRepository.kt`: Implement Firestore offline-cached user profile and reading log flows.

### Phase 4: Jetpack Compose User Interface
1. `KhatmaTheme.kt`: Islamic visual styling (Royal Navy, Emerald Green, Warm Gold, Parchment).
2. `MainDashboardScreen.kt`:
   - Circular Khatma completion arc.
   - Telemetry metrics grid (Remaining pages, Rub's, Pace, Target date).
   - Quick stop point dialog (Surah picker + Ayah or Page).
   - Offline AI Reflection card with one-click Image/Text sharing.
   - Top Bar with Admin Crown Icon (visible ONLY for `savant47@gmail.com`).
3. `AdminDashboardScreen.kt`:
   - Admin KPI metrics (Total Users, Active Khatmas, Daily Active Readers).
   - Search & Filter bar by email or progress.
   - Live Users Progress LazyColumn showing Email, Name, Current Surah/Ayah, Page, Rub's, and Last Sync Timestamp.
4. `AuthScreen.kt`: Dignified Islamic sign-in view.

### Phase 5: Verification
1. Run `compile_applet` to verify compilation.
2. Run security grep test ensuring zero unauthorized auth fallbacks.

---

## 5. Verification Checklist

- [ ] `compile_applet` completes successfully without errors.
- [ ] Offline AI Engine operates 100% locally without external API keys.
- [ ] Master Admin role automatically assigned and enforced for `savant47@gmail.com`.
- [ ] Admin Control Panel Screen accessible ONLY by `savant47@gmail.com`.
- [ ] Firestore Security Rules enforce user privacy and master read access for admin.
- [ ] Reflection sharing exports clean cards with Dua for parents.

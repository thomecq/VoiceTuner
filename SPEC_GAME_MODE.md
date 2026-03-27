# Tryb Gry — Specyfikacja

## Cel

Wciagajacy tryb cwiczenia sluchu muzycznego dla dzieci. Losowe dzwieki/akordy, zbieranie punktow, seria 10 rund. Kluczowa zasada: **dziecko nie widzi nazwy odgrywanego dzwieku** — cwiczenie sluchu, nie wzroku.

---

## Dwa podtryby gry

### 1. Pojedyncze nuty
- Losowa nuta z zakresu C4–H5 (2 oktawy — naturalny zakres dzieciecego glosu)
- Nuta jest odgrywana, dziecko spiewa "laaaa"
- Ocena celnosci

### 2. Akordy (2–3 nuty)
- Losowy akord 2- lub 3-tonowy (interwaly: tercja, kwarta, kwinta — brzmiace naturalnie)
- Akord jest odgrywany
- Dziecko musi:
  1. **Wybrac ile dzwiekow uslyszalo** (2 lub 3) — dodatkowe punkty za poprawna odpowiedz
  2. **Zaspiewac kazda nute po kolei** (od najnizszej do najwyzszej) — punkty za kazda trafiona nute

---

## Dwa poziomy trudnosci

### Latwy
- **W trakcie spiewania** widoczna jest graficzna reprezentacja wysokosci (wskaznik pitch) — dziecko widzi na zywo czy spiewa za wysoko/za nisko i moze skorygowac
- Nie widzi nazwy nuty, ale widzi wskazowke wizualna
- Wiecej czasu na odpowiedz

### Standardowy
- **Zadnej informacji zwrotnej w trakcie spiewania** — ekran pokazuje tylko animacje nasluchiwania
- Wynik pojawia sie **dopiero po zakonczeniu spiewania**
- Pelna ocena sluchu bez pomocy

---

## Przebieg gry (flow)

### Ekran startowy gry
```
┌──────────────────────────────┐
│                              │
│      [ikona nuty/gry]        │
│                              │
│      TRYB GRY                │
│                              │
│  ┌────────────────────────┐  │
│  │  Pojedyncze nuty       │  │
│  └────────────────────────┘  │
│  ┌────────────────────────┐  │
│  │  Akordy                │  │
│  └────────────────────────┘  │
│                              │
│  Poziom:  [Latwy] [Standard] │
│                              │
│     [ START ]                │
│                              │
└──────────────────────────────┘
```

### Runda (pojedyncza nuta)
```
Stan 1: Odgrywanie
┌──────────────────────────────┐
│   Runda 3 / 10       120 pkt│
│   ★★★☆☆☆☆☆☆☆               │
│                              │
│        ♪  (animacja)         │
│     "Posluchaj..."           │
│                              │
│   [nazwa nuty UKRYTA]        │
│                              │
└──────────────────────────────┘

Stan 2: Spiewanie (tryb latwy)
┌──────────────────────────────┐
│   Runda 3 / 10       120 pkt│
│                              │
│      [wskaznik pitch]        │
│      (bez nazwy nuty!)       │
│                              │
│     "Spiewaj..."             │
│                              │
└──────────────────────────────┘

Stan 2: Spiewanie (tryb standardowy)
┌──────────────────────────────┐
│   Runda 3 / 10       120 pkt│
│                              │
│    (pulsujaca animacja       │
│     nasluchiwania)           │
│                              │
│     "Spiewaj..."             │
│                              │
└──────────────────────────────┘

Stan 3: Wynik rundy
┌──────────────────────────────┐
│   Runda 3 / 10       145 pkt│
│                              │
│   ★ SWIETNIE! +25 pkt       │
│                              │
│   Zagrano: C4                │
│   Zaspiewano: C4 (+5 ct)     │
│                              │
│      [Dalej →]               │
│                              │
└──────────────────────────────┘
```

### Runda (akord)
```
Stan 1: Odgrywanie
┌──────────────────────────────┐
│   Runda 5 / 10       200 pkt│
│                              │
│    ♪♪♪  (animacja akordu)    │
│     "Posluchaj akordu..."    │
│                              │
│  Mozesz odtworzyc ponownie:  │
│     [▶ Powtorz]              │
│                              │
└──────────────────────────────┘

Stan 2: Pytanie o liczbe dzwiekow
┌──────────────────────────────┐
│   Runda 5 / 10       200 pkt│
│                              │
│   Ile dzwiekow uslyszales?   │
│                              │
│     [2]     [3]              │
│                              │
└──────────────────────────────┘

Stan 3: Spiewanie nut po kolei
┌──────────────────────────────┐
│   Runda 5 / 10       200 pkt│
│                              │
│   Zaspiewaj nute 1 z 3       │
│   (od najnizszej)            │
│                              │
│   [wskaznik / animacja]      │
│                              │
└──────────────────────────────┘

Stan 4: Wynik rundy
┌──────────────────────────────┐
│   Runda 5 / 10       255 pkt│
│                              │
│   Akord: C4 + E4 + G4       │
│   Liczba nut: 3 ✓ (+10 pkt) │
│   Nuta 1: C4 ✓ (+20 pkt)    │
│   Nuta 2: E4 ~ (+10 pkt)    │
│   Nuta 3: G4 ✗ (+0 pkt)     │
│                              │
│      [Dalej →]               │
│                              │
└──────────────────────────────┘
```

### Ekran koncowy
```
┌──────────────────────────────┐
│                              │
│     [trophy / medal ikona]   │
│                              │
│      WYNIK KONCOWY           │
│        285 / 300             │
│                              │
│   ★★★★★★★★★☆  (9/10)       │
│                              │
│   Celne nuty:   8/10         │
│   Sredni blad:  12 ct        │
│   Najlepsza:    C4 (+2 ct)   │
│                              │
│   [Zagraj ponownie]          │
│   [Powrot]                   │
│                              │
└──────────────────────────────┘
```

---

## System punktacji

### Pojedyncze nuty (max 30 pkt / runde)

| Dokladnosc | Punkty | Ikona |
|------------|--------|-------|
| Perfekcyjnie (±5 ct) | 30 | ★ z efektem swiecenia |
| Swietnie (±15 ct) | 25 | ★ |
| Dobrze (±25 ct) | 20 | ☆ |
| Blisko (±40 ct) | 10 | ○ |
| Pudlo (>40 ct) | 0 | ✗ |

- **Bonus za serie** (streak): 3+ poprawne z rzedu → +5 pkt bonus za kazda kolejna
- **Max za 10 rund**: 300 pkt (10 × 30) + bonusy za serie

### Akordy (max 40 pkt / runde)

| Element | Punkty |
|---------|--------|
| Poprawna liczba dzwiekow | 10 |
| Kazda trafiona nuta (±25 ct) | 10 |
| Bled o polton | 5 |
| Pudlo | 0 |

- Max za akord 3-tonowy: 10 + 30 = 40 pkt
- Max za akord 2-tonowy: 10 + 20 = 30 pkt

### Wizualna reprezentacja wyniku rundy

- **30 pkt**: duza zlota gwiazda z animacja wybuchu/confetti
- **25 pkt**: gwiazda z lekkim swieceniem
- **20 pkt**: polpelna gwiazda
- **10 pkt**: male kolko
- **0 pkt**: X z delikatna animacja "sprobuj ponownie"

### Pasek postepu

Gorna czesc ekranu: 10 kropeczelek/gwiazdek reprezentujacych rundy.
- Pusta = jeszcze nie zagrana
- Wypelniona kolorem zaleznie od wyniku (zloty/zielony/zolty/szary/czerwony)
- Aktualna runda pulsuje

---

## Generowanie losowych dzwiekow

### Pojedyncze nuty
- Zakres: C4 (MIDI 60) do H5 (MIDI 83) — 2 oktawy
- Tylko biale klawisze w trybie latwy (C, D, E, F, G, A, H)
- Biale + czarne w trybie standardowy
- Bez powtarzania tej samej nuty 2× z rzedu

### Akordy
- Typy akordow (losowo):
  - **Durowy**: podstawa + tercja wielka (+4 poltonow) + kwinta (+7)
  - **Molowy**: podstawa + tercja mala (+3) + kwinta (+7)
  - **Interwaly 2-tonowe**: tercja (+3 lub +4), kwarta (+5), kwinta (+7)
- Zakres podstawy: C4 do G4 (zeby akord nie wyszedl poza zakres glosu)
- Mozliwosc powtorzenia akordu 1× (przycisk "Powtorz")

---

## Nawigacja i integracja

### Nowy tab w bottom navigation
- Ikona: `Icons.Filled.SportsEsports` / `Icons.Outlined.SportsEsports`
- Label: "Gra"
- Pozycja: druga (miedzy Klawiatura a Historia)

### Nowe ekrany
1. `GameSetupScreen` — wybor trybu, poziomu, start
2. `GamePlayScreen` — rozgrywka (rundy)
3. `GameResultScreen` — wynik koncowy

### Nowy ViewModel
- `GameViewModel` — stan gry, generowanie nut, punktacja, historia rund

---

## Modele danych

```kotlin
enum class GameType { SINGLE_NOTES, CHORDS }
enum class GameDifficulty { EASY, STANDARD }

data class GameConfig(
    val type: GameType,
    val difficulty: GameDifficulty,
    val roundCount: Int = 10
)

data class GameRound(
    val index: Int,              // 0-9
    val targetNotes: List<Note>, // 1 nuta lub 2-3 (akord)
    val isChord: Boolean
)

data class RoundResult(
    val round: GameRound,
    val detectedNotes: List<Note?>,
    val centsOffsets: List<Float>,
    val scores: List<Int>,
    val countGuess: Int?,         // ile dzwiekow zgadnieto (akordy)
    val countCorrect: Boolean?,   // czy zgadnieto poprawnie
    val totalScore: Int,
    val streakBonus: Int
)

enum class NoteScore(val points: Int, val label: String) {
    PERFECT(30, "Perfekcyjnie!"),
    GREAT(25, "Swietnie!"),
    GOOD(20, "Dobrze"),
    CLOSE(10, "Blisko"),
    MISS(0, "Pudlo")
}

data class GameState(
    val config: GameConfig,
    val rounds: List<GameRound>,
    val results: List<RoundResult>,
    val currentRoundIndex: Int,
    val totalScore: Int,
    val currentStreak: Int,       // ile poprawnych z rzedu
    val phase: GamePhase
)

enum class GamePhase {
    SETUP,          // wybor trybu
    PLAYING_NOTE,   // odgrywanie dzwieku
    COUNT_GUESS,    // pytanie o liczbe nut (akordy)
    SINGING,        // nasluchiwanie spiewu
    ROUND_RESULT,   // wynik rundy
    GAME_OVER       // wynik koncowy
}
```

---

## Pliki do stworzenia

```
app/src/main/kotlin/com/voicetuner/
├── model/
│   ├── GameConfig.kt        # GameConfig, GameType, GameDifficulty
│   ├── GameRound.kt         # GameRound, RoundResult, NoteScore
│   └── GameState.kt         # GameState, GamePhase
├── game/
│   └── NoteGenerator.kt     # logika losowania nut i akordow
├── viewmodel/
│   └── GameViewModel.kt     # caly stan gry, punktacja, streak
└── ui/
    └── screens/
        ├── GameSetupScreen.kt   # wybor trybu i startu
        ├── GamePlayScreen.kt    # rozgrywka — wszystkie stany rundy
        └── GameResultScreen.kt  # ekran koncowy z wynikami
```

## Pliki do zmodyfikowac

```
├── ui/navigation/AppNavigation.kt  # nowy tab "Gra", nowe routes
├── res/values/strings.xml          # nowe stringi PL
```

---

## Istniejace elementy do reuse

- `PitchIndicator` — w trybie latwy, podczas spiewania (bez nazwy nuty)
- `AudioEngine` + `PianoSynthesizer` — odgrywanie nut i akordow
- `PitchViewModel` logika detekcji — wyekstrahowac wspolna logike nagrywania/YIN do reusable
- Kolory z `Color.kt` — zloty dla perfect, zielony/zolty/czerwony dla poziomow
- Animacje z `MainScreen` (pulsowanie, fadeIn) — reuse w stanach gry

---

## Weryfikacja

1. Zagrac 10 rund w trybie pojedynczych nut (latwy + standardowy)
2. Zagrac 10 rund w trybie akordow
3. Sprawdzic ze nazwa nuty NIGDY nie pojawia sie podczas odgrywania/spiewania
4. Sprawdzic poprawnosc punktacji i bonusow za serie
5. Sprawdzic ze powtorzenie akordu dziala
6. Sprawdzic ekran koncowy — statystyki, gwiazdki
7. Sprawdzic nawigacje — tab "Gra", powrot do menu

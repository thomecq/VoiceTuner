# VoiceTuner — Specyfikacja projektu

## Cel
Aplikacja Android do przygotowania dzieci na egzaminy wstępne do szkoły muzycznej. Umożliwia zagranie dźwięku lub akordu na wirtualnym pianinie, a następnie nagranie głosu dziecka śpiewającego "laaaa" — aplikacja analizuje wysokość dźwięku i daje szczegółowy feedback.

## Funkcjonalności

### Wirtualne pianino
- 3 oktawy (C3–B5), 36 klawiszy
- Rysowane na Canvas (nie osobne composable per klawisz)
- Multi-touch dla akordów
- Polska notacja muzyczna (H zamiast B)

### Synteza dźwięku
- Addytywna synteza z 8 harmonicznymi (amplitudy `A_n = 1/n^1.5`)
- Obwiednia ADSR: attack 5ms, decay 200ms, sustain 0.6, release 300ms
- Akordy: suma fal poszczególnych nut, normalizacja `1/N`
- Cache pre-compute'owanych nut przy starcie

### Detekcja wysokości dźwięku
- Algorytm YIN (de Cheveigné & Kawahara, 2002)
- Mikrofon: AudioRecord, 44100Hz, bufor 2048 samples
- Pipeline: MicrophoneCapture (Flow) → YinPitchDetector → PitchResult → median smoothing → PitchFeedback
- High-pass filter 80Hz (szumy pomieszczenia)
- Centy: `1200 * log2(detectedFreq / noteFreq)`

### Feedback
- Wizualny wskaźnik: za nisko / dobrze / za wysoko (kolorowy gauge)
- Dokładna różnica w centach i półtonach
- Historia prób (in-memory v1)
- Tolerancja konfigurowalna (domyślnie ±20 centów)

### Tryby
- Pojedyncze nuty
- Akordy

## Ekrany
1. **Klawiatura** (główny) — pianino w dolnej połowie, wskaźnik pitch + feedback w górnej
2. **Historia** — LazyColumn z listą prób (data, nuta docelowa, wykryta, celność)
3. **Ustawienia** — slider tolerancji, toggle trybu nuty/akordy

## Stack technologiczny
- Kotlin + Jetpack Compose
- Min SDK 26 (Android 8.0), Target SDK 35
- MVVM (ViewModel + StateFlow)
- Bez zewnętrznych bibliotek audio (synteza i detekcja od zera)
- Bez bazy danych (historia in-memory)
- Bez DI framework (manual constructor injection)

## Motyw kolorystyczny
- Primary: Deep blue (#1565C0)
- Secondary: Warm orange (#FF8F00)
- Correct: Green (#4CAF50)
- Too high: Red-orange (#FF5722)
- Too low: Blue (#2196F3)
- Background: Soft warm white (#FFF8E1)

## Język interfejsu
- Polski

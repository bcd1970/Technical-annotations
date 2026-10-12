# 48 — Sessions 44–46: Localization Phase 2

> **From 6 languages to 13, plus the terminology database, IATE pipeline, ISO 14224 failure codes, and ML Kit translation.** The foundation for cross-standard, cross-language field documentation.

---

## S-44 · 13-Language String Expansion

### ATOMS
| ID | Name | Size | Depends |
|----|------|------|---------|
| L10N-06 | Full 13-language strings | L | L10N-01 |

### CONTEXT

Phase 1 ships with 6 languages (en, de, fr, es, it, pt). This session adds 7 more: Dutch (nl), Polish (pl), Swedish (sv), Czech (cs), Romanian (ro), Chinese Simplified (zh-CN), Chinese Traditional (zh-TW).

### KEY CONSIDERATIONS

**Polish plurals:** Polish has complex plural categories (few/many). All `plurals` resources must include `few` and `many` categories:
```xml
<plurals name="annotation_count">
    <item quantity="one">%d adnotacja</item>
    <item quantity="few">%d adnotacje</item>
    <item quantity="many">%d adnotacji</item>
    <item quantity="other">%d adnotacji</item>
</plurals>
```

**Chinese:** Minimum 14sp body text (characters need more space). Test that shorter Chinese text doesn't break layouts designed for longer European strings. Both zh-CN (Simplified, PRC) and zh-TW (Traditional, Taiwan/HK) need separate string files.

**`resConfigs` update:** Add all 13 locales to `resConfigs` in `app/build.gradle.kts` to strip unused resources from AAB.

### DELIVERABLES
1. `values-nl/strings.xml` — Dutch
2. `values-pl/strings.xml` — Polish (with plurals)
3. `values-sv/strings.xml` — Swedish
4. `values-cs/strings.xml` — Czech
5. `values-ro/strings.xml` — Romanian
6. `values-zh-rCN/strings.xml` — Chinese Simplified
7. `values-zh-rTW/strings.xml` — Chinese Traditional

> **Note:** 7 files exceeds the 5-file rule. Split into S-44a (nl, pl, sv, cs, ro) and S-44b (zh-CN, zh-TW + `resConfigs` update) if needed.

### ACCEPTANCE
- All 13 languages render correctly in all screens
- Polish plurals correct for 0, 1, 2, 5, 21
- Chinese text at 14sp minimum, layouts don't break
- No untranslated strings visible in any locale
- `./gradlew assembleDebug` passes for all locales
- Locale switching (L10N-02 from Phase 1) works for new locales

---

## S-45 · Terminology Database + IATE + ISO 14224

### ATOMS
| ID | Name | Size | Depends |
|----|------|------|---------|
| L10N-07 | Terminology database (concept-ID) | M | PD-01, L10N-01 |
| L10N-08 | IATE TBX import pipeline | M | L10N-07 |
| L10N-09 | Structured annotation i18n (ISO 14224) | M | L10N-07, PD-04 |
| L10N-16 | Dual-standard reference fields | S | L10N-07 |

### TERMINOLOGY DATABASE (L10N-07)

Room tables implementing the concept-ID architecture from Doc 16. Every technical term gets a language-independent identifier mapping to standard-specific codes and localized terms.

```kotlin
@Entity
data class ConceptEntity(
    @PrimaryKey val conceptId: String,  // e.g., "WELD-FILLET-001"
    val domain: String,                  // e.g., "welding"
    val standardRefs: String,            // JSON: ["AWS A2.4", "ISO 2553"]
)

@Entity(
    foreignKeys = [ForeignKey(
        entity = ConceptEntity::class,
        parentColumns = ["conceptId"],
        childColumns = ["conceptId"],
    )]
)
data class TermEntity(
    @PrimaryKey(autoGenerate = true) val termId: Long = 0,
    val conceptId: String,
    val locale: String,           // "en", "de", "fr", etc.
    val term: String,             // "fillet weld", "Kehlnaht", "soudure d'angle"
    val source: String,           // "IATE", "manual", "IEC"
    val reliability: Int = 3,     // 1-4 (IATE scale)
)

@Dao
interface TerminologyDao {
    @Query("SELECT * FROM TermEntity WHERE conceptId = :conceptId")
    fun getTermsForConcept(conceptId: String): List<TermEntity>
    
    @Query("SELECT * FROM TermEntity WHERE term LIKE :query AND locale = :locale")
    fun searchTerms(query: String, locale: String): List<TermEntity>
    
    @Query("""
        SELECT c.*, t.term, t.locale FROM ConceptEntity c 
        JOIN TermEntity t ON c.conceptId = t.conceptId 
        WHERE t.term = :term AND t.locale = :sourceLocale
    """)
    fun lookupConcept(term: String, sourceLocale: String): List<ConceptWithTerms>
}
```

Target: ~2000 concepts × 5 languages = ~10,000 terms. <5MB database. <5ms lookup.

### IATE TBX IMPORT (L10N-08)

Build-time pipeline that parses IATE's TBX (TermBase eXchange, ISO 30042) export files, filters by domain (industry, technology), filters by reliability (≥3), and generates the pre-built SQLite database bundled with the app.

```kotlin
// Build-time script (Gradle task or standalone)
class IATETBXImporter {
    fun parseTBX(inputFile: File): List<TBXEntry>
    fun filterByDomain(entries: List<TBXEntry>, domains: Set<String>): List<TBXEntry>
    fun filterByReliability(entries: List<TBXEntry>, minReliability: Int): List<TBXEntry>
    fun mapToConceptEntities(entries: List<TBXEntry>): List<ConceptEntity>
    fun generateSQLite(concepts: List<ConceptEntity>, outputFile: File)
}
```

Supplemented with IEC Electropedia terms for electrical domain and manual cross-standard mappings for critical welding/GD&T/piping terms.

### ISO 14224 FAILURE CODES (L10N-09)

Structured annotation inputs using ISO 14224 coded defect types. Instead of free-text defect descriptions, the technician selects from coded lists that render as localized terms in any language.

```kotlin
enum class ISO14224Problem(val code: String, val conceptId: String) {
    ABNORMAL_INSTRUMENT_READING("AIR", "ISO14224-AIR"),
    BREAKDOWN("BRD", "ISO14224-BRD"),
    CORROSION("COR", "ISO14224-COR"),
    CRACKING("CRA", "ISO14224-CRA"),
    DEFORMATION("DEF", "ISO14224-DEF"),
    EROSION("ERO", "ISO14224-ERO"),
    EXTERNAL_LEAKAGE("ELU", "ISO14224-ELU"),
    // ~38 problem codes total
}

enum class ISO14224Cause(val code: String, val conceptId: String) {
    DESIGN_RELATED("DES", "ISO14224-CAUSE-DES"),
    FABRICATION_INSTALLATION("FAB", "ISO14224-CAUSE-FAB"),
    OPERATING_CONDITIONS("OPC", "ISO14224-CAUSE-OPC"),
    WEAR("WEA", "ISO14224-CAUSE-WEA"),
    // ~20 cause codes
}
```

Each code maps to a concept in the terminology database → automatically localized.

### DUAL-STANDARD FIELDS (L10N-16)

When a technician references a standard, the UI shows both conventions:
- "Fillet weld (AWS A2.4) / Kehlnaht (ISO 2553)"
- "AWG 12 / 3.31 mm²"

```kotlin
@Composable
fun DualStandardField(
    conceptId: String,
    primaryStandard: String,   // user's preferred standard
) {
    val terms = terminologyDb.getTermsForConcept(conceptId)
    val primary = terms.find { it.source == primaryStandard }
    val secondary = terms.find { it.source != primaryStandard }
    // Display: "primary term (primary standard) / secondary term (secondary standard)"
}
```

### DELIVERABLES
1. `TerminologyDatabase.kt` — Room entities + DAO
2. `IATETBXImporter.kt` — build-time TBX parser
3. `ISO14224Codes.kt` — problem + cause + detection code enums
4. `DualStandardResolver.kt` — ISO↔ASME↔DIN mapping
5. `terminology.db` — pre-built SQLite asset (~5MB)

### ACCEPTANCE
- <5ms concept lookup
- ~2000 concepts × 5 languages populated
- ISO 14224 codes render in user locale
- Dual-standard shows both conventions correctly
- TBX import filters correctly by domain + reliability

---

## S-46 · ML Kit Translation + Phrase Cache + Glossary

### ATOMS
| ID | Name | Size | Depends |
|----|------|------|---------|
| L10N-10 | ML Kit offline translation | M | L10N-07 |
| L10N-11 | Pre-translated phrase cache | S | L10N-07 |
| L10N-12 | Terminology glossary overlay | S | L10N-07, L10N-10 |

### ML KIT TRANSLATION (L10N-10)

Last-resort translation for free-text annotation notes. Uses ML Kit's on-device translation (~30MB per language pack, downloaded on demand over Wi-Fi only).

**Critical:** ML Kit is the LAST resort, not the primary translation method. The pipeline is:
1. **Terminology database** → exact match? Use it (always correct)
2. **Phrase cache** → previously translated? Use it (verified)
3. **ML Kit** → machine translate + glossary overlay warning

```kotlin
class TranslationPipeline(
    private val terminologyDb: TerminologyDao,
    private val phraseCache: PhraseCacheDao,
    private val mlKit: MLKitTranslator,
) {
    suspend fun translate(
        text: String,
        from: String,
        to: String,
    ): TranslationResult {
        // 1. Check terminology DB for exact concept match
        val conceptMatch = terminologyDb.lookupConcept(text, from)
        if (conceptMatch != null) return TranslationResult.Verified(conceptMatch.getTerm(to))
        
        // 2. Check phrase cache
        val cached = phraseCache.lookup(text, from, to)
        if (cached != null) return TranslationResult.Cached(cached.translation)
        
        // 3. ML Kit fallback with warning
        val mlResult = mlKit.translate(text, from, to)
        return TranslationResult.MachineTranslated(
            text = mlResult,
            warning = "Machine translated — verify technical accuracy",
        )
    }
}
```

### ML KIT TRANSLATOR

```kotlin
class MLKitTranslator {
    private val downloadedModels = mutableSetOf<String>()
    
    suspend fun ensureModel(languageCode: String) {
        // Download on Wi-Fi only
        // ~30MB per language pack
        // 13 languages supported
    }
    
    suspend fun translate(text: String, from: String, to: String): String
    fun isModelAvailable(languageCode: String): Boolean
}
```

### PHRASE CACHE (L10N-11)

Room table storing previously translated phrases. Populated from:
- User corrections of ML Kit translations
- Common phrases from anonymized annotation patterns (future: Phase 3 network intelligence)
- Manual seeding for critical phrases

```kotlin
@Entity
data class PhraseCacheEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sourceText: String,
    val sourceLocale: String,
    val targetText: String,
    val targetLocale: String,
    val source: String,           // "user_correction", "seed", "network"
    val useCount: Int = 0,
    val lastUsed: Long,
)
```

### GLOSSARY OVERLAY (L10N-12)

When ML Kit translation is used, a glossary overlay highlights potentially dangerous technical mistranslations. The overlay cross-references ML Kit output against the terminology database and flags terms where the machine translation differs from the verified term.

```kotlin
@Composable
fun GlossaryOverlay(
    translatedText: String,
    sourceLocale: String,
    targetLocale: String,
    terminologyDb: TerminologyDao,
) {
    // For each word/phrase in translated text:
    //   1. Check if it matches a known concept
    //   2. If ML Kit's translation differs from terminology DB's verified term:
    //      → Highlight in amber with tooltip: "Did you mean: [verified term]?"
    //   3. Unknown technical terms: underline in grey (unverified)
}
```

### DELIVERABLES
1. `MLKitTranslator.kt` — model management + translation
2. `TranslationPipeline.kt` — terminology → cache → ML Kit fallback
3. `PhraseCache.kt` — Room entity + DAO
4. `GlossaryOverlay.kt` — Compose overlay with term highlighting

### ACCEPTANCE
- ML Kit downloads on Wi-Fi only
- Translation pipeline follows correct priority order
- Glossary highlights terms where ML Kit differs from terminology DB
- Phrase cache reduces repeat ML Kit calls
- Machine-translated text always shows warning indicator

---

*April 2026 · TechNotes Phase 2 Sessions 44–46 v1.0*

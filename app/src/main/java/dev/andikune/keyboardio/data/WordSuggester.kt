package dev.andikune.keyboardio.data

import java.util.Locale
import kotlin.math.min

object WordSuggester {

    data class WordEntry(val word: String, val frequency: Int)

    // Spatial QWERTY keyboard adjacency map for calculating fat-finger touch distance
    private val QWERTY_NEIGHBORS: Map<Char, Set<Char>> = mapOf(
        'q' to setOf('w', 'a', 's'),
        'w' to setOf('q', 'e', 'a', 's', 'd'),
        'e' to setOf('w', 'r', 's', 'd', 'f'),
        'r' to setOf('e', 't', 'd', 'f', 'g'),
        't' to setOf('r', 'y', 'f', 'g', 'h'),
        'y' to setOf('t', 'u', 'g', 'h', 'j'),
        'u' to setOf('y', 'i', 'h', 'j', 'k'),
        'i' to setOf('u', 'o', 'j', 'k', 'l'),
        'o' to setOf('i', 'p', 'k', 'l'),
        'p' to setOf('o', 'l'),
        'a' to setOf('q', 'w', 's', 'z'),
        's' to setOf('w', 'e', 'a', 'd', 'z', 'x'),
        'd' to setOf('e', 'r', 's', 'f', 'x', 'c'),
        'f' to setOf('r', 't', 'd', 'g', 'c', 'v'),
        'g' to setOf('t', 'y', 'f', 'h', 'v', 'b'),
        'h' to setOf('y', 'u', 'g', 'j', 'b', 'n'),
        'j' to setOf('u', 'i', 'h', 'k', 'n', 'm'),
        'k' to setOf('i', 'o', 'j', 'l', 'm'),
        'l' to setOf('o', 'p', 'k'),
        'z' to setOf('a', 's', 'x'),
        'x' to setOf('z', 's', 'd', 'c'),
        'c' to setOf('x', 'd', 'f', 'v'),
        'v' to setOf('c', 'f', 'g', 'b'),
        'b' to setOf('v', 'g', 'h', 'n'),
        'n' to setOf('b', 'h', 'j', 'm'),
        'm' to setOf('n', 'j', 'k')
    )

    // Rich Indonesian Frequency Lexicon (Unigrams + Frequency Weights)
    // Curated from standard Indonesian corpus (wordfreq, Leipzig, KBBI) + common informal expressions
    private val FREQUENT_WORDS: List<WordEntry> = listOf(
        // High frequency Indonesian particles, pronouns, auxiliaries
        WordEntry("yang", 100), WordEntry("di", 99), WordEntry("dan", 98), WordEntry("ini", 97),
        WordEntry("dari", 96), WordEntry("untuk", 95), WordEntry("tidak", 94), WordEntry("dengan", 93),
        WordEntry("itu", 92), WordEntry("ada", 91), WordEntry("saya", 90), WordEntry("aku", 90),
        WordEntry("kamu", 90), WordEntry("ke", 89), WordEntry("akan", 88), WordEntry("bisa", 87),
        WordEntry("sudah", 86), WordEntry("juga", 85), WordEntry("pada", 84), WordEntry("mereka", 83),
        WordEntry("kami", 82), WordEntry("kita", 82), WordEntry("dia", 82), WordEntry("anda", 81),
        WordEntry("adalah", 80), WordEntry("karena", 80), WordEntry("lagi", 79), WordEntry("mau", 79),
        WordEntry("tapi", 78), WordEntry("tetapi", 77), WordEntry("atau", 77), WordEntry("hanya", 76),
        WordEntry("harus", 76), WordEntry("saja", 75), WordEntry("bukan", 75), WordEntry("belum", 74),
        WordEntry("oleh", 74), WordEntry("dalam", 73), WordEntry("saat", 73), WordEntry("seperti", 72),
        WordEntry("apa", 72), WordEntry("apakah", 71), WordEntry("siapa", 71), WordEntry("kapan", 70),
        WordEntry("dimana", 70), WordEntry("kemana", 69), WordEntry("darimana", 68), WordEntry("mengapa", 68),
        WordEntry("kenapa", 68), WordEntry("bagaimana", 67), WordEntry("gimana", 67), WordEntry("berapa", 66),

        // Common informal, chat, and slang terms (Prevents erroneous autocorrect!)
        WordEntry("iya", 85), WordEntry("ya", 85), WordEntry("oke", 84), WordEntry("ok", 84),
        WordEntry("siap", 83), WordEntry("mantap", 82), WordEntry("halo", 82), WordEntry("hai", 81),
        WordEntry("makasih", 80), WordEntry("terimakasih", 80), WordEntry("terima", 80), WordEntry("kasih", 80),
        WordEntry("gak", 79), WordEntry("nggak", 79), WordEntry("ngga", 78), WordEntry("tak", 77),
        WordEntry("udah", 78), WordEntry("blm", 75), WordEntry("kalo", 77), WordEntry("kalau", 77),
        WordEntry("nih", 76), WordEntry("tuh", 76), WordEntry("deh", 76), WordEntry("dong", 76),
        WordEntry("sih", 76), WordEntry("kan", 76), WordEntry("lah", 75), WordEntry("kok", 75),
        WordEntry("wkwk", 75), WordEntry("bener", 75), WordEntry("banget", 75), WordEntry("bgt", 74),
        WordEntry("kepo", 70), WordEntry("yoi", 70), WordEntry("bro", 72), WordEntry("kak", 73),
        WordEntry("mas", 72), WordEntry("mbak", 72), WordEntry("bang", 72), WordEntry("om", 70),

        // Common everyday verbs
        WordEntry("makan", 80), WordEntry("minum", 79), WordEntry("tidur", 79), WordEntry("bangun", 77),
        WordEntry("kerja", 78), WordEntry("bekerja", 77), WordEntry("belajar", 78), WordEntry("baca", 77),
        WordEntry("membaca", 76), WordEntry("tulis", 77), WordEntry("menulis", 76), WordEntry("dengar", 76),
        WordEntry("mendengar", 75), WordEntry("lihat", 78), WordEntry("melihat", 77), WordEntry("nonton", 76),
        WordEntry("menonton", 75), WordEntry("jalan", 78), WordEntry("berjalan", 77), WordEntry("lari", 75),
        WordEntry("duduk", 76), WordEntry("berdiri", 75), WordEntry("pergi", 78), WordEntry("pulang", 78),
        WordEntry("datang", 77), WordEntry("tiba", 76), WordEntry("sampai", 78), WordEntry("mulai", 77),
        WordEntry("selesai", 77), WordEntry("beli", 78), WordEntry("membeli", 76), WordEntry("jual", 76),
        WordEntry("menjual", 75), WordEntry("bayar", 77), WordEntry("ambil", 77), WordEntry("mengambil", 75),
        WordEntry("kasih", 78), WordEntry("memberi", 76), WordEntry("buka", 77), WordEntry("membuka", 76),
        WordEntry("tutup", 77), WordEntry("menutup", 75), WordEntry("kirim", 78), WordEntry("mengirim", 77),
        WordEntry("terima", 78), WordEntry("menerima", 76), WordEntry("cari", 77), WordEntry("mencari", 76),
        WordEntry("temu", 76), WordEntry("menemukan", 75), WordEntry("pikir", 77), WordEntry("berpikir", 76),
        WordEntry("ingat", 77), WordEntry("lupa", 77), WordEntry("tahu", 78), WordEntry("mengerti", 77),
        WordEntry("paham", 77), WordEntry("kenal", 76), WordEntry("suka", 78), WordEntry("cinta", 78),
        WordEntry("sayang", 78), WordEntry("bantu", 78), WordEntry("membantu", 77), WordEntry("tolong", 78),
        WordEntry("buat", 78), WordEntry("membuat", 77), WordEntry("pakai", 78), WordEntry("memakai", 77),
        WordEntry("punya", 78), WordEntry("memiliki", 76), WordEntry("tanya", 76), WordEntry("bertanya", 75),
        WordEntry("jawab", 76), WordEntry("menjawab", 75), WordEntry("bawa", 76), WordEntry("membawa", 75),
        WordEntry("tunggu", 77), WordEntry("menunggu", 76), WordEntry("simpan", 75), WordEntry("hapus", 75),

        // Common everyday nouns
        WordEntry("rumah", 80), WordEntry("sekolah", 78), WordEntry("kampus", 76), WordEntry("kantor", 78),
        WordEntry("toko", 77), WordEntry("pasar", 77), WordEntry("kamar", 77), WordEntry("jalan", 78),
        WordEntry("kota", 77), WordEntry("desa", 75), WordEntry("negara", 76), WordEntry("dunia", 77),
        WordEntry("orang", 80), WordEntry("manusia", 76), WordEntry("teman", 79), WordEntry("kawan", 77),
        WordEntry("sahabat", 77), WordEntry("keluarga", 78), WordEntry("ayah", 78), WordEntry("ibu", 78),
        WordEntry("bapak", 78), WordEntry("anak", 78), WordEntry("kakak", 77), WordEntry("adik", 77),
        WordEntry("suami", 77), WordEntry("istri", 77), WordEntry("pacar", 77), WordEntry("guru", 76),
        WordEntry("dokter", 76), WordEntry("polisi", 75), WordEntry("bos", 75), WordEntry("mobil", 77),
        WordEntry("motor", 78), WordEntry("sepeda", 76), WordEntry("kereta", 76), WordEntry("pesawat", 76),
        WordEntry("bus", 76), WordEntry("kapal", 75), WordEntry("uang", 79), WordEntry("harga", 78),
        WordEntry("biaya", 76), WordEntry("waktu", 80), WordEntry("hari", 80), WordEntry("malam", 79),
        WordEntry("pagi", 79), WordEntry("siang", 79), WordEntry("sore", 78), WordEntry("jam", 79),
        WordEntry("menit", 77), WordEntry("detik", 76), WordEntry("minggu", 78), WordEntry("bulan", 78),
        WordEntry("tahun", 79), WordEntry("sekarang", 79), WordEntry("nanti", 78), WordEntry("kemarin", 77),
        WordEntry("besok", 78), WordEntry("lusa", 75), WordEntry("pesan", 78), WordEntry("surat", 76),
        WordEntry("kabar", 78), WordEntry("berita", 77), WordEntry("foto", 77), WordEntry("gambar", 76),
        WordEntry("video", 77), WordEntry("musik", 76), WordEntry("lagu", 77), WordEntry("suara", 76),
        WordEntry("telepon", 77), WordEntry("hp", 78), WordEntry("laptop", 76), WordEntry("komputer", 76),
        WordEntry("keyboard", 78), WordEntry("internet", 77), WordEntry("wifi", 76), WordEntry("aplikasi", 77),
        WordEntry("game", 76), WordEntry("buku", 77), WordEntry("kertas", 75), WordEntry("pena", 75),
        WordEntry("meja", 76), WordEntry("kursi", 76), WordEntry("pintu", 76), WordEntry("jendela", 75),
        WordEntry("baju", 77), WordEntry("celana", 76), WordEntry("sepatu", 76), WordEntry("tas", 76),
        WordEntry("makanan", 78), WordEntry("minuman", 77), WordEntry("nasi", 78), WordEntry("air", 79),
        WordEntry("kopi", 78), WordEntry("teh", 77), WordEntry("susu", 77), WordEntry("roti", 76),
        WordEntry("kucing", 77), WordEntry("anjing", 76), WordEntry("burung", 75), WordEntry("ikan", 76),
        WordEntry("masalah", 78), WordEntry("solusi", 76), WordEntry("cara", 78), WordEntry("contoh", 77),
        WordEntry("nama", 79), WordEntry("nomor", 78), WordEntry("tempat", 78), WordEntry("lokasi", 77),

        // Common adjectives
        WordEntry("baik", 80), WordEntry("bagus", 80), WordEntry("keren", 79), WordEntry("hebat", 78),
        WordEntry("indah", 77), WordEntry("cantik", 78), WordEntry("ganteng", 77), WordEntry("pintar", 77),
        WordEntry("benar", 79), WordEntry("salah", 77), WordEntry("mudah", 77), WordEntry("gampang", 77),
        WordEntry("sulit", 76), WordEntry("susah", 76), WordEntry("cepat", 78), WordEntry("lambat", 76),
        WordEntry("pelan", 76), WordEntry("baru", 79), WordEntry("lama", 78), WordEntry("tua", 76),
        WordEntry("muda", 76), WordEntry("besar", 78), WordEntry("kecil", 78), WordEntry("panjang", 77),
        WordEntry("pendek", 76), WordEntry("tinggi", 77), WordEntry("rendah", 75), WordEntry("jauh", 77),
        WordEntry("dekat", 77), WordEntry("bersih", 76), WordEntry("kotor", 75), WordEntry("panas", 77),
        WordEntry("dingin", 77), WordEntry("sejuk", 75), WordEntry("lapar", 77), WordEntry("haus", 76),
        WordEntry("kenyang", 76), WordEntry("lelah", 76), WordEntry("capek", 77), WordEntry("senang", 78),
        WordEntry("bahagia", 77), WordEntry("sedih", 76), WordEntry("marah", 76), WordEntry("takut", 76),
        WordEntry("aman", 77), WordEntry("nyaman", 77), WordEntry("mahal", 77), WordEntry("murah", 77),
        WordEntry("gratis", 77), WordEntry("ramai", 76), WordEntry("sepi", 75), WordEntry("terang", 75),

        // Common English frequently typed on mobile
        WordEntry("the", 90), WordEntry("to", 90), WordEntry("and", 89), WordEntry("a", 88),
        WordEntry("of", 88), WordEntry("in", 87), WordEntry("is", 87), WordEntry("you", 87),
        WordEntry("that", 86), WordEntry("it", 86), WordEntry("he", 85), WordEntry("was", 85),
        WordEntry("for", 85), WordEntry("on", 85), WordEntry("are", 84), WordEntry("as", 84),
        WordEntry("with", 84), WordEntry("his", 83), WordEntry("they", 83), WordEntry("i", 85),
        WordEntry("at", 83), WordEntry("be", 83), WordEntry("this", 83), WordEntry("have", 83),
        WordEntry("from", 82), WordEntry("or", 82), WordEntry("one", 82), WordEntry("had", 81),
        WordEntry("by", 81), WordEntry("word", 80), WordEntry("but", 81), WordEntry("not", 82),
        WordEntry("what", 82), WordEntry("all", 81), WordEntry("were", 80), WordEntry("we", 81),
        WordEntry("when", 81), WordEntry("your", 82), WordEntry("can", 82), WordEntry("said", 80),
        WordEntry("there", 81), WordEntry("use", 80), WordEntry("an", 80), WordEntry("each", 79),
        WordEntry("which", 80), WordEntry("she", 80), WordEntry("do", 81), WordEntry("how", 81),
        WordEntry("their", 80), WordEntry("if", 80), WordEntry("will", 81), WordEntry("up", 80),
        WordEntry("other", 79), WordEntry("about", 80), WordEntry("out", 80), WordEntry("many", 79),
        WordEntry("then", 79), WordEntry("them", 79), WordEntry("these", 79), WordEntry("so", 80),
        WordEntry("some", 80), WordEntry("her", 79), WordEntry("would", 80), WordEntry("make", 80),
        WordEntry("like", 81), WordEntry("him", 79), WordEntry("into", 79), WordEntry("time", 80),
        WordEntry("has", 80), WordEntry("look", 79), WordEntry("two", 79), WordEntry("more", 80),
        WordEntry("write", 79), WordEntry("go", 80), WordEntry("see", 80), WordEntry("number", 78),
        WordEntry("no", 81), WordEntry("way", 79), WordEntry("could", 79), WordEntry("people", 79),
        WordEntry("my", 81), WordEntry("than", 79), WordEntry("first", 79), WordEntry("water", 78),
        WordEntry("been", 79), WordEntry("call", 78), WordEntry("who", 80), WordEntry("oil", 77),
        WordEntry("its", 79), WordEntry("now", 80), WordEntry("find", 79), WordEntry("long", 78),
        WordEntry("down", 78), WordEntry("day", 79), WordEntry("did", 79), WordEntry("get", 80),
        WordEntry("come", 79), WordEntry("made", 78), WordEntry("may", 78), WordEntry("part", 77),
        WordEntry("good", 82), WordEntry("new", 80), WordEntry("great", 80), WordEntry("best", 80),
        WordEntry("hello", 81), WordEntry("thanks", 81), WordEntry("thank", 81), WordEntry("sorry", 80),
        WordEntry("please", 80), WordEntry("welcome", 80), WordEntry("love", 81), WordEntry("help", 80)
    ).distinctBy { it.word.lowercase(Locale.ROOT) }

    // Fast lookup set of known valid words (in lowercase)
    private val VALID_WORDS_SET: Set<String> = FREQUENT_WORDS.map { it.word.lowercase(Locale.ROOT) }.toSet()

    // Fast lookup map for word frequency
    private val WORD_FREQ_MAP: Map<String, Int> = FREQUENT_WORDS.associate { it.word.lowercase(Locale.ROOT) to it.frequency }

    // Bigram Language Model for Next-Word Prediction: P(w_next | w_previous)
    // Curated Indonesian conversational collocations and natural phrase continuations
    private val BIGRAM_PREDICTIONS: Map<String, List<String>> = mapOf(
        "terima" to listOf("kasih", "banyak", "kembali"),
        "selamat" to listOf("pagi", "siang", "malam", "datang", "ulang tahun"),
        "apa" to listOf("kabar", "yang", "itu", "ini", "ada"),
        "sama" to listOf("sama", "sekali", "dengan"),
        "sampai" to listOf("jumpa", "nanti", "saat ini", "bertemu"),
        "semoga" to listOf("sukses", "sehat", "berhasil", "harimu menyenangkan"),
        "tidak" to listOf("bisa", "mau", "ada", "tahu", "masalah", "apa-apa"),
        "gak" to listOf("bisa", "mau", "tahu", "apa-apa", "masalah"),
        "nggak" to listOf("bisa", "mau", "tahu", "masalah", "ada"),
        "ngga" to listOf("bisa", "mau", "tahu", "masalah"),
        "bisa" to listOf("tolong", "bantu", "bicara", "datang", "jadi"),
        "tolong" to listOf("bantu", "saya", "kirimkan", "dengar"),
        "aku" to listOf("lagi", "mau", "sudah", "bisa", "sayang", "cinta"),
        "saya" to listOf("ingin", "mau", "sudah", "bisa", "sangat", "berharap"),
        "kamu" to listOf("lagi", "mau", "dimana", "sudah", "bisa", "kenapa"),
        "dia" to listOf("lagi", "sudah", "mau", "tidak", "ada"),
        "mereka" to listOf("sudah", "sedang", "akan", "tidak", "semua"),
        "kita" to listOf("harus", "bisa", "akan", "semua", "sama-sama"),
        "kami" to listOf("siap", "akan", "sudah", "mengucapkan"),
        "lagi" to listOf("apa", "dimana", "ngapain", "sibuk", "di jalan"),
        "sedang" to listOf("apa", "sibuk", "belajar", "bekerja", "makan"),
        "sudah" to listOf("makan", "tidur", "selesai", "lama", "sampai"),
        "belum" to listOf("selesai", "makan", "tidur", "ada", "tahu"),
        "kapan" to listOf("kamu", "bisa", "pulang", "sampai", "mulai"),
        "dimana" to listOf("kamu", "posisimu", "rumahnya", "lokasinya"),
        "kemana" to listOf("kamu", "tujuannya", "perginya"),
        "kenapa" to listOf("kamu", "bisa", "tidak", "jadi"),
        "bagaimana" to listOf("kabarmu", "caranya", "keadaannya", "hasilnya"),
        "gimana" to listOf("kabarmu", "caranya", "keadaannya", "jadinya"),
        "berapa" to listOf("harganya", "jumlahnya", "nomornya", "lama"),
        "siapa" to listOf("namamu", "yang", "dia", "kamu"),
        "hati" to listOf("hati", "di jalan"),
        "jangan" to listOf("lupa", "khawatir", "ragu", "pernah", "marah"),
        "ada" to listOf("apa", "yang", "banyak", "masalah"),
        "mau" to listOf("makan", "pergi", "tidur", "tanya", "beli"),
        "ingin" to listOf("tahu", "bertanya", "belajar", "bertemu"),
        "harus" to listOf("bisa", "selesai", "semangat", "yakin"),
        "sangat" to listOf("bagus", "baik", "senang", "indah", "membantu"),
        "banget" to listOf("sih", "nih", "ya", "bagus"),
        "enak" to listOf("sekali", "banget", "nih"),
        "bagus" to listOf("sekali", "banget", "nih"),
        "oke" to listOf("siap", "terima kasih", "baiklah"),
        "siap" to listOf("laksanakan", "komandan", "mantap", "terima kasih"),
        "mantap" to listOf("jiwa", "banget", "sekali"),
        "hari" to listOf("ini", "esok", "senin", "minggu", "libur"),
        "malam" to listOf("ini", "minggu", "nanti", "hari"),
        "pagi" to listOf("ini", "hari", "semua"),
        "siang" to listOf("ini", "nanti", "hari"),
        "sore" to listOf("ini", "nanti", "hari"),
        "rumah" to listOf("sakit", "saya", "kamu", "makan"),
        "toko" to listOf("buku", "roti", "online"),
        "nomor" to listOf("telepon", "rekening", "hp", "wa"),

        // English Bigrams
        "good" to listOf("morning", "afternoon", "night", "job", "luck"),
        "thank" to listOf("you", "God", "so much", "you very much"),
        "how" to listOf("are you", "much", "is it", "do you", "to"),
        "nice" to listOf("to meet you", "day", "one", "try"),
        "have" to listOf("a great day", "a nice day", "fun", "a good one"),
        "see" to listOf("you", "you later", "you soon", "you again"),
        "take" to listOf("care", "it easy", "time", "your time"),
        "i" to listOf("am", "love you", "think", "want", "hope", "need", "can"),
        "you" to listOf("are", "can", "know", "have", "want", "will"),
        "what" to listOf("are you doing", "is this", "happened", "do you mean"),
        "where" to listOf("are you", "is it", "do you live"),
        "do" to listOf("you want", "you know", "not worry", "it now"),
        "please" to listOf("help me", "let me know", "call me", "come in")
    )

    /**
     * Next-Word Prediction (Language Model):
     * Predicts the 3 most likely next words given the completed preceding word.
     * Used when composingWord is empty (cursor is right after a space or start of text).
     */
    fun getNextWordPredictions(
        previousWord: String,
        maxCount: Int = 3,
        learnedWords: Map<String, Int> = emptyMap()
    ): List<String> {
        val cleanPrev = previousWord.trim().lowercase(Locale.ROOT)
        if (cleanPrev.isEmpty() || maxCount <= 0) return emptyList()
        val contextual = BIGRAM_PREDICTIONS[cleanPrev].orEmpty()
        if (contextual.isEmpty()) return emptyList()
        return contextual.distinct()
            .sortedByDescending { candidate ->
                val token = candidate.substringBefore(' ').lowercase(Locale.ROOT)
                learnedWords[token] ?: 0
            }
            .take(maxCount)
    }

    /**
     * Word Completion & Auto-Correction (Spell Checking):
     * Used when user is actively typing a word (composingWord is not empty).
     *
     * Returns a list of 3 ranked suggestions:
     * - Left: Raw verbatim word (in quotes or exact) so user can always keep what they typed!
     * - Center: Best match (high-frequency prefix completion or high-confidence autocorrect)
     * - Right: Alternative completion or secondary correction
     */
    fun getWordCompletionsAndCorrections(
        composingWord: String,
        maxCount: Int = 3,
        learnedWords: Map<String, Int> = emptyMap()
    ): List<String> {
        val trimmed = composingWord.trim()
        if (trimmed.isEmpty() || maxCount <= 0) return emptyList()
        val lower = trimmed.lowercase(Locale.ROOT)
        val firstCapital = trimmed.firstOrNull()?.isUpperCase() == true
        val allCaps = trimmed.length > 1 && trimmed.all { it.isUpperCase() }
        val learnedCandidates = learnedWords.entries
            .filter { it.key.startsWith(lower) && it.key != lower }
            .sortedByDescending { it.value }
            .map { formatCase(it.key, firstCapital, allCaps) }
        val dictionaryCandidates = FREQUENT_WORDS.asSequence()
            .filter { it.word.startsWith(lower) && it.word != lower }
            .sortedWith(compareByDescending<WordEntry> { it.frequency }.thenBy { it.word.length })
            .map { formatCase(it.word, firstCapital, allCaps) }
            .toList()
        // Keep the user's exact spelling as the first choice; never surface fuzzy guesses as if verified.
        return (listOf(trimmed) + learnedCandidates + dictionaryCandidates).distinct().take(maxCount)
    }

    /**
     * Missing Space ("Saran Spasi") Algorithm:
     * Discovers if an unspaced word S can be segmented into two valid words W1 + W2
     * (e.g. "terimakasih" -> "terima kasih", "selamatpagi" -> "selamat pagi", "apakabar" -> "apa kabar",
     * "sudahmakan" -> "sudah makan", "lagiapa" -> "lagi apa").
     * Selects the split with the highest combined word frequency.
     */
    fun findMissingSpaceSuggestion(
        word: String,
        learnedWords: Map<String, Int> = emptyMap()
    ): String? {
        val trimmed = word.trim()
        if (trimmed.length < 5) return null
        val lower = trimmed.lowercase(Locale.ROOT)

        val isFirstCapital = trimmed[0].isUpperCase()
        val isAllCaps = trimmed.all { it.isUpperCase() }

        var bestSplit: Pair<String, String>? = null
        var bestScore = -1

        for (i in 2..(lower.length - 2)) {
            val part1 = lower.substring(0, i)
            val part2 = lower.substring(i)

            val part1Valid = VALID_WORDS_SET.contains(part1) || learnedWords.containsKey(part1)
            val part2Valid = VALID_WORDS_SET.contains(part2) || learnedWords.containsKey(part2)

            if (part1Valid && part2Valid) {
                val f1 = WORD_FREQ_MAP[part1] ?: (learnedWords[part1]?.times(10) ?: 40)
                val f2 = WORD_FREQ_MAP[part2] ?: (learnedWords[part2]?.times(10) ?: 40)
                val score = f1 + f2
                if (score > bestScore) {
                    bestScore = score
                    bestSplit = Pair(part1, part2)
                }
            }
        }

        return bestSplit?.let { (p1, p2) ->
            val formatted1 = formatCase(p1, isFirstCapital, isAllCaps)
            val formatted2 = if (isAllCaps) p2.uppercase(Locale.ROOT) else p2
            "$formatted1 $formatted2"
        }
    }

    /**
     * Backward-compatible helper method used by callers.
     * Dispatches intelligently: if word is empty or followed by space, returns next-word predictions;
     * if word is actively being typed, returns completions and typo corrections with learned word ranking.
     */
    fun getSuggestions(
        wordOrPrefix: String,
        maxCount: Int = 3,
        learnedWords: Map<String, Int> = emptyMap()
    ): List<String> {
        val trimmed = wordOrPrefix.trim()
        if (trimmed.isEmpty()) {
            return getNextWordPredictions("", maxCount)
        }
        return getWordCompletionsAndCorrections(trimmed, maxCount, learnedWords)
    }

    /**
     * Resolve a traced QWERTY path to a dictionary/learned word.
     * A result is returned only when the geometric spelling is sufficiently close.
     */
    fun getSwipeSuggestion(path: String, learnedWords: Map<String, Int> = emptyMap()): String? {
        val normalized = path.lowercase(Locale.ROOT).filter { it in 'a'..'z' }
        if (normalized.length < 3 || normalized.length > 18) return null
        val pool = (FREQUENT_WORDS + learnedWords.map { (word, count) ->
            WordEntry(word.lowercase(Locale.ROOT), 100 + count.coerceAtMost(1000))
        }).distinctBy { it.word.lowercase(Locale.ROOT) }
        val ranked = pool.asSequence()
            .filter { kotlin.math.abs(it.word.length - normalized.length) <= 3 }
            .map { entry ->
                val candidate = entry.word.lowercase(Locale.ROOT)
                val normalizedDistance = calculateSpatialEditDistance(normalized, candidate) /
                    maxOf(normalized.length, candidate.length).toDouble()
                val learnedBoost = (learnedWords[candidate] ?: 0).coerceAtMost(500) / 10000.0
                Triple(candidate, normalizedDistance - learnedBoost, normalizedDistance)
            }
            .filter { it.third <= 0.34 }
            .sortedBy { it.second }
            .toList()
        val best = ranked.firstOrNull() ?: return null
        val second = ranked.getOrNull(1)
        if (second != null && second.second - best.second < 0.025 && best.third > 0.02) return null
        return best.first
    }

    /**
     * Strict Auto-Correction logic executed when user taps Spacebar:
     * - NEVER autocorrect a word that is already valid or recognized (in dictionary or learned words).
     * - NEVER autocorrect words shorter than 3 characters.
     * - ONLY autocorrect if there is a candidate with an overwhelmingly high spatial probability (cost <= 1.1).
     * Returns null if no safe auto-correction should take place.
     */
    fun findAutoCorrection(
        word: String,
        learnedWords: Map<String, Int> = emptyMap()
    ): String? {
        val trimmed = word.trim()
        if (trimmed.length < 3 || trimmed.any { !it.isLetter() }) return null
        val lower = trimmed.lowercase(Locale.ROOT)
        val learned = learnedWords.keys.map { it.lowercase(Locale.ROOT) }.toSet()
        if (lower in VALID_WORDS_SET || lower in learned) return null
        val pool = (FREQUENT_WORDS + learnedWords.map { (w, count) ->
            WordEntry(w.lowercase(Locale.ROOT), 100 + count.coerceAtMost(1000))
        }).distinctBy { it.word.lowercase(Locale.ROOT) }
        val ranked = pool.asSequence()
            .filter { kotlin.math.abs(it.word.length - lower.length) <= 1 }
            .map { entry -> Triple(entry.word.lowercase(Locale.ROOT), calculateSpatialEditDistance(lower, entry.word.lowercase(Locale.ROOT)), entry.frequency) }
            .filter { it.second <= 0.85 }
            .sortedWith(compareBy<Triple<String, Double, Int>> { it.second }.thenByDescending { it.third })
            .toList()
        val best = ranked.firstOrNull() ?: return null
        val second = ranked.getOrNull(1)
        if (second != null && second.second - best.second < 0.25) return null
        return formatCase(best.first, trimmed.first().isUpperCase(), trimmed.length > 1 && trimmed.all { it.isUpperCase() })
    }

    /**
     * Spatial-Weighted Damerau-Levenshtein Distance:
     * - Match: cost = 0.0
     * - Adjacent QWERTY substitution (fat-finger): cost = 0.5
     * - Non-adjacent substitution: cost = 1.0
     * - Transposition of adjacent characters (e.g. "teh" -> "the", "apakh" -> "apakah"): cost = 0.6
     * - Insertion / Deletion: cost = 0.8
     */
    private fun calculateSpatialEditDistance(s1: String, s2: String): Double {
        val len1 = s1.length
        val len2 = s2.length
        val dp = Array(len1 + 1) { DoubleArray(len2 + 1) }

        for (i in 0..len1) dp[i][0] = i * 0.8
        for (j in 0..len2) dp[0][j] = j * 0.8

        for (i in 1..len1) {
            val c1 = s1[i - 1]
            for (j in 1..len2) {
                val c2 = s2[j - 1]

                val subCost = when {
                    c1 == c2 -> 0.0
                    QWERTY_NEIGHBORS[c1]?.contains(c2) == true -> 0.5
                    else -> 1.0
                }

                var minCost = min(
                    dp[i - 1][j] + 0.8, // deletion
                    min(
                        dp[i][j - 1] + 0.8, // insertion
                        dp[i - 1][j - 1] + subCost // substitution
                    )
                )

                // Transposition check (Damerau condition)
                if (i > 1 && j > 1 && s1[i - 1] == s2[j - 2] && s1[i - 2] == s2[j - 1]) {
                    minCost = min(minCost, dp[i - 2][j - 2] + 0.6)
                }

                dp[i][j] = minCost
            }
        }
        return dp[len1][len2]
    }

    private fun formatCase(word: String, isFirstCapital: Boolean, isAllCaps: Boolean): String {
        return when {
            isAllCaps -> word.uppercase(Locale.ROOT)
            isFirstCapital -> word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
            else -> word
        }
    }
}

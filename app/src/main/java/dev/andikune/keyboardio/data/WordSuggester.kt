package dev.andikune.keyboardio.data

import java.util.Locale
import kotlin.math.min

object WordSuggester {

    // Common Indonesian and English frequent words
    val DICTIONARY = listOf(
        // Indonesian
        "ada", "adalah", "akan", "aku", "anda", "apa", "apakah", "atas", "atau", "awal",
        "bagaimana", "bagi", "bagus", "bahkan", "bahwa", "baik", "banyak", "bapak", "baru", "bawah",
        "beberapa", "benar", "belum", "besar", "bisa", "bila", "bukan", "boleh",
        "cara", "cukup", "cepat", "cinta", "contoh",
        "dan", "dapat", "dari", "dalam", "dengan", "depan", "desa", "dia", "diri", "di",
        "email", "enak", "empat",
        "hanya", "hari", "harus", "hati", "hidup", "hingga", "hubungan",
        "ia", "ibu", "ini", "itu", "ingin", "ingat", "informasi",
        "jika", "juga", "jangan", "jalan", "jelas", "jumlah",
        "karena", "kami", "kamu", "kata", "ke", "kemudian", "kepada", "kerja", "ketika", "kita", "kabar", "keyboard", "kirim", "kapan", "keluarga", "kondisi", "kota", "kurang",
        "lagi", "lain", "lalu", "lama", "langsung", "lebih", "lihat", "luar",
        "maka", "malam", "mana", "masih", "masuk", "mata", "mau", "mereka", "menjadi", "melihat", "membuat", "membantu", "memiliki", "mengapa", "mengetahui", "menggunakan", "mungkin", "mulai",
        "nama", "nanti", "namun", "negara", "nomor",
        "oleh", "orang", "otomatis",
        "pada", "pagi", "paling", "pasti", "panjang", "para", "pemerintah", "perlu", "pernah", "pesan", "pilihan", "pikir",
        "rasa", "rumah", "ruang",
        "saat", "saja", "salah", "sama", "sampai", "satu", "saya", "sebab", "sebagai", "sebelum", "sebuah", "secara", "sedang", "sehingga", "sejak", "sekali", "sekarang", "selalu", "selama", "selesai", "semua", "semoga", "sendiri", "seperti", "sering", "serta", "sesuai", "setelah", "setiap", "siapa", "sudah", "sukses",
        "tahu", "tahun", "tambah", "tanpa", "tapi", "telah", "tempat", "tengah", "tentang", "tentu", "terhadap", "terjadi", "terkait", "terima", "kasih", "tetap", "tetapi", "tidak", "tinggi", "tujuan",
        "uang", "untuk", "utama",
        "waktu", "walau", "walaupun", "warga",
        "ya", "yang",

        // English
        "about", "after", "again", "all", "also", "and", "any", "are", "back", "because",
        "been", "before", "best", "better", "both", "but", "can", "come", "could", "day",
        "did", "do", "does", "done", "even", "every", "first", "for", "from", "get", "give",
        "go", "good", "great", "had", "has", "have", "hello", "help", "here", "him", "his",
        "how", "into", "just", "know", "like", "look", "make", "many", "more", "most", "much",
        "must", "name", "new", "next", "not", "now", "off", "one", "only", "other", "our",
        "out", "over", "people", "right", "said", "same", "say", "see", "she", "should", "some",
        "take", "than", "that", "the", "their", "them", "then", "there", "these", "they", "thing",
        "think", "this", "time", "two", "very", "want", "way", "well", "what", "when", "which",
        "who", "will", "with", "would", "year", "you", "your"
    ).distinct()

    private val BIGRAM_MAP = mapOf(
        "terima" to listOf("kasih", "banyak", "kembali"),
        "selamat" to listOf("pagi", "siang", "malam", "datang"),
        "apa" to listOf("kabar", "yang", "itu"),
        "sama" to listOf("sama", "sekali", "dengan"),
        "sampai" to listOf("jumpa", "saat ini", "nanti"),
        "semoga" to listOf("sukses", "sehat", "berhasil", "harimu menyenangkan"),
        "tidak" to listOf("bisa", "tahu", "masalah", "apa-apa"),
        "bisa" to listOf("tolong", "bantu", "datang"),
        "thank" to listOf("you", "you very much", "God"),
        "how" to listOf("are you", "is it going", "to"),
        "good" to listOf("morning", "afternoon", "night", "job"),
        "have" to listOf("a great day", "fun", "a good one"),
        "nice" to listOf("to meet you", "one", "day"),
        "i" to listOf("am", "love you", "think", "want", "can")
    )

    fun getSuggestions(
        prefix: String,
        maxCount: Int = 3,
        learnedWords: Map<String, Int> = emptyMap()
    ): List<String> {
        val trimmed = prefix.trim()
        if (trimmed.isEmpty()) return emptyList()

        val lowerPrefix = trimmed.lowercase(Locale.ROOT)
        val isFirstCapital = trimmed[0].isUpperCase()
        val isAllCaps = trimmed.all { it.isUpperCase() }

        // Check if user just typed a completed word followed by space, or full word
        BIGRAM_MAP[lowerPrefix]?.let { nextWords ->
            return nextWords
                .sortedWith(compareByDescending<String> { learnedWords[it.lowercase(Locale.ROOT)] ?: 0 }.thenBy { it.length })
                .take(maxCount)
                .map { formatCase(it, isFirstCapital, isAllCaps) }
        }

        // 1. Prefix match. User-learned words are ranked before generic words.
        val prefixMatches = (DICTIONARY + learnedWords.keys)
            .distinct()
            .filter { it.startsWith(lowerPrefix) }
            .sortedWith(compareByDescending<String> { learnedWords[it] ?: 0 }.thenBy { it.length }.thenBy { it })

        if (prefixMatches.isNotEmpty()) {
            return prefixMatches.take(maxCount).map { formatCase(it, isFirstCapital, isAllCaps) }
        }

        // 2. Typo correction via Levenshtein Distance
        val fuzzyMatches = (DICTIONARY + learnedWords.keys)
            .distinct()
            .map { it to levenshteinDistance(lowerPrefix, it) }
            .filter { it.second <= correctionDistance(lowerPrefix.length) && it.second < lowerPrefix.length }
            .sortedWith(compareBy<Pair<String, Int>> { it.second }.thenByDescending { learnedWords[it.first] ?: 0 })
            .map { it.first }

        return fuzzyMatches.take(maxCount).map { formatCase(it, isFirstCapital, isAllCaps) }
    }

    fun findAutoCorrection(word: String, learnedWords: Map<String, Int> = emptyMap()): String? {
        val trimmed = word.trim().lowercase(Locale.ROOT)
        if (trimmed.length < 3 || !trimmed.all { it.isLetter() || it == '\'' }) return null
        if (DICTIONARY.contains(trimmed) || learnedWords.containsKey(trimmed)) return null

        val best = (DICTIONARY + learnedWords.keys)
            .distinct()
            .map { it to levenshteinDistance(trimmed, it) }
            .filter { (candidate, distance) ->
                distance <= correctionDistance(trimmed.length) &&
                        candidate.length >= 3 &&
                        distance < maxOf(2, trimmed.length / 2) &&
                        (candidate.length != trimmed.length || isAdjacentTransposition(trimmed, candidate))
            }
            .minWithOrNull(compareBy<Pair<String, Int>> { it.second }
                .thenByDescending { learnedWords[it.first] ?: 0 }
                .thenBy { it.first.length })
            ?.first

        return best?.let { formatCase(it, word[0].isUpperCase(), word.all { ch -> ch.isUpperCase() }) }
    }

    private fun correctionDistance(length: Int): Int = when {
        length <= 4 -> 1
        else -> 2
    }

    private fun isAdjacentTransposition(first: String, second: String): Boolean {
        if (first.length != second.length) return false
        val different = first.indices.filter { first[it] != second[it] }
        if (different.size != 2) return false
        val left = different[0]
        val right = different[1]
        return right == left + 1 && first[left] == second[right] && first[right] == second[left]
    }

    private fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j

        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = min(
                    dp[i - 1][j] + 1,
                    min(dp[i][j - 1] + 1, dp[i - 1][j - 1] + cost)
                )
            }
        }
        return dp[s1.length][s2.length]
    }

    private fun formatCase(word: String, isFirstCapital: Boolean, isAllCaps: Boolean): String {
        return when {
            isAllCaps -> word.uppercase(Locale.ROOT)
            isFirstCapital -> word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
            else -> word
        }
    }
}

package dev.andikune.keyboardio.data

import java.util.Locale

object WordSuggester {

    // Common Indonesian and English frequent words
    private val DICTIONARY = listOf(
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
        "tahu", "tahun", "tambah", "tanpa", "tapi", "telah", "tempat", "tengah", "tentang", "tentu", "terhadap", "terjadi", "terkait", "terima", "terima kasih", "tetap", "tetapi", "tidak", "tinggi", "tujuan",
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

    fun getSuggestions(prefix: String, maxCount: Int = 3): List<String> {
        val trimmed = prefix.trim()
        if (trimmed.length < 2) return emptyList()

        val lowerPrefix = trimmed.lowercase(Locale.ROOT)
        val isFirstCapital = trimmed[0].isUpperCase()
        val isAllCaps = trimmed.all { it.isUpperCase() }

        val matches = DICTIONARY
            .filter { it.startsWith(lowerPrefix) && it != lowerPrefix }
            .sortedBy { it.length }
            .take(maxCount)

        return matches.map { word ->
            when {
                isAllCaps -> word.uppercase(Locale.ROOT)
                isFirstCapital -> word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
                else -> word
            }
        }
    }
}

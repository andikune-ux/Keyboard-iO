package dev.andikune.keyboardio.data

import java.util.Locale

object OfflineTranslator {

    private val PHRASES_ID_EN = listOf(
        "selamat pagi" to "good morning",
        "selamat siang" to "good afternoon",
        "selamat sore" to "good afternoon",
        "selamat malam" to "good night",
        "terima kasih banyak" to "thank you very much",
        "terima kasih" to "thank you",
        "sama sama" to "you are welcome",
        "apa kabar" to "how are you",
        "sampai jumpa" to "see you later",
        "saya cinta kamu" to "I love you",
        "senang bertemu denganmu" to "nice to meet you",
        "semoga harimu menyenangkan" to "have a great day",
        "tidak masalah" to "no problem",
        "hati hati di jalan" to "take care on the way",
        "saya sedang sibuk" to "I am busy right now",
        "tunggu sebentar" to "wait a minute",
        "saya tidak mengerti" to "I do not understand",
        "bisa tolong saya" to "can you help me"
    )

    private val VOCAB_ID_EN = mapOf(
        "halo" to "hello", "hai" to "hi", "ya" to "yes", "tidak" to "no", "bukan" to "not",
        "saya" to "I", "aku" to "me", "kamu" to "you", "anda" to "you", "dia" to "they",
        "kami" to "we", "kita" to "we", "mereka" to "they", "ini" to "this", "itu" to "that",
        "apa" to "what", "siapa" to "who", "kapan" to "when", "dimana" to "where", "mengapa" to "why",
        "bagaimana" to "how", "ada" to "there is", "adalah" to "is", "akan" to "will",
        "bisa" to "can", "dapat" to "can", "harus" to "must", "mau" to "want", "ingin" to "want",
        "suka" to "like", "cinta" to "love", "baik" to "good", "bagus" to "great", "buruk" to "bad",
        "indah" to "beautiful", "cepat" to "fast", "lambat" to "slow", "besar" to "big", "kecil" to "small",
        "baru" to "new", "lama" to "old", "benar" to "correct", "salah" to "wrong",
        "kerja" to "work", "belajar" to "study", "makan" to "eat", "minum" to "drink", "tidur" to "sleep",
        "bicara" to "speak", "baca" to "read", "tulis" to "write", "dengar" to "listen", "jalan" to "walk",
        "rumah" to "house", "sekolah" to "school", "kantor" to "office", "uang" to "money", "hari" to "day",
        "malam" to "night", "waktu" to "time", "teman" to "friend", "keluarga" to "family", "orang" to "person",
        "pesan" to "message", "telepon" to "phone", "mobil" to "car", "makanan" to "food", "air" to "water",
        "sekarang" to "now", "besok" to "tomorrow", "kemarin" to "yesterday", "selalu" to "always",
        "pernah" to "ever", "belum" to "not yet", "sudah" to "already", "lagi" to "again", "sangat" to "very",
        "dengan" to "with", "tanpa" to "without", "untuk" to "for", "dari" to "from", "ke" to "to",
        "di" to "at", "pada" to "on", "dalam" to "in", "luar" to "out", "atas" to "up", "bawah" to "down"
    )

    // Build reverse map for English -> Indonesian
    private val PHRASES_EN_ID = PHRASES_ID_EN.map { it.second to it.first }
    private val VOCAB_EN_ID = VOCAB_ID_EN.entries.associate { (k, v) -> v.lowercase(Locale.ROOT) to k }

    fun translate(text: String, isIdToEn: Boolean): String {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return ""

        var working = trimmed.lowercase(Locale.ROOT)

        // 1. Check exact phrase matches first
        val phraseList = if (isIdToEn) PHRASES_ID_EN else PHRASES_EN_ID
        for ((source, target) in phraseList) {
            if (working == source) {
                return preserveCase(trimmed, target)
            }
            if (working.contains(source)) {
                working = working.replace(source, target)
            }
        }

        // 2. Word by word replacement
        val dict = if (isIdToEn) VOCAB_ID_EN else VOCAB_EN_ID
        val tokens = working.split(Regex("(?<=\\b)|(?=\\b)"))
        val translatedTokens = tokens.map { token ->
            val clean = token.lowercase(Locale.ROOT)
            dict[clean] ?: token
        }

        val result = translatedTokens.joinToString("")
        return preserveCase(trimmed, result)
    }

    fun translateIdToEn(text: String): String = translate(text, true)
    fun translateEnToId(text: String): String = translate(text, false)

    private fun preserveCase(original: String, translated: String): String {
        if (original.isEmpty() || translated.isEmpty()) return translated
        return when {
            original.all { it.isUpperCase() } -> translated.uppercase(Locale.ROOT)
            original[0].isUpperCase() -> translated.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
            else -> translated
        }
    }
}

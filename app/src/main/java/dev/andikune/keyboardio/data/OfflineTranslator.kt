package dev.andikune.keyboardio.data

import java.util.Locale

object OfflineTranslator {

    // Longer phrases MUST come first to avoid partial replacement bugs
    private val PHRASES_ID_TO_EN = listOf(
        "terima kasih banyak" to "thank you very much",
        "terima kasih" to "thank you",
        "makasih banyak" to "thank you very much",
        "terimakasih" to "thank you",
        "makasih" to "thanks",
        "sama sama" to "you are welcome",
        "sama-sama" to "you are welcome",
        "selamat ulang tahun" to "happy birthday",
        "selamat hari raya" to "happy holidays",
        "selamat tahun baru" to "happy new year",
        "selamat datang" to "welcome",
        "selamat pagi" to "good morning",
        "selamat siang" to "good afternoon",
        "selamat sore" to "good afternoon",
        "selamat malam" to "good night",
        "selamat tidur" to "sleep well",
        "selamat makan" to "bon appetit",
        "apa kabar" to "how are you",
        "bagaimana kabarmu" to "how are you doing",
        "sampai jumpa" to "see you later",
        "sampai ketemu lagi" to "see you again",
        "hati hati di jalan" to "take care on the way",
        "hati-hati" to "be careful",
        "saya cinta kamu" to "I love you",
        "aku cinta kamu" to "I love you",
        "aku sayang kamu" to "I love you",
        "saya sayang kamu" to "I love you",
        "senang bertemu denganmu" to "nice to meet you",
        "senang kenal kamu" to "nice to know you",
        "semoga harimu menyenangkan" to "have a great day",
        "tidak masalah" to "no problem",
        "nggak masalah" to "no problem",
        "gak apa apa" to "no problem",
        "tidak apa-apa" to "it is okay",
        "saya sedang sibuk" to "I am busy right now",
        "aku lagi sibuk" to "I am busy right now",
        "tunggu sebentar" to "wait a minute",
        "tunggu dulu" to "wait a moment",
        "saya tidak mengerti" to "I do not understand",
        "aku gak paham" to "I do not understand",
        "bisa tolong saya" to "can you help me",
        "tolong bantu saya" to "please help me",
        "sedang apa" to "what are you doing",
        "lagi ngapain" to "what are you doing",
        "lagi apa" to "what are you doing",
        "sudah makan" to "have you eaten",
        "kamu mau kemana" to "where are you going",
        "kamu lagi dimana" to "where are you right now",
        "dimana kamu" to "where are you",
        "ada apa" to "what is wrong",
        "kenapa kamu" to "why are you like that",
        "tentu saja" to "of course",
        "siapa namamu" to "what is your name",
        "nama saya" to "my name is",
        "nama aku" to "my name is",
        "bisa bicara bahasa inggris" to "can you speak English",
        "saya baik baik saja" to "I am doing well",
        "aku baik-baik saja" to "I am doing well",
        "bagus sekali" to "very good",
        "keren banget" to "so cool",
        "enak sekali" to "very delicious",
        "jangan lupa" to "do not forget"
    )

    private val PHRASES_EN_TO_ID = listOf(
        "thank you very much" to "terima kasih banyak",
        "thank you so much" to "terima kasih banyak",
        "thank you" to "terima kasih",
        "thanks a lot" to "terima kasih banyak",
        "thanks" to "terima kasih",
        "you are welcome" to "sama-sama",
        "you're welcome" to "sama-sama",
        "happy birthday" to "selamat ulang tahun",
        "happy new year" to "selamat tahun baru",
        "good morning" to "selamat pagi",
        "good afternoon" to "selamat siang",
        "good evening" to "selamat malam",
        "good night" to "selamat tidur",
        "how are you" to "apa kabar",
        "how are you doing" to "bagaimana kabarmu",
        "see you later" to "sampai jumpa lagi",
        "see you again" to "sampai jumpa lagi",
        "see you" to "sampai jumpa",
        "take care" to "hati-hati",
        "be careful" to "hati-hati",
        "i love you" to "aku cinta kamu",
        "nice to meet you" to "senang bertemu denganmu",
        "have a great day" to "semoga harimu menyenangkan",
        "have a good day" to "semoga harimu menyenangkan",
        "no problem" to "tidak masalah",
        "it is okay" to "tidak apa-apa",
        "it's okay" to "tidak apa-apa",
        "wait a minute" to "tunggu sebentar",
        "wait a moment" to "tunggu sebentar",
        "i do not understand" to "saya tidak mengerti",
        "i don't understand" to "saya tidak mengerti",
        "can you help me" to "bisa tolong saya",
        "please help me" to "tolong bantu saya",
        "what are you doing" to "sedang apa",
        "where are you going" to "kamu mau kemana",
        "where are you" to "kamu dimana",
        "what is your name" to "siapa namamu",
        "my name is" to "nama saya",
        "what's your name" to "siapa namamu",
        "i am fine" to "saya baik-baik saja",
        "i'm fine" to "saya baik-baik saja",
        "of course" to "tentu saja",
        "very good" to "bagus sekali",
        "so cool" to "keren sekali",
        "do not forget" to "jangan lupa",
        "don't forget" to "jangan lupa"
    )

    private val VOCAB_ID_TO_EN = mapOf(
        // Greetings & Civility
        "halo" to "hello", "hai" to "hi", "ya" to "yes", "iya" to "yes", "yoi" to "yeah",
        "tidak" to "no", "bukan" to "not", "nggak" to "no", "gak" to "no", "tak" to "not",
        "tolong" to "please", "maaf" to "sorry", "permisi" to "excuse me", "silakan" to "please",
        "oke" to "okay", "ok" to "okay", "siap" to "ready", "mantap" to "awesome", "semangat" to "keep going",

        // Pronouns
        "saya" to "I", "aku" to "I", "kamu" to "you", "kau" to "you", "anda" to "you",
        "dia" to "they", "ia" to "he", "beliau" to "he", "kami" to "we", "kita" to "we",
        "mereka" to "they", "ini" to "this", "itu" to "that", "sini" to "here", "situ" to "there", "sana" to "there",

        // Question words
        "apa" to "what", "siapa" to "who", "kapan" to "when", "dimana" to "where",
        "kemana" to "where to", "darimana" to "where from", "mengapa" to "why", "kenapa" to "why",
        "bagaimana" to "how", "gimana" to "how", "berapa" to "how much",

        // Verbs
        "ada" to "exists", "adalah" to "is", "jadi" to "become", "akan" to "will",
        "bisa" to "can", "dapat" to "can", "harus" to "must", "mau" to "want", "ingin" to "want",
        "suka" to "like", "cinta" to "love", "sayang" to "care", "benci" to "hate",
        "kerja" to "work", "bekerja" to "work", "belajar" to "study", "makan" to "eat",
        "minum" to "drink", "tidur" to "sleep", "bangun" to "wake up", "bicara" to "talk",
        "baca" to "read", "membaca" to "read", "tulis" to "write", "menulis" to "write",
        "dengar" to "hear", "mendengar" to "listen", "lihat" to "look", "melihat" to "see",
        "nonton" to "watch", "jalan" to "walk", "berjalan" to "walk", "lari" to "run",
        "berlari" to "run", "duduk" to "sit", "berdiri" to "stand", "pergi" to "go",
        "pulang" to "go home", "datang" to "come", "tiba" to "arrive", "sampai" to "reach",
        "berhenti" to "stop", "mulai" to "start", "selesai" to "finish", "beli" to "buy",
        "membeli" to "buy", "jual" to "sell", "menjual" to "sell", "bayar" to "pay",
        "ambil" to "take", "memberi" to "give", "kasih" to "give", "buka" to "open",
        "tutup" to "close", "kirim" to "send", "mengirim" to "send", "terima" to "receive",
        "cari" to "search", "mencari" to "search", "temu" to "find", "menemukan" to "find",
        "pikir" to "think", "berpikir" to "think", "ingat" to "remember", "lupa" to "forget",
        "tahu" to "know", "mengerti" to "understand", "paham" to "understand", "kenal" to "know",

        // Adjectives
        "baik" to "good", "bagus" to "great", "buruk" to "bad", "jelek" to "ugly",
        "indah" to "beautiful", "cantik" to "pretty", "ganteng" to "handsome",
        "keren" to "cool", "hebat" to "awesome", "pintar" to "smart", "bodoh" to "stupid",
        "cepat" to "fast", "lambat" to "slow", "besar" to "big", "kecil" to "small",
        "tinggi" to "tall", "pendek" to "short", "panjang" to "long", "jauh" to "far",
        "dekat" to "near", "baru" to "new", "lama" to "old", "tua" to "old", "muda" to "young",
        "benar" to "correct", "bener" to "correct", "salah" to "wrong", "mudah" to "easy",
        "gampang" to "easy", "sulit" to "difficult", "susah" to "hard", "murah" to "cheap",
        "mahal" to "expensive", "gratis" to "free", "senang" to "happy", "sedih" to "sad",
        "marah" to "angry", "takut" to "afraid", "lelah" to "tired", "capek" to "tired",
        "lapar" to "hungry", "haus" to "thirsty", "kenyang" to "full", "bersih" to "clean",
        "kotor" to "dirty", "panas" to "hot", "dingin" to "cold", "sejuk" to "cool",
        "ramai" to "crowded", "sepi" to "quiet", "terang" to "bright", "gelap" to "dark",

        // Nouns
        "rumah" to "house", "sekolah" to "school", "kampus" to "college", "kantor" to "office",
        "toko" to "shop", "pasar" to "market", "stasiun" to "station", "bandara" to "airport",
        "kamar" to "room", "jalan" to "road", "kota" to "city", "desa" to "village",
        "negara" to "country", "dunia" to "world", "uang" to "money", "harga" to "price",
        "hari" to "day", "malam" to "night", "pagi" to "morning", "siang" to "afternoon",
        "sore" to "late afternoon", "waktu" to "time", "jam" to "hour", "menit" to "minute",
        "detik" to "second", "minggu" to "week", "bulan" to "month", "tahun" to "year",
        "sekarang" to "now", "nanti" to "later", "kemarin" to "yesterday", "besok" to "tomorrow",
        "teman" to "friend", "kawan" to "friend", "sahabat" to "best friend", "musuh" to "enemy",
        "keluarga" to "family", "orang" to "person", "ayah" to "father", "ibu" to "mother",
        "kakak" to "older sibling", "adik" to "younger sibling", "anak" to "child",
        "suami" to "husband", "istri" to "wife", "pacar" to "partner", "guru" to "teacher",
        "dokter" to "doctor", "polisi" to "police", "bos" to "boss",
        "kucing" to "cat", "anjing" to "dog", "burung" to "bird", "ikan" to "fish",
        "makanan" to "food", "minuman" to "drink", "air" to "water", "nasi" to "rice",
        "kopi" to "coffee", "teh" to "tea", "roti" to "bread",
        "mobil" to "car", "motor" to "motorcycle", "sepeda" to "bicycle", "kereta" to "train",
        "pesawat" to "plane", "bus" to "bus", "kapal" to "ship",
        "pesan" to "message", "surat" to "letter", "telepon" to "phone", "hp" to "cellphone",
        "buku" to "book", "kertas" to "paper", "pena" to "pen", "meja" to "table",
        "kursi" to "chair", "pintu" to "door", "jendela" to "window", "foto" to "photo",
        "gambar" to "picture", "musik" to "music", "lagu" to "song", "masalah" to "problem",

        // Adverbs & Conjunctions
        "selalu" to "always", "sering" to "often", "kadang" to "sometimes", "jarang" to "rarely",
        "tidak pernah" to "never", "pernah" to "ever", "sudah" to "already", "udah" to "already",
        "belum" to "not yet", "blm" to "not yet", "lagi" to "again", "sedang" to "currently",
        "sangat" to "very", "sekali" to "very", "banget" to "very", "paling" to "most",
        "lebih" to "more", "kurang" to "less", "juga" to "also", "hanya" to "only",
        "cuma" to "only", "bahkan" to "even", "dengan" to "with", "tanpa" to "without",
        "untuk" to "for", "dari" to "from", "ke" to "to", "di" to "at", "pada" to "on",
        "dalam" to "in", "luar" to "out", "atas" to "above", "bawah" to "under",
        "depan" to "front", "belakang" to "back", "dan" to "and", "atau" to "or",
        "tetapi" to "but", "tapi" to "but", "karena" to "because", "sebab" to "because",
        "jika" to "if", "kalau" to "if", "seperti" to "like", "tentang" to "about"
    )

    private val VOCAB_EN_TO_ID = mapOf(
        // Greetings
        "hello" to "halo", "hi" to "hai", "yes" to "ya", "no" to "tidak", "not" to "bukan",
        "please" to "tolong", "sorry" to "maaf", "excuse" to "permisi", "okay" to "oke",
        "ready" to "siap", "awesome" to "mantap", "cool" to "keren",

        // Pronouns
        "i" to "saya", "me" to "saya", "my" to "saya", "you" to "kamu", "your" to "kamu",
        "he" to "dia", "she" to "dia", "they" to "mereka", "we" to "kita", "us" to "kami",
        "this" to "ini", "that" to "itu", "here" to "sini", "there" to "sana",

        // Question words
        "what" to "apa", "who" to "siapa", "when" to "kapan", "where" to "dimana",
        "why" to "mengapa", "how" to "bagaimana",

        // Verbs
        "is" to "adalah", "are" to "adalah", "am" to "adalah", "was" to "adalah", "were" to "adalah",
        "will" to "akan", "can" to "bisa", "could" to "bisa", "must" to "harus",
        "want" to "mau", "like" to "suka", "love" to "cinta", "hate" to "benci",
        "work" to "kerja", "study" to "belajar", "learn" to "belajar", "eat" to "makan",
        "drink" to "minum", "sleep" to "tidur", "wake" to "bangun", "speak" to "bicara",
        "talk" to "bicara", "read" to "baca", "write" to "tulis", "hear" to "dengar",
        "listen" to "dengar", "see" to "lihat", "look" to "lihat", "watch" to "nonton",
        "walk" to "jalan", "run" to "lari", "sit" to "duduk", "stand" to "berdiri",
        "go" to "pergi", "come" to "datang", "arrive" to "tiba", "stop" to "berhenti",
        "start" to "mulai", "finish" to "selesai", "buy" to "beli", "sell" to "jual",
        "pay" to "bayar", "take" to "ambil", "give" to "beri", "open" to "buka",
        "close" to "tutup", "send" to "kirim", "receive" to "terima", "search" to "cari",
        "find" to "temukan", "think" to "pikir", "remember" to "ingat", "forget" to "lupa",
        "know" to "tahu", "understand" to "mengerti",

        // Adjectives
        "good" to "baik", "great" to "bagus", "bad" to "buruk", "ugly" to "jelek",
        "beautiful" to "indah", "pretty" to "cantik", "handsome" to "ganteng",
        "smart" to "pintar", "fast" to "cepat", "slow" to "lambat",
        "big" to "besar", "small" to "kecil", "tall" to "tinggi", "short" to "pendek",
        "new" to "baru", "old" to "lama", "correct" to "benar", "wrong" to "salah",
        "easy" to "mudah", "difficult" to "sulit", "hard" to "susah", "cheap" to "murah",
        "expensive" to "mahal", "free" to "gratis", "happy" to "senang", "sad" to "sedih",
        "angry" to "marah", "tired" to "lelah", "hungry" to "lapar", "thirsty" to "haus",
        "clean" to "bersih", "dirty" to "kotor", "hot" to "panas", "cold" to "dingin",

        // Nouns
        "house" to "rumah", "home" to "rumah", "school" to "sekolah", "office" to "kantor",
        "market" to "pasar", "station" to "stasiun", "airport" to "bandara", "room" to "kamar",
        "road" to "jalan", "street" to "jalan", "city" to "kota", "village" to "desa",
        "country" to "negara", "world" to "dunia", "money" to "uang", "price" to "harga",
        "day" to "hari", "night" to "malam", "morning" to "pagi", "afternoon" to "siang",
        "time" to "waktu", "hour" to "jam", "minute" to "menit", "second" to "detik",
        "week" to "minggu", "month" to "bulan", "year" to "tahun", "today" to "hari ini",
        "now" to "sekarang", "later" to "nanti", "yesterday" to "kemarin", "tomorrow" to "besok",
        "friend" to "teman", "family" to "keluarga", "person" to "orang", "people" to "orang-orang",
        "father" to "ayah", "mother" to "ibu", "brother" to "saudara laki-laki", "sister" to "saudara perempuan",
        "child" to "anak", "children" to "anak-anak", "cat" to "kucing", "dog" to "anjing",
        "food" to "makanan", "water" to "air", "coffee" to "kopi", "tea" to "teh",
        "car" to "mobil", "motorcycle" to "motor", "bicycle" to "sepeda", "train" to "kereta",
        "plane" to "pesawat", "bus" to "bus", "message" to "pesan", "book" to "buku",
        "pen" to "pena", "table" to "meja", "chair" to "kursi", "door" to "pintu",

        // Adverbs & Conjunctions
        "always" to "selalu", "often" to "sering", "sometimes" to "kadang-kadang", "never" to "tidak pernah",
        "already" to "sudah", "again" to "lagi", "very" to "sangat", "so" to "sangat",
        "more" to "lebih", "less" to "kurang", "also" to "juga", "only" to "hanya",
        "with" to "dengan", "without" to "tanpa", "for" to "untuk", "from" to "dari",
        "to" to "ke", "at" to "di", "in" to "dalam", "on" to "pada", "out" to "luar",
        "and" to "dan", "or" to "atau", "but" to "tetapi", "because" to "karena", "if" to "jika"
    )

    fun translate(text: String, isIdToEn: Boolean): String {
        if (text.isBlank()) return text

        val phrases = (if (isIdToEn) PHRASES_ID_TO_EN else PHRASES_EN_TO_ID)
            .sortedByDescending { it.first.length }
        val phrasePattern = Regex("(?<![\\p{L}\\p{N}])(?:${phrases.joinToString("|") { Regex.escape(it.first) }})(?![\\p{L}\\p{N}])", RegexOption.IGNORE_CASE)
        var result = phrasePattern.replace(text) { match ->
            val pair = phrases.firstOrNull { it.first.equals(match.value, ignoreCase = true) }
            pair?.let { preserveTokenCase(match.value, it.second) } ?: match.value
        }

        val dict = if (isIdToEn) VOCAB_ID_TO_EN else VOCAB_EN_TO_ID
        val wordPattern = Regex("[\\p{L}\\p{N}]+(?:['’][\\p{L}\\p{N}]+)*")
        result = wordPattern.replace(result) { match ->
            val translated = dict[match.value.lowercase(Locale.ROOT)]
            if (translated == null) match.value else preserveTokenCase(match.value, translated)
        }
        return result
    }

    private fun preserveTokenCase(original: String, translated: String): String {
        if (original.isEmpty() || translated.isEmpty()) return translated
        return when {
            original.all { !it.isLetter() || it.isUpperCase() } -> translated.uppercase(Locale.ROOT)
            original.firstOrNull { it.isLetter() }?.isUpperCase() == true ->
                translated.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
            else -> translated
        }
    }

    fun translateIdToEn(text: String): String = translate(text, true)
    fun translateEnToId(text: String): String = translate(text, false)

    /** Detects the dominant language of the input without requiring a manual toggle. */
    fun detectIdToEn(text: String): Boolean {
        val normalized = text.lowercase(Locale.ROOT)
        val words = Regex("[\\p{L}']+").findAll(normalized).map { it.value }.toList()
        if (words.isEmpty()) return true

        val idScore = words.count { VOCAB_ID_TO_EN.containsKey(it) } * 2 +
                PHRASES_ID_TO_EN.count { normalized.contains(it.first) } * 3
        val enScore = words.count { VOCAB_EN_TO_ID.containsKey(it) } * 2 +
                PHRASES_EN_TO_ID.count { normalized.contains(it.first) } * 3

        return when {
            idScore == 0 && enScore == 0 -> true
            idScore == enScore -> !words.any { VOCAB_EN_TO_ID.containsKey(it) }
            else -> idScore > enScore
        }
    }

    fun translateAuto(text: String): String = translate(text, detectIdToEn(text))

    private fun preserveCase(original: String, translated: String): String {
        if (original.isEmpty() || translated.isEmpty()) return translated
        return when {
            original.all { it.isUpperCase() } -> translated.uppercase(Locale.ROOT)
            original[0].isUpperCase() -> translated.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString()
            }
            else -> translated
        }
    }
}

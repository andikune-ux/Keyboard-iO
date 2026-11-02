package dev.andikune.keyboardio.data

data class NicknameStyle(
    val id: String,
    val name: String,
    val previewSample: String,
    val transform: (String) -> String
)

object NicknameFontGenerator {

    private fun mapChars(input: String, mapUpper: String, mapLower: String, mapDigits: String? = null): String {
        return buildString {
            for (ch in input) {
                when {
                    ch in 'A'..'Z' -> {
                        val idx = ch - 'A'
                        // Code points might be surrogate pairs
                        val codePoint = mapUpper.codePointAt(mapUpper.offsetByCodePoints(0, idx))
                        append(String(Character.toChars(codePoint)))
                    }
                    ch in 'a'..'z' -> {
                        val idx = ch - 'a'
                        val codePoint = mapLower.codePointAt(mapLower.offsetByCodePoints(0, idx))
                        append(String(Character.toChars(codePoint)))
                    }
                    ch in '0'..'9' && mapDigits != null -> {
                        val idx = ch - '0'
                        val codePoint = mapDigits.codePointAt(mapDigits.offsetByCodePoints(0, idx))
                        append(String(Character.toChars(codePoint)))
                    }
                    else -> append(ch)
                }
            }
        }
    }

    val STYLES = listOf(
        NicknameStyle(
            id = "gothic",
            name = "Gothic (Fraktur)",
            previewSample = "𝔎𝔢𝔶𝔟𝔬𝔞𝔯𝔡 𝔦𝔒",
            transform = { text ->
                mapChars(
                    text,
                    "𝔄𝔅ℭ𝔇𝔈𝔉𝔊ℌℑ𝔍𝔎𝔏𝔐𝔑𝔒𝔓𝔔ℜ𝔖𝔗𝔘𝔙𝔚𝔛𝔜ℨ",
                    "𝔞𝔟𝔠𝔡𝔢int𝔤𝔥𝔦𝔧𝔨𝔩𝔪𝔫𝔬𝔭𝔮𝔯𝔰𝔱𝔲𝔳𝔴𝔵𝔶𝔷".replace("int", "𝔣")
                )
            }
        ),
        NicknameStyle(
            id = "calligraphy",
            name = "Calligraphy (Script)",
            previewSample = "𝒦ℯ𝓎𝒷ℴ𝒶𝓇𝒹 𝒾𝒪",
            transform = { text ->
                mapChars(
                    text,
                    "𝒜ℬ𝒞𝒟ℰℱ𝒢ℋℐ𝒥𝒦ℒℳ𝒩𝒪𝒫𝒬ℛ𝒮𝒯𝒰𝒱𝒲𝒳𝒴𝒵",
                    "𝒶𝒷𝒸𝒹ℯ𝒻ℊ𝒽𝒾𝒿𝓀𝓁𝓂𝓃ℴ𝓅𝓆𝓇𝓈𝓉𝓊𝓋𝓌𝓍𝓎𝓏"
                )
            }
        ),
        NicknameStyle(
            id = "bubble",
            name = "Bubble (Circled)",
            previewSample = "Ⓚⓔⓨⓑⓞⓐⓡⓓ ⓘⓄ",
            transform = { text ->
                mapChars(
                    text,
                    "ⒶⒷⒸⒹⒺⒻⒼⒽⒾⒿⓀⓁⓂⓃⓄⓅⓆⓇⓈⓉⓊⓋⓌⓍⓎⓏ",
                    "ⓐⓑⓒⓓⓔⓕⓖⓗⓘⓙⓚⓛⓜⓝⓞⓟⓠⓡⓢⓣⓤⓥⓦⓧⓨⓩ",
                    "⓪①②③④⑤⑥⑦⑧⑨"
                )
            }
        ),
        NicknameStyle(
            id = "small_caps",
            name = "Small Caps",
            previewSample = "ᴋᴇʏʙᴏᴀʀᴅ ɪᴏ",
            transform = { text ->
                val map = mapOf(
                    'a' to "ᴀ", 'b' to "ʙ", 'c' to "ᴄ", 'd' to "ᴅ", 'e' to "ᴇ", 'f' to "ꜰ",
                    'g' to "ɢ", 'h' to "ʜ", 'i' to "ɪ", 'j' to "ᴊ", 'k' to "ᴋ", 'l' to "ʟ",
                    'm' to "ᴍ", 'n' to "ɴ", 'o' to "ᴏ", 'p' to "ᴘ", 'q' to "ǫ", 'r' to "ʀ",
                    's' to "ꜱ", 't' to "ᴛ", 'u' to "ᴜ", 'v' to "ᴠ", 'w' to "ᴡ", 'x' to "x",
                    'y' to "ʏ", 'z' to "ᴢ"
                )
                buildString {
                    for (ch in text) {
                        append(map[ch.lowercaseChar()] ?: ch.toString())
                    }
                }
            }
        ),
        NicknameStyle(
            id = "bold_sans",
            name = "Bold Sans-Serif",
            previewSample = "𝗞𝗲𝘆𝗯𝗼𝗮𝗿𝗱 𝗶𝗢",
            transform = { text ->
                mapChars(
                    text,
                    "𝗔𝗕𝗖𝗗𝗘𝗙𝗚𝗛𝗜𝗝𝗞𝗟𝗠𝗡𝗢𝗣𝗤𝗥𝗦𝗧𝗨𝗩𝗪𝗫𝗬𝗭",
                    "𝗮𝗯𝗰𝗱𝗲𝗳𝗴𝗵𝗶𝗷𝗸𝗹𝗺𝗻𝗼𝗽𝗾𝗿𝘀𝘁𝘂𝘃𝘄𝘅𝘆𝘇",
                    "𝟬𝟭𝟮𝟯𝟰𝟱𝟲𝟳𝟴𝟵"
                )
            }
        ),
        NicknameStyle(
            id = "italic",
            name = "Miring (Italic)",
            previewSample = "𝘒𝘦𝘺𝘣𝘰𝘢𝘳𝘥 𝘪𝘖",
            transform = { text ->
                mapChars(
                    text,
                    "𝘈𝘉𝘊𝘋𝘌𝘍𝘎𝘏𝘐𝘑𝘒𝘓𝘔𝘕𝘖𝘗𝘘𝘙𝘚𝘛𝘜𝘝𝘞𝘟𝘠𝘡",
                    "𝘢𝘣𝘤𝘥𝘦𝘧𝘨𝘩𝘪𝘫𝘬𝘭𝘮𝘯𝘰𝘱𝘲𝘳𝘴𝘵𝘶𝘷𝘸𝘹𝘺𝘻"
                )
            }
        ),
        NicknameStyle(
            id = "bold_italic",
            name = "Tebal-Miring (Bold Italic)",
            previewSample = "𝙆𝙚𝙮𝙗𝙤𝙖𝙧𝙙 𝙞𝙊",
            transform = { text ->
                mapChars(
                    text,
                    "𝘼𝘽𝘾𝘿𝙀𝙁𝙂𝙃𝙄𝙅𝙆𝙇𝙈𝙉𝙊𝙋𝙌𝙍𝙎𝙏𝙐𝙑𝙒𝙓𝙔𝙕",
                    "𝙖𝙗𝙘𝙙𝙚𝙛𝙜𝙝𝙞𝙟𝙠𝙡𝙢𝙣𝙤𝙥𝙦𝙧𝙨𝙩𝙪𝙫𝙬𝙭𝙮𝙯"
                )
            }
        ),
        NicknameStyle(
            id = "monospace",
            name = "Monospace (Mesin)",
            previewSample = "𝙺𝚎𝚢𝚋𝚘𝚊𝚛𝚍 𝚒𝙾",
            transform = { text ->
                mapChars(
                    text,
                    "𝙰𝙱𝙲𝙳𝙴𝙵𝙶𝙷𝙸𝙹𝙺𝙻𝙼𝙽𝙾𝙿𝚀𝚁𝚂𝚃𝚄𝚅𝚆𝚇𝚈𝚉",
                    "𝚊𝚋𝚌𝚍𝚎𝚏𝚐𝚑𝚒𝚓𝚔𝚕𝚖𝚗𝚘𝚙𝚚𝚛𝚜𝚝𝚞𝚟𝚠𝚡𝚢𝚣",
                    "𝟶𝟷𝟸𝟹𝟺𝟻𝟼𝟽𝟾𝟿"
                )
            }
        ),
        NicknameStyle(
            id = "gamer_wing",
            name = "Sultan Game (Sayap)",
            previewSample = "꧁༺Keyboard iO༻꧂",
            transform = { text -> "꧁༺$text༻꧂" }
        ),
        NicknameStyle(
            id = "samurai",
            name = "Samurai King",
            previewSample = "亗 Keyboard iO 亗",
            transform = { text -> "亗 $text 亗" }
        ),
        NicknameStyle(
            id = "neon_star",
            name = "Neon Cyber Star",
            previewSample = "★彡 Keyboard iO 彡★",
            transform = { text -> "★彡 $text 彡★" }
        )
    )

    fun convertText(text: String, styleId: String): String {
        val style = STYLES.find { it.id == styleId } ?: STYLES.first()
        return style.transform(text)
    }
}

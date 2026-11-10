package dev.andikune.keyboardio.data

data class NicknameStyle(
    val id: String,
    val name: String,
    val previewSample: String,
    val transform: (String) -> String
)

object NicknameFontGenerator {

    private fun mappedCodePoint(mapping: String, index: Int): String? {
        val count = mapping.codePointCount(0, mapping.length)
        if (index !in 0 until count) return null
        val offset = mapping.offsetByCodePoints(0, index)
        return String(Character.toChars(mapping.codePointAt(offset)))
    }

    private fun mapChars(input: String, mapUpper: String, mapLower: String, mapDigits: String? = null): String =
        buildString {
            for (ch in input) {
                val mapped = when {
                    ch in 'A'..'Z' -> mappedCodePoint(mapUpper, ch - 'A')
                    ch in 'a'..'z' -> mappedCodePoint(mapLower, ch - 'a')
                    ch in '0'..'9' && mapDigits != null -> mappedCodePoint(mapDigits, ch - '0')
                    else -> null
                }
                append(mapped ?: ch.toString())
            }
        }

    private fun mapAvailableCharacters(input: String, mapping: Map<Char, String>): String =
        buildString {
            for (ch in input) append(mapping[ch] ?: ch.toString())
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
                    "𝔞𝔟𝔠𝔡𝔢𝔣𝔤𝔥𝔦𝔧𝔨𝔩𝔪𝔫𝔬𝔭𝔮𝔯𝔰𝔱𝔲𝔳𝔴𝔵𝔶𝔷"
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
            id = "double_struck",
            name = "Double-Struck",
            previewSample = "𝕂𝕖𝕪𝕓𝕠𝕒𝕣𝕕 𝕚𝕆",
            transform = { text ->
                mapChars(
                    text,
                    "𝔸𝔹ℂ𝔻𝔼𝔽𝔾ℍ𝕀𝕁𝕂𝕃𝕄ℕ𝕆ℙℚℝ𝕊𝕋𝕌𝕍𝕎𝕏𝕐ℤ",
                    "𝕒𝕓𝕔𝕕𝕖𝕗𝕘𝕙𝕚𝕛𝕜𝕝𝕞𝕟𝕠𝕡𝕢𝕣𝕤𝕥𝕦𝕧𝕨𝕩𝕪𝕫",
                    "𝟘𝟙𝟚𝟛𝟜𝟝𝟞𝟟𝟠𝟡"
                )
            }
        ),
        NicknameStyle(
            id = "bold_serif",
            name = "Bold Serif",
            previewSample = "𝐊𝐞𝐲𝐛𝐨𝐚𝐫𝐝 𝐢𝐎",
            transform = { text ->
                mapChars(
                    text,
                    "𝐀𝐁𝐂𝐃𝐄𝐅𝐆𝐇𝐈𝐉𝐊𝐋𝐌𝐍𝐎𝐏𝐐𝐑𝐒𝐓𝐔𝐕𝐖𝐗𝐘𝐙",
                    "𝐚𝐛𝐜𝐝𝐞𝐟𝐠𝐡𝐢𝐣𝐤𝐥𝐦𝐧𝐨𝐩𝐪𝐫𝐬𝐭𝐮𝐯𝐰𝐱𝐲𝐳",
                    "𝟎𝟏𝟐𝟑𝟒𝟓𝟔𝟕𝟖𝟗"
                )
            }
        ),
        NicknameStyle(
            id = "italic_serif",
            name = "Italic Serif",
            previewSample = "𝐾𝑒𝑦𝑏𝑜𝑎𝑟𝑑 𝑖𝑂",
            transform = { text ->
                mapChars(
                    text,
                    "𝐴𝐵𝐶𝐷𝐸𝐹𝐺𝐻𝐼𝐽𝐾𝐿𝑀𝑁𝑂𝑃𝑄𝑅𝑆𝑇𝑈𝑉𝑊𝑋𝑌𝑍",
                    "𝑎𝑏𝑐𝑑𝑒𝑓𝑔ℎ𝑖𝑗𝑘𝑙𝑚𝑛𝑜𝑝𝑞𝑟𝑠𝑡𝑢𝑣𝑤𝑥𝑦𝑧"
                )
            }
        ),
        NicknameStyle(
            id = "bold_italic_serif",
            name = "Bold Italic Serif",
            previewSample = "𝑲𝒆𝒚𝒃𝒐𝒂𝒓𝒅 𝒊𝑶",
            transform = { text ->
                mapChars(
                    text,
                    "𝑨𝑩𝑪𝑫𝑬𝑭𝑮𝑯𝑰𝑱𝑲𝑳𝑴𝑵𝑶𝑷𝑸𝑹𝑺𝑻𝑼𝑽𝑾𝑿𝒀𝒁",
                    "𝒂𝒃𝒄𝒅𝒆𝒇𝒈𝒉𝒊𝒋𝒌𝒍𝒎𝒏𝒐𝒑𝒒𝒓𝒔𝒕𝒖𝒗𝒘𝒙𝒚𝒛"
                )
            }
        ),
        NicknameStyle(
            id = "sans_serif",
            name = "Sans-Serif",
            previewSample = "𝖪𝖾𝗒𝖻𝗈𝖺𝗋𝖽 𝗂𝖮",
            transform = { text ->
                mapChars(
                    text,
                    "𝖠𝖡𝖢𝖣𝖤𝖥𝖦𝖧𝖨𝖩𝖪𝖫𝖬𝖭𝖮𝖯𝖰𝖱𝖲𝖳𝖴𝖵𝖶𝖷𝖸𝖹",
                    "𝖺𝖻𝖼𝖽𝖾𝖿𝗀𝗁𝗂𝗃𝗄𝗅𝗆𝗇𝗈𝗉𝗊𝗋𝗌𝗍𝗎𝗏𝗐𝗑𝗒𝗓",
                    "𝟢𝟣𝟤𝟥𝟦𝟧𝟨𝟩𝟪𝟫"
                )
            }
        ),
        NicknameStyle(
            id = "fraktur_bold",
            name = "Fraktur Bold",
            previewSample = "𝕶𝖊𝖞𝖇𝖔𝖆𝖗𝖉 𝖎𝕺",
            transform = { text ->
                mapChars(
                    text,
                    "𝕬𝕭𝕮𝕯𝕰𝕱𝕲𝕳𝕴𝕵𝕶𝕷𝕸𝕹𝕺𝕻𝕼𝕽𝕾𝕿𝖀𝖁𝖂𝖃𝖄𝖅",
                    "𝖆𝖇𝖈𝖉𝖊𝖋𝖌𝖍𝖎𝖏𝖐𝖑𝖒𝖓𝖔𝖕𝖖𝖗𝖘𝖙𝖚𝖛𝖜𝖝𝖞𝖟"
                )
            }
        ),
        NicknameStyle(
            id = "fullwidth",
            name = "Fullwidth",
            previewSample = "Ｋｅｙｂｏａｒｄ ｉＯ",
            transform = { text ->
                mapChars(
                    text,
                    "ＡＢＣＤＥＦＧＨＩＪＫＬＭＮＯＰＱＲＳＴＵＶＷＸＹＺ",
                    "ａｂｃｄｅｆｇｈｉｊｋｌｍｎｏｐｑｒｓｔｕｖｗｘｙｚ",
                    "０１２３４５６７８９"
                )
            }
        ),
        NicknameStyle(
            id = "squared",
            name = "Squared Outline",
            previewSample = "🄺🄴🅈🄱🄾🄰🅁🄳",
            transform = { text ->
                mapChars(
                    text,
                    "🄰🄱🄲🄳🄴🄵🄶🄷🄸🄹🄺🄻🄼🄽🄾🄿🅀🅁🅂🅃🅄🅅🅆🅇🅈🅉",
                    ""
                )
            }
        ),
        NicknameStyle(
            id = "negative_squared",
            name = "Negative Squared",
            previewSample = "🅺🅴🆈🅱🅾🅰🆁🅳",
            transform = { text ->
                mapChars(
                    text,
                    "🅰🅱🅲🅳🅴🅵🅶🅷🅸🅹🅺🅻🅼🅽🅾🅿🆀🆁🆂🆃🆄🆅🆆🆇🆈🆉",
                    ""
                )
            }
        ),
        NicknameStyle(
            id = "upside_down",
            name = "Upside Down",
            previewSample = "pɹɐoqʎǝʞ",
            transform = { text ->
                mapChars(
                    text,
                    "ɐqɔpǝɟƃɥᴉɾʞןɯuodbɹsʇnʌʍxʎz",
                    "ɐqɔpǝɟƃɥᴉɾʞןɯuodbɹsʇnʌʍxʎz"
                )
            }
        ),
        NicknameStyle(
            id = "superscript",
            name = "Superscript",
            previewSample = "ᵏᵉʸᵇᵒᵃʳᵈ ⁱᴼ",
            transform = { text ->
                mapAvailableCharacters(
                    text,
                    mapOf(
                        'a' to "ᵃ", 'b' to "ᵇ", 'c' to "ᶜ", 'd' to "ᵈ", 'e' to "ᵉ",
                        'f' to "ᶠ", 'g' to "ᵍ", 'h' to "ʰ", 'i' to "ⁱ", 'j' to "ʲ",
                        'k' to "ᵏ", 'l' to "ˡ", 'm' to "ᵐ", 'n' to "ⁿ", 'o' to "ᵒ",
                        'p' to "ᵖ", 'r' to "ʳ", 's' to "ˢ", 't' to "ᵗ", 'u' to "ᵘ",
                        'v' to "ᵛ", 'w' to "ʷ", 'x' to "ˣ", 'y' to "ʸ", 'z' to "ᶻ",
                        '0' to "⁰", '1' to "¹", '2' to "²", '3' to "³", '4' to "⁴",
                        '5' to "⁵", '6' to "⁶", '7' to "⁷", '8' to "⁸", '9' to "⁹"
                    )
                )
            }
        ),
        NicknameStyle(
            id = "subscript",
            name = "Subscript",
            previewSample = "ₖₑᵧᵦₒₐᵣ𝒹",
            transform = { text ->
                mapAvailableCharacters(
                    text,
                    mapOf(
                        'a' to "ₐ", 'e' to "ₑ", 'h' to "ₕ", 'i' to "ᵢ", 'j' to "ⱼ",
                        'k' to "ₖ", 'l' to "ₗ", 'm' to "ₘ", 'n' to "ₙ", 'o' to "ₒ",
                        'p' to "ₚ", 'r' to "ᵣ", 's' to "ₛ", 't' to "ₜ", 'u' to "ᵤ",
                        'v' to "ᵥ", 'x' to "ₓ",
                        '0' to "₀", '1' to "₁", '2' to "₂", '3' to "₃", '4' to "₄",
                        '5' to "₅", '6' to "₆", '7' to "₇", '8' to "₈", '9' to "₉"
                    )
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
        val style = STYLES.find { it.id == styleId } ?: return text
        return style.transform(text)
    }
}

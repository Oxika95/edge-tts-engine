package com.edgetts.engine.edge

import java.util.Locale

data class EdgeTtsVoice(
    val id: String,
    val lang: String,
    val name: String,
) {
    val locale: Locale get() = Locale.forLanguageTag(lang)
}

object EdgeTtsVoices {
    const val PREFIX = "edge:"

    val all: List<EdgeTtsVoice> by lazy { buildVoices() }

    val locales: List<Locale> by lazy {
        all.map { it.locale }.distinctBy { localeKey(it) }
    }

    fun fromId(id: String): EdgeTtsVoice? = all.firstOrNull { it.id == id }

    fun matching(locale: Locale): List<EdgeTtsVoice> {
        val exact = all.filter { localeKey(it.locale) == localeKey(locale) }
        if (exact.isNotEmpty()) return exact
        return all.filter { it.locale.language.equals(locale.language, ignoreCase = true) }
    }

    fun localeKey(locale: Locale): String {
        val language = locale.language.lowercase(Locale.ROOT)
        val country = locale.country.lowercase(Locale.ROOT)
        return if (country.isEmpty()) language else "$language-$country"
    }

    private fun buildVoices(): List<EdgeTtsVoice> {
        return CATALOG.flatMap { (lang, ids) ->
            ids.map { id ->
                EdgeTtsVoice(
                    id = id,
                    lang = lang,
                    name = id.removePrefix("$lang-").removeSuffix("Neural"),
                )
            }
        }
    }

    // Voice catalog ported from Readest's Edge Speech list.
    private val CATALOG: Map<String, List<String>> = linkedMapOf(
        "af-ZA" to listOf("af-ZA-AdriNeural", "af-ZA-WillemNeural"),
        "am-ET" to listOf("am-ET-AmehaNeural", "am-ET-MekdesNeural"),
        "ar-AE" to listOf("ar-AE-FatimaNeural", "ar-AE-HamdanNeural"),
        "ar-BH" to listOf("ar-BH-AliNeural", "ar-BH-LailaNeural"),
        "ar-DZ" to listOf("ar-DZ-AminaNeural", "ar-DZ-IsmaelNeural"),
        "ar-EG" to listOf("ar-EG-SalmaNeural", "ar-EG-ShakirNeural"),
        "ar-IQ" to listOf("ar-IQ-BasselNeural", "ar-IQ-RanaNeural"),
        "ar-JO" to listOf("ar-JO-SanaNeural", "ar-JO-TaimNeural"),
        "ar-KW" to listOf("ar-KW-FahedNeural", "ar-KW-NouraNeural"),
        "ar-LB" to listOf("ar-LB-LaylaNeural", "ar-LB-RamiNeural"),
        "ar-LY" to listOf("ar-LY-ImanNeural", "ar-LY-OmarNeural"),
        "ar-MA" to listOf("ar-MA-JamalNeural", "ar-MA-MounaNeural"),
        "ar-OM" to listOf("ar-OM-AbdullahNeural", "ar-OM-AyshaNeural"),
        "ar-QA" to listOf("ar-QA-AmalNeural", "ar-QA-MoazNeural"),
        "ar-SA" to listOf("ar-SA-HamedNeural", "ar-SA-ZariyahNeural"),
        "ar-SY" to listOf("ar-SY-AmanyNeural", "ar-SY-LaithNeural"),
        "ar-TN" to listOf("ar-TN-HediNeural", "ar-TN-ReemNeural"),
        "ar-YE" to listOf("ar-YE-MaryamNeural", "ar-YE-SalehNeural"),
        "az-AZ" to listOf("az-AZ-BabekNeural", "az-AZ-BanuNeural"),
        "bg-BG" to listOf("bg-BG-BorislavNeural", "bg-BG-KalinaNeural"),
        "bn-BD" to listOf("bn-BD-NabanitaNeural", "bn-BD-PradeepNeural"),
        "bn-IN" to listOf("bn-IN-BashkarNeural", "bn-IN-TanishaaNeural"),
        "bs-BA" to listOf("bs-BA-GoranNeural", "bs-BA-VesnaNeural"),
        "ca-ES" to listOf("ca-ES-EnricNeural", "ca-ES-JoanaNeural"),
        "cs-CZ" to listOf("cs-CZ-AntoninNeural", "cs-CZ-VlastaNeural"),
        "cy-GB" to listOf("cy-GB-AledNeural", "cy-GB-NiaNeural"),
        "da-DK" to listOf("da-DK-ChristelNeural", "da-DK-JeppeNeural"),
        "de-AT" to listOf("de-AT-IngridNeural", "de-AT-JonasNeural"),
        "de-CH" to listOf("de-CH-JanNeural", "de-CH-LeniNeural"),
        "de-DE" to listOf(
            "de-DE-AmalaNeural",
            "de-DE-ConradNeural",
            "de-DE-FlorianMultilingualNeural",
            "de-DE-KatjaNeural",
            "de-DE-KillianNeural",
            "de-DE-SeraphinaMultilingualNeural",
        ),
        "el-GR" to listOf("el-GR-AthinaNeural", "el-GR-NestorasNeural"),
        "en-AU" to listOf("en-AU-NatashaNeural", "en-AU-WilliamNeural"),
        "en-CA" to listOf("en-CA-ClaraNeural", "en-CA-LiamNeural"),
        "en-GB" to listOf(
            "en-GB-LibbyNeural",
            "en-GB-MaisieNeural",
            "en-GB-RyanNeural",
            "en-GB-SoniaNeural",
            "en-GB-ThomasNeural",
        ),
        "en-HK" to listOf("en-HK-SamNeural", "en-HK-YanNeural"),
        "en-IE" to listOf("en-IE-ConnorNeural", "en-IE-EmilyNeural"),
        "en-IN" to listOf("en-IN-NeerjaExpressiveNeural", "en-IN-NeerjaNeural", "en-IN-PrabhatNeural"),
        "en-KE" to listOf("en-KE-AsiliaNeural", "en-KE-ChilembaNeural"),
        "en-NG" to listOf("en-NG-AbeoNeural", "en-NG-EzinneNeural"),
        "en-NZ" to listOf("en-NZ-MitchellNeural", "en-NZ-MollyNeural"),
        "en-PH" to listOf("en-PH-JamesNeural", "en-PH-RosaNeural"),
        "en-SG" to listOf("en-SG-LunaNeural", "en-SG-WayneNeural"),
        "en-TZ" to listOf("en-TZ-ElimuNeural", "en-TZ-ImaniNeural"),
        "en-US" to listOf(
            "en-US-AnaNeural",
            "en-US-AndrewMultilingualNeural",
            "en-US-AndrewNeural",
            "en-US-AriaNeural",
            "en-US-AvaMultilingualNeural",
            "en-US-AvaNeural",
            "en-US-BrianMultilingualNeural",
            "en-US-BrianNeural",
            "en-US-ChristopherNeural",
            "en-US-EmmaMultilingualNeural",
            "en-US-EmmaNeural",
            "en-US-EricNeural",
            "en-US-GuyNeural",
            "en-US-JennyNeural",
            "en-US-MichelleNeural",
            "en-US-RogerNeural",
            "en-US-SteffanNeural",
        ),
        "es-AR" to listOf("es-AR-ElenaNeural", "es-AR-TomasNeural"),
        "es-BO" to listOf("es-BO-MarceloNeural", "es-BO-SofiaNeural"),
        "es-CL" to listOf("es-CL-CatalinaNeural", "es-CL-LorenzoNeural"),
        "es-CO" to listOf("es-CO-GonzaloNeural", "es-CO-SalomeNeural"),
        "es-CR" to listOf("es-CR-JuanNeural", "es-CR-MariaNeural"),
        "es-CU" to listOf("es-CU-BelkysNeural", "es-CU-ManuelNeural"),
        "es-DO" to listOf("es-DO-EmilioNeural", "es-DO-RamonaNeural"),
        "es-EC" to listOf("es-EC-AndreaNeural", "es-EC-LuisNeural"),
        "es-ES" to listOf("es-ES-AlvaroNeural", "es-ES-ElviraNeural", "es-ES-XimenaNeural"),
        "es-US" to listOf("es-US-AlonsoNeural", "es-US-PalomaNeural"),
        "et-EE" to listOf("et-EE-AnuNeural", "et-EE-KertNeural"),
        "fa-IR" to listOf("fa-IR-DilaraNeural", "fa-IR-FaridNeural"),
        "fi-FI" to listOf("fi-FI-HarriNeural", "fi-FI-NooraNeural"),
        "fil-PH" to listOf("fil-PH-AngeloNeural", "fil-PH-BlessicaNeural"),
        "fr-BE" to listOf("fr-BE-CharlineNeural", "fr-BE-GerardNeural"),
        "fr-CA" to listOf("fr-CA-AntoineNeural", "fr-CA-JeanNeural", "fr-CA-SylvieNeural", "fr-CA-ThierryNeural"),
        "fr-CH" to listOf("fr-CH-ArianeNeural", "fr-CH-FabriceNeural"),
        "fr-FR" to listOf(
            "fr-FR-DeniseNeural",
            "fr-FR-EloiseNeural",
            "fr-FR-HenriNeural",
            "fr-FR-RemyMultilingualNeural",
            "fr-FR-VivienneMultilingualNeural",
        ),
        "ga-IE" to listOf("ga-IE-ColmNeural", "ga-IE-OrlaNeural"),
        "gl-ES" to listOf("gl-ES-RoiNeural", "gl-ES-SabelaNeural"),
        "gu-IN" to listOf("gu-IN-DhwaniNeural", "gu-IN-NiranjanNeural"),
        "he-IL" to listOf("he-IL-AvriNeural", "he-IL-HilaNeural"),
        "hi-IN" to listOf("hi-IN-MadhurNeural", "hi-IN-SwaraNeural"),
        "hr-HR" to listOf("hr-HR-GabrijelaNeural", "hr-HR-SreckoNeural"),
        "hu-HU" to listOf("hu-HU-NoemiNeural", "hu-HU-TamasNeural"),
        "id-ID" to listOf("id-ID-ArdiNeural", "id-ID-GadisNeural"),
        "is-IS" to listOf("is-IS-GudrunNeural", "is-IS-GunnarNeural"),
        "it-IT" to listOf(
            "it-IT-DiegoNeural",
            "it-IT-ElsaNeural",
            "it-IT-GiuseppeMultilingualNeural",
            "it-IT-IsabellaNeural",
        ),
        "iu-Cans-CA" to listOf("iu-Cans-CA-SiqiniqNeural", "iu-Cans-CA-TaqqiqNeural"),
        "iu-Latn-CA" to listOf("iu-Latn-CA-SiqiniqNeural", "iu-Latn-CA-TaqqiqNeural"),
        "ja-JP" to listOf("ja-JP-KeitaNeural", "ja-JP-NanamiNeural"),
        "jv-ID" to listOf("jv-ID-DimasNeural", "jv-ID-SitiNeural"),
        "ka-GE" to listOf("ka-GE-EkaNeural", "ka-GE-GiorgiNeural"),
        "kk-KZ" to listOf("kk-KZ-AigulNeural", "kk-KZ-DauletNeural"),
        "km-KH" to listOf("km-KH-PisethNeural", "km-KH-SreymomNeural"),
        "kn-IN" to listOf("kn-IN-GaganNeural", "kn-IN-SapnaNeural"),
        "ko-KR" to listOf("ko-KR-HyunsuMultilingualNeural", "ko-KR-InJoonNeural", "ko-KR-SunHiNeural"),
        "lo-LA" to listOf("lo-LA-ChanthavongNeural", "lo-LA-KeomanyNeural"),
        "lt-LT" to listOf("lt-LT-LeonasNeural", "lt-LT-OnaNeural"),
        "lv-LV" to listOf("lv-LV-EveritaNeural", "lv-LV-NilsNeural"),
        "mk-MK" to listOf("mk-MK-AleksandarNeural", "mk-MK-MarijaNeural"),
        "ml-IN" to listOf("ml-IN-MidhunNeural", "ml-IN-SobhanaNeural"),
        "mn-MN" to listOf("mn-MN-BataaNeural", "mn-MN-YesuiNeural"),
        "mr-IN" to listOf("mr-IN-AarohiNeural", "mr-IN-ManoharNeural"),
        "ms-MY" to listOf("ms-MY-OsmanNeural", "ms-MY-YasminNeural"),
        "mt-MT" to listOf("mt-MT-GraceNeural", "mt-MT-JosephNeural"),
        "my-MM" to listOf("my-MM-NilarNeural", "my-MM-ThihaNeural"),
        "nb-NO" to listOf("nb-NO-FinnNeural", "nb-NO-PernilleNeural"),
        "ne-NP" to listOf("ne-NP-HemkalaNeural", "ne-NP-SagarNeural"),
        "nl-BE" to listOf("nl-BE-ArnaudNeural", "nl-BE-DenaNeural"),
        "nl-NL" to listOf("nl-NL-ColetteNeural", "nl-NL-FennaNeural", "nl-NL-MaartenNeural"),
        "pl-PL" to listOf("pl-PL-MarekNeural", "pl-PL-ZofiaNeural"),
        "ps-AF" to listOf("ps-AF-GulNawazNeural", "ps-AF-LatifaNeural"),
        "pt-BR" to listOf("pt-BR-AntonioNeural", "pt-BR-FranciscaNeural", "pt-BR-ThalitaMultilingualNeural"),
        "pt-PT" to listOf("pt-PT-DuarteNeural", "pt-PT-RaquelNeural"),
        "ro-RO" to listOf("ro-RO-AlinaNeural", "ro-RO-EmilNeural"),
        "ru-RU" to listOf("ru-RU-DmitryNeural", "ru-RU-SvetlanaNeural"),
        "si-LK" to listOf("si-LK-SameeraNeural", "si-LK-ThiliniNeural"),
        "sk-SK" to listOf("sk-SK-LukasNeural", "sk-SK-ViktoriaNeural"),
        "sl-SI" to listOf("sl-SI-PetraNeural", "sl-SI-RokNeural"),
        "so-SO" to listOf("so-SO-MuuseNeural", "so-SO-UbaxNeural"),
        "sq-AL" to listOf("sq-AL-AnilaNeural", "sq-AL-IlirNeural"),
        "sr-RS" to listOf("sr-RS-NicholasNeural", "sr-RS-SophieNeural"),
        "su-ID" to listOf("su-ID-JajangNeural", "su-ID-TutiNeural"),
        "sv-SE" to listOf("sv-SE-MattiasNeural", "sv-SE-SofieNeural"),
        "sw-KE" to listOf("sw-KE-RafikiNeural", "sw-KE-ZuriNeural"),
        "sw-TZ" to listOf("sw-TZ-DaudiNeural", "sw-TZ-RehemaNeural"),
        "ta-IN" to listOf("ta-IN-PallaviNeural", "ta-IN-ValluvarNeural"),
        "ta-LK" to listOf("ta-LK-KumarNeural", "ta-LK-SaranyaNeural"),
        "ta-MY" to listOf("ta-MY-KaniNeural", "ta-MY-SuryaNeural"),
        "ta-SG" to listOf("ta-SG-AnbuNeural", "ta-SG-VenbaNeural"),
        "te-IN" to listOf("te-IN-MohanNeural", "te-IN-ShrutiNeural"),
        "th-TH" to listOf("th-TH-NiwatNeural", "th-TH-PremwadeeNeural"),
        "tr-TR" to listOf("tr-TR-AhmetNeural", "tr-TR-EmelNeural"),
        "uk-UA" to listOf("uk-UA-OstapNeural", "uk-UA-PolinaNeural"),
        "ur-IN" to listOf("ur-IN-GulNeural", "ur-IN-SalmanNeural"),
        "ur-PK" to listOf("ur-PK-AsadNeural", "ur-PK-UzmaNeural"),
        "uz-UZ" to listOf("uz-UZ-MadinaNeural", "uz-UZ-SardorNeural"),
        "vi-VN" to listOf("vi-VN-HoaiMyNeural", "vi-VN-NamMinhNeural"),
        "zh-CN" to listOf(
            "zh-CN-XiaoxiaoNeural",
            "zh-CN-XiaoyiNeural",
            "zh-CN-YunjianNeural",
            "zh-CN-YunxiNeural",
            "zh-CN-YunxiaNeural",
            "zh-CN-YunyangNeural",
            "zh-CN-liaoning-XiaobeiNeural",
            "zh-CN-shaanxi-XiaoniNeural",
        ),
        "zh-HK" to listOf("zh-HK-HiuGaaiNeural", "zh-HK-HiuMaanNeural", "zh-HK-WanLungNeural"),
        "zh-TW" to listOf("zh-TW-HsiaoChenNeural", "zh-TW-HsiaoYuNeural", "zh-TW-YunJheNeural"),
        "zu-ZA" to listOf("zu-ZA-ThandoNeural", "zu-ZA-ThembaNeural"),
    )
}

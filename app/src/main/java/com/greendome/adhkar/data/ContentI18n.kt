package com.greendome.adhkar.data

object ContentI18n {
    fun dhikr(textAr: String, lang: String): String? = lookup(DHIKR, textAr, lang)

    fun collectionTitle(id: String, lang: String): String? =
        COLLECTIONS[id]?.get(lang)?.takeIf { it.isNotBlank() }

    fun reciter(nameAr: String, lang: String): String? = lookup(RECITERS, nameAr, lang)

    fun azkarMeaning(textAr: String, lang: String): String? {
        lookup(AZKAR_MEANING, textAr, lang)?.let { return it }
        val n = normalizeAr(textAr)
        return when {
            "الحي القيوم" in n && "لا تاخذه سنه" in n -> AYAT_KURSI[lang]
            "قل هو الله احد" in n -> IKHLAS[lang]
            "قل اعوذ برب الفلق" in n -> FALAQ[lang]
            "قل اعوذ برب الناس" in n -> NAS[lang]
            "سيد الاستغفار" in n || ("انت ربي" in n && "لا يغفر الذنوب" in n) ||
                ("خلقتني وانا عبدك" in n && "لا يغفر الذنوب الا انت" in n) -> SAYYID_ISTIGHFAR[lang]
            "فتحه ونصره" in n -> FATH_NASR[lang]
            "اصبحنا واصبح الملك لله" in n && "هذا اليوم" in n -> ASBAHNA_MULK[lang]
            "امسينا وامسى الملك لله" in n -> AMSAYNA_MULK[lang]
            "بك اصبحنا" in n && "اليك النشور" in n -> BAKKA_ASBAHNA[lang]
            "بك امسينا" in n && "اليك المصير" in n -> BAKKA_AMSAYNA[lang]
            "عالم الغيب والشهاده" in n -> ALIM_GHAYB[lang]
            "اصبحت اشهدك" in n -> ASHHADUKA_MORNING[lang]
            "امسيت اشهدك" in n -> ASHHADUKA_EVENING[lang]
            "ان اشرك بك وانا اعلم" in n -> SHIRK_ISTIGHFAR[lang]
            "ما اصبح بي من نعمه" in n -> NIMA_MORNING[lang]
            "ما امسي بي من نعمه" in n -> NIMA_EVENING[lang]
            "عافني في بدني" in n -> AFIYAH_BADAN[lang]
            "من الهم والحزن" in n -> HAMM_HAZAN[lang]
            "اسالك العفو والعافيه" in n && "استر عوراتي" in n -> AFW_AFIYAH[lang]
            "لا يضر مع اسمه شيء" in n -> BISMILLAH_LA_YADURR[lang]
            "اصبحنا علي فطره الاسلام" in n -> FITRA_MORNING[lang]
            "امسينا علي فطره الاسلام" in n -> FITRA_EVENING[lang]
            "علما نافعا" in n && "رزقا طيبا" in n -> ILM_NAFI[lang]
            "استغفر الله العظيم الذي لا اله الا هو الحي القيوم" in n -> ISTIGHFAR_AZIM[lang]
            "عده خلقه" in n && "عرشه" in n && "كلماته" in n -> TASBIH_COUNT[lang]
            "لا اله الا الله وحده لا شريك له" in n && "له الملك وله الحمد" in n -> TAWHID[lang]
            "خلقت نفسي وانت توفاها" in n -> SLEEP_TAWAFFA[lang]
            "قني عذابك يوم تبعث" in n -> SLEEP_QINI[lang]
            "باسمك اللهم اموت واحيا" in n -> SLEEP_AMUTU[lang]
            "عافاني في جسدي ورد علي روحي" in n -> WAKE_AFAANI[lang]
            "مثل ما يقول الموذن" in n || "حيعلتين" in n -> ADHAN_REPEAT[lang]
            "رب هذه الدعوه التامه" in n -> ADHAN_WASILA[lang]
            "خير المولج" in n -> HOME_ENTER[lang]
            "ان اضل او اضل" in n -> HOME_LEAVE[lang]
            else -> null
        }
    }

    fun virtue(virtueAr: String, lang: String): String? {
        lookup(VIRTUES, virtueAr, lang)?.let { return it }
        val n = normalizeAr(virtueAr)
        return when {
            "سؤال خير اليوم" in n -> VIRTUE_KHAIR_YAWM[lang]
            "تفويض الامر لله والتوكل عليه" in n -> VIRTUE_TAFWID[lang]
            "الحفظ من شر النفس والشيطان" in n -> VIRTUE_NAFS[lang]
            "الوقايه من الشرك" in n -> VIRTUE_SHIRK[lang]
            "اداء شكر اليوم" in n -> VIRTUE_SHUKR_YAWM[lang]
            "سؤال العافيه" in n -> VIRTUE_AFIYAH[lang]
            "تفريج الهموم" in n -> VIRTUE_HAMM[lang]
            "الحفظ الشامل" in n -> VIRTUE_HIFZ[lang]
            "اصلاح الشان كله" in n -> VIRTUE_SHAAN[lang]
            "سؤال الفتح والنور" in n -> VIRTUE_FATH[lang]
            "تجديد الايمان" in n -> VIRTUE_IMAN[lang]
            "اجر مضاعف" in n -> VIRTUE_AJR[lang]
            "سؤال العلم النافع" in n -> VIRTUE_ILM[lang]
            "تطهير القلب" in n -> VIRTUE_QALB[lang]
            "سؤال خير الليله" in n -> VIRTUE_KHAIR_LAYL[lang]
            "تفويض الامر لله عند المساء" in n -> VIRTUE_TAFWID_MASA[lang]
            "اداء شكر الليله" in n -> VIRTUE_SHUKR_LAYL[lang]
            "ذوات السموم" in n -> VIRTUE_SAMM[lang]
            "ختم اليوم بالثبات" in n -> VIRTUE_THABAT[lang]
            "المعوذات بعد الصلاه" in n -> VIRTUE_MUAWWIDHAT[lang]
            else -> null
        }
    }

    private fun lookup(table: Map<String, Map<String, String>>, source: String, lang: String): String? {
        if (lang == "ar") return null
        val exact = table[source]?.get(lang)
        if (!exact.isNullOrBlank()) return exact
        val normalized = normalizeAr(source)
        table.forEach { (key, translations) ->
            if (normalizeAr(key) == normalized) {
                return translations[lang]?.takeIf { it.isNotBlank() }
            }
        }
        return null
    }

    /** للغات غير الإنجليزية: لا تُعرض الإنجليزية بديلاً. */
    fun withoutEnglishFallback(lang: String, arabic: String, english: String = ""): String =
        if (lang == "en") english.ifBlank { arabic } else arabic

    fun normalizeAr(text: String): String {
        val tashkeel = Regex("[\\u064B-\\u065F\\u0670\\u06D6-\\u06ED]")
        return text
            .replace(tashkeel, "")
            .replace('أ', 'ا')
            .replace('إ', 'ا')
            .replace('آ', 'ا')
            .replace('ة', 'ه')
            .replace('ى', 'ي')
            .replace('ؤ', 'و')
            .replace('ئ', 'ي')
            .replace("ـ", "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private val COLLECTIONS = mapOf(
        "morning" to mapOf(
            "fr" to "Adhkar du matin",
            "es" to "Adhkar de la mañana",
            "tr" to "Sabah zikirleri",
            "ur" to "صبح کے اذکار",
            "id" to "Dzikir pagi",
            "hi" to "प्रातःकालीन अज़कार",
        ),
        "evening" to mapOf(
            "fr" to "Adhkar du soir",
            "es" to "Adhkar de la tarde",
            "tr" to "Akşam zikirleri",
            "ur" to "شام کے اذکار",
            "id" to "Dzikir petang",
            "hi" to "सायंकालीन अज़कार",
        ),
        "after_prayer" to mapOf(
            "fr" to "Après la prière obligatoire",
            "es" to "Después de la oración obligatoria",
            "tr" to "Farż namazdan sonra",
            "ur" to "فرض نماز کے بعد",
            "id" to "Setelah shalat wajib",
            "hi" to "अनिवार्य नमाज़ के बाद",
        ),
        "sleep" to mapOf(
            "fr" to "Adhkar du sommeil",
            "es" to "Adhkar del sueño",
            "tr" to "Uyku zikirleri",
            "ur" to "سونے کے اذکار",
            "id" to "Dzikir tidur",
            "hi" to "सोने के अज़कार",
        ),
        "wake_up" to mapOf(
            "fr" to "Au réveil",
            "es" to "Al despertar",
            "tr" to "Uyanınca",
            "ur" to "بیداری کے اذکار",
            "id" to "Saat bangun",
            "hi" to "जागने के अज़कार",
        ),
        "adhan" to mapOf(
            "fr" to "En entendant l'adhan",
            "es" to "Al oír el adhan",
            "tr" to "Ezan duyunca",
            "ur" to "اذان سن کر",
            "id" to "Saat mendengar adzan",
            "hi" to "अज़ान सुनते समय",
        ),
        "home" to mapOf(
            "fr" to "Entrer et sortir de chez soi",
            "es" to "Entrar y salir de casa",
            "tr" to "Eve girip çıkınca",
            "ur" to "گھر میں داخلے اور خروج",
            "id" to "Masuk dan keluar rumah",
            "hi" to "घर में प्रवेश और निर्गमन",
        ),
        "jawami" to mapOf(
            "fr" to "Jawami du tasbih",
            "es" to "Jawami del tasbih",
            "tr" to "Cevami tesbih",
            "ur" to "جوامع التسبیح",
            "id" to "Jawami tasbih",
            "hi" to "जवामी तस्बीह",
        ),
        "favorites" to mapOf(
            "fr" to "Adhkar favoris",
            "es" to "Adhkar favoritos",
            "tr" to "Favori zikirler",
            "ur" to "پسندیدہ اذکار",
            "id" to "Dzikir favorit",
            "hi" to "पसंदीदा अज़कार",
        ),
    )

    private val RECITERS = mapOf(
        "أصوات متنوعة" to mapOf(
            "tr" to "Çeşitli sesler",
            "ur" to "متنوع آوازیں",
            "id" to "Berbagai suara",
            "hi" to "विविध आवाज़ें",
        ),
        "ناصر الدين صبيحات" to mapOf(
            "tr" to "Naser Al-Din Subaihat",
            "ur" to "ناصر الدین صبیحات",
            "id" to "Naser Al-Din Subaihat",
            "hi" to "नासिर अल-दीन सुबैहात",
        ),
    )

    private val AYAT_KURSI = mapOf(
        "en" to "Ayat al-Kursi: Allah — there is no god but He, the Ever-Living, the Self-Sustaining. Neither slumber nor sleep overtakes Him. To Him belongs what is in the heavens and the earth.",
        "fr" to "Le Verset du Trône : Allah, nulle divinité que Lui, le Vivant, le Subsistant. Ni somnolence ni sommeil ne Le saisissent. À Lui appartient ce qui est dans les cieux et la terre.",
        "es" to "El versículo del Trono: Allah, no hay dios sino Él, el Viviente, el Subsistente. No Le toma ni somnolencia ni sueño. Suyo es lo que hay en los cielos y la tierra.",
        "tr" to "Ayetel Kürsi: Allah, O'ndan başka ilah yoktur. Diridir, kaimdir. O'nu ne uyuklama tutar ne uyku. Göklerde ve yerde ne varsa O'nundur.",
        "ur" to "آیت الکرسی: اللہ، اس کے سوا کوئی معبود نہیں، زندہ ہے قائم رکھنے والا۔ اسے نہ اونگھ آتی ہے نہ نیند۔ آسمانوں اور زمین میں جو کچھ ہے اسی کا ہے۔",
        "id" to "Ayat Kursi: Allah, tidak ada tuhan selain Dia, Yang Mahahidup, Yang terus-menerus mengurus. Tidak mengantuk dan tidak tidur. Milik-Nya apa yang di langit dan di bumi.",
        "hi" to "आयतुल कुर्सी: अल्लाह, उसके सिवा कोई माबूद नहीं, सदा जीवित, सबका संभालने वाला। उसे न ऊँघ आती है न नींद। आसमानों और ज़मीन में जो कुछ है उसी का है।",
    )
    private val IKHLAS = mapOf(
        "en" to "Surah Al-Ikhlas: Say, He is Allah, One. Allah, the Eternal Refuge. He neither begets nor is born, and there is none comparable to Him.",
        "fr" to "Sourate Al-Ikhlas : Dis : Il est Allah, Unique. Allah, le Seul à qui on s'adresse. Il n'engendre pas et n'est pas engendré, et nul n'est égal à Lui.",
        "es" to "Sura Al-Ijlas: Di: Él es Allah, Uno. Allah, el Eterno. No engendra ni es engendrado, y no hay nadie comparable a Él.",
        "tr" to "İhlas suresi: De ki: O Allah birdir. Allah Samed'dir. O doğurmamış ve doğmamıştır. Hiçbir şey O'na denk değildir.",
        "ur" to "سورہ اخلاص: کہو وہ اللہ ایک ہے۔ اللہ بے نیاز ہے۔ نہ اس نے جنا نہ جنا گیا، اور کوئی اس کا ہمسر نہیں۔",
        "id" to "Surah Al-Ikhlas: Katakanlah, Dialah Allah, Yang Maha Esa. Allah tempat bergantung. Dia tidak beranak dan tidak diperanakkan, dan tidak ada sesuatu pun yang setara dengan-Nya.",
        "hi" to "सूरह इख़लास: कहो, वह अल्लाह एक है। अल्लाह बेनियाज़ है। न उसने जना न जना गया, और कोई उसका सानी नहीं।",
    )
    private val FALAQ = mapOf(
        "en" to "Surah Al-Falaq: I seek refuge in the Lord of daybreak from the evil of what He created, from darkness when it gathers, from those who blow on knots, and from the envier when he envies.",
        "fr" to "Sourate Al-Falaq : Je cherche protection auprès du Seigneur de l'aube naissante contre le mal de ce qu'Il a créé.",
        "es" to "Sura Al-Falaq: Busco refugio en el Señor del amanecer del mal de lo que creó.",
        "tr" to "Felak suresi: Sabahın Rabbine sığınırım, yarattığının şerrinden, karanlık çöktüğünde, düğümlere üfürenlerin şerrinden ve hasetçi haset ettiği zaman.",
        "ur" to "سورہ فلق: میں صبح کے رب کی پناہ مانگتا ہوں اس کی مخلوق کے شر سے، اندھیرے کے شر سے جب چھا جائے، گرہوں میں پھونکنے والیوں کے شر سے، اور حاسد کے شر سے جب حسد کرے۔",
        "id" to "Surah Al-Falaq: Aku berlindung kepada Tuhan yang menguasai subuh dari kejahatan makhluk-Nya, dari gelap gulita, dari yang meniup pada buhul, dan dari pendengki ketika ia dengki.",
        "hi" to "सूरह फ़लक: मैं सुबह के रब की पनाह लेता हूँ उसकी सृष्टि के शर से, अंधेरे के शर से, गिरहों में फूँकने वालों से, और हसद करने वाले से।",
    )
    private val NAS = mapOf(
        "en" to "Surah An-Nas: I seek refuge in the Lord of mankind, the King of mankind, the God of mankind, from the evil of the whisperer who withdraws.",
        "fr" to "Sourate An-Nas : Je cherche protection auprès du Seigneur des hommes, Roi des hommes, Dieu des hommes, contre le mal du mauvais conseiller furtif.",
        "es" to "Sura An-Nas: Busco refugio en el Señor de las gentes, Rey de las gentes, Dios de las gentes, del mal del susurrador que se retira.",
        "tr" to "Nas suresi: İnsanların Rabbine, Melikine, İlahına sığınırım; o sinsi vesvesecinin şerrinden.",
        "ur" to "سورہ ناس: میں لوگوں کے رب، بادشاہ اور معبود کی پناہ مانگتا ہوں، وسوسہ ڈالنے والے کے شر سے جو چھپ جاتا ہے۔",
        "id" to "Surah An-Nas: Aku berlindung kepada Tuhan manusia, Raja manusia, Sembahan manusia, dari kejahatan bisikan yang tersembunyi.",
        "hi" to "सूरह नास: मैं लोगों के रब, बादशाह और माबूद की पनाह लेता हूँ, उस वसवसा डालने वाले के शर से जो छिप जाता है।",
    )
    private val SAYYID_ISTIGHFAR = mapOf(
        "en" to "The master of seeking forgiveness: O Allah, You are my Lord, there is no god but You. You created me and I am Your servant. I abide by Your covenant as best I can. I seek refuge in You from the evil of what I have done. I acknowledge Your favor upon me and I acknowledge my sin, so forgive me, for none forgives sins except You.",
        "fr" to "Le maître de la demande de pardon : Ô Allah, Tu es mon Seigneur, nulle divinité que Toi. Tu m'as créé et je suis Ton serviteur…",
        "es" to "El señor del perdón: Oh Allah, Tú eres mi Señor, no hay dios sino Tú. Tú me creaste y soy Tu siervo…",
        "tr" to "İstiğfarın efendisi: Allahım, Sen benim Rabbimsin, Senden başka ilah yoktur. Beni Sen yarattın, ben Senin kulunum. Gücüm yettiğince ahdine bağlıyım. Yaptığımın şerrinden Sana sığınırım. Nimetini ve günahımı itiraf ederim; beni bağışla, günahları ancak Sen bağışlarsın.",
        "ur" to "سید الاستغفار: اے اللہ تو میرا رب ہے، تیرے سوا کوئی معبود نہیں، تو نے مجھے پیدا کیا اور میں تیرا بندہ ہوں… گناہوں کو تیرے سوا کوئی نہیں بخشتا۔",
        "id" to "Penghulu istighfar: Ya Allah, Engkau Tuhanku, tidak ada tuhan selain Engkau. Engkau menciptakanku dan aku hamba-Mu… Tidak ada yang mengampuni dosa kecuali Engkau.",
        "hi" to "इस्तिग़फ़ार का सरदार: ऐ अल्लाह तू मेरा रब है, तेरे सिवा कोई माबूद नहीं। तूने मुझे पैदा किया और मैं तेरा बंदा हूँ… गुनाह तेरे सिवा कोई नहीं बख्शता।",
    )

    private val DHIKR = mapOf(
        "سبحان الله" to mapOf(
            "en" to "Subhan Allah",
            "fr" to "Soubhan Allah",
            "es" to "Subhan Allah",
            "tr" to "Sübhanallah",
            "ur" to "سبحان اللہ",
            "id" to "Subhanallah",
            "hi" to "सुब्हानल्लाह",
        ),
        "الحمدلله" to mapOf(
            "en" to "Alhamdulillah",
            "fr" to "Alhamdulillah",
            "es" to "Alhamdulillah",
            "tr" to "Elhamdülillah",
            "ur" to "الحمد للہ",
            "id" to "Alhamdulillah",
            "hi" to "अलहम्दुलिल्लाह",
        ),
        "لا إله إلا الله" to mapOf(
            "en" to "La ilaha illallah",
            "fr" to "La ilaha illallah",
            "es" to "La ilaha illallah",
            "tr" to "Lâ ilâhe illallah",
            "ur" to "لا الہ الا اللہ",
            "id" to "La ilaha illallah",
            "hi" to "ला इलाहा इल्लल्लाह",
        ),
        "الله أكبر" to mapOf(
            "en" to "Allahu Akbar",
            "fr" to "Allahu Akbar",
            "es" to "Allahu Akbar",
            "tr" to "Allahu ekber",
            "ur" to "اللہ اکبر",
            "id" to "Allahu Akbar",
            "hi" to "अल्लाहु अकबर",
        ),
        "لا حول ولا قوة الا بالله" to mapOf(
            "en" to "La hawla wa la quwwata illa billah",
            "fr" to "La hawla wa la quwwata illa billah",
            "es" to "La hawla wa la quwwata illa billah",
            "tr" to "Lâ havle velâ kuvvete illâ billah",
            "ur" to "لا حول ولا قوة الا باللہ",
            "id" to "La hawla wa la quwwata illa billah",
            "hi" to "ला हौला वला कुव्वता इल्ला बिल्लाह",
        ),
        "سبحان الله ، والحمد لله ، ولا إله إلا الله ، والله أكبر" to mapOf(
            "en" to "Subhan Allah, Alhamdulillah, La ilaha illallah, Allahu Akbar",
            "fr" to "Soubhan Allah, Alhamdulillah, La ilaha illallah, Allahu Akbar",
            "es" to "Subhan Allah, Alhamdulillah, La ilaha illallah, Allahu Akbar",
            "tr" to "Sübhanallah, elhamdülillah, lâ ilâhe illallah, Allahu ekber",
            "ur" to "سبحان اللہ، والحمد للہ، ولا الہ الا اللہ، واللہ اکبر",
            "id" to "Subhanallah, alhamdulillah, la ilaha illallah, Allahu Akbar",
            "hi" to "सुब्हानल्लाह, अलहम्दुलिल्लाह, ला इलाहा इल्लल्लाह, अल्लाहु अकबर",
        ),
        "أستغفر الله وأتوب اليه" to mapOf(
            "en" to "I seek Allah's forgiveness and I repent to Him",
            "fr" to "Je demande pardon à Allah et je me repens à Lui",
            "es" to "Pido perdón a Allah y me arrepiento ante Él",
            "tr" to "Estağfirullah ve O'na tövbe ederim",
            "ur" to "استغفر اللہ وأتوب الیہ",
            "id" to "Aku memohon ampun kepada Allah dan bertobat kepada-Nya",
            "hi" to "मैं अल्लाह से क्षमा माँगता हूँ और उसी की ओर लौटता हूँ",
        ),
        "اللهم صل وسلم وبارك على نبينا محمد" to mapOf(
            "en" to "O Allah, send blessings, peace, and grace upon our Prophet Muhammad",
            "fr" to "Ô Allah, accorde la prière, la paix et la bénédiction à notre Prophète Muhammad",
            "es" to "Oh Allah, envía bendiciones, paz y gracia a nuestro Profeta Muhammad",
            "tr" to "Allahım! Peygamberimiz Muhammed'e salât, selam ve bereket ver",
            "ur" to "اے اللہ! ہمارے نبی محمد پر درود و سلام اور برکت نازل فرما",
            "id" to "Ya Allah, limpahkan shalawat, salam, dan berkah kepada Nabi kami Muhammad",
            "hi" to "ऐ अल्लाह, हमारे नबी मुहम्मद पर दुरूद, सलाम और बरकत भेज",
        ),
        "سبحان الله وبحمده" to mapOf(
            "en" to "Subhan Allahi wa bihamdih",
            "fr" to "Soubhan Allahi wa bihamdih",
            "es" to "Subhan Allahi wa bihamdih",
            "tr" to "Sübhanallahi ve bihamdih",
            "ur" to "سبحان اللہ وبحمدہ",
            "id" to "Subhanallah wa bihamdih",
            "hi" to "सुब्हानल्लाहि व बिहम्दिही",
        ),
        "سبحان الله العظيم" to mapOf(
            "tr" to "Sübhanallahi'l-azîm",
            "ur" to "سبحان اللہ العظیم",
            "id" to "Subhanallah Al-Azim",
            "hi" to "सुब्हानल्लाह अल-अज़ीम",
        ),
        "لَا إلَه إلّا اللهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ وَهُوَ عَلَى كُلُّ شَيْءٍ قَدِيرٍ" to mapOf(
            "tr" to "Allah'tan başka ilah yoktur; O tektir, ortağı yoktur. Mülk O'nundur, hamd O'nadır. O her şeye kadirdir.",
            "ur" to "اللہ کے سوا کوئی معبود نہیں، وہ یکتا ہے، اس کا کوئی شریک نہیں، بادشاہی اسی کی ہے اور تعریف اسی کے لیے ہے، اور وہ ہر چیز پر قادر ہے۔",
            "id" to "Tidak ada tuhan selain Allah, Yang Maha Esa, tiada sekutu bagi-Nya. Bagi-Nya kerajaan dan pujian, dan Dia Mahakuasa atas segala sesuatu.",
            "hi" to "अल्लाह के सिवा कोई माबूद नहीं, वह अकेला है, उसका कोई साझी नहीं। राज्य उसी का है, स्तुति उसी की है, और वह हर चीज़ पर सक्षम है।",
        ),
        "سبحان الله وبحمده عدد خلقه ورضا نفسه وزنة عرشه ومداد كلماته" to mapOf(
            "tr" to "Allah'ı hamd ile tesbih ederim; yaratıklarının sayısı, rızası, Arş'ının ağırlığı ve kelimelerinin mürekkebi kadar.",
            "ur" to "سبحان اللہ وبحمدہ، اس کی مخلوق کی تعداد، اس کی رضا، اس کے عرش کے وزن اور اس کے کلمات کی روشنائی کے برابر۔",
            "id" to "Mahasuci Allah dengan memuji-Nya, sebanyak ciptaan-Nya, keridaan-Nya, berat Arsy-Nya, dan tinta firman-Nya.",
            "hi" to "अल्लाह की पवित्रता और स्तुति — उसकी सृष्टि की संख्या, उसकी प्रसन्नता, उसके सिंहासन के भार और उसके शब्दों की स्याही जितनी।",
        ),
        "لا إله إلا أنت سبحانك إني كنت من ظالمين" to mapOf(
            "tr" to "Senden başka ilah yoktur. Seni tesbih ederim. Gerçekten ben zalimlerden oldum.",
            "ur" to "تیرے سوا کوئی معبود نہیں، تو پاک ہے، بے شک میں ظالموں میں سے تھا۔",
            "id" to "Tidak ada tuhan selain Engkau. Mahasuci Engkau. Sungguh aku termasuk orang-orang zalim.",
            "hi" to "तेरे सिवा कोई माबूद नहीं। तू पाक है। निश्चय मैं ज़ालिमों में से था।",
        ),
        "يا ذا الجلال والإكرام" to mapOf(
            "tr" to "Ey celâl ve ikram sahibi!",
            "ur" to "اے جلال و اکرام والے!",
            "id" to "Wahai Pemilik keagungan dan kemuliaan",
            "hi" to "ऐ महिमा और उदारता के स्वामी",
        ),
        "حسبي الله ونعم الوكيل" to mapOf(
            "tr" to "Allah bana yeter, O ne güzel vekildir.",
            "ur" to "اللہ مجھے کافی ہے اور وہ بہترین کارساز ہے۔",
            "id" to "Cukuplah Allah bagiku, dan Dia sebaik-baik pelindung.",
            "hi" to "अल्लाह मुझे काफ़ी है, और वह सबसे अच्छा कार्यसाधक है।",
        ),
        "جميع الأذكار (متتابعة)" to mapOf(
            "en" to "All adhkar (sequential)",
            "fr" to "Tous les adhkar (en séquence)",
            "es" to "Todos los adhkar (en secuencia)",
            "tr" to "Tüm zikirler (ardışık)",
            "ur" to "تمام اذکار (مسلسل)",
            "id" to "Semua dzikir (berurutan)",
            "hi" to "सभी अज़कार (क्रम से)",
        ),
        "الله أكبر الله أكبر لا إله إلا الله والله أكبر الله أكبر ولله الحمد" to mapOf(
            "tr" to "Allahu ekber, Allahu ekber, lâ ilâhe illallah, Allahu ekber, Allahu ekber, ve lillahi'l-hamd",
            "ur" to "اللہ اکبر اللہ اکبر لا الہ الا اللہ واللہ اکبر اللہ اکبر وللہ الحمد",
            "id" to "Allahu Akbar, Allahu Akbar, la ilaha illallah, Allahu Akbar, Allahu Akbar, wa lillahil hamd",
            "hi" to "अल्लाहु अकबर, अल्लाहु अकबर, ला इलाहा इल्लल्लाह, अल्लाहु अकबर, अल्लाहु अकबर, व लिल्लाहिल हम्द",
        ),
        "سُبْحَانَ اللهِ وَبِحَمْدِهِ عَدَدَ خَلْقِهِ، وَرِضَا نَفْسِهِ وَزِنَةَ عَرْشِهِ، وَمِدَادَ كَلِمَاتِهِ." to mapOf(
            "tr" to "Allah'ı hamd ile tesbih ederim; yaratıklarının sayısı, rızası, Arş'ının ağırlığı ve kelimelerinin mürekkebi kadar.",
            "ur" to "اللہ کی تسبیح اور حمد، اس کی مخلوق کی تعداد، اس کی رضا، اس کے عرش کے وزن اور اس کے کلمات کی روشنائی کے برابر۔",
            "id" to "Mahasuci Allah dengan memuji-Nya, sebanyak ciptaan-Nya, keridaan-Nya, berat Arsy-Nya, dan tinta firman-Nya.",
            "hi" to "अल्लाह की पवित्रता और स्तुति — उसकी सृष्टि की संख्या, उसकी प्रसन्नता, उसके सिंहासन के भार और उसके शब्दों की स्याही जितनी।",
        ),
    )

    private val AZKAR_MEANING = mapOf(
        "أَسْتَغْفِرُ اللهَ." to mapOf(
            "en" to "I seek Allah's forgiveness.",
            "fr" to "Je demande pardon à Allah.",
            "es" to "Pido perdón a Allah.",
            "tr" to "Allah'tan bağışlanma dilerim.",
            "ur" to "میں اللہ سے بخشش مانگتا ہوں۔",
            "id" to "Aku memohon ampun kepada Allah.",
            "hi" to "मैं अल्लाह से क्षमा माँगता हूँ।",
        ),
        "سُبْحَانَ اللهِ." to mapOf(
            "en" to "Glory be to Allah.",
            "fr" to "Gloire à Allah.",
            "es" to "Gloria a Allah.",
            "tr" to "Allah her noksandan münezzehtir.",
            "ur" to "اللہ پاک ہے۔",
            "id" to "Mahasuci Allah.",
            "hi" to "अल्लाह पाक है।",
        ),
        "الْحَمْدُ لِلَّهِ." to mapOf(
            "en" to "All praise is for Allah.",
            "fr" to "Louange à Allah.",
            "es" to "Alabado sea Allah.",
            "tr" to "Hamd Allah'adır.",
            "ur" to "تمام تعریف اللہ کے لیے ہے۔",
            "id" to "Segala puji bagi Allah.",
            "hi" to "सारी स्तुति अल्लाह के लिए है।",
        ),
        "اللهُ أَكْبَرُ." to mapOf(
            "en" to "Allah is the Greatest.",
            "fr" to "Allah est le plus grand.",
            "es" to "Allah es el más grande.",
            "tr" to "Allah en büyüktür.",
            "ur" to "اللہ سب سے بڑا ہے۔",
            "id" to "Allah Mahabesar.",
            "hi" to "अल्लाह सबसे बड़ा है।",
        ),
        "اللَّهُمَّ أَنْتَ السَّلَامُ وَمِنْكَ السَّلَامُ تَبَارَكْتَ يَا ذَا الْجَلَالِ وَالْإِكْرَامِ." to mapOf(
            "en" to "O Allah, You are Peace and from You is peace. Blessed are You, O Owner of majesty and honor.",
            "fr" to "Ô Allah, Tu es la Paix et de Toi vient la paix. Béni sois-Tu, Ô Détenteur de la majesté et de la générosité.",
            "es" to "Oh Allah, Tú eres la Paz y de Ti viene la paz. Bendito eres, Oh Poseedor de la majestad y el honor.",
            "tr" to "Allahım! Sen Selam'sın, selam Sendendir. Celâl ve ikram sahibi olan Sen mübareksin.",
            "ur" to "اے اللہ! تو سلام ہے اور سلام تیری طرف سے ہے، تو بابرکت ہے اے جلال و اکرام والے!",
            "id" to "Ya Allah, Engkau adalah As-Salam dan dari-Mu-lah keselamatan. Mahasuci Engkau, wahai Pemilik keagungan dan kemuliaan.",
            "hi" to "ऐ अल्लाह, तू सलाम है और सलाम तेरी ओर से है। तू बरकत वाला है, ऐ महिमा और इज़्ज़त के स्वामी।",
        ),
        "الْحَمْدُ لِلَّهِ الَّذِي أَحْيَانَا بَعْدَ مَا أَمَاتَنَا وَإِلَيْهِ النُّشُورُ." to mapOf(
            "en" to "Praise be to Allah who gave us life after causing us to die, and to Him is the resurrection.",
            "fr" to "Louange à Allah qui nous a rendu la vie après nous avoir fait mourir, et vers Lui est la résurrection.",
            "es" to "Alabado sea Allah que nos dio vida después de hacernos morir, y a Él es la resurrección.",
            "tr" to "Bizi öldürdükten sonra dirilten Allah'a hamd olsun. Dönüş O'nadır.",
            "ur" to "اللہ کا شکر ہے جس نے ہمیں موت کے بعد زندگی دی، اور اسی کی طرف اٹھنا ہے۔",
            "id" to "Segala puji bagi Allah yang menghidupkan kami setelah mematikan kami, dan kepada-Nya-lah kebangkitan.",
            "hi" to "अल्लाह की स्तुति जिसने हमें मृत्यु के बाद जीवन दिया, और उसी की ओर उठना है।",
        ),
        "بِاسْمِكَ رَبِّي وَضَعْتُ جَنْبِي، وَبِكَ أَرْفَعُهُ، إِنْ أَمْسَكْتَ نَفْسِي فَارْحَمْهَا، وَإِنْ أَرْسَلْتَهَا فَاحْفَظْهَا بِمَا تَحْفَظُ بِهِ عِبَادَكَ الصَّالِحِينَ." to mapOf(
            "en" to "In Your name, my Lord, I lie down, and in Your name I rise. If You take my soul, have mercy on it; if You send it back, protect it as You protect Your righteous servants.",
            "fr" to "En Ton nom, Seigneur, je me couche et en Ton nom je me lève. Si Tu retiens mon âme, fais-lui miséricorde ; si Tu la renvoies, protège-la comme Tu protèges Tes serviteurs pieux.",
            "es" to "En Tu nombre, Señor, me acuesto y en Tu nombre me levanto. Si retienes mi alma, ten misericordia de ella; si la devuelves, protégela como proteges a Tus siervos justos.",
            "tr" to "Rabbim, Senin adınla yanımı koyarım ve Senin adınla kaldırırım. Ruhumu alırsan ona merhamet et; geri gönderirsen salih kullarını koruduğun gibi onu da koru.",
            "ur" to "اے میرے رب! تیرے نام سے میں پہلو رکھتا ہوں اور تیرے نام سے اٹھاتا ہوں۔ اگر تو میری جان روک لے تو اس پر رحم کر، اور اگر اسے واپس بھیجے تو اپنے نیک بندوں کی طرح اس کی حفاظت فرما۔",
            "id" to "Dengan nama-Mu, Tuhanku, aku berbaring, dan dengan nama-Mu aku bangun. Jika Engkau menahan jiwaku, rahmatilah ia; jika Engkau mengembalikannya, jagalah ia sebagaimana Engkau menjaga hamba-hamba-Mu yang saleh.",
            "hi" to "ऐ मेरे रब, तेरे नाम से मैं करवट रखता हूँ और तेरे नाम से उठाता हूँ। यदि तू मेरी जान रोक ले तो उस पर रहम कर, और यदि वापस करे तो अपने नेक बंदों की तरह उसकी हिफ़ाज़त कर।",
        ),
        "بِسْمِ اللهِ وَلَجْنَا، وَبِسْمِ اللهِ خَرَجْنَا، وَعَلَى رَبِّنَا تَوَكَّلْنَا." to mapOf(
            "en" to "In the name of Allah we enter, in the name of Allah we leave, and upon our Lord we rely.",
            "fr" to "Au nom d'Allah nous entrons, au nom d'Allah nous sortons, et c'est en notre Seigneur que nous plaçons notre confiance.",
            "es" to "En el nombre de Allah entramos, en el nombre de Allah salimos, y en nuestro Señor confiamos.",
            "tr" to "Allah'ın adıyla girdik, Allah'ın adıyla çıktık ve Rabbimize tevekkül ettik.",
            "ur" to "اللہ کے نام سے ہم داخل ہوئے، اللہ کے نام سے نکلے، اور اپنے رب پر بھروسہ کیا۔",
            "id" to "Dengan nama Allah kami masuk, dengan nama Allah kami keluar, dan kepada Tuhan kamilah kami bertawakal.",
            "hi" to "अल्लाह के नाम से हम दाख़िल हुए, अल्लाह के नाम से निकले, और अपने रब पर भरोसा किया।",
        ),
        "بِسْمِ اللهِ، تَوَكَّلْتُ عَلَى اللهِ، وَلَا حَوْلَ وَلَا قُوَّةَ إِلَّا بِاللهِ." to mapOf(
            "en" to "In the name of Allah, I rely upon Allah, and there is no power nor strength except with Allah.",
            "fr" to "Au nom d'Allah, je place ma confiance en Allah, et il n'y a de force ni de puissance qu'en Allah.",
            "es" to "En el nombre de Allah, confío en Allah, y no hay poder ni fuerza sino en Allah.",
            "tr" to "Allah'ın adıyla, Allah'a tevekkül ettim. Güç ve kuvvet ancak Allah iledir.",
            "ur" to "اللہ کے نام سے، میں نے اللہ پر بھروسہ کیا، اور کوئی طاقت و قوت نہیں مگر اللہ کے ساتھ۔",
            "id" to "Dengan nama Allah, aku bertawakal kepada Allah, dan tidak ada daya serta kekuatan kecuali dengan Allah.",
            "hi" to "अल्लाह के नाम से, मैंने अल्लाह पर भरोसा किया, और कोई शक्ति नहीं सिवा अल्लाह के।",
        ),
        "حَسْبِيَ اللهُ لَا إِلَهَ إِلَّا هُوَ ، عَلَيْهِ تَوَكَّلْتُ ، وَهُوَ رَبُّ الْعَرْشِ الْعَظِيْمِ." to mapOf(
            "en" to "Allah is sufficient for me. There is no god but He. Upon Him I rely, and He is the Lord of the Mighty Throne.",
            "fr" to "Allah me suffit. Nul dieu que Lui. C'est en Lui que je place ma confiance, et Il est le Seigneur du Trône immense.",
            "es" to "Allah me basta. No hay dios sino Él. En Él confío, y Él es el Señor del Trono inmenso.",
            "tr" to "Allah bana yeter. O'ndan başka ilah yoktur. O'na tevekkül ettim. O, yüce Arş'ın Rabbidir.",
            "ur" to "اللہ مجھے کافی ہے، اس کے سوا کوئی معبود نہیں، اسی پر میں نے بھروسہ کیا، اور وہ عرش عظیم کا رب ہے۔",
            "id" to "Cukuplah Allah bagiku. Tiada tuhan selain Dia. Kepada-Nya aku bertawakal, dan Dia adalah Tuhan Arsy yang agung.",
            "hi" to "अल्लाह मुझे काफ़ी है, उसके सिवा कोई माबूद नहीं, उसी पर मैंने भरोसा किया, और वह महान सिंहासन का रब है।",
        ),
        "رَضِيْتُ بِاللهِ رَبًّا ، وَبِالْإِسْلَامِ دِيْنًا ، وَبِمُحَمَّدٍ صَلَّى اللهُ عَلَيْهِ وَسَلَّمَ نَبِيًّا." to mapOf(
            "en" to "I am pleased with Allah as Lord, with Islam as religion, and with Muhammad ﷺ as Prophet.",
            "fr" to "Je me satisfais d'Allah comme Seigneur, de l'islam comme religion, et de Muhammad ﷺ comme prophète.",
            "es" to "Me complazco con Allah como Señor, con el islam como religión y con Muhammad ﷺ como profeta.",
            "tr" to "Allah'ı Rab, İslam'ı din, Muhammed'i ﷺ peygamber olarak razı oldum.",
            "ur" to "میں اللہ کو رب، اسلام کو دین، اور محمد ﷺ کو نبی مان کر راضی ہوا۔",
            "id" to "Aku ridha Allah sebagai Tuhan, Islam sebagai agama, dan Muhammad ﷺ sebagai nabi.",
            "hi" to "मैं अल्लाह को रब, इस्लाम को दीन, और मुहम्मद ﷺ को नबी मानकर राज़ी हुआ।",
        ),
        "سُبْحَانَ اللهِ وَبِحَمْدِهِ." to mapOf(
            "en" to "Glory and praise be to Allah.",
            "fr" to "Gloire et louange à Allah.",
            "es" to "Gloria y alabanza a Allah.",
            "tr" to "Allah'ı hamd ile tesbih ederim.",
            "ur" to "اللہ کی تسبیح اور اس کی حمد ہے۔",
            "id" to "Mahasuci Allah dengan memuji-Nya.",
            "hi" to "अल्लाह की पवित्रता और स्तुति है।",
        ),
        "أَعُوْذُ بِكَلِمَاتِ اللهِ التَّامَّاتِ مِنْ شَرِّ مَا خَلَقَ." to mapOf(
            "en" to "I seek refuge in the perfect words of Allah from the evil of what He created.",
            "fr" to "Je cherche protection dans les paroles parfaites d'Allah contre le mal de ce qu'Il a créé.",
            "es" to "Busco refugio en las palabras perfectas de Allah del mal de lo que Él creó.",
            "tr" to "Allah'ın eksiksiz kelimelerine, yarattıklarının şerrinden sığınırım.",
            "ur" to "میں اللہ کے کامل کلمات کی پناہ لیتا ہوں اس کی مخلوق کے شر سے۔",
            "id" to "Aku berlindung dengan kalimat-kalimat Allah yang sempurna dari kejahatan makhluk yang Dia ciptakan.",
            "hi" to "मैं अल्लाह के पूर्ण शब्दों की पनाह लेता हूँ उसकी सृष्टि के शर से।",
        ),
        "اللَّهُمَّ صَلِّ وَسَلِّمْ وَبَارِكْ على نَبِيِّنَا مُحمَّد." to mapOf(
            "en" to "O Allah, send blessings, peace, and grace upon our Prophet Muhammad.",
            "fr" to "Ô Allah, accorde la prière, la paix et la bénédiction à notre Prophète Muhammad.",
            "es" to "Oh Allah, envía bendiciones, paz y gracia a nuestro Profeta Muhammad.",
            "tr" to "Allahım! Peygamberimiz Muhammed'e salât, selam ve bereket ver.",
            "ur" to "اے اللہ! ہمارے نبی محمد پر درود و سلام اور برکت نازل فرما۔",
            "id" to "Ya Allah, limpahkan shalawat, salam, dan berkah kepada Nabi kami Muhammad.",
            "hi" to "ऐ अल्लाह, हमारे नबी मुहम्मद पर दुरूद, सलाम और बरकत भेज।",
        ),
        "أَسْتَغْفِرُ اللهَ وَأَتُوْبُ إِلَيْهِ." to mapOf(
            "en" to "I seek Allah's forgiveness and I repent to Him.",
            "fr" to "Je demande pardon à Allah et je me repens à Lui.",
            "es" to "Pido perdón a Allah y me arrepiento ante Él.",
            "tr" to "Allah'tan bağışlanma diler ve O'na tövbe ederim.",
            "ur" to "میں اللہ سے بخشش مانگتا ہوں اور اسی کی طرف رجوع کرتا ہوں۔",
            "id" to "Aku memohon ampun kepada Allah dan bertobat kepada-Nya.",
            "hi" to "मैं अल्लाह से क्षमा माँगता हूँ और उसी की ओर लौटता हूँ।",
        ),
        "يَا حَيُّ يَا قَيُّوْمُ ، بِرَحْمَتِكَ أَسْتَغِيْثُ ، أَصْلِحْ لِي شَأْنِيْ كُلَّهُ ، وَلَا تَكِلْنِيْ إِلَىٰ نَفْسِيْ طَرْفَةَ عَيْنٍ." to mapOf(
            "en" to "O Ever-Living, O Self-Sustaining, I seek help in Your mercy. Set right all my affairs and do not leave me to myself even for the blink of an eye.",
            "fr" to "Ô Vivant, Ô Subsistant, je cherche secours dans Ta miséricorde. Arrange toutes mes affaires et ne me confie pas à moi-même même le temps d'un clin d'œil.",
            "es" to "Oh Viviente, Oh Subsistente, busco ayuda en Tu misericordia. Arregla todos mis asuntos y no me dejes a mí mismo ni el parpadeo de un ojo.",
            "tr" to "Ey Hayy, ey Kayyûm! Rahmetinle yardım isterim. Bütün işlerimi düzelt ve göz açıp kapayana kadar beni nefsimle baş başa bırakma.",
            "ur" to "اے حی، اے قیوم! تیری رحمت سے فریاد کرتا ہوں، میرے تمام معاملات درست فرما، اور مجھے ایک پل کے لیے بھی میرے نفس کے حوالے نہ کر۔",
            "id" to "Wahai Yang Mahahidup, wahai Yang Maha Berdiri sendiri, dengan rahmat-Mu aku memohon pertolongan. Perbaikilah seluruh urusanku, dan jangan biarkan aku pada diriku meski sekejap mata.",
            "hi" to "ऐ हय्य, ऐ क़य्यूम, तेरी रहमत से फ़रियाद करता हूँ। मेरे सारे काम सुधार दे, और मुझे पलक झपकने जितनी देर भी मेरे नफ्स के हवाले न कर।",
        ),
    )

    private val VIRTUES = mapOf(
        "من قالها حين يصبح أجير من الجن حتى يمسى." to mapOf(
            "en" to "Whoever says it in the morning is protected from the jinn until evening.",
            "fr" to "Quiconque la dit le matin est protégé des djinns jusqu'au soir.",
            "es" to "Quien la dice por la mañana queda protegido de los yinn hasta la tarde.",
            "tr" to "Sabah söyleyen, akşama kadar cinlerden korunur.",
            "ur" to "جو اسے صبح پڑھے، شام تک جنات سے محفوظ رہتا ہے۔",
            "id" to "Siapa yang mengucapkannya di pagi hari dilindungi dari jin hingga petang.",
            "hi" to "जो इसे सुबह पढ़े, शाम तक जिन्नों से सुरक्षित रहता है।",
        ),
        "من قالها حين يصبح أجير من الجن حتى يمسى ومن قالها حين يمسى أجير من الجن حتى يصبح." to mapOf(
            "en" to "Whoever says it in the morning is protected from the jinn until evening, and whoever says it in the evening is protected until morning.",
            "fr" to "Quiconque la dit le matin est protégé des djinns jusqu'au soir, et quiconque la dit le soir est protégé jusqu'au matin.",
            "es" to "Quien la dice por la mañana queda protegido de los yinn hasta la tarde, y quien la dice por la tarde queda protegido hasta la mañana.",
            "tr" to "Sabah söyleyen akşama, akşam söyleyen sabaha kadar cinlerden korunur.",
            "ur" to "جو اسے صبح پڑھے شام تک، اور جو شام پڑھے صبح تک جنات سے محفوظ رہتا ہے۔",
            "id" to "Siapa yang mengucapkannya pagi dilindungi hingga petang, dan siapa yang mengucapkannya petang dilindungi hingga pagi.",
            "hi" to "जो सुबह पढ़े शाम तक, और जो शाम पढ़े सुबह तक जिन्नों से सुरक्षित रहता है।",
        ),
        "من قالها حين يصبح وحين يمسى كفته من كل شيء." to mapOf(
            "en" to "Whoever says it in the morning and evening, it will suffice him against everything.",
            "fr" to "Quiconque la dit le matin et le soir, elle lui suffit contre toute chose.",
            "es" to "Quien la dice por la mañana y por la tarde, le basta contra todo.",
            "tr" to "Sabah ve akşam söyleyene her şeye karşı yeter.",
            "ur" to "جو اسے صبح و شام پڑھے، وہ اسے ہر چیز سے کفایت کرتی ہے۔",
            "id" to "Siapa yang mengucapkannya pagi dan petang, itu mencukupinya dari segala sesuatu.",
            "hi" to "जो इसे सुबह-शाम पढ़े, वह उसे हर चीज़ से काफ़ी होती है।",
        ),
        "سيد الاستغفار؛ من قاله موقناً ومات دخل الجنة." to mapOf(
            "en" to "The master of seeking forgiveness; whoever says it with certainty and dies will enter Paradise.",
            "fr" to "Le maître de la demande de pardon ; quiconque la dit avec certitude et meurt entre au Paradis.",
            "es" to "El señor del perdón; quien la dice con certeza y muere entra al Paraíso.",
            "tr" to "İstiğfarın efendisi; kesin inanarak söyleyip ölen cennete girer.",
            "ur" to "سید الاستغفار؛ جو اسے یقین کے ساتھ کہے اور مر جائے وہ جنت میں داخل ہوگا۔",
            "id" to "Penghulu istighfar; siapa yang mengucapkannya dengan yakin lalu meninggal, masuk surga.",
            "hi" to "इस्तिग़फ़ार का सरदार; जो इसे यक़ीन के साथ कहे और मर जाए वह जन्नत में जाएगा।",
        ),
        "العتق من النار." to mapOf(
            "en" to "Freedom from the Fire.",
            "fr" to "Affranchissement du Feu.",
            "es" to "Liberación del Fuego.",
            "tr" to "Cehennemden azat.",
            "ur" to "آگ سے آزادی۔",
            "id" to "Pembebasan dari neraka.",
            "hi" to "आग से आज़ादी।",
        ),
        "حماية تامة؛ لا يضره شيء." to mapOf(
            "en" to "Complete protection; nothing will harm him.",
            "fr" to "Protection complète ; rien ne lui nuira.",
            "es" to "Protección completa; nada le dañará.",
            "tr" to "Tam koruma; ona hiçbir şey zarar vermez.",
            "ur" to "مکمل حفاظت؛ اسے کچھ نقصان نہیں پہنچے گا۔",
            "id" to "Perlindungan sempurna; tidak ada yang membahayakannya.",
            "hi" to "पूर्ण सुरक्षा; उसे कुछ हानि नहीं होगी।",
        ),
        "حق على الله أن يرضيه يوم القيامة." to mapOf(
            "en" to "It is a right upon Allah that He will please him on the Day of Resurrection.",
            "fr" to "Il appartient à Allah de l'agréer le Jour de la Résurrection.",
            "es" to "Es un derecho sobre Allah complacerlo el Día de la Resurrección.",
            "tr" to "Kıyamet günü Allah'ın onu razı etmesi haktır.",
            "ur" to "قیامت کے دن اللہ کا حق ہے کہ اسے راضی کرے۔",
            "id" to "Hak atas Allah untuk meridainya pada hari kiamat.",
            "hi" to "क़ियामत के दिन अल्लाह का हक़ है कि वह उसे राज़ी करे।",
        ),
        "حط الخطايا وإن كانت كزبد البحر." to mapOf(
            "en" to "It wipes away sins even if they are like the foam of the sea.",
            "fr" to "Elle efface les péchés même s'ils sont comme l'écume de la mer.",
            "es" to "Borra los pecados aunque sean como la espuma del mar.",
            "tr" to "Denizin köpüğü kadar da olsa günahları siler.",
            "ur" to "گناہ مٹا دیتی ہے چاہے سمندر کی جھاگ کے برابر ہوں۔",
            "id" to "Menghapus dosa meski sebanyak buih laut.",
            "hi" to "गुनाह मिटा देती है चाहे समुद्र की झाग जितने हों।",
        ),
        "حرز من الشيطان، ومغفرة، وأجر عتق رقاب." to mapOf(
            "en" to "A shield from Satan, forgiveness, and the reward of freeing slaves.",
            "fr" to "Une protection contre Satan, un pardon, et la récompense de l'affranchissement d'esclaves.",
            "es" to "Escudo contra Satanás, perdón y recompensa de liberar esclavos.",
            "tr" to "Şeytandan koruma, mağfiret ve köle azat etme sevabı.",
            "ur" to "شیطان سے بچاؤ، مغفرت، اور غلام آزاد کرنے کا اجر۔",
            "id" to "Perisai dari setan, ampunan, dan pahala memerdekakan budak.",
            "hi" to "शैतान से बचाव, माफ़ी, और ग़ुलाम आज़ाद करने का अज्र।",
        ),
        "إدراك شفاعة النبي ﷺ يوم القيامة." to mapOf(
            "en" to "Attaining the Prophet's ﷺ intercession on the Day of Resurrection.",
            "fr" to "Obtenir l'intercession du Prophète ﷺ le Jour de la Résurrection.",
            "es" to "Alcanzar la intercesión del Profeta ﷺ el Día de la Resurrección.",
            "tr" to "Kıyamet günü Peygamber'in ﷺ şefaatine nail olmak.",
            "ur" to "قیامت کے دن نبی ﷺ کی شفاعت پانا۔",
            "id" to "Meraih syafaat Nabi ﷺ pada hari kiamat.",
            "hi" to "क़ियामत के दिन नबी ﷺ की सिफ़ारिश पाना।",
        ),
        "غفران الذنوب ولو كانت من الكبائر." to mapOf(
            "en" to "Forgiveness of sins even if they are among the major ones.",
            "fr" to "Pardon des péchés même s'ils font partie des grands péchés.",
            "es" to "Perdón de los pecados aunque sean de los mayores.",
            "tr" to "Büyük günahlardan olsa bile günahların bağışlanması.",
            "ur" to "گناہوں کی مغفرت چاہے وہ کبیرہ ہوں۔",
            "id" to "Pengampunan dosa meski termasuk dosa besar.",
            "hi" to "गुनाहों की माफ़ी चाहे वे कबीरा हों।",
        ),
        "كفاية هموم الدنيا والآخرة." to mapOf(
            "en" to "Sufficiency against the worries of this world and the Hereafter.",
            "fr" to "Suffisance contre les soucis de ce monde et de l'au-delà.",
            "es" to "Suficiencia contra las preocupaciones de este mundo y del más allá.",
            "tr" to "Dünya ve ahiret kaygılarından yeterlilik.",
            "ur" to "دنیا و آخرت کی فکر سے کفایت۔",
            "id" to "Kecukupan dari kesusahan dunia dan akhirat.",
            "hi" to "दुनिया और आख़िरत की चिंताओं से काफ़ियत।",
        ),
        "عند الدخول والخروج." to mapOf(
            "en" to "Upon entering and leaving.",
            "fr" to "En entrant et en sortant.",
            "es" to "Al entrar y al salir.",
            "tr" to "Girerken ve çıkarken.",
            "ur" to "داخل ہوتے اور نکلتے وقت۔",
            "id" to "Saat masuk dan keluar.",
            "hi" to "दाख़िल होते और निकलते समय।",
        ),
        "عند الدخول." to mapOf(
            "en" to "Upon entering.",
            "fr" to "En entrant.",
            "es" to "Al entrar.",
            "tr" to "Girerken.",
            "ur" to "داخل ہوتے وقت۔",
            "id" to "Saat masuk.",
            "hi" to "दाख़िल होते समय।",
        ),
        "عند الخروج." to mapOf(
            "en" to "Upon leaving.",
            "fr" to "En sortant.",
            "es" to "Al salir.",
            "tr" to "Çıkarken.",
            "ur" to "نکلتے وقت۔",
            "id" to "Saat keluar.",
            "hi" to "निकलते समय।",
        ),
        "من قرأ آية الكرسي دبر كل صلاة مكتوبة لم يمنعه من دخول الجنة إلا الموت." to mapOf(
            "en" to "Whoever recites Ayat al-Kursi after every obligatory prayer, nothing will keep him from Paradise except death.",
            "fr" to "Quiconque récite le Verset du Trône après chaque prière obligatoire, rien ne l'empêchera d'entrer au Paradis sinon la mort.",
            "es" to "Quien recita el versículo del Trono tras cada oración obligatoria, nada le impedirá el Paraíso salvo la muerte.",
            "tr" to "Her farz namazın ardından Ayetel Kürsi okuyanı cennete girmekten ancak ölüm alıkoyar.",
            "ur" to "جو ہر فرض نماز کے بعد آیت الکرسی پڑھے، اسے جنت میں داخل ہونے سے صرف موت روکے گی۔",
            "id" to "Siapa yang membaca Ayat Kursi setelah setiap shalat wajib, tidak ada yang menghalanginya masuk surga kecuali kematian.",
            "hi" to "जो हर फ़र्ज़ नमाज़ के बाद आयतुल कुर्सी पढ़े, उसे जन्नत में जाने से केवल मृत्यु रोकेगी।",
        ),
    )
}

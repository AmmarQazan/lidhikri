const LANGS = {
  ar: { dir: "rtl", name: "العربية" },
  en: { dir: "ltr", name: "English" },
  fr: { dir: "ltr", name: "Français" },
  es: { dir: "ltr", name: "Español" },
};

const I18N = {
  home: {
    ar: {
      title: "لذكري — Lidhikri",
      h1: "لذكري",
      tag: "Lidhikri",
      aboutTitle: "من نحن",
      about:
        "لذكري يطوّر تطبيقات أندرويد للذكر: تسبيح وأذكار تلقائية أثناء استخدام الهاتف، مع أذان ومواقيت صلاة وأذكار ما بعد الفرض، مسبحة رقمية، وويدجت. بلا حساب، بلا إعلانات، وبلا تتبع.",
      contactTitle: "التواصل",
      contactLead: "البريد العام للمستخدمين والدعم:",
      privacy: "سياسة الخصوصية",
    },
    en: {
      title: "Lidhikri",
      h1: "Lidhikri",
      tag: "لذكري",
      aboutTitle: "About",
      about:
        "Lidhikri builds Android apps for dhikr: automatic tasbih and adhkar while you use your phone, plus adhan, prayer times, after-prayer adhkar, a digital misbaha, and home-screen widgets. No account, no ads, and no tracking.",
      contactTitle: "Contact",
      contactLead: "Public email for users and support:",
      privacy: "Privacy policy",
    },
    fr: {
      title: "Lidhikri",
      h1: "Lidhikri",
      tag: "لذكري",
      aboutTitle: "À propos",
      about:
        "Lidhikri crée des applications Android pour le dhikr : tasbih et adhkar automatiques pendant l’usage du téléphone, plus adhan, horaires de prière, adhkar après la prière, misbaha numérique et widgets d’accueil. Pas de compte, pas de publicité, pas de suivi.",
      contactTitle: "Contact",
      contactLead: "E-mail public pour les utilisateurs et le support :",
      privacy: "Politique de confidentialité",
    },
    es: {
      title: "Lidhikri",
      h1: "Lidhikri",
      tag: "لذكري",
      aboutTitle: "Quiénes somos",
      about:
        "Lidhikri crea aplicaciones Android para el dhikr: tasbih y adhkar automáticos mientras usas el teléfono, más adhan, horarios de oración, adhkar después de la oración, misbaha digital y widgets de inicio. Sin cuenta, sin anuncios y sin rastreo.",
      contactTitle: "Contacto",
      contactLead: "Correo público para usuarios y soporte:",
      privacy: "Política de privacidad",
    },
  },
  privacy: {
    ar: {
      title: "سياسة الخصوصية — لذكري",
      h1: "سياسة الخصوصية — لذكري",
      lead: "الإعدادات والأذكار والتسجيلات تبقى على جهازك. لا إعلانات ولا تتبع.",
      col1: "البيان",
      col2: "التفاصيل",
      rows: [
        ["التخزين", "كل الإعدادات والأذكار المخصصة والتسجيلات الصوتية تُحفظ محلياً على جهازك."],
        ["الحساب", "لا تسجيل دخول للمستخدم. مزامنة المحتوى قد تستخدم معرّفاً مجهولاً لدى فايربيس لتحميل الحزمة فقط."],
        ["الموقع", "مواقيت الصلاة والقبلة تُحسب على الجهاز. اختيار المدينة أو المنزل قد يستخدم خدمة العناوين في أندرويد لحظياً. الموقع اختياري."],
        ["التتبع", "لا إعلانات، لا تحليلات، لا تتبع."],
        ["الإنترنت", "يُستخدم لتحميل ملفات الصوت عند الطلب، ولمزامنة حزمة المحتوى إن وُجدت."],
        ["النسخ الاحتياطي", "قد يشمل Android Backup إعدادات التطبيق حسب إعدادات جهازك."],
      ],
      updated: "آخر تحديث: 2 أيلول 2026",
      home: "الصفحة الرئيسية",
    },
    en: {
      title: "Privacy policy — Lidhikri",
      h1: "Privacy policy — Lidhikri",
      lead: "Settings, dhikr, and recordings stay on your device. No ads and no tracking.",
      col1: "Item",
      col2: "Details",
      rows: [
        ["Storage", "Settings, custom dhikr, and recordings stay on your device."],
        ["Account", "No user sign-in. Content sync may use an anonymous Firebase ID only to download the catalog."],
        ["Location", "Prayer times and qibla are calculated on device. City or home lookup may use Android Geocoder briefly. Location is optional."],
        ["Tracking", "No ads, no analytics, no tracking."],
        ["Internet", "Used to download audio on demand and to sync the content pack if available."],
        ["Backup", "Android Backup may include app settings according to your device settings."],
      ],
      updated: "Last updated: 2 Sep 2026",
      home: "Home",
    },
    fr: {
      title: "Politique de confidentialité — Lidhikri",
      h1: "Politique de confidentialité — Lidhikri",
      lead: "Réglages, adhkar et enregistrements restent sur l’appareil. Pas de publicité ni de suivi.",
      col1: "Élément",
      col2: "Détails",
      rows: [
        ["Stockage", "Réglages, adhkar personnalisés et enregistrements restent sur l’appareil."],
        ["Compte", "Pas de connexion utilisateur. La synchro du contenu peut utiliser un identifiant Firebase anonyme uniquement pour télécharger le catalogue."],
        ["Localisation", "Horaires de prière et qibla sont calculés sur l’appareil. La recherche de ville peut utiliser le géocodeur Android brièvement. La localisation est facultative."],
        ["Suivi", "Pas de publicité, pas d’analyse, pas de suivi."],
        ["Internet", "Utilisé pour télécharger l’audio à la demande et synchroniser le pack de contenu s’il est disponible."],
        ["Sauvegarde", "La sauvegarde Android peut inclure les réglages de l’app selon l’appareil."],
      ],
      updated: "Dernière mise à jour : 2 septembre 2026",
      home: "Accueil",
    },
    es: {
      title: "Política de privacidad — Lidhikri",
      h1: "Política de privacidad — Lidhikri",
      lead: "Ajustes, adhkar y grabaciones permanecen en el dispositivo. Sin anuncios ni rastreo.",
      col1: "Dato",
      col2: "Detalles",
      rows: [
        ["Almacenamiento", "Ajustes, adhkar personalizados y grabaciones permanecen en el dispositivo."],
        ["Cuenta", "No hay inicio de sesión. La sincronización puede usar un ID anónimo de Firebase solo para descargar el catálogo."],
        ["Ubicación", "Horarios de oración y qibla se calculan en el dispositivo. Buscar ciudad puede usar el geocodificador de Android brevemente. La ubicación es opcional."],
        ["Rastreo", "Sin anuncios, sin analítica y sin rastreo."],
        ["Internet", "Se usa para descargar audio bajo demanda y sincronizar el paquete de contenido si existe."],
        ["Copia de seguridad", "La copia de Android puede incluir ajustes de la app según el dispositivo."],
      ],
      updated: "Última actualización: 2 sep 2026",
      home: "Inicio",
    },
  },
};

function detectLang() {
  const q = new URLSearchParams(location.search).get("lang");
  if (q && LANGS[q]) return q;
  const saved = localStorage.getItem("lidhikri-lang");
  if (saved && LANGS[saved]) return saved;
  const nav = (navigator.language || "ar").slice(0, 2).toLowerCase();
  return LANGS[nav] ? nav : "ar";
}

function setLang(lang) {
  localStorage.setItem("lidhikri-lang", lang);
  const url = new URL(location.href);
  url.searchParams.set("lang", lang);
  history.replaceState(null, "", url);
  apply(lang);
}

function apply(lang) {
  const meta = LANGS[lang];
  document.documentElement.lang = lang;
  document.documentElement.dir = meta.dir;
  document.querySelectorAll("[data-lang-btn]").forEach((btn) => {
    btn.setAttribute("aria-current", btn.dataset.langBtn === lang ? "true" : "false");
  });
  const page = document.body.dataset.page || "home";
  const t = I18N[page][lang];
  document.title = t.title;
  document.querySelectorAll("[data-i18n]").forEach((el) => {
    const key = el.dataset.i18n;
    if (t[key]) el.textContent = t[key];
  });
  const tbody = document.querySelector("[data-rows]");
  if (tbody && t.rows) {
    tbody.innerHTML = t.rows
      .map(([a, b]) => `<tr><td>${a}</td><td>${b}</td></tr>`)
      .join("");
  }
  const col1 = document.querySelector("[data-col1]");
  const col2 = document.querySelector("[data-col2]");
  if (col1) col1.textContent = t.col1;
  if (col2) col2.textContent = t.col2;
  document.querySelectorAll('a[href*="privacy"]').forEach((a) => {
    a.href = `privacy.html?lang=${lang}`;
  });
  document.querySelectorAll('a[href*="index"]').forEach((a) => {
    a.href = `index.html?lang=${lang}`;
  });
}

function mountLangNav(el) {
  el.innerHTML = Object.entries(LANGS)
    .map(
      ([code, meta]) =>
        `<button type="button" data-lang-btn="${code}">${meta.name}</button>`
    )
    .join("");
  el.addEventListener("click", (e) => {
    const btn = e.target.closest("[data-lang-btn]");
    if (btn) setLang(btn.dataset.langBtn);
  });
}

document.addEventListener("DOMContentLoaded", () => {
  mountLangNav(document.querySelector("nav.langs"));
  apply(detectLang());
});

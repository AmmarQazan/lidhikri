const LANGS = {
  ar: { dir: "rtl", name: "العربية" },
  en: { dir: "ltr", name: "English" },
  fr: { dir: "ltr", name: "Français" },
  es: { dir: "ltr", name: "Español" },
};

const I18N = {
  home: {
    ar: {
      title: "سَبّح — Sabbih",
      h1: "سَبّح",
      tag: "Sabbih",
      aboutTitle: "من نحن",
      about:
        "سَبّح يطوّر تطبيقات أندرويد للذكر والتسبيح: تسبيح تلقائي أثناء استخدام الهاتف، أذكار حصن المسلم، مسبحة رقمية، وويدجت على الشاشة الرئيسية. بلا حساب، بلا إعلانات، وبلا تتبع.",
      contactTitle: "التواصل",
      contactLead: "البريد العام للمستخدمين والدعم:",
      privacy: "سياسة الخصوصية",
    },
    en: {
      title: "Sabbih",
      h1: "Sabbih",
      tag: "سَبّح",
      aboutTitle: "About",
      about:
        "Sabbih builds Android apps for dhikr and tasbih: automatic reminders while you use your phone, Hisnul Muslim collections, a digital misbaha, and home-screen widgets. No account, no ads, and no tracking.",
      contactTitle: "Contact",
      contactLead: "Public email for users and support:",
      privacy: "Privacy policy",
    },
    fr: {
      title: "Sabbih",
      h1: "Sabbih",
      tag: "سَبّح",
      aboutTitle: "À propos",
      about:
        "Sabbih crée des applications Android pour le dhikr et le tasbih : rappels automatiques pendant l’usage du téléphone, collections Hisnul Muslim, misbaha numérique et widgets d’accueil. Pas de compte, pas de publicité, pas de suivi.",
      contactTitle: "Contact",
      contactLead: "E-mail public pour les utilisateurs et le support :",
      privacy: "Politique de confidentialité",
    },
    es: {
      title: "Sabbih",
      h1: "Sabbih",
      tag: "سَبّح",
      aboutTitle: "Quiénes somos",
      about:
        "Sabbih crea aplicaciones Android para el dhikr y el tasbih: recordatorios automáticos mientras usas el teléfono, colecciones de Hisnul Muslim, misbaha digital y widgets de inicio. Sin cuenta, sin anuncios y sin rastreo.",
      contactTitle: "Contacto",
      contactLead: "Correo público para usuarios y soporte:",
      privacy: "Política de privacidad",
    },
  },
  privacy: {
    ar: {
      title: "سياسة الخصوصية — سَبّح",
      h1: "سياسة الخصوصية — سَبّح",
      lead: "سَبّح لا يجمع بيانات شخصية ولا يرسلها لأي خادم.",
      col1: "البيان",
      col2: "التفاصيل",
      rows: [
        ["التخزين", "كل الإعدادات والأذكار المخصصة والتسجيلات الصوتية تُحفظ محلياً على جهازك."],
        ["الحساب", "لا يوجد تسجيل دخول أو حساب مستخدم."],
        ["التتبع", "لا إعلانات، لا تحليلات، لا تتبع."],
        ["الإنترنت", "يُستخدم فقط لتحميل ملفات الصوت عند الطلب (اختياري)، ولمزامنة حزمة المحتوى إن وُجدت."],
        ["النسخ الاحتياطي", "قد يشمل Android Backup إعدادات التطبيق حسب إعدادات جهازك."],
      ],
      updated: "آخر تحديث: 27 آب 2026",
      home: "الصفحة الرئيسية",
    },
    en: {
      title: "Privacy policy — Sabbih",
      h1: "Privacy policy — Sabbih",
      lead: "Sabbih does not collect personal data or send it to any server.",
      col1: "Item",
      col2: "Details",
      rows: [
        ["Storage", "Settings, custom dhikr, and recordings stay on your device."],
        ["Account", "No sign-in and no user account."],
        ["Tracking", "No ads, no analytics, no tracking."],
        ["Internet", "Used only to download audio on demand (optional), and to sync the content pack if available."],
        ["Backup", "Android Backup may include app settings according to your device settings."],
      ],
      updated: "Last updated: 27 Aug 2026",
      home: "Home",
    },
    fr: {
      title: "Politique de confidentialité — Sabbih",
      h1: "Politique de confidentialité — Sabbih",
      lead: "Sabbih ne collecte pas de données personnelles et n’en envoie à aucun serveur.",
      col1: "Élément",
      col2: "Détails",
      rows: [
        ["Stockage", "Réglages, adhkar personnalisés et enregistrements restent sur l’appareil."],
        ["Compte", "Aucune connexion et aucun compte utilisateur."],
        ["Suivi", "Pas de publicité, pas d’analyse, pas de suivi."],
        ["Internet", "Utilisé uniquement pour télécharger l’audio à la demande (optionnel) et synchroniser le pack de contenu s’il est disponible."],
        ["Sauvegarde", "La sauvegarde Android peut inclure les réglages de l’app selon l’appareil."],
      ],
      updated: "Dernière mise à jour : 27 août 2026",
      home: "Accueil",
    },
    es: {
      title: "Política de privacidad — Sabbih",
      h1: "Política de privacidad — Sabbih",
      lead: "Sabbih no recopila datos personales ni los envía a ningún servidor.",
      col1: "Dato",
      col2: "Detalles",
      rows: [
        ["Almacenamiento", "Ajustes, adhkar personalizados y grabaciones permanecen en el dispositivo."],
        ["Cuenta", "No hay inicio de sesión ni cuenta de usuario."],
        ["Rastreo", "Sin anuncios, sin analítica y sin rastreo."],
        ["Internet", "Solo se usa para descargar audio bajo demanda (opcional) y sincronizar el paquete de contenido si existe."],
        ["Copia de seguridad", "La copia de Android puede incluir ajustes de la app según el dispositivo."],
      ],
      updated: "Última actualización: 27 ago 2026",
      home: "Inicio",
    },
  },
};

function detectLang() {
  const q = new URLSearchParams(location.search).get("lang");
  if (q && LANGS[q]) return q;
  const saved = localStorage.getItem("sabbih-lang");
  if (saved && LANGS[saved]) return saved;
  const nav = (navigator.language || "ar").slice(0, 2).toLowerCase();
  return LANGS[nav] ? nav : "ar";
}

function setLang(lang) {
  localStorage.setItem("sabbih-lang", lang);
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

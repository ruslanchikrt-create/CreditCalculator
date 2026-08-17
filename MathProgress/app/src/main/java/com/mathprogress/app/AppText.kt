package com.mathprogress.app

object AppText {
    fun languageName(code: String): String = when(code){
        "en" -> "English"; "tr" -> "Türkçe"; "es" -> "Español"; else -> "Русский"
    }

    fun t(code: String, key: String): String {
        val map = when(code){
            "en" -> en
            "tr" -> tr
            "es" -> es
            else -> ru
        }
        return map[key] ?: ru[key] ?: key
    }

    private val ru = mapOf(
        "choose_language" to "Выберите язык",
        "choose_language_sub" to "Язык можно изменить позже в настройках",
        "important" to "Важно",
        "disclaimer_title" to "Перед началом",
        "disclaimer_body" to "«Математика — Прогресс» предназначено для самостоятельной практики, решения математических задач и отслеживания личного прогресса. Оценки, проценты, уровни и результаты внутри приложения являются внутренними показателями для обучения и мотивации. Они не являются официальной проверкой знаний, школьной или экзаменационной оценкой и не заменяют оценку учителя, преподавателя или образовательной организации.",
        "acknowledge" to "Я ознакомился",
        "app_title" to "Математика — Прогресс",
        "results" to "Результаты",
        "solve" to "Решить задачу",
        "practice" to "Проверка знаний",
        "daily" to "Ежедневная тренировка",
        "history" to "История",
        "mistakes" to "Мои ошибки",
        "trash" to "Корзина",
        "settings" to "Настройки",
        "guide" to "Инструкция",
        "about" to "О приложении",
        "exit" to "Выход",
        "profile_sub" to "Профиль и смена пользователя",
        "language" to "Язык",
        "security" to "Безопасность",
        "backup" to "Резервная копия",
        "light" to "Светлая",
        "dark" to "Тёмная",
        "difficulty" to "Сложность",
        "easy" to "Лёгкий",
        "medium" to "Средний",
        "hard" to "Сложный",
        "expert" to "Эксперт",
        "adaptive" to "Адаптивный",
        "count" to "Количество",
        "topic" to "Тема",
        "start_test" to "Начать проверку",
        "your_answer" to "Ваш ответ",
        "correct_answer" to "Правильный ответ",
        "detailed_solution" to "Полное решение",
        "correct" to "Верно",
        "incorrect" to "Неверно",
        "next" to "Дальше",
        "check" to "Проверить",
        "feedback_error" to "Сообщить об ошибке",
        "feedback_idea" to "Предложить улучшение",
        "attach_media" to "Прикрепить фото или видео",
        "send_email" to "Отправить на почту",
        "attachments" to "Вложения",
        "no_attachments" to "Нет вложений",
        "disclaimer_short" to "Оценка приложения — внутренний показатель прогресса. Она не является школьной оценкой."
    )

    private val en = mapOf(
        "choose_language" to "Choose language",
        "choose_language_sub" to "You can change the language later in Settings",
        "important" to "Important",
        "disclaimer_title" to "Before you start",
        "disclaimer_body" to "MathProgress is intended for self-practice, solving math problems, and tracking personal progress. Grades, percentages, levels, and results inside the app are internal learning and motivation indicators. They are not an official knowledge assessment, school or exam grade, and do not replace a teacher's or educational institution's assessment.",
        "acknowledge" to "I understand",
        "app_title" to "MathProgress",
        "results" to "Results", "solve" to "Solve a problem", "practice" to "Knowledge practice", "daily" to "Daily practice",
        "history" to "History", "mistakes" to "My mistakes", "trash" to "Trash", "settings" to "Settings", "guide" to "Guide", "about" to "About", "exit" to "Exit",
        "profile_sub" to "Profile and switch user", "language" to "Language", "security" to "Security", "backup" to "Backup", "light" to "Light", "dark" to "Dark",
        "difficulty" to "Difficulty", "easy" to "Easy", "medium" to "Medium", "hard" to "Hard", "expert" to "Expert", "adaptive" to "Adaptive",
        "count" to "Count", "topic" to "Topic", "start_test" to "Start practice", "your_answer" to "Your answer", "correct_answer" to "Correct answer", "detailed_solution" to "Detailed solution",
        "correct" to "Correct", "incorrect" to "Incorrect", "next" to "Next", "check" to "Check", "feedback_error" to "Report an error", "feedback_idea" to "Suggest an improvement",
        "attach_media" to "Attach photo or video", "send_email" to "Send by email", "attachments" to "Attachments", "no_attachments" to "No attachments",
        "disclaimer_short" to "The app grade is an internal progress indicator and is not a school grade."
    )

    private val tr = mapOf(
        "choose_language" to "Dil seçin",
        "choose_language_sub" to "Dili daha sonra Ayarlar'dan değiştirebilirsiniz",
        "important" to "Önemli",
        "disclaimer_title" to "Başlamadan önce",
        "disclaimer_body" to "MathProgress bireysel pratik, matematik problemleri çözme ve kişisel ilerlemeyi takip etme amacıyla hazırlanmıştır. Uygulamadaki notlar, yüzdeler, seviyeler ve sonuçlar öğrenme ve motivasyon için dahili göstergelerdir. Resmî bir bilgi ölçümü, okul veya sınav notu değildir ve öğretmen ya da eğitim kurumu değerlendirmesinin yerine geçmez.",
        "acknowledge" to "Okudum ve anladım",
        "app_title" to "MathProgress",
        "results" to "Sonuçlar", "solve" to "Soru çöz", "practice" to "Bilgi alıştırması", "daily" to "Günlük alıştırma",
        "history" to "Geçmiş", "mistakes" to "Hatalarım", "trash" to "Çöp kutusu", "settings" to "Ayarlar", "guide" to "Kılavuz", "about" to "Uygulama hakkında", "exit" to "Çıkış",
        "profile_sub" to "Profil ve kullanıcı değiştir", "language" to "Dil", "security" to "Güvenlik", "backup" to "Yedekleme", "light" to "Açık", "dark" to "Koyu",
        "difficulty" to "Zorluk", "easy" to "Kolay", "medium" to "Orta", "hard" to "Zor", "expert" to "Uzman", "adaptive" to "Uyarlanabilir",
        "count" to "Sayı", "topic" to "Konu", "start_test" to "Alıştırmayı başlat", "your_answer" to "Cevabınız", "correct_answer" to "Doğru cevap", "detailed_solution" to "Ayrıntılı çözüm",
        "correct" to "Doğru", "incorrect" to "Yanlış", "next" to "İleri", "check" to "Kontrol et", "feedback_error" to "Hata bildir", "feedback_idea" to "İyileştirme öner",
        "attach_media" to "Fotoğraf veya video ekle", "send_email" to "E-posta ile gönder", "attachments" to "Ekler", "no_attachments" to "Ek yok",
        "disclaimer_short" to "Uygulama notu kişisel ilerleme göstergesidir; okul notu değildir."
    )

    private val es = mapOf(
        "choose_language" to "Elige el idioma",
        "choose_language_sub" to "Puedes cambiar el idioma más tarde en Ajustes",
        "important" to "Importante",
        "disclaimer_title" to "Antes de empezar",
        "disclaimer_body" to "MathProgress está pensado para la práctica personal, la resolución de problemas matemáticos y el seguimiento del progreso. Las notas, porcentajes, niveles y resultados de la aplicación son indicadores internos de aprendizaje y motivación. No constituyen una evaluación oficial de conocimientos, una nota escolar o de examen, y no sustituyen la evaluación de un profesor o centro educativo.",
        "acknowledge" to "He leído y entendido",
        "app_title" to "MathProgress",
        "results" to "Resultados", "solve" to "Resolver problema", "practice" to "Práctica de conocimientos", "daily" to "Práctica diaria",
        "history" to "Historial", "mistakes" to "Mis errores", "trash" to "Papelera", "settings" to "Ajustes", "guide" to "Guía", "about" to "Acerca de", "exit" to "Salir",
        "profile_sub" to "Perfil y cambiar usuario", "language" to "Idioma", "security" to "Seguridad", "backup" to "Copia de seguridad", "light" to "Claro", "dark" to "Oscuro",
        "difficulty" to "Dificultad", "easy" to "Fácil", "medium" to "Medio", "hard" to "Difícil", "expert" to "Experto", "adaptive" to "Adaptativo",
        "count" to "Cantidad", "topic" to "Tema", "start_test" to "Iniciar práctica", "your_answer" to "Tu respuesta", "correct_answer" to "Respuesta correcta", "detailed_solution" to "Solución detallada",
        "correct" to "Correcto", "incorrect" to "Incorrecto", "next" to "Siguiente", "check" to "Comprobar", "feedback_error" to "Informar de un error", "feedback_idea" to "Sugerir una mejora",
        "attach_media" to "Adjuntar foto o vídeo", "send_email" to "Enviar por correo", "attachments" to "Adjuntos", "no_attachments" to "Sin adjuntos",
        "disclaimer_short" to "La nota de la aplicación es un indicador interno de progreso y no es una nota escolar."
    )
}

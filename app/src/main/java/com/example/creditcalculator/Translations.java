package com.example.creditcalculator;

import java.util.HashMap;
import java.util.Map;

/** Lightweight translation catalog for Turkish and Spanish. English remains the fallback. */
public final class Translations {
    private static final Map<String, String> TR = new HashMap<>();
    private static final Map<String, String> ES = new HashMap<>();

    static {
        // Core/navigation
        add("Калькуляторы", "Hesaplayıcılar", "Calculadoras");
        add("Выберите нужный раздел", "Gerekli bölümü seçin", "Elige una sección");
        add("Кредит", "Kredi", "Préstamo");
        add("Ипотека", "Konut kredisi", "Hipoteca");
        add("Автокредит", "Araç kredisi", "Préstamo de auto");
        add("Рассрочка", "Taksit", "Pago a plazos");
        add("Вклад", "Mevduat", "Depósito");
        add("Мои платежи", "Ödemelerim", "Mis pagos");
        add("Архив", "Arşiv", "Archivo");
        add("Корзина", "Çöp kutusu", "Papelera");
        add("Настройки", "Ayarlar", "Ajustes");
        add("О приложении", "Uygulama hakkında", "Acerca de");
        add("Выход", "Çıkış", "Salir");
        add("Финансовый калькулятор", "Finans hesaplayıcısı", "Calculadora financiera");
        add("Назад", "Geri", "Atrás");
        add("Сохранить", "Kaydet", "Guardar");
        add("Отмена", "İptal", "Cancelar");
        add("Применить", "Uygula", "Aplicar");
        add("Продолжить", "Devam et", "Continuar");
        add("Удалить", "Sil", "Eliminar");
        add("Редактировать", "Düzenle", "Editar");
        add("История изменений", "Değişiklik geçmişi", "Historial de cambios");

        // Calculator
        add("Рассчитать", "Hesapla", "Calcular");
        add("Результат", "Sonuç", "Resultado");
        add("Сумма кредита, ₽", "Kredi tutarı, ₽", "Importe del préstamo, ₽");
        add("Стоимость жилья, ₽", "Konut fiyatı, ₽", "Precio de la vivienda, ₽");
        add("Стоимость автомобиля, ₽", "Araç fiyatı, ₽", "Precio del vehículo, ₽");
        add("Стоимость покупки, ₽", "Satın alma tutarı, ₽", "Precio de compra, ₽");
        add("Сумма вклада, ₽", "Mevduat tutarı, ₽", "Importe del depósito, ₽");
        add("Первоначальный взнос, ₽", "Peşinat, ₽", "Pago inicial, ₽");
        add("Страховка, ₽", "Sigorta, ₽", "Seguro, ₽");
        add("Страховка, ₽ (входит в сумму кредита)", "Sigorta, ₽ (krediye dahil)", "Seguro, ₽ (incluido en el préstamo)");
        add("Включить страховку в сумму кредита", "Sigortayı kredi tutarına dahil et", "Incluir el seguro en el préstamo");
        add("Включить страховку в рассрочку", "Sigortayı taksite dahil et", "Incluir el seguro en las cuotas");
        add("Срок", "Vade", "Plazo");
        add("Срок кредита", "Kredi vadesi", "Plazo del préstamo");
        add("Срок ипотеки", "Konut kredisi vadesi", "Plazo de la hipoteca");
        add("Срок рассрочки", "Taksit süresi", "Plazo de cuotas");
        add("Срок вклада", "Mevduat süresi", "Plazo del depósito");
        add("Процентная ставка, % годовых", "Yıllık faiz, %", "Interés anual, %");
        add("Ставка, % годовых", "Yıllık faiz, %", "Interés anual, %");
        add("Тип платежей", "Ödeme türü", "Tipo de pagos");
        add("Аннуитетный", "Eşit taksitli", "Cuota fija");
        add("Дифференцированный", "Azalan taksitli", "Cuota decreciente");
        add("Ежемесячный платёж", "Aylık ödeme", "Pago mensual");
        add("Первый платёж", "İlk ödeme", "Primer pago");
        add("Последний платёж", "Son ödeme", "Último pago");
        add("Общая сумма выплат", "Toplam ödeme", "Total de pagos");
        add("Переплата", "Fazla ödeme", "Sobrecoste");
        add("Переплата по процентам", "Faiz maliyeti", "Intereses totales");
        add("Всего выплат банку", "Bankaya toplam ödeme", "Total pagado al banco");
        add("Сумма в рассрочку", "Taksit tutarı", "Importe financiado");
        add("Общая стоимость", "Toplam maliyet", "Coste total");
        add("Доход по вкладу", "Mevduat getirisi", "Rendimiento del depósito");
        add("Итоговая сумма", "Toplam tutar", "Importe final");
        add("Капитализация", "Bileşik faiz", "Capitalización");
        add("Ежемесячная", "Aylık", "Mensual");
        add("Без капитализации", "Bileşik faiz yok", "Sin capitalización");
        add("Ежемесячная капитализация процентов", "Aylık faiz kapitalizasyonu", "Capitalización mensual de intereses");
        add("Заполните все поля правильно", "Tüm alanları doğru doldurun", "Completa correctamente todos los campos");
        add("Выберите раздел", "Bir bölüm seçin", "Elige una sección");

        // Payment records
        add("Новое напоминание", "Yeni hatırlatıcı", "Nuevo recordatorio");
        add("Редактировать запись", "Kaydı düzenle", "Editar registro");
        add("Добавить платёж", "Ödeme ekle", "Añadir pago");
        add("Название", "Ad", "Nombre");
        add("Тип", "Tür", "Tipo");
        add("Дата первого платежа", "İlk ödeme tarihi", "Fecha del primer pago");
        add("Дата открытия вклада", "Mevduat açılış tarihi", "Fecha de apertura");
        add("Напомнить до платежа", "Ödemeden önce hatırlat", "Recordar antes del pago");
        add("Время напоминания", "Hatırlatma saati", "Hora del recordatorio");
        add("Запись сохранена", "Kayıt kaydedildi", "Registro guardado");
        add("Изменения сохранены", "Değişiklikler kaydedildi", "Cambios guardados");
        add("Проверьте заполненные поля", "Alanları kontrol edin", "Revisa los campos");
        add("Выберите дату первого платежа", "İlk ödeme tarihini seçin", "Elige la fecha del primer pago");
        add("Выберите дату открытия вклада", "Mevduat açılış tarihini seçin", "Elige la fecha de apertura");
        add("Прошедшие платежи", "Geçmiş ödemeler", "Pagos anteriores");
        add("Прошедшие платежи были оплачены по графику?", "Geçmiş ödemeler plana göre ödendi mi?", "¿Los pagos anteriores se pagaron según el calendario?");
        add("Да, отметить оплаченными", "Evet, ödendi olarak işaretle", "Sí, marcarlos como pagados");
        add("Нет, оставить неоплаченными", "Hayır, ödenmemiş bırak", "No, dejarlos pendientes");

        // Payments overview
        add("Сначала показываются записи с ближайшим платежом.", "En yakın ödemeler önce gösterilir.", "Primero se muestran los pagos más próximos.");
        add("Общая сводка", "Genel özet", "Resumen general");
        add("Общий остаток долга", "Toplam kalan borç", "Deuda restante total");
        add("К оплате в этом месяце", "Bu ay ödenecek", "A pagar este mes");
        add("Оплачено", "Ödendi", "Pagado");
        add("Осталось оплатить", "Kalan ödeme", "Pendiente");
        add("Просрочено", "Gecikmiş", "Vencido");
        add("Следующий платёж", "Sonraki ödeme", "Próximo pago");
        add("Ближайший платёж", "En yakın ödeme", "Pago más próximo");
        add("Сегодня", "Bugün", "Hoy");
        add("Предстоящий", "Yaklaşan", "Próximo");
        add("Оплачен", "Ödendi", "Pagado");
        add("Просрочен", "Gecikmiş", "Vencido");
        add("Активные записи — ", "Aktif kayıtlar — ", "Registros activos — ");
        add("Фильтр", "Filtre", "Filtro");
        add("Все", "Tümü", "Todos");
        add("Сортировка ⇅", "Sıralama ⇅", "Ordenar ⇅");
        add("Ближайший платёж", "En yakın ödeme", "Pago más próximo");
        add("Дата создания — новые сначала", "Oluşturma — yeniler önce", "Creación — nuevos primero");
        add("Дата создания — старые сначала", "Oluşturma — eskiler önce", "Creación — antiguos primero");
        add("Название А–Я", "Ad A–Z", "Nombre A–Z");
        add("Название Я–А", "Ad Z–A", "Nombre Z–A");
        add("Остаток долга — больше сначала", "Borç — yüksekten düşüğe", "Deuda — mayor primero");
        add("Остаток долга — меньше сначала", "Borç — düşükten yükseğe", "Deuda — menor primero");
        add("Ежемесячный платёж — больше сначала", "Ödeme — yüksekten düşüğe", "Pago — mayor primero");
        add("Ежемесячный платёж — меньше сначала", "Ödeme — düşükten yükseğe", "Pago — menor primero");
        add("Процентная ставка — больше сначала", "Faiz — yüksekten düşüğe", "Interés — mayor primero");
        add("Процентная ставка — меньше сначала", "Faiz — düşükten yükseğe", "Interés — menor primero");
        add("По типу", "Türe göre", "Por tipo");
        add("Переплата по активным кредитам", "Aktif kredilerin faiz maliyeti", "Intereses de préstamos activos");
        add("Общая переплата", "Toplam faiz", "Intereses totales");
        add("Уже выплачено процентов", "Ödenen faiz", "Intereses pagados");
        add("Осталось выплатить процентов", "Kalan faiz", "Intereses pendientes");
        add("Активные вклады", "Aktif mevduatlar", "Depósitos activos");
        add("Во вкладах", "Mevduatta", "Depositado");
        add("Ожидаемый доход", "Beklenen getiri", "Rendimiento esperado");
        add("Итого к получению", "Beklenen toplam", "Total esperado");
        add("Исходная сумма: ", "Başlangıç tutarı: ", "Importe inicial: ");
        add("Остаток долга: ", "Kalan borç: ", "Deuda restante: ");
        add("Ежемесячно: ", "Aylık: ", "Mensual: ");
        add("Переплата: ", "Faiz: ", "Intereses: ");
        add("Следующий платёж: ", "Sonraki ödeme: ", "Próximo pago: ");
        add("Срок завершён", "Vade tamamlandı", "Plazo finalizado");

        // Details/schedule
        add("Условия кредита", "Kredi koşulları", "Condiciones del préstamo");
        add("Проценты и переплата", "Faiz ve maliyet", "Intereses y sobrecoste");
        add("Остаток долга", "Kalan borç", "Deuda restante");
        add("Дата платежа", "Ödeme tarihi", "Fecha de pago");
        add("Ставка", "Faiz", "Interés");
        add("Осталось платить", "Kalan vade", "Plazo restante");
        add("Исходная сумма", "Başlangıç tutarı", "Importe inicial");
        add("Страховка", "Sigorta", "Seguro");
        add("Сумма кредита с учётом страховки", "Sigorta dahil kredi tutarı", "Préstamo con seguro");
        add("Первоначальный срок", "İlk vade", "Plazo inicial");
        add("Первый платёж", "İlk ödeme", "Primer pago");
        add("Общая переплата по кредиту", "Toplam kredi faizi", "Intereses totales del préstamo");
        add("Выплачено", "Ödendi", "Pagado");
        add("Погашено основного долга: ", "Ödenen anapara: ", "Capital amortizado: ");
        add("График платежей", "Ödeme planı", "Calendario de pagos");
        add("Скрыть архив", "Arşivi gizle", "Ocultar archivo");
        add("Основной долг", "Anapara", "Capital");
        add("Проценты", "Faiz", "Intereses");
        add("Остаток долга после платежа", "Ödeme sonrası kalan borç", "Deuda después del pago");
        add("Изменить сумму", "Tutarı değiştir", "Cambiar importe");
        add("Плановый платёж", "Planlanan ödeme", "Pago previsto");
        add("Фактически оплачено", "Gerçekte ödenen", "Pagado realmente");
        add("Только к этому платежу", "Yalnızca bu ödemeye", "Solo a este pago");
        add("К этому и всем следующим", "Bu ve sonraki tüm ödemelere", "A este y a todos los siguientes");
        add("Изменить также архивные платежи?", "Arşivdeki ödemeler de değiştirilsin mi?", "¿Cambiar también los pagos archivados?");
        add("Нет, оставить архив без изменений", "Hayır, arşivi değiştirme", "No, dejar el archivo sin cambios");
        add("Да, применить и к архивным", "Evet, arşive de uygula", "Sí, aplicar también al archivo");
        add("Платёж оплачен", "Ödeme yapıldı", "Pago realizado");
        add("Дата оплаты", "Ödeme tarihi", "Fecha de pago");
        add("Штраф / пеня, ₽", "Ceza / gecikme, ₽", "Penalización, ₽");

        // Early/refinance
        add("Досрочное погашение", "Erken ödeme", "Amortización anticipada");
        add("Сумма досрочного погашения, ₽", "Erken ödeme tutarı, ₽", "Importe anticipado, ₽");
        add("Дата досрочного погашения", "Erken ödeme tarihi", "Fecha de amortización");
        add("Что изменить после погашения?", "Ödemeden sonra ne değişsin?", "¿Qué cambiar después del pago?");
        add("Уменьшить ежемесячный платёж", "Aylık ödemeyi azalt", "Reducir la cuota mensual");
        add("Сократить срок", "Vadeyi kısalt", "Reducir el plazo");
        add("Новый платёж", "Yeni ödeme", "Nueva cuota");
        add("Новый срок", "Yeni vade", "Nuevo plazo");
        add("Новая дата окончания", "Yeni bitiş tarihi", "Nueva fecha final");
        add("Осталось процентов", "Kalan faiz", "Intereses pendientes");
        add("Экономия на процентах", "Faiz tasarrufu", "Ahorro en intereses");
        add("Выгоднее по переплате", "Faiz açısından daha avantajlı", "Más conveniente por intereses");
        add("Меньше экономия на процентах", "Daha az faiz tasarrufu", "Menor ahorro en intereses");
        add("Применить выбранный вариант", "Seçilen seçeneği uygula", "Aplicar la opción elegida");
        add("Рефинансирование", "Yeniden finansman", "Refinanciación");
        add("Оставить текущий кредит", "Mevcut krediyi koru", "Mantener préstamo actual");
        add("Рефинансировать", "Yeniden finanse et", "Refinanciar");
        add("Новая ставка, %", "Yeni faiz, %", "Nuevo interés, %");
        add("Новый срок, мес.", "Yeni vade, ay", "Nuevo plazo, meses");
        add("Комиссия, ₽", "Komisyon, ₽", "Comisión, ₽");
        add("Страховка нового кредита, ₽", "Yeni kredi sigortası, ₽", "Seguro del nuevo préstamo, ₽");
        add("Дата рефинансирования", "Yeniden finansman tarihi", "Fecha de refinanciación");
        add("Новая сумма кредита, ₽", "Yeni kredi tutarı, ₽", "Nuevo importe del préstamo, ₽");
        add("Дополнительные расходы", "Ek masraflar", "Gastos adicionales");
        add("Всего будущих выплат", "Gelecek toplam ödemeler", "Pagos futuros totales");
        add("Экономия", "Tasarruf", "Ahorro");

        // Settings/security
        add("Язык приложения", "Uygulama dili", "Idioma de la aplicación");
        add("Оформление", "Görünüm", "Apariencia");
        add("День", "Gündüz", "Día");
        add("Ночь", "Gece", "Noche");
        add("Фон приложения", "Uygulama arka planı", "Fondo de la aplicación");
        add("Выбрать изображение", "Görsel seç", "Elegir imagen");
        add("Сбросить фон", "Arka planı sıfırla", "Restablecer fondo");
        add("Оповещения", "Bildirimler", "Notificaciones");
        add("Звук уведомления", "Bildirim sesi", "Sonido de notificación");
        add("Вибрация", "Titreşim", "Vibración");
        add("Резервное копирование", "Yedekleme", "Copia de seguridad");
        add("Сохранить резервную копию", "Yedek oluştur", "Guardar copia");
        add("Восстановить из файла", "Dosyadan geri yükle", "Restaurar desde archivo");
        add("Без пароля", "Parolasız", "Sin contraseña");
        add("С паролем", "Parolalı", "Con contraseña");
        add("Безопасность", "Güvenlik", "Seguridad");
        add("Защита приложения", "Uygulama koruması", "Protección de la aplicación");
        add("PIN-код", "PIN kodu", "Código PIN");
        add("Пароль", "Parola", "Contraseña");
        add("Отпечаток пальца / биометрия", "Parmak izi / biyometri", "Huella / biometría");
        add("Автоблокировка", "Otomatik kilit", "Bloqueo automático");
        add("Сразу", "Hemen", "Inmediatamente");
        add("Через 1 минуту", "1 dakika sonra", "Después de 1 minuto");
        add("Через 5 минут", "5 dakika sonra", "Después de 5 minutos");
        add("Через 15 минут", "15 dakika sonra", "Después de 15 minutos");
        add("Изменить PIN/пароль", "PIN/parolayı değiştir", "Cambiar PIN/contraseña");
        add("Сбросить все данные", "Tüm verileri sıfırla", "Restablecer todos los datos");
        add("Полный сброс приложения", "Uygulamayı tamamen sıfırla", "Restablecimiento completo");
        add("Сбросить все данные?", "Tüm veriler sıfırlansın mı?", "¿Restablecer todos los datos?");
        add("Подтвердить сброс", "Sıfırlamayı onayla", "Confirmar restablecimiento");
        add("Введите СБРОС", "SIFIRLA yazın", "Escribe RESTABLECER");
        add("Данные удалены", "Veriler silindi", "Datos eliminados");

        // Lock/language
        add("Выберите язык", "Dil seçin", "Elige idioma");
        add("Язык можно изменить позже в настройках.", "Dili daha sonra ayarlardan değiştirebilirsiniz.", "Puedes cambiar el idioma más tarde en Ajustes.");
        add("Введите PIN-код", "PIN kodunu girin", "Introduce el PIN");
        add("Введите пароль", "Parolayı girin", "Introduce la contraseña");
        add("Разблокировать", "Kilidi aç", "Desbloquear");
        add("Неверный PIN/пароль", "Yanlış PIN/parola", "PIN/contraseña incorrectos");
        add("Использовать биометрию", "Biyometri kullan", "Usar biometría");

        // Notifications
        add("Платёж сегодня", "Ödeme bugün", "Pago hoy");
        add("Срок оплаты: сегодня", "Son ödeme: bugün", "Vence hoy");
        add("Напомнить позже", "Daha sonra hatırlat", "Recordar más tarde");
        add("5 минут", "5 dakika", "5 minutos");
        add("10 минут", "10 dakika", "10 minutos");
        add("15 минут", "15 dakika", "15 minutos");
        add("30 минут", "30 dakika", "30 minutos");
        add("1 час", "1 saat", "1 hora");
        add("Выбрать вручную", "Elle seç", "Elegir manualmente");

        // About/instruction/feedback
        add("Инструкция", "Kullanım kılavuzu", "Instrucciones");
        add("Обратная связь", "Geri bildirim", "Comentarios");
        add("Сообщить об ошибке", "Hata bildir", "Informar de un error");
        add("Предложить улучшение", "İyileştirme öner", "Sugerir una mejora");
        add("Адрес обратной связи будет добавлен позже.", "Geri bildirim adresi daha sonra eklenecek.", "La dirección de contacto se añadirá más adelante.");
        add("Оценить приложение", "Uygulamayı değerlendir", "Valorar la aplicación");
        add("Мои обращения", "Başvurularım", "Mis solicitudes");
        add("Название обращения", "Başvuru başlığı", "Título de la solicitud");
        add("Описание", "Açıklama", "Descripción");
        add("Фото и видео", "Fotoğraf ve video", "Fotos y vídeos");
        add("Прикрепить фото или видео", "Fotoğraf veya video ekle", "Adjuntar fotos o vídeos");
        add("Отправить обращение", "Başvuruyu gönder", "Enviar solicitud");
        add("Ошибка", "Hata", "Error");
        add("Предложение", "Öneri", "Sugerencia");
        add("Оценить", "Değerlendir", "Valorar");
        add("Не сейчас", "Şimdi değil", "Ahora no");
        add("Осталось платежей в этом месяце", "Bu ay kalan ödeme sayısı", "Pagos restantes este mes");
        add("Финансовая статистика", "Finansal istatistikler", "Estadísticas financieras");
        add("Досрочно погашено по активным кредитам", "Aktif kredilerde erken ödenen", "Amortizado anticipadamente en créditos activos");
        add("Кредиты — за всё время", "Krediler — tüm dönem", "Créditos — histórico");
        add("Активные кредиты — что впереди", "Aktif krediler — gelecek", "Créditos activos — futuro");
        add("Архив кредитов", "Kredi arşivi", "Archivo de créditos");
        add("Вклады", "Mevduatlar", "Depósitos");
        add("Всего внесено по кредитам", "Kredilere toplam ödeme", "Total pagado a créditos");
        add("Досрочно погашено", "Erken ödenen", "Amortización anticipada");
        add("Уже выплачено процентов", "Ödenmiş faiz", "Intereses ya pagados");
        add("Будущие проценты", "Gelecek faiz", "Intereses futuros");
        add("Сэкономлено за всё время", "Toplam tasarruf", "Ahorro histórico");
        add("Ожидаемый доход по активным вкладам", "Aktif mevduat beklenen getirisi", "Ingreso esperado de depósitos activos");
        add("Доход завершённых вкладов", "Tamamlanan mevduat getirisi", "Ingreso de depósitos finalizados");
        add("Разработчик", "Geliştirici", "Desarrollador");
        add("Версия", "Sürüm", "Versión");
    }

    private Translations() {}

    private static void add(String ru, String tr, String es) {
        TR.put(ru, tr);
        ES.put(ru, es);
    }

    public static String translate(String language, String ru, String en) {
        String value = "tr".equals(language) ? TR.get(ru) : ES.get(ru);
        return value == null || value.trim().isEmpty() ? en : value;
    }
}

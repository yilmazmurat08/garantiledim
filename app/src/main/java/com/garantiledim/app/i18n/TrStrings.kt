package com.garantiledim.app.i18n

import com.garantiledim.app.domain.Category
import com.garantiledim.app.domain.DurationType
import com.garantiledim.app.domain.ProductFilter
import java.time.LocalDate
import java.util.Locale

object TrStrings : Strings {
    override val language = AppLanguage.TR
    override val locale: Locale = AppLanguage.TR.locale
    private val months = listOf("Ocak", "Şubat", "Mart", "Nisan", "Mayıs", "Haziran", "Temmuz", "Ağustos", "Eylül", "Ekim", "Kasım", "Aralık")
    private val monthsShort = listOf("Oca", "Şub", "Mar", "Nis", "May", "Haz", "Tem", "Ağu", "Eyl", "Eki", "Kas", "Ara")
    private val weekdays = listOf("Pazartesi", "Salı", "Çarşamba", "Perşembe", "Cuma", "Cumartesi", "Pazar")

    /** "1 yıl 3 ay", "2 ay", "45 gün" */
    private fun amount(days: Long, years: Int, months: Int): String = when {
        days <= 60 -> "$days gün"
        years > 0 && months > 0 -> "$years yıl $months ay"
        years > 0 -> "$years yıl"
        else -> "$months ay"
    }

    // Tarihler
    override fun dateShort(date: LocalDate): String = "${date.dayOfMonth} ${monthsShort[date.monthValue - 1]} ${date.year}"
    override fun dateLong(date: LocalDate): String = "${date.dayOfMonth} ${months[date.monthValue - 1]} ${date.year}, ${weekday(date)}"
    override fun dateNumeric(date: LocalDate): String = String.format(Locale.ROOT, "%02d.%02d.%04d", date.dayOfMonth, date.monthValue, date.year)
    override fun weekday(date: LocalDate): String = weekdays[date.dayOfWeek.value - 1]
    override fun monthShortUpper(date: LocalDate): String = monthsShort[date.monthValue - 1].uppercase(locale)
    override fun time(hour: Int, minute: Int): String = String.format(Locale.ROOT, "%02d:%02d", hour, minute)

    // Kalan süre
    override val expired: String = "Süre doldu"
    override val lastDayToday: String = "Bugün son gün"
    override val today: String = "Bugün"
    override fun remainingShort(days: Long, years: Int, months: Int): String = amount(days, years, months)
    override fun remaining(days: Long, years: Int, months: Int): String = "${amount(days, years, months)} kaldı"
    override fun elapsedExpired(end: LocalDate): String = "Süre ${dateLong(end)} tarihinde doldu."
    override fun elapsedNotStarted(start: LocalDate): String = "Süre teslim tarihinde (${dateShort(start)}) başlayacak."
    override fun elapsedProgress(type: DurationType, totalYears: Int?, totalDays: Long, elapsedDays: Long): String = buildString {
        append(if (totalYears != null) "$totalYears yıllık " else "$totalDays günlük ")
        append(if (type == DurationType.IADE) "iade süresinin " else "garantinin ")
        append(if (elapsedDays == 0L) "ilk günü." else "$elapsedDays günü geçti.")
    }.replaceFirstChar { it.uppercase(locale) }
    override val manualDateNote: String = " Tarih elle girildi."

    // Ürün alanları
    override fun category(category: Category): String = when (category) {
        Category.ELEKTRONIK -> "Elektronik"
        Category.BEYAZ_ESYA -> "Beyaz eşya"
        Category.GIYIM -> "Giyim"
        Category.DIGER -> "Diğer"
    }
    override fun duration(type: DurationType): String = if (type == DurationType.IADE) "İade hakkı" else "Garanti"
    override fun durationShort(type: DurationType): String = if (type == DurationType.IADE) "İade" else "Garanti"
    override fun durationEnd(type: DurationType): String = if (type == DurationType.IADE) "İade son günü" else "Garanti bitişi"
    override fun filter(filter: ProductFilter): String = when (filter) {
        ProductFilter.ALL -> "Tümü"
        ProductFilter.UPCOMING -> "Yaklaşan"
        ProductFilter.ACTIVE -> "Aktif"
        ProductFilter.EXPIRED -> "Süresi dolan"
    }
    override fun fileSize(bytes: Long): String = when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> String.format(locale, "%.1f MB", bytes / (1024.0 * 1024.0))
    }

    // Ajanda
    override val agendaThisWeek: String = "Bu hafta"
    override val agendaNext30: String = "Önümüzdeki 30 gün"
    override val agendaLater: String = "Daha sonra"

    // Bildirim metinleri
    override val notificationChannel: String = "Süre hatırlatmaları"
    override val notificationChannelDescription: String = "İade ve garanti süreleri dolmadan hatırlatır"
    override fun reminderDaysLeftTitle(type: DurationType): String = "${duration(type)} süresi bitiyor"
    override fun reminderDaysLeftBody(name: String, type: DurationType, days: Long): String = "$name için ${duration(type)} süresinin bitmesine $days gün kaldı"
    override fun reminderDueTodayTitle(type: DurationType): String = "${duration(type)} süresi bugün doluyor"
    override fun reminderDueTodayBody(name: String, type: DurationType): String = "$name için ${duration(type)} süresi bugün doluyor"
    override fun reminderExpiredTitle(type: DurationType): String = "${duration(type)} süresi doldu"
    override fun reminderExpiredBody(name: String, type: DurationType): String = "$name için ${duration(type)} süresi doldu"

    // Gezinme
    override val tabHome: String = "Ana Sayfa"
    override val tabList: String = "Garanti Belgelerim"
    override val tabAgenda: String = "Ajanda ve Hatırlatıcılar"
    override val tabProfile: String = "Profil & Ayarlar"
    override val back: String = "Geri"

    // Ana Sayfa
    override val homeSuggested: String = "Önerilen İşlemler"
    override val actionAddTitle: String = "Yeni Garanti Ekle"
    override val actionAddBody: String = "Hızlıca yeni ürün ve belgesini kaydet."
    override val actionReminderTitle: String = "Hatırlatıcı Ayarla"
    override val actionReminderBody: String = "Yaklaşan bitiş tarihleri için alarm"
    override val actionDocsTitle: String = "Belgeleri Düzenle"
    override val actionDocsBody: String = "Tüm kayıtları kolayca filtrele"
    override val homeToday: String = "Günün Garantileri"
    override val seeAll: String = "Tümü"
    override val homeNothingUpcoming: String = "Yaklaşan ya da yeni dolan bir süre yok. Tüm ürünlerin Garanti Belgelerim'de."
    override val hello: String = "Merhaba!"
    override fun helloName(name: String): String = "Merhaba, $name!"
    override val tagline: String = "Garanti sürelerini takipte kal!"
    override val notifications: String = "Bildirimler"
    override val notificationsUrgent: String = "Bildirimler, acil hatırlatma var"
    override val firstProduct: String = "İlk ürününü ekle"
    override val legalNoticePrefix: String = "Garantiledim'i kullanarak "
    override val legalNoticeMiddle: String = "'nı kabul etmiş olursun. Bilgilerin yalnızca bu telefonda saklanır; ayrıntılar "
    override val legalNoticeSuffix: String = "'nda."

    // Garanti Belgelerim
    override fun productsTracked(count: Int): String = "$count ürün takip ediliyor"
    override val searchHint: String = "Ürün veya mağaza ara"
    override val noProductsTitle: String = "Henüz ürün yok"
    override val noProductsBody: String = "Sağ alttaki + ile ilk ürününü ve faturasını ekle."
    override val noMatchTitle: String = "Sonuç yok"
    override val noMatchBody: String = "Bu filtreye uyan ürün yok."
    override val expiredSection: String = "Süresi dolanlar"
    override val addWarrantyA11y: String = "Yeni garanti ekle"

    // Ajanda
    override fun reminderSummary(leadDays: Int, time: String): String = "Bitişten $leadDays gün önce başlar, her gün $time'da"
    override val notificationsOff: String = "Bildirimler kapalı, hatırlatma alamazsın"
    override val openSettings: String = "Ayarlara git"
    override val reminderSetting: String = "Hatırlatma ayarı"
    override val adjust: String = "Ayarla"
    override val agendaEmptyTitle: String = "Yaklaşan bir bitiş tarihi yok"
    override val agendaEmptyBody: String = "Süresi devam eden ürünlerin bitiş tarihleri burada sıralanır."

    // Yasal
    override val privacyPolicy: String = "Gizlilik Politikası"
    override val termsOfUse: String = "Kullanım Koşulları"
    override val legalLoadErrorTitle: String = "Metin açılamadı"
    override fun legalLoadErrorBody(email: String): String = "Lütfen uygulamayı güncelleyin ya da $email adresine yazın."
    override fun askQuestion(email: String): String = "Soru sor: $email"

    // Ortak
    override val ok: String = "Tamam"
    override val cancel: String = "Vazgeç"
    override val close: String = "Kapat"
    override val save: String = "Kaydet"
    override fun selectA11y(label: String): String = "$label seç"
    override val notificationTime: String = "Bildirim saati"
    override val fileTypePdf: String = "PDF"
    override val fileTypeFile: String = "DOSYA"
    override val fileTypeImage: String = "Görsel"
    override val original: String = "orijinal"
    override val removeReceiptA11y: String = "Fişi kaldır"

    // Ürün Detayı
    override val detailTitle: String = "Ürün Detayı"
    override val edit: String = "Düzenle"
    override val productNotFoundTitle: String = "Ürün bulunamadı"
    override val productNotFoundBody: String = "Bu ürün silinmiş olabilir."
    override val purchaseDate: String = "Satın alma"
    override val deliveryDate: String = "Teslim"
    override val receiptSaved: String = "Fatura İndirilenler klasörüne kaydedildi"
    override val receiptSaveFailed: String = "Dosya indirilemedi"
    override val storagePermissionNeeded: String = "İndirmek için depolama izni gerekiyor"
    override val noAppToOpen: String = "Bu dosyayı açacak uygulama bulunamadı"
    override val addReceipt: String = "Fiş / fatura ekle"
    override val lockedTitle: String = "Bu ürünün hatırlatması kapalı"
    override fun lockedBody(limit: Int): String = "Ücretsiz sürümde ilk $limit ürünün hatırlatmaları çalışır."
    override val premium: String = "Premium"
    override val openReceiptA11y: String = "Faturayı aç"
    override val receiptMissing: String = "Dosya bu telefonda yok, yeniden ekleyebilirsin"
    override val add: String = "Ekle"
    override val download: String = "İndir"

    // Ürün ekle / düzenle
    override val filePickerMissing: String = "Dosya seçici bulunamadı"
    override val galleryMissing: String = "Galeri bulunamadı"
    override val cameraMissing: String = "Kamera uygulaması bulunamadı"
    override val editTitle: String = "Ürünü Düzenle"
    override val productName: String = "Ürün adı *"
    override val store: String = "Mağaza"
    override val purchaseDateField: String = "Satın alma tarihi"
    override val deliveryDateField: String = "Teslim tarihi"
    override val categoryField: String = "Kategori"
    override val trackedPeriods: String = "Takip edilecek süreler"
    override val periodsHelp: String = "Mağazadan aldıysan iade hakkını kapatabilir ya da mağazanın süresini girebilirsin. Ek garanti varsa garanti tarihini değiştir."
    override val receiptField: String = "Fiş / fatura"
    override val fromGallery: String = "Galeriden / dosyadan"
    override val takePhoto: String = "Kamera ile çek"
    override val receiptHelp: String = "Fotoğraf veya PDF fatura eklenebilir. Orijinal dosya bozulmadan saklanır."
    override val addProductPhoto: String = "Ürün fotoğrafı ekle (opsiyonel)"
    override val chooseFromGallery: String = "Galeriden seç"
    override val productPhoto: String = "Ürün fotoğrafı"
    override val removePhotoA11y: String = "Fotoğrafı kaldır"
    override val deleteProduct: String = "Ürünü sil"
    override val unsavedTitle: String = "Değişiklikler kaydedilmedi"
    override val unsavedBody: String = "Çıkarsan girdiğin bilgiler ve eklediğin dosyalar kaybolacak."
    override val leave: String = "Çık"
    override val deleteTitle: String = "Ürün silinsin mi?"
    override fun deleteBody(name: String): String = "\"$name\" ve eklenen fiş/fatura kalıcı olarak silinecek."
    override val delete: String = "Sil"
    override val enteredManually: String = "Elle girildi · "
    override val reset: String = "Sıfırla"
    override fun periodDefault(type: DurationType): String = if (type == DurationType.IADE) "Teslimden itibaren 14 gün" else "Teslimden itibaren 2 yıl"
    override val errorProductNotFound: String = "Ürün bulunamadı"
    override val errorNameEmpty: String = "Ürün adı boş olamaz"
    override val errorDeliveryBeforePurchase: String = "Teslim tarihi satın alma tarihinden önce olamaz"
    override val errorNoPeriod: String = "En az bir süre takip edilmeli"
    override val errorEndBeforeDelivery: String = "Bitiş tarihi teslim tarihinden önce olamaz"
    override fun errorFreeLimit(limit: Int): String = "Ücretsiz sürümde en fazla $limit ürün eklenebilir"
    override val errorSaveFailed: String = "Kaydedilemedi, tekrar dene"
    override val errorReceiptAdd: String = "Fiş/fatura eklenemedi"
    override val errorPhotoAdd: String = "Fotoğraf eklenemedi"

    // Profil & Ayarlar
    override fun backupSaved(count: Int): String = "Yedek kaydedildi ($count ürün)"
    override fun backupRestored(count: Int): String = "Yedek geri yüklendi ($count ürün)"
    override val backupNeedsPremium: String = "Yedekleme Premium'a dahildir"
    override val backupCorrupt: String = "Yedek dosyası bozuk"
    override val backupNotOurs: String = "Bu dosya bir Garantiledim yedeği değil"
    override val backupTooNew: String = "Bu yedek daha yeni bir sürümle alınmış; önce uygulamayı güncelle"
    override val operationFailed: String = "İşlem tamamlanamadı, tekrar dene"
    override val choosePhotoA11y: String = "Fotoğraf seç"
    override val addYourName: String = "Adını ekle"
    override val storedOnDevice: String = "Bilgilerin sadece bu cihazda saklanır"
    override val editNameA11y: String = "Adı düzenle"
    override val removeProfilePhoto: String = "Profil fotoğrafını kaldır"
    override val premiumActiveSummary: String = "Sınırsız ürün ve yedekleme"
    override val active: String = "Aktif"
    override val goPremium: String = "Premium'a geç"
    override val goPremiumSummary: String = "Sınırsız ürün, yedekleme · tek seferlik ödeme"
    override val backupSection: String = "Yedekleme"
    override val backupAction: String = "Yedekle"
    override val processing: String = "İşleniyor…"
    override val backupSummary: String = "Ürünler, faturalar ve fotoğraflar tek dosyada"
    override val backupFilePrefix: String = "garantiledim-yedek"
    override val saveScreenMissing: String = "Dosya kaydetme ekranı bulunamadı"
    override val restoreAction: String = "Yedekten geri yükle"
    override val restoreSummary: String = "Yeni telefona taşırken"
    override val remindersSection: String = "Hatırlatmalar"
    override val reminderStart: String = "Hatırlatma başlangıcı"
    override fun daysBeforeEnd(days: Int): String = "Bitişten $days gün önce"
    override fun everyDayAt(time: String): String = "Her gün $time"
    override val notificationPermission: String = "Bildirim izni"
    override val permissionOn: String = "Açık"
    override val permissionOffTap: String = "Kapalı · açmak için dokun"
    override val defaultPeriods: String = "Varsayılan süreler"
    override val days14: String = "14 gün"
    override val years2: String = "2 yıl"
    override val legalSection: String = "Yasal"
    override val otherSection: String = "Diğer"
    override val contactUs: String = "Bize yazın"
    override val about: String = "Hakkında"
    override fun version(name: String): String = "Sürüm $name"
    override val restoreConfirmTitle: String = "Yedekten geri yüklensin mi?"
    override val restoreConfirmBody: String = "Bu telefondaki ürünler, seçeceğin yedekteki ürünlerle değiştirilecek. Önce mevcut verilerini yedeklemek istersen Vazgeç'e bas."
    override val chooseBackup: String = "Yedeği seç"
    override val yourName: String = "Adın"
    override val nameHint: String = "Karşılama ekranında görünür"
    override val languageSection: String = "Dil"
    override val appLanguage: String = "Uygulama dili"
    override val languageSystem: String = "Sistem dili"

    // Premium
    override val premiumTitle: String = "Garantiledim Premium"
    override val premiumThanks: String = "Premium aktif. Desteğin için teşekkürler!"
    override val benefitUnlimited: String = "Sınırsız ürün"
    override val benefitUnlimitedBody: String = "İstediğin kadar ürün, fiş ve fatura ekle"
    override val benefitReminders: String = "Her ürüne hatırlatma"
    override val benefitRemindersBody: String = "Tüm iade ve garanti süreleri için bildirim"
    override val benefitBackup: String = "Yedekle ve taşı"
    override val benefitBackupBody: String = "Ürünlerin, faturaların ve fotoğrafların tek dosyada; yeni telefona geri yükle"
    override val benefitOneTime: String = "Tek seferlik ödeme"
    override val benefitOneTimeBody: String = "Abonelik yok. Bir kez öde, hep senin"
    override fun freeTierNote(limit: Int): String = "Ücretsiz sürümde $limit ürüne kadar tüm özellikler (yedekleme hariç) kullanılabilir."
    override val continueLabel: String = "Devam et"
    override fun buyPremium(price: String): String = "Premium'u al · $price"
    override val retry: String = "Tekrar dene"
    override val playUnavailable: String = "Google Play'e bağlanılamadı. İnternet bağlantını ve Play Store hesabını kontrol et."
    override val oneTimeDisclosure: String = "Tek seferlik ödeme · Abonelik değildir, otomatik yenilenmez. Ödeme Google Play hesabından alınır."
    override val restorePurchase: String = "Satın alımı geri yükle"
    override fun paywallLimit(limit: Int): String = "Ücretsiz sürümde en fazla $limit ürün takip edebilirsin. Premium ile sınır kalkar."
    override val paywallBackup: String = "Yedekleme Premium'a dahil. Ürünlerini ve faturalarını yeni telefonuna güvenle taşı."
    override val paywallGeneral: String = "Tüm iade ve garanti sürelerin, faturaların tek yerde. Sınırsız."
    override val playRetryLater: String = "Google Play'e bağlanılamadı, biraz sonra tekrar dene"
    override val playConnecting: String = "Google Play'e bağlanılıyor, biraz sonra tekrar dene"
    override val purchaseFailed: String = "Satın alma tamamlanamadı, tekrar dene"
    override val purchaseNotFound: String = "Bu Google hesabında Premium satın alımı bulunamadı"
    override val purchaseRestored: String = "Premium geri yüklendi"
    override val purchaseNotVerified: String = "Satın alma doğrulanamadı"
    override val purchasePending: String = "Ödeme onay bekliyor; tamamlanınca Premium kendiliğinden açılır"

    // Dosya adları
    override val receiptFilePrefix: String = "fatura"
    override val cameraReceiptPrefix: String = "fis"
}

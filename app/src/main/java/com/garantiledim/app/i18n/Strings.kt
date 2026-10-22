package com.garantiledim.app.i18n

import com.garantiledim.app.domain.Category
import com.garantiledim.app.domain.DurationType
import com.garantiledim.app.domain.ProductFilter
import java.time.LocalDate
import java.util.Locale

/**
 * Uygulamadaki tüm kullanıcıya görünen metinler. Her dil bu arayüzü eksiksiz uygular;
 * bir metnin çevirisi eksikse uygulama derlenmez.
 */
interface Strings {
    val language: AppLanguage
    val locale: Locale

    // Tarihler
    /** 5 Eki 2026 / Oct 5, 2026 */
    fun dateShort(date: LocalDate): String
    /** 5 Ekim 2026, Pazartesi / Monday, October 5, 2026 */
    fun dateLong(date: LocalDate): String
    /** Giriş alanları: 05.10.2026 / Oct 5, 2026 */
    fun dateNumeric(date: LocalDate): String
    fun weekday(date: LocalDate): String
    /** EKİ / OCT */
    fun monthShortUpper(date: LocalDate): String
    fun time(hour: Int, minute: Int): String

    // Kalan süre
    val expired: String
    val lastDayToday: String
    val today: String
    /** Kısa rozet: "2 gün", "2 ay" */
    fun remainingShort(days: Long, years: Int, months: Int): String
    /** "2 gün kaldı" */
    fun remaining(days: Long, years: Int, months: Int): String
    fun elapsedExpired(end: LocalDate): String
    fun elapsedNotStarted(start: LocalDate): String
    /** "14 günlük iade süresinin 12 günü geçti." */
    fun elapsedProgress(type: DurationType, totalYears: Int?, totalDays: Long, elapsedDays: Long): String
    val manualDateNote: String

    // Ürün alanları
    fun category(category: Category): String
    fun duration(type: DurationType): String
    fun durationShort(type: DurationType): String
    fun durationEnd(type: DurationType): String
    fun filter(filter: ProductFilter): String
    /** 1,2 MB / 1.2 MB */
    fun fileSize(bytes: Long): String

    // Ajanda
    val agendaThisWeek: String
    val agendaNext30: String
    val agendaLater: String

    // Bildirim metinleri
    val notificationChannel: String
    val notificationChannelDescription: String
    fun reminderDaysLeftTitle(type: DurationType): String
    fun reminderDaysLeftBody(name: String, type: DurationType, days: Long): String
    fun reminderDueTodayTitle(type: DurationType): String
    fun reminderDueTodayBody(name: String, type: DurationType): String
    fun reminderExpiredTitle(type: DurationType): String
    fun reminderExpiredBody(name: String, type: DurationType): String

    // Gezinme
    val tabHome: String
    val tabList: String
    val tabAgenda: String
    val tabProfile: String
    val back: String

    // Ana Sayfa
    val homeSuggested: String
    val actionAddTitle: String
    val actionAddBody: String
    val actionReminderTitle: String
    val actionReminderBody: String
    val actionDocsTitle: String
    val actionDocsBody: String
    val homeToday: String
    val seeAll: String
    val homeNothingUpcoming: String
    val hello: String
    fun helloName(name: String): String
    val tagline: String
    val notifications: String
    val notificationsUrgent: String
    val firstProduct: String
    val legalNoticePrefix: String
    val legalNoticeMiddle: String
    val legalNoticeSuffix: String

    // Garanti Belgelerim
    fun productsTracked(count: Int): String
    val searchHint: String
    val noProductsTitle: String
    val noProductsBody: String
    val noMatchTitle: String
    val noMatchBody: String
    val expiredSection: String
    val addWarrantyA11y: String

    // Ajanda
    fun reminderSummary(leadDays: Int, time: String): String
    val notificationsOff: String
    val openSettings: String
    val reminderSetting: String
    val adjust: String
    val agendaEmptyTitle: String
    val agendaEmptyBody: String

    // Yasal
    val privacyPolicy: String
    val termsOfUse: String
    val legalLoadErrorTitle: String
    fun legalLoadErrorBody(email: String): String
    fun askQuestion(email: String): String

    // Ortak
    val ok: String
    val cancel: String
    val close: String
    val save: String
    fun selectA11y(label: String): String
    val notificationTime: String
    val fileTypePdf: String
    val fileTypeFile: String
    val fileTypeImage: String
    val original: String
    val removeReceiptA11y: String

    // Ürün Detayı
    val detailTitle: String
    val edit: String
    val productNotFoundTitle: String
    val productNotFoundBody: String
    val purchaseDate: String
    val deliveryDate: String
    val receiptSaved: String
    val receiptSaveFailed: String
    val storagePermissionNeeded: String
    val noAppToOpen: String
    val addReceipt: String
    val lockedTitle: String
    fun lockedBody(limit: Int): String
    val premium: String
    val openReceiptA11y: String
    val receiptMissing: String
    val add: String
    val download: String

    // Ürün ekle / düzenle
    val filePickerMissing: String
    val galleryMissing: String
    val cameraMissing: String
    val editTitle: String
    val productName: String
    val store: String
    val purchaseDateField: String
    val deliveryDateField: String
    val categoryField: String
    val trackedPeriods: String
    val periodsHelp: String
    val receiptField: String
    val fromGallery: String
    val takePhoto: String
    val receiptHelp: String
    val addProductPhoto: String
    val chooseFromGallery: String
    val productPhoto: String
    val removePhotoA11y: String
    val deleteProduct: String
    val unsavedTitle: String
    val unsavedBody: String
    val leave: String
    val deleteTitle: String
    fun deleteBody(name: String): String
    val delete: String
    val enteredManually: String
    val reset: String
    fun periodDefault(type: DurationType): String
    val errorProductNotFound: String
    val errorNameEmpty: String
    val errorDeliveryBeforePurchase: String
    val errorNoPeriod: String
    val errorEndBeforeDelivery: String
    fun errorFreeLimit(limit: Int): String
    val errorSaveFailed: String
    val errorReceiptAdd: String
    val errorPhotoAdd: String

    // Profil & Ayarlar
    fun backupSaved(count: Int): String
    fun backupRestored(count: Int): String
    val backupNeedsPremium: String
    val backupCorrupt: String
    val backupNotOurs: String
    val backupTooNew: String
    val operationFailed: String
    val choosePhotoA11y: String
    val addYourName: String
    val storedOnDevice: String
    val editNameA11y: String
    val removeProfilePhoto: String
    val premiumActiveSummary: String
    val active: String
    val goPremium: String
    val goPremiumSummary: String
    val backupSection: String
    val backupAction: String
    val processing: String
    val backupSummary: String
    val backupFilePrefix: String
    val saveScreenMissing: String
    val restoreAction: String
    val restoreSummary: String
    val remindersSection: String
    val reminderStart: String
    fun daysBeforeEnd(days: Int): String
    fun everyDayAt(time: String): String
    val notificationPermission: String
    val permissionOn: String
    val permissionOffTap: String
    val defaultPeriods: String
    val days14: String
    val years2: String
    val legalSection: String
    val otherSection: String
    val contactUs: String
    val about: String
    fun version(name: String): String
    val restoreConfirmTitle: String
    val restoreConfirmBody: String
    val chooseBackup: String
    val yourName: String
    val nameHint: String
    val languageSection: String
    val appLanguage: String
    val languageSystem: String

    // Premium
    val premiumTitle: String
    val premiumThanks: String
    val benefitUnlimited: String
    val benefitUnlimitedBody: String
    val benefitReminders: String
    val benefitRemindersBody: String
    val benefitBackup: String
    val benefitBackupBody: String
    val benefitOneTime: String
    val benefitOneTimeBody: String
    fun freeTierNote(limit: Int): String
    val continueLabel: String
    fun buyPremium(price: String): String
    val retry: String
    val playUnavailable: String
    val oneTimeDisclosure: String
    val restorePurchase: String
    fun paywallLimit(limit: Int): String
    val paywallBackup: String
    val paywallGeneral: String
    val playRetryLater: String
    val playConnecting: String
    val purchaseFailed: String
    val purchaseNotFound: String
    val purchaseRestored: String
    val purchaseNotVerified: String
    val purchasePending: String

    // Dosya adları
    val receiptFilePrefix: String
    val cameraReceiptPrefix: String
}

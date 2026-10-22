package com.garantiledim.app.i18n

import com.garantiledim.app.domain.Category
import com.garantiledim.app.domain.DurationType
import com.garantiledim.app.domain.ProductFilter
import java.time.LocalDate
import java.util.Locale

object EnStrings : Strings {
    override val language = AppLanguage.EN
    override val locale: Locale = AppLanguage.EN.locale
    private val months = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
    private val monthsShort = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    private val weekdays = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

    private fun plural(n: Number, one: String, many: String) = if (n.toLong() == 1L) "$n $one" else "$n $many"

    /** "1 year 3 months", "2 months", "45 days" */
    private fun amount(days: Long, years: Int, months: Int): String = when {
        days <= 60 -> plural(days, "day", "days")
        years > 0 && months > 0 -> "${plural(years, "year", "years")} ${plural(months, "month", "months")}"
        years > 0 -> plural(years, "year", "years")
        else -> plural(months, "month", "months")
    }

    /** "14-day return period", "2-year warranty" */
    private fun period(type: DurationType, totalYears: Int?, totalDays: Long): String {
        val length = if (totalYears != null) "$totalYears-year" else "$totalDays-day"
        return if (type == DurationType.IADE) "$length return period" else "$length warranty"
    }

    // Tarihler
    override fun dateShort(date: LocalDate): String = "${monthsShort[date.monthValue - 1]} ${date.dayOfMonth}, ${date.year}"
    override fun dateLong(date: LocalDate): String = "${weekday(date)}, ${months[date.monthValue - 1]} ${date.dayOfMonth}, ${date.year}"
    override fun dateNumeric(date: LocalDate): String = dateShort(date)
    override fun weekday(date: LocalDate): String = weekdays[date.dayOfWeek.value - 1]
    override fun monthShortUpper(date: LocalDate): String = monthsShort[date.monthValue - 1].uppercase(locale)
    override fun time(hour: Int, minute: Int): String = String.format(Locale.ROOT, "%02d:%02d", hour, minute)

    // Kalan süre
    override val expired: String = "Expired"
    override val lastDayToday: String = "Last day today"
    override val today: String = "Today"
    override fun remainingShort(days: Long, years: Int, months: Int): String = amount(days, years, months)
    override fun remaining(days: Long, years: Int, months: Int): String = "${amount(days, years, months)} left"
    override fun elapsedExpired(end: LocalDate): String = "Expired on ${dateLong(end)}."
    override fun elapsedNotStarted(start: LocalDate): String = "Starts on the delivery date (${dateShort(start)})."
    override fun elapsedProgress(type: DurationType, totalYears: Int?, totalDays: Long, elapsedDays: Long): String = if (elapsedDays == 0L) {
        "First day of the ${period(type, totalYears, totalDays)}."
    } else {
        "${plural(elapsedDays, "day", "days")} of the ${period(type, totalYears, totalDays)} have passed."
    }
    override val manualDateNote: String = " Date entered manually."

    // Ürün alanları
    override fun category(category: Category): String = when (category) {
        Category.ELEKTRONIK -> "Electronics"
        Category.BEYAZ_ESYA -> "Home appliances"
        Category.GIYIM -> "Clothing"
        Category.DIGER -> "Other"
    }
    override fun duration(type: DurationType): String = if (type == DurationType.IADE) "Return period" else "Warranty"
    override fun durationShort(type: DurationType): String = if (type == DurationType.IADE) "Return" else "Warranty"
    override fun durationEnd(type: DurationType): String = if (type == DurationType.IADE) "Last day to return" else "Warranty ends"
    override fun filter(filter: ProductFilter): String = when (filter) {
        ProductFilter.ALL -> "All"
        ProductFilter.UPCOMING -> "Upcoming"
        ProductFilter.ACTIVE -> "Active"
        ProductFilter.EXPIRED -> "Expired"
    }
    override fun fileSize(bytes: Long): String = when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> String.format(locale, "%.1f MB", bytes / (1024.0 * 1024.0))
    }

    // Ajanda
    override val agendaThisWeek: String = "This week"
    override val agendaNext30: String = "Next 30 days"
    override val agendaLater: String = "Later"

    // Bildirim metinleri
    override val notificationChannel: String = "Deadline reminders"
    override val notificationChannelDescription: String = "Reminds you before return and warranty periods end"
    override fun reminderDaysLeftTitle(type: DurationType): String = "${duration(type)} ending soon"
    override fun reminderDaysLeftBody(name: String, type: DurationType, days: Long): String = "${plural(days, "day", "days")} left on the ${duration(type).lowercase(locale)} for $name"
    override fun reminderDueTodayTitle(type: DurationType): String = "${duration(type)} ends today"
    override fun reminderDueTodayBody(name: String, type: DurationType): String = "The ${duration(type).lowercase(locale)} for $name ends today"
    override fun reminderExpiredTitle(type: DurationType): String = "${duration(type)} has ended"
    override fun reminderExpiredBody(name: String, type: DurationType): String = "The ${duration(type).lowercase(locale)} for $name has ended"

    // Gezinme
    override val tabHome: String = "Home"
    override val tabList: String = "My Warranties"
    override val tabAgenda: String = "Agenda & Reminders"
    override val tabProfile: String = "Profile & Settings"
    override val back: String = "Back"

    // Ana Sayfa
    override val homeSuggested: String = "Suggested Actions"
    override val actionAddTitle: String = "Add Warranty"
    override val actionAddBody: String = "Quickly save a new product and its receipt."
    override val actionReminderTitle: String = "Set Reminders"
    override val actionReminderBody: String = "Alerts for upcoming end dates"
    override val actionDocsTitle: String = "Organize Documents"
    override val actionDocsBody: String = "Filter all your records easily"
    override val homeToday: String = "Today's Warranties"
    override val seeAll: String = "See all"
    override val homeNothingUpcoming: String = "No upcoming or recently ended periods. All your products are in My Warranties."
    override val hello: String = "Hello!"
    override fun helloName(name: String): String = "Hello, $name!"
    override val tagline: String = "Stay on top of your warranties!"
    override val notifications: String = "Notifications"
    override val notificationsUrgent: String = "Notifications, urgent reminder"
    override val firstProduct: String = "Add your first product"
    override val legalNoticePrefix: String = "By using Garantiledim you agree to the "
    override val legalNoticeMiddle: String = ". Your information stays only on this phone; see the "
    override val legalNoticeSuffix: String = " for details."

    // Garanti Belgelerim
    override fun productsTracked(count: Int): String = if (count == 1) "1 product tracked" else "$count products tracked"
    override val searchHint: String = "Search product or store"
    override val noProductsTitle: String = "No products yet"
    override val noProductsBody: String = "Tap + at the bottom right to add your first product and receipt."
    override val noMatchTitle: String = "No results"
    override val noMatchBody: String = "No products match this filter."
    override val expiredSection: String = "Expired"
    override val addWarrantyA11y: String = "Add warranty"

    // Ajanda
    override fun reminderSummary(leadDays: Int, time: String): String = "Starts $leadDays ${if (leadDays == 1) "day" else "days"} before the end date, every day at $time"
    override val notificationsOff: String = "Notifications are off, you won't get reminders"
    override val openSettings: String = "Open settings"
    override val reminderSetting: String = "Reminder settings"
    override val adjust: String = "Adjust"
    override val agendaEmptyTitle: String = "No upcoming end dates"
    override val agendaEmptyBody: String = "End dates of your active products are listed here."

    // Yasal
    override val privacyPolicy: String = "Privacy Policy"
    override val termsOfUse: String = "Terms of Use"
    override val legalLoadErrorTitle: String = "Couldn't open the document"
    override fun legalLoadErrorBody(email: String): String = "Please update the app or write to $email."
    override fun askQuestion(email: String): String = "Ask a question: $email"

    // Ortak
    override val ok: String = "OK"
    override val cancel: String = "Cancel"
    override val close: String = "Close"
    override val save: String = "Save"
    override fun selectA11y(label: String): String = "Select $label"
    override val notificationTime: String = "Notification time"
    override val fileTypePdf: String = "PDF"
    override val fileTypeFile: String = "FILE"
    override val fileTypeImage: String = "Image"
    override val original: String = "original"
    override val removeReceiptA11y: String = "Remove receipt"

    // Ürün Detayı
    override val detailTitle: String = "Product Details"
    override val edit: String = "Edit"
    override val productNotFoundTitle: String = "Product not found"
    override val productNotFoundBody: String = "This product may have been deleted."
    override val purchaseDate: String = "Purchased"
    override val deliveryDate: String = "Delivered"
    override val receiptSaved: String = "Receipt saved to Downloads"
    override val receiptSaveFailed: String = "Couldn't save the file"
    override val storagePermissionNeeded: String = "Storage permission is needed to download"
    override val noAppToOpen: String = "No app found to open this file"
    override val addReceipt: String = "Add receipt / invoice"
    override val lockedTitle: String = "Reminders are off for this product"
    override fun lockedBody(limit: Int): String = "In the free version, reminders work for the first $limit products."
    override val premium: String = "Premium"
    override val openReceiptA11y: String = "Open receipt"
    override val receiptMissing: String = "File is not on this phone, you can add it again"
    override val add: String = "Add"
    override val download: String = "Download"

    // Ürün ekle / düzenle
    override val filePickerMissing: String = "No file picker found"
    override val galleryMissing: String = "No gallery app found"
    override val cameraMissing: String = "No camera app found"
    override val editTitle: String = "Edit Product"
    override val productName: String = "Product name *"
    override val store: String = "Store"
    override val purchaseDateField: String = "Purchase date"
    override val deliveryDateField: String = "Delivery date"
    override val categoryField: String = "Category"
    override val trackedPeriods: String = "Periods to track"
    override val periodsHelp: String = "If you bought in store, you can turn off the return period or enter the store's own period. Change the warranty date if you have an extended warranty."
    override val receiptField: String = "Receipt / invoice"
    override val fromGallery: String = "From gallery / files"
    override val takePhoto: String = "Take a photo"
    override val receiptHelp: String = "You can add a photo or a PDF invoice. The original file is kept unchanged."
    override val addProductPhoto: String = "Add product photo (optional)"
    override val chooseFromGallery: String = "Choose from gallery"
    override val productPhoto: String = "Product photo"
    override val removePhotoA11y: String = "Remove photo"
    override val deleteProduct: String = "Delete product"
    override val unsavedTitle: String = "Changes not saved"
    override val unsavedBody: String = "If you leave, the information and files you added will be lost."
    override val leave: String = "Leave"
    override val deleteTitle: String = "Delete product?"
    override fun deleteBody(name: String): String = "\"$name\" and its receipt will be permanently deleted."
    override val delete: String = "Delete"
    override val enteredManually: String = "Entered manually · "
    override val reset: String = "Reset"
    override fun periodDefault(type: DurationType): String = if (type == DurationType.IADE) "14 days from delivery" else "2 years from delivery"
    override val errorProductNotFound: String = "Product not found"
    override val errorNameEmpty: String = "Product name can't be empty"
    override val errorDeliveryBeforePurchase: String = "Delivery date can't be before the purchase date"
    override val errorNoPeriod: String = "Track at least one period"
    override val errorEndBeforeDelivery: String = "End date can't be before the delivery date"
    override fun errorFreeLimit(limit: Int): String = "The free version allows up to $limit products"
    override val errorSaveFailed: String = "Couldn't save, please try again"
    override val errorReceiptAdd: String = "Couldn't add the receipt"
    override val errorPhotoAdd: String = "Couldn't add the photo"

    // Profil & Ayarlar
    override fun backupSaved(count: Int): String = "Backup saved ($count ${if (count == 1) "product" else "products"})"
    override fun backupRestored(count: Int): String = "Backup restored ($count ${if (count == 1) "product" else "products"})"
    override val backupNeedsPremium: String = "Backup is part of Premium"
    override val backupCorrupt: String = "The backup file is damaged"
    override val backupNotOurs: String = "This file is not a Garantiledim backup"
    override val backupTooNew: String = "This backup was made with a newer version; update the app first"
    override val operationFailed: String = "Couldn't complete, please try again"
    override val choosePhotoA11y: String = "Choose photo"
    override val addYourName: String = "Add your name"
    override val storedOnDevice: String = "Your information is stored only on this device"
    override val editNameA11y: String = "Edit name"
    override val removeProfilePhoto: String = "Remove profile photo"
    override val premiumActiveSummary: String = "Unlimited products and backup"
    override val active: String = "Active"
    override val goPremium: String = "Go Premium"
    override val goPremiumSummary: String = "Unlimited products, backup · one-time payment"
    override val backupSection: String = "Backup"
    override val backupAction: String = "Back up"
    override val processing: String = "Working…"
    override val backupSummary: String = "Products, receipts and photos in one file"
    override val backupFilePrefix: String = "garantiledim-backup"
    override val saveScreenMissing: String = "No file saving app found"
    override val restoreAction: String = "Restore from backup"
    override val restoreSummary: String = "When moving to a new phone"
    override val remindersSection: String = "Reminders"
    override val reminderStart: String = "Reminder start"
    override fun daysBeforeEnd(days: Int): String = "${if (days == 1) "1 day" else "$days days"} before the end date"
    override fun everyDayAt(time: String): String = "Every day at $time"
    override val notificationPermission: String = "Notification permission"
    override val permissionOn: String = "On"
    override val permissionOffTap: String = "Off · tap to turn on"
    override val defaultPeriods: String = "Default periods"
    override val days14: String = "14 days"
    override val years2: String = "2 years"
    override val legalSection: String = "Legal"
    override val otherSection: String = "Other"
    override val contactUs: String = "Contact us"
    override val about: String = "About"
    override fun version(name: String): String = "Version $name"
    override val restoreConfirmTitle: String = "Restore from backup?"
    override val restoreConfirmBody: String = "Products on this phone will be replaced with the ones in the backup you choose. Tap Cancel if you want to back up your current data first."
    override val chooseBackup: String = "Choose backup"
    override val yourName: String = "Your name"
    override val nameHint: String = "Shown on the home screen"
    override val languageSection: String = "Language"
    override val appLanguage: String = "App language"
    override val languageSystem: String = "System language"

    // Premium
    override val premiumTitle: String = "Garantiledim Premium"
    override val premiumThanks: String = "Premium is active. Thank you for your support!"
    override val benefitUnlimited: String = "Unlimited products"
    override val benefitUnlimitedBody: String = "Add as many products and receipts as you like"
    override val benefitReminders: String = "Reminders for every product"
    override val benefitRemindersBody: String = "Notifications for all return and warranty periods"
    override val benefitBackup: String = "Back up and move"
    override val benefitBackupBody: String = "Your products, receipts and photos in one file; restore on a new phone"
    override val benefitOneTime: String = "One-time payment"
    override val benefitOneTimeBody: String = "No subscription. Pay once, yours forever"
    override fun freeTierNote(limit: Int): String = "The free version includes all features (except backup) for up to $limit products."
    override val continueLabel: String = "Continue"
    override fun buyPremium(price: String): String = "Get Premium · $price"
    override val retry: String = "Try again"
    override val playUnavailable: String = "Couldn't connect to Google Play. Check your internet connection and Play Store account."
    override val oneTimeDisclosure: String = "One-time payment · Not a subscription, does not renew. Charged to your Google Play account."
    override val restorePurchase: String = "Restore purchase"
    override fun paywallLimit(limit: Int): String = "The free version tracks up to $limit products. Premium removes the limit."
    override val paywallBackup: String = "Backup is part of Premium. Move your products and receipts safely to a new phone."
    override val paywallGeneral: String = "All your return and warranty periods and receipts in one place. Unlimited."
    override val playRetryLater: String = "Couldn't connect to Google Play, try again later"
    override val playConnecting: String = "Connecting to Google Play, try again in a moment"
    override val purchaseFailed: String = "Purchase couldn't be completed, please try again"
    override val purchaseNotFound: String = "No Premium purchase found on this Google account"
    override val purchaseRestored: String = "Premium restored"
    override val purchaseNotVerified: String = "The purchase couldn't be verified"
    override val purchasePending: String = "Payment is pending; Premium unlocks automatically once it completes"

    // Dosya adları
    override val receiptFilePrefix: String = "receipt"
    override val cameraReceiptPrefix: String = "receipt-photo"
}

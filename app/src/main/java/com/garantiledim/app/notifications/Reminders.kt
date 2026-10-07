package com.garantiledim.app.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.garantiledim.app.GarantiledimApp
import com.garantiledim.app.MainActivity
import com.garantiledim.app.R
import com.garantiledim.app.data.ProductEntity
import com.garantiledim.app.data.toDomain
import com.garantiledim.app.domain.DurationType
import com.garantiledim.app.domain.ReminderAction
import com.garantiledim.app.domain.deadline
import com.garantiledim.app.domain.reminderAction
import com.garantiledim.app.domain.text
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

private const val CHANNEL_ID = "hatirlatmalar"
private const val DAILY_WORK = "gunluk-kontrol"
private const val CATCH_UP_WORK = "kacirilan-kontrol"
const val EXTRA_PRODUCT_ID = "productId"

object Reminders {

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Süre hatırlatmaları",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = "İade ve garanti süreleri dolmadan hatırlatır" }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun canNotify(context: Context): Boolean {
        val permitted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        return permitted && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    /**
     * Günlük kontrolü ayarlardaki saate göre (yeniden) zamanlar. O günün saati geçtiyse
     * kaçırılan kontrol hemen bir kez çalıştırılır; aynı gün tekrar bildirim gönderilmez.
     */
    fun schedule(context: Context, hour: Int, minute: Int) {
        val now = LocalDateTime.now()
        val todayAt = LocalDate.now().atTime(LocalTime.of(hour, minute))
        val next = if (now.isBefore(todayAt)) todayAt else todayAt.plusDays(1)
        val delay = Duration.between(now, next).toMillis()

        val work = WorkManager.getInstance(context)
        val daily = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .build()
        work.enqueueUniquePeriodicWork(DAILY_WORK, ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE, daily)

        if (!now.isBefore(todayAt)) {
            work.enqueueUniqueWork(
                CATCH_UP_WORK,
                ExistingWorkPolicy.KEEP,
                OneTimeWorkRequestBuilder<ReminderWorker>().build(),
            )
        }
    }

    fun show(context: Context, product: ProductEntity, type: DurationType, action: ReminderAction) {
        if (!canNotify(context)) return
        val text = action.text(product.name, type)
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(EXTRA_PRODUCT_ID, product.id)
        val notificationId = (product.id * 2 + type.ordinal).toInt()
        val pending = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(0xFFFF7AB8.toInt())
            .setContentTitle(text.title)
            .setContentText(text.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text.body))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (e: SecurityException) {
            // İzin bu arada geri alındı; bildirim gönderilmez.
        }
    }
}

/** Tüm ürünlerin sürelerini tek seferde kontrol eden günlük iş (SPEC.md madde 7). */
class ReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as GarantiledimApp).container
        val dao = container.database.productDao()
        val settings = container.settings.settings.first()
        val today = LocalDate.now()

        for (entity in dao.getAll()) {
            val product = entity.toDomain()
            var updated = entity
            for (type in DurationType.entries) {
                val deadline = product.deadline(type) ?: continue
                val (lastReminder, expiredNotified) = when (type) {
                    DurationType.IADE -> entity.returnLastReminder to entity.returnExpiredNotified
                    DurationType.GARANTI -> entity.warrantyLastReminder to entity.warrantyExpiredNotified
                }
                val action = reminderAction(
                    endDate = deadline.endDate,
                    today = today,
                    leadDays = settings.reminderLeadDays,
                    lastReminder = lastReminder,
                    expiredNotified = expiredNotified,
                ) ?: continue

                if (action !is ReminderAction.Expired || action.notify) {
                    Reminders.show(applicationContext, entity, type, action)
                }
                updated = when (type) {
                    DurationType.IADE -> if (action is ReminderAction.Expired) {
                        updated.copy(returnExpiredNotified = true)
                    } else {
                        updated.copy(returnLastReminder = today)
                    }
                    DurationType.GARANTI -> if (action is ReminderAction.Expired) {
                        updated.copy(warrantyExpiredNotified = true)
                    } else {
                        updated.copy(warrantyLastReminder = today)
                    }
                }
            }
            if (updated != entity) dao.update(updated)
        }
        return Result.success()
    }
}

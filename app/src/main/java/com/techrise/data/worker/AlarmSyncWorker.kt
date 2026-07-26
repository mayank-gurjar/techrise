package com.techrise.data.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.techrise.data.repository.TechRiseRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class AlarmSyncWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: TechRiseRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        // 1. Only proceed if an employee/admin is logged in on this device
        val role = repository.getRole()
        if (role?.uppercase() != "ADMIN") {
            return Result.success()
        }

        val prefs = context.getSharedPreferences("techrise_alarm_prefs", Context.MODE_PRIVATE)
        val seenComplaints = prefs.getStringSet("seen_complaints", emptySet())?.toMutableSet() ?: mutableSetOf()
        val escalatedComplaints = prefs.getStringSet("escalated_complaints", emptySet())?.toMutableSet() ?: mutableSetOf()

        var alarmTriggered = false

        // 2. Fetch all complaints
        repository.getComplaints().onSuccess { complaints ->
            val twentyFourHoursInSeconds = 24 * 60 * 60
            val currentSeconds = System.currentTimeMillis() / 1000

            complaints.forEach { complaint ->
                val id = complaint.id
                val isResolved = complaint.status.uppercase() == "RESOLVED"

                // Check A: New Complaint Alarm (only if not resolved and not seen before)
                if (!isResolved && !seenComplaints.contains(id)) {
                    triggerNewComplaintAlarm(complaint.title, id)
                    seenComplaints.add(id)
                    alarmTriggered = true
                }

                // Check B: 24-Hour Unresolved Alarm (only if not resolved, older than 24 hours, and not escalated before)
                val ageSeconds = complaint.createdAt?.let { currentSeconds - it._seconds } ?: 0
                if (!isResolved && ageSeconds > twentyFourHoursInSeconds && !escalatedComplaints.contains(id)) {
                    triggerEscalationAlarm(complaint.title, id)
                    escalatedComplaints.add(id)
                    alarmTriggered = true
                }
            }

            // Save updated sets to SharedPreferences
            prefs.edit()
                .putStringSet("seen_complaints", seenComplaints)
                .putStringSet("escalated_complaints", escalatedComplaints)
                .apply()

            if (alarmTriggered) {
                // Play alarming sound for 8 seconds
                playAlarmSound(context, 8000)
            }
        }.onFailure {
            return Result.retry()
        }

        return Result.success()
    }

    private fun playAlarmSound(context: Context, durationMs: Long) {
        try {
            val alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            val ringtone = RingtoneManager.getRingtone(context, alertUri)
            if (ringtone != null) {
                // Play the sound on the main thread
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    ringtone.play()
                    // Stop after the duration (8 seconds)
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        if (ringtone.isPlaying) {
                            ringtone.stop()
                        }
                    }, durationMs)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun triggerNewComplaintAlarm(complaintTitle: String, complaintId: String) {
        val channelId = "techrise_new_complaints"
        val notificationId = complaintId.hashCode()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create Channel for Android 8.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Tech Rise New Complaints"
            val descriptionText = "Loud alarm warnings for newly received complaints"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
                enableLights(true)
                lightColor = android.graphics.Color.GREEN
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("🚨 NEW COMPLAINT RECEIVED")
            .setContentText("New Ticket $complaintId: '$complaintTitle'")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)

        notificationManager.notify(notificationId, builder.build())
    }

    private fun triggerEscalationAlarm(complaintTitle: String, complaintId: String) {
        val channelId = "techrise_escalation_alarms"
        val notificationId = complaintId.hashCode() + 1 // Offset to avoid collisions

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create Channel for Android 8.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Tech Rise Escalation Alarms"
            val descriptionText = "Loud alarm warnings for 24-hour stagnant complaints"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
                enableLights(true)
                lightColor = android.graphics.Color.RED
                enableVibration(true)
                vibrationPattern = longArrayOf(100, 400, 100, 400, 100, 400, 500, 1000)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("⚠️ 24-HOUR UNRESOLVED ALARM")
            .setContentText("Complaint $complaintId: '$complaintTitle' is unresolved for over 24 hours!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)

        notificationManager.notify(notificationId, builder.build())
    }
}

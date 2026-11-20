package com.example.groceryapp

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/**
 * "Order is on the way" notification, shown 30 seconds after the buyer places an order
 * (short delay for easy testing). Uses WorkManager so it still arrives if the app is closed.
 * Cancelling the order cancels the pending notification.
 */
object OrderNotifier {

    private const val CHANNEL_ID = "orders"
    private const val DELAY_SECONDS = 30L

    private fun workName(orderId: String) = "order_on_the_way_$orderId"

    fun scheduleOnTheWay(context: Context, orderId: String) {
        val request = OneTimeWorkRequestBuilder<OnTheWayWorker>()
            .setInitialDelay(DELAY_SECONDS, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(workName(orderId), androidx.work.ExistingWorkPolicy.REPLACE, request)
    }

    fun cancelOnTheWay(context: Context, orderId: String) {
        WorkManager.getInstance(context).cancelUniqueWork(workName(orderId))
    }

    fun show(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Orders", NotificationManager.IMPORTANCE_HIGH)
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return // the buyer didn't allow notifications

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_basket)
            .setContentTitle("Grocery App")
            .setContentText("Order is on the way")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(System.currentTimeMillis().toInt(), notification)
    }

    class OnTheWayWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
        override fun doWork(): Result {
            show(applicationContext)
            return Result.success()
        }
    }
}

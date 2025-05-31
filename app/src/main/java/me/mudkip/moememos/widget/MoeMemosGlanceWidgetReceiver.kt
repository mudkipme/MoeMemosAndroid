package me.mudkip.moememos.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

class MoeMemosGlanceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MoeMemosGlanceWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        // Schedule periodic updates when the first widget is added
        scheduleWidgetUpdates(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        // Cancel updates when the last widget is removed
        cancelWidgetUpdates(context)
    }

    companion object {
        private const val WIDGET_UPDATE_WORK = "moe_memos_widget_update_work"

        fun scheduleWidgetUpdates(context: Context) {
            val updateRequest = PeriodicWorkRequestBuilder<WidgetUpdateWorker>(
                30, TimeUnit.MINUTES
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WIDGET_UPDATE_WORK,
                ExistingPeriodicWorkPolicy.KEEP,
                updateRequest
            )
        }

        fun cancelWidgetUpdates(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WIDGET_UPDATE_WORK)
        }
    }

    /**
     * Worker class to update the widget periodically
     */
    class WidgetUpdateWorker(
        private val appContext: Context,
        workerParams: WorkerParameters
    ) : CoroutineWorker(appContext, workerParams) {
        override suspend fun doWork(): Result {
            // Update all instances of the widget
            val manager = GlanceAppWidgetManager(appContext)
            val glanceIds = manager.getGlanceIds(MoeMemosGlanceWidget::class.java)
            glanceIds.forEach { glanceId ->
                MoeMemosGlanceWidget().update(appContext, glanceId)
            }
            return Result.success()
        }
    }
}

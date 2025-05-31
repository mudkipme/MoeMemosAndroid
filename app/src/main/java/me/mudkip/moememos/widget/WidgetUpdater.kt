package me.mudkip.moememos.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Utility class to update all widget instances when memos are changed
 */
object WidgetUpdater {
    /**
     * Update all widget instances
     */
    fun updateWidgets(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val manager = GlanceAppWidgetManager(context)
            val glanceIds = manager.getGlanceIds(MoeMemosGlanceWidget::class.java)
            
            // Only update if there are widgets
            if (glanceIds.isNotEmpty()) {
                glanceIds.forEach { glanceId ->
                    MoeMemosGlanceWidget().update(context, glanceId)
                }
            }
        }
    }
}

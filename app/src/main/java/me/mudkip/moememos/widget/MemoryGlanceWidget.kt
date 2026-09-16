package me.mudkip.moememos.widget

import android.content.Context
import android.content.Intent
import android.text.format.DateUtils
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.skydoves.sandwich.suspendOnSuccess
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import me.mudkip.moememos.MainActivity
import me.mudkip.moememos.R
import me.mudkip.moememos.data.local.entity.MemoEntity
import me.mudkip.moememos.data.model.MemoEditGesture
import me.mudkip.moememos.data.service.MemoService
import me.mudkip.moememos.ext.settingsDataStore
import timber.log.Timber
import java.time.Instant

class MemoryGlanceWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val widgetEntryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            WidgetEntryPoint::class.java
        )
        val memoService = widgetEntryPoint.memoService()
        val settings = context.settingsDataStore.data.first()
        val openInEditor = settings.usersList
            .firstOrNull { it.accountKey == settings.currentUser }
            ?.settings?.editGesture == MemoEditGesture.SINGLE

        provideContent(createContent(context, memoService, openInEditor))
    }

    // Keep composable lambda captures outside the coroutine state machine so Compose
    // can collect stack trace mappings for the widget content.
    private fun createContent(
        context: Context,
        memoService: MemoService,
        openInEditor: Boolean
    ): @Composable () -> Unit = {
        GlanceTheme {
            WidgetContent(context, memoService, openInEditor)
        }
    }

    @Composable
    private fun WidgetContent(context: Context, memoService: MemoService, openInEditor: Boolean) {
        var memo by remember { mutableStateOf<MemoEntity?>(null) }
        var isLoading by remember { mutableStateOf(true) }
        var error by remember { mutableStateOf<String?>(null) }

        LaunchedEffect(Unit) {
            withContext(Dispatchers.IO) {
                try {
                    memoService.getRepository().listMemos().suspendOnSuccess {
                        memo = data.shuffled().firstOrNull()
                        error = null
                    }
                } catch (e: Exception) {
                    error = e.message ?: "Unknown error"
                    Timber.tag("MemoryWidget").e(e, "Exception in memory widget")
                } finally {
                    isLoading = false
                }
            }
        }

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.background)
                .clickable(actionStartActivity(createOpenAppIntent(context)))
                .padding(12.dp)
        ) {
            when {
                isLoading -> {
                    Box(
                        modifier = GlanceModifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = context.getString(R.string.loading),
                            style = TextStyle(color = GlanceTheme.colors.onBackground)
                        )
                    }
                }
                error != null -> {
                    Box(
                        modifier = GlanceModifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = error ?: context.getString(R.string.error_unknown),
                            style = TextStyle(color = GlanceTheme.colors.error)
                        )
                    }
                }
                memo == null -> {
                    Box(
                        modifier = GlanceModifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = context.getString(R.string.no_memos),
                            style = TextStyle(color = GlanceTheme.colors.onBackground)
                        )
                    }
                }
                else -> {
                    val loadedMemo = memo ?: return@Column
                    Column(
                        modifier = GlanceModifier
                            .fillMaxSize()
                            .clickable(actionStartActivity(createMemoIntent(context, loadedMemo.identifier, openInEditor)))
                    ) {
                        Text(
                            text = DateUtils.getRelativeTimeSpanString(
                                loadedMemo.date.toEpochMilli(),
                                Instant.now().toEpochMilli(),
                                DateUtils.MINUTE_IN_MILLIS
                            ).toString(),
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        )
                        Spacer(modifier = GlanceModifier.height(8.dp))
                        Text(
                            text = loadedMemo.content.take(560) +
                                    if (loadedMemo.content.length > 560) "..." else "",
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurface,
                                fontSize = 14.sp
                            ),
                            maxLines = 20
                        )
                    }
                }
            }
        }
    }
}

private fun createOpenAppIntent(context: Context): Intent =
    Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }

private fun createMemoIntent(context: Context, memoId: String, openInEditor: Boolean): Intent =
    Intent(context, MainActivity::class.java).apply {
        action = if (openInEditor) MainActivity.ACTION_EDIT_MEMO else MainActivity.ACTION_VIEW_MEMO
        putExtra(MainActivity.EXTRA_MEMO_ID, memoId)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }

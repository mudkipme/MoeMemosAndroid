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
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.itemsIndexed
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.skydoves.sandwich.suspendOnSuccess
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.mudkip.moememos.MainActivity
import me.mudkip.moememos.R
import me.mudkip.moememos.data.local.entity.MemoEntity
import me.mudkip.moememos.data.model.MemoVisibility
import me.mudkip.moememos.data.service.MemoService
import java.time.Instant

class MoeMemosGlanceWidget : GlanceAppWidget() {

    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val widgetEntryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            WidgetEntryPoint::class.java
        )
        val memoService = widgetEntryPoint.memoService()

        provideContent {
            val prefs = currentState<Preferences>()
            GlanceTheme {
                WidgetContent(context, memoService, prefs)
            }
        }
    }

    @Composable
    private fun WidgetContent(context: Context, memoService: MemoService, prefs: Preferences) {
        var memos by remember { mutableStateOf<List<MemoEntity>>(emptyList()) }
        var isLoading by remember { mutableStateOf(true) }
        var error by remember { mutableStateOf<String?>(null) }

        val filterTag = prefs[MoeMemosWidgetKeys.filterTag]
        val pinnedOnly = prefs[MoeMemosWidgetKeys.pinnedOnly] ?: false
        val maxItems = prefs[MoeMemosWidgetKeys.maxItems] ?: 10
        val refreshKey = prefs[MoeMemosWidgetKeys.refreshKey] ?: 0L

        LaunchedEffect(filterTag, pinnedOnly, maxItems, refreshKey) {
            withContext(Dispatchers.IO) {
                try {
                    isLoading = true
                    memoService.getRepository().listMemos().suspendOnSuccess {
                        // Filter and sort memos
                        val filteredMemos = data.filter { memo ->
                            val matchesTag = filterTag == null || memo.content.contains("#$filterTag")
                            val matchesPinned = !pinnedOnly || memo.pinned
                            matchesTag && matchesPinned
                        }
                        
                        val sortedMemos = filteredMemos.sortedWith(
                            compareByDescending<MemoEntity> { it.pinned }
                                .thenByDescending { it.date }
                        ).take(maxItems)
                        
                        memos = sortedMemos
                        error = null
                    }
                } catch (e: Exception) {
                    error = e.message ?: "Unknown error"
                    android.util.Log.e("MoeMemosWidget", "Exception in widget", e)
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
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // App icon
                Image(
                    provider = ImageProvider(R.mipmap.ic_launcher),
                    contentDescription = null,
                    modifier = GlanceModifier.size(24.dp)
                )
                Spacer(modifier = GlanceModifier.width(8.dp))
                Column {
                    Text(
                        text = context.getString(R.string.memos),
                        style = TextStyle(
                            color = GlanceTheme.colors.primary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    if (filterTag != null) {
                        Text(
                            text = "#$filterTag",
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        )
                    }
                }
                Spacer(modifier = GlanceModifier.defaultWeight())
                // Refresh button
                Box(
                    modifier = GlanceModifier
                        .size(36.dp)
                        .clickable(actionRunCallback<RefreshAction>())
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_shortcut_refresh),
                        contentDescription = context.getString(R.string.refresh),
                        modifier = GlanceModifier.size(24.dp)
                    )
                }
                Spacer(modifier = GlanceModifier.width(8.dp))
                // Add new memo button
                Box(
                    modifier = GlanceModifier
                        .size(36.dp)
                        .clickable(actionStartActivity(createNewMemoIntent(context)))
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_shortcut_add),
                        contentDescription = context.getString(R.string.edit),
                        modifier = GlanceModifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = GlanceModifier.height(8.dp))

            // Content
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
                memos.isEmpty() -> {
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
                    LazyColumn(
                        modifier = GlanceModifier.fillMaxSize()
                    ) {
                        itemsIndexed(memos) { index, memo ->
                            val isLastMemo = index == memos.size - 1

                            MemoItem(context, memo, isLastMemo)

                            if (!isLastMemo) {
                                Spacer(modifier = GlanceModifier.height(2.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun MemoItem(context: Context, memo: MemoEntity, isLastMemo: Boolean = false) {
        // Card-like container with rounded corners
        Box(
            modifier = GlanceModifier
                .fillMaxWidth()
                .padding(2.dp, 4.dp, 2.dp, if (isLastMemo) 0.dp else 4.dp)
        ) {
            // Card content with rounded corners and borders (via XML drawables)
            Column(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .background(
                        ImageProvider(
                            if (memo.pinned) R.drawable.widget_card_pinned_background 
                            else R.drawable.widget_card_background
                        )
                    )
                    .clickable(actionStartActivity(createViewMemoIntent(context, memo.identifier)))
                    .padding(12.dp, 12.dp, 12.dp, if (isLastMemo) 8.dp else 12.dp)
            ) {
                // Memo header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = GlanceModifier.fillMaxWidth()
                ) {
                    // Date
                    Text(
                        text = DateUtils.getRelativeTimeSpanString(
                            memo.date.toEpochMilli(),
                            Instant.now().toEpochMilli(),
                            DateUtils.MINUTE_IN_MILLIS
                        ).toString(),
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    )
                    
                    if (memo.visibility != MemoVisibility.PUBLIC){
                        Spacer(modifier = GlanceModifier.width(4.dp))
                        Image(
                            provider = ImageProvider(android.R.drawable.ic_lock_lock),
                            contentDescription = "Private",
                            modifier = GlanceModifier.size(14.dp)
                        )
                    }
                    
                    // Pinned indicator
                    if (memo.pinned) {
                        Spacer(modifier = GlanceModifier.width(4.dp))
                        Image(
                            provider = ImageProvider(R.drawable.ic_pin),
                            contentDescription = "Pinned",
                            modifier = GlanceModifier.size(14.dp),
                            colorFilter = ColorFilter.tint(GlanceTheme.colors.onSurfaceVariant)
                        )
                    }
                    
                    Spacer(modifier = GlanceModifier.defaultWeight())
                }
                
                Spacer(modifier = GlanceModifier.height(if (isLastMemo) 4.dp else 8.dp))
                
                // Memo content
                Text(
                    text = memo.content.take(if (isLastMemo) 80 else 100) + 
                           if (memo.content.length > (if (isLastMemo) 80 else 100)) "..." else "",
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontSize = 14.sp
                    ),
                    maxLines = if (isLastMemo) 2 else 3
                )
            }
        }
    }
}

class RefreshAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        updateAppWidgetState(context, glanceId) { prefs ->
            val current = prefs[MoeMemosWidgetKeys.refreshKey] ?: 0L
            prefs[MoeMemosWidgetKeys.refreshKey] = current + 1
        }
        MoeMemosGlanceWidget().update(context, glanceId)
    }
}

object MoeMemosWidgetKeys {
    val filterTag = stringPreferencesKey("filter_tag")
    val pinnedOnly = booleanPreferencesKey("pinned_only")
    val maxItems = intPreferencesKey("max_items")
    val refreshKey = longPreferencesKey("refresh_key")
}

private fun createOpenAppIntent(context: Context): Intent =
    Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }

private fun createNewMemoIntent(context: Context): Intent =
    Intent(context, MainActivity::class.java).apply {
        action = MainActivity.ACTION_NEW_MEMO
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }


private fun createViewMemoIntent(context: Context, memoId: String): Intent =
    Intent(context, MainActivity::class.java).apply {
        action = MainActivity.ACTION_VIEW_MEMO
        putExtra(MainActivity.EXTRA_MEMO_ID, memoId)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }

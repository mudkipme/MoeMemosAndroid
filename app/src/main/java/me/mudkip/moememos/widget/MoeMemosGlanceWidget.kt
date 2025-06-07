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
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.ColorFilter
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.background
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
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.skydoves.sandwich.ApiResponse
import com.skydoves.sandwich.suspendOnSuccess
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.mudkip.moememos.MainActivity
import me.mudkip.moememos.R
import me.mudkip.moememos.data.model.Memo
import me.mudkip.moememos.data.model.MemoVisibility
import me.mudkip.moememos.data.service.MemoService
import me.mudkip.moememos.ext.string
import java.time.Instant

class MoeMemosGlanceWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val widgetEntryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            WidgetEntryPoint::class.java
        )
        val memoService = widgetEntryPoint.memoService()
        
        provideContent {
            GlanceTheme {
                WidgetContent(context, memoService)
            }
        }
    }

    @Composable
    private fun WidgetContent(context: Context, memoService: MemoService) {
        var memos by remember { mutableStateOf<List<Memo>>(emptyList()) }
        var isLoading by remember { mutableStateOf(true) }
        var error by remember { mutableStateOf<String?>(null) }

        LaunchedEffect(Unit) {
            withContext(Dispatchers.IO) {
                try {
                    memoService.repository.listMemos().suspendOnSuccess {
                        // Get pinned memos first, then most recent
                        val sortedMemos = data.sortedWith(
                            compareByDescending<Memo> { it.pinned }
                                .thenByDescending { it.date }
                        ).take(3)
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
                .clickable(actionRunCallback<OpenAppAction>())
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
                Text(
                    text = context.getString(R.string.memos),
                    style = TextStyle(
                        color = GlanceTheme.colors.primary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = GlanceModifier.defaultWeight())
                // Add new memo button
                Box(
                    modifier = GlanceModifier
                        .size(36.dp)
                        .clickable(actionRunCallback<AddNewMemoAction>())
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
                    Column(
                        modifier = GlanceModifier.fillMaxWidth(),
                    ) {
                        memos.forEachIndexed { index, memo ->
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
    private fun MemoItem(context: Context, memo: Memo, isLastMemo: Boolean = false) {
        // Card-like container with rounded corners
        Box(
            modifier = GlanceModifier
                .fillMaxWidth()
                .padding(2.dp, 4.dp, 2.dp, if (isLastMemo) 0.dp else 4.dp)
        ) {
            // Card content with rounded corners
            Column(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .background(
                        ImageProvider(
                            if (memo.pinned) R.drawable.widget_card_pinned_background 
                            else R.drawable.widget_card_background
                        )
                    )
                    .clickable(actionRunCallback<OpenMemoAction>(
                        actionParametersOf(
                            OpenMemoAction.MEMO_ID to memo.identifier
                        )
                    ))
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

class AddNewMemoAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = MainActivity.ACTION_NEW_MEMO
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        context.startActivity(intent)
    }
}

class OpenAppAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        context.startActivity(intent)
    }
}

class OpenMemoAction : ActionCallback {
    companion object {
        val MEMO_ID = ActionParameters.Key<String>("memo_id")
    }

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val memoId = parameters[MEMO_ID] ?: return
        val intent = Intent(context, MainActivity::class.java).apply {
            action = MainActivity.ACTION_EDIT_MEMO
            putExtra(MainActivity.EXTRA_MEMO_ID, memoId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        context.startActivity(intent)
    }
}

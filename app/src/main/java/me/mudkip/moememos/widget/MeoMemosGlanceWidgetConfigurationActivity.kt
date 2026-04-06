package me.mudkip.moememos.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.lifecycle.lifecycleScope
import com.skydoves.sandwich.suspendOnSuccess
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import me.mudkip.moememos.R
import me.mudkip.moememos.data.service.MemoService
import me.mudkip.moememos.ui.theme.MoeMemosTheme
import javax.inject.Inject

@AndroidEntryPoint
class MeoMemosGlanceWidgetConfigurationActivity : ComponentActivity() {

    @Inject
    lateinit var memoService: MemoService

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        val resultValue = Intent().apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        setResult(RESULT_CANCELED, resultValue)

        setContent {
            MoeMemosTheme {
                ConfigurationScreen()
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
    @Composable
    fun ConfigurationScreen() {
        var selectedTag by remember { mutableStateOf<String?>(null) }
        var pinnedOnly by remember { mutableStateOf(false) }
        var maxItems by remember { mutableFloatStateOf(10f) }
        var tags by remember { mutableStateOf<List<String>>(emptyList()) }
        var isLoading by remember { mutableStateOf(true) }

        LaunchedEffect(Unit) {
            try {
                // Fetch tags
                val result = memoService.getRepository().listTags()
                result.suspendOnSuccess {
                    tags = data
                }

                // Fetch current preferences
                val glanceId = GlanceAppWidgetManager(this@MeoMemosGlanceWidgetConfigurationActivity)
                    .getGlanceIdBy(appWidgetId)
                val prefs = getAppWidgetState(
                    context = this@MeoMemosGlanceWidgetConfigurationActivity,
                    definition = PreferencesGlanceStateDefinition,
                    glanceId = glanceId
                )
                
                selectedTag = prefs[MoeMemosWidgetKeys.filterTag]
                pinnedOnly = prefs[MoeMemosWidgetKeys.pinnedOnly] ?: false
                maxItems = (prefs[MoeMemosWidgetKeys.maxItems] ?: 10).toFloat()
            } catch (e: Exception) {
                // Ignore error
            } finally {
                isLoading = false
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.preferences)) },
                    navigationIcon = {
                        IconButton(onClick = { finish() }) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cancel))
                        }
                    },
                    actions = {
                        IconButton(onClick = { saveConfig(selectedTag, pinnedOnly, maxItems.toInt()) }) {
                            Icon(Icons.Default.Check, contentDescription = stringResource(R.string.confirm))
                        }
                    }
                )
            }
        ) { padding ->
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Tag Selection
                    Column {
                        Text(
                            text = stringResource(R.string.filter_by_tag),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = selectedTag == null,
                                onClick = { selectedTag = null },
                                label = { Text("None") }
                            )
                            tags.forEach { tag ->
                                FilterChip(
                                    selected = selectedTag == tag,
                                    onClick = { selectedTag = tag },
                                    label = { Text("#$tag") }
                                )
                            }
                        }
                    }

                    // Pinned Only
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.pinned_only),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Switch(
                            checked = pinnedOnly,
                            onCheckedChange = { pinnedOnly = it }
                        )
                    }

                    // Max Items
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Max Items",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = maxItems.toInt().toString(),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                        Slider(
                            value = maxItems,
                            onValueChange = { maxItems = it },
                            valueRange = 1f..30f,
                            steps = 28
                        )
                    }
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { saveConfig(selectedTag, pinnedOnly, maxItems.toInt()) }
                    ) {
                        Text(stringResource(R.string.confirm))
                    }
                }
            }
        }
    }

    private fun saveConfig(filterTag: String?, pinnedOnly: Boolean, maxItems: Int) {
        lifecycleScope.launch {
            val glanceId = GlanceAppWidgetManager(this@MeoMemosGlanceWidgetConfigurationActivity)
                .getGlanceIdBy(appWidgetId)
            
            updateAppWidgetState(this@MeoMemosGlanceWidgetConfigurationActivity, glanceId) { prefs ->
                if (filterTag != null) {
                    prefs[MoeMemosWidgetKeys.filterTag] = filterTag
                } else {
                    prefs.remove(MoeMemosWidgetKeys.filterTag)
                }
                prefs[MoeMemosWidgetKeys.pinnedOnly] = pinnedOnly
                prefs[MoeMemosWidgetKeys.maxItems] = maxItems
            }
            
            MoeMemosGlanceWidget().update(this@MeoMemosGlanceWidgetConfigurationActivity, glanceId)
            
            val resultValue = Intent().apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            setResult(RESULT_OK, resultValue)
            finish()
        }
    }
}

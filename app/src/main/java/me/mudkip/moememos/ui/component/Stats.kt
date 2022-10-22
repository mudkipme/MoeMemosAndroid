package me.mudkip.moememos.ui.component

import android.text.format.DateUtils
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import me.mudkip.moememos.viewmodel.LocalUserState
import me.mudkip.moememos.viewmodel.MemosViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.TimeZone

@Composable
fun Stats(
    memosViewModel: MemosViewModel
) {
    val days = LocalUserState.current.currentUser?.let { currentUser ->
        ChronoUnit.DAYS.between(LocalDateTime.ofEpochSecond(currentUser.createdTs, 0, OffsetDateTime.now().offset).toLocalDate(), LocalDate.now())
    } ?: 0

    Row(Modifier.padding(20.dp).fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(memosViewModel.memos.count().toString(),
                style = MaterialTheme.typography.headlineMedium
            )
            Text("Memo".uppercase(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.outline
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(memosViewModel.tags.count().toString(),
                style = MaterialTheme.typography.headlineMedium
            )
            Text("Tag".uppercase(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.outline
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(days.toString(),
                style = MaterialTheme.typography.headlineMedium
            )
            Text("Day".uppercase(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}
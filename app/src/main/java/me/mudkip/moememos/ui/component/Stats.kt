package me.mudkip.moememos.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.mudkip.moememos.R
import me.mudkip.moememos.ext.string
import me.mudkip.moememos.viewmodel.LocalMemos
import me.mudkip.moememos.viewmodel.LocalUserState
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit

@Composable
fun Stats() {
    val memosViewModel = LocalMemos.current
    val userStateViewModel = LocalUserState.current
    val days = remember(userStateViewModel.currentUser, LocalDate.now()) {
        userStateViewModel.currentUser?.let { currentUser ->
                ChronoUnit.DAYS.between(currentUser.startDate.atZone(OffsetDateTime.now().offset).toLocalDate(), LocalDate.now())
        } ?: 0
    }

    Row(
        Modifier
            .padding(20.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                memosViewModel.memos.count().toString(),
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                R.string.memo.string.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.outline
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                memosViewModel.tags.count().toString(),
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                R.string.tag.string.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.outline
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                days.toString(),
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                R.string.day.string.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}
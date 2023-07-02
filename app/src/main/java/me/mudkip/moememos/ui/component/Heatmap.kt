package me.mudkip.moememos.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import me.mudkip.moememos.viewmodel.LocalMemos
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.Locale
import kotlin.math.ceil

@Composable
fun Heatmap() {
    val memosViewModel = LocalMemos.current

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        LazyHorizontalGrid(
            rows = GridCells.Fixed(7),
            horizontalArrangement = Arrangement.spacedBy(2.dp, alignment = Alignment.End),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            userScrollEnabled = false
        ) {
            memosViewModel.matrix.takeLast(countHeatmap(constraints)).forEach {
                item(key = it.date) {
                    HeatmapStat(day = it)
                }
            }
        }
    }
}

fun countHeatmap(constraints: Constraints): Int {
    val cellSize = ceil(constraints.maxHeight.toDouble() / 7).toInt()
    if (cellSize <= 0) {
        return 0
    }
    val columns = constraints.maxWidth / cellSize
    val fullCells = columns * 7

    val firstDayOfWeek = WeekFields.of(Locale.getDefault()).firstDayOfWeek
    val firstDayOfThisWeek = LocalDate.now().with(TemporalAdjusters.previousOrSame(firstDayOfWeek))
    val lastColumn = ChronoUnit.DAYS.between(firstDayOfThisWeek, LocalDate.now()).toInt() + 1
    if (lastColumn % 7 == 0) {
        return fullCells
    }
    return fullCells - 7 + lastColumn
}
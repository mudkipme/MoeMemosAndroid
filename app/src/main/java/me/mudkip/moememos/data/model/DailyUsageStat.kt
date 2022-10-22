package me.mudkip.moememos.data.model

import java.time.LocalDate

data class DailyUsageStat(
    val date: LocalDate,
    val count: Int = 0
) {
    companion object {
        val initialMatrix: List<DailyUsageStat> by lazy {
            val now = LocalDate.now()
            (1..now.lengthOfYear()).map { day ->
                DailyUsageStat(date = now.minusDays(day - 1L))
            }.reversed()
        }
    }
}
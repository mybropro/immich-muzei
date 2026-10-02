package dev.abdus.apps.immich.provider

import dev.abdus.apps.immich.data.ImmichConfig
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Lower bound for the "photos from the last N days" filter, as an ISO-8601 instant.
 *
 * Immich validates these against a strict datetime schema, so a bare `2026-08-02` is rejected
 * with a 400 and the search comes back with nothing. Midnight local time, rendered in UTC.
 */
fun ImmichConfig.takenAfterIso(): String? = filterPresetDaysBack?.let { days ->
    val startOfDay = LocalDate.now()
        .minusDays(days.toLong())
        .atStartOfDay(ZoneId.systemDefault())
        .toInstant()
    DateTimeFormatter.ISO_INSTANT.format(startOfDay)
}

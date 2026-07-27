package dev.abdus.apps.immich.provider

import dev.abdus.apps.immich.data.AppPreferences
import dev.abdus.apps.immich.data.ImmichConfig

/**
 * Which album or person the next batch of photos should come from.
 * At most one of the two is set — see [nextSourceSelection].
 */
data class SourceSelection(
    val albumIds: List<String>? = null,
    val personIds: List<String>? = null
) {
    override fun toString(): String = when {
        albumIds != null -> "album ${albumIds.first()}"
        personIds != null -> "person ${personIds.first()}"
        else -> "whole library"
    }
}

/**
 * Selected albums and people form a single pool of sources, drawn from one at a time.
 *
 * Immich ANDs its search filters together, so asking for an album *and* a person in one
 * query returns their intersection, which is usually empty. Cycling through the pool one
 * source per refresh unions them over time instead, which is what "photos from these
 * albums and/or these people" should mean.
 *
 * Selecting nothing at all leaves both filters unset, i.e. the whole library.
 */
fun nextSourceSelection(config: ImmichConfig, prefs: AppPreferences): SourceSelection {
    // Sorted so a given index maps to the same source every time: the sets come out of
    // SharedPreferences unordered, and an unstable order would make the round-robin hop
    // around instead of cycling.
    val albumIds = config.selectedAlbumIds.sorted()
    val personIds = config.selectedPersonIds.sorted()
    val total = albumIds.size + personIds.size

    return when {
        total == 0 -> SourceSelection()
        total == 1 ->
            if (albumIds.isNotEmpty()) SourceSelection(albumIds = albumIds)
            else SourceSelection(personIds = personIds)

        else -> {
            val index = prefs.getNextSourceIndex(total)
            if (index < albumIds.size) {
                SourceSelection(albumIds = listOf(albumIds[index]))
            } else {
                SourceSelection(personIds = listOf(personIds[index - albumIds.size]))
            }
        }
    }
}

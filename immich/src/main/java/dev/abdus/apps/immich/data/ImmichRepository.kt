package dev.abdus.apps.immich.data

import dev.abdus.apps.immich.api.ImmichAlbum
import dev.abdus.apps.immich.api.ImmichAsset
import dev.abdus.apps.immich.api.ImmichClient
import dev.abdus.apps.immich.api.ImmichPerson
import dev.abdus.apps.immich.api.SearchAssetsRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ImmichRepository(private val client: ImmichClient) {
    companion object {
        private const val TAG = "ImmichRepository"
        private const val PEOPLE_PAGE_SIZE = 1000
        private const val MAX_PEOPLE_PAGES = 10
    }

    suspend fun fetchAlbums(): List<ImmichAlbum> {
        val result = withContext(Dispatchers.IO) { client.getAlbums() }
        return result.map {
            if (it.albumThumbnailAssetId != null) {
               it.albumThumbnailAssetThumbnailUrl = client.buildAssetThumbnailUrl(it.albumThumbnailAssetId)
            }
            it
        }
    }

    suspend fun fetchTags(): List<dev.abdus.apps.immich.api.ImmichTag> =
        withContext(Dispatchers.IO) { client.getTags() }

    /**
     * Fetch every named person, walking the paginated /people endpoint.
     * Unnamed faces are dropped — they are useless as a wallpaper filter you pick by name.
     */
    suspend fun fetchPeople(): List<ImmichPerson> = withContext(Dispatchers.IO) {
        val people = mutableListOf<ImmichPerson>()
        var page = 1
        while (page <= MAX_PEOPLE_PAGES) {
            val response = client.getPeople(page = page, size = PEOPLE_PAGE_SIZE, withHidden = false)
            people += response.people
            if (!response.hasNextPage) break
            page++
        }
        people
            .filter { !it.isHidden && it.name.isNotBlank() }
            .map {
                it.thumbnailUrl = client.buildPersonThumbnailUrl(it.id)
                it
            }
    }

    suspend fun fetchRandomAssets(
        albumIds: List<String>?,
        tagIds: List<String>?,
        personIds: List<String>? = null,
        favoritesOnly: Boolean = false,
        takenAfter: String? = null,
        takenBefore: String? = null,
        size: Int = 10,
        exclusionQuery: String = ""
    ): List<ImmichAsset> = withContext(Dispatchers.IO) {
        val request = SearchAssetsRequest(
            albumIds = albumIds,
            tagIds = tagIds,
            personIds = personIds,
            size = size,
            isFavorite = if (favoritesOnly) true else null,
            takenAfter = takenAfter,
            takenBefore = takenBefore
        )
        val query = exclusionQuery.trim()
        val excludedIds = if (query.isEmpty()) {
            emptySet()
        } else {
            // Fetch before random selection. A failed search must not silently disable exclusions.
            client.searchSmart(request.copy(size = 100, query = query, page = 1))
                .assets.items.take(100).map { it.id }.toSet()
        }
        val result = client.getRandomAssets(request).filterNot { it.id in excludedIds }
        result.map {
            it.downloadUrl = client.buildAssetDownloadUrl(it.id)
            it.previewUrl = client.buildAssetPreviewUrl(it.id)
            it.viewUrl = client.buildAssetViewUrl(it.id)
            it
        }
    }
}

package dev.abdus.apps.immich.api

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Query

private const val HEADER_API_KEY = "x-api-key"

interface ImmichApi {
    @GET("server/about")
    suspend fun getServerInfo(): kotlinx.serialization.json.JsonObject

    @GET("albums")
    suspend fun getAlbums(): List<ImmichAlbum>

    @GET("tags")
    suspend fun getTags(): List<ImmichTag>

    @GET("people")
    suspend fun getPeople(
        @Query("page") page: Int,
        @Query("size") size: Int,
        @Query("withHidden") withHidden: Boolean,
    ): ImmichPeopleResponse

    @POST("search/random")
    suspend fun getRandomAssets(@Body request: SearchAssetsRequest): List<ImmichAsset>

    @POST("search/smart")
    suspend fun searchSmart(@Body request: SearchAssetsRequest): SmartSearchResponse

    @PUT("assets")
    suspend fun updateAssets(@Body request: UpdateAssetsRequest)
}

class ImmichClient private constructor(
    val baseUrl: String,
    val apiKey: String,
    private val api: ImmichApi,
) : ImmichApi by api {
    fun buildAssetDownloadUrl(assetId: String): String {
        return "${baseUrl}assets/$assetId/original?apiKey=$apiKey"
    }

    fun buildAssetPreviewUrl(assetId: String): String {
        return "${baseUrl}assets/$assetId/thumbnail?size=preview&apiKey=$apiKey"
    }

    fun buildAssetThumbnailUrl(assetId: String): String {
        return "${baseUrl}assets/$assetId/thumbnail?size=thumbnail&apiKey=$apiKey"
    }

    fun buildAssetViewUrl(assetId: String): String {
        return "${baseUrl}photos/$assetId"
    }

    fun buildPersonThumbnailUrl(personId: String): String {
        return "${baseUrl}people/$personId/thumbnail?apiKey=$apiKey"
    }

    companion object {
        fun create(baseUrl: String, apiKey: String): ImmichClient {
            val baseUrlClean = baseUrl.trimEnd('/') + "/"
            val apiKeyClean = apiKey.trim()

            val json = Json {
                ignoreUnknownKeys = true
                encodeDefaults = true  // Changed to true so size parameter is sent
                explicitNulls = false
            }
            val loggingInterceptor = okhttp3.logging.HttpLoggingInterceptor().apply {
                level = okhttp3.logging.HttpLoggingInterceptor.Level.BODY
            }
            val client = OkHttpClient.Builder()
                .addInterceptor(ApiKeyInterceptor(apiKeyClean))
                .addInterceptor(loggingInterceptor)
                .build()

            val retrofitApi = Retrofit.Builder()
                .baseUrl(baseUrlClean)
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .client(client)
                .build()
                .create(ImmichApi::class.java)

            return ImmichClient(baseUrlClean, apiKeyClean, retrofitApi)
        }
    }
}

@Serializable
data class SearchAssetsRequest(
    val type: String = "IMAGE",
    val albumIds: List<String>? = null,
    val tagIds: List<String>? = null,
    val personIds: List<String>? = null,
    val size: Int = 10,
    val isFavorite: Boolean? = null,
    // Filter by when the photo was taken, not when it was uploaded — `createdAfter`/`createdBefore`
    // match on the upload timestamp, which excludes old photos of a person added to a recent import.
    // ISO-8601 instant, e.g. 2026-08-02T00:00:00Z; the API rejects date-only strings.
    val takenAfter: String? = null,
    val takenBefore: String? = null,
    // Shared filter DTO keeps smart-search exclusions scoped exactly like random selection.
    // These fields are omitted from random requests.
    val query: String? = null,
    val page: Int? = null
)

@Serializable
data class SmartSearchResponse(val assets: SmartSearchAssets)

@Serializable
data class SmartSearchAssets(
    val items: List<SmartSearchAsset>,
    val nextPage: String? = null
)

@Serializable
data class SmartSearchAsset(val id: String)

@Serializable
data class UpdateAssetsRequest(
    val ids: List<String>,
    val isFavorite: Boolean? = null
)


@Serializable
data class ImmichAlbum(
    val id: String,
    val albumName: String,
    val albumThumbnailAssetId: String?,
    var albumThumbnailAssetThumbnailUrl: String?,
    val assetCount: Int,
    val updatedAt: String? = null,
    val lastModifiedAssetTimestamp: String? = null
)

@Serializable
data class ImmichTag(
    val id: String,
    val name: String,
    val value: String
)

@Serializable
data class ImmichPeopleResponse(
    val people: List<ImmichPerson> = emptyList(),
    val total: Int = 0,
    val hidden: Int = 0,
    val hasNextPage: Boolean = false
)

@Serializable
data class ImmichPerson(
    val id: String,
    // Unnamed faces come back with an empty name
    val name: String = "",
    val isHidden: Boolean = false,
    val birthDate: String? = null,
    var thumbnailUrl: String? = null
)

@Serializable
data class ImmichAsset(
    val id: String,
    val type: String? = null,
    val albumId: String? = null,
    val originalFileName: String? = null,
    val originalMimeType: String? = null,
    val ownerId: String? = null,
    val resized: Boolean? = null,
    val originalPath: String,
    val fileCreatedAt: String? = null,
    var downloadUrl: String? = null,
    var previewUrl: String? = null,
    var viewUrl: String? = null,
) {
    fun createdDate(): String {
        return fileCreatedAt?.substringBefore('T') ?: "Unknown"
    }
}

private class ApiKeyInterceptor(
    private val apiKey: String
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val newRequest = chain.request().newBuilder()
            .addHeader(HEADER_API_KEY, apiKey)
            .build()
        return chain.proceed(newRequest)
    }
}

package dev.abdus.apps.immich.data

import dev.abdus.apps.immich.api.ImmichClient
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import java.util.concurrent.TimeUnit

class ImmichRepositoryTest {
    private lateinit var server: MockWebServer
    private lateinit var repository: ImmichRepository

    @Before
    fun setup() {
        server = MockWebServer()
        server.start()
        repository = ImmichRepository(ImmichClient.create(server.url("/api/").toString(), "test-key"))
    }

    @After
    fun teardown() { server.shutdown() }

    private fun respond(body: String, code: Int = 200) {
        server.enqueue(MockResponse().setResponseCode(code).setHeader("Content-Type", "application/json").setBody(body))
    }

    private fun randomAsset(id: String) = """{"id":"$id","type":"IMAGE","originalPath":"/photos/$id.jpg"}"""

    @Test
    fun `smart search uses identical filters and removes matching IDs`() = runBlocking {
        respond("""{"assets":{"items":[{"id":"excluded"}],"nextPage":"2"}}""")
        respond("[${randomAsset("excluded")},${randomAsset("kept")}]")

        val result = repository.fetchRandomAssets(
            albumIds = listOf("album"), tagIds = listOf("tag"), personIds = listOf("person"),
            favoritesOnly = true, takenAfter = "2026-09-01T04:00:00Z",
            takenBefore = "2026-10-01T04:00:00Z", size = 6, exclusionQuery = "  bath or tub  "
        )
        assertEquals(listOf("kept"), result.map { it.id })
        assertTrue(result.single().downloadUrl!!.contains("assets/kept/original"))

        val smart = server.takeRequest(2, TimeUnit.SECONDS)!!
        val random = server.takeRequest(2, TimeUnit.SECONDS)!!
        assertEquals("/api/search/smart", smart.path)
        assertEquals("/api/search/random", random.path)
        val smartBody = Json.parseToJsonElement(smart.body.readUtf8()).jsonObject
        val randomBody = Json.parseToJsonElement(random.body.readUtf8()).jsonObject
        assertEquals(Json.parseToJsonElement("100"), smartBody["size"])
        assertEquals(Json.parseToJsonElement("1"), smartBody["page"])
        assertEquals(Json.parseToJsonElement("\"bath or tub\""), smartBody["query"])
        assertEquals(Json.parseToJsonElement("6"), randomBody["size"])
        assertEquals(Json.parseToJsonElement("\"IMAGE\""), randomBody["type"])
        assertEquals(randomBody["type"], smartBody["type"])
        assertFalse(randomBody.containsKey("query"))
        assertFalse(randomBody.containsKey("page"))
        assertEquals(randomBody.filterKeys { it != "size" }, smartBody.filterKeys { it !in setOf("size", "query", "page") })
        assertEquals(2, server.requestCount) // Do not follow nextPage beyond the first 100.
    }

    @Test
    fun `blank query skips smart search and optional filters are omitted`() = runBlocking {
        respond("[${randomAsset("kept")}]")
        val result = repository.fetchRandomAssets(null, null, exclusionQuery = " \n ")
        assertEquals(listOf("kept"), result.map { it.id })
        val request = server.takeRequest(2, TimeUnit.SECONDS)!!
        assertEquals("/api/search/random", request.path)
        val body = Json.parseToJsonElement(request.body.readUtf8()).jsonObject
        assertEquals(setOf("size", "type"), body.keys)
        assertEquals(Json.parseToJsonElement("\"IMAGE\""), body["type"])
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `empty smart results retain random photos`() = runBlocking {
        respond("""{"assets":{"items":[],"nextPage":null}}""")
        respond("[${randomAsset("kept")}]")
        assertEquals(listOf("kept"), repository.fetchRandomAssets(null, null, exclusionQuery = "tub").map { it.id })
    }

    @Test
    fun `non-image assets cannot become wallpaper even if server ignores type filter`() = runBlocking {
        respond("""[${randomAsset("photo")},
            {"id":"movie","type":"VIDEO","originalPath":"/photos/movie.mp4","originalMimeType":"video/mp4"},
            {"id":"audio","type":"AUDIO","originalPath":"/photos/audio.mp3"},
            {"id":"unknown","originalPath":"/photos/unknown"}]""")
        assertEquals(listOf("photo"), repository.fetchRandomAssets(null, null).map { it.id })
    }

    @Test
    fun `fully excluded random batch returns no photos`() = runBlocking {
        respond("""{"assets":{"items":[{"id":"excluded"}]}}""")
        respond("[${randomAsset("excluded")}]")
        assertTrue(repository.fetchRandomAssets(null, null, exclusionQuery = "tub").isEmpty())
        assertEquals(2, server.requestCount)
    }

    @Test
    fun `failed smart search does not supply unfiltered random photos`() = runBlocking {
        respond("{}", 503)
        try {
            repository.fetchRandomAssets(null, null, exclusionQuery = "tub")
            fail("Expected smart search failure")
        } catch (e: HttpException) {
            assertEquals(503, e.code())
        }
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `only first 100 matches are excluded even if server returns more`() = runBlocking {
        val matches = (1..101).joinToString(",") { """{"id":"photo-$it"}""" }
        respond("""{"assets":{"items":[$matches]}}""")
        respond("[${randomAsset("photo-100")},${randomAsset("photo-101")}]")
        assertEquals(listOf("photo-101"), repository.fetchRandomAssets(null, null, exclusionQuery = "tub").map { it.id })
    }
}

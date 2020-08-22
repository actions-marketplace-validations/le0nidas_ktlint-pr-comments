@file:Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CollectPrChangesTest {
    @Test fun `the event triggers the collection of all kt-related changes`() {
        mockWebServer.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return when (request.path) {
                    "/repos/le0nidas/ktlint-playground/pulls/7/files?page=1" ->
                        MockResponse().setBody(
                                "[\n" +
                                        "  {\n" +
                                        "    \"filename\": \"src/main/kotlin/Main.kt\",\n" +
                                        "    \"status\": \"added\"\n" +
                                        "  },\n" +
                                        "  {\n" +
                                        "    \"filename\": \"src/main/kotlin/Main2.kt\",\n" +
                                        "    \"status\": \"added\"\n" +
                                        "  }\n" +
                                        "]"
                        )
                    else ->
                        throw IllegalArgumentException("Unknown path: ${request.path}")
                }
            }
        }

        val pathToEventFile = CollectPrChangesTest::class.java.classLoader.getResource("event.json").path
        val userToken = "abc1234"

        val actual = collectPrChanges(arrayOf(pathToEventFile, userToken), mockWebServer.url("/"))

        assertThat(
                actual,
                equalTo(CollectedChanges("src/main/kotlin/Main.kt src/main/kotlin/Main2.kt"))
        )
    }

    @Test fun `the provided token is being sent in the request's header`() {
        mockWebServer.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return when (request.path) {
                    "/repos/le0nidas/ktlint-playground/pulls/7/files?page=1" ->
                        MockResponse().setBody(
                                "[\n" +
                                        "  {\n" +
                                        "    \"filename\": \"src/main/kotlin/Main.kt\",\n" +
                                        "    \"status\": \"added\"\n" +
                                        "  }\n" +
                                        "]"
                        )
                    else ->
                        throw IllegalArgumentException("Unknown path: ${request.path}")
                }
            }
        }

        val pathToEventFile = CollectPrChangesTest::class.java.classLoader.getResource("event.json").path
        val userToken = "abc1234"

        collectPrChanges(arrayOf(pathToEventFile, userToken), mockWebServer.url("/"))

        assertThat(mockWebServer.takeRequest().headers["Authorization"], equalTo("token abc1234"))
    }

    lateinit var mockWebServer: MockWebServer

    @BeforeEach
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
    }

    @AfterEach
    fun tearDown() {
        mockWebServer.shutdown()
    }
}
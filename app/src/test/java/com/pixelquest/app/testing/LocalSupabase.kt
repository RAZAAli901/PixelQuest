package com.pixelquest.app.testing

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.MemoryCodeVerifierCache
import io.github.jan.supabase.auth.MemorySessionManager
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * The local Supabase test bench (npx supabase start; docs/LOCAL_SUPABASE.md) for JVM tests of the
 * app's real repositories. Opt-in: tests using it are skipped unless PIXELQUEST_LOCAL_SUPABASE=1.
 * The URL and keys come from `supabase status` on this computer; nothing talks to a hosted project.
 */
object LocalSupabase {
    val enabled: Boolean get() = System.getenv("PIXELQUEST_LOCAL_SUPABASE") == "1"

    data class Status(val apiUrl: String, val publishableKey: String, val serviceKey: String, val mailUrl: String)

    val status: Status by lazy {
        val raw = System.getenv("SUPABASE_LOCAL_STATUS_JSON") ?: run {
            val repo = File("").absoluteFile.let { if (File(it, "supabase").isDirectory) it else it.parentFile }
            val npx = if (System.getProperty("os.name").startsWith("Windows")) listOf("cmd", "/c", "npx") else listOf("npx")
            val process = ProcessBuilder(npx + listOf("--yes", "supabase@2.120.0", "status", "-o", "json"))
                .directory(repo).redirectErrorStream(true).start()
            val out: String = process.inputStream.bufferedReader().readText()
            process.waitFor()
            out
        }
        val json = Json.parseToJsonElement(raw.substring(raw.indexOf('{'), raw.lastIndexOf('}') + 1)).jsonObject
        fun field(vararg names: String): String = names.firstNotNullOf { name -> json[name]?.jsonPrimitive?.content }
        Status(
            apiUrl = field("API_URL"),
            publishableKey = field("PUBLISHABLE_KEY", "ANON_KEY"),
            serviceKey = field("SERVICE_ROLE_KEY"),
            mailUrl = field("MAILPIT_URL", "INBUCKET_URL")
        )
    }

    /**
     * A client like the app's (SupabaseClient.kt), with the session kept in memory. It uses Ktor's
     * CIO engine: the app's Android engine runs on HttpURLConnection, which on a phone can send
     * PATCH (leaving the leaderboard) but on the JVM can't.
     */
    fun client(): SupabaseClient = createSupabaseClient(status.apiUrl, status.publishableKey) {
        httpEngine = io.ktor.client.engine.cio.CIO.create()
        install(Auth) {
            sessionManager = MemorySessionManager()
            codeVerifierCache = MemoryCodeVerifierCache()
            autoLoadFromStorage = false
            alwaysAutoRefresh = false
            enableLifecycleCallbacks = false
        }
        install(Postgrest)
    }

    fun newEmail(tag: String) = "$tag.${System.currentTimeMillis()}.${(100000..999999).random()}@pixelquest.test"

    /** The 6-digit code in the newest email the local mail catcher has for [to]. */
    fun latestCode(to: String): String {
        repeat(50) {
            val query = URLEncoder.encode("to:\"$to\"", "UTF-8")
            val list = get("${status.mailUrl}/api/v1/search?query=$query")
            val first = (Json.parseToJsonElement(list).jsonObject["messages"] as? JsonArray)?.firstOrNull()
            if (first != null) {
                val id = first.jsonObject["ID"]!!.jsonPrimitive.content
                val message = Json.parseToJsonElement(get("${status.mailUrl}/api/v1/message/$id")).jsonObject
                val text = message["Text"]?.jsonPrimitive?.content.orEmpty() + message["HTML"]?.jsonPrimitive?.content.orEmpty()
                return Regex("\\b(\\d{6})\\b").find(text)?.groupValues?.get(1) ?: error("no code in the email to $to")
            }
            Thread.sleep(200)
        }
        error("no email arrived for $to")
    }

    data class Reply(val status: Int, val body: String)

    /** A REST call with the service role key (bypasses row-level security), for setup and checks. */
    fun asService(path: String, method: String = "GET", body: String? = null, prefer: String? = null): Reply {
        val connection = URL("${status.apiUrl}$path").openConnection() as HttpURLConnection
        connection.setRequestProperty("apikey", status.serviceKey)
        connection.setRequestProperty("Authorization", "Bearer ${status.serviceKey}")
        connection.setRequestProperty("Content-Type", "application/json")
        prefer?.let { connection.setRequestProperty("Prefer", it) }
        connection.requestMethod = method // GET, POST or DELETE: the JVM's HttpURLConnection can't PATCH
        if (body != null) {
            connection.doOutput = true
            connection.outputStream.use { it.write(body.toByteArray()) }
        }
        val code = connection.responseCode
        val text = (if (code < 400) connection.inputStream else connection.errorStream)?.bufferedReader()?.readText().orEmpty()
        return Reply(code, text)
    }

    fun rows(path: String) = Json.parseToJsonElement(asService(path).body).jsonArray

    private fun get(url: String): String = URL(url).readText()

    /** Sets columns on a player's row as the service role (an upsert, since the JVM can't PATCH). */
    fun setProfileColumns(userId: String, json: String) =
        asService("/rest/v1/profiles?on_conflict=id", "POST", """{"id":"$userId",$json}""", prefer = "resolution=merge-duplicates")

    /** A confirmed player made with the admin API (no email round trip); returns their id. */
    fun createPlayer(email: String): String {
        val reply = asService("/auth/v1/admin/users", "POST", """{"email":"$email","email_confirm":true}""")
        check(reply.status in 200..201) { "admin create user: ${reply.status} ${reply.body}" }
        return Json.parseToJsonElement(reply.body).jsonObject["id"]!!.jsonPrimitive.content
    }
}

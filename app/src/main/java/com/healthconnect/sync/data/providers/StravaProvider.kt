package com.healthconnect.sync.data.providers

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.healthconnect.sync.data.WorkoutTelemetry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.Duration

class StravaProvider(private val context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var accessToken: String?
        get() = prefs.getString(KEY_ACCESS_TOKEN, null)
        set(value) = prefs.edit().putString(KEY_ACCESS_TOKEN, value).apply()

    var refreshToken: String?
        get() = prefs.getString(KEY_REFRESH_TOKEN, null)
        set(value) = prefs.edit().putString(KEY_REFRESH_TOKEN, value).apply()

    val isConnected: Boolean
        get() = !accessToken.isNullOrBlank()

    fun createAuthorizationIntent(clientId: String = "DEFAULT_CLIENT_ID"): Intent {
        val redirectUri = "healthconnect://strava-callback"
        val scope = "activity:write,activity:read_all"
        val authUrl = "https://www.strava.com/oauth/mobile/authorize" +
                "?client_id=$clientId" +
                "&redirect_uri=${URLEncoder.encode(redirectUri, "UTF-8")}" +
                "&response_type=code" +
                "&approval_prompt=auto" +
                "&scope=${URLEncoder.encode(scope, "UTF-8")}"

        return Intent(Intent.ACTION_VIEW, Uri.parse(authUrl))
    }

    suspend fun uploadWorkout(workout: WorkoutTelemetry): Result<String> = withContext(Dispatchers.IO) {
        val token = accessToken
        if (token.isNullOrBlank()) {
            return@withContext Result.failure(IllegalStateException("Strava is not authorized. Please connect Strava account."))
        }

        try {
            val apiUrl = URL("https://www.strava.com/api/v3/activities")
            val connection = (apiUrl.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                doOutput = true
                connectTimeout = 15000
                readTimeout = 15000
            }

            val durationSeconds = Duration.between(workout.startTime, workout.endTime).seconds
            val postData = buildString {
                append("name=").append(URLEncoder.encode(workout.title, "UTF-8"))
                append("&sport_type=").append(URLEncoder.encode(workout.exerciseType, "UTF-8"))
                append("&start_date_local=").append(URLEncoder.encode(workout.startTime.toString(), "UTF-8"))
                append("&elapsed_time=").append(durationSeconds)
                workout.distanceMeters?.let {
                    append("&distance=").append(it)
                }
                append("&description=").append(URLEncoder.encode("Synchronized by healthConnect", "UTF-8"))
            }

            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(postData)
                writer.flush()
            }

            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(responseBody)
                val activityId = json.optString("id", "uploaded")
                Result.success(activityId)
            } else {
                val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "HTTP $responseCode"
                Result.failure(Exception("Strava API error ($responseCode): $errorBody"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        private const val PREFS_NAME = "strava_provider_prefs"
        private const val KEY_ACCESS_TOKEN = "strava_access_token"
        private const val KEY_REFRESH_TOKEN = "strava_refresh_token"
    }
}

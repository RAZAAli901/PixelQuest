package com.pixelquest.app.auth

import com.pixelquest.app.data.remote.SupabaseResult

/**
 * Signing in with a code sent by email (no password): the player types their address, Supabase emails
 * a one-time code, and typing it in signs them in, making the account the first time. These are the
 * rules and the messages; AuthRepository talks to Supabase and AuthViewModel runs the steps.
 */
object EmailSignIn {
    private val EMAIL = Regex("^[^@\\s]+@[^@\\s.]+(\\.[^@\\s.]+)+$")

    /** Supabase allows one code per address per minute, so the app waits that long too. */
    const val RESEND_AFTER_SECONDS = 60L

    fun normalizeEmail(email: String): String = email.trim().lowercase()

    fun isValidEmail(email: String): Boolean = EMAIL.matches(normalizeEmail(email))

    /** Digits only: pasted codes often come with spaces. */
    fun normalizeCode(code: String): String = code.filter { it.isDigit() }

    /** Supabase sends 6 digits unless the project chose a longer code (up to 10). */
    fun isValidCode(code: String): Boolean = normalizeCode(code).length in 6..10

    const val INVALID_EMAIL = "That email address doesn't look right."
    const val INVALID_CODE = "Enter the code from the email: 6 digits, numbers only."

    fun codeSentMessage(email: String) = "We emailed a sign-in code to $email. It may take a minute; check spam too."

    /** Why a code couldn't be sent. Supabase's error codes come through as the ServerError message. */
    fun sendFailure(result: SupabaseResult<*>): String = when (result) {
        is SupabaseResult.NetworkError -> "Couldn't reach the server. Check your connection and try again."
        is SupabaseResult.ServerError -> when (result.message) {
            "over_email_send_rate_limit", "over_request_rate_limit" ->
                "Too many codes asked for. Wait a minute, then try again."
            "email_address_invalid", "validation_failed" -> INVALID_EMAIL
            "email_address_not_authorized" ->
                "Sign-in emails can't be sent to this address yet. Try Google sign-in, or try again later."
            "signup_disabled", "otp_disabled", "email_provider_disabled" ->
                "Email sign-in is switched off on the server. Try Google sign-in."
            else -> "Couldn't send the code. Try again later."
        }
        else -> "Couldn't send the code. Try again later."
    }

    /** Why a code didn't sign in. */
    fun verifyFailure(result: SupabaseResult<*>): String = when (result) {
        is SupabaseResult.NetworkError -> "Couldn't reach the server. Check your connection and try again."
        is SupabaseResult.ServerError -> when (result.message) {
            "over_request_rate_limit" -> "Too many tries. Wait a minute, then try again."
            else -> "That code is wrong or has expired. Check the newest email, or send a new code."
        }
        else -> "That code is wrong or has expired. Check the newest email, or send a new code."
    }
}

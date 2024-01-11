package com.example.eventconnect

import java.util.regex.Pattern


object EmailValidator {
    private const val EMAIL_REGEX = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\$"

    fun isValidEmail(email: String?): Boolean {
        val pattern = Pattern.compile(EMAIL_REGEX, Pattern.CASE_INSENSITIVE)
        return pattern.matcher(email).matches()
    }
}

object PasswordValidator {
    fun isValidPassword(password: String): Boolean {
        // Patrón para validar que la contraseña tenga al menos 6 caracteres, una letra y un número
        val regex = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d!@#\$%^&*()_+\\-=\\[\\]{};':\",./<>?\\\\|]{6,}$"
        return password.matches(Regex(regex))
    }
}

object PhoneValidator {
    fun isValidPhone(phone: String): Boolean {
        return phone.isNotEmpty() && phone.length >= 9
    }
}
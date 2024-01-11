package com.example.eventconnect

fun main() {
    val password = "!Lunatommy67"
    val isValid = PasswordValidator.isValidPassword(password)
    println("La contraseña \"$password\" es válida: $isValid")
}
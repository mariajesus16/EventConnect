package com.example.eventconnect

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

fun main() {
    val eventRepository = EventRepository()

    // Crear un evento de ejemplo
    val event = Event("Evento 1", "Lugar 1", "2022-12-01")

    // Almacenar el evento en la base de datos
    eventRepository.saveEvent(event)
}








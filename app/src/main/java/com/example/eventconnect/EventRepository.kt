package com.example.eventconnect


import com.google.firebase.database.*
import kotlinx.serialization.Serializable

@Serializable
data class Event(val name: String, val location: String, val date: String)

class EventRepository {
    private val databaseReference = FirebaseDatabase.getInstance("https://eventconnect-150ed-default-rtdb.europe-west1.firebasedatabase.app/").reference
    fun saveEvent(event: Event) {
        // Generar un nuevo ID único para el evento
        val eventId = databaseReference.child("events").push().key

        // Almacenar el evento en la base de datos
        if (eventId != null) {
            databaseReference.child("events").child(eventId).setValue(event)
        }
    }
}

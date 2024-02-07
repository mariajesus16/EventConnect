package com.example.eventconnect

data class Evento(
    val id: String,
    val ciudad: String,
    val date: String,
    val info: String,
    val link: String,
    val lugar: String,
    val name: String,
    var imagenUrl: String
)
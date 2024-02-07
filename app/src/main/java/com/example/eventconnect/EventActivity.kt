package com.example.eventconnect

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

class EventActivity : AppCompatActivity() {
    private var eventId: String? = null
    private lateinit var sharedPreferences: SharedPreferences

    private val EVENT_ID_KEY = "eventId"
    private lateinit var database: FirebaseDatabase
    private lateinit var storageRef: StorageReference
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var eventosRef: DatabaseReference
    private lateinit var button: Button
    private lateinit var fragmentContainer: FrameLayout
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_event)
        sharedPreferences = getSharedPreferences("event_prefs", Context.MODE_PRIVATE)
        firebaseAuth = FirebaseAuth.getInstance()
        // Obtén la referencia de Firebase Storage
        storageRef = FirebaseStorage.getInstance("gs://eventconnect-150ed.appspot.com").reference
        // Inicializa la base de datos
        database =
            FirebaseDatabase.getInstance("https://eventconnect-150ed-default-rtdb.europe-west1.firebasedatabase.app/")
        eventosRef = database.getReference("eventos")
        // Aquí puedes usar el eventId según sea necesario
        // Por ejemplo, mostrar los detalles del evento correspondiente al ID proporcionado
        button = findViewById(R.id.btn_get_tickets)
        fragmentContainer = findViewById(R.id.fragment_container_event)
        eventId = getSavedEventId()
        button.setOnClickListener {

            // Crear un diálogo de confirmación
            val builder = AlertDialog.Builder(this)
            builder.setTitle(getString(R.string.confirmation))
            builder.setMessage(getString(R.string.access_to_buy_ticket))
            builder.setPositiveButton(getString(R.string.yes)) { dialog, which ->
                // Acción cuando se selecciona "Sí"
                // Abrir el enlace del evento en un navegador web
                eventId?.let { it1 ->
                    getEventFromFirebase(it1) { evento ->
                        val url = evento?.link
                        val intent = Intent(Intent.ACTION_VIEW)
                        intent.data = Uri.parse(url)
                        startActivity(intent)
                    }
                }

            }
            builder.setNegativeButton(getString(R.string.cancel), null)
            val dialog = builder.create()
            dialog.show()
        }
        // Cargar el fragmento evento
        loadFragment(EventFragment())
    }

    private fun getEventFromFirebase(eventId: String, callback: (Evento?) -> Unit) {
        val eventQuery = eventosRef.child(eventId)
        eventQuery.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Verificar si existe un evento con el ID proporcionado
                if (snapshot.exists()) {
                    // Obtener los datos del evento
                    val ciudad = snapshot.child("ciudad").getValue(String::class.java)
                    val date = snapshot.child("date").getValue(String::class.java)
                    val info = snapshot.child("info").getValue(String::class.java)
                    val link = snapshot.child("link").getValue(String::class.java)
                    val lugar = snapshot.child("lugar").getValue(String::class.java)
                    val name = snapshot.child("name").getValue(String::class.java)
                    val imageUrl = snapshot.child("imagenUrl").getValue(String::class.java)
                    if (ciudad != null && date != null && info != null &&
                        link != null && lugar != null && name != null && imageUrl != null
                    ) {
                        val evento =
                            Evento(eventId, ciudad, date, info, link, lugar, name, imageUrl)
                        callback(evento)
                    }
                    // Aquí puedes usar los datos del evento según sea necesario
                } else {
                    // El evento con el ID proporcionado no existe
                    callback(null)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                callback(null)
            }
        })

    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container_event, fragment)
            .commit()
    }

    private fun getSavedEventId(): String? {
        return sharedPreferences.getString(EVENT_ID_KEY, null)
    }
}
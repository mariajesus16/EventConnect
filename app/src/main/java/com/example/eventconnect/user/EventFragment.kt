package com.example.eventconnect.user

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.example.eventconnect.Evento
import com.example.eventconnect.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.squareup.picasso.Picasso
/**
 * A simple [Fragment] subclass.
 * Use the [EventFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class EventFragment : Fragment() {
    private lateinit var database: FirebaseDatabase
    private lateinit var storageRef: StorageReference
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var eventosRef: DatabaseReference
    private lateinit var sharedPreferences: SharedPreferences

    private lateinit var imageEvento : ImageView
    private lateinit var nombreEvento : TextView

    private val EVENT_ID_KEY = "eventId"
    private var eventId: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_event, container, false)
        sharedPreferences = requireActivity().getSharedPreferences("event_prefs", Context.MODE_PRIVATE)
        firebaseAuth = FirebaseAuth.getInstance()
        // Obtén la referencia de Firebase Storage
        storageRef = FirebaseStorage.getInstance("gs://eventconnect-150ed.appspot.com").reference
        // Inicializa la base de datos
        database =
            FirebaseDatabase.getInstance("https://eventconnect-150ed-default-rtdb.europe-west1.firebasedatabase.app/")
        eventosRef = database.getReference("eventos")


        imageEvento = view.findViewById(R.id.imageEvento)
        nombreEvento = view.findViewById(R.id.nombreEvento)
        eventId = getSavedEventId()
        eventId?.let { it1 ->
            getEventFromFirebase(it1) { evento ->
                Picasso.get().load(evento?.imagenUrl).into(imageEvento)
                nombreEvento.text = evento?.name
            }
        }
        return view
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

    private fun getSavedEventId(): String? {
        return sharedPreferences.getString(EVENT_ID_KEY, null)
    }
}
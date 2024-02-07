package com.example.eventconnect

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

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
        sharedPreferences = requireActivity().getSharedPreferences("event_prefs", Context.MODE_PRIVATE)
        firebaseAuth = FirebaseAuth.getInstance()
        // Obtén la referencia de Firebase Storage
        storageRef = FirebaseStorage.getInstance("gs://eventconnect-150ed.appspot.com").reference
        // Inicializa la base de datos
        database =
            FirebaseDatabase.getInstance("https://eventconnect-150ed-default-rtdb.europe-west1.firebasedatabase.app/")
        eventosRef = database.getReference("eventos")
        eventId = getSavedEventId()
        return inflater.inflate(R.layout.fragment_event, container, false)
    }
    private fun getEventFromFirebase(eventId: String) {
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
                        val evento = Evento(eventId, ciudad, date, info, link, lugar, name, imageUrl)
                    }
                    // Aquí puedes usar los datos del evento según sea necesario
                } else {
                    // El evento con el ID proporcionado no existe
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment EventFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            EventFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }

    private fun getSavedEventId(): String? {
        return sharedPreferences.getString(EVENT_ID_KEY, null)
    }
}
package com.example.eventconnect.admin

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.example.eventconnect.Evento
import com.example.eventconnect.MainActivity
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
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale

class AdminEventFragment : Fragment() {
    private lateinit var database: FirebaseDatabase
    private lateinit var storageRef: StorageReference
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var eventosRef: DatabaseReference
    private lateinit var sharedPreferences: SharedPreferences

    private lateinit var imageEvento: ImageView
    private lateinit var fechaEvento: TextView
    private lateinit var nombreEvento: TextView
    private lateinit var lugarEvento: TextView
    private lateinit var infoEvento: TextView

    private lateinit var btnOpenMap: Button
    private lateinit var btnDeleteEvent : ImageView
    private lateinit var btnEditEvent : ImageView
    private val EVENT_ID_KEY = "eventId"
    private var eventId: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_admin_event, container, false)

        sharedPreferences =
            requireActivity().getSharedPreferences("event_prefs", Context.MODE_PRIVATE)
        firebaseAuth = FirebaseAuth.getInstance()
        // Obtén la referencia de Firebase Storage
        storageRef = FirebaseStorage.getInstance("gs://eventconnect-150ed.appspot.com").reference
        // Inicializa la base de datos
        database =
            FirebaseDatabase.getInstance("https://eventconnect-150ed-default-rtdb.europe-west1.firebasedatabase.app/")
        eventosRef = database.getReference("eventos")


        imageEvento = view.findViewById(R.id.imageEvento)
        nombreEvento = view.findViewById(R.id.nombreEvento)
        fechaEvento = view.findViewById(R.id.fechaEvento)
        lugarEvento = view.findViewById(R.id.lugarEvento)
        infoEvento = view.findViewById(R.id.infoEvento)
        btnOpenMap = view.findViewById(R.id.btnOpenMap)
        btnDeleteEvent = view.findViewById(R.id.deleteEvent)
        btnEditEvent = view.findViewById(R.id.editEvent)

        eventId = getSavedEventId()
        eventId?.let { it1 ->
            getEventFromFirebase(it1) { evento ->
                val formattedDateTime = formatDateTime(evento!!.date ?: "")

                Picasso.get().load(evento.imagenUrl).into(imageEvento)
                nombreEvento.text = evento.name
                fechaEvento.text = formattedDateTime
                lugarEvento.text = evento.lugar
                infoEvento.text = evento.info

                val readMoreTextView: TextView = view.findViewById(R.id.readMoreText)

                if(infoEvento.lineCount < 2){
                    readMoreTextView.isEnabled = false
                    readMoreTextView.text = ""
                }

                readMoreTextView.setOnClickListener {
                    if (infoEvento.maxLines == 2) {
                        infoEvento.maxLines = Int.MAX_VALUE
                        readMoreTextView.text = getString(R.string.read_less)
                    } else {
                        infoEvento.maxLines = 2
                        readMoreTextView.text = getString(R.string.read_more)
                    }
                }

                // En desarrollo
                btnEditEvent.setOnClickListener {

                }

                btnDeleteEvent.setOnClickListener {
                    borrarEvento(evento.id!!)
                }

                btnOpenMap.setOnClickListener {
                    val lugarEvento = evento.lugar // Obtén la ubicación del evento aquí

                    // Crear el intent para abrir Google Maps
                    val gmmIntentUri = Uri.parse("geo:0,0?q=$lugarEvento")
                    val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                    mapIntent.setPackage("com.google.android.apps.maps") // Establecer el paquete de Google Maps

                    // Verificar si la aplicación de Google Maps está instalada
                    if (mapIntent.resolveActivity(requireActivity().packageManager) != null) {
                        // Abrir Google Maps si está instalado
                        startActivity(mapIntent)
                    } else {
                        // Notificar al usuario de que la aplicación de Google Maps no está instalada
                        showToast(R.string.google_maps_not_installed)
                    }
                }
            }
        }
        return view
    }

    private fun getEventFromFirebase(eventId: String, callback: (Evento?) -> Unit) {
        val databaseReference =
            FirebaseDatabase.getInstance("https://eventconnect-150ed-default-rtdb.europe-west1.firebasedatabase.app/").reference
        val eventosRef = databaseReference.child("eventos").child(eventId)

        eventosRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Verificar si existe un evento con el ID proporcionado
                if (snapshot.exists()) {
                    // Obtener los datos del evento
                    val ciudad = snapshot.child("ciudad").getValue(String::class.java) ?: ""
                    val date = snapshot.child("date").getValue(String::class.java) ?: ""
                    val info = snapshot.child("info").getValue(String::class.java) ?: ""
                    val link = snapshot.child("link").getValue(String::class.java) ?: ""
                    val lugar = snapshot.child("lugar").getValue(String::class.java) ?: ""
                    val name = snapshot.child("name").getValue(String::class.java) ?: ""
                    val imageUrl = snapshot.child("imagenUrl").getValue(String::class.java) ?: ""

                    // Crear un objeto Evento con los datos obtenidos
                    val evento = Evento(eventId, name, ciudad, lugar, link, info, date, imageUrl)
                    // Devolver el objeto Evento a través del callback
                    callback(evento)
                } else {
                    // El evento con el ID proporcionado no existe
                    callback(null)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Ocurrió un error al obtener los datos, devolver null
                callback(null)
            }
        })
    }


    private fun getSavedEventId(): String? {
        return sharedPreferences.getString(EVENT_ID_KEY, null)
    }

    private fun formatDateTime(dateTimeString: String): String {
        val inputFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val outputFormat = SimpleDateFormat("EEEE, MMMM d · HH:mm", Locale.getDefault())

        return try {
            val dateTime = inputFormat.parse(dateTimeString)
            outputFormat.format(dateTime)
        } catch (e: ParseException) {
            e.printStackTrace()
            ""
        }
    }
    private fun borrarEvento(eventId: String) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle(getString(R.string.confirmation))
        builder.setMessage(getString(R.string.confirm_delete_event))
        builder.setPositiveButton(getString(R.string.yes)) { dialogInterface: DialogInterface, _: Int ->
            // Borrar el evento de Firebase Realtime Database
            val databaseReference =
                FirebaseDatabase.getInstance("https://eventconnect-150ed-default-rtdb.europe-west1.firebasedatabase.app/").reference
            val eventosRef = databaseReference.child("eventos").child(eventId)
            eventosRef.removeValue()
                .addOnSuccessListener {
                    // El evento se borró correctamente
                    showToast(R.string.event_deleted_success)
                    val intent = Intent(requireContext(), AdminMenuActivity::class.java)
                    startActivity(intent)
                }
                .addOnFailureListener {
                    // Error al borrar el evento
                    showToast(R.string.delete_event_error)
                }
            dialogInterface.dismiss()
        }
        builder.setNegativeButton(getString(R.string.not)) { dialogInterface: DialogInterface, _: Int ->
            dialogInterface.dismiss()
        }
        builder.show()
    }
    private fun showToast(messageResId: Int) {
        Toast.makeText(requireContext(), getString(messageResId), Toast.LENGTH_SHORT).show()
    }

}
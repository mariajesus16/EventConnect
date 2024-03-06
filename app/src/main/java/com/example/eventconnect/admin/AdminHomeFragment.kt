package com.example.eventconnect.admin

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SearchView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
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
import java.text.SimpleDateFormat
import java.util.Locale


class AdminHomeFragment : Fragment() {
    private lateinit var sharedPreferences: SharedPreferences

    private val EVENT_ID_KEY = "eventId"
    private lateinit var scrollView: ScrollView
    private lateinit var linearLayout: LinearLayout
    private lateinit var database: FirebaseDatabase
    private lateinit var storageRef: StorageReference
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var eventosRef: DatabaseReference
    private lateinit var favoritesRef: DatabaseReference
    private lateinit var searchView: SearchView
    private var searchText: String = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        sharedPreferences =
            requireActivity().getSharedPreferences("event_prefs", Context.MODE_PRIVATE)
        firebaseAuth = FirebaseAuth.getInstance()
        // Obtén la referencia de Firebase Storage
        storageRef = FirebaseStorage.getInstance("gs://eventconnect-150ed.appspot.com").reference
        // Inicializa la base de datos
        database =
            FirebaseDatabase.getInstance("https://eventconnect-150ed-default-rtdb.europe-west1.firebasedatabase.app/")
        eventosRef = database.getReference("eventos")
        favoritesRef = database.getReference("favoritos")

        // Obtén referencias a tus vistas
        searchView = view.findViewById(R.id.searchView)
        scrollView = view.findViewById(R.id.scrollView)
        linearLayout = view.findViewById(R.id.dynamicContent)

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                // Este método se llama cuando se presiona el botón de búsqueda o se envía el texto de búsqueda.
                query?.let {
                    searchText = it
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                // Este método se llama cuando cambia el texto en el SearchView.
                newText?.let {
                    searchText = it
                }
                return true
            }
        })

        obtenerListaDeEventos { listaEventos ->
            if (listaEventos != null) {
                // La lista de eventos se obtuvo exitosamente
                // Itera sobre la lista de eventos y agrega dinámicamente los elementos al LinearLayout

                val listaEventosFiltrada = listaEventos.filter { it -> it.ciudad == searchText  }
                for (evento in listaEventos) {
                    // Crear el diseño de la tarjeta
                    val cardView = CardView(requireContext())
                    val cardLayoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    cardLayoutParams.setMargins(
                        resources.getDimensionPixelSize(R.dimen.card_margin_horizontal),
                        resources.getDimensionPixelSize(R.dimen.card_margin_vertical),
                        resources.getDimensionPixelSize(R.dimen.card_margin_horizontal),
                        resources.getDimensionPixelSize(R.dimen.card_margin_vertical)
                    )
                    cardView.layoutParams = cardLayoutParams
                    cardView.cardElevation = resources.getDimension(R.dimen.card_elevation)
                    cardView.radius = resources.getDimension(R.dimen.card_corner_radius)

                    // Configurar el diseño interno de la tarjeta
                    val cardContentLayout = LinearLayout(requireContext())
                    cardContentLayout.orientation = LinearLayout.VERTICAL

                    // Agregar la imagen del evento a la tarjeta
                    val imageView = ImageView(requireContext())
                    imageView.layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        resources.getDimensionPixelSize(R.dimen.card_image_height)
                    )
                    imageView.scaleType = ImageView.ScaleType.CENTER_CROP
                    cargarImagenEvento(evento.id!!) { imageUrl ->
                        if (!imageUrl.isNullOrEmpty()) {
                            // La URL de la imagen no está vacía, la cargamos en el ImageView
                            Picasso.get().load(imageUrl).into(imageView)
                        } else {
                            // La URL de la imagen es nula o vacía, cargamos una imagen de error
                            Picasso.get().load(R.drawable.logo).into(imageView)
                        }
                    }
                    cardContentLayout.addView(imageView)

                    // Agregar el nombre del evento a la tarjeta
                    val nameTextView = TextView(requireContext())
                    val nameLayoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    nameLayoutParams.setMargins(
                        resources.getDimensionPixelSize(R.dimen.card_text_margin_horizontal),
                        resources.getDimensionPixelSize(R.dimen.card_text_margin_vertical),
                        resources.getDimensionPixelSize(R.dimen.card_text_margin_horizontal),
                        resources.getDimensionPixelSize(R.dimen.card_text_margin_vertical)
                    )
                    nameTextView.layoutParams = nameLayoutParams
                    nameTextView.text = evento.name
                    nameTextView.gravity = Gravity.START // Alinear a la izquierda
                    nameTextView.textSize = resources.getDimension(R.dimen.card_text_name_size)
                    nameTextView.setTextColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.black
                        )
                    )
                    nameTextView.setTypeface(null, Typeface.BOLD)
                    cardContentLayout.addView(nameTextView)

                    // Agregar la fecha del evento a la tarjeta
                    val dateTextView = TextView(requireContext())
                    val dateLayoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    dateLayoutParams.setMargins(
                        resources.getDimensionPixelSize(R.dimen.card_text_margin_horizontal),
                        resources.getDimensionPixelSize(R.dimen.card_text_margin_vertical),
                        resources.getDimensionPixelSize(R.dimen.card_text_margin_horizontal),
                        resources.getDimensionPixelSize(R.dimen.card_text_margin_vertical)
                    )
                    dateTextView.layoutParams = dateLayoutParams
                    // Formatear la fecha
                    val inputFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                    val outputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    val formattedDate =
                        inputFormat.parse(evento.date!!)?.let { outputFormat.format(it) }

                    dateTextView.text = formattedDate
                    dateTextView.gravity = Gravity.START // Alinear a la izquierda
                    dateTextView.textSize = resources.getDimension(R.dimen.card_text_size)
                    dateTextView.setTextColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.black
                        )
                    )
                    cardContentLayout.addView(dateTextView)

                    // Agregar la ciudad del evento a la tarjeta
                    val cityTextView = TextView(requireContext())
                    val cityLayoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    cityLayoutParams.setMargins(
                        resources.getDimensionPixelSize(R.dimen.card_text_margin_horizontal),
                        resources.getDimensionPixelSize(R.dimen.card_text_margin_vertical),
                        resources.getDimensionPixelSize(R.dimen.card_text_margin_horizontal),
                        resources.getDimensionPixelSize(R.dimen.card_text_margin_vertical)
                    )
                    cityTextView.layoutParams = cityLayoutParams
                    cityTextView.text = evento.ciudad
                    cityTextView.gravity = Gravity.START // Alinear a la izquierda
                    cityTextView.textSize = resources.getDimension(R.dimen.card_text_size)
                    cityTextView.setTextColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.black
                        )
                    )
                    cardContentLayout.addView(cityTextView)

                    // botón de favorito a la tarjeta
                    /*
                    val favoriteButton = ImageView(requireContext())
                    val buttonLayoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    buttonLayoutParams.setMargins(
                        resources.getDimensionPixelSize(R.dimen.card_text_margin_horizontal),
                        resources.getDimensionPixelSize(R.dimen.card_text_margin_vertical),
                        resources.getDimensionPixelSize(R.dimen.card_text_margin_horizontal),
                        resources.getDimensionPixelSize(R.dimen.card_text_margin_vertical)
                    )
                    favoriteButton.layoutParams = buttonLayoutParams
                    Picasso.get().load(R.drawable.logo).into(favoriteButton)

                    cardContentLayout.addView(favoriteButton)
                     */
                    // Configurar el OnClickListener para la tarjeta
                    cardView.setOnClickListener {

                        saveEventId(evento.id!!)
                        val intent = Intent(requireActivity(), AdminEventActivity::class.java)
                        startActivity(intent)

                    }
                    // Agregar el diseño interno a la tarjeta
                    cardView.addView(cardContentLayout)

                    // Agregar la tarjeta al contenedor lineal
                    linearLayout.addView(cardView)
                }
            } else {
                // Ocurrió un error al obtener la lista de eventos
            }
        }

        return view
    }

    // Función para obtener la lista de eventos desde Firebase Realtime Database
    fun obtenerListaDeEventos(callback: (List<Evento>?) -> Unit) {
        val databaseReference = FirebaseDatabase.getInstance("https://eventconnect-150ed-default-rtdb.europe-west1.firebasedatabase.app/").reference
        val eventosRef = databaseReference.child("eventos")
        eventosRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val listaEventos = mutableListOf<Evento>()
                for (eventSnapshot in snapshot.children) {
                    val id = eventSnapshot.key ?: ""
                    val ciudad = eventSnapshot.child("ciudad").getValue(String::class.java)
                    val date = eventSnapshot.child("date").getValue(String::class.java)
                    val info = eventSnapshot.child("info").getValue(String::class.java)
                    val link = eventSnapshot.child("link").getValue(String::class.java)
                    val lugar = eventSnapshot.child("lugar").getValue(String::class.java)
                    val name = eventSnapshot.child("name").getValue(String::class.java)
                    val imageUrl = eventSnapshot.child("imagenUrl").getValue(String::class.java)

                    // Verificar que todos los campos necesarios no sean nulos o vacíos
                    if (id.isNotEmpty() && ciudad != null && date != null && info != null &&
                        link != null && lugar != null && name != null && imageUrl != null
                    ) {
                        // Crear un objeto Evento con los datos obtenidos y agregarlo a la lista
                        val evento = Evento(id, name, ciudad, lugar, link, info, date, imageUrl)
                        listaEventos.add(evento)
                    }
                }
                callback(listaEventos)
                // Agregar log para imprimir los eventos obtenidos
                listaEventos.forEach {
                    println("Evento: $it")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                callback(null)
            }
        })
    }



    fun guardarFavorito(userId: String?, eventId: String) {
        val favoriteId = favoritesRef.push().key // Generar una nueva clave para el favorito

        // Verificar si se ha proporcionado un ID de usuario válido
        if (userId != null && !userId.isEmpty()) {

            // Guardar el favorito en la base de datos
            favoritesRef.child(favoriteId ?: "").child("userId").setValue(userId)
            favoritesRef.child(favoriteId ?: "").child("eventId").setValue(eventId)
                .addOnSuccessListener {
                    // La relación de favoritos se guardó exitosamente
                    // Aquí puedes mostrar un mensaje de éxito si lo deseas
                }
                .addOnFailureListener { exception ->
                    // Error al guardar el favorito
                    // Aquí puedes manejar el error adecuadamente
                }
        }
    }

    // Función para cargar la URL de la imagen de un evento desde Firebase Realtime Database
    fun cargarImagenEvento(eventoId: String, listener: (String?) -> Unit) {
        val database = FirebaseDatabase.getInstance()
        val eventoRef = database.reference.child("eventos").child(eventoId)

        eventoRef.child("imagenUrl").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val imageUrl = dataSnapshot.value as String?
                listener(imageUrl)
            }

            override fun onCancelled(databaseError: DatabaseError) {
                databaseError.toException().printStackTrace()
                listener(null)
            }
        })
    }

    // Método para guardar el eventId en SharedPreferences
    private fun saveEventId(eventId: String) {
        val editor = sharedPreferences.edit()
        editor.putString(EVENT_ID_KEY, eventId)
        editor.apply()
    }

}
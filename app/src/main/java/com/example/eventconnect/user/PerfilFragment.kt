package com.example.eventconnect.user

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.exifinterface.media.ExifInterface
import androidx.fragment.app.Fragment
import com.example.eventconnect.CircleTransform
import com.example.eventconnect.Evento
import com.example.eventconnect.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * A simple [Fragment] subclass.
 * Use the [PerfilFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class PerfilFragment : Fragment() {
    private lateinit var profileImageView: ImageView
    private lateinit var database: FirebaseDatabase
    private lateinit var userId: String
    private val PICK_IMAGE_REQUEST = 1
    private lateinit var scrollView: HorizontalScrollView
    private lateinit var linearLayout: LinearLayout

    private lateinit var storageRef: StorageReference
    private lateinit var firebaseAuth: FirebaseAuth

    private lateinit var profileNameTextView: TextView
    private lateinit var profileEmailTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_perfil, container, false)
        scrollView = view.findViewById(R.id.horizontalScrollView)
        linearLayout = view.findViewById(R.id.dynamicContent)

        profileImageView = view.findViewById(R.id.profileImageView)
        profileImageView.scaleType = ImageView.ScaleType.CENTER_CROP

        profileNameTextView = view.findViewById(R.id.profileNameTextView)
        profileEmailTextView = view.findViewById(R.id.profileEmailTextView)

        profileImageView.setOnClickListener {
            val intent = Intent()
            intent.type = "image/*"
            intent.action = Intent.ACTION_GET_CONTENT
            startActivityForResult(
                Intent.createChooser(intent, "Select Image"),
                PICK_IMAGE_REQUEST
            )
        }

        firebaseAuth = FirebaseAuth.getInstance()
        // Obtén la referencia de Firebase Storage
        storageRef = FirebaseStorage.getInstance("gs://eventconnect-150ed.appspot.com").reference
        // Inicializa la base de datos
        database = FirebaseDatabase.getInstance("https://eventconnect-150ed-default-rtdb.europe-west1.firebasedatabase.app/")
        // Obtener userId de las preferencias compartidas
        val sharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(requireContext())
        userId = sharedPreferences.getString("userId", "") ?: ""

        // Cargar la imagen del perfil
        cargarImagenPerfilUsuario(userId)
        // Cargar el nombre y correo electrónico del perfil
        cargarDatosPerfilUsuario(userId)

        obtenerEventosFavoritos(userId) { listaEventos ->
            if (listaEventos != null) {
                for (evento in listaEventos) {
                    // Crear el diseño de la tarjeta
                    val cardView = CardView(requireContext())
                    val cardLayoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    cardLayoutParams.setMargins(
                        resources.getDimensionPixelSize(R.dimen.card_margin_horizontal_perfil),
                        resources.getDimensionPixelSize(R.dimen.card_margin_vertical_peril),
                        resources.getDimensionPixelSize(R.dimen.card_margin_horizontal_perfil),
                        resources.getDimensionPixelSize(R.dimen.card_margin_vertical_peril)
                    )
                    cardView.layoutParams = cardLayoutParams
                    cardView.cardElevation = resources.getDimension(R.dimen.card_elevation)
                    cardView.radius = resources.getDimension(R.dimen.card_corner_radius)

                    // Configurar el diseño interno de la tarjeta
                    val cardContentLayout = LinearLayout(requireContext())
                    cardContentLayout.orientation = LinearLayout.HORIZONTAL

                    // Agregar la imagen del evento a la tarjeta
                    val imageView = ImageView(requireContext())
                    imageView.layoutParams = ViewGroup.LayoutParams(
                        resources.getDimensionPixelSize(R.dimen.card_image_width_perfil),
                        resources.getDimensionPixelSize(R.dimen.card_image_height)
                    )
                    imageView.scaleType = ImageView.ScaleType.CENTER_CROP
                    // Agregar un indicador de carga (ProgressBar)
                    val progressBar = ProgressBar(requireContext())
                    val progressBarParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )

                    progressBarParams.gravity = Gravity.CENTER
                    progressBar.layoutParams = progressBarParams
                    cardContentLayout.addView(progressBar)

                    cargarImagenEvento(evento.id!!) { imageUrl ->
                        if (!imageUrl.isNullOrEmpty()) {
                            // La URL de la imagen no está vacía, la cargamos en el ImageView
                            Picasso.get().load(imageUrl).into(imageView, object : Callback {
                                override fun onSuccess() {
                                    // Si la carga es exitosa, oculta el indicador de carga
                                    progressBar.visibility = View.GONE
                                }

                                override fun onError(e: Exception?) {
                                    // Si hay un error en la carga, también oculta el indicador de carga
                                    progressBar.visibility = View.GONE
                                }
                            })
                        } else {
                            // La URL de la imagen es nula o vacía, cargamos una imagen de error
                            progressBar.visibility = View.GONE
                            Picasso.get().load(R.drawable.logo).into(imageView)
                        }
                    }

                    cardContentLayout.addView(imageView)

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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.data != null) {
            val selectedImageUri: Uri = data.data!!

            try {
                // Obtener la orientación de la imagen
                val inputStream = requireContext().contentResolver.openInputStream(selectedImageUri)
                val exif = ExifInterface(inputStream!!)
                val orientation =
                    exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)

                // Obtener el ángulo de rotación necesario
                val rotation = when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270
                    else -> 0
                }

                // Rotar la imagen antes de cargarla
                val rotatedBitmap = rotateBitmap(selectedImageUri, rotation)

                // Guardar la imagen rotada en un archivo temporal
                val rotatedImageFile = saveBitmapToTempFile(rotatedBitmap)

                // Cargar la imagen utilizando Picasso y aplicar la transformación circular
                Picasso.get()
                    .load(rotatedImageFile)
                    .transform(CircleTransform())
                    .into(profileImageView)

                // Actualizar la foto de perfil del usuario en Firebase
                actualizarFotoPerfilUsuario(userId, rotatedBitmap)

                showToast(getString(R.string.profile_photo_updated_success))

            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    // Actualizar la foto de perfil del usuario en Firebase
    fun actualizarFotoPerfilUsuario(userId: String?, bitmap: Bitmap) {
        // Verificar que el userId no sea nulo
        if (userId != null) {
            // Subir la imagen a Firebase Storage y obtener la URL de descarga
            val storageRef = FirebaseStorage.getInstance().reference.child("profileImages/$userId")
            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
            val imageData: ByteArray = baos.toByteArray()

            storageRef.putBytes(imageData)
                .addOnSuccessListener { taskSnapshot ->
                    taskSnapshot.storage.downloadUrl.addOnSuccessListener { uri ->
                        val imageUrl = uri.toString()

                        // Actualizar la URL de la foto de perfil en Firebase Realtime Database
                        database.reference.child("usuarios").child(userId).child("fotoPerfil")
                            .setValue(imageUrl)
                            .addOnSuccessListener {
                                // Foto de perfil actualizada correctamente
                            }
                            .addOnFailureListener { e ->
                                // Error al actualizar la foto de perfil
                                e.printStackTrace()
                            }
                    }
                }
                .addOnFailureListener { e ->
                    // Error al subir la imagen a Firebase Storage
                    e.printStackTrace()
                }
        }
    }

    // Función para cargar la imagen de perfil del usuario desde Firebase Realtime Database
    fun cargarImagenPerfilUsuario(userId: String) {
        // Lee la URL de la foto de perfil del usuario desde Firebase Realtime Database
        database.reference.child("usuarios").child(userId).child("fotoPerfil")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val imageUrl = dataSnapshot.value as String?

                    // Carga de imagen en la ImageView
                    if (!imageUrl.isNullOrEmpty()) {
                        // Carga la imagen utilizando Picasso y aplica la transformación circular
                        Picasso.get()
                            .load(imageUrl)
                            .transform(CircleTransform())
                            .into(profileImageView)
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // Error al leer la URL de la foto de perfil del usuario desde Firebase Realtime Database
                    databaseError.toException().printStackTrace()
                }
            })
    }

    // Función para mostrar un Toast
    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
    private fun rotateBitmap(uri: Uri, degrees: Int): Bitmap {
        val inputStream = requireContext().contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun saveBitmapToTempFile(bitmap: Bitmap): File {
        val tempFile = File.createTempFile("temp_image", ".jpg", requireContext().cacheDir)
        val outputStream = FileOutputStream(tempFile)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        outputStream.flush()
        outputStream.close()
        return tempFile
    }

    // Función para cargar el nombre y el correo electrónico del usuario desde Firebase Realtime Database
    private fun cargarDatosPerfilUsuario(userId: String) {
        // Lee el nombre y el correo electrónico del usuario desde Firebase Realtime Database
        database.reference.child("usuarios").child(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val nombrePerfil = dataSnapshot.child("nombrePerfil").value as String?
                    val correo = dataSnapshot.child("correo").value as String?

                    // Mostrar el nombre y el correo electrónico en los TextView
                    if (!nombrePerfil.isNullOrEmpty() && !correo.isNullOrEmpty()) {
                        profileNameTextView.text = nombrePerfil
                        profileEmailTextView.text = correo
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // Error al leer el nombre y el correo electrónico del usuario desde Firebase Realtime Database
                    databaseError.toException().printStackTrace()
                }
            })
    }

    fun obtenerEventosFavoritos(userId: String, callback: (List<Evento>?) -> Unit) {
        val databaseReference = FirebaseDatabase.getInstance("https://eventconnect-150ed-default-rtdb.europe-west1.firebasedatabase.app/").reference
        val favoritosRef = databaseReference.child("favoritos").child(userId)

        favoritosRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val eventIdList = mutableListOf<String>()

                // Iterar sobre todas las entradas de la tabla de favoritos
                for (favSnapshot in snapshot.children) {
                    val eventId = favSnapshot.key // Obtiene el eventId de la entrada

                    // Verificar si el valor es true y el eventId no es nulo
                    if (eventId != null && favSnapshot.value as? Boolean == true) {
                        eventIdList.add(eventId) // Agregar el eventId a la lista
                    }
                }

                // Lista para almacenar los eventos
                val listaEventos = mutableListOf<Evento>()

                // Iterar sobre cada eventId y obtener el evento correspondiente
                for (eventId in eventIdList) {
                    getEventFromFirebase(eventId) { evento ->
                        if (evento != null) {
                            listaEventos.add(evento)
                        }
                        // Verificar si se han agregado todos los eventos
                        if (listaEventos.size == eventIdList.size) {
                            // Devolver la lista de eventos a través del callback
                            callback(listaEventos)
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Manejar la cancelación de la operación
                callback(null)
            }
        })
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

    fun cargarImagenEvento(eventoId: String, listener: (String?) -> Unit) {
        val database =
            FirebaseDatabase.getInstance("https://eventconnect-150ed-default-rtdb.europe-west1.firebasedatabase.app/")
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
}

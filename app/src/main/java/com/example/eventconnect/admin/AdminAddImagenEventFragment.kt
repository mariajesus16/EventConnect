package com.example.eventconnect.admin

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.eventconnect.Evento
import com.example.eventconnect.LocaleHelper
import com.example.eventconnect.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import androidx.appcompat.widget.Toolbar
import androidx.exifinterface.media.ExifInterface
import com.example.eventconnect.CircleTransform
import com.example.eventconnect.user.EventActivity
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.squareup.picasso.Picasso
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class AdminAddImagenEventFragment : Fragment() {
    private lateinit var localeHelper: LocaleHelper
    private lateinit var toolbar: Toolbar
    private var activityListener: ActivityListener? = null
    private lateinit var database: FirebaseDatabase
    private lateinit var storageRef: StorageReference
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var eventosRef: DatabaseReference

    private lateinit var imagenEvento : ImageView
    private lateinit var btnAnterior : Button
    private lateinit var btnAddEvent : Button
    private var rotatedBitmap : Bitmap? = null
    private val PICK_IMAGE_REQUEST = 1

    private var evento: Evento? = null
    interface ActivityListener {
        fun restartActivity()
    }

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Obtener el objeto evento de los argumentos
        arguments?.let {
            evento = it.getParcelable("evento")
        }
        val view = inflater.inflate(R.layout.fragment_admin_add_imagen_event, container, false)
        toolbar = view.findViewById(R.id.toolbarAddEvent)
        localeHelper = LocaleHelper()

        val savedLanguage = LocaleHelper.getLanguage(requireContext())
        savedLanguage?.let { LocaleHelper.setLocale(requireContext(), it) }

        // Configurar el selector de idioma en el Toolbar
        toolbar.inflateMenu(R.menu.menu_language_selector)
        toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_english -> {
                    LocaleHelper.persist(requireContext(), "en")
                    LocaleHelper.setLocale(requireContext(), "en")
                    activityListener?.restartActivity() // Llamar al método en la actividad para reiniciarla
                }
                R.id.action_spanish -> {
                    LocaleHelper.persist(requireContext(), "es")
                    LocaleHelper.setLocale(requireContext(), "es")
                    activityListener?.restartActivity() // Llamar al método en la actividad para reiniciarla
                }
            }
            true
        }

        firebaseAuth = FirebaseAuth.getInstance()
        // Obtén la referencia de Firebase Storage
        storageRef = FirebaseStorage.getInstance("gs://eventconnect-150ed.appspot.com").reference
        // Inicializa la base de datos
        database =
            FirebaseDatabase.getInstance("https://eventconnect-150ed-default-rtdb.europe-west1.firebasedatabase.app/")
        eventosRef = database.getReference("eventos")

        imagenEvento = view.findViewById(R.id.ImageEvent)
        btnAddEvent = view.findViewById(R.id.btnAddEvent)
        btnAnterior = view.findViewById(R.id.btnAnterior)

        btnAnterior.setOnClickListener {
            // Crear una instancia del nuevo fragmento y establecer los argumentos
            val adminAddEventFragment = AdminAddEventFragment()

            // Reemplazar el fragmento actual con el nuevo fragmento
            val transaction = requireActivity().supportFragmentManager.beginTransaction()
            transaction.replace(R.id.fragment_container, adminAddEventFragment)
            transaction.addToBackStack(null)  // Para que el usuario pueda regresar al fragmento anterior
            transaction.commit()
        }


        imagenEvento.setOnClickListener {
            val intent = Intent()
            intent.type = "image/*"
            intent.action = Intent.ACTION_GET_CONTENT
            startActivityForResult(
                Intent.createChooser(intent, "Select Image"),
                PICK_IMAGE_REQUEST
            )
        }

        if (evento!!.id == ""){
            // Cargar la imagen del evento
            cargarImagenEvento(evento!!.id!!)
        }

        btnAddEvent.setOnClickListener {
            addEvent(evento!!)
            // Actualizar la foto de perfil del usuario en Firebase
            actualizarImagenEvento(evento!!.id, rotatedBitmap!!)

            showToast(R.string.profile_photo_updated_success)
            val intent = Intent(requireActivity(), AdminMenuActivity::class.java)
            startActivity(intent)
        }

        return view
    }

    override fun onDetach() {
        super.onDetach()
        activityListener = null // Evitar referencias a la actividad después de la desconexión
    }

    private fun addEvent(evento: Evento) {
        val databaseReference = FirebaseDatabase.getInstance("https://eventconnect-150ed-default-rtdb.europe-west1.firebasedatabase.app/").reference
        val eventosRef = databaseReference.child("eventos")

        // Generar un nuevo ID para el evento
        val eventoId = eventosRef.push().key

        // Verificar si se generó correctamente el ID
        if (eventoId != null) {
            // Establecer el ID del evento
            evento.id = eventoId

            // Guardar el evento en Firebase Realtime Database
            eventosRef.child(eventoId).setValue(evento)
                .addOnSuccessListener {
                    // Éxito al agregar el evento
                    Log.d("FirebaseDatabase", "Evento agregado correctamente en Firebase.")
                    showToast(R.string.event_added_success)
                }
                .addOnFailureListener { e ->
                    // Error al agregar el evento
                    Log.e("FirebaseDatabaseError", "Error al agregar el evento: $e")
                    showToast(R.string.event_add_error)
                }
        } else {
            // Error al generar el ID del evento
            Log.e("FirebaseDatabaseError", "Error al generar el ID del evento.")
        }
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
                rotatedBitmap = rotateBitmap(selectedImageUri, rotation)

                // Guardar la imagen rotada en un archivo temporal
                val rotatedImageFile = saveBitmapToTempFile(rotatedBitmap!!)

                // Cargar la imagen utilizando Picasso y aplicar la transformación circular
                Picasso.get()
                    .load(rotatedImageFile)
                    .into(imagenEvento)


            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
    fun actualizarImagenEvento(eventoId: String?, bitmap: Bitmap) {
        // Verificar que el eventoId no sea nulo
        if (eventoId != null) {
            // Subir la imagen a Firebase Storage y obtener la URL de descarga
            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
            val imageData: ByteArray = baos.toByteArray()

            storageRef.child("eventsImages/$eventoId").putBytes(imageData)
                .addOnSuccessListener { taskSnapshot ->
                    taskSnapshot.storage.downloadUrl.addOnSuccessListener { uri ->
                        val imageUrl = uri.toString()

                        // Actualizar la URL de la foto de perfil en Firebase Realtime Database
                        eventosRef.child(eventoId).child("imagenUrl")
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
    fun cargarImagenEvento(eventId: String) {
        val databaseReference = FirebaseDatabase.getInstance("https://eventconnect-150ed-default-rtdb.europe-west1.firebasedatabase.app/").reference
        val eventosRef = databaseReference.child("eventos")

        // Lee la URL de la foto de perfil del usuario desde Firebase Realtime Database
        eventosRef.child(eventId).child("imagenUrl")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val imageUrl = dataSnapshot.value as String?

                    // Carga de imagen en la ImageView
                    if (!imageUrl.isNullOrEmpty()) {
                        // Carga la imagen utilizando Picasso y aplica la transformación circular
                        Picasso.get()
                            .load(imageUrl)
                            .into(imagenEvento)
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // Error al leer la URL de la foto de perfil del usuario desde Firebase Realtime Database
                    databaseError.toException().printStackTrace()
                }
            })
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
    private fun showToast(messageResId: Int) {
        Toast.makeText(requireContext(), getString(messageResId), Toast.LENGTH_SHORT).show()
    }
}


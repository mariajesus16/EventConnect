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

    private val PICK_IMAGE_REQUEST = 1
    private var selectedImageUri: Uri? = null
    private var rotatedBitmap : Bitmap? = null

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
                Intent.createChooser(intent, getString(R.string.select_image)),
                PICK_IMAGE_REQUEST
            )
        }
        btnAddEvent.isEnabled = false

        btnAddEvent.setOnClickListener {
            // Generar un nuevo ID para el evento
            val eventoId = eventosRef.push().key
            // Aquí deberías obtener la URI de la imagen seleccionada
            if (selectedImageUri != null) {
                // Subir la imagen a Firebase Storage
                uploadImageToFirebaseStorage(eventoId,selectedImageUri!!) { imageUrl ->
                    // Una vez que se ha completado la subida de la imagen y se ha obtenido la URL de descarga
                    if (!imageUrl.isNullOrEmpty()) {
                        val intent = Intent(requireActivity(), AdminMenuActivity::class.java)
                        startActivity(intent)

                        // Actualizar la URL de la imagen en el evento
                        evento?.imagenUrl = imageUrl

                        // Agregar el evento a Firebase Realtime Database
                        addEvent(evento!!,eventoId)


                    } else {
                        //showToast(R.string.image_upload_error)
                    }
                }
            } else {
                //showToast(R.string.image_selection_error)
            }
        }

        return view
    }

    override fun onDetach() {
        super.onDetach()
        activityListener = null // Evitar referencias a la actividad después de la desconexión
    }

    private fun addEvent(evento: Evento,eventoId : String?) {
        val databaseReference = FirebaseDatabase.getInstance("https://eventconnect-150ed-default-rtdb.europe-west1.firebasedatabase.app/").reference
        val eventosRef = databaseReference.child("eventos")

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
            selectedImageUri = data.data!!

            try {
                // Obtener la orientación de la imagen
                val inputStream = requireContext().contentResolver.openInputStream(selectedImageUri!!)
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
                rotatedBitmap = rotateBitmap(selectedImageUri!!, rotation)

                imagenEvento.setImageBitmap(rotatedBitmap)

                btnAddEvent.isEnabled = true

            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
    private fun uploadImageToFirebaseStorage(eventoId :String?,imageUri: Uri, callback: (String?) -> Unit) {
        // Verificar que el ID del evento no sea nulo
        if (eventoId != null) {
            // Generar un nombre único para la imagen en Firebase Storage
            val storageRef = FirebaseStorage.getInstance("gs://eventconnect-150ed.appspot.com").reference

            // Crear una referencia específica para la imagen del evento dentro de la carpeta eventsImages
            val ref = storageRef.child("eventsImages/${eventoId}.jpg")

            // Subir la imagen a Firebase Storage
            ref.putFile(imageUri)
                .addOnSuccessListener { taskSnapshot ->
                    // Obtener la URL de descarga de la imagen subida
                    ref.downloadUrl.addOnSuccessListener { uri ->
                        val imageUrl = uri.toString()
                        callback(imageUrl) // Llamar a la devolución de llamada con la URL de descarga
                    }
                }
                .addOnFailureListener { e ->
                    // Manejar errores de subida de la imagen
                    e.printStackTrace()
                    callback(null) // Llamar a la devolución de llamada con un valor nulo en caso de error
                }
        } else {
            // Manejar el caso en el que el ID del evento sea nulo
            Log.e("FirebaseStorageError", "El ID del evento es nulo.")
            callback(null) // Llamar a la devolución de llamada con un valor nulo
        }
    }


    private fun rotateBitmap(uri: Uri, degrees: Int): Bitmap {
        val inputStream = requireContext().contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
    private fun showToast(messageResId: Int) {
        Toast.makeText(requireContext(), getString(messageResId), Toast.LENGTH_SHORT).show()
    }
}


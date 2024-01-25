package com.example.eventconnect

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceManager
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.exifinterface.media.ExifInterface
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.squareup.picasso.Picasso
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [PerfilFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class PerfilFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    private lateinit var profileImageView: ImageView
    private lateinit var database: FirebaseDatabase
    private lateinit var userId: String
    private val PICK_IMAGE_REQUEST = 1

    private lateinit var storageRef: StorageReference
    private lateinit var firebaseAuth: FirebaseAuth

    private lateinit var profileNameTextView: TextView
    private lateinit var profileEmailTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_perfil, container, false)
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
        return view
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment PerfilFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            PerfilFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
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

                showToast("Foto de perfil actualizada correctamente")

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
}

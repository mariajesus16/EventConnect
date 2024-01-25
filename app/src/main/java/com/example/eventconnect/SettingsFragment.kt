package com.example.eventconnect

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [SettingsFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class SettingsFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }
    private lateinit var cerrarSesion: TextView
    private lateinit var borrarCuenta: TextView
    private lateinit var database: FirebaseDatabase
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var usersRef: DatabaseReference
    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)
        cerrarSesion = view.findViewById(R.id.tCerrarSesion)
        cerrarSesion.setOnClickListener {
            val intent = Intent(requireContext(), LoginActivity::class.java)
            startActivity(intent)

            showToast(getString(R.string.logout_success))
        }

        // Inicializar Firebase Authentication
        firebaseAuth = FirebaseAuth.getInstance()

        // Obtener referencia a la base de datos "usuarios"
        database =
            FirebaseDatabase.getInstance("https://eventconnect-150ed-default-rtdb.europe-west1.firebasedatabase.app/")
        usersRef = database.getReference("usuarios")

        borrarCuenta = view.findViewById(R.id.tBorrarCuenta)
        borrarCuenta.setOnClickListener {
            borrarCuentaOnClick()
        }
        return view
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment SettingsFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            SettingsFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
    // Función para configurar el toolbar
    private fun setupToolbar(title: String) {
        val activity = requireActivity() as AppCompatActivity
        val toolbar: Toolbar = activity.findViewById(R.id.toolbar)
        activity.setSupportActionBar(toolbar)

        val actionBar: ActionBar? = activity.supportActionBar
        if (actionBar != null) {
            val titleTextView: TextView = toolbar.findViewById(R.id.toolbar_title)
            titleTextView.text = title

            val params: Toolbar.LayoutParams? = titleTextView.layoutParams as? Toolbar.LayoutParams
            params?.gravity = Gravity.CENTER
            titleTextView.layoutParams = params

            toolbar.setNavigationOnClickListener {
                activity.finish()
            }
        }
    }
    // Función para borrar la cuenta del usuario
    private fun borrarCuentaOnClick() {
        val sharedPreferences: SharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(context)
        val userId = sharedPreferences.getString("userId", "") ?: ""

        val dialogBuilder = context?.let {
            AlertDialog.Builder(it)
                .setTitle(getString(R.string.confirmation))
                .setMessage(getString(R.string.confirm_delete_account))
                .setPositiveButton(getString(R.string.delete)) { dialog, which ->
                    // Eliminar la cuenta de Firebase Authentication
                    val firebaseUser = FirebaseAuth.getInstance().currentUser
                    firebaseUser?.delete()?.addOnCompleteListener { deleteTask ->
                        if (deleteTask.isSuccessful) {
                            // Borrar la caché local del userId en SharedPreferences
                            sharedPreferences.edit().remove(userId).apply()

                            // Restablecer userId a una cadena vacía
                            sharedPreferences.edit().putString("userId", "").apply()

                            // Eliminar la entrada correspondiente en la tabla de usuarios
                            usersRef.child(userId).removeValue()

                            showToast(getString(R.string.account_deleted_success))

                            val intent = Intent(requireContext(), LoginActivity::class.java)
                            startActivity(intent)
                        }
                    }
                }
                .setNegativeButton(getString(R.string.cancel), null)
        }

        dialogBuilder?.create()?.show()
    }


    // Función para mostrar un Toast
    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}

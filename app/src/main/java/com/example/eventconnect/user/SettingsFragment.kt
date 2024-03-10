package com.example.eventconnect.user

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
import com.example.eventconnect.LoginActivity
import com.example.eventconnect.MainActivity
import com.example.eventconnect.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import android.provider.Settings
class SettingsFragment : Fragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }
    private lateinit var cerrarSesion: TextView
    private lateinit var borrarCuenta: TextView
    private lateinit var notificaciones : TextView
    private lateinit var database: FirebaseDatabase
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var usersRef: DatabaseReference
    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)
        // Cerrar Sesion
        cerrarSesion = view.findViewById(R.id.tCerrarSesion)
        cerrarSesion.setOnClickListener {
            val sharedPreferences: SharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(context)
            val userId = sharedPreferences.getString("userId", "") ?: ""
            // Borrar la caché local del userId en SharedPreferences
            sharedPreferences.edit().remove(userId).apply()

            // Restablecer userId a una cadena vacía
            sharedPreferences.edit().putString("userId", "").apply()

            val intent = Intent(requireContext(), MainActivity::class.java)
            startActivity(intent)

            showToast(getString(R.string.logout_success))
        }

        // Inicializar Firebase Authentication
        firebaseAuth = FirebaseAuth.getInstance()

        // Obtener referencia a la base de datos "usuarios"
        database =
            FirebaseDatabase.getInstance("https://eventconnect-150ed-default-rtdb.europe-west1.firebasedatabase.app/")
        usersRef = database.getReference("usuarios")

        // Borrar cuenta
        borrarCuenta = view.findViewById(R.id.tBorrarCuenta)
        borrarCuenta.setOnClickListener {
            borrarCuentaOnClick()
        }
        
        // Notificaciones
        notificaciones = view.findViewById(R.id.tNotificaciones)
        notificaciones.setOnClickListener {
            showNotificationSettingsDialog()
        }
        return view
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

    // Método para mostrar un diálogo de confirmación antes de redirigir al usuario a la configuración
    private fun showNotificationSettingsDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Notificaciones desactivadas")
        builder.setMessage("Las notificaciones de esta aplicación están desactivadas. ¿Desea habilitarlas ahora?")
        builder.setPositiveButton("Sí") { _, _ ->
            // Llamar a la función para abrir la configuración de notificaciones
            openNotificationSettings()
        }
        builder.setNegativeButton("No") { dialog, _ ->
            dialog.dismiss()
        }
        builder.show()
    }

    // Método para abrir la configuración de notificaciones
    private fun openNotificationSettings() {
        val intent = Intent()
        intent.action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
        intent.putExtra(Settings.EXTRA_APP_PACKAGE, requireContext().packageName)
        startActivity(intent)
    }
    // Función para mostrar un Toast
    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}

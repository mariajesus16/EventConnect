package com.example.eventconnect.admin

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
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
import java.util.Calendar
import androidx.appcompat.widget.Toolbar

class AdminAddEventFragment : Fragment() {
    private lateinit var localeHelper: LocaleHelper
    private lateinit var toolbar: Toolbar
    private var activityListener: ActivityListener? = null
    private lateinit var database: FirebaseDatabase
    private lateinit var storageRef: StorageReference
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var eventosRef: DatabaseReference

    private lateinit var etNombre: EditText
    private lateinit var etCiudad: EditText
    private lateinit var etLugar: EditText
    private lateinit var etFecha: EditText
    private lateinit var etLink: EditText
    private lateinit var etInfo: EditText
    private lateinit var btnNext : Button

    private var evento: Evento? = null
    interface ActivityListener {
        fun restartActivity()
    }

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_admin_add_event, container, false)
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

        etNombre = view.findViewById(R.id.etNombreEvento)
        etCiudad = view.findViewById(R.id.etCiudadEvento)
        etLugar = view.findViewById(R.id.etLugarEvento)
        etFecha = view.findViewById(R.id.etFechaEvento)
        etLink = view.findViewById(R.id.etLinkEvento)
        etInfo = view.findViewById(R.id.etInformacionEvento)
        btnNext = view.findViewById(R.id.btnNext)

        validarCamposRellenos()

        etFecha.setOnClickListener {
            showDateTimePickerDialog()
        }

        // Inicializar el objeto evento antes de acceder a sus miembros
        evento = Evento("", "", "", "", "", "", "", "")

        btnNext.setOnClickListener {
            evento!!.id = ""
            evento!!.name = etNombre.text.toString().trim()
            evento!!.ciudad = etCiudad.text.toString().trim()
            evento!!.lugar = etLugar.text.toString().trim()
            evento!!.link = etLink.text.toString().trim()
            evento!!.info = etInfo.text.toString().trim()
            evento!!.date = etFecha.text.toString().trim()
            evento!!.imagenUrl = ""

            // Crear un Bundle y agregar el evento como argumento
            val bundle = Bundle().apply {
                putParcelable("evento", evento)
            }

            // Crear una instancia del nuevo fragmento y establecer los argumentos
            val adminAddImagenEventFragment = AdminAddImagenEventFragment()
            adminAddImagenEventFragment.arguments = bundle

            // Reemplazar el fragmento actual con el nuevo fragmento
            val transaction = requireActivity().supportFragmentManager.beginTransaction()
            transaction.replace(R.id.fragment_container, adminAddImagenEventFragment)
            transaction.addToBackStack(null)  // Para que el usuario pueda regresar al fragmento anterior
            transaction.commit()
        }


        return view
    }

    override fun onDetach() {
        super.onDetach()
        activityListener = null // Evitar referencias a la actividad después de la desconexión
    }
    private fun validarCamposRellenos() {
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // No se requiere implementación
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val ciudad = etCiudad.text.toString().trim()
                val lugar = etLugar.text.toString().trim()
                val nombre = etNombre.text.toString().trim()
                val link = etLink.text.toString().trim()
                val info = etInfo.text.toString().trim()
                val date = etFecha.text.toString().trim()

                btnNext.isEnabled = ciudad.isNotEmpty() &&
                        lugar.isNotEmpty() && nombre.isNotEmpty() &&
                        link.isNotEmpty() && info.isNotEmpty() && date.isNotEmpty()
            }

            override fun afterTextChanged(s: Editable?) {
                // No se requiere implementación
            }
        }

        etCiudad.addTextChangedListener(textWatcher)
        etLugar.addTextChangedListener(textWatcher)
        etNombre.addTextChangedListener(textWatcher)
        etLink.addTextChangedListener(textWatcher)
        etInfo.addTextChangedListener(textWatcher)
        etFecha.addTextChangedListener(textWatcher)

        btnNext.isEnabled = etCiudad.text.isNotEmpty() &&
                etLugar.text.isNotEmpty() && etNombre.text.isNotEmpty() &&
                etLink.text.isNotEmpty() && etInfo.text.isNotEmpty() && etFecha.text.isNotEmpty()
    }
    private fun showDateTimePickerDialog() {
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)

        val dateTimePickerDialog = DatePickerDialog(requireContext(), { _, year, month, day ->
            val timePickerDialog = TimePickerDialog(requireContext(), { _, hourOfDay, minute ->
                val formattedDateTime = String.format("%02d/%02d/%04d %02d:%02d", day, month + 1, year, hourOfDay, minute)
                etFecha.setText(formattedDateTime)
            }, currentHour, currentMinute, true)

            timePickerDialog.show()
        }, currentYear, currentMonth, currentDay)

        dateTimePickerDialog.show()
    }

    private fun showToast(messageResId: Int) {
        Toast.makeText(requireContext(), getString(messageResId), Toast.LENGTH_SHORT).show()
    }
}


package com.example.sentycare

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.outlined.PersonOff
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Bed
import androidx.compose.ui.draw.clip
import com.example.sentycare.ui.theme.*
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

// Importamos SesionState para trazabilidad de quién registró el paciente

private enum class LookupState { IDLE, LOADING, ACTIVE, INACTIVE, NOT_FOUND }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onBackClick: () -> Unit = {},
    onCancelClick: () -> Unit = {},
    onRegisterClick: (Patient) -> Unit = {}   // mismo nombre que en MainActivity
) {
    BackHandler { onBackClick() }
    val focusManager = LocalFocusManager.current
    val context      = LocalContext.current
    val db           = FirebaseFirestore.getInstance()

    var noDoc           by remember { mutableStateOf("") }
    var nombre          by remember { mutableStateOf("") }
    var apellido        by remember { mutableStateOf("") }
    var genero          by remember { mutableStateOf("") }
    var fechaNacimiento by remember { mutableStateOf("") }
    var rh              by remember { mutableStateOf("") }
    var numeroCama      by remember { mutableStateOf("") }
    var diagnostico     by remember { mutableStateOf("") }

    val generos        = listOf("Masculino", "Femenino")
    val tiposDeSangre  = listOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")
    var generoExpanded by remember { mutableStateOf(false) }
    var rhExpanded     by remember { mutableStateOf(false) }
    var camaExpanded   by remember { mutableStateOf(false) }
    var camasTemporales by remember { mutableStateOf<List<String>>(emptyList()) }
    var showAddCamaTempDialog by remember { mutableStateOf(false) }
    var nuevaCamaTemp  by remember { mutableStateOf("") }

    val camasDisponibles = remember(camasTemporales, camasOcupadas, noDoc) {
        val todas = (1..10).map { it.toString() } + camasTemporales
        todas.filter { cama -> cama !in camasOcupadas }
    }

    LaunchedEffect(Unit) {
        db.collection("pacientes")
            .whereEqualTo("activo", true)
            .get()
            .addOnSuccessListener { result ->
                camasOcupadas = result.documents
                    .mapNotNull { it.getString("numeroCama") }
                    .filter { it.isNotBlank() }
                    .toSet()
            }
    }

    var lookupState  by remember { mutableStateOf(LookupState.IDLE) }
    var foundPatient by remember { mutableStateOf<Patient?>(null) }

    var showActiveDialog by remember { mutableStateOf(false) }
    var showCamaDialog   by remember { mutableStateOf(false) }
    var camaOcupadaPor   by remember { mutableStateOf("") }
    var isRegistering    by remember { mutableStateOf(false) }
    var camasOcupadas    by remember { mutableStateOf<Set<String>>(emptySet()) }
    var checkingCamaTemp by remember { mutableStateOf(false) }
    var camasTempError   by remember { mutableStateOf("") }

    // Solo campos de identidad son readonly en INACTIVE
    val isReadOnly = lookupState == LookupState.INACTIVE

    val calendar = Calendar.getInstance()
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, day ->
            fechaNacimiento = "%02d/%02d/%04d".format(day, month + 1, year)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    fun resetForm() {
        lookupState  = LookupState.IDLE
        foundPatient = null
        nombre = ""; apellido = ""; genero = ""; fechaNacimiento = ""
        rh = ""; numeroCama = ""; diagnostico = ""
    }

    fun lookupPatient(doc: String) {
        if (doc.length !in 6..10) { resetForm(); return }
        lookupState = LookupState.LOADING
        db.collection("pacientes")
            .whereEqualTo("noDoc", doc)
            .get()
            .addOnSuccessListener { result ->
                val snap = result.documents.firstOrNull()
                if (snap == null) {
                    lookupState  = LookupState.NOT_FOUND
                    foundPatient = null
                } else {
                    val activo  = snap.getBoolean("activo") ?: true
                    val patient = Patient(
                        nombre          = snap.getString("nombre")          ?: "",
                        apellido        = snap.getString("apellido")        ?: "",
                        genero          = snap.getString("genero")          ?: "",
                        noDoc           = snap.getString("noDoc")           ?: doc,
                        fechaNacimiento = snap.getString("fechaNacimiento") ?: "",
                        rh              = snap.getString("rh")              ?: "",
                        numeroCama      = snap.getString("numeroCama")      ?: "",
                        diagnostico     = snap.getString("diagnostico")     ?: ""
                    )
                    if (activo) {
                        foundPatient     = patient
                        lookupState      = LookupState.ACTIVE
                        showActiveDialog = true
                    } else {
                        foundPatient = patient
                        numeroCama   = patient.numeroCama
                        diagnostico  = patient.diagnostico
                        lookupState  = LookupState.INACTIVE
                    }
                }
            }
            .addOnFailureListener {
                lookupState  = LookupState.NOT_FOUND
                foundPatient = null
                Toast.makeText(context, "Error al consultar paciente", Toast.LENGTH_SHORT).show()
            }
    }

    if (showAddCamaTempDialog) {
        AlertDialog(
            onDismissRequest = { showAddCamaTempDialog = false; nuevaCamaTemp = ""; camasTempError = "" },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp),
            title = { Text("Agregar cama temporal", fontWeight = FontWeight.Medium) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = nuevaCamaTemp,
                        onValueChange = { if (it.length <= 2 && it.all { c -> c.isDigit() }) { nuevaCamaTemp = it; camasTempError = "" } },
                        label = { Text("Número de cama temporal") },
                        placeholder = { Text("Ej: 11, 12") },
                        singleLine = true,
                        isError = camasTempError.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    if (camasTempError.isNotEmpty()) {
                        Text(camasTempError, fontSize = 12.sp, color = Color(0xFFA32D2D))
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cama = nuevaCamaTemp.trim()
                        if (cama.isBlank()) return@Button
                        checkingCamaTemp = true
                        db.collection("pacientes")
                            .whereEqualTo("numeroCama", cama)
                            .whereEqualTo("activo", true)
                            .get()
                            .addOnSuccessListener { result ->
                                checkingCamaTemp = false
                                val ocupante = result.documents.firstOrNull { it.getString("noDoc") != noDoc }
                                if (ocupante != null) {
                                    val n = "${ocupante.getString("nombre") ?: ""} ${ocupante.getString("apellido") ?: ""}".trim()
                                    camasTempError = "Ocupada por ${n.ifBlank { "otro paciente" }}"
                                } else {
                                    camasTemporales = camasTemporales + cama
                                    numeroCama = cama
                                    showAddCamaTempDialog = false
                                    nuevaCamaTemp = ""
                                    camasTempError = ""
                                }
                            }
                            .addOnFailureListener { checkingCamaTemp = false; camasTempError = "Error al verificar" }
                    },
                    enabled = nuevaCamaTemp.isNotBlank() && !checkingCamaTemp,
                    colors = ButtonDefaults.buttonColors(containerColor = DarkBlue),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (checkingCamaTemp) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                    else Text("Agregar")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAddCamaTempDialog = false; nuevaCamaTemp = ""; camasTempError = "" }, shape = RoundedCornerShape(8.dp)) { Text("Cancelar") }
            }
        )
    }

    val formValidNotFound =
        lookupState == LookupState.NOT_FOUND &&
                nombre.isNotBlank() && apellido.isNotBlank() && genero.isNotBlank() &&
                noDoc.length in 6..10 && fechaNacimiento.isNotBlank() && rh.isNotBlank() &&
                numeroCama.isNotBlank() && diagnostico.isNotBlank()

    val formValidInactive =
        lookupState == LookupState.INACTIVE &&
                numeroCama.isNotBlank() && diagnostico.isNotBlank()

    val formValid = formValidNotFound || formValidInactive

    // ── Diálogo: paciente ACTIVO ──────────────────────────────────────────
    if (showActiveDialog) {
        AlertDialog(
            onDismissRequest = {},
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp),
            title = null,
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFCEBEB)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.PersonOff,
                            contentDescription = null,
                            tint = Color(0xFFA32D2D),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Paciente activo",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Este paciente ya tiene una hospitalización en curso y no puede ser registrado nuevamente.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 22.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showActiveDialog = false; noDoc = ""; resetForm() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFA32D2D),
                        contentColor   = Color(0xFFF7C1C1)
                    )
                ) { Text("Entendido", fontWeight = FontWeight.Medium) }
            }
        )
    }

    // ── Diálogo: cama OCUPADA ─────────────────────────────────────────────
    if (showCamaDialog) {
        AlertDialog(
            onDismissRequest = {},
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp),
            title = null,
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFCEBEB)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.Bed,
                            contentDescription = null,
                            tint = Color(0xFFA32D2D),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Cama no disponible",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        buildAnnotatedString {
                            append("La cama ")
                            withStyle(SpanStyle(
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )) { append(numeroCama) }
                            append(" está ocupada por ")
                            withStyle(SpanStyle(
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )) { append(camaOcupadaPor) }
                            append(". Por favor asigne una cama diferente.")
                        },
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 22.sp
                    )
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = { showCamaDialog = false },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFA32D2D),
                            contentColor   = Color(0xFFF7C1C1)
                        )
                    ) { Text("Entendido", fontWeight = FontWeight.Medium) }
                }
            },
            confirmButton = {}
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Registrar Paciente", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBlue)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.White)
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
                .pointerInput(Unit) { detectTapGestures(onTap = { focusManager.clearFocus() }) }
        ) {
            Text("Datos del Paciente", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = DarkBlue)
            Spacer(Modifier.height(16.dp))

            // ── Documento ─────────────────────────────────────────────────
            LimitedFormField(
                label        = "Documento paciente *",
                value        = noDoc,
                onChange     = { new ->
                    noDoc = new
                    if (new.length < 6) resetForm() else lookupPatient(new)
                },
                placeholder  = "6–10 dígitos",
                keyboardType = KeyboardType.Number,
                maxLength    = 10,
                minLength    = 6
            )

            if (lookupState == LookupState.LOADING) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = DarkBlue)
                    Spacer(Modifier.width(8.dp))
                    Text("Consultando paciente…", fontSize = 13.sp, color = Color.Gray)
                }
            }

            if (lookupState == LookupState.INACTIVE) {
                Spacer(Modifier.height(12.dp))
                Card(
                    shape  = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("ℹ️", fontSize = 18.sp)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "Paciente encontrado como inactivo. Datos cargados automáticamente. " +
                                    "Asigne una cama y diagnóstico, luego presione Registrar.",
                            fontSize = 13.sp, color = Color(0xFF0D47A1), lineHeight = 19.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // ── Nombre ────────────────────────────────────────────────────
            if (isReadOnly) ReadOnlyField("Nombre", foundPatient?.nombre ?: "")
            else            FormField("Nombre *", nombre, { v -> if (v.all { it.isLetter() || it == ' ' }) nombre = v }, "Ej: Juan")
            Spacer(Modifier.height(14.dp))

            // ── Apellido ──────────────────────────────────────────────────
            if (isReadOnly) ReadOnlyField("Apellido", foundPatient?.apellido ?: "")
            else            FormField("Apellido *", apellido, { v -> if (v.all { it.isLetter() || it == ' ' }) apellido = v }, "Ej: Pérez")
            Spacer(Modifier.height(14.dp))

            // ── Género ────────────────────────────────────────────────────
            if (isReadOnly) {
                ReadOnlyField("Género", foundPatient?.genero ?: "")
            } else {
                DropdownField(
                    label            = "Género *",
                    value            = genero,
                    items            = generos,
                    expanded         = generoExpanded,
                    onExpandedChange = { generoExpanded = !generoExpanded },
                    onSelect         = { genero = it; generoExpanded = false }
                )
            }
            Spacer(Modifier.height(14.dp))

            // ── Fecha de nacimiento ───────────────────────────────────────
            // Box encima del OutlinedTextField (siempre disabled) captura el tap
            // en todo el recuadro y abre el DatePicker solo cuando no es readonly.
            Text("Fecha nacimiento *", color = DarkBlue, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (!isReadOnly) Modifier.clickable { datePickerDialog.show() } else Modifier)
            ) {
                OutlinedTextField(
                    value         = if (isReadOnly) foundPatient?.fechaNacimiento ?: "" else fechaNacimiento,
                    onValueChange = {},
                    readOnly      = true,
                    enabled       = false,
                    placeholder   = { Text("DD/MM/AAAA", color = LightGray) },
                    trailingIcon  = {
                        if (!isReadOnly) Icon(Icons.Outlined.CalendarMonth, contentDescription = null)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(8.dp),
                    colors   = OutlinedTextFieldDefaults.colors(
                        disabledBorderColor       = if (isReadOnly) Color(0xFF4CAF50) else LightGray,
                        disabledTextColor         = Color.Black,
                        disabledPlaceholderColor  = LightGray,
                        disabledTrailingIconColor = Color.DarkGray
                    )
                )
            }
            Spacer(Modifier.height(14.dp))

            // ── Tipo de sangre ────────────────────────────────────────────
            if (isReadOnly) {
                ReadOnlyField("Tipo de sangre", foundPatient?.rh ?: "")
            } else {
                DropdownField(
                    label            = "Tipo de sangre *",
                    value            = rh,
                    items            = tiposDeSangre,
                    expanded         = rhExpanded,
                    onExpandedChange = { rhExpanded = !rhExpanded },
                    onSelect         = { rh = it; rhExpanded = false }
                )
            }
            Spacer(Modifier.height(14.dp))

            // ── Número de cama: dropdown con camas 1-10 + temporales ─────
            Text("Número de cama *", color = DarkBlue, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(6.dp))
            ExposedDropdownMenuBox(expanded = camaExpanded, onExpandedChange = { camaExpanded = !camaExpanded }) {
                OutlinedTextField(
                    value = numeroCama,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    placeholder = { Text("Seleccione una cama", color = LightGray) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = camaExpanded) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DarkBlue,
                        unfocusedBorderColor = if (numeroCama.isNotBlank()) Color(0xFF4CAF50) else LightGray
                    )
                )
                ExposedDropdownMenu(expanded = camaExpanded, onDismissRequest = { camaExpanded = false }) {
                    camasDisponibles.forEach { cama ->
                        DropdownMenuItem(
                            text = { Text("Cama $cama") },
                            onClick = { numeroCama = cama; camaExpanded = false }
                        )
                    }
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.Add, contentDescription = null, tint = DarkBlue, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Agregar cama temporal", color = DarkBlue, fontWeight = FontWeight.Medium)
                            }
                        },
                        onClick = { camaExpanded = false; showAddCamaTempDialog = true }
                    )
                }
            }
            Spacer(Modifier.height(14.dp))

            // ── Diagnóstico: SIEMPRE editable ─────────────────────────────
            FormField("Diagnóstico *", diagnostico, { diagnostico = it }, "Ej: Neumonía")

            Spacer(Modifier.height(32.dp))

            // ── Botones ───────────────────────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick  = onCancelClick,
                    modifier = Modifier.weight(1f),
                    shape    = RoundedCornerShape(10.dp),
                    border   = BorderStroke(1.dp, DarkBlue)
                ) { Text("Cancelar", color = DarkBlue) }

                Button(
                    onClick = {
                        isRegistering = true
                        // 1️⃣ Verificar cama libre (ignorar al propio paciente en INACTIVE)
                        db.collection("pacientes")
                            .whereEqualTo("numeroCama", numeroCama)
                            .whereEqualTo("activo", true)
                            .get()
                            .addOnSuccessListener { result ->
                                val ocupante = result.documents.firstOrNull { s ->
                                    s.getString("noDoc") != noDoc
                                }
                                if (ocupante != null) {
                                    val n = "${ocupante.getString("nombre") ?: ""} ${ocupante.getString("apellido") ?: ""}".trim()
                                    camaOcupadaPor = n.ifBlank { "otro paciente" }
                                    showCamaDialog = true
                                    isRegistering  = false
                                } else {
                                    when (lookupState) {

                                        // ── INACTIVE: solo actualizar cama y diagnóstico ──
                                        // activo=true lo pone ConsentScreen al confirmar
                                        LookupState.INACTIVE -> {
                                            val patient = foundPatient!!.copy(
                                                numeroCama  = numeroCama,
                                                diagnostico = diagnostico
                                            )
                                            db.collection("pacientes")
                                                .whereEqualTo("noDoc", noDoc)
                                                .get()
                                                .addOnSuccessListener { snap ->
                                                    val docRef = snap.documents.firstOrNull()?.reference
                                                    if (docRef == null) {
                                                        isRegistering = false
                                                        Toast.makeText(context, "Paciente no encontrado", Toast.LENGTH_SHORT).show()
                                                        return@addOnSuccessListener
                                                    }
                                                    docRef.update(mapOf(
                                                        "numeroCama"               to numeroCama,
                                                        "diagnostico"              to diagnostico,
                                                        "registradoPorId"          to SesionState.usuario.uid,
                                                        "registradoPorNombre"      to SesionState.usuario.nombreCompleto,
                                                        "registradoPorEspecialidad" to SesionState.usuario.especialidad,
                                                        "registradoEn"             to System.currentTimeMillis()
                                                        // activo se mantiene false; ConsentScreen lo pone en true
                                                    )).addOnSuccessListener {
                                                        isRegistering = false
                                                        onRegisterClick(patient)   // → MainActivity navega a "consent"
                                                    }.addOnFailureListener { e ->
                                                        isRegistering = false
                                                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                                .addOnFailureListener { e ->
                                                    isRegistering = false
                                                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                                }
                                        }

                                        // ── NOT_FOUND: crear paciente nuevo ──────────────
                                        // activo=false; ConsentScreen lo pone en true
                                        LookupState.NOT_FOUND -> {
                                            val registradoEn = System.currentTimeMillis()
                                            val patient = Patient(
                                                nombre                   = nombre,
                                                apellido                 = apellido,
                                                genero                   = genero,
                                                noDoc                    = noDoc,
                                                fechaNacimiento          = fechaNacimiento,
                                                rh                       = rh,
                                                numeroCama               = numeroCama,
                                                diagnostico              = diagnostico,
                                                registradoPorId          = SesionState.usuario.uid,
                                                registradoPorNombre      = SesionState.usuario.nombreCompleto,
                                                registradoPorEspecialidad = SesionState.usuario.especialidad,
                                                registradoEn             = registradoEn
                                            )
                                            db.collection("pacientes")
                                                .add(hashMapOf(
                                                    "nombre"                   to patient.nombre,
                                                    "apellido"                 to patient.apellido,
                                                    "genero"                   to patient.genero,
                                                    "noDoc"                    to patient.noDoc,
                                                    "fechaNacimiento"          to patient.fechaNacimiento,
                                                    "rh"                       to patient.rh,
                                                    "numeroCama"               to patient.numeroCama,
                                                    "diagnostico"              to patient.diagnostico,
                                                    "activo"                   to false,
                                                    "registradoPorId"          to patient.registradoPorId,
                                                    "registradoPorNombre"      to patient.registradoPorNombre,
                                                    "registradoPorEspecialidad" to patient.registradoPorEspecialidad,
                                                    "registradoEn"             to patient.registradoEn
                                                ))
                                                .addOnSuccessListener {
                                                    isRegistering = false
                                                    onRegisterClick(patient)   // → MainActivity navega a "consent"
                                                }
                                                .addOnFailureListener { e ->
                                                    isRegistering = false
                                                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                                }
                                        }

                                        else -> { isRegistering = false }
                                    }
                                }
                            }
                            .addOnFailureListener { e ->
                                isRegistering = false
                                Toast.makeText(context, "Error al verificar cama: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    },
                    enabled  = formValid && !isRegistering,
                    modifier = Modifier.weight(1f),
                    shape    = RoundedCornerShape(10.dp),
                    border   = if (formValid && !isRegistering) null else BorderStroke(1.dp, LightGray),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor         = DarkBlue,
                        contentColor           = Color.White,
                        disabledContainerColor = Color.White,
                        disabledContentColor   = LightGray
                    )
                ) {
                    if (isRegistering) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                    } else {
                        Text("Registrar", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}

// ── Composables reutilizables ─────────────────────────────────────────────────

@Composable
fun ReadOnlyField(label: String, value: String) {
    Text(label, color = DarkBlue, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    Spacer(Modifier.height(6.dp))
    OutlinedTextField(
        value         = value,
        onValueChange = {},
        readOnly      = true,
        enabled       = false,
        modifier      = Modifier.fillMaxWidth(),
        shape         = RoundedCornerShape(8.dp),
        colors        = OutlinedTextFieldDefaults.colors(
            disabledBorderColor = Color(0xFF4CAF50),
            disabledTextColor   = Color.Black
        )
    )
}

@Composable
fun FormField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    Text(label, color = DarkBlue, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    Spacer(Modifier.height(6.dp))
    OutlinedTextField(
        value           = value,
        onValueChange   = onChange,
        placeholder     = { Text(placeholder, color = LightGray) },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier        = Modifier.fillMaxWidth(),
        shape           = RoundedCornerShape(8.dp),
        singleLine      = true,
        colors          = OutlinedTextFieldDefaults.colors(
            focusedBorderColor   = DarkBlue,
            unfocusedBorderColor = LightGray
        )
    )
}

@Composable
fun LimitedFormField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    maxLength: Int,
    minLength: Int = 1
) {
    val isError    = value.isNotEmpty() && value.length < minLength
    val isComplete = value.length in minLength..maxLength && value.isNotEmpty()

    Text(label, color = DarkBlue, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    Spacer(Modifier.height(6.dp))
    OutlinedTextField(
        value           = value,
        onValueChange   = { if (it.length <= maxLength) onChange(it) },
        placeholder     = { Text(placeholder, color = LightGray) },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier        = Modifier.fillMaxWidth(),
        shape           = RoundedCornerShape(8.dp),
        singleLine      = true,
        isError         = isError,
        colors          = OutlinedTextFieldDefaults.colors(
            focusedBorderColor   = when {
                isComplete -> Color(0xFF4CAF50)
                isError    -> Color(0xFFF44336)
                else       -> DarkBlue
            },
            unfocusedBorderColor = when {
                isComplete -> Color(0xFF4CAF50)
                isError    -> Color(0xFFF44336)
                else       -> LightGray
            }
        ),
        supportingText = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text     = when {
                        isError    -> "Mínimo $minLength caracteres"
                        isComplete -> "✓ Correcto"
                        else       -> ""
                    },
                    fontSize = 11.sp,
                    color    = when {
                        isError    -> Color(0xFFF44336)
                        isComplete -> Color(0xFF4CAF50)
                        else       -> Color.Transparent
                    }
                )
                Text(
                    text     = "${value.length}/$maxLength",
                    fontSize = 11.sp,
                    color    = if (isComplete) Color(0xFF4CAF50) else LightGray
                )
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownField(
    label: String,
    value: String,
    items: List<String>,
    expanded: Boolean,
    onExpandedChange: () -> Unit,
    onSelect: (String) -> Unit
) {
    Text(label, color = DarkBlue, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    Spacer(Modifier.height(6.dp))
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { onExpandedChange() }) {
        OutlinedTextField(
            value         = value,
            onValueChange = {},
            readOnly      = true,
            modifier      = Modifier.menuAnchor().fillMaxWidth(),
            shape         = RoundedCornerShape(8.dp),
            placeholder   = { Text("Seleccione", color = LightGray) },
            trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors        = OutlinedTextFieldDefaults.colors(
                focusedBorderColor   = DarkBlue,
                unfocusedBorderColor = LightGray
            )
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { onExpandedChange() }) {
            items.forEach { item ->
                DropdownMenuItem(text = { Text(item) }, onClick = { onSelect(item) })
            }
        }
    }
}
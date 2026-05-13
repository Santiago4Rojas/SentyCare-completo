package com.example.sentycare

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.sentycare.data.Rol
import com.example.sentycare.permissions.Permisos
import com.example.sentycare.ui.theme.*
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

private enum class SortBy(val label: String) {
    FECHA("Fecha"), NOMBRE("Nombre"), CAMA("Cama"), DIAGNOSTICO("Diagnóstico"), RH("RH")
}

fun rolColor(rol: Rol): Color = when (rol) {
    Rol.ADMIN -> Color(0xFF7B1FA2)
    Rol.JEFE_UNIDAD -> Color(0xFF1565C0)
    Rol.MEDICO_ESPECIALISTA -> Color(0xFF2E7D32)
    Rol.MEDICO_RESIDENTE -> Color(0xFF00838F)
    Rol.ENFERMERA -> Color(0xFFE65100)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onRegisterClick: () -> Unit = {},
    onPatientClick: (Patient) -> Unit = {},
    onLogoutClick: () -> Unit = {},
    onAdminClick: () -> Unit = {}
) {
    BackHandler {}
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    var patients by remember { mutableStateOf<List<Patient>>(emptyList()) }
    var expandedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var patientToEditCama by remember { mutableStateOf<Patient?>(null) }
    var newCamaValue by remember { mutableStateOf("") }
    var sortBy by remember { mutableStateOf(SortBy.FECHA) }
    var showChangePassword by remember { mutableStateOf(false) }
    var fotoUrl by remember { mutableStateOf(SesionState.usuario.fotoUrl) }
    var uploadingPhoto by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        uploadingPhoto = true
        val uid = auth.currentUser?.uid ?: return@rememberLauncherForActivityResult
        val ref = FirebaseStorage.getInstance().reference.child("profilePhotos/$uid.jpg")
        ref.putFile(uri).addOnSuccessListener {
            ref.downloadUrl.addOnSuccessListener { downloadUri ->
                val url = downloadUri.toString()
                fotoUrl = url
                SesionState.usuario = SesionState.usuario.copy(fotoUrl = url)
                db.collection("usuarios").document(uid).update("fotoUrl", url)
                uploadingPhoto = false
            }
        }.addOnFailureListener {
            uploadingPhoto = false
            Toast.makeText(context, "Error al subir foto", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        db.collection("pacientes").get().addOnSuccessListener { result ->
            val loaded = result.documents.mapNotNull { doc ->
                doc.toObject(Patient::class.java)?.copy(id = doc.id)
            }.filter { it.activo }
            patients = loaded
            expandedIds = loaded.map { it.id }.toSet() // all panels open on load
        }
    }

    val sortedPatients = remember(patients, sortBy) {
        when (sortBy) {
            SortBy.FECHA -> patients.sortedByDescending { it.registradoEn }
            SortBy.NOMBRE -> patients.sortedBy { "${it.nombre} ${it.apellido}" }
            SortBy.CAMA -> patients.sortedBy { it.numeroCama.padStart(3, '0') }
            SortBy.DIAGNOSTICO -> patients.sortedBy { it.diagnostico.lowercase() }
            SortBy.RH -> patients.sortedBy { it.rh }
        }
    }

    if (showChangePassword) {
        CambiarContrasenaDialog(
            onDismiss = { showChangePassword = false },
            onCambiar = { currentPass, newPass ->
                val user = auth.currentUser
                val cred = EmailAuthProvider.getCredential(user?.email ?: "", currentPass)
                user?.reauthenticate(cred)
                    ?.addOnSuccessListener {
                        user.updatePassword(newPass)
                            .addOnSuccessListener {
                                Toast.makeText(context, "Contraseña actualizada correctamente", Toast.LENGTH_SHORT).show()
                                showChangePassword = false
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                    ?.addOnFailureListener {
                        Toast.makeText(context, "Contraseña actual incorrecta", Toast.LENGTH_SHORT).show()
                    }
            }
        )
    }

    patientToEditCama?.let { patient ->
        var camaOcupadaError by remember { mutableStateOf("") }
        var isCheckingCama by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { patientToEditCama = null; newCamaValue = ""; camaOcupadaError = "" },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp),
            title = null,
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Modificar cama", fontSize = 17.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(6.dp))
                    Text("Nueva cama para ${patient.nombre} ${patient.apellido}", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = newCamaValue,
                        onValueChange = { if (it.length <= 2) { newCamaValue = it; camaOcupadaError = "" } },
                        label = { Text("Número de cama") },
                        singleLine = true,
                        isError = camaOcupadaError.isNotEmpty(),
                        supportingText = {
                            if (camaOcupadaError.isNotEmpty())
                                Text(camaOcupadaError, color = MaterialTheme.colorScheme.error)
                        },
                        shape = RoundedCornerShape(8.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(20.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = { patientToEditCama = null; newCamaValue = ""; camaOcupadaError = "" },
                            modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp)
                        ) { Text("Cancelar", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        Button(
                            onClick = {
                                if (newCamaValue.isNotEmpty() && newCamaValue != patient.numeroCama) {
                                    isCheckingCama = true
                                    db.collection("pacientes")
                                        .whereEqualTo("numeroCama", newCamaValue)
                                        .whereEqualTo("activo", true)
                                        .get()
                                        .addOnSuccessListener { result ->
                                            val ocupante = result.documents.firstOrNull {
                                                it.getString("noDoc") != patient.noDoc
                                            }
                                            if (ocupante != null) {
                                                val nombre = "${ocupante.getString("nombre") ?: ""} ${ocupante.getString("apellido") ?: ""}".trim()
                                                camaOcupadaError = "Ocupada por ${nombre.ifBlank { "otro paciente" }}"
                                                isCheckingCama = false
                                            } else {
                                                patients = patients.map { if (it.id == patient.id) it.copy(numeroCama = newCamaValue) else it }
                                                db.collection("pacientes").document(patient.id).update("numeroCama", newCamaValue)
                                                isCheckingCama = false; patientToEditCama = null; newCamaValue = ""; camaOcupadaError = ""
                                            }
                                        }
                                        .addOnFailureListener { camaOcupadaError = "Error al verificar la cama"; isCheckingCama = false }
                                } else if (newCamaValue == patient.numeroCama) {
                                    patientToEditCama = null; newCamaValue = ""
                                }
                            },
                            enabled = newCamaValue.isNotEmpty() && !isCheckingCama,
                            modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DarkBlue)
                        ) {
                            if (isCheckingCama)
                                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                            else
                                Text("Guardar", color = Color.White, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color.White,
                modifier = Modifier.width(300.dp)
            ) {
                // Header — white background
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(start = 20.dp, top = 24.dp, end = 20.dp, bottom = 20.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Avatar with photo or initials + edit button
                        Box(contentAlignment = Alignment.BottomEnd) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(DarkBlue)
                                    .border(2.dp, DarkBlue, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (fotoUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = fotoUrl,
                                        contentDescription = "Foto de perfil",
                                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                    )
                                } else {
                                    val initials = buildString {
                                        SesionState.usuario.nombre.firstOrNull()?.let { append(it.uppercaseChar()) }
                                        SesionState.usuario.apellido.firstOrNull()?.let { append(it.uppercaseChar()) }
                                    }.ifBlank { "?" }
                                    Text(initials, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            // Edit/camera button
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                                    .border(1.5.dp, DarkBlue, CircleShape)
                                    .clickable { photoPickerLauncher.launch("image/*") },
                                contentAlignment = Alignment.Center
                            ) {
                                if (uploadingPhoto) {
                                    CircularProgressIndicator(Modifier.size(12.dp), strokeWidth = 1.5.dp, color = DarkBlue)
                                } else {
                                    Icon(Icons.Outlined.CameraAlt, null, tint = DarkBlue, modifier = Modifier.size(12.dp))
                                }
                            }
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                SesionState.usuario.nombreCompleto.ifBlank { "Usuario" },
                                color = DarkBlue,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 20.sp
                            )
                            Text(
                                SesionState.usuario.rol.displayName,
                                color = DarkBlue.copy(alpha = 0.7f),
                                fontSize = 12.sp
                            )
                            Text(
                                    SesionState.usuario.especialidad.ifBlank { "No aplica" },
                                    color = Color.Gray,
                                    fontSize = 11.sp
                                )
                        }
                    }
                }

                // User info rows
                Column(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "INFORMACIÓN PERSONAL",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp
                    )
                    DrawerInfoRow("Apellido", SesionState.usuario.apellido.ifBlank { "—" })
                    DrawerInfoRow("Especialidad", SesionState.usuario.especialidad.ifBlank { "No aplica" })
                    DrawerInfoRow("Documento", SesionState.usuario.noDoc.ifBlank { "—" })
                    DrawerInfoRow("RH", SesionState.usuario.rh.ifBlank { "—" })
                }

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                Spacer(Modifier.height(8.dp))

                NavigationDrawerItem(
                    label = { Text("Cambiar contraseña", fontWeight = FontWeight.Medium) },
                    selected = false,
                    onClick = { scope.launch { drawerState.close() }; showChangePassword = true },
                    icon = { Icon(Icons.Outlined.Lock, contentDescription = null, tint = DarkBlue) },
                    colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent),
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                NavigationDrawerItem(
                    label = { Text("Cerrar sesión", fontWeight = FontWeight.Medium, color = Color(0xFFE53935)) },
                    selected = false,
                    onClick = { scope.launch { drawerState.close() }; onLogoutClick() },
                    icon = { Icon(Icons.Outlined.Logout, contentDescription = null, tint = Color(0xFFE53935)) },
                    colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent),
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) { detectTapGestures(onTap = { focusManager.clearFocus() }) }
        ) {
            Column(modifier = Modifier.fillMaxSize().background(Color.White)) {

                // Top header row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkBlue)
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(rolColor(SesionState.rol))
                            .clickable { scope.launch { drawerState.open() } },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.Person,
                            contentDescription = "Abrir menú",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(R.drawable.logosentycare),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp).clip(CircleShape)
                            )
                            Spacer(Modifier.width(5.dp))
                            Text("SentyCare", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Text(
                            text = SesionState.usuario.nombreCompleto.ifBlank { "Usuario" },
                            color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold
                        )
                        val subtitulo = buildString {
                            append(SesionState.usuario.rol.displayName)
                            append(" · ")
                            append(SesionState.usuario.especialidad.ifBlank { "No aplica" })
                        }
                        Text(subtitulo, color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                    }
                    Box(
                        modifier = Modifier.size(32.dp).clickable { openPdfFromAssets(context, "ManualSentyCare.pdf") },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.QuestionMark,
                            contentDescription = "Manual",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Sort filter chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFE8EEF7))
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SortBy.entries.forEach { option ->
                        FilterChip(
                            selected = sortBy == option,
                            onClick = { sortBy = option },
                            label = { Text(option.label, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = DarkBlue,
                                selectedLabelColor = Color.White,
                                containerColor = Color.White,
                                labelColor = DarkBlue
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = sortBy == option,
                                selectedBorderColor = DarkBlue,
                                borderColor = LightGray
                            )
                        )
                    }
                }

                val listState = rememberLazyListState()
                if (sortedPatients.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Actualmente no hay registro de\npacientes hospitalizados en esta área",
                            fontSize = 15.sp,
                            color = LightGray,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(top = 16.dp, bottom = 88.dp)
                    ) {
                        items(sortedPatients, key = { it.id }) { patient ->
                            val isExpanded = patient.id in expandedIds
                            ExpandablePatientCard(
                                patient = patient,
                                isExpanded = isExpanded,
                                onCardClick = {
                                    expandedIds = if (isExpanded) expandedIds - patient.id else expandedIds + patient.id
                                },
                                onGoToPatient = { onPatientClick(patient) },
                                onEditCama = { newCamaValue = patient.numeroCama; patientToEditCama = patient },
                                onDischarge = {
                                    db.collection("pacientes").document(patient.id)
                                        .update(mapOf("activo" to false, "numeroCama" to "", "diagnostico" to ""))
                                        .addOnSuccessListener {
                                            patients = patients.filter { it.id != patient.id }
                                            expandedIds = expandedIds - patient.id
                                        }
                                }
                            )
                        }
                    }
                }
            }

            if (Permisos.puedeRegistrarPaciente(SesionState.rol)) {
                ExtendedFloatingActionButton(
                    onClick = onRegisterClick,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = 20.dp),
                    containerColor = DarkBlue,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(14.dp),
                    icon = { Icon(imageVector = Icons.Outlined.PersonAdd, contentDescription = null) },
                    text = { Text("Registrar paciente", fontWeight = FontWeight.SemiBold) }
                )
            }
        }
    }
}

@Composable
private fun DrawerInfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Text(label, fontSize = 12.sp, color = Color.Gray, modifier = Modifier.width(90.dp))
        Text(value, fontSize = 13.sp, color = DarkBlue, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun CambiarContrasenaDialog(onDismiss: () -> Unit, onCambiar: (String, String) -> Unit) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showCurrent by remember { mutableStateOf(false) }
    var showNew by remember { mutableStateOf(false) }
    var showConfirm by remember { mutableStateOf(false) }

    val hasUpper = newPassword.any { it.isUpperCase() }
    val hasLower = newPassword.any { it.isLowerCase() }
    val hasDigit = newPassword.any { it.isDigit() }
    val hasSpecial = newPassword.any { !it.isLetterOrDigit() }
    val hasLength = newPassword.length >= 8
    val passwordsMatch = newPassword == confirmPassword && newPassword.isNotBlank()
    val isValid = hasUpper && hasLower && hasDigit && hasSpecial && hasLength && passwordsMatch && currentPassword.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(16.dp),
        title = { Text("Cambiar contraseña", fontWeight = FontWeight.Bold, color = DarkBlue) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = currentPassword, onValueChange = { currentPassword = it },
                    label = { Text("Contraseña actual") }, modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp), singleLine = true,
                    visualTransformation = if (showCurrent) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showCurrent = !showCurrent }) {
                            Icon(if (showCurrent) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, null, tint = Color.Gray)
                        }
                    }
                )
                OutlinedTextField(
                    value = newPassword, onValueChange = { newPassword = it },
                    label = { Text("Nueva contraseña") }, modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp), singleLine = true,
                    visualTransformation = if (showNew) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showNew = !showNew }) {
                            Icon(if (showNew) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, null, tint = Color.Gray)
                        }
                    }
                )
                OutlinedTextField(
                    value = confirmPassword, onValueChange = { confirmPassword = it },
                    label = { Text("Confirmar contraseña") }, modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp), singleLine = true,
                    isError = confirmPassword.isNotBlank() && !passwordsMatch,
                    visualTransformation = if (showConfirm) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showConfirm = !showConfirm }) {
                            Icon(if (showConfirm) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, null, tint = Color.Gray)
                        }
                    }
                )
                if (confirmPassword.isNotBlank() && !passwordsMatch) {
                    Text("Las contraseñas no coinciden", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                }
                Spacer(Modifier.height(4.dp))
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    PasswordRequirement("Mínimo 8 caracteres", hasLength)
                    PasswordRequirement("Mayúscula (A-Z)", hasUpper)
                    PasswordRequirement("Minúscula (a-z)", hasLower)
                    PasswordRequirement("Número (0-9)", hasDigit)
                    PasswordRequirement("Carácter especial (!@#\$...)", hasSpecial)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onCambiar(currentPassword, newPassword) },
                enabled = isValid,
                colors = ButtonDefaults.buttonColors(containerColor = DarkBlue),
                shape = RoundedCornerShape(8.dp)
            ) { Text("Cambiar") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(8.dp)) { Text("Cancelar") }
        }
    )
}

@Composable
fun PasswordRequirement(text: String, met: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = if (met) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (met) Color(0xFF2E7D32) else Color(0xFFBBBBBB),
            modifier = Modifier.size(14.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(text, fontSize = 11.sp, color = if (met) Color(0xFF2E7D32) else Color.Gray)
    }
}

fun openPdfFromAssets(context: Context, assetFileName: String) {
    try {
        val inputStream = context.assets.open(assetFileName)
        val outFile = File(context.cacheDir, assetFileName)
        val outputStream = FileOutputStream(outFile)
        inputStream.copyTo(outputStream)
        inputStream.close()
        outputStream.close()
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", outFile)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
        }
        context.startActivity(Intent.createChooser(intent, "Abrir manual"))
    } catch (e: Exception) {
        Toast.makeText(context, "No se pudo abrir el manual", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun ExpandablePatientCard(
    patient: Patient,
    isExpanded: Boolean,
    onCardClick: () -> Unit,
    onGoToPatient: () -> Unit,
    onEditCama: () -> Unit,
    onDischarge: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onCardClick() },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                val genderColor = when (patient.genero.lowercase()) {
                    "femenino" -> Color(0xFFE91E8C)
                    else -> DarkBlue
                }
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape).border(2.dp, genderColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Outlined.Person, contentDescription = null, tint = genderColor, modifier = Modifier.size(26.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${patient.nombre} ${patient.apellido}",
                        fontSize = 16.sp, fontWeight = FontWeight.Bold, color = DarkBlue
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = "Cama ${patient.numeroCama} • ${patient.fechaNacimiento}", fontSize = 13.sp, color = Color.Gray)
                    Text(text = patient.diagnostico, fontSize = 13.sp, color = Color.Gray)
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = null,
                    tint = LightGray
                )
            }

            AnimatedVisibility(visible = isExpanded, enter = expandVertically(), exit = shrinkVertically()) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = LightGray.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(8.dp))

                    // Extra patient info
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Row {
                            Text("RH: ", fontSize = 12.sp, color = Color.Gray)
                            Text(patient.rh.ifBlank { "—" }, fontSize = 12.sp, color = DarkBlue, fontWeight = FontWeight.Medium)
                        }
                        if (patient.registradoPorNombre.isNotBlank()) {
                            Row {
                                Text("Registrado por: ", fontSize = 12.sp, color = Color.Gray)
                                Text(patient.registradoPorNombre, fontSize = 12.sp, color = DarkBlue, fontWeight = FontWeight.Medium)
                            }
                        }
                        if (patient.registradoEn > 0L) {
                            val fechaReg = remember(patient.registradoEn) {
                                SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(patient.registradoEn))
                            }
                            Row {
                                Text("Registro: ", fontSize = 12.sp, color = Color.Gray)
                                Text(fechaReg, fontSize = 12.sp, color = DarkBlue, fontWeight = FontWeight.Medium)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onGoToPatient,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DarkBlue),
                            contentPadding = PaddingValues(vertical = 10.dp)
                        ) {
                            Icon(imageVector = Icons.Outlined.AssignmentInd, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Ir", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                        OutlinedButton(
                            onClick = onEditCama,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBlue),
                            contentPadding = PaddingValues(vertical = 10.dp)
                        ) {
                            Icon(imageVector = Icons.Outlined.Edit, contentDescription = null, tint = DarkBlue, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Cama", fontSize = 12.sp, color = DarkBlue, fontWeight = FontWeight.SemiBold)
                        }
                        if (Permisos.puedeDarDeAlta(SesionState.rol)) {
                            OutlinedButton(
                                onClick = onDischarge,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE53935)),
                                contentPadding = PaddingValues(vertical = 10.dp)
                            ) {
                                Icon(imageVector = Icons.Outlined.ExitToApp, contentDescription = null, tint = Color(0xFFE53935), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Alta", fontSize = 12.sp, color = Color(0xFFE53935), fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}

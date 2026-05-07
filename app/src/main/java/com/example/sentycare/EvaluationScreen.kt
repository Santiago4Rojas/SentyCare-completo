package com.example.sentycare

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import com.example.sentycare.ui.theme.*
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EvaluationScreen(
    patient: Patient,
    onComfortClick: () -> Unit = {},
    onRassClick: () -> Unit = {},
    onDolorClick: () -> Unit = {},
    onHomeClick: () -> Unit = {},
    onHistorialClick: () -> Unit = {},
    onInfoClick: () -> Unit = {},
    onPatientUpdated: (Patient) -> Unit = {},
    onPatientDischarged: () -> Unit = {}
) {
    BackHandler { onHomeClick() }

    val db = FirebaseFirestore.getInstance()
    var currentPatient by remember { mutableStateOf(patient) }
    var showCamaDialog by remember { mutableStateOf(false) }
    var newCamaValue by remember { mutableStateOf("") }
    var showAltaDialog by remember { mutableStateOf(false) }

    if (showCamaDialog) {
        var camaOcupadaError by remember { mutableStateOf("") }
        var isCheckingCama   by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showCamaDialog = false; newCamaValue = ""; camaOcupadaError = "" },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp),
            title = null,
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Modificar cama",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Nueva cama para ${currentPatient.nombre}",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value         = newCamaValue,
                        onValueChange = { if (it.length <= 2) { newCamaValue = it; camaOcupadaError = "" } },
                        label         = { Text("Número de cama") },
                        singleLine    = true,
                        isError       = camaOcupadaError.isNotEmpty(),
                        supportingText = {
                            if (camaOcupadaError.isNotEmpty())
                                Text(camaOcupadaError, color = MaterialTheme.colorScheme.error)
                        },
                        shape           = RoundedCornerShape(8.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier        = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(20.dp))
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick  = { showCamaDialog = false; newCamaValue = ""; camaOcupadaError = "" },
                            modifier = Modifier.weight(1f),
                            shape    = RoundedCornerShape(8.dp)
                        ) { Text("Cancelar", color = MaterialTheme.colorScheme.onSurfaceVariant) }

                        Button(
                            onClick = {
                                if (newCamaValue.isNotEmpty() && newCamaValue != currentPatient.numeroCama) {
                                    isCheckingCama = true
                                    db.collection("pacientes")
                                        .whereEqualTo("numeroCama", newCamaValue)
                                        .whereEqualTo("activo", true)
                                        .get()
                                        .addOnSuccessListener { result ->
                                            val ocupante = result.documents.firstOrNull {
                                                it.getString("noDoc") != currentPatient.noDoc
                                            }
                                            if (ocupante != null) {
                                                val nombre = "${ocupante.getString("nombre") ?: ""} ${ocupante.getString("apellido") ?: ""}".trim()
                                                camaOcupadaError = "Ocupada por ${nombre.ifBlank { "otro paciente" }}"
                                                isCheckingCama   = false
                                            } else {
                                                val camaFinal = newCamaValue
                                                currentPatient = currentPatient.copy(numeroCama = camaFinal)
                                                onPatientUpdated(currentPatient)
                                                db.collection("pacientes")
                                                    .document(currentPatient.id)
                                                    .update("numeroCama", camaFinal)
                                                isCheckingCama   = false
                                                showCamaDialog   = false
                                                newCamaValue     = ""
                                                camaOcupadaError = ""
                                            }
                                        }
                                        .addOnFailureListener {
                                            camaOcupadaError = "Error al verificar la cama"
                                            isCheckingCama   = false
                                        }
                                } else if (newCamaValue == currentPatient.numeroCama) {
                                    showCamaDialog = false
                                    newCamaValue   = ""
                                }
                            },
                            enabled  = newCamaValue.isNotEmpty() && !isCheckingCama,
                            modifier = Modifier.weight(1f),
                            shape    = RoundedCornerShape(8.dp),
                            colors   = ButtonDefaults.buttonColors(containerColor = DarkBlue)
                        ) {
                            if (isCheckingCama) {
                                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                            } else {
                                Text("Guardar", color = Color.White, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    if (showAltaDialog) {
        AlertDialog(
            onDismissRequest = { showAltaDialog = false },
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
                            Icons.Outlined.ExitToApp,
                            contentDescription = null,
                            tint = Color(0xFFA32D2D),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Dar de alta",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        buildAnnotatedString {
                            withStyle(SpanStyle(
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )) {
                                append("${currentPatient.nombre} ${currentPatient.apellido}")
                            }
                            append(" será retirado de la lista de pacientes activos.")
                        },
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 22.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Esta acción no se puede deshacer.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                    Spacer(Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showAltaDialog = false },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) { Text("Cancelar", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        Button(
                            onClick = {
                                db.collection("pacientes").document(currentPatient.id)
                                    .update(mapOf("activo" to false, "numeroCama" to "", "diagnostico" to ""))
                                    .addOnSuccessListener { showAltaDialog = false; onPatientDischarged() }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFA32D2D),
                                contentColor   = Color(0xFFF7C1C1)
                            )
                        ) { Text("Dar de alta", fontWeight = FontWeight.Medium) }
                    }
                }
            },
            confirmButton = {}
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Evaluación", color = Color.White, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBlue)
            )
        },
        bottomBar = {
            SentyCareBottomBar(
                currentTab = SentyCareTab.EVALUACION,
                onInicioClick = onHomeClick,
                onEvaluacionClick = {},
                onHistorialClick = onHistorialClick,
                onInfoClick = onInfoClick
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF0F4F8))
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {

                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        val genderColor = when (currentPatient.genero.lowercase()) {
                            "femenino" -> Color(0xFFE91E8C)
                            else -> DarkBlue
                        }
                        Box(
                            modifier = Modifier.size(44.dp).clip(CircleShape).border(2.dp, genderColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.Person, contentDescription = null, tint = genderColor, modifier = Modifier.size(28.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${currentPatient.nombre} ${currentPatient.apellido}", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = DarkBlue)
                            Text(currentPatient.diagnostico, fontSize = 13.sp, color = Color.Gray)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Color(0xFFEEEEEE))
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        PatientInfoChip(label = "Cama", value = currentPatient.numeroCama, modifier = Modifier.weight(1f))
                        PatientInfoChip(label = "RH", value = currentPatient.rh, modifier = Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        PatientInfoChip(label = "Género", value = currentPatient.genero, modifier = Modifier.weight(1f))
                        PatientInfoChip(label = "Fecha nac.", value = currentPatient.fechaNacimiento, modifier = Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Color(0xFFEEEEEE))
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { newCamaValue = currentPatient.numeroCama; showCamaDialog = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBlue),
                            contentPadding = PaddingValues(vertical = 10.dp)
                        ) {
                            Icon(Icons.Outlined.Edit, contentDescription = null, tint = DarkBlue, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Cama", fontSize = 13.sp, color = DarkBlue, fontWeight = FontWeight.SemiBold)
                        }
                        OutlinedButton(
                            onClick = { showAltaDialog = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE53935)),
                            contentPadding = PaddingValues(vertical = 10.dp)
                        ) {
                            Icon(Icons.Outlined.ExitToApp, contentDescription = null, tint = Color(0xFFE53935), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Alta", fontSize = 13.sp, color = Color(0xFFE53935), fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Color(0xFFEEEEEE))
                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Seleccione la escala a aplicar", fontSize = 14.sp, color = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            ScaleCard(title = "COMFORT B", subtitle = "Nivel de sedación (6-30)", circleColor = Color(0xFF4CAF50), emoji = "🧠", onClick = onComfortClick)
            Spacer(modifier = Modifier.height(12.dp))
            ScaleCard(title = "RASS", subtitle = "Agitación – Sedación (+4 a -5)", circleColor = Color(0xFFFF9800), emoji = "💭", onClick = onRassClick)
            Spacer(modifier = Modifier.height(12.dp))
            ScaleCard(title = "DOLOR", subtitle = "FLACC / FACES", circleColor = Color(0xFFF44336), emoji = "😊", onClick = onDolorClick)
        }
    }
}

@Composable
fun PatientInfoChip(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(end = 8.dp)) {
        Text(text = label, fontSize = 11.sp, color = LightGray, fontWeight = FontWeight.Medium)
        Text(text = value, fontSize = 14.sp, color = DarkBlue, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun ScaleCard(title: String, subtitle: String, circleColor: Color, emoji: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(circleColor.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                Text(text = emoji, fontSize = 24.sp)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = DarkBlue)
                Text(text = subtitle, fontSize = 13.sp, color = Color.Gray)
            }
            Icon(imageVector = Icons.Outlined.ChevronRight, contentDescription = null, tint = LightGray)
        }
    }
}

enum class SentyCareTab { INICIO, EVALUACION, HISTORIAL, INFO }

@Composable
fun SentyCareBottomBar(
    currentTab: SentyCareTab,
    onInicioClick: () -> Unit,
    onEvaluacionClick: () -> Unit,
    onHistorialClick: () -> Unit,
    onInfoClick: () -> Unit
) {
    NavigationBar(containerColor = Color.White, tonalElevation = 4.dp) {
        NavigationBarItem(
            selected = currentTab == SentyCareTab.INICIO,
            onClick = onInicioClick,
            icon = { Icon(Icons.Filled.Home, contentDescription = "Inicio") },
            label = { Text("Inicio") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = DarkBlue, selectedTextColor = DarkBlue,
                indicatorColor = Color(0xFFE8EAF6),
                unselectedIconColor = LightGray, unselectedTextColor = LightGray
            )
        )
        NavigationBarItem(
            selected = currentTab == SentyCareTab.EVALUACION,
            onClick = onEvaluacionClick,
            icon = { Icon(Icons.Outlined.Assignment, contentDescription = "Evaluación") },
            label = { Text("Evaluación") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = DarkBlue, selectedTextColor = DarkBlue,
                indicatorColor = Color(0xFFE8EAF6),
                unselectedIconColor = LightGray, unselectedTextColor = LightGray
            )
        )
        NavigationBarItem(
            selected = currentTab == SentyCareTab.HISTORIAL,
            onClick = onHistorialClick,
            icon = { Icon(Icons.Outlined.AccessTime, contentDescription = "Historial") },
            label = { Text("Historial") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = DarkBlue, selectedTextColor = DarkBlue,
                indicatorColor = Color(0xFFE8EAF6),
                unselectedIconColor = LightGray, unselectedTextColor = LightGray
            )
        )
        NavigationBarItem(
            selected = currentTab == SentyCareTab.INFO,
            onClick = onInfoClick,
            icon = { Icon(Icons.Filled.Info, contentDescription = "Info") },
            label = { Text("Info") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = DarkBlue, selectedTextColor = DarkBlue,
                indicatorColor = Color(0xFFE8EAF6),
                unselectedIconColor = LightGray, unselectedTextColor = LightGray
            )
        )
    }
}
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
        AlertDialog(
            onDismissRequest = { showCamaDialog = false; newCamaValue = "" },
            title = { Text("Modificar Cama", fontWeight = FontWeight.Bold, color = DarkBlue) },
            text = {
                Column {
                    Text("Nuevo número de cama para ${currentPatient.nombre}", color = Color.Gray, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newCamaValue,
                        onValueChange = { if (it.length <= 2) newCamaValue = it },
                        label = { Text("Número de cama") },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newCamaValue.isNotEmpty()) {
                            val camaFinal = newCamaValue
                            val patientId = currentPatient.id
                            currentPatient = currentPatient.copy(numeroCama = camaFinal)
                            onPatientUpdated(currentPatient)
                            showCamaDialog = false
                            newCamaValue = ""
                            db.collection("pacientes").document(patientId).update("numeroCama", camaFinal)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkBlue),
                    shape = RoundedCornerShape(8.dp)
                ) { Text("Guardar", color = Color.White) }
            },
            dismissButton = {
                OutlinedButton(onClick = { showCamaDialog = false; newCamaValue = "" }, shape = RoundedCornerShape(8.dp)) {
                    Text("Cancelar", color = DarkBlue)
                }
            },
            shape = RoundedCornerShape(12.dp)
        )
    }

    if (showAltaDialog) {
        AlertDialog(
            onDismissRequest = { showAltaDialog = false },
            title = { Text("Dar de Alta", fontWeight = FontWeight.Bold, color = Color(0xFFE53935)) },
            text = {
                Text(
                    "¿Confirma dar de alta a ${currentPatient.nombre} ${currentPatient.apellido}? Esta acción lo retirará de la lista de pacientes activos.",
                    fontSize = 14.sp, color = Color.Gray
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        db.collection("pacientes").document(currentPatient.id)
                            .update("activo", false)
                            .addOnSuccessListener { showAltaDialog = false; onPatientDischarged() }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                    shape = RoundedCornerShape(8.dp)
                ) { Text("Dar de Alta", color = Color.White) }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAltaDialog = false }, shape = RoundedCornerShape(8.dp)) {
                    Text("Cancelar", color = DarkBlue)
                }
            },
            shape = RoundedCornerShape(12.dp)
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
package com.example.sentycare

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalContext
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import com.example.sentycare.ui.theme.*
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun HomeScreen(
    doctorName: String = "Doctor",
    onRegisterClick: () -> Unit = {},
    onPatientClick: (Patient) -> Unit = {},
    onLogoutClick: () -> Unit = {}
) {
    BackHandler {}
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()

    var patients by remember { mutableStateOf<List<Patient>>(emptyList()) }
    var expandedPatientId by remember { mutableStateOf<String?>(null) }
    var patientToEditCama by remember { mutableStateOf<Patient?>(null) }
    var newCamaValue by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        db.collection("pacientes").get().addOnSuccessListener { result ->
            patients = result.documents.mapNotNull { doc ->
                doc.toObject(Patient::class.java)?.copy(id = doc.id)
            }.filter { it.activo }
        }
    }

    // Dialog modificar cama
    patientToEditCama?.let { patient ->
        AlertDialog(
            onDismissRequest = { patientToEditCama = null; newCamaValue = "" },
            title = { Text("Modificar Cama", fontWeight = FontWeight.Bold, color = DarkBlue) },
            text = {
                Column {
                    Text(
                        "Ingrese el nuevo número de cama para ${patient.nombre} ${patient.apellido}",
                        color = Color.Gray, fontSize = 14.sp
                    )
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
                            val patientId = patient.id
                            patients = patients.map {
                                if (it.id == patientId) it.copy(numeroCama = camaFinal) else it
                            }
                            focusManager.clearFocus()
                            patientToEditCama = null
                            newCamaValue = ""
                            db.collection("pacientes").document(patientId).update("numeroCama", camaFinal)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkBlue),
                    shape = RoundedCornerShape(8.dp)
                ) { Text("Guardar", color = Color.White) }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { patientToEditCama = null; newCamaValue = "" },
                    shape = RoundedCornerShape(8.dp)
                ) { Text("Cancelar", color = DarkBlue) }
            },
            shape = RoundedCornerShape(12.dp)
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {

        // HEADER
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkBlue)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.logosentycare),
                contentDescription = "Logo SentyCare",
                modifier = Modifier.size(48.dp).clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Bienvenido", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                Text(text = doctorName, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(
                    onClick = onRegisterClick,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Icon(imageVector = Icons.Outlined.PersonAdd, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Registrar", color = Color.White, fontSize = 16.sp)
                }
                Box(
                    modifier = Modifier.size(32.dp).clickable { openPdfFromAssets(context, "ManualSentyCare.pdf") },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Outlined.QuestionMark, contentDescription = "Manual", tint = Color.White, modifier = Modifier.size(18.dp))
                }
                Box(
                    modifier = Modifier.size(32.dp).clickable { onLogoutClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Outlined.Logout, contentDescription = "Cerrar sesión", tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }

        // LISTA DE PACIENTES
        if (patients.isEmpty()) {
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
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(patients, key = { it.id }) { patient ->
                    val isExpanded = expandedPatientId == patient.id
                    ExpandablePatientCard(
                        patient = patient,
                        isExpanded = isExpanded,
                        onCardClick = { expandedPatientId = if (isExpanded) null else patient.id },
                        onGoToPatient = { onPatientClick(patient) },
                        onEditCama = { newCamaValue = patient.numeroCama; patientToEditCama = patient },
                        onDischarge = {
                            db.collection("pacientes").document(patient.id)
                                .update("activo", false)
                                .addOnSuccessListener {
                                    patients = patients.filter { it.id != patient.id }
                                    if (expandedPatientId == patient.id) expandedPatientId = null
                                }
                        }
                    )
                }
                item { Spacer(modifier = Modifier.height(8.dp)) }
            }
        }
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
                    Text(text = "${patient.nombre} ${patient.apellido}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = DarkBlue)
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
                    Spacer(modifier = Modifier.height(12.dp))
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
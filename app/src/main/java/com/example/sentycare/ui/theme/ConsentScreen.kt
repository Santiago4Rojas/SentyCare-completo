package com.example.sentycare

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import com.example.sentycare.ui.theme.*
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsentScreen(
    patient: Patient,
    onBackClick: () -> Unit = {},
    onConsentAccepted: () -> Unit = {}
) {
    BackHandler { onBackClick() }
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()

    var acudienteNombre by remember { mutableStateOf("") }
    var acudienteCedula by remember { mutableStateOf("") }
    var acudienteTelefono by remember { mutableStateOf("") }
    var acudienteParentesco by remember { mutableStateOf("") }
    val parentescos = listOf("Madre", "Padre", "Tutor", "Acudiente")
    var parentescoExpanded by remember { mutableStateOf(false) }
    var consentAccepted by remember { mutableStateOf(false) }

    val canProceed =
        acudienteNombre.isNotBlank() &&
                acudienteCedula.length in 6..10 &&
                acudienteTelefono.length == 10 &&
                acudienteParentesco.isNotBlank() &&
                consentAccepted

    fun openLink(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    }

    fun saveConsent() {
        val updatedPatient = hashMapOf(
            "acudienteNombre"     to acudienteNombre,
            "acudienteCedula"     to acudienteCedula,
            "acudienteTelefono"   to acudienteTelefono,
            "acudienteParentesco" to acudienteParentesco
        )
        db.collection("pacientes")
            .whereEqualTo("noDoc", patient.noDoc)
            .get()
            .addOnSuccessListener { result ->
                result.documents.firstOrNull()?.reference?.update(updatedPatient as Map<String, Any>)
            }
        val consent = hashMapOf(
            "pacienteNombre"      to "${patient.nombre} ${patient.apellido}",
            "pacienteDocumento"   to patient.noDoc,
            "responsableNombre"   to acudienteNombre,
            "responsableCedula"   to acudienteCedula,
            "responsableTelefono" to acudienteTelefono,
            "parentesco"          to acudienteParentesco,
            "aceptado"            to true,
            "fecha"               to System.currentTimeMillis()
        )
        db.collection("consentimientos").add(consent)
            .addOnSuccessListener {
                Toast.makeText(context, "Consentimiento guardado", Toast.LENGTH_LONG).show()
                onConsentAccepted()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Error al guardar", Toast.LENGTH_LONG).show()
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Consentimiento Informado", color = Color.White, fontWeight = FontWeight.Bold) },
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
                .background(Color(0xFFF0F4F8))
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
                .pointerInput(Unit){ detectTapGestures(onTap = { focusManager.clearFocus() }) }
        ) {
            // ── Card principal ────────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(3.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {

                    // Paciente
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFEEF2FB), RoundedCornerShape(10.dp))
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.Person, contentDescription = null, tint = DarkBlue, modifier = Modifier.size(26.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("${patient.nombre} ${patient.apellido}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = DarkBlue)
                            Text("Cama ${patient.numeroCama} · ${patient.diagnostico}", fontSize = 13.sp, color = Color.Gray)
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    HorizontalDivider(color = Color(0xFFEEEEEE))
                    Spacer(modifier = Modifier.height(20.dp))

                    // Declaración
                    Text("Declaración", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = DarkBlue)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Declaro que he recibido información suficiente sobre las evaluaciones clínicas de sedación y dolor que se realizarán al paciente, comprendo su propósito asistencial y autorizo su aplicación por parte del personal de enfermería de la UCI Pediátrica.",
                        fontSize = 14.sp,
                        color = Color.DarkGray,
                        lineHeight = 22.sp
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider(color = Color(0xFFEEEEEE))
                    Spacer(modifier = Modifier.height(20.dp))

                    // Datos del acudiente
                    Text("Datos del Acudiente", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = DarkBlue)
                    Spacer(modifier = Modifier.height(16.dp))

                    // Nombre
                    Text("Nombre completo *", fontSize = 13.sp, color = DarkBlue, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = acudienteNombre,
                        onValueChange = { acudienteNombre = it },
                        placeholder = { Text("Ej: María Pérez", color = LightGray) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DarkBlue,
                            unfocusedBorderColor = LightGray
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Cédula — entre 6 y 10
                    LimitedFormField(
                        label = "Cédula *",
                        value = acudienteCedula,
                        onChange = { acudienteCedula = it },
                        placeholder = "Entre 6 y 10 dígitos",
                        keyboardType = KeyboardType.Number,
                        maxLength = 10,
                        minLength = 6
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Teléfono — exactamente 10
                    LimitedFormField(
                        label = "Teléfono *",
                        value = acudienteTelefono,
                        onChange = { acudienteTelefono = it },
                        placeholder = "10 dígitos",
                        keyboardType = KeyboardType.Phone,
                        maxLength = 10,
                        minLength = 10
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Parentesco
                    Text("Parentesco *", fontSize = 13.sp, color = DarkBlue, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(6.dp))
                    ExposedDropdownMenuBox(
                        expanded = parentescoExpanded,
                        onExpandedChange = { parentescoExpanded = !parentescoExpanded }
                    ) {
                        OutlinedTextField(
                            value = acudienteParentesco,
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            placeholder = { Text("Seleccione", color = LightGray) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = parentescoExpanded) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = DarkBlue,
                                unfocusedBorderColor = LightGray
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = parentescoExpanded,
                            onDismissRequest = { parentescoExpanded = false }
                        ) {
                            parentescos.forEach { item ->
                                DropdownMenuItem(
                                    text = { Text(item) },
                                    onClick = { acudienteParentesco = item; parentescoExpanded = false }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Checkbox + Leyes ──────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (consentAccepted) Color(0xFFE8F5E9) else Color.White
                ),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = consentAccepted,
                            onCheckedChange = { consentAccepted = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = DarkBlue,
                                uncheckedColor = LightGray
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Acepto el consentimiento informado y autorizo las evaluaciones clínicas del paciente.",
                            fontSize = 14.sp,
                            color = if (consentAccepted) DarkBlue else Color.DarkGray,
                            fontWeight = if (consentAccepted) FontWeight.Medium else FontWeight.Normal,
                            lineHeight = 20.sp
                        )
                    }

                    HorizontalDivider(color = Color(0xFFEEEEEE))

                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Text("Leyes", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = DarkBlue)
                        Spacer(modifier = Modifier.height(6.dp))
                        TextButton(
                            onClick = { openLink("https://www.funcionpublica.gov.co/eva/gestornormativo/norma.php?i=49981") },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                "🔗 Ley 1581 de 2012 - Protección de Datos Personales",
                                color = MediumBlue,
                                fontSize = 13.sp,
                                textDecoration = TextDecoration.Underline
                            )
                        }
                        TextButton(
                            onClick = { openLink("https://www.funcionpublica.gov.co/eva/gestornormativo/norma.php?i=34492") },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                "🔗 Ley 1273 de 2009 - Delitos Informáticos",
                                color = MediumBlue,
                                fontSize = 13.sp,
                                textDecoration = TextDecoration.Underline
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { saveConsent() },
                enabled = canProceed,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(10.dp),
                border = if (canProceed) null else BorderStroke(1.dp, LightGray),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DarkBlue,
                    contentColor = Color.White,
                    disabledContainerColor = Color.White,
                    disabledContentColor = LightGray
                )
            ) {
                Text("Confirmar Consentimiento", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
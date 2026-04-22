package com.example.sentycare

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sentycare.ui.theme.*
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onBackClick: () -> Unit = {},
    onCancelClick: () -> Unit = {},
    onRegisterClick: (Patient) -> Unit = {}
) {

    var nombre by remember { mutableStateOf("") }
    var apellido by remember { mutableStateOf("") }

    var genero by remember { mutableStateOf("") }
    val generos = listOf("Masculino", "Femenino")
    var generoExpanded by remember { mutableStateOf(false) }

    var noDoc by remember { mutableStateOf("") }
    var fechaNacimiento by remember { mutableStateOf("") }

    var rh by remember { mutableStateOf("") }
    val tiposDeSangre =
        listOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")
    var rhExpanded by remember { mutableStateOf(false) }

    var numeroCama by remember { mutableStateOf("") }
    var diagnostico by remember { mutableStateOf("") }

    // DATOS ACUDIENTE
    var acudienteNombre by remember { mutableStateOf("") }
    var acudienteCedula by remember { mutableStateOf("") }
    var acudienteTelefono by remember { mutableStateOf("") }
    var acudienteParentesco by remember { mutableStateOf("") }

    val parentescos = listOf(
        "Madre",
        "Padre",
        "Tutor",
        "Acudiente"
    )
    var parentescoExpanded by remember {
        mutableStateOf(false)
    }

    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()

    val calendar = Calendar.getInstance()

    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, day ->
            fechaNacimiento =
                "%02d/%02d/%04d".format(day, month + 1, year)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    val formValid =
        nombre.isNotBlank() &&
                apellido.isNotBlank() &&
                genero.isNotBlank() &&
                noDoc.isNotBlank() &&
                fechaNacimiento.isNotBlank() &&
                rh.isNotBlank() &&
                numeroCama.isNotBlank() &&
                diagnostico.isNotBlank() &&
                acudienteNombre.isNotBlank() &&
                acudienteCedula.isNotBlank() &&
                acudienteTelefono.isNotBlank() &&
                acudienteParentesco.isNotBlank()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Registrar Paciente",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector =
                                Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBlue
                )
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
        ) {

            FormField(
                "Nombre *",
                nombre,
                { nombre = it },
                "Ej: Juan"
            )

            Spacer(modifier = Modifier.height(14.dp))

            FormField(
                "Apellido *",
                apellido,
                { apellido = it },
                "Ej: Pérez"
            )

            Spacer(modifier = Modifier.height(14.dp))

            DropdownField(
                label = "Género *",
                value = genero,
                items = generos,
                expanded = generoExpanded,
                onExpandedChange = {
                    generoExpanded = !generoExpanded
                },
                onSelect = {
                    genero = it
                    generoExpanded = false
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            FormField(
                "Documento paciente *",
                noDoc,
                { noDoc = it },
                "Ej: 123456789",
                KeyboardType.Number
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Fecha nacimiento *",
                color = DarkBlue
            )

            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = fechaNacimiento,
                onValueChange = {},
                readOnly = true,
                trailingIcon = {
                    IconButton(
                        onClick = {
                            datePickerDialog.show()
                        }
                    ) {
                        Icon(
                            imageVector =
                                Icons.Outlined.CalendarMonth,
                            contentDescription = null
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            DropdownField(
                label = "Tipo sangre *",
                value = rh,
                items = tiposDeSangre,
                expanded = rhExpanded,
                onExpandedChange = {
                    rhExpanded = !rhExpanded
                },
                onSelect = {
                    rh = it
                    rhExpanded = false
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            FormField(
                "Número cama *",
                numeroCama,
                { numeroCama = it },
                "Ej: 5"
            )

            Spacer(modifier = Modifier.height(14.dp))

            FormField(
                "Diagnóstico *",
                diagnostico,
                { diagnostico = it },
                "Ej: Neumonía"
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Datos del Acudiente",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = DarkBlue
            )

            Spacer(modifier = Modifier.height(14.dp))

            FormField(
                "Nombre acudiente *",
                acudienteNombre,
                { acudienteNombre = it },
                "Ej: María Pérez"
            )

            Spacer(modifier = Modifier.height(14.dp))

            FormField(
                "Cédula acudiente *",
                acudienteCedula,
                { acudienteCedula = it },
                "Ej: 10203040",
                KeyboardType.Number
            )

            Spacer(modifier = Modifier.height(14.dp))

            FormField(
                "Teléfono acudiente *",
                acudienteTelefono,
                { acudienteTelefono = it },
                "Ej: 3001234567",
                KeyboardType.Phone
            )

            Spacer(modifier = Modifier.height(14.dp))

            DropdownField(
                label = "Parentesco *",
                value = acudienteParentesco,
                items = parentescos,
                expanded = parentescoExpanded,
                onExpandedChange = {
                    parentescoExpanded =
                        !parentescoExpanded
                },
                onSelect = {
                    acudienteParentesco = it
                    parentescoExpanded = false
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(12.dp)
            ) {

                OutlinedButton(
                    onClick = onCancelClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancelar")
                }

                Button(
                    onClick = {

                        val patient = Patient(
                            nombre = nombre,
                            apellido = apellido,
                            genero = genero,
                            noDoc = noDoc,
                            fechaNacimiento =
                                fechaNacimiento,
                            rh = rh,
                            numeroCama =
                                numeroCama,
                            diagnostico =
                                diagnostico,

                            acudienteNombre =
                                acudienteNombre,
                            acudienteCedula =
                                acudienteCedula,
                            acudienteTelefono =
                                acudienteTelefono,
                            acudienteParentesco =
                                acudienteParentesco
                        )

                        db.collection("pacientes")
                            .add(patient)
                            .addOnSuccessListener {

                                Toast.makeText(
                                    context,
                                    "Paciente registrado",
                                    Toast.LENGTH_LONG
                                ).show()

                                onRegisterClick(patient)
                            }
                    },
                    enabled = formValid,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DarkBlue
                    )
                ) {
                    Text(
                        "Registrar",
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun FormField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType =
        KeyboardType.Text
) {

    Text(
        text = label,
        color = DarkBlue
    )

    Spacer(modifier = Modifier.height(6.dp))

    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        placeholder = { Text(placeholder) },
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType
        ),
        modifier = Modifier.fillMaxWidth()
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

    Text(
        text = label,
        color = DarkBlue
    )

    Spacer(modifier = Modifier.height(6.dp))

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = {
            onExpandedChange()
        }
    ) {

        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),

            trailingIcon = {
                ExposedDropdownMenuDefaults
                    .TrailingIcon(
                        expanded = expanded
                    )
            }
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                onExpandedChange()
            }
        ) {

            items.forEach { item ->

                DropdownMenuItem(
                    text = { Text(item) },
                    onClick = {
                        onSelect(item)
                    }
                )
            }
        }
    }
}
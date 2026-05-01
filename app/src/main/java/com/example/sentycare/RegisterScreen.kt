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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import com.example.sentycare.ui.theme.*
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.material3.OutlinedTextFieldDefaults
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onBackClick: () -> Unit = {},
    onCancelClick: () -> Unit = {},
    onRegisterClick: (Patient) -> Unit = {}
) {
    BackHandler { onBackClick() }
    val focusManager = LocalFocusManager.current
    var nombre by remember { mutableStateOf("") }
    var apellido by remember { mutableStateOf("") }
    var genero by remember { mutableStateOf("") }
    val generos = listOf("Masculino", "Femenino")
    var generoExpanded by remember { mutableStateOf(false) }
    var noDoc by remember { mutableStateOf("") }
    var fechaNacimiento by remember { mutableStateOf("") }
    var rh by remember { mutableStateOf("") }
    val tiposDeSangre = listOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")
    var rhExpanded by remember { mutableStateOf(false) }
    var numeroCama by remember { mutableStateOf("") }
    var diagnostico by remember { mutableStateOf("") }

    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
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

    val formValid =
        nombre.isNotBlank() &&
                apellido.isNotBlank() &&
                genero.isNotBlank() &&
                noDoc.length == 10 &&
                fechaNacimiento.isNotBlank() &&
                rh.isNotBlank() &&
                numeroCama.isNotBlank() &&
                numeroCama.length <= 2 &&
                diagnostico.isNotBlank()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Registrar Paciente", color = Color.White, fontWeight = FontWeight.Bold)
                },
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
                .pointerInput(Unit){
                    detectTapGestures(onTap = { focusManager.clearFocus() })
                }
        ) {
            Text("Datos del Paciente", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = DarkBlue)
            Spacer(modifier = Modifier.height(16.dp))

            FormField("Nombre *", nombre, { nombre = it }, "Ej: Juan")
            Spacer(modifier = Modifier.height(14.dp))

            FormField("Apellido *", apellido, { apellido = it }, "Ej: Pérez")
            Spacer(modifier = Modifier.height(14.dp))

            DropdownField(
                label = "Género *",
                value = genero,
                items = generos,
                expanded = generoExpanded,
                onExpandedChange = { generoExpanded = !generoExpanded },
                onSelect = { selected -> genero = selected; generoExpanded = false }
            )
            Spacer(modifier = Modifier.height(14.dp))

            LimitedFormField(
                label = "Documento paciente *",
                value = noDoc,
                onChange = { noDoc = it },
                placeholder = "10 dígitos",
                keyboardType = KeyboardType.Number,
                maxLength = 10,
                minLength = 10
            )
            Spacer(modifier = Modifier.height(14.dp))

            Text("Fecha nacimiento *", color = DarkBlue, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = fechaNacimiento,
                onValueChange = {},
                readOnly = true,
                placeholder = { Text("DD/MM/AAAA", color = LightGray) },
                trailingIcon = {
                    IconButton(onClick = { datePickerDialog.show() }) {
                        Icon(Icons.Outlined.CalendarMonth, contentDescription = null)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { datePickerDialog.show() },
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DarkBlue,
                    unfocusedBorderColor = LightGray,
                    disabledBorderColor = LightGray,
                    disabledTextColor = Color.Black,
                    disabledPlaceholderColor = LightGray
                ),
                enabled = false
            )
            Spacer(modifier = Modifier.height(14.dp))

            DropdownField(
                label = "Tipo de sangre *",
                value = rh,
                items = tiposDeSangre,
                expanded = rhExpanded,
                onExpandedChange = { rhExpanded = !rhExpanded },
                onSelect = { selected -> rh = selected; rhExpanded = false }
            )
            Spacer(modifier = Modifier.height(14.dp))

            LimitedFormField(
                label = "Número de cama *",
                value = numeroCama,
                onChange = { numeroCama = it },
                placeholder = "Máx. 2 caracteres",
                keyboardType = KeyboardType.Number,
                maxLength = 2,
                minLength = 1
            )
            Spacer(modifier = Modifier.height(14.dp))

            FormField("Diagnóstico *", diagnostico, { diagnostico = it }, "Ej: Neumonía")
            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onCancelClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBlue)
                ) {
                    Text("Cancelar", color = DarkBlue)
                }
                Button(
                    onClick = {
                        val patient = Patient(
                            nombre = nombre,
                            apellido = apellido,
                            genero = genero,
                            noDoc = noDoc,
                            fechaNacimiento = fechaNacimiento,
                            rh = rh,
                            numeroCama = numeroCama,
                            diagnostico = diagnostico
                        )
                        db.collection("pacientes").add(patient).addOnSuccessListener {
                            Toast.makeText(context, "Paciente registrado", Toast.LENGTH_LONG).show()
                            onRegisterClick(patient)
                        }
                    },
                    enabled = formValid,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    border = if (formValid) null else BorderStroke(1.dp, LightGray),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DarkBlue,
                        contentColor = Color.White,
                        disabledContainerColor = Color.White,
                        disabledContentColor = LightGray
                    )
                ) {
                    Text("Registrar", fontWeight = FontWeight.SemiBold)
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
    keyboardType: KeyboardType = KeyboardType.Text
) {
    Text(text = label, color = DarkBlue, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    Spacer(modifier = Modifier.height(6.dp))
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        placeholder = { Text(placeholder, color = LightGray) },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = DarkBlue,
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
    val isError = value.isNotEmpty() && value.length < minLength
    val isComplete = value.length >= minLength && value.length <= maxLength && value.isNotEmpty()

    Text(text = label, color = DarkBlue, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    Spacer(modifier = Modifier.height(6.dp))
    OutlinedTextField(
        value = value,
        onValueChange = { if (it.length <= maxLength) onChange(it) },
        placeholder = { Text(placeholder, color = LightGray) },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        singleLine = true,
        isError = isError,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = when {
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = when {
                        isError    -> "Mínimo $minLength caracteres"
                        isComplete -> "✓ Correcto"
                        else       -> ""
                    },
                    fontSize = 11.sp,
                    color = when {
                        isError    -> Color(0xFFF44336)
                        isComplete -> Color(0xFF4CAF50)
                        else       -> Color.Transparent
                    }
                )
                Text(
                    text = "${value.length}/$maxLength",
                    fontSize = 11.sp,
                    color = if (isComplete) Color(0xFF4CAF50) else LightGray
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
    Text(text = label, color = DarkBlue, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    Spacer(modifier = Modifier.height(6.dp))
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { onExpandedChange() }) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            placeholder = { Text("Seleccione", color = LightGray) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = DarkBlue,
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
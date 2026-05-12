package com.example.sentycare

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sentycare.data.Rol
import com.example.sentycare.data.Usuario
import com.example.sentycare.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    onLogoutClick: () -> Unit = {}
) {
    BackHandler {}
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()

    var usuarios by remember { mutableStateOf<List<Usuario>>(emptyList()) }
    var cargando by remember { mutableStateOf(true) }
    var showCrearDialog by remember { mutableStateOf(false) }
    var showEditarDialog by remember { mutableStateOf<Usuario?>(null) }

    fun cargarUsuarios() {
        cargando = true
        db.collection("usuarios").get().addOnSuccessListener { result ->
            usuarios = result.documents.mapNotNull { doc ->
                doc.toObject(Usuario::class.java)?.copy(id = doc.id)
            }.sortedBy { it.nombre }
            cargando = false
        }.addOnFailureListener { cargando = false }
    }

    LaunchedEffect(Unit) { cargarUsuarios() }

    if (showCrearDialog) {
        CrearUsuarioDialog(
            onDismiss = { showCrearDialog = false },
            onCrear = { email, password, usuario ->
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnSuccessListener { result ->
                        val uid = result.user?.uid ?: return@addOnSuccessListener
                        db.collection("usuarios").document(uid).set(
                            mapOf(
                                "nombre"       to usuario.nombre,
                                "apellido"     to usuario.apellido,
                                "email"        to email,
                                "rol"          to usuario.rol,
                                "especialidad" to usuario.especialidad,
                                "noDoc"        to usuario.noDoc,
                                "rh"           to usuario.rh,
                                "activo"       to true,
                                "creadoEn"     to System.currentTimeMillis()
                            )
                        ).addOnSuccessListener {
                            Toast.makeText(context, "Usuario creado correctamente", Toast.LENGTH_SHORT).show()
                            showCrearDialog = false
                            cargarUsuarios()
                        }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        )
    }

    showEditarDialog?.let { usuario ->
        EditarUsuarioDialog(
            usuario = usuario,
            onDismiss = { showEditarDialog = null },
            onGuardar = { datos ->
                db.collection("usuarios").document(usuario.id)
                    .update(datos)
                    .addOnSuccessListener {
                        Toast.makeText(context, "Usuario actualizado", Toast.LENGTH_SHORT).show()
                        showEditarDialog = null
                        cargarUsuarios()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gestión de Usuarios", color = Color.White, fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onLogoutClick) {
                        Icon(Icons.Outlined.Logout, contentDescription = "Cerrar sesión", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBlue)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCrearDialog = true },
                containerColor = DarkBlue,
                contentColor = Color.White,
                shape = RoundedCornerShape(14.dp),
                icon = { Icon(Icons.Outlined.PersonAdd, contentDescription = null) },
                text = { Text("Nuevo usuario", fontWeight = FontWeight.SemiBold) }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).background(Color(0xFFF0F4F8))) {
            when {
                cargando -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = DarkBlue)
                }
                usuarios.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No hay usuarios registrados", color = LightGray)
                }
                else -> {
                    val activos = usuarios.filter { it.activo }
                    val inactivos = usuarios.filter { !it.activo }
                    var showInactivos by remember { mutableStateOf(false) }
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
                    ) {
                        if (activos.isNotEmpty()) {
                            item {
                                Text(
                                    "Usuarios activos (${activos.size})",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(bottom = 2.dp)
                                )
                            }
                            items(activos) { usuario ->
                                UsuarioCard(usuario = usuario, onEditar = { showEditarDialog = usuario })
                            }
                        }
                        if (inactivos.isNotEmpty()) {
                            item {
                                Spacer(Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { showInactivos = !showInactivos }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Usuarios inactivos (${inactivos.size})",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFFE53935),
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(
                                        imageVector = if (showInactivos) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                                        contentDescription = null,
                                        tint = Color(0xFFE53935),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            if (showInactivos) {
                                items(inactivos) { usuario ->
                                    UsuarioCard(usuario = usuario, onEditar = { showEditarDialog = usuario })
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UsuarioCard(usuario: Usuario, onEditar: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "${usuario.nombre} ${usuario.apellido}".trim(),
                    fontSize = 16.sp, fontWeight = FontWeight.Bold, color = DarkBlue
                )
                Text(usuario.email, fontSize = 12.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    RolChip(Rol.fromString(usuario.rol))
                    if (usuario.especialidad.isNotBlank()) {
                        Text(usuario.especialidad, fontSize = 11.sp, color = Color.Gray)
                    }
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = onEditar) {
                    Icon(Icons.Outlined.Edit, contentDescription = "Editar", tint = DarkBlue)
                }
                if (!usuario.activo) {
                    Text("Inactivo", fontSize = 10.sp, color = Color(0xFFE53935))
                }
            }
        }
    }
}

@Composable
fun RolChip(rol: Rol) {
    val (color, text) = when (rol) {
        Rol.ADMIN -> Color(0xFF7B1FA2) to "Admin"
        Rol.JEFE_UNIDAD -> Color(0xFF1565C0) to "Jefe UCI"
        Rol.MEDICO_ESPECIALISTA -> Color(0xFF2E7D32) to "Especialista"
        Rol.MEDICO_RESIDENTE -> Color(0xFF00838F) to "Residente"
        Rol.ENFERMERA -> Color(0xFFE65100) to "Enfermería"
    }
    Surface(shape = RoundedCornerShape(20.dp), color = color.copy(alpha = 0.12f)) {
        Text(text, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), fontSize = 11.sp, color = color, fontWeight = FontWeight.SemiBold)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditarUsuarioDialog(usuario: Usuario, onDismiss: () -> Unit, onGuardar: (Map<String, Any>) -> Unit) {
    val tiposDeSangre = listOf("O+", "O-", "A+", "A-", "B+", "B-", "AB+", "AB-")
    var nombre by remember { mutableStateOf(usuario.nombre) }
    var apellido by remember { mutableStateOf(usuario.apellido) }
    var rolSeleccionado by remember { mutableStateOf(Rol.fromString(usuario.rol)) }
    var especialidad by remember { mutableStateOf(usuario.especialidad) }
    var noDoc by remember { mutableStateOf(usuario.noDoc) }
    var rh by remember { mutableStateOf(usuario.rh) }
    var activo by remember { mutableStateOf(usuario.activo) }
    var showRolMenu by remember { mutableStateOf(false) }
    var showRhMenu by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(16.dp),
        title = { Text("Editar usuario", fontWeight = FontWeight.Bold, color = DarkBlue) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = nombre, onValueChange = { nombre = it }, label = { Text("Nombre") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), singleLine = true)
                OutlinedTextField(value = apellido, onValueChange = { apellido = it }, label = { Text("Apellido") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), singleLine = true)

                ExposedDropdownMenuBox(expanded = showRolMenu, onExpandedChange = { showRolMenu = it }) {
                    OutlinedTextField(
                        value = rolSeleccionado.displayName, onValueChange = {}, readOnly = true,
                        label = { Text("Rol") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showRolMenu) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(), shape = RoundedCornerShape(8.dp)
                    )
                    ExposedDropdownMenu(expanded = showRolMenu, onDismissRequest = { showRolMenu = false }) {
                        Rol.entries.forEach { r ->
                            DropdownMenuItem(text = { Text(r.displayName) }, onClick = { rolSeleccionado = r; showRolMenu = false })
                        }
                    }
                }

                OutlinedTextField(value = especialidad, onValueChange = { especialidad = it }, label = { Text("Especialidad") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), singleLine = true)
                OutlinedTextField(
                    value = noDoc,
                    onValueChange = { if (it.length <= 10 && it.all { c -> c.isDigit() }) noDoc = it },
                    label = { Text("Documento de identidad") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true,
                    supportingText = { Text("6–10 dígitos", fontSize = 11.sp, color = Color.Gray) }
                )
                ExposedDropdownMenuBox(expanded = showRhMenu, onExpandedChange = { showRhMenu = it }) {
                    OutlinedTextField(
                        value = rh, onValueChange = {}, readOnly = true,
                        label = { Text("RH / Tipo de sangre") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showRhMenu) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(), shape = RoundedCornerShape(8.dp)
                    )
                    ExposedDropdownMenu(expanded = showRhMenu, onDismissRequest = { showRhMenu = false }) {
                        tiposDeSangre.forEach { tipo ->
                            DropdownMenuItem(text = { Text(tipo) }, onClick = { rh = tipo; showRhMenu = false })
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = activo, onCheckedChange = { activo = it }, colors = CheckboxDefaults.colors(checkedColor = DarkBlue))
                    Text("Usuario activo", fontSize = 14.sp, color = DarkBlue)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onGuardar(mapOf(
                        "nombre"       to nombre,
                        "apellido"     to apellido,
                        "rol"          to rolSeleccionado.name,
                        "especialidad" to especialidad,
                        "noDoc"        to noDoc,
                        "rh"           to rh,
                        "activo"       to activo
                    ))
                },
                colors = ButtonDefaults.buttonColors(containerColor = DarkBlue),
                shape = RoundedCornerShape(8.dp)
            ) { Text("Guardar") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(8.dp)) { Text("Cancelar") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrearUsuarioDialog(onDismiss: () -> Unit, onCrear: (String, String, Usuario) -> Unit) {
    var nombre by remember { mutableStateOf("") }
    var apellido by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    val tiposDeSangre = listOf("O+", "O-", "A+", "A-", "B+", "B-", "AB+", "AB-")
    var rolSeleccionado by remember { mutableStateOf(Rol.ENFERMERA) }
    var especialidad by remember { mutableStateOf("") }
    var noDoc by remember { mutableStateOf("") }
    var rh by remember { mutableStateOf("") }
    var showRolMenu by remember { mutableStateOf(false) }
    var showRhMenu by remember { mutableStateOf(false) }
    var showPassword by remember { mutableStateOf(false) }
    var showConfirm by remember { mutableStateOf(false) }

    val hasUpper = password.any { it.isUpperCase() }
    val hasLower = password.any { it.isLowerCase() }
    val hasDigit = password.any { it.isDigit() }
    val hasSpecial = password.any { !it.isLetterOrDigit() }
    val hasLength = password.length >= 8
    val passwordsMatch = password == confirmPassword && password.isNotBlank()
    val passwordValido = hasUpper && hasLower && hasDigit && hasSpecial && hasLength && passwordsMatch

    val esValido = nombre.isNotBlank() && email.isNotBlank() && passwordValido

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(16.dp),
        title = { Text("Crear usuario", fontWeight = FontWeight.Bold, color = DarkBlue) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = nombre, onValueChange = { nombre = it }, label = { Text("Nombre *") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), singleLine = true)
                OutlinedTextField(value = apellido, onValueChange = { apellido = it }, label = { Text("Apellido") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), singleLine = true)
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Correo electrónico *") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), singleLine = true)
                OutlinedTextField(
                    value = password, onValueChange = { password = it },
                    label = { Text("Contraseña *") }, modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp), singleLine = true,
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(if (showPassword) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, null, tint = Color.Gray)
                        }
                    }
                )
                OutlinedTextField(
                    value = confirmPassword, onValueChange = { confirmPassword = it },
                    label = { Text("Confirmar contraseña *") }, modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp), singleLine = true,
                    isError = confirmPassword.isNotBlank() && !passwordsMatch,
                    visualTransformation = if (showConfirm) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showConfirm = !showConfirm }) {
                            Icon(if (showConfirm) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, null, tint = Color.Gray)
                        }
                    }
                )
                if (password.isNotBlank()) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        PasswordRequirement("Mínimo 8 caracteres", hasLength)
                        PasswordRequirement("Mayúscula (A-Z)", hasUpper)
                        PasswordRequirement("Minúscula (a-z)", hasLower)
                        PasswordRequirement("Número (0-9)", hasDigit)
                        PasswordRequirement("Carácter especial (!@#\$...)", hasSpecial)
                        if (confirmPassword.isNotBlank()) PasswordRequirement("Contraseñas coinciden", passwordsMatch)
                    }
                }

                ExposedDropdownMenuBox(expanded = showRolMenu, onExpandedChange = { showRolMenu = it }) {
                    OutlinedTextField(
                        value = rolSeleccionado.displayName, onValueChange = {}, readOnly = true,
                        label = { Text("Rol *") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showRolMenu) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(), shape = RoundedCornerShape(8.dp)
                    )
                    ExposedDropdownMenu(expanded = showRolMenu, onDismissRequest = { showRolMenu = false }) {
                        Rol.entries.forEach { r ->
                            DropdownMenuItem(text = { Text(r.displayName) }, onClick = { rolSeleccionado = r; showRolMenu = false })
                        }
                    }
                }

                OutlinedTextField(value = especialidad, onValueChange = { especialidad = it }, label = { Text("Especialidad") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), singleLine = true)
                OutlinedTextField(
                    value = noDoc,
                    onValueChange = { if (it.length <= 10 && it.all { c -> c.isDigit() }) noDoc = it },
                    label = { Text("Documento de identidad") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true,
                    supportingText = { Text("6–10 dígitos", fontSize = 11.sp, color = Color.Gray) }
                )
                ExposedDropdownMenuBox(expanded = showRhMenu, onExpandedChange = { showRhMenu = it }) {
                    OutlinedTextField(
                        value = rh, onValueChange = {}, readOnly = true,
                        label = { Text("RH / Tipo de sangre") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showRhMenu) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(), shape = RoundedCornerShape(8.dp)
                    )
                    ExposedDropdownMenu(expanded = showRhMenu, onDismissRequest = { showRhMenu = false }) {
                        tiposDeSangre.forEach { tipo ->
                            DropdownMenuItem(text = { Text(tipo) }, onClick = { rh = tipo; showRhMenu = false })
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val usuario = Usuario(nombre = nombre, apellido = apellido, rol = rolSeleccionado.name, especialidad = especialidad, noDoc = noDoc, rh = rh)
                    onCrear(email, password, usuario)
                },
                enabled = esValido,
                colors = ButtonDefaults.buttonColors(containerColor = DarkBlue),
                shape = RoundedCornerShape(8.dp)
            ) { Text("Crear") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(8.dp)) { Text("Cancelar") }
        }
    )
}

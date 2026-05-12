package com.example.sentycare

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NotificationsActive
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
import androidx.core.content.ContextCompat
import com.example.sentycare.ui.theme.DarkBlue

@Composable
fun ReminderButton(
    paciente: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }
    var horas by remember { mutableStateOf("") }
    var minutos by remember { mutableStateOf("") }
    var segundos by remember { mutableStateOf("") }

    val notifPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) showDialog = true
        else Toast.makeText(context, "Permiso de notificaciones denegado", Toast.LENGTH_SHORT).show()
    }

    OutlinedButton(
        onClick = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val perm = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                if (perm == PackageManager.PERMISSION_GRANTED) showDialog = true
                else notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                showDialog = true
            }
        },
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBlue)
    ) {
        Icon(Icons.Outlined.NotificationsActive, contentDescription = null, tint = DarkBlue, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text("Recordatorio", color = DarkBlue, fontSize = 13.sp)
    }

    if (showDialog) {
        val h = horas.toIntOrNull() ?: 0
        val m = minutos.toIntOrNull() ?: 0
        val s = segundos.toIntOrNull() ?: 0
        val totalMs = (h * 3600L + m * 60L + s) * 1000L
        val esValido = totalMs > 0

        AlertDialog(
            onDismissRequest = { showDialog = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp),
            title = { Text("Programar recordatorio", fontWeight = FontWeight.Bold, color = DarkBlue) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Ingrese el tiempo para la alerta:", fontSize = 14.sp, color = Color.Gray)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TimeField(
                            value = horas,
                            onValueChange = { if (it.length <= 2 && (it.toIntOrNull() ?: 0) <= 23) horas = it },
                            label = "h",
                            modifier = Modifier.weight(1f)
                        )
                        Text(":", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = DarkBlue)
                        TimeField(
                            value = minutos,
                            onValueChange = { if (it.length <= 2 && (it.toIntOrNull() ?: 0) <= 59) minutos = it },
                            label = "min",
                            modifier = Modifier.weight(1f)
                        )
                        Text(":", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = DarkBlue)
                        TimeField(
                            value = segundos,
                            onValueChange = { if (it.length <= 2 && (it.toIntOrNull() ?: 0) <= 59) segundos = it },
                            label = "seg",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (esValido) {
                        val resumen = buildString {
                            if (h > 0) append("$h h ")
                            if (m > 0) append("$m min ")
                            if (s > 0) append("$s seg")
                        }.trim()
                        Text("Recordatorio en: $resumen", fontSize = 12.sp, color = DarkBlue)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        ReminderHelper.programarRecordatorio(
                            context = context,
                            delayMs = totalMs,
                            paciente = paciente
                        )
                        val resumen = buildString {
                            if (h > 0) append("$h h ")
                            if (m > 0) append("$m min ")
                            if (s > 0) append("$s seg")
                        }.trim()
                        Toast.makeText(context, "Recordatorio en $resumen", Toast.LENGTH_SHORT).show()
                        showDialog = false
                        horas = ""; minutos = ""; segundos = ""
                    },
                    enabled = esValido,
                    colors = ButtonDefaults.buttonColors(containerColor = DarkBlue),
                    shape = RoundedCornerShape(8.dp)
                ) { Text("Programar") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun TimeField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            placeholder = { Text("0", color = Color.LightGray) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = DarkBlue,
                unfocusedBorderColor = Color(0xFFCCCCCC)
            )
        )
        Text(label, fontSize = 11.sp, color = Color.Gray)
    }
}

package com.example.sentycare

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.sentycare.ui.theme.DarkBlue

private val OPCIONES_TIEMPO = listOf(
    "15 minutos"  to 15L * 60_000,
    "30 minutos"  to 30L * 60_000,
    "1 hora"      to 60L * 60_000,
    "2 horas"     to 120L * 60_000,
    "4 horas"     to 240L * 60_000
)

@Composable
fun ReminderButton(
    paciente: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }

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
        AlertDialog(
            onDismissRequest = { showDialog = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp),
            title = { Text("Programar recordatorio", fontWeight = FontWeight.Bold, color = DarkBlue) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("¿En cuánto tiempo desea recibir la alerta?", fontSize = 14.sp, color = Color.Gray)
                    OPCIONES_TIEMPO.forEach { (label, delayMs) ->
                        OutlinedButton(
                            onClick = {
                                ReminderHelper.programarRecordatorio(
                                    context = context,
                                    delayMs = delayMs,
                                    paciente = paciente
                                )
                                Toast.makeText(context, "Recordatorio en $label", Toast.LENGTH_SHORT).show()
                                showDialog = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBlue)
                        ) {
                            Text(label, color = DarkBlue, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Cancelar") }
            }
        )
    }
}

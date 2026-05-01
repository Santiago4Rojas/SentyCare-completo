package com.example.sentycare

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import com.example.sentycare.ui.theme.*

// ─── Modelos de datos temporales (reemplazar con Firestore en v2) ─────────────

data class EvaluationRecord(
    val escala: String,
    val puntaje: String,
    val interpretacion: String,
    val color: Color,
    val fecha: String,
    val hora: String
)

data class PatientHistory(
    val patient: Patient,
    val evaluaciones: List<EvaluationRecord>
)

// Datos de ejemplo mientras no hay Firebase
val sampleHistorial = listOf(
    PatientHistory(
        patient = Patient(
            nombre = "Arturo", apellido = "García", genero = "Masculino",
            noDoc = "1234567890", fechaNacimiento = "01/03/2020",
            rh = "O+", numeroCama = "12", diagnostico = "Neumonía severa"
        ),
        evaluaciones = listOf(
            EvaluationRecord("COMFORT-B", "15", "Sedación adecuada", Color(0xFF4CAF50), "01/04/2025", "08:30"),
            EvaluationRecord("RASS", "0", "Alerta y calmado", Color(0xFF4CAF50), "01/04/2025", "08:35"),
            EvaluationRecord("FLACC", "3", "Dolor leve", Color(0xFF8BC34A), "01/04/2025", "08:40"),
        )
    ),
    PatientHistory(
        patient = Patient(
            nombre = "María", apellido = "López", genero = "Femenino",
            noDoc = "0987654321", fechaNacimiento = "15/06/2019",
            rh = "A+", numeroCama = "5", diagnostico = "Sepsis"
        ),
        evaluaciones = listOf(
            EvaluationRecord("COMFORT-B", "22", "Sedación insuficiente", Color(0xFFFFC107), "01/04/2025", "09:10"),
            EvaluationRecord("RASS", "+2", "Agitado", Color(0xFFFF7043), "01/04/2025", "09:15"),
            EvaluationRecord("FACES", "6", "Dolor moderado", Color(0xFFFFC107), "01/04/2025", "09:20"),
        )
    )
)

// ─── Pantalla principal ───────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistorialScreen(
    onBackClick: () -> Unit = {}
) {
    val historial = sampleHistorial

    BackHandler { onBackClick() }  // ← AQUÍ

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Historial de Evaluaciones",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBlue)
            )
        }
    ) { paddingValues ->
        if (historial.isEmpty()) {
            EmptyHistorial(modifier = Modifier.padding(paddingValues))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(Color(0xFFF0F4F8))
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(historial) { entry ->
                    PatientHistoryCard(entry = entry)
                }
                item { Spacer(modifier = Modifier.height(8.dp)) }
            }
        }
    }
}

// ─── Tarjeta por paciente ─────────────────────────────────────────────────────

@Composable
fun PatientHistoryCard(entry: PatientHistory) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // Encabezado del paciente
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(DarkBlue.copy(alpha = 0.10f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Person,
                        contentDescription = null,
                        tint = DarkBlue,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${entry.patient.nombre} ${entry.patient.apellido}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkBlue
                    )
                    Text(
                        text = "Cama ${entry.patient.numeroCama} · ${entry.patient.diagnostico}",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                }
                // Badge con cantidad de evaluaciones
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(DarkBlue.copy(alpha = 0.08f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${entry.evaluaciones.size} eval.",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DarkBlue
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = Color(0xFFEEEEEE))
            Spacer(modifier = Modifier.height(12.dp))

            // Lista de evaluaciones del paciente
            entry.evaluaciones.forEach { eval ->
                EvaluationRow(eval = eval)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

// ─── Fila de una evaluación ───────────────────────────────────────────────────

@Composable
fun EvaluationRow(eval: EvaluationRecord) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFF8F9FC))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Círculo con color del resultado
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(eval.color),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = eval.puntaje,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = eval.escala,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = DarkBlue
            )
            Text(
                text = eval.interpretacion,
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
        // Fecha y hora
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.AccessTime,
                contentDescription = null,
                tint = LightGray,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(text = eval.fecha, fontSize = 11.sp, color = LightGray)
                Text(text = eval.hora, fontSize = 11.sp, color = LightGray)
            }
        }
    }
}

// ─── Estado vacío ─────────────────────────────────────────────────────────────

@Composable
fun EmptyHistorial(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("📋", fontSize = 52.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Sin evaluaciones registradas",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = DarkBlue
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Las evaluaciones aparecerán aquí\nuna vez realizadas",
                fontSize = 14.sp,
                color = LightGray
            )
        }
    }
}
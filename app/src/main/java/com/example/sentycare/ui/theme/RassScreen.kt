package com.example.sentycare

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import com.example.sentycare.ui.theme.*
import com.google.firebase.firestore.FirebaseFirestore

data class RassOption(val score: Int, val term: String, val description: String)

val RASS_OPTIONS = listOf(
    RassOption(+4, "Combativo",         "Peligro inmediato"),
    RassOption(+3, "Muy agitado",       "Retira tubos"),
    RassOption(+2, "Agitado",           "Movimientos frecuentes"),
    RassOption(+1, "Intranquilo",       "Ansioso"),
    RassOption( 0, "Alerta y calmado",  "Estado ideal"),
    RassOption(-1, "Somnoliento",       "Despierta >10 seg"),
    RassOption(-2, "Sedación ligera",   "Despierta <10 seg"),
    RassOption(-3, "Sedación moderada", "Sin contacto visual"),
    RassOption(-4, "Sedación profunda", "Solo responde físico"),
    RassOption(-5, "No despierta",      "Sin respuesta")
)

data class RassResult(val label: String, val color: Color)

fun interpretRass(score: Int): RassResult = when {
    score >= 4  -> RassResult("AGITACIÓN EXTREMA",  Color(0xFFB71C1C))
    score == 3  -> RassResult("MUY AGITADO",        Color(0xFFF44336))
    score == 2  -> RassResult("AGITADO",            Color(0xFFFF7043))
    score == 1  -> RassResult("INTRANQUILO",        Color(0xFFFF9800))
    score == 0  -> RassResult("ALERTA Y CALMADO",   Color(0xFF4CAF50))
    score == -1 -> RassResult("SOMNOLIENTO",        Color(0xFF8BC34A))
    score == -2 -> RassResult("SEDACIÓN LIGERA",    Color(0xFF2196F3))
    score == -3 -> RassResult("SEDACIÓN MODERADA",  Color(0xFF1976D2))
    score == -4 -> RassResult("SEDACIÓN PROFUNDA",  Color(0xFF7B1FA2))
    else        -> RassResult("NO DESPIERTA",       Color(0xFF4A148C))
}

fun rassColor(score: Int): Color = when {
    score >= 3  -> Color(0xFFF44336)
    score == 2  -> Color(0xFFFF7043)
    score == 1  -> Color(0xFFFF9800)
    score == 0  -> Color(0xFF4CAF50)
    score == -1 -> Color(0xFF8BC34A)
    score == -2 -> Color(0xFF2196F3)
    score == -3 -> Color(0xFF1976D2)
    score == -4 -> Color(0xFF7B1FA2)
    else        -> Color(0xFF4A148C)
}

private fun rassRecomendaciones(score: Int): List<String> = when {
    score >= 3  -> listOf("Valoración médica inmediata", "Considerar sedación adicional urgente", "Asegurar vías y dispositivos del paciente")
    score == 2  -> listOf("Aumentar sedación según protocolo", "Evaluar causa de agitación (dolor, delirio)", "Monitorear cada 30 minutos")
    score == 1  -> listOf("Monitorear estrechamente", "Evaluar nivel de dolor (EVA / FLACC)", "Optimizar entorno: reducir ruido, luz adecuada", "Presencia del cuidador si es posible")
    score == 0  -> listOf("Mantener manejo actual", "Continuar monitoreo por turnos", "Registrar evolución en historial")
    score == -1 -> listOf("Verificar respuesta a llamado verbal", "Considerar reducción gradual de sedación", "Evaluar despertar diario")
    score == -2 -> listOf("Evaluar necesidad de sedación actual", "Intentar contacto visual periódico", "Vigilar función respiratoria")
    else        -> listOf("Evaluar sedación profunda vs. necesaria", "Monitorear función neurológica", "Verificar reflejos protectores de vía aérea")
}

private val RASS_RANGOS = listOf(
    "+4" to Color(0xFFB71C1C), "+3" to Color(0xFFF44336), "+2" to Color(0xFFFF7043),
    "+1" to Color(0xFFFF9800), " 0" to Color(0xFF4CAF50), "-1" to Color(0xFF8BC34A),
    "-2" to Color(0xFF2196F3), "-3" to Color(0xFF1976D2), "-4" to Color(0xFF7B1FA2),
    "-5" to Color(0xFF4A148C)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RassScreen(
    patient: Patient,
    onBackClick: () -> Unit = {},
    onNewEvaluation: () -> Unit = {},
    onHomeClick: () -> Unit = {}
) {
    var selectedScore by remember { mutableStateOf<Int?>(null) }
    var showResult by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()

    BackHandler {                          // ← AQUÍ
        if (showResult) showResult = false else onBackClick()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (showResult) "Resultado RASS" else "Escala RASS", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { if (showResult) showResult = false else onBackClick() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBlue)
            )
        }
    ) { padding ->
        if (!showResult) {
            RassSelectionStep(
                selectedScore = selectedScore,
                onSelect = { selectedScore = it },
                onEvaluate = { if (selectedScore != null) showResult = true },
                modifier = Modifier.padding(padding)
            )
        } else {
            selectedScore?.let { score ->
                LaunchedEffect(Unit) {
                    val result = interpretRass(score)
                    val data = hashMapOf(
                        "pacienteNombre"    to "${patient.nombre} ${patient.apellido}",
                        "pacienteDocumento" to patient.noDoc,
                        "escala"            to "RASS",
                        "puntaje"           to score,
                        "clasificacion"     to result.label,
                        "fecha"             to System.currentTimeMillis()
                    )
                    db.collection("evaluaciones").add(data)
                    Toast.makeText(context, "Evaluación guardada", Toast.LENGTH_SHORT).show()
                }
                RassResultStep(
                    score = score,
                    onNewEvaluation = { selectedScore = null; showResult = false; onNewEvaluation() },
                    onHome = onHomeClick,
                    modifier = Modifier.padding(padding)
                )
            }
        }
    }
}

@Composable
fun RassSelectionStep(
    selectedScore: Int?,
    onSelect: (Int) -> Unit,
    onEvaluate: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize().background(Color.White)) {
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 24.dp)) {
            Text("Seleccione estado clínico", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = DarkBlue)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Observe al paciente antes de seleccionar", fontSize = 13.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color(0xFFF5F5F5)).padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFFF44336)))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Agitación", fontSize = 11.sp, color = Color.Gray)
                }
                Text("→", fontSize = 11.sp, color = Color.LightGray)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFF4CAF50)))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Ideal", fontSize = 11.sp, color = Color.Gray)
                }
                Text("→", fontSize = 11.sp, color = Color.LightGray)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFF4A148C)))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Sedación prof.", fontSize = 11.sp, color = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            RASS_OPTIONS.forEach { option ->
                val isSelected = selectedScore == option.score
                val color = rassColor(option.score)
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp).clickable { onSelect(option.score) },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) color else Color(0xFFE0E0E0)),
                    colors = CardDefaults.cardColors(containerColor = if (isSelected) color.copy(alpha = 0.08f) else Color.White),
                    elevation = CardDefaults.cardElevation(if (isSelected) 0.dp else 2.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(38.dp).clip(CircleShape).background(color), contentAlignment = Alignment.Center) {
                            Text(if (option.score > 0) "+${option.score}" else option.score.toString(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(option.term, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold, color = if (isSelected) color else Color.DarkGray, fontSize = 15.sp)
                            Text(option.description, fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
        Box(modifier = Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 20.dp, vertical = 16.dp)) {
            Button(
                onClick = onEvaluate, enabled = selectedScore != null,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DarkBlue, disabledContainerColor = LightGray)
            ) {
                Text("Ver resultado →", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun RassResultStep(score: Int, onNewEvaluation: () -> Unit, onHome: () -> Unit, modifier: Modifier = Modifier) {
    val result = interpretRass(score)
    Column(
        modifier = modifier.fillMaxSize().background(Color(0xFFF0F4F8)).verticalScroll(rememberScrollState()).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Box(modifier = Modifier.size(120.dp).clip(CircleShape).background(result.color), contentAlignment = Alignment.Center) {
            Text(if (score > 0) "+$score" else score.toString(), fontSize = 40.sp, color = Color.White, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(12.dp))
        RASS_OPTIONS.firstOrNull { it.score == score }?.let {
            Text(it.term, fontSize = 14.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(8.dp))
        }
        Box(modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(result.color).padding(horizontal = 20.dp, vertical = 8.dp)) {
            Text(result.label, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
        Spacer(modifier = Modifier.height(24.dp))
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(3.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Recomendación", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = DarkBlue)
                Spacer(modifier = Modifier.height(12.dp))
                rassRecomendaciones(score).forEach { rec ->
                    Row(modifier = Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
                        Box(modifier = Modifier.padding(top = 6.dp).size(7.dp).clip(CircleShape).background(result.color))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(rec, fontSize = 14.sp, color = Color.DarkGray)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text("Referencia de rangos:", fontSize = 13.sp, color = Color.Gray, modifier = Modifier.align(Alignment.Start))
        Spacer(modifier = Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            RASS_RANGOS.forEach { (label, color) ->
                val scoreVal = label.trim().toIntOrNull() ?: 0
                val isActive = scoreVal == score
                Box(
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(6.dp))
                        .background(if (isActive) color else color.copy(alpha = 0.30f))
                        .then(if (isActive) Modifier.border(2.dp, Color.White, RoundedCornerShape(6.dp)) else Modifier)
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(label.trim(), fontSize = 11.sp, fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal, color = Color.White, textAlign = TextAlign.Center)
                }
            }
        }
        Spacer(modifier = Modifier.height(28.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onNewEvaluation, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, DarkBlue)) {
                Text("Nueva evaluación", color = DarkBlue)
            }
            Button(onClick = onHome, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = DarkBlue)) {
                Text("Ir a Inicio", color = Color.White)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}
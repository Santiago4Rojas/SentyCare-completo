package com.example.sentycare

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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

data class ComfortBCategory(val title: String, val options: List<Pair<Int, String>>)

val COMFORT_B_CATEGORIES = listOf(
    ComfortBCategory("Nivel de conciencia", listOf(1 to "Profundamente dormido", 2 to "Ligeramente dormido", 3 to "Somnoliento", 4 to "Despierto y alerta", 5 to "Hiperalerta")),
    ComfortBCategory("Calma – Agitación", listOf(1 to "Calma", 2 to "Ligera ansiedad", 3 to "Ansiedad", 4 to "Mucha ansiedad", 5 to "Pánico")),
    ComfortBCategory("Respuesta respiratoria", listOf(1 to "No respiración espontánea", 2 to "Respiración espontánea sin ruidos", 3 to "Tos ocasional o resistencia", 4 to "Respira activamente contra respirador", 5 to "Lucha contra respirador")),
    ComfortBCategory("Movimientos físicos", listOf(1 to "Ausencia de movimientos", 2 to "Movimientos ocasionales", 3 to "Movimientos frecuentes", 4 to "Movimientos vigorosos", 5 to "Movimientos extremos")),
    ComfortBCategory("Tono muscular", listOf(1 to "Totalmente relajado", 2 to "Reducción tono", 3 to "Normal", 4 to "Aumentado", 5 to "Rigidez extrema")),
    ComfortBCategory("Tensión facial", listOf(1 to "Relajada", 2 to "Normal", 3 to "Tensión leve", 4 to "Tensión mantenida", 5 to "Muecas intensas"))
)

data class ComfortBResult(val label: String, val color: Color)

fun interpretComfortB(total: Int): ComfortBResult = when {
    total < 10  -> ComfortBResult("SOBRESEDACIÓN",         Color(0xFF9C27B0))
    total <= 12 -> ComfortBResult("SEDACIÓN PROFUNDA",     Color(0xFF2196F3))
    total <= 17 -> ComfortBResult("SEDACIÓN ADECUADA",     Color(0xFF4CAF50))
    total <= 22 -> ComfortBResult("SEDACIÓN INSUFICIENTE", Color(0xFFFFC107))
    else        -> ComfortBResult("MUY INSUFICIENTE",      Color(0xFFF44336))
}

private fun comfortRecomendaciones(total: Int): List<String> = when {
    total < 10  -> listOf("Reducir dosis de sedación progresivamente", "Evaluar necesidad de ventilación mecánica", "Monitorear función neurológica")
    total <= 12 -> listOf("Considerar reducción gradual de sedación", "Evaluar posibilidad de despertar diario", "Vigilar signos de depresión respiratoria")
    total <= 17 -> listOf("Mantener manejo actual de sedación", "Continuar monitoreo cada turno", "Registrar evolución clínica")
    total <= 22 -> listOf("Aumentar dosis de sedación", "Considerar agregar otro sedante", "Evaluar causas de agitación")
    else        -> listOf("Ajuste urgente de sedación", "Valoración médica inmediata", "Revisar vías y dispositivos")
}

private val COMFORT_RANGOS = listOf(
    "<10"   to Color(0xFF9C27B0),
    "10-12" to Color(0xFF2196F3),
    "13-17" to Color(0xFF4CAF50),
    "18-22" to Color(0xFFFFC107),
    ">22"   to Color(0xFFF44336)
)
private val COMFORT_LABELS = listOf("Sobre.", "Profunda", "Adecuada", "Insuf.", "Muy Insuf.")

private fun comfortRangoActivo(index: Int, total: Int) = when (index) {
    0 -> total < 10
    1 -> total in 10..12
    2 -> total in 13..17
    3 -> total in 18..22
    else -> total > 22
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComfortBScreen(
    patient: Patient,
    onBackClick: () -> Unit = {},
    onNewEvaluation: () -> Unit = {},
    onHomeClick: () -> Unit = {}
) {
    var step by remember { mutableIntStateOf(0) }
    val scores = remember { mutableStateListOf<Int?>(null, null, null, null, null, null) }
    val totalScore = scores.filterNotNull().sum()
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()

    BackHandler {
        when {
            step == 0 -> onBackClick()
            step <= 6 -> step--
            else -> step = 0
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when { step < 6 -> "COMFORT B"; step == 6 -> "Resumen"; else -> "Resultado" },
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        when { step == 0 -> onBackClick(); step <= 6 -> step--; else -> step = 0 }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBlue)
            )
        }
    ) { padding ->
        AnimatedContent(
            targetState = step,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            modifier = Modifier.fillMaxSize().padding(padding),
            label = ""
        ) { current ->
            when {
                current < 6 -> CategoryStep(
                    stepIndex = current,
                    category = COMFORT_B_CATEGORIES[current],
                    selectedScore = scores[current],
                    onSelect = { scores[current] = it },
                    onNext = { if (current == 5) step = 6 else step++ }
                )
                current == 6 -> SummaryStep(total = totalScore, onVerResult = { step = 7 })
                else -> {
                    LaunchedEffect(Unit) {
                        val result = interpretComfortB(totalScore)
                        val data = hashMapOf(
                            "pacienteNombre"    to "${patient.nombre} ${patient.apellido}",
                            "pacienteDocumento" to patient.noDoc,
                            "escala"            to "Comfort B",
                            "puntaje"           to totalScore,
                            "clasificacion"     to result.label,
                            "fecha"             to System.currentTimeMillis()
                        )
                        db.collection("evaluaciones").add(data)
                        Toast.makeText(context, "Evaluación guardada", Toast.LENGTH_SHORT).show()
                    }
                    ResultStep(
                        total = totalScore,
                        onNewEvaluation = { scores.replaceAll { null }; step = 0; onNewEvaluation() },
                        onHome = onHomeClick
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryStep(
    stepIndex: Int,
    category: ComfortBCategory,
    selectedScore: Int?,
    onSelect: (Int) -> Unit,
    onNext: () -> Unit
) {
    fun scoreColor(score: Int): Color = when (score) {
        1 -> Color(0xFF4CAF50)
        2 -> Color(0xFF8BC34A)
        3 -> Color(0xFFFFC107)
        4 -> Color(0xFFFF7043)
        5 -> Color(0xFFF44336)
        else -> Color(0xFF9E9E9E)
    }

    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        LinearProgressIndicator(
            progress = { (stepIndex + 1f) / 6f },
            modifier = Modifier.fillMaxWidth().height(4.dp),
            color = DarkBlue,
            trackColor = Color(0xFFE0E0E0)
        )
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(DarkBlue), contentAlignment = Alignment.Center) {
                    Text((stepIndex + 1).toString(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(category.title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = DarkBlue)
                    Text("Seleccione el nivel observado", fontSize = 13.sp, color = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color(0xFFF5F5F5)).padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFF4CAF50)))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Normal", fontSize = 11.sp, color = Color.Gray)
                }
                Text("→", fontSize = 11.sp, color = Color.LightGray)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFFFFC107)))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Moderado", fontSize = 11.sp, color = Color.Gray)
                }
                Text("→", fontSize = 11.sp, color = Color.LightGray)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFFF44336)))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Crítico", fontSize = 11.sp, color = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            category.options.forEach { (score, text) ->
                val isSelected = selectedScore == score
                val color = scoreColor(score)
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp).clickable { onSelect(score) },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = if (isSelected) color.copy(alpha = 0.08f) else Color.White),
                    border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) color else Color(0xFFE0E0E0)),
                    elevation = CardDefaults.cardElevation(if (isSelected) 0.dp else 2.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(38.dp).clip(CircleShape).background(color), contentAlignment = Alignment.Center) {
                            Text(score.toString(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = text, fontSize = 14.sp,
                            color = if (isSelected) color else Color.DarkGray,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            modifier = Modifier.weight(1f), lineHeight = 19.sp
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
        Box(modifier = Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 20.dp, vertical = 16.dp)) {
            Button(
                onClick = onNext, enabled = selectedScore != null,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DarkBlue, disabledContainerColor = LightGray)
            ) {
                Text(if (stepIndex == 5) "Ver resumen →" else "Siguiente →", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun SummaryStep(total: Int, onVerResult: () -> Unit) {
    val result = interpretComfortB(total)
    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFFF0F4F8)).verticalScroll(rememberScrollState()).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Box(modifier = Modifier.size(120.dp).clip(CircleShape).background(result.color), contentAlignment = Alignment.Center) {
            Text(total.toString(), fontSize = 40.sp, color = Color.White, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(14.dp))
        Box(modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(result.color).padding(horizontal = 20.dp, vertical = 8.dp)) {
            Text(result.label, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
        Spacer(modifier = Modifier.height(24.dp))
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(3.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Recomendación", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = DarkBlue)
                Spacer(modifier = Modifier.height(12.dp))
                comfortRecomendaciones(total).forEach { rec ->
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
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            COMFORT_RANGOS.forEachIndexed { index, (rango, color) ->
                val isActive = comfortRangoActivo(index, total)
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                            .background(if (isActive) color else color.copy(alpha = 0.28f))
                            .then(if (isActive) Modifier.border(2.dp, Color.White, RoundedCornerShape(8.dp)) else Modifier)
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(rango, fontSize = 11.sp, fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal, color = Color.White, textAlign = TextAlign.Center)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(COMFORT_LABELS[index], fontSize = 9.sp, color = if (isActive) color else Color.Gray, fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal, textAlign = TextAlign.Center)
                }
            }
        }
        Spacer(modifier = Modifier.height(28.dp))
        Button(onClick = onVerResult, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = DarkBlue)) {
            Text("Ver resultado", color = Color.White)
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun ResultStep(total: Int, onNewEvaluation: () -> Unit, onHome: () -> Unit) {
    val result = interpretComfortB(total)
    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFFF0F4F8)).verticalScroll(rememberScrollState()).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Box(modifier = Modifier.size(120.dp).clip(CircleShape).background(result.color), contentAlignment = Alignment.Center) {
            Text(total.toString(), fontSize = 40.sp, color = Color.White, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(14.dp))
        Box(modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(result.color).padding(horizontal = 20.dp, vertical = 8.dp)) {
            Text(result.label, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
        Spacer(modifier = Modifier.height(24.dp))
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(3.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Recomendación", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = DarkBlue)
                Spacer(modifier = Modifier.height(12.dp))
                comfortRecomendaciones(total).forEach { rec ->
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
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            COMFORT_RANGOS.forEachIndexed { index, (rango, color) ->
                val isActive = comfortRangoActivo(index, total)
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                            .background(if (isActive) color else color.copy(alpha = 0.28f))
                            .then(if (isActive) Modifier.border(2.dp, Color.White, RoundedCornerShape(8.dp)) else Modifier)
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(rango, fontSize = 11.sp, fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal, color = Color.White, textAlign = TextAlign.Center)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(COMFORT_LABELS[index], fontSize = 9.sp, color = if (isActive) color else Color.Gray, fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal, textAlign = TextAlign.Center)
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
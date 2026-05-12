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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import kotlinx.coroutines.launch
import com.example.sentycare.SesionState
import com.example.sentycare.ai.ContextoEvaluacion
import com.example.sentycare.ai.RecomendacionAIService
import com.example.sentycare.permissions.Permisos
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
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var recomendacionMedico by remember { mutableStateOf("") }
    var iaRecomendacion by remember { mutableStateOf("") }
    var iaCargando by remember { mutableStateOf(false) }
    var guardado by remember { mutableStateOf(false) }
    var guardando by remember { mutableStateOf(false) }
    var showBackConfirm by remember { mutableStateOf(false) }

    if (showBackConfirm) {
        AlertDialog(
            onDismissRequest = { showBackConfirm = false },
            containerColor = Color.White,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            title = { Text("¿Salir sin guardar?", fontWeight = FontWeight.Bold, color = DarkBlue) },
            text = { Text("La evaluación no ha sido guardada. Si sale ahora, se perderán los datos.", fontSize = 14.sp) },
            confirmButton = {
                Button(
                    onClick = { showBackConfirm = false; onBackClick() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                ) { Text("Salir", color = Color.White) }
            },
            dismissButton = {
                OutlinedButton(onClick = { showBackConfirm = false }, shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)) { Text("Quedarme") }
            }
        )
    }

    fun guardarEvaluacion(onSuccess: () -> Unit) {
        if (guardado || guardando) return
        guardando = true
        val result = interpretComfortB(totalScore)
        val data = hashMapOf(
            "pacienteNombre"             to "${patient.nombre} ${patient.apellido}",
            "pacienteDocumento"          to patient.noDoc,
            "escala"                     to "Comfort B",
            "puntaje"                    to totalScore,
            "clasificacion"              to result.label,
            "fecha"                      to System.currentTimeMillis(),
            "evaluadorId"                to SesionState.usuario.uid,
            "evaluadorNombre"            to SesionState.usuario.nombreCompleto,
            "evaluadorEspecialidad"      to SesionState.usuario.especialidad,
            "recomendacionesAutomaticas" to comfortRecomendaciones(totalScore),
            "recomendacionMedico"        to recomendacionMedico,
            "recomendacionIA"            to "",
            "recomendacionIAGeneradaEn"  to 0L
        )
        db.collection("evaluaciones").add(data)
            .addOnSuccessListener { docRef ->
                guardado  = true
                guardando = false
                Toast.makeText(context, "Evaluación guardada", Toast.LENGTH_SHORT).show()
                scope.launch {
                    iaCargando = true
                    val iaTexto = RecomendacionAIService.generarRecomendacion(
                        ContextoEvaluacion(
                            escala          = "Comfort B",
                            puntaje         = totalScore,
                            clasificacion   = result.label,
                            nombrePaciente  = "${patient.nombre} ${patient.apellido}",
                            diagnostico     = patient.diagnostico,
                            fechaNacimiento = patient.fechaNacimiento
                        )
                    )
                    iaCargando = false
                    if (iaTexto.isNotBlank()) {
                        iaRecomendacion = iaTexto
                        docRef.update("recomendacionIA", iaTexto, "recomendacionIAGeneradaEn", System.currentTimeMillis())
                    }
                }
                onSuccess()
            }
            .addOnFailureListener { e ->
                guardando = false
                Toast.makeText(context, "Error al guardar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    BackHandler {
        when {
            step == 0 -> onBackClick()
            step <= 5 -> step--
            guardado -> onBackClick()
            else -> showBackConfirm = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (step < 6) "COMFORT B" else "Resultado",
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        when { step == 0 -> onBackClick(); step <= 5 -> step--; guardado -> onBackClick(); else -> showBackConfirm = true }
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
                else -> ResultStep(
                    total = totalScore,
                    pacienteNombre = "${patient.nombre} ${patient.apellido}",
                    recomendacionMedico = recomendacionMedico,
                    onRecomendacionMedicoChange = { recomendacionMedico = it },
                    iaRecomendacion = iaRecomendacion,
                    iaCargando = iaCargando,
                    guardado = guardado,
                    guardando = guardando,
                    onGuardar = { guardarEvaluacion {} },
                    onNewEvaluation = {
                        scores.replaceAll { null }
                        recomendacionMedico = ""
                        iaRecomendacion = ""
                        guardado = false
                        step = 0
                        onNewEvaluation()
                    },
                    onHome = onHomeClick
                )
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
                Text(if (stepIndex == 5) "Ver resultado →" else "Siguiente →", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
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
fun ResultStep(
    total: Int,
    pacienteNombre: String = "",
    recomendacionMedico: String = "",
    onRecomendacionMedicoChange: (String) -> Unit = {},
    iaRecomendacion: String = "",
    iaCargando: Boolean = false,
    guardado: Boolean = false,
    guardando: Boolean = false,
    onGuardar: () -> Unit = {},
    onNewEvaluation: () -> Unit,
    onHome: () -> Unit
) {
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

        // Recomendaciones automáticas basadas en protocolo
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(3.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Recomendación clínica", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = DarkBlue)
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

        // Campo abierto de recomendación médica (solo para roles médicos)
        if (Permisos.puedeAgregarRecomendacionMedico(SesionState.rol)) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(3.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Recomendación del médico", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = DarkBlue)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = recomendacionMedico,
                        onValueChange = onRecomendacionMedicoChange,
                        placeholder = { Text("Escriba su recomendación clínica adicional...", fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 6,
                        shape = RoundedCornerShape(8.dp),
                        enabled = !guardado,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DarkBlue,
                            unfocusedBorderColor = Color(0xFFCCCCCC)
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default)
                    )
                }
            }
        }

        // Recomendación IA
        Spacer(modifier = Modifier.height(16.dp))
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F0FF)), elevation = CardDefaults.cardElevation(3.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("✨", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Recomendación IA", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6200EE))
                }
                Spacer(modifier = Modifier.height(8.dp))
                when {
                    iaCargando -> {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = Color(0xFF6200EE))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Generando recomendación basada en protocolos PICU...", fontSize = 12.sp, color = Color.Gray)
                    }
                    iaRecomendacion.isNotBlank() -> {
                        Text(iaRecomendacion, fontSize = 14.sp, color = Color.DarkGray, lineHeight = 22.sp)
                    }
                    guardado -> {
                        Text("Sin respuesta de la IA en este momento.", fontSize = 13.sp, color = Color.Gray)
                    }
                    else -> {
                        Text("Se generará al guardar la evaluación.", fontSize = 13.sp, color = Color.Gray)
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

        // Botón de guardar (antes de ir a nueva evaluación o inicio)
        if (!guardado) {
            Button(
                onClick = onGuardar,
                enabled = !guardando,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DarkBlue)
            ) {
                if (guardando) {
                    CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text("Guardar evaluación", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        if (guardado) {
            ReminderButton(
                paciente = pacienteNombre,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

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
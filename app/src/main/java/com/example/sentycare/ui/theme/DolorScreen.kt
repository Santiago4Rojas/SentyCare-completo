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
import androidx.compose.ui.text.input.ImeAction
import kotlinx.coroutines.launch
import com.example.sentycare.SesionState
import com.example.sentycare.ai.ContextoEvaluacion
import com.example.sentycare.ai.RecomendacionAIService
import com.example.sentycare.permissions.Permisos
import com.example.sentycare.ui.theme.*
import com.google.firebase.firestore.FirebaseFirestore

enum class DolorEscala { FLACC, FACES }

data class FlaccCategory(val title: String, val letter: String, val options: List<Pair<Int, String>>)

val FLACC_CATEGORIES = listOf(
    FlaccCategory("Cara", "F", listOf(0 to "Sin expresión particular o sonrisa", 1 to "Mueca o fruncimiento ocasional, expresión desinteresada", 2 to "Mandíbula apretada, mentón tembloroso frecuente")),
    FlaccCategory("Piernas", "L", listOf(0 to "Posición normal o relajada", 1 to "Inquietas, agitadas o tensas", 2 to "Pateando o piernas encogidas")),
    FlaccCategory("Actividad", "A", listOf(0 to "Acostado tranquilamente, posición normal, se mueve con facilidad", 1 to "Se retuerce, se mece hacia adelante y hacia atrás, tenso", 2 to "Arqueado, rígido o sacudidas")),
    FlaccCategory("Llanto", "C", listOf(0 to "Sin llanto (despierto o dormido)", 1 to "Gemidos o quejidos, llanto ocasional", 2 to "Llanto constante, gritos o sollozos, queja frecuente")),
    FlaccCategory("Consolabilidad", "C", listOf(0 to "Contento, relajado", 1 to "Se tranquiliza con el toque ocasional, abrazos o palabras. Distraíble", 2 to "Difícil de consolar o reconfortar"))
)

data class FacesOption(val score: Int, val emoji: String, val description: String)

val FACES_OPTIONS = listOf(
    FacesOption(0,  "😊", "Sin dolor"),
    FacesOption(2,  "🙁", "Duele un poco"),
    FacesOption(4,  "😟", "Duele un poco más"),
    FacesOption(6,  "😣", "Duele bastante"),
    FacesOption(8,  "😢", "Duele mucho"),
    FacesOption(10, "😭", "El peor dolor posible")
)

data class DolorResult(val label: String, val color: Color, val recommendations: List<String>)

fun interpretFlacc(total: Int): DolorResult = when (total) {
    0       -> DolorResult("SIN DOLOR",      Color(0xFF4CAF50), listOf("Sin intervención analgésica requerida", "Continuar monitoreo de rutina", "Mantener medidas de confort"))
    in 1..3 -> DolorResult("DOLOR LEVE",     Color(0xFF8BC34A), listOf("Medidas no farmacológicas: succión no nutritiva, musicoterapia, presencia del cuidador", "Considerar Acetaminofén enteral si persiste", "Reevaluar en 30 minutos"))
    in 4..6 -> DolorResult("DOLOR MODERADO", Color(0xFFFFC107), listOf("Iniciar Acetaminofén IV/enteral y/o AINES", "Evaluar necesidad de opioide de rescate", "Aplicar medidas no farmacológicas complementarias", "Reevaluar en 30 minutos"))
    else    -> DolorResult("DOLOR SEVERO",   Color(0xFFF44336), listOf("Opioide IV como analgésico primario (Morfina / Fentanil)", "Morfina rescate: 0.05–0.1 mg/k IV c/4h", "Fentanil rescate: 1–2 mcg/k en 5 min", "Notificar al médico tratante de inmediato", "Reevaluar a los 15 minutos"))
}

fun interpretFaces(score: Int): DolorResult = when (score) {
    0  -> DolorResult("SIN DOLOR",          Color(0xFF4CAF50), listOf("Sin intervención analgésica requerida", "Continuar monitoreo de rutina"))
    2  -> DolorResult("DOLOR LEVE",         Color(0xFF8BC34A), listOf("Medidas no farmacológicas: distracción, presencia del cuidador", "Considerar Acetaminofén si persiste", "Reevaluar en 30 minutos"))
    4  -> DolorResult("DOLOR LEVE–MODERADO",Color(0xFFFFEB3B), listOf("Acetaminofén y/o AINES", "Aplicar medidas no farmacológicas", "Reevaluar en 30 minutos"))
    6  -> DolorResult("DOLOR MODERADO",     Color(0xFFFFC107), listOf("Acetaminofén IV + AINE", "Evaluar opioide de rescate", "Reevaluar en 30 minutos"))
    8  -> DolorResult("DOLOR INTENSO",      Color(0xFFFF7043), listOf("Opioide IV de rescate", "Morfina: 0.05–0.1 mg/k IV", "Notificar al médico tratante", "Reevaluar a los 15 minutos"))
    else-> DolorResult("DOLOR MÁXIMO",      Color(0xFFF44336), listOf("Intervención analgésica urgente", "Opioide IV inmediato", "Fentanil: 1–2 mcg/k en 5 min", "Notificar al médico tratante de inmediato", "Reevaluar a los 15 minutos"))
}

fun dolorColor(score: Int, maxScore: Int): Color {
    val ratio = score.toFloat() / maxScore
    return when {
        ratio == 0f   -> Color(0xFF4CAF50)
        ratio <= 0.3f -> Color(0xFF8BC34A)
        ratio <= 0.5f -> Color(0xFFFFC107)
        ratio <= 0.7f -> Color(0xFFFF9800)
        ratio <= 0.9f -> Color(0xFFFF7043)
        else          -> Color(0xFFF44336)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DolorScreen(
    patient: Patient,
    onBackClick: () -> Unit = {},
    onNewEvaluation: () -> Unit = {},
    onHomeClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var escala by remember { mutableStateOf<DolorEscala?>(null) }
    var step by remember { mutableIntStateOf(0) }
    val flaccScores = remember { mutableStateListOf<Int?>(null, null, null, null, null) }
    var facesScore by remember { mutableStateOf<Int?>(null) }
    var showResult by remember { mutableStateOf(false) }
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

    fun guardarEvaluacion(nombreEscala: String, puntaje: Int, clasificacion: String, recommendations: List<String>, onSuccess: () -> Unit) {
        if (guardado || guardando) return
        guardando = true
        val data = hashMapOf(
            "pacienteNombre"             to "${patient.nombre} ${patient.apellido}",
            "pacienteDocumento"          to patient.noDoc,
            "escala"                     to nombreEscala,
            "puntaje"                    to puntaje,
            "clasificacion"              to clasificacion,
            "fecha"                      to System.currentTimeMillis(),
            "evaluadorId"                to SesionState.usuario.uid,
            "evaluadorNombre"            to SesionState.usuario.nombreCompleto,
            "evaluadorEspecialidad"      to SesionState.usuario.especialidad,
            "recomendacionesAutomaticas" to recommendations,
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
                            escala          = nombreEscala,
                            puntaje         = puntaje,
                            clasificacion   = clasificacion,
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
            showResult && !guardado -> showBackConfirm = true
            showResult && guardado -> onBackClick()
            escala == DolorEscala.FLACC && step > 0 -> step--
            escala != null -> { escala = null; step = 0 }
            else -> onBackClick()
        }
    }

    val topBarTitle = when {
        escala == null -> "Evaluación de Dolor"
        showResult -> "Resultado"
        escala == DolorEscala.FLACC && step < 5 -> "FLACC"
        escala == DolorEscala.FLACC && step == 5 -> "Resumen FLACC"
        else -> "FACES"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(topBarTitle, color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        when {
                            showResult && !guardado -> showBackConfirm = true
                            showResult && guardado -> onBackClick()
                            escala == DolorEscala.FLACC && step > 0 -> step--
                            escala != null -> { escala = null; step = 0 }
                            else -> onBackClick()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBlue)
            )
        }
    ) { paddingValues ->
        AnimatedContent(
            targetState = Triple(escala, step, showResult),
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            label = "dolor_step"
        ) { (currentEscala, currentStep, isResult) ->
            when {
                currentEscala == null -> EscalaSelector(patient = patient, onSelectFlacc = { escala = DolorEscala.FLACC }, onSelectFaces = { escala = DolorEscala.FACES })
                currentEscala == DolorEscala.FLACC && !isResult && currentStep < 5 -> FlaccCategoryStep(
                    stepIndex = currentStep, category = FLACC_CATEGORIES[currentStep],
                    selectedScore = flaccScores[currentStep],
                    onSelect = { flaccScores[currentStep] = it },
                    onNext = { if (flaccScores[currentStep] != null) step = if (currentStep == 4) 5 else currentStep + 1 }
                )
                currentEscala == DolorEscala.FLACC && !isResult && currentStep == 5 -> FlaccSummaryStep(scores = flaccScores.toList(), onVerResult = { showResult = true })
                currentEscala == DolorEscala.FACES && !isResult -> FacesSelectionStep(selectedScore = facesScore, onSelect = { facesScore = it }, onEvaluate = { if (facesScore != null) showResult = true })
                currentEscala == DolorEscala.FLACC && isResult -> {
                    val total = flaccScores.filterNotNull().sum()
                    val flaccResult = interpretFlacc(total)
                    DolorResultStep(
                        scoreDisplay = total.toString(), maxScore = 10, numericScore = total,
                        result = flaccResult, escalaLabel = "FLACC (0–10)",
                        rangeLabels = listOf("0 Sin dolor", "1-3 Leve", "4-6 Moderado", "7-10 Severo"),
                        rangeColors = listOf(Color(0xFF4CAF50), Color(0xFF8BC34A), Color(0xFFFFC107), Color(0xFFF44336)),
                        activeRange = when (total) { 0 -> 0; in 1..3 -> 1; in 4..6 -> 2; else -> 3 },
                        pacienteNombre = "${patient.nombre} ${patient.apellido}",
                        recomendacionMedico = recomendacionMedico,
                        onRecomendacionMedicoChange = { recomendacionMedico = it },
                        iaRecomendacion = iaRecomendacion,
                        iaCargando = iaCargando,
                        guardado = guardado,
                        guardando = guardando,
                        onGuardar = { guardarEvaluacion("Dolor FLACC", total, flaccResult.label, flaccResult.recommendations) {} },
                        onNewEvaluation = {
                            flaccScores.replaceAll { null }; step = 0; showResult = false; escala = null
                            recomendacionMedico = ""; iaRecomendacion = ""; guardado = false
                            onNewEvaluation()
                        },
                        onHome = onHomeClick
                    )
                }
                currentEscala == DolorEscala.FACES && isResult -> {
                    val s = facesScore ?: 0
                    val facesResult = interpretFaces(s)
                    DolorResultStep(
                        scoreDisplay = FACES_OPTIONS.first { it.score == s }.emoji, maxScore = 10, numericScore = s,
                        result = facesResult, escalaLabel = "FACES (0–10)",
                        rangeLabels = listOf("0", "2", "4", "6", "8", "10"),
                        rangeColors = listOf(Color(0xFF4CAF50), Color(0xFF8BC34A), Color(0xFFFFEB3B), Color(0xFFFFC107), Color(0xFFFF7043), Color(0xFFF44336)),
                        activeRange = FACES_OPTIONS.indexOfFirst { it.score == s },
                        pacienteNombre = "${patient.nombre} ${patient.apellido}",
                        recomendacionMedico = recomendacionMedico,
                        onRecomendacionMedicoChange = { recomendacionMedico = it },
                        iaRecomendacion = iaRecomendacion,
                        iaCargando = iaCargando,
                        guardado = guardado,
                        guardando = guardando,
                        onGuardar = { guardarEvaluacion("Dolor FACES", s, facesResult.label, facesResult.recommendations) {} },
                        onNewEvaluation = {
                            facesScore = null; showResult = false; escala = null
                            recomendacionMedico = ""; iaRecomendacion = ""; guardado = false
                            onNewEvaluation()
                        },
                        onHome = onHomeClick
                    )
                }
                else -> {}
            }
        }
    }
}

@Composable
fun EscalaSelector(patient: Patient, onSelectFlacc: () -> Unit, onSelectFaces: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF0F4F8)).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 24.dp)) {
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFEEF2FB)), elevation = CardDefaults.cardElevation(0.dp)) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("👤", fontSize = 20.sp)
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("${patient.nombre} ${patient.apellido}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = DarkBlue)
                    Text("Cama ${patient.numeroCama} • ${patient.fechaNacimiento}", fontSize = 13.sp, color = Color.Gray)
                }
            }
        }
        Spacer(modifier = Modifier.height(28.dp))
        Text("Seleccione la escala de dolor", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = DarkBlue)
        Spacer(modifier = Modifier.height(6.dp))
        Text("Elija según la edad y capacidad comunicativa del paciente", fontSize = 14.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(24.dp))
        EscalaCard(emoji = "🔍", title = "FLACC", subtitle = "No comunicativos / ≤ 10 años", description = "Evaluación por observación: Cara, Piernas, Actividad, Llanto, Consolabilidad", color = Color(0xFFFF7043), onClick = onSelectFlacc)
        Spacer(modifier = Modifier.height(16.dp))
        EscalaCard(emoji = "😊", title = "FACES (Wong-Baker)", subtitle = "Comunicativos / ≥ 3 años", description = "El paciente señala la cara que representa su nivel de dolor", color = Color(0xFF2196F3), onClick = onSelectFaces)
        Spacer(modifier = Modifier.height(28.dp))
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Referencia rápida", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = DarkBlue)
                Spacer(modifier = Modifier.height(8.dp))
                ReferenceRow("FLACC", "Recién nacidos – 10 años", "No verbal / Ventilado")
                ReferenceRow("FACES", "≥ 3 años", "Comunicativo")
                ReferenceRow("COMFORT B", "≤ 10 años", "No comunicativo / UCI")
            }
        }
    }
}

@Composable
fun EscalaCard(emoji: String, title: String, subtitle: String, description: String, color: Color, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(3.dp)) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(56.dp).clip(CircleShape).background(color.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) { Text(emoji, fontSize = 26.sp) }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = DarkBlue)
                Text(subtitle, fontSize = 12.sp, color = color, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(description, fontSize = 13.sp, color = Color.Gray, lineHeight = 18.sp)
            }
            Text("›", fontSize = 24.sp, color = LightGray)
        }
    }
}

@Composable
fun ReferenceRow(escala: String, edad: String, tipo: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(escala, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = DarkBlue, modifier = Modifier.width(72.dp))
        Text("$edad · $tipo", fontSize = 12.sp, color = Color.Gray)
    }
}

@Composable
fun FlaccCategoryStep(stepIndex: Int, category: FlaccCategory, selectedScore: Int?, onSelect: (Int) -> Unit, onNext: () -> Unit) {
    fun scoreColor(score: Int): Color = when (score) {
        0 -> Color(0xFF4CAF50)
        1 -> Color(0xFFFFC107)
        2 -> Color(0xFFF44336)
        else -> Color(0xFF9E9E9E)
    }

    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        LinearProgressIndicator(progress = { (stepIndex + 1f) / 5f }, modifier = Modifier.fillMaxWidth().height(4.dp), color = Color(0xFFFF7043), trackColor = Color(0xFFE0E0E0))
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFFFF7043)), contentAlignment = Alignment.Center) {
                    Text(category.letter, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("${stepIndex + 1}. ${category.title}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = DarkBlue)
                    Text("Seleccione el nivel observado", fontSize = 13.sp, color = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color(0xFFF5F5F5)).padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFF4CAF50)))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Sin dolor", fontSize = 11.sp, color = Color.Gray)
                }
                Text("→", fontSize = 11.sp, color = Color.LightGray)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFFFFC107)))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Leve", fontSize = 11.sp, color = Color.Gray)
                }
                Text("→", fontSize = 11.sp, color = Color.LightGray)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFFF44336)))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Significativo", fontSize = 11.sp, color = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            category.options.forEach { (score, description) ->
                val isSelected = selectedScore == score
                val color = scoreColor(score)
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp).clickable { onSelect(score) },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = if (isSelected) color.copy(alpha = 0.08f) else Color.White),
                    border = if (isSelected) BorderStroke(2.dp, color) else BorderStroke(1.dp, Color(0xFFE0E0E0)),
                    elevation = CardDefaults.cardElevation(if (isSelected) 0.dp else 2.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(color), contentAlignment = Alignment.Center) {
                            Text(score.toString(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(text = description, fontSize = 14.sp, color = if (isSelected) color else Color.DarkGray, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal, modifier = Modifier.weight(1f), lineHeight = 19.sp)
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
        Box(modifier = Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 20.dp, vertical = 16.dp)) {
            Button(onClick = onNext, enabled = selectedScore != null, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = DarkBlue, disabledContainerColor = LightGray)) {
                Text(if (stepIndex == 4) "Ver resumen →" else "Siguiente →", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun FlaccSummaryStep(scores: List<Int?>, onVerResult: () -> Unit) {
    val total = scores.filterNotNull().sum()
    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 24.dp)) {
            Text("Resumen FLACC", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = DarkBlue)
            Spacer(modifier = Modifier.height(20.dp))
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(3.dp)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    FLACC_CATEGORIES.forEachIndexed { i, cat ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(28.dp).clip(CircleShape).background(Color(0xFFFF7043)), contentAlignment = Alignment.Center) {
                                    Text(cat.letter, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(cat.title, fontSize = 15.sp, color = Color.DarkGray)
                            }
                            Text(scores[i]?.toString() ?: "-", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = DarkBlue)
                        }
                        if (i < FLACC_CATEGORIES.size - 1) HorizontalDivider(color = Color(0xFFF0F0F0))
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFDDDDDD), thickness = 1.5.dp)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("PUNTAJE TOTAL", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = DarkBlue)
                        Text(total.toString(), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = DarkBlue)
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
        Box(modifier = Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 20.dp, vertical = 16.dp)) {
            Button(onClick = onVerResult, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = MediumBlue)) {
                Text("Ver resultado →", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun FacesSelectionStep(selectedScore: Int?, onSelect: (Int) -> Unit, onEvaluate: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 24.dp)) {
            Text("Escala de Caras (Wong-Baker)", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = DarkBlue)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Pida al paciente que señale la cara que mejor describe su dolor", fontSize = 14.sp, color = Color.Gray, lineHeight = 20.sp)
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color(0xFFF5F5F5)).padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFF4CAF50)))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Sin dolor", fontSize = 11.sp, color = Color.Gray)
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
                    Text("Máximo", fontSize = 11.sp, color = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            FACES_OPTIONS.forEach { option ->
                val isSelected = selectedScore == option.score
                val chipColor = dolorColor(option.score, 10)
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp).clickable { onSelect(option.score) },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = if (isSelected) chipColor.copy(alpha = 0.08f) else Color.White),
                    border = if (isSelected) BorderStroke(2.dp, chipColor) else BorderStroke(1.dp, Color(0xFFE0E0E0)),
                    elevation = CardDefaults.cardElevation(if (isSelected) 0.dp else 2.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(option.emoji, fontSize = 36.sp)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(text = option.description, fontSize = 16.sp, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal, color = if (isSelected) chipColor else Color.DarkGray, modifier = Modifier.weight(1f))
                        Box(modifier = Modifier.size(34.dp).clip(CircleShape).background(chipColor), contentAlignment = Alignment.Center) {
                            Text(option.score.toString(), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
        Box(modifier = Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 20.dp, vertical = 16.dp)) {
            Button(onClick = onEvaluate, enabled = selectedScore != null, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = DarkBlue, disabledContainerColor = LightGray)) {
                Text("Ver resultado →", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun DolorResultStep(
    scoreDisplay: String, maxScore: Int, numericScore: Int,
    result: DolorResult, escalaLabel: String,
    rangeLabels: List<String>, rangeColors: List<Color>, activeRange: Int,
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
    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFFF0F4F8)).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.size(120.dp).clip(CircleShape).background(result.color), contentAlignment = Alignment.Center) {
            Text(scoreDisplay, fontSize = 40.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(escalaLabel, fontSize = 13.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(8.dp))
        Box(modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(result.color).padding(horizontal = 20.dp, vertical = 8.dp)) {
            Text(result.label, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center)
        }
        Spacer(modifier = Modifier.height(24.dp))

        // Recomendaciones automáticas
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Recomendación clínica", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = DarkBlue)
                Spacer(modifier = Modifier.height(12.dp))
                result.recommendations.forEach { rec ->
                    Row(modifier = Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
                        Box(modifier = Modifier.padding(top = 6.dp).size(7.dp).clip(CircleShape).background(result.color))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(rec, fontSize = 14.sp, color = Color.DarkGray, lineHeight = 20.sp)
                    }
                }
            }
        }

        // Campo abierto de recomendación médica
        if (Permisos.puedeAgregarRecomendacionMedico(SesionState.rol)) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
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
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F0FF)), elevation = CardDefaults.cardElevation(2.dp)) {
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
                    iaRecomendacion.isNotBlank() -> Text(iaRecomendacion, fontSize = 14.sp, color = Color.DarkGray, lineHeight = 22.sp)
                    guardado -> Text("Sin respuesta de la IA en este momento.", fontSize = 13.sp, color = Color.Gray)
                    else -> Text("Se generará al guardar la evaluación.", fontSize = 13.sp, color = Color.Gray)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        Text("Referencia de rangos:", fontSize = 13.sp, color = Color.Gray, modifier = Modifier.align(Alignment.Start))
        Spacer(modifier = Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            rangeLabels.forEachIndexed { i, label ->
                val isActive = i == activeRange
                Box(
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                        .background(rangeColors[i].copy(alpha = if (isActive) 1f else 0.35f))
                        .then(if (isActive) Modifier.border(2.dp, Color.White, RoundedCornerShape(8.dp)) else Modifier)
                        .padding(vertical = 8.dp, horizontal = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center)
                }
            }
        }
        Spacer(modifier = Modifier.height(32.dp))

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

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onNewEvaluation, modifier = Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(10.dp), border = BorderStroke(1.5.dp, DarkBlue), colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White, contentColor = DarkBlue)) {
                Text("Nueva evaluación", color = DarkBlue, fontWeight = FontWeight.Medium, fontSize = 14.sp)
            }
            Button(onClick = onHome, modifier = Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = MediumBlue)) {
                Text("Ir a Inicio", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
        }
    }
}
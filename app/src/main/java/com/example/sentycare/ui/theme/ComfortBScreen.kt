package com.example.sentycare

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sentycare.ui.theme.*
import com.google.firebase.firestore.FirebaseFirestore

data class ComfortBCategory(
    val title: String,
    val options: List<Pair<Int, String>>
)

val COMFORT_B_CATEGORIES = listOf(
    ComfortBCategory(
        "Nivel de conciencia",
        listOf(
            1 to "Profundamente dormido",
            2 to "Ligeramente dormido",
            3 to "Somnoliento",
            4 to "Despierto y alerta",
            5 to "Hiperalerta"
        )
    ),
    ComfortBCategory(
        "Calma – Agitación",
        listOf(
            1 to "Calma",
            2 to "Ligera ansiedad",
            3 to "Ansiedad",
            4 to "Mucha ansiedad",
            5 to "Pánico"
        )
    ),
    ComfortBCategory(
        "Respuesta respiratoria",
        listOf(
            1 to "No respiración espontánea",
            2 to "Respiración espontánea sin ruidos",
            3 to "Tos ocasional o resistencia",
            4 to "Respira activamente contra respirador",
            5 to "Lucha contra respirador"
        )
    ),
    ComfortBCategory(
        "Movimientos físicos",
        listOf(
            1 to "Ausencia de movimientos",
            2 to "Movimientos ocasionales",
            3 to "Movimientos frecuentes",
            4 to "Movimientos vigorosos",
            5 to "Movimientos extremos"
        )
    ),
    ComfortBCategory(
        "Tono muscular",
        listOf(
            1 to "Totalmente relajado",
            2 to "Reducción tono",
            3 to "Normal",
            4 to "Aumentado",
            5 to "Rigidez extrema"
        )
    ),
    ComfortBCategory(
        "Tensión facial",
        listOf(
            1 to "Relajada",
            2 to "Normal",
            3 to "Tensión leve",
            4 to "Tensión mantenida",
            5 to "Muecas intensas"
        )
    )
)

data class ComfortBResult(
    val label: String,
    val color: Color
)

fun interpretComfortB(total: Int): ComfortBResult = when {
    total < 10 ->
        ComfortBResult(
            "SOBRESEDACIÓN",
            Color(0xFF9C27B0)
        )

    total <= 12 ->
        ComfortBResult(
            "SEDACIÓN PROFUNDA",
            Color(0xFF2196F3)
        )

    total <= 17 ->
        ComfortBResult(
            "SEDACIÓN ADECUADA",
            Color(0xFF4CAF50)
        )

    total <= 22 ->
        ComfortBResult(
            "SEDACIÓN INSUFICIENTE",
            Color(0xFFFFC107)
        )

    else ->
        ComfortBResult(
            "MUY INSUFICIENTE",
            Color(0xFFF44336)
        )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComfortBScreen(
    patient: Patient,
    onBackClick: () -> Unit = {},
    onNewEvaluation: () -> Unit = {},
    onHomeClick: () -> Unit = {}
) {

    var step by remember {
        mutableIntStateOf(0)
    }

    val scores =
        remember {
            mutableStateListOf<Int?>(
                null, null, null,
                null, null, null
            )
        }

    val totalScore =
        scores.filterNotNull().sum()

    val context =
        LocalContext.current

    val db =
        FirebaseFirestore.getInstance()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text =
                            when {
                                step < 6 ->
                                    "COMFORT B"

                                step == 6 ->
                                    "Resumen"

                                else ->
                                    "Resultado"
                            },

                        color =
                            Color.White
                    )
                },

                navigationIcon = {
                    IconButton(
                        onClick = {
                            when {
                                step == 0 ->
                                    onBackClick()

                                step <= 6 ->
                                    step--

                                else ->
                                    step = 0
                            }
                        }
                    ) {
                        Icon(
                            imageVector =
                                Icons.AutoMirrored
                                    .Filled
                                    .ArrowBack,

                            contentDescription =
                                null,

                            tint =
                                Color.White
                        )
                    }
                },

                colors =
                    TopAppBarDefaults
                        .topAppBarColors(
                            containerColor =
                                DarkBlue
                        )
            )
        }
    ) { padding ->

        AnimatedContent(
            targetState = step,
            transitionSpec = {
                fadeIn() togetherWith
                        fadeOut()
            },

            modifier = Modifier
                .fillMaxSize()
                .padding(padding),

            label = ""
        ) { current ->

            when {

                current < 6 -> {

                    CategoryStep(
                        stepIndex = current,
                        category =
                            COMFORT_B_CATEGORIES[current],

                        selectedScore =
                            scores[current],

                        onSelect = {
                            scores[current] = it
                        },

                        onNext = {
                            if (current == 5)
                                step = 6
                            else
                                step++
                        }
                    )
                }

                current == 6 -> {

                    SummaryStep(
                        total = totalScore,
                        onVerResult = {
                            step = 7
                        }
                    )
                }

                else -> {

                    LaunchedEffect(Unit) {

                        val result =
                            interpretComfortB(
                                totalScore
                            )

                        val data =
                            hashMapOf(

                                "pacienteNombre" to
                                        "${patient.nombre} ${patient.apellido}",

                                "pacienteDocumento" to
                                        patient.noDoc,

                                "escala" to
                                        "Comfort B",

                                "puntaje" to
                                        totalScore,

                                "clasificacion" to
                                        result.label,

                                "fecha" to
                                        System.currentTimeMillis()
                            )

                        db.collection(
                            "evaluaciones"
                        ).add(data)

                        Toast.makeText(
                            context,
                            "Evaluación guardada",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    ResultStep(
                        total = totalScore,
                        onNewEvaluation = {

                            scores.replaceAll {
                                null
                            }

                            step = 0
                            onNewEvaluation()
                        },

                        onHome =
                            onHomeClick
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(20.dp)
    ) {

        Text(
            text =
                "${stepIndex + 1}. ${category.title}",

            fontSize = 22.sp,
            fontWeight =
                FontWeight.Bold,

            color = DarkBlue
        )

        Spacer(
            modifier =
                Modifier.height(20.dp)
        )

        category.options.forEach {
                (score, text) ->

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clickable {
                        onSelect(score)
                    }
            ) {

                Row(
                    modifier =
                        Modifier.padding(16.dp),

                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Box(
                        modifier =
                            Modifier
                                .size(34.dp)
                                .clip(
                                    CircleShape
                                )
                                .background(
                                    MediumBlue
                                ),

                        contentAlignment =
                            Alignment.Center
                    ) {

                        Text(
                            score.toString(),
                            color =
                                Color.White
                        )
                    }

                    Spacer(
                        modifier =
                            Modifier.width(14.dp)
                    )

                    Text(
                        text = text,
                        modifier =
                            Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(
            modifier =
                Modifier.weight(1f)
        )

        Button(
            onClick = onNext,
            enabled =
                selectedScore != null,

            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),

            colors =
                ButtonDefaults
                    .buttonColors(
                        containerColor =
                            DarkBlue
                    )
        ) {
            Text(
                "Siguiente",
                color = Color.White
            )
        }
    }
}

@Composable
fun SummaryStep(
    total: Int,
    onVerResult: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(20.dp)
    ) {

        Text(
            "Resumen",
            fontSize = 22.sp,
            fontWeight =
                FontWeight.Bold,
            color = DarkBlue
        )

        Spacer(
            modifier =
                Modifier.height(20.dp)
        )

        Text(
            "Puntaje total: $total",
            fontSize = 18.sp
        )

        Spacer(
            modifier =
                Modifier.weight(1f)
        )

        Button(
            onClick = onVerResult,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),

            colors =
                ButtonDefaults
                    .buttonColors(
                        containerColor =
                            DarkBlue
                    )
        ) {
            Text(
                "Ver resultado",
                color = Color.White
            )
        }
    }
}

@Composable
fun ResultStep(
    total: Int,
    onNewEvaluation: () -> Unit,
    onHome: () -> Unit
) {

    val result =
        interpretComfortB(total)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(20.dp),

        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {

        Spacer(
            modifier =
                Modifier.height(20.dp)
        )

        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(
                    result.color
                ),

            contentAlignment =
                Alignment.Center
        ) {

            Text(
                total.toString(),
                fontSize = 40.sp,
                color = Color.White,
                fontWeight =
                    FontWeight.Bold
            )
        }

        Spacer(
            modifier =
                Modifier.height(20.dp)
        )

        Text(
            result.label,
            fontSize = 22.sp,
            fontWeight =
                FontWeight.Bold,
            color =
                result.color,
            textAlign =
                TextAlign.Center
        )

        Spacer(
            modifier =
                Modifier.weight(1f)
        )

        Row(
            modifier =
                Modifier.fillMaxWidth(),

            horizontalArrangement =
                Arrangement.spacedBy(10.dp)
        ) {

            OutlinedButton(
                onClick =
                    onNewEvaluation,

                modifier =
                    Modifier.weight(1f)
            ) {
                Text(
                    "Nueva"
                )
            }

            Button(
                onClick = onHome,
                modifier =
                    Modifier.weight(1f),

                colors =
                    ButtonDefaults
                        .buttonColors(
                            containerColor =
                                DarkBlue
                        )
            ) {
                Text(
                    "Inicio",
                    color =
                        Color.White
                )
            }
        }
    }
}
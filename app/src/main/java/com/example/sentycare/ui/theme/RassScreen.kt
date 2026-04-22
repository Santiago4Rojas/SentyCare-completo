package com.example.sentycare

import android.widget.Toast
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
import com.example.sentycare.ui.theme.*
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.foundation.BorderStroke

data class RassOption(
    val score: Int,
    val term: String,
    val description: String
)

val RASS_OPTIONS = listOf(
    RassOption(+4,"Combativo","Peligro inmediato"),
    RassOption(+3,"Muy agitado","Retira tubos"),
    RassOption(+2,"Agitado","Movimientos frecuentes"),
    RassOption(+1,"Intranquilo","Ansioso"),
    RassOption(0,"Alerta y calmado","Estado ideal"),
    RassOption(-1,"Somnoliento","Despierta >10 seg"),
    RassOption(-2,"Sedación ligera","Despierta <10 seg"),
    RassOption(-3,"Sedación moderada","Sin contacto visual"),
    RassOption(-4,"Sedación profunda","Solo responde físico"),
    RassOption(-5,"No despierta","Sin respuesta")
)

data class RassResult(
    val label: String,
    val color: Color
)

fun interpretRass(score: Int): RassResult = when {
    score >= 4 ->
        RassResult(
            "AGITACIÓN EXTREMA",
            Color(0xFFB71C1C)
        )

    score == 3 ->
        RassResult(
            "MUY AGITADO",
            Color(0xFFF44336)
        )

    score == 2 ->
        RassResult(
            "AGITADO",
            Color(0xFFFF7043)
        )

    score == 1 ->
        RassResult(
            "INTRANQUILO",
            Color(0xFFFF9800)
        )

    score == 0 ->
        RassResult(
            "ALERTA Y CALMADO",
            Color(0xFF4CAF50)
        )

    score == -1 ->
        RassResult(
            "SOMNOLIENTO",
            Color(0xFF8BC34A)
        )

    score == -2 ->
        RassResult(
            "SEDACIÓN LIGERA",
            Color(0xFF2196F3)
        )

    score == -3 ->
        RassResult(
            "SEDACIÓN MODERADA",
            Color(0xFF1976D2)
        )

    score == -4 ->
        RassResult(
            "SEDACIÓN PROFUNDA",
            Color(0xFF7B1FA2)
        )

    else ->
        RassResult(
            "NO DESPIERTA",
            Color(0xFF4A148C)
        )
}

fun rassColor(score: Int): Color = when {
    score >= 3 -> Color(0xFFF44336)
    score == 2 -> Color(0xFFFF7043)
    score == 1 -> Color(0xFFFF9800)
    score == 0 -> Color(0xFF4CAF50)
    score == -1 -> Color(0xFF8BC34A)
    score == -2 -> Color(0xFF2196F3)
    score == -3 -> Color(0xFF1976D2)
    score == -4 -> Color(0xFF7B1FA2)
    else -> Color(0xFF4A148C)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RassScreen(
    patient: Patient,
    onBackClick: () -> Unit = {},
    onNewEvaluation: () -> Unit = {},
    onHomeClick: () -> Unit = {}
) {

    var selectedScore by remember {
        mutableStateOf<Int?>(null)
    }

    var showResult by remember {
        mutableStateOf(false)
    }

    val context =
        LocalContext.current

    val db =
        FirebaseFirestore.getInstance()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (showResult)
                            "Resultado RASS"
                        else
                            "Escala RASS",

                        color =
                            Color.White
                    )
                },

                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (showResult)
                                showResult = false
                            else
                                onBackClick()
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

        if (!showResult) {

            RassSelectionStep(
                selectedScore =
                    selectedScore,

                onSelect = {
                    selectedScore = it
                },

                onEvaluate = {
                    if (selectedScore != null)
                        showResult = true
                },

                modifier =
                    Modifier.padding(
                        padding
                    )
            )

        } else {

            selectedScore?.let { score ->

                LaunchedEffect(Unit) {

                    val result =
                        interpretRass(score)

                    val data =
                        hashMapOf(

                            "pacienteNombre" to
                                    "${patient.nombre} ${patient.apellido}",

                            "pacienteDocumento" to
                                    patient.noDoc,

                            "escala" to
                                    "RASS",

                            "puntaje" to
                                    score,

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

                RassResultStep(
                    score = score,

                    onNewEvaluation = {
                        selectedScore = null
                        showResult = false
                        onNewEvaluation()
                    },

                    onHome =
                        onHomeClick,

                    modifier =
                        Modifier.padding(
                            padding
                        )
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(20.dp)
    ) {

        Text(
            "Seleccione estado clínico",
            fontSize = 22.sp,
            fontWeight =
                FontWeight.Bold,
            color = DarkBlue
        )

        Spacer(
            modifier =
                Modifier.height(20.dp)
        )

        Column(
            modifier =
                Modifier.weight(1f)
                    .verticalScroll(
                        rememberScrollState()
                    )
        ) {

            RASS_OPTIONS.forEach {

                val selected =
                    selectedScore ==
                            it.score

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp)
                        .clickable {
                            onSelect(it.score)
                        },

                    border =
                        if (selected)
                            BorderStroke(
                                2.dp,
                                rassColor(
                                    it.score
                                )
                            )
                        else
                            BorderStroke(
                                1.dp,
                                Color.LightGray
                            )
                ) {

                    Row(
                        modifier =
                            Modifier.padding(
                                14.dp
                            ),

                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Box(
                            modifier =
                                Modifier
                                    .size(38.dp)
                                    .clip(
                                        CircleShape
                                    )
                                    .background(
                                        rassColor(
                                            it.score
                                        )
                                    ),

                            contentAlignment =
                                Alignment.Center
                        ) {

                            Text(
                                if (it.score > 0)
                                    "+${it.score}"
                                else
                                    it.score.toString(),

                                color =
                                    Color.White
                            )
                        }

                        Spacer(
                            modifier =
                                Modifier.width(
                                    14.dp
                                )
                        )

                        Column {

                            Text(
                                it.term,
                                fontWeight =
                                    FontWeight.Bold
                            )

                            Text(
                                it.description,
                                fontSize =
                                    12.sp,
                                color =
                                    Color.Gray
                            )
                        }
                    }
                }
            }
        }

        Button(
            onClick =
                onEvaluate,

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
                "Ver resultado",
                color = Color.White
            )
        }
    }
}

@Composable
fun RassResultStep(
    score: Int,
    onNewEvaluation: () -> Unit,
    onHome: () -> Unit,
    modifier: Modifier = Modifier
) {

    val result =
        interpretRass(score)

    Column(
        modifier = modifier
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
            modifier =
                Modifier
                    .size(120.dp)
                    .clip(
                        CircleShape
                    )
                    .background(
                        result.color
                    ),

            contentAlignment =
                Alignment.Center
        ) {

            Text(
                if (score > 0)
                    "+$score"
                else
                    score.toString(),

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
            color =
                result.color,
            fontWeight =
                FontWeight.Bold,
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
                Arrangement.spacedBy(
                    10.dp
                )
        ) {

            OutlinedButton(
                onClick =
                    onNewEvaluation,

                modifier =
                    Modifier.weight(
                        1f
                    )
            ) {
                Text(
                    "Nueva"
                )
            }

            Button(
                onClick = onHome,

                modifier =
                    Modifier.weight(
                        1f
                    ),

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
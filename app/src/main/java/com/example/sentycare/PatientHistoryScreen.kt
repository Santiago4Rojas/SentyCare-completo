package com.example.sentycare

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import com.example.sentycare.ui.theme.*
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

data class Evaluacion(
    val id: String = "",
    val pacienteDocumento: String = "",
    val pacienteNombre: String = "",
    val escala: String = "",
    val puntaje: Any? = "",
    val clasificacion: String = "",
    val fecha: Long = 0L,
    val evaluadorId: String = "",
    val evaluadorNombre: String = "",
    val evaluadorEspecialidad: String = "",
    val evaluadorNivel: String = "",
    val recomendacionMedico: String = "",
    val recomendacionIA: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientHistoryScreen(
    patient: Patient,
    onBackClick: () -> Unit = {},
    onHomeClick: () -> Unit = {},
    onEvaluacionClick: () -> Unit = {},
    onInfoClick: () -> Unit = {}
) {

    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current

    var evaluaciones by remember { mutableStateOf<List<Evaluacion>>(emptyList()) }
    var cargando by remember { mutableStateOf(true) }
    var selectedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    val isSelecting = selectedIds.isNotEmpty()

    BackHandler {
        if (isSelecting) selectedIds = emptySet() else onBackClick()
    }

    LaunchedEffect(Unit) {
        db.collection("evaluaciones")
            .whereEqualTo("pacienteDocumento", patient.noDoc)
            .get()
            .addOnSuccessListener { result ->
                evaluaciones = result.documents.mapNotNull { doc ->
                    doc.toObject(Evaluacion::class.java)?.copy(id = doc.id)
                }.sortedByDescending { it.fecha }
                cargando = false
            }
            .addOnFailureListener { cargando = false }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSelecting)
                        Text("${selectedIds.size} seleccionada(s)", color = Color.White, fontWeight = FontWeight.Bold)
                    else
                        Text("Historial", color = Color.White, fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    if (isSelecting) {
                        IconButton(onClick = { selectedIds = emptySet() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                        }
                    }
                },
                actions = {
                    if (isSelecting) {
                        IconButton(onClick = {
                            val seleccionadas = evaluaciones.filter { it.id in selectedIds }
                            PdfExportHelper.generarYCompartirPdf(context, patient, seleccionadas)
                        }) {
                            Icon(Icons.Outlined.PictureAsPdf, contentDescription = "Exportar PDF", tint = Color.White)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBlue)
            )
        },
        bottomBar = {
            if (!isSelecting) {
                SentyCareBottomBar(
                    currentTab = SentyCareTab.HISTORIAL,
                    onInicioClick = onHomeClick,
                    onEvaluacionClick = onEvaluacionClick,
                    onHistorialClick = {},
                    onInfoClick = onInfoClick
                )
            }
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.White)
        ) {

            // TARJETA PACIENTE
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),

                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            Color(0xFFF5F8FC)
                    ),

                shape =
                    RoundedCornerShape(12.dp)
            ) {

                Column(
                    modifier = Modifier
                        .padding(16.dp)
                ) {

                    Text(
                        "${patient.nombre} ${patient.apellido}",
                        fontSize = 18.sp,
                        fontWeight =
                            FontWeight.Bold,
                        color = DarkBlue
                    )

                    Spacer(
                        modifier =
                            Modifier.height(4.dp)
                    )

                    Text(
                        "Documento: ${patient.noDoc}",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )

                    Text(
                        "Cama ${patient.numeroCama}",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                }
            }

            when {

                cargando -> {

                    Box(
                        modifier =
                            Modifier.fillMaxSize(),
                        contentAlignment =
                            Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                evaluaciones.isEmpty() -> {

                    Box(
                        modifier =
                            Modifier.fillMaxSize(),
                        contentAlignment =
                            Alignment.Center
                    ) {

                        Column(
                            horizontalAlignment =
                                Alignment.CenterHorizontally
                        ) {

                            Icon(
                                Icons.Outlined.Assessment,
                                contentDescription = null,
                                tint = LightGray,
                                modifier =
                                    Modifier.size(64.dp)
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(12.dp)
                            )

                            Text(
                                "Sin evaluaciones aún",
                                color = DarkBlue,
                                fontWeight =
                                    FontWeight.Bold
                            )
                        }
                    }
                }

                else -> {
                    if (!isSelecting) {
                        Text(
                            "Mantén presionada una evaluación\npara seleccionarla y exportar PDF",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(evaluaciones) { item ->
                            val isSelected = item.id in selectedIds
                            SelectableEvaluationCard(
                                item = item,
                                isSelected = isSelected,
                                isSelecting = isSelecting,
                                onLongClick = { selectedIds = selectedIds + item.id },
                                onToggle = {
                                    selectedIds = if (isSelected) selectedIds - item.id else selectedIds + item.id
                                }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(20.dp)) }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SelectableEvaluationCard(
    item: Evaluacion,
    isSelected: Boolean,
    isSelecting: Boolean,
    onLongClick: () -> Unit,
    onToggle: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(if (isSelected) 2.dp else 0.dp, if (isSelected) DarkBlue else Color.Transparent, RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = { if (isSelecting) onToggle() },
                onLongClick = { if (!isSelecting) onLongClick() }
            )
    ) {
        EvaluationCard(item)
        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(DarkBlue),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.CheckCircle, null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun EvaluationCard(
    item: Evaluacion
) {

    val fechaTexto =
        remember(item.fecha) {
            SimpleDateFormat(
                "dd/MM/yyyy HH:mm",
                Locale.getDefault()
            ).format(Date(item.fecha))
        }

    val colorEscala = when {
        item.escala.contains("Comfort") ->
            Color(0xFF2196F3)

        item.escala.contains("RASS") ->
            Color(0xFF9C27B0)

        item.escala.contains("Dolor") ->
            Color(0xFFF44336)

        else ->
            DarkBlue
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 3.dp
            ),

        colors =
            CardDefaults.cardColors(
                containerColor = Color.White
            )
    ) {

        Column(
            modifier =
                Modifier.padding(16.dp)
        ) {

            Row(
                modifier =
                    Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.SpaceBetween,

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Text(
                    item.escala,
                    fontWeight =
                        FontWeight.Bold,
                    color = colorEscala,
                    fontSize = 16.sp
                )

                Text(
                    fechaTexto,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            Spacer(
                modifier =
                    Modifier.height(8.dp)
            )

            Text(
                "Puntaje: ${item.puntaje}",
                fontSize = 14.sp,
                color = DarkBlue
            )

            Text(
                item.clasificacion,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Gray
            )

            if (item.evaluadorNombre.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                val evaluadorInfo = buildString {
                    append(item.evaluadorNombre)
                    if (item.evaluadorEspecialidad.isNotBlank()) append(" · ${item.evaluadorEspecialidad}")
                    if (item.evaluadorNivel.isNotBlank()) append(" · ${item.evaluadorNivel}")
                }
                Text(
                    "Evaluado por: $evaluadorInfo",
                    fontSize = 12.sp,
                    color = Color(0xFF555555)
                )
            }

            if (item.recomendacionMedico.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Recomendación médico:", fontSize = 12.sp, color = DarkBlue, fontWeight = FontWeight.SemiBold)
                Text(item.recomendacionMedico, fontSize = 13.sp, color = Color.DarkGray)
            }

            if (item.recomendacionIA.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("✨", fontSize = 13.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Recomendación IA:", fontSize = 12.sp, color = Color(0xFF6200EE), fontWeight = FontWeight.SemiBold)
                }
                Text(item.recomendacionIA, fontSize = 13.sp, color = Color.DarkGray)
            }
        }
    }
}
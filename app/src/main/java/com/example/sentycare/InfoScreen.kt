package com.example.sentycare

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sentycare.ui.theme.*

// ─── Modelo de datos ──────────────────────────────────────────────────────────

data class EscalaInfo(
    val nombre: String,
    val emoji: String,
    val accentColor: Color,
    val descripcion: String,
    val indicacion: String,
    val rangos: List<Pair<String, String>>,  // rango → interpretación
    val nota: String? = null
)

val ESCALAS_INFO = listOf(
    EscalaInfo(
        nombre = "COMFORT-B",
        emoji = "🧠",
        accentColor = Color(0xFF4CAF50),
        descripcion = "Escala de sedación conductual diseñada para pacientes pediátricos en ventilación mecánica o con procedimientos dolorosos. Evalúa 6 dimensiones comportamentales sin requerir estimulación del paciente.",
        indicacion = "Pacientes ≤ 18 años en UCI Pediátrica, bajo ventilación mecánica o sedación continua.",
        rangos = listOf(
            "< 10"   to "Sobresedación — reducir sedantes",
            "10 – 12" to "Sedación profunda — considerar reducción gradual",
            "13 – 17" to "Sedación adecuada — mantener manejo actual",
            "18 – 22" to "Sedación insuficiente — aumentar dosis",
            "> 22"   to "Muy insuficiente — ajuste urgente"
        ),
        nota = "Evalúa: Nivel de conciencia · Calma-Agitación · Respuesta respiratoria · Movimientos físicos · Tono muscular · Tensión facial"
    ),
    EscalaInfo(
        nombre = "RASS",
        emoji = "💭",
        accentColor = Color(0xFFFF9800),
        descripcion = "Richmond Agitation-Sedation Scale. Escala de 10 niveles que cuantifica el grado de sedación y agitación del paciente. Es el estándar de referencia para ajustar la sedación en UCI.",
        indicacion = "Todo paciente en UCI que reciba sedación o que presente agitación. Precede la evaluación de delirio (CAM-ICU / CAPD).",
        rangos = listOf(
            "+4" to "Combativo — peligro inmediato para el personal",
            "+3" to "Muy agitado — se retira tubos/catéteres",
            "+2" to "Agitado — movimientos frecuentes sin propósito",
            "+1" to "Intranquilo — ansioso, sin movimientos agresivos",
            " 0" to "Alerta y calmado — objetivo terapéutico",
            "-1" to "Somnoliento — despierta > 10 seg al llamado",
            "-2" to "Sedación ligera — contacto visual < 10 seg",
            "-3" to "Sedación moderada — movimiento sin contacto visual",
            "-4" to "Sedación profunda — responde solo a estímulo físico",
            "-5" to "No despierta — sin respuesta a ningún estímulo"
        ),
        nota = "RASS -4 o -5: NO realizar evaluación de delirio. Reevaluar en el próximo turno."
    ),
    EscalaInfo(
        nombre = "FLACC",
        emoji = "🔍",
        accentColor = Color(0xFFFF7043),
        descripcion = "Face, Legs, Activity, Cry, Consolability. Escala de dolor observacional para pacientes que no pueden comunicar verbalmente su dolor. Ideal para neonatos, lactantes y niños con alteración del estado de conciencia.",
        indicacion = "Pacientes no comunicativos, ≤ 10 años o bajo sedación. No requiere participación activa del paciente.",
        rangos = listOf(
            "0"      to "Sin dolor — sin intervención requerida",
            "1 – 3"  to "Dolor leve — medidas no farmacológicas",
            "4 – 6"  to "Dolor moderado — Acetaminofén ± AINES",
            "7 – 10" to "Dolor severo — opioide IV (Morfina / Fentanil)"
        ),
        nota = "Evalúa 5 ítems (0-2 c/u): Cara · Piernas · Actividad · Llanto · Consolabilidad. Puntaje total: 0-10."
    ),
    EscalaInfo(
        nombre = "FACES (Wong-Baker)",
        emoji = "😊",
        accentColor = Color(0xFF2196F3),
        descripcion = "Escala de caras de Wong-Baker. El paciente señala directamente la cara que representa su nivel de dolor. Requiere que el niño comprenda la instrucción y pueda señalar.",
        indicacion = "Pacientes comunicativos ≥ 3 años con capacidad cognitiva para indicar su dolor.",
        rangos = listOf(
            "0"  to "Sin dolor 😊",
            "2"  to "Duele un poco 🙁",
            "4"  to "Duele un poco más 😟",
            "6"  to "Duele bastante 😣",
            "8"  to "Duele mucho 😢",
            "10" to "El peor dolor posible 😭"
        ),
        nota = "Escala subjetiva — validada para uso pediátrico en entornos hospitalarios. Puntuación par: 0, 2, 4, 6, 8, 10."
    )
)

// ─── Pantalla principal ───────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoScreen(
    onBackClick: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Escalas Clínicas",
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF0F4F8))
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Encabezado
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkBlue),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Guía de referencia",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Escalas de sedación, agitación y dolor utilizadas en la UCI Pediátrica del Hospital Universitario de Neiva.",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.85f),
                        lineHeight = 19.sp
                    )
                }
            }

            // Tarjeta de cada escala
            ESCALAS_INFO.forEach { escala ->
                EscalaInfoCard(escala = escala)
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

// ─── Tarjeta de escala ────────────────────────────────────────────────────────

@Composable
fun EscalaInfoCard(escala: EscalaInfo) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {

            // Encabezado con emoji y nombre
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(escala.accentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(escala.emoji, fontSize = 24.sp)
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = escala.nombre,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkBlue
                    )
                    Text(
                        text = escala.indicacion,
                        fontSize = 12.sp,
                        color = escala.accentColor,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Descripción
            Text(
                text = escala.descripcion,
                fontSize = 13.sp,
                color = Color.DarkGray,
                lineHeight = 19.sp
            )

            // Nota clínica si existe
            escala.nota?.let { nota ->
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(escala.accentColor.copy(alpha = 0.08f))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text("ℹ️", fontSize = 13.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = nota,
                        fontSize = 12.sp,
                        color = DarkBlue,
                        lineHeight = 17.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = Color(0xFFEEEEEE))
            Spacer(modifier = Modifier.height(12.dp))

            // Botón expandir/colapsar rangos
            TextButton(
                onClick = { expanded = !expanded },
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    text = if (expanded) "▲ Ocultar rangos" else "▼ Ver rangos de interpretación",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = escala.accentColor
                )
            }

            // Tabla de rangos
            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                escala.rangos.forEachIndexed { index, (rango, interpretacion) ->
                    RangoRow(
                        rango = rango,
                        interpretacion = interpretacion,
                        color = escala.accentColor,
                        isLast = index == escala.rangos.size - 1
                    )
                }
            }
        }
    }
}

// ─── Fila de rango ────────────────────────────────────────────────────────────

@Composable
fun RangoRow(
    rango: String,
    interpretacion: String,
    color: Color,
    isLast: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(color.copy(alpha = 0.12f))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                text = rango.trim(),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = interpretacion,
            fontSize = 13.sp,
            color = Color.DarkGray,
            modifier = Modifier.weight(1f),
            lineHeight = 17.sp
        )
    }
    if (!isLast) HorizontalDivider(color = Color(0xFFF5F5F5))
}
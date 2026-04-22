package com.example.sentycare

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sentycare.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EvaluationScreen(
    patient: Patient,
    onChangePatient: () -> Unit = {},
    onComfortClick: () -> Unit = {},
    onRassClick: () -> Unit = {},
    onDolorClick: () -> Unit = {},
    onHomeClick: () -> Unit = {},
    onHistorialClick: () -> Unit = {},
    onInfoClick: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "SentyCare",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBlue)
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 4.dp
            ) {
                NavigationBarItem(
                    selected = true,
                    onClick = onHomeClick,
                    icon = { Icon(Icons.Filled.Home, contentDescription = "Inicio") },
                    label = { Text("Inicio") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = DarkBlue,
                        selectedTextColor = DarkBlue,
                        indicatorColor = Color(0xFFE8EAF6)
                    )
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onHistorialClick,
                    icon = { Icon(Icons.Outlined.AccessTime, contentDescription = "Historial") },
                    label = { Text("Historial") },
                    colors = NavigationBarItemDefaults.colors(
                        unselectedIconColor = LightGray,
                        unselectedTextColor = LightGray
                    )
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onInfoClick,
                    icon = { Icon(Icons.Filled.Info, contentDescription = "Info") },
                    label = { Text("Info") },
                    colors = NavigationBarItemDefaults.colors(
                        unselectedIconColor = LightGray,
                        unselectedTextColor = LightGray
                    )
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF0F4F8))
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            // Tarjeta del paciente
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Person,
                                contentDescription = null,
                                tint = DarkBlue,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${patient.nombre} ${patient.apellido}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = DarkBlue
                            )
                        }
                        OutlinedButton(
                            onClick = onChangePatient,
                            shape = RoundedCornerShape(8.dp),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                brush = androidx.compose.ui.graphics.SolidColor(DarkBlue)
                            ),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color.White,
                                contentColor = DarkBlue
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(text = "Cambiar", fontSize = 12.sp, color = DarkBlue)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = Color(0xFFEEEEEE))
                    Spacer(modifier = Modifier.height(10.dp))

                    // Grid 2x2 datos clínicos
                    Row(modifier = Modifier.fillMaxWidth()) {
                        PatientInfoChip(label = "Cama", value = patient.numeroCama, modifier = Modifier.weight(1f))
                        PatientInfoChip(label = "RH", value = patient.rh, modifier = Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        PatientInfoChip(label = "Género", value = patient.genero, modifier = Modifier.weight(1f))
                        PatientInfoChip(label = "Diagnóstico", value = patient.diagnostico, modifier = Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Color(0xFFEEEEEE))
                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Seleccione la escala a aplicar",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            ScaleCard(
                title = "COMFORT B",
                subtitle = "Nivel de sedación (6-30)",
                circleColor = Color(0xFF4CAF50),
                emoji = "🧠",
                onClick = onComfortClick
            )

            Spacer(modifier = Modifier.height(12.dp))

            ScaleCard(
                title = "RASS",
                subtitle = "Agitación – Sedación (+4 a -5)",
                circleColor = Color(0xFFFF9800),
                emoji = "💭",
                onClick = onRassClick
            )

            Spacer(modifier = Modifier.height(12.dp))

            ScaleCard(
                title = "DOLOR",
                subtitle = "FLACC / FACES",
                circleColor = Color(0xFFF44336),
                emoji = "😊",
                onClick = onDolorClick
            )
        }
    }
}

@Composable
fun PatientInfoChip(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(end = 8.dp)) {
        Text(text = label, fontSize = 11.sp, color = LightGray, fontWeight = FontWeight.Medium)
        Text(text = value, fontSize = 14.sp, color = DarkBlue, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun ScaleCard(
    title: String,
    subtitle: String,
    circleColor: Color,
    emoji: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(circleColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = emoji, fontSize = 24.sp)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkBlue
                )
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = Color.Gray
                )
            }
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = LightGray
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun EvaluationScreenPreview() {
    EvaluationScreen(
        patient = Patient(
            nombre = "Arturo",
            apellido = "García",
            genero = "Masculino",
            noDoc = "1234567890",
            fechaNacimiento = "01/03/2020",
            rh = "O+",
            numeroCama = "12",
            diagnostico = "Cáncer"
        )
    )
}
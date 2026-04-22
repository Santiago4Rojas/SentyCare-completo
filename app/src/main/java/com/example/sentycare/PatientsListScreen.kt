package com.example.sentycare

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sentycare.ui.theme.*
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientsListScreen(
    patients: List<Patient> = emptyList(),
    onBackClick: () -> Unit = {},
    onPatientClick: (Patient) -> Unit = {}
) {
    val db = FirebaseFirestore.getInstance()

    var patientsFirebase by remember {
        mutableStateOf<List<Patient>>(emptyList())
    }

    LaunchedEffect(Unit) {

        db.collection("pacientes")
            .get()
            .addOnSuccessListener { result ->

                val lista = result.documents.mapNotNull { doc ->
                    doc.toObject(Patient::class.java)
                }

                patientsFirebase = lista
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Pacientes Registrados",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },

                navigationIcon = {
                    IconButton(
                        onClick = onBackClick
                    ) {
                        Icon(
                            imageVector =
                                Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color.White
                        )
                    }
                },

                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBlue
                )
            )
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.White)
        ) {

            if (patientsFirebase.isEmpty()) {

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {

                    Column(
                        horizontalAlignment =
                            Alignment.CenterHorizontally
                    ) {

                        Icon(
                            imageVector =
                                Icons.Outlined.Person,
                            contentDescription = null,
                            tint = LightGray,
                            modifier = Modifier.size(64.dp)
                        )

                        Spacer(
                            modifier = Modifier.height(16.dp)
                        )

                        Text(
                            text = "No hay pacientes registrados",
                            fontSize = 16.sp,
                            color = DarkBlue,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(
                            modifier = Modifier.height(8.dp)
                        )

                        Text(
                            text = "Registre un paciente primero",
                            fontSize = 14.sp,
                            color = LightGray
                        )
                    }
                }

            } else {

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            horizontal = 16.dp,
                            vertical = 16.dp
                        ),

                    verticalArrangement =
                        Arrangement.spacedBy(12.dp)
                ) {

                    items(patientsFirebase) { patient ->

                        PatientCard(
                            patient = patient,
                            onClick = {
                                onPatientClick(patient)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PatientCard(
    patient: Patient,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClick = onClick
            ),

        shape = RoundedCornerShape(10.dp),

        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),

        elevation = CardDefaults.cardElevation(
            defaultElevation = 3.dp
        )
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Icon(
                imageVector = Icons.Outlined.Person,
                contentDescription = null,
                tint = DarkBlue,
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text =
                        "${patient.nombre} ${patient.apellido}",

                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkBlue
                )

                Spacer(
                    modifier = Modifier.height(2.dp)
                )

                Text(
                    text =
                        "Cama ${patient.numeroCama} • ${patient.fechaNacimiento}",

                    fontSize = 13.sp,
                    color = Color.Gray
                )

                Text(
                    text = patient.diagnostico,
                    fontSize = 13.sp,
                    color = Color.Gray
                )
            }

            Icon(
                imageVector =
                    Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = LightGray
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PatientsListScreenPreview() {
    PatientsListScreen()
}
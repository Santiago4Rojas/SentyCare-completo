package com.example.sentycare

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sentycare.ui.theme.*
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsentScreen(
    patient: Patient,
    onBackClick: () -> Unit = {},
    onConsentAccepted: () -> Unit = {}
) {

    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()

    val paths = remember { mutableStateListOf<Path>() }
    var currentPath by remember { mutableStateOf<Path?>(null) }
    var hasDrawnSignature by remember { mutableStateOf(false) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    val canProceed = hasDrawnSignature

    fun saveConsent() {

        val consent = hashMapOf(

            "pacienteNombre" to
                    "${patient.nombre} ${patient.apellido}",

            "pacienteDocumento" to patient.noDoc,

            "responsableNombre" to
                    patient.acudienteNombre,

            "responsableCedula" to
                    patient.acudienteCedula,

            "responsableTelefono" to
                    patient.acudienteTelefono,

            "parentesco" to
                    patient.acudienteParentesco,

            "firmo" to true,

            "fecha" to
                    System.currentTimeMillis()
        )

        db.collection("consentimientos")
            .add(consent)
            .addOnSuccessListener {

                Toast.makeText(
                    context,
                    "Consentimiento guardado",
                    Toast.LENGTH_LONG
                ).show()

                onConsentAccepted()
            }
            .addOnFailureListener {

                Toast.makeText(
                    context,
                    "Error al guardar",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    fun openLink(url: String) {
        val intent =
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse(url)
            )

        context.startActivity(intent)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text =
                            "Consentimiento Informado",
                        color = Color.White,
                        fontWeight =
                            FontWeight.Bold
                    )
                },

                navigationIcon = {
                    IconButton(
                        onClick = onBackClick
                    ) {
                        Icon(
                            imageVector =
                                Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription =
                                null,
                            tint = Color.White
                        )
                    }
                },

                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor =
                            DarkBlue
                    )
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.White)
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(20.dp)
        ) {

            Card(
                modifier =
                    Modifier.fillMaxWidth(),
                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            Color(0xFFEEF2FB)
                    )
            ) {

                Row(
                    modifier =
                        Modifier.padding(16.dp),
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Icon(
                        imageVector =
                            Icons.Outlined.Person,
                        contentDescription =
                            null,
                        tint = DarkBlue
                    )

                    Spacer(
                        modifier =
                            Modifier.width(10.dp)
                    )

                    Column {

                        Text(
                            text =
                                "${patient.nombre} ${patient.apellido}",
                            fontWeight =
                                FontWeight.Bold,
                            color = DarkBlue
                        )

                        Text(
                            text =
                                "Cama ${patient.numeroCama}",
                            fontSize = 13.sp,
                            color = Color.Gray
                        )

                        Text(
                            text =
                                patient.diagnostico,
                            fontSize = 13.sp,
                            color = Color.Gray
                        )
                    }
                }
            }

            Spacer(
                modifier =
                    Modifier.height(20.dp)
            )

            Text(
                text = "Responsable",
                fontSize = 18.sp,
                fontWeight =
                    FontWeight.Bold,
                color = DarkBlue
            )

            Spacer(
                modifier =
                    Modifier.height(10.dp)
            )

            Card(
                modifier =
                    Modifier.fillMaxWidth()
            ) {

                Column(
                    modifier =
                        Modifier.padding(16.dp)
                ) {

                    Text(
                        text =
                            "Nombre: ${patient.acudienteNombre}"
                    )

                    Spacer(
                        modifier =
                            Modifier.height(6.dp)
                    )

                    Text(
                        text =
                            "Cédula: ${patient.acudienteCedula}"
                    )

                    Spacer(
                        modifier =
                            Modifier.height(6.dp)
                    )

                    Text(
                        text =
                            "Teléfono: ${patient.acudienteTelefono}"
                    )

                    Spacer(
                        modifier =
                            Modifier.height(6.dp)
                    )

                    Text(
                        text =
                            "Parentesco: ${patient.acudienteParentesco}"
                    )
                }
            }

            Spacer(
                modifier =
                    Modifier.height(20.dp)
            )

            Text(
                text =
                    "Declaro que autorizo las evaluaciones clínicas del paciente y he recibido información suficiente.",
                color = Color.DarkGray,
                fontSize = 14.sp,
                lineHeight = 22.sp
            )

            Spacer(
                modifier =
                    Modifier.height(18.dp)
            )

            Card(
                modifier =
                    Modifier.fillMaxWidth(),
                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            Color(0xFFF4F7FB)
                    )
            ) {

                Column(
                    modifier =
                        Modifier.padding(16.dp)
                ) {

                    Text(
                        text =
                            "Marco Legal",
                        fontWeight =
                            FontWeight.Bold,
                        color = DarkBlue
                    )

                    Spacer(
                        modifier =
                            Modifier.height(10.dp)
                    )

                    TextButton(
                        onClick = {
                            openLink(
                                "https://www.funcionpublica.gov.co/eva/gestornormativo/norma.php?i=49981"
                            )
                        },
                        contentPadding =
                            PaddingValues(0.dp)
                    ) {
                        Text(
                            text =
                                "🔗 Ley 1581 de 2012 - Protección de Datos Personales",
                            color =
                                MediumBlue,
                            textDecoration =
                                TextDecoration.Underline
                        )
                    }

                    TextButton(
                        onClick = {
                            openLink(
                                "https://www.funcionpublica.gov.co/eva/gestornormativo/norma.php?i=34492"
                            )
                        },
                        contentPadding =
                            PaddingValues(0.dp)
                    ) {
                        Text(
                            text =
                                "🔗 Ley 1273 de 2009 - Delitos Informáticos",
                            color =
                                MediumBlue,
                            textDecoration =
                                TextDecoration.Underline
                        )
                    }
                }
            }

            Spacer(
                modifier =
                    Modifier.height(24.dp)
            )

            Text(
                text =
                    "Firma del responsable *",
                fontWeight =
                    FontWeight.Bold,
                color = DarkBlue
            )

            Spacer(
                modifier =
                    Modifier.height(10.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .border(
                        1.dp,
                        LightGray,
                        RoundedCornerShape(8.dp)
                    )
                    .background(Color.White)
                    .onGloballyPositioned {
                        canvasSize = it.size
                    }
                    .pointerInput(Unit) {

                        detectDragGestures(

                            onDragStart = {
                                currentPath =
                                    Path().apply {
                                        moveTo(
                                            it.x,
                                            it.y
                                        )
                                    }

                                hasDrawnSignature =
                                    true
                            },

                            onDrag = {
                                    change, _ ->

                                currentPath?.lineTo(
                                    change.position.x,
                                    change.position.y
                                )
                            },

                            onDragEnd = {

                                currentPath?.let {
                                    paths.add(it)
                                }

                                currentPath =
                                    null
                            }
                        )
                    }
            ) {

                Canvas(
                    modifier =
                        Modifier.fillMaxSize()
                ) {

                    paths.forEach { path ->

                        drawPath(
                            path = path,
                            color = DarkBlue,
                            style =
                                Stroke(
                                    width = 4f
                                )
                        )
                    }

                    currentPath?.let { path ->

                        drawPath(
                            path = path,
                            color = DarkBlue,
                            style =
                                Stroke(
                                    width = 4f
                                )
                        )
                    }
                }

                if (!hasDrawnSignature) {

                    Text(
                        text = "Firme aquí",
                        color = LightGray,
                        modifier =
                            Modifier.align(
                                Alignment.Center
                            )
                    )
                }
            }

            TextButton(
                onClick = {
                    paths.clear()
                    currentPath = null
                    hasDrawnSignature = false
                },

                modifier =
                    Modifier.align(
                        Alignment.End
                    )
            ) {
                Text("Borrar firma")
            }

            Spacer(
                modifier =
                    Modifier.height(24.dp)
            )

            Button(
                onClick = {
                    saveConsent()
                },

                enabled =
                    canProceed,

                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),

                colors =
                    ButtonDefaults.buttonColors(
                        containerColor =
                            DarkBlue,
                        disabledContainerColor =
                            LightGray
                    )
            ) {

                Text(
                    text =
                        "Confirmar Consentimiento",
                    color = Color.White
                )
            }

            Spacer(
                modifier =
                    Modifier.height(20.dp)
            )
        }
    }
}
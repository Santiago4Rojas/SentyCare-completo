package com.example.sentycare.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import com.example.sentycare.ui.theme.*
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordRecoveryScreen(
    onBackClick: () -> Unit
) {
    BackHandler { onBackClick() }
    val focusManager = LocalFocusManager.current
    var email by remember { mutableStateOf("") }

    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Recuperar cuenta",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Regresar",
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
                .padding(24.dp)
                .pointerInput(Unit){
                    detectTapGestures(onTap = { focusManager.clearFocus() })
                },


            horizontalAlignment = Alignment.CenterHorizontally

        ) {

            Text(
                text = "Ingresa tu correo para recibir las instrucciones de restablecimiento.",
                fontSize = 14.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(32.dp))

            SentyCareField(
                label = "Correo de recuperación",
                value = email,
                onValueChange = { email = it },
                placeholder = "Ingresa tu usuario"
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {

                    auth.sendPasswordResetEmail(email)
                        .addOnSuccessListener {

                            Toast.makeText(
                                context,
                                "Correo enviado. Revisa bandeja y spam.",
                                Toast.LENGTH_LONG
                            ).show()

                            onBackClick()
                        }
                        .addOnFailureListener { e ->

                            Toast.makeText(
                                context,
                                e.message ?: "Error al enviar correo",
                                Toast.LENGTH_LONG
                            ).show()
                        }

                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),

                shape = RoundedCornerShape(10.dp),

                border = if (email.isNotEmpty()) null else BorderStroke(1.dp, LightGray),

                colors = ButtonDefaults.buttonColors(
                    containerColor = DarkBlue,
                    disabledContainerColor = Color.White,
                    contentColor = Color.White,
                    disabledContentColor = LightGray
                ),

                enabled = email.isNotEmpty()
            ) {
                Text(
                    text = "Enviar código",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
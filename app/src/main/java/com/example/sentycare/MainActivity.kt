package com.example.sentycare

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import com.example.sentycare.ui.screens.*
import com.example.sentycare.ui.theme.SentyCareTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {

            SentyCareTheme {

                var currentScreen by remember {
                    mutableStateOf("login")
                }

                var currentPatient by remember {
                    mutableStateOf<Patient?>(null)
                }

                var patientsList by remember {
                    mutableStateOf(
                        PatientRepository.patients.toList()
                    )
                }
                var doctorName by remember { mutableStateOf("Doctor") }

                when (currentScreen) {

                    // LOGIN
                    "login" -> LoginScreen(
                        onNavigateToRecovery = { currentScreen = "recovery" },
                        onLoginClick = { nombre ->
                            doctorName = nombre          // guarda el nombre en un var
                            currentScreen = "home"
                        }
                    )

                    // RECUPERAR CONTRASEÑA
                    "recovery" -> PasswordRecoveryScreen(

                        onBackClick = {
                            currentScreen = "login"
                        }
                    )

                    // HOME
                    "home" -> HomeScreen(
                        doctorName = doctorName,
                        onRegisterClick = {
                            currentScreen = "register"
                        },
                        onPatientClick = { patient ->
                            currentPatient = patient
                            currentScreen = "evaluation"
                        },
                        onLogoutClick = {
                            currentScreen = "login"
                        }
                    )

                    // REGISTRAR PACIENTE
                    "register" -> RegisterScreen(

                        onBackClick = {
                            currentScreen = "home"
                        },

                        onCancelClick = {
                            currentScreen = "home"
                        },

                        onRegisterClick = { patient ->

                            PatientRepository.addPatient(patient)

                            patientsList =
                                PatientRepository.patients.toList()

                            currentPatient = patient

                            currentScreen = "consent"
                        }
                    )

                    // CONSENTIMIENTO
                    "consent" -> currentPatient?.let { patient ->

                        ConsentScreen(
                            patient = patient,

                            onBackClick = {
                                currentScreen = "register"
                            },

                            onConsentAccepted = {
                                currentScreen = "evaluation"
                            }
                        )
                    }

                    "evaluation" -> currentPatient?.let { patient ->
                        EvaluationScreen(
                            patient = patient,
                            onHomeClick = { currentScreen = "home" },
                            onComfortClick = { currentScreen = "comfortB" },
                            onRassClick = { currentScreen = "Rass" },
                            onDolorClick = { currentScreen = "Dolor" },
                            onHistorialClick = { currentScreen = "patientHistory" },
                            onInfoClick = { currentScreen = "info" },
                            onPatientUpdated = { updatedPatient ->
                                currentPatient = updatedPatient
                            },
                            onPatientDischarged = {
                                currentPatient = null
                                currentScreen = "home"
                            }
                        )
                    }

                    // HISTORIAL PACIENTE
                    "patientHistory" -> currentPatient?.let { patient ->
                        PatientHistoryScreen(
                            patient = patient,
                            onBackClick = { currentScreen = "evaluation" },
                            onHomeClick = { currentScreen = "home" },
                            onEvaluacionClick = { currentScreen = "evaluation" },
                            onInfoClick = { currentScreen = "info" }
                        )
                    }

                    // COMFORT B
                    "comfortB" -> currentPatient?.let { patient ->

                        ComfortBScreen(
                            patient = patient,

                            onBackClick = {
                                currentScreen = "evaluation"
                            },

                            onNewEvaluation = {
                                currentScreen = "evaluation"
                            },

                            onHomeClick = {
                                currentScreen = "home"
                            }
                        )
                    }

                    // RASS
                    "Rass" -> currentPatient?.let { patient ->

                        RassScreen(
                            patient = patient,

                            onBackClick = {
                                currentScreen = "evaluation"
                            },

                            onNewEvaluation = {
                                currentScreen = "evaluation"
                            },

                            onHomeClick = {
                                currentScreen = "home"
                            }
                        )
                    }

                    // DOLOR
                    "Dolor" -> currentPatient?.let { patient ->

                        DolorScreen(
                            patient = patient,

                            onBackClick = {
                                currentScreen = "evaluation"
                            },

                            onNewEvaluation = {
                                currentScreen = "evaluation"
                            },

                            onHomeClick = {
                                currentScreen = "home"
                            }
                        )
                    }

                    // INFO
                    "info" -> InfoScreen(
                        onBackClick = { currentScreen = "evaluation" },
                        onHomeClick = { currentScreen = "home" },
                        onEvaluacionClick = { currentScreen = "evaluation" },
                        onHistorialClick = { currentScreen = "patientHistory" }
                    )
                }
            }
        }
    }
}
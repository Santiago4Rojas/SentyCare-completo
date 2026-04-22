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

                when (currentScreen) {

                    // LOGIN
                    "login" -> LoginScreen(

                        onNavigateToRecovery = {
                            currentScreen = "recovery"
                        },

                        onNavigateToRegister = {
                            currentScreen = "createUser"
                        },

                        onLoginClick = {
                            currentScreen = "home"
                        }
                    )

                    // CREAR USUARIO
                    "createUser" -> CreateUserScreen(

                        onBackClick = {
                            currentScreen = "login"
                        },

                        onUserCreated = {
                            currentScreen = "login"
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

                        onRegisterClick = {
                            currentScreen = "register"
                        },

                        onViewPatientsClick = {
                            currentScreen = "patientsList"
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

                    // LISTA PACIENTES
                    "patientsList" -> PatientsListScreen(

                        patients = patientsList,

                        onBackClick = {
                            currentScreen = "home"
                        },

                        onPatientClick = { patient ->

                            currentPatient = patient
                            currentScreen = "evaluation"
                        }
                    )

                    // MENÚ EVALUACIONES
                    "evaluation" -> currentPatient?.let { patient ->

                        EvaluationScreen(
                            patient = patient,

                            onChangePatient = {
                                currentScreen = "patientsList"
                            },

                            onHomeClick = {
                                currentScreen = "home"
                            },

                            onComfortClick = {
                                currentScreen = "comfortB"
                            },

                            onRassClick = {
                                currentScreen = "Rass"
                            },

                            onDolorClick = {
                                currentScreen = "Dolor"
                            },

                            onHistorialClick = {
                                currentScreen = "patientHistory"
                            },

                            onInfoClick = {
                                currentScreen = "info"
                            }
                        )
                    }

                    // HISTORIAL PACIENTE
                    "patientHistory" -> currentPatient?.let { patient ->

                        PatientHistoryScreen(
                            patient = patient,

                            onBackClick = {
                                currentScreen = "evaluation"
                            }
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

                        onBackClick = {
                            currentScreen = "evaluation"
                        }
                    )
                }
            }
        }
    }
}
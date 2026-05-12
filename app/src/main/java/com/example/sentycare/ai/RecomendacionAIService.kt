package com.example.sentycare.ai

import android.util.Log
import com.example.sentycare.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ContextoEvaluacion(
    val escala: String,
    val puntaje: Int,
    val clasificacion: String,
    val nombrePaciente: String,
    val diagnostico: String,
    val fechaNacimiento: String,
    val historialReciente: List<Triple<String, Int, String>> = emptyList()
)

object RecomendacionAIService {

    private const val MODELO = "gemini-1.5-flash"
    private const val TAG    = "RecomendacionAI"

    private val model by lazy {
        check(BuildConfig.GEMINI_API_KEY.isNotBlank()) {
            "GEMINI_API_KEY no configurada — agrega GEMINI_API_KEY en local.properties"
        }
        GenerativeModel(modelName = MODELO, apiKey = BuildConfig.GEMINI_API_KEY)
    }

    // Returns the recommendation text, or throws an exception with a descriptive message.
    suspend fun generarRecomendacion(ctx: ContextoEvaluacion): String = withContext(Dispatchers.IO) {
        val historialTexto = if (ctx.historialReciente.isEmpty()) {
            "Sin historial previo."
        } else {
            ctx.historialReciente.take(3).joinToString("\n") { (escala, puntaje, clas) ->
                "- $escala: puntaje $puntaje → $clas"
            }
        }

        val protocolos = """
PROTOCOLOS DE REFERENCIA (SCCM 2022 / PICU guidelines):
- COMFORT-B (6-30): objetivo terapéutico 13-17 (sedación adecuada).
  <10=sobresedación (reducir), 10-12=sedación profunda, 13-17=adecuada (mantener),
  18-22=insuficiente (ajustar), >22=muy insuficiente (intervención urgente).
- RASS (-5 a +4): objetivo 0 a -2 en ventilación mecánica.
  ≥+2=agitación que requiere intervención. ≤-4=sedación profunda, evaluar reducción.
- FLACC (0-10): 0=sin dolor, 1-3=leve (medidas no farmacológicas), 4-6=moderado
  (analgesia leve), 7-10=severo (analgesia mayor o derivación).
- FACES (0-10): autoreporte pediátrico. >4 requiere intervención analgésica.
        """.trimIndent()

        val prompt = """
Eres un asistente de apoyo clínico para UCI pediátrica, entrenado en protocolos internacionales de sedoanalgesia pediátrica.

$protocolos

DATOS DEL PACIENTE:
- Nombre: ${ctx.nombrePaciente}
- Diagnóstico: ${ctx.diagnostico}
- Fecha de nacimiento: ${ctx.fechaNacimiento}

EVALUACIÓN ACTUAL:
- Escala: ${ctx.escala}
- Puntaje: ${ctx.puntaje}
- Clasificación: ${ctx.clasificacion}

HISTORIAL RECIENTE (últimas evaluaciones):
$historialTexto

Basado en los protocolos de sedoanalgesia pediátrica (SCCM 2022, PICU guidelines), proporciona exactamente 3 recomendaciones clínicas específicas, accionables y basadas en evidencia para este paciente.
- Usa terminología médica apropiada en español.
- Considera el diagnóstico y el historial de evaluaciones.
- No incluyas advertencias legales ni descargos de responsabilidad.
- Responde únicamente los 3 puntos numerados (1. 2. 3.), sin encabezados adicionales.
        """.trimIndent()

        val response = model.generateContent(prompt)
        val texto = response.text?.trim().orEmpty()
        Log.d(TAG, "Respuesta IA (${texto.length} chars)")
        texto
    }
}

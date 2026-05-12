package com.example.sentycare

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object PdfExportHelper {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 40f
    private const val LINE_HEIGHT = 16f
    private const val SECTION_SPACING = 10f

    private val paintTitle = Paint().apply {
        textSize = 18f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        color = Color.rgb(10, 40, 80)
    }
    private val paintSection = Paint().apply {
        textSize = 13f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        color = Color.rgb(10, 40, 80)
    }
    private val paintLabel = Paint().apply {
        textSize = 10f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        color = Color.rgb(80, 80, 80)
    }
    private val paintBody = Paint().apply {
        textSize = 10f
        color = Color.rgb(40, 40, 40)
    }
    private val paintSmall = Paint().apply {
        textSize = 9f
        color = Color.rgb(120, 120, 120)
    }
    private val paintDivider = Paint().apply {
        color = Color.rgb(200, 200, 200)
        strokeWidth = 1f
    }
    private val paintHeaderBg = Paint().apply {
        color = Color.rgb(10, 40, 80)
    }
    private val paintHeaderText = Paint().apply {
        textSize = 14f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        color = Color.WHITE
    }
    private val paintEvalTitle = Paint().apply {
        textSize = 12f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        color = Color.rgb(30, 30, 30)
    }

    private val sdfDate = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    fun generarYCompartirPdf(
        context: Context,
        patient: Patient,
        evaluaciones: List<Evaluacion>
    ) {
        val doc = PdfDocument()
        var pageNum = 1
        var page = newPage(doc, pageNum)
        var canvas = page.canvas
        var y = MARGIN

        y = drawHeader(canvas, y)
        y += SECTION_SPACING
        y = drawGeneralInfo(canvas, y, patient)
        y += SECTION_SPACING

        if (patient.acudienteNombre.isNotBlank()) {
            if (y + 80 > PAGE_HEIGHT - MARGIN) {
                doc.finishPage(page); pageNum++; page = newPage(doc, pageNum); canvas = page.canvas; y = MARGIN
            }
            y = drawConsentInfo(canvas, y, patient)
            y += SECTION_SPACING
        }

        // Scale reference summary
        if (y + 100 > PAGE_HEIGHT - MARGIN) {
            doc.finishPage(page); pageNum++; page = newPage(doc, pageNum); canvas = page.canvas; y = MARGIN
        }
        y = drawScaleReference(canvas, y, evaluaciones)

        doc.finishPage(page)

        // Evaluation pages
        evaluaciones.forEachIndexed { idx, ev ->
            pageNum++
            page = newPage(doc, pageNum)
            canvas = page.canvas
            y = MARGIN
            drawEvaluacion(canvas, y, idx + 1, ev, patient)
            doc.finishPage(page)
        }

        // Save and share
        val fileName = "SentyCare_${patient.noDoc}_${System.currentTimeMillis()}.pdf"
        val file = File(context.cacheDir, fileName)
        doc.writeTo(FileOutputStream(file))
        doc.close()

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Evaluaciones SentyCare — ${patient.nombre} ${patient.apellido}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Exportar PDF"))
    }

    private fun newPage(doc: PdfDocument, number: Int): PdfDocument.Page {
        val info = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, number).create()
        return doc.startPage(info)
    }

    private fun drawHeader(canvas: Canvas, startY: Float): Float {
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 40f, paintHeaderBg)
        canvas.drawText("SentyCare — Informe de Evaluaciones UCI Pediátrica", MARGIN, 26f, paintHeaderText)
        val fecha = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        canvas.drawText("Generado: $fecha", PAGE_WIDTH - MARGIN - 80f, 26f, Paint(paintSmall).apply { color = Color.WHITE })
        return 50f
    }

    private fun drawGeneralInfo(canvas: Canvas, startY: Float, p: Patient): Float {
        var y = startY
        canvas.drawText("INFORMACIÓN DEL PACIENTE", MARGIN, y, paintSection)
        y += LINE_HEIGHT
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, paintDivider)
        y += 8f

        val col1 = MARGIN
        val col2 = 220f
        val rows = listOf(
            Pair("Nombre completo", "${p.nombre} ${p.apellido}"),
            Pair("Documento", p.noDoc),
            Pair("Fecha de nacimiento", p.fechaNacimiento),
            Pair("Género", p.genero),
            Pair("Tipo de sangre / RH", p.rh),
            Pair("Diagnóstico", p.diagnostico)
        )
        rows.forEachIndexed { i, (label, value) ->
            val x = if (i % 2 == 0) col1 else col2
            val rowY = y + (i / 2) * (LINE_HEIGHT + 4f)
            canvas.drawText(label, x, rowY, paintLabel)
            canvas.drawText(value.ifBlank { "—" }, x, rowY + 12f, paintBody)
        }
        y += ((rows.size / 2 + rows.size % 2) * (LINE_HEIGHT + 4f)) + LINE_HEIGHT
        if (p.registradoPorNombre.isNotBlank()) {
            canvas.drawText("Registrado por: ${p.registradoPorNombre}", MARGIN, y, paintSmall)
            y += LINE_HEIGHT
        }
        return y
    }

    private fun drawConsentInfo(canvas: Canvas, startY: Float, p: Patient): Float {
        var y = startY
        canvas.drawText("INFORMACIÓN DE CONSENTIMIENTO", MARGIN, y, paintSection)
        y += LINE_HEIGHT
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, paintDivider)
        y += 8f
        listOf(
            "Acudiente" to p.acudienteNombre,
            "Documento acudiente" to p.acudienteCedula,
            "Teléfono" to p.acudienteTelefono,
            "Parentesco" to p.acudienteParentesco,
            "Consentimiento aceptado" to "Sí"
        ).forEach { (label, value) ->
            canvas.drawText("$label: ", MARGIN, y, paintLabel)
            canvas.drawText(value.ifBlank { "—" }, MARGIN + 130f, y, paintBody)
            y += LINE_HEIGHT + 2f
        }
        return y
    }

    private fun drawScaleReference(canvas: Canvas, startY: Float, evaluaciones: List<Evaluacion>): Float {
        var y = startY
        canvas.drawText("REFERENCIA DE ESCALAS UTILIZADAS", MARGIN, y, paintSection)
        y += LINE_HEIGHT
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, paintDivider)
        y += 8f

        val escalasUsadas = evaluaciones.map { it.escala }.distinct()
        val refs = mapOf(
            "Comfort B" to "Sedación conductual pediátrica. Rango: 6–30. Objetivo: 13–17. <10=sobresedación, >22=muy insuficiente.",
            "RASS"      to "Richmond Agitation-Sedation Scale. Rango: -5 a +4. Objetivo: 0 a -2. ≥+2=agitación. ≤-4=sedación profunda.",
            "Dolor FLACC" to "Face, Legs, Activity, Cry, Consolability. Rango: 0–10. 0=sin dolor, 1-3=leve, 4-6=moderado, 7-10=severo.",
            "Dolor FACES" to "Wong-Baker FACES. Rango: 0–10. Autoreporte del paciente. 0=sin dolor, 10=peor dolor posible."
        )
        escalasUsadas.forEach { escala ->
            val desc = refs.entries.firstOrNull { escala.contains(it.key, ignoreCase = true) }?.value ?: return@forEach
            canvas.drawText("• $escala:", MARGIN, y, paintLabel)
            y += LINE_HEIGHT
            wrapText(canvas, desc, MARGIN + 10f, y, PAGE_WIDTH - MARGIN * 2 - 10f, paintSmall)
            y += LINE_HEIGHT + 4f
        }
        return y
    }

    private fun drawEvaluacion(canvas: Canvas, startY: Float, num: Int, ev: Evaluacion, p: Patient): Float {
        var y = startY
        // Mini header
        canvas.drawRect(MARGIN, y - 4f, PAGE_WIDTH - MARGIN, y + 18f, Paint().apply { color = Color.rgb(230, 240, 255) })
        canvas.drawText("Evaluación #$num — ${ev.escala}", MARGIN + 4f, y + 12f, paintEvalTitle)
        y += 26f

        val fecha = if (ev.fecha > 0) sdfDate.format(Date(ev.fecha)) else "—"
        canvas.drawText("Fecha: $fecha", MARGIN, y, paintBody)
        y += LINE_HEIGHT

        canvas.drawText("Puntaje: ${ev.puntaje}", MARGIN, y, paintBody)
        canvas.drawText("Clasificación: ${ev.clasificacion}", MARGIN + 120f, y, paintBody)
        y += LINE_HEIGHT

        if (ev.evaluadorNombre.isNotBlank()) {
            val evalInfo = buildString {
                append(ev.evaluadorNombre)
                if (ev.evaluadorEspecialidad.isNotBlank()) append(" · ${ev.evaluadorEspecialidad}")
            }
            canvas.drawText("Evaluado por: $evalInfo", MARGIN, y, paintSmall)
            y += LINE_HEIGHT
        }

        if (ev.recomendacionMedico.isNotBlank()) {
            y += 4f
            canvas.drawText("Recomendación médico:", MARGIN, y, paintLabel)
            y += LINE_HEIGHT
            y = wrapText(canvas, ev.recomendacionMedico, MARGIN + 8f, y, PAGE_WIDTH - MARGIN * 2 - 8f, paintBody)
            y += 4f
        }

        if (ev.recomendacionIA.isNotBlank()) {
            y += 4f
            canvas.drawText("Recomendación IA:", MARGIN, y, paintLabel)
            y += LINE_HEIGHT
            y = wrapText(canvas, ev.recomendacionIA, MARGIN + 8f, y, PAGE_WIDTH - MARGIN * 2 - 8f, paintBody)
            y += 4f
        }

        canvas.drawLine(MARGIN, y + 4f, PAGE_WIDTH - MARGIN, y + 4f, paintDivider)
        return y + 12f
    }

    private fun wrapText(canvas: Canvas, text: String, x: Float, startY: Float, maxWidth: Float, paint: Paint): Float {
        var y = startY
        val words = text.split(" ")
        val lineBuilder = StringBuilder()
        words.forEach { word ->
            val test = if (lineBuilder.isEmpty()) word else "$lineBuilder $word"
            if (paint.measureText(test) > maxWidth) {
                canvas.drawText(lineBuilder.toString(), x, y, paint)
                y += LINE_HEIGHT
                lineBuilder.clear()
                lineBuilder.append(word)
            } else {
                if (lineBuilder.isNotEmpty()) lineBuilder.append(" ")
                lineBuilder.append(word)
            }
        }
        if (lineBuilder.isNotEmpty()) {
            canvas.drawText(lineBuilder.toString(), x, y, paint)
            y += LINE_HEIGHT
        }
        return y
    }
}

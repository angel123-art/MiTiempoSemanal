
package com.senati.mitiemposemanal.mitiemposemanal

import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class ReportesActivity : AppCompatActivity() {

    private lateinit var db: DBHelper
    private lateinit var spinnerSemanas: Spinner
    private lateinit var contenedor: LinearLayout
    private lateinit var textoVacio: TextView

    private var reportes: List<ReporteSemanal> = emptyList()

    private val formatoFecha =
        DateTimeFormatter.ofPattern("dd/MM/yyyy")

    private val formatoFechaHora =
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        db = DBHelper(this)

        val pantalla = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 24, 20, 16)
        }

        pantalla.addView(TextView(this).apply {
            text = "Reportes semanales"
            textSize = 26f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 16)
        })

        spinnerSemanas = Spinner(this)
        pantalla.addView(spinnerSemanas)

        textoVacio = TextView(this).apply {
            text = "Aún no hay reportes"
            textSize = 16f
            setPadding(0, 16, 0, 16)
        }
        pantalla.addView(textoVacio)

        contenedor = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val scroll = ScrollView(this)
        scroll.addView(contenedor)

        pantalla.addView(
            scroll,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        pantalla.addView(
            android.widget.Button(this).apply {
                text = "Volver al inicio"
                setOnClickListener { finish() }
            }
        )

        setContentView(pantalla)

        spinnerSemanas.onItemSelectedListener =
            object : android.widget.AdapterView.OnItemSelectedListener {

                override fun onItemSelected(
                    parent: android.widget.AdapterView<*>?,
                    view: android.view.View?,
                    position: Int,
                    id: Long
                ) {
                    if (position in reportes.indices) {
                        mostrarReporte(reportes[position])
                    }
                }

                override fun onNothingSelected(
                    parent: android.widget.AdapterView<*>?
                ) {}
            }

        cargarReportes()
    }

    private fun cargarReportes() {
        reportes = db.obtenerReportesSemanales()

        if (reportes.isEmpty()) {
            textoVacio.visibility = android.view.View.VISIBLE
            textoVacio.text = "Aún no hay reportes"
            spinnerSemanas.isEnabled = false
            contenedor.removeAllViews()
            return
        }

        textoVacio.visibility = android.view.View.GONE
        spinnerSemanas.isEnabled = true

        val opciones = reportes.map { reporte ->
            val inicio = LocalDate.parse(reporte.inicioSemana)
            val fin = LocalDate.parse(reporte.finSemana)

            "${inicio.format(formatoFecha)} – ${fin.format(formatoFecha)}"
        }

        spinnerSemanas.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            opciones
        ).also {
            it.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
            )
        }
    }

    private fun mostrarReporte(reporte: ReporteSemanal) {
        contenedor.removeAllViews()

        agregarTexto(
            "Semana ${LocalDate.parse(reporte.inicioSemana).format(formatoFecha)} – " +
                    LocalDate.parse(reporte.finSemana).format(formatoFecha),
            20f
        )

        agregarTexto("Resumen por categoría", 18f)

        val categorias = db.obtenerCategoriasReporte(reporte.id)
        val totalSemana = 7L * 24 * 60 * 60
        val totalRegistrado = categorias.sumOf { it.totalSegundos }
        val sinRegistrar = (totalSemana - totalRegistrado).coerceAtLeast(0L)

        categorias.forEach { categoria ->
            val horas = categoria.totalSegundos / 3600
            val minutos = (categoria.totalSegundos % 3600) / 60

            agregarTexto(
                "${categoria.nombreCategoria}: " +
                        "${horas} h ${minutos} min · " +
                        String.format(Locale.US, "%.1f", categoria.porcentaje) + " %",
                16f
            )
        }

        val horasSinRegistrar = sinRegistrar / 3600
        val minutosSinRegistrar = (sinRegistrar % 3600) / 60
        val porcentajeSinRegistrar =
            sinRegistrar.toDouble() / totalSemana * 100.0

        agregarTexto(
            "Sin registrar: ${horasSinRegistrar} h ${minutosSinRegistrar} min · " +
                    String.format(Locale.US, "%.1f", porcentajeSinRegistrar) + " %",
            16f
        )

        agregarTexto("Eventos de la semana", 20f)

        val eventos = db.obtenerEventosReporte(
            reporte.inicioSemana,
            reporte.finSemana
        )

        if (eventos.isEmpty()) {
            agregarTexto("No hay eventos finalizados para mostrar.", 14f)
            return
        }

        eventos.forEach { evento ->
            val inicio = formatearFechaHora(evento.inicio)
            val fin = evento.fin?.let { formatearFechaHora(it) } ?: "En curso"
            val duracion = evento.duracionSegundos?.let {
                formatearDuracion(it)
            } ?: "En curso"

            agregarTexto(
                "${evento.nombreTarea} · ${evento.nombreCategoria}\n" +
                        "Inicio: $inicio\n" +
                        "Fin: $fin\n" +
                        "Duración: $duracion",
                14f
            )
        }
    }

    private fun agregarTexto(texto: String, tamano: Float) {
        contenedor.addView(TextView(this).apply {
            text = texto
            textSize = tamano
            setPadding(8, 12, 8, 12)
        })
    }

    private fun formatearFechaHora(fecha: String): String {
        return try {
            LocalDateTime.parse(fecha).format(formatoFechaHora)
        } catch (_: Exception) {
            fecha
        }
    }

    private fun formatearDuracion(segundos: Long): String {
        val horas = segundos / 3600
        val minutos = (segundos % 3600) / 60
        val seg = segundos % 60

        return "${horas} h ${minutos} min ${seg} s"
    }
}

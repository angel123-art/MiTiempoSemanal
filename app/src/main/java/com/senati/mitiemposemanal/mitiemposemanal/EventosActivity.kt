
package com.senati.mitiemposemanal.mitiemposemanal

import android.app.AlertDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import android.widget.ProgressBar
import java.time.LocalDate
import java.time.DayOfWeek
import java.util.Locale
class EventosActivity : AppCompatActivity() {
    private lateinit var contenedorResumenSemanal: LinearLayout
    private lateinit var textoSemana: TextView
    private lateinit var db: DBHelper
    private lateinit var cajaBusqueda: EditText
    private lateinit var textoCategoria: TextView
    private lateinit var textoEstado: TextView
    private lateinit var contenedorResultados: LinearLayout
    private lateinit var contenedorEventos: LinearLayout
    private lateinit var btnIniciar: Button
    private lateinit var btnFinalizar: Button

    private var tareaSeleccionadaId: Int? = null
    private var nombreTareaSeleccionada: String? = null
    private var categoriaSeleccionada: String? = null

    // Evita que la selección de una tarea se borre al actualizar el buscador.
    private var actualizandoBusqueda = false

    private val formato =
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        db = DBHelper(this)

        val pantalla = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 24, 20, 16)
            setBackgroundColor(0xFFF5F7FB.toInt())
        }

        pantalla.addView(TextView(this).apply {
            text = "Eventos"
            textSize = 26f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 16)
        })
        // PANEL SEMANAL - SPRINT 3
        textoSemana = TextView(this).apply {
            textSize = 16f
            setPadding(0, 8, 0, 8)
        }

        pantalla.addView(textoSemana)

        contenedorResumenSemanal = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(12, 12, 12, 12)
            setBackgroundColor(0xFFFFFFFF.toInt())
        }

        pantalla.addView(
            contenedorResumenSemanal,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        cajaBusqueda = EditText(this).apply {
            hint = "Buscar tarea (mínimo 2 letras)"
            setSingleLine(true)
        }
        pantalla.addView(cajaBusqueda)

        textoCategoria = TextView(this).apply {
            text = "Selecciona una tarea de los resultados"
            textSize = 14f
            setPadding(0, 8, 0, 8)
        }
        pantalla.addView(textoCategoria)

        pantalla.addView(TextView(this).apply {
            text = "Resultados de búsqueda"
            textSize = 18f
            setPadding(0, 8, 0, 4)
        })

        contenedorResultados = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        pantalla.addView(contenedorResultados)

        textoEstado = TextView(this).apply {
            textSize = 15f
            setPadding(0, 12, 0, 12)
        }
        pantalla.addView(textoEstado)

        btnIniciar = Button(this).apply {
            text = "Iniciar"
            setOnClickListener { iniciarSeleccion() }
        }
        pantalla.addView(btnIniciar)

        btnFinalizar = Button(this).apply {
            text = "Finalizar"
            setOnClickListener { finalizarActual() }
        }
        pantalla.addView(btnFinalizar)

        pantalla.addView(TextView(this).apply {
            text = "Eventos registrados"
            textSize = 20f
            setPadding(0, 16, 0, 8)
        })

        contenedorEventos = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val scroll = ScrollView(this)
        scroll.addView(contenedorEventos)

        pantalla.addView(
            scroll,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        pantalla.addView(Button(this).apply {
            text = "Volver al inicio"
            setOnClickListener { finish() }
        })

        setContentView(pantalla)

        cajaBusqueda.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {}

            override fun onTextChanged(
                s: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
                if (actualizandoBusqueda) return

                tareaSeleccionadaId = null
                nombreTareaSeleccionada = null
                categoriaSeleccionada = null

                textoCategoria.text =
                    "Selecciona una tarea de los resultados"

                cargarResultados(s?.toString().orEmpty())
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        actualizarEstado()
        cargarEventos()
        actualizarResumenSemanal()
    }
    override fun onResume() {
        super.onResume()

        if (::db.isInitialized && ::textoEstado.isInitialized) {
            actualizarEstado()
            cargarEventos()
            actualizarResumenSemanal()
        }
    }

    // BÚSQUEDA EN LA BASE DE DATOS
    private fun cargarResultados(texto: String) {
        contenedorResultados.removeAllViews()

        if (texto.trim().length < 2) return

        val resultados = db.buscarTareas(texto)

        if (resultados.isEmpty()) {
            contenedorResultados.addView(TextView(this).apply {
                text = "No se encontraron tareas. Puedes crearla al pulsar Iniciar."
                setPadding(4, 8, 4, 8)
            })
            return
        }

        resultados.forEach { tarea ->
            contenedorResultados.addView(Button(this).apply {
                text = "${tarea.second} · ${tarea.third}"

                setOnClickListener {
                    // Guardamos primero la selección.
                    tareaSeleccionadaId = tarea.first
                    nombreTareaSeleccionada = tarea.second
                    categoriaSeleccionada = tarea.third

                    // Evitamos que setText borre la selección.
                    actualizandoBusqueda = true
                    cajaBusqueda.setText(tarea.second)
                    cajaBusqueda.setSelection(cajaBusqueda.text.length)
                    actualizandoBusqueda = false

                    textoCategoria.text = "Categoría: ${tarea.third}"
                    contenedorResultados.removeAllViews()
                }
            })
        }
    }

    // INICIAR UNA TAREA EXISTENTE O CREAR UNA NUEVA
    private fun iniciarSeleccion() {
        if (db.obtenerEventoActivo() != null) {
            Toast.makeText(
                this,
                "Ya tienes un evento en curso. Finalízalo primero.",
                Toast.LENGTH_LONG
            ).show()
            actualizarEstado()
            return
        }

        val nombre = (
                nombreTareaSeleccionada
                    ?: cajaBusqueda.text.toString().trim()
                ).trim()

        if (nombre.isBlank()) {
            cajaBusqueda.error = "Escribe el nombre de una tarea"
            return
        }

        val idExistente = tareaSeleccionadaId

        if (idExistente != null) {
            comenzarEvento(idExistente)
        } else {
            // Comprueba si el texto coincide exactamente con una tarea.
            val existente = db.obtenerTareas().firstOrNull {
                it.second.equals(nombre, ignoreCase = true)
            }

            if (existente != null) {
                tareaSeleccionadaId = existente.first
                nombreTareaSeleccionada = existente.second
                categoriaSeleccionada = existente.third
                textoCategoria.text = "Categoría: ${existente.third}"
                comenzarEvento(existente.first)
            } else {
                crearTareaYComenzar(nombre)
            }
        }
    }

    // CREAR TAREA SIN SALIR DE EVENTOS
    private fun crearTareaYComenzar(nombre: String) {
        val categorias = db.obtenerCategorias()

        if (categorias.isEmpty()) {
            Toast.makeText(
                this,
                "Primero debes registrar una categoría",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val spinner = Spinner(this)
        spinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            categorias.map { it.second }
        ).also {
            it.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
            )
        }

        val contenido = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 8, 24, 8)

            addView(TextView(this@EventosActivity).apply {
                text = "Tarea nueva: $nombre"
                textSize = 16f
            })

            addView(TextView(this@EventosActivity).apply {
                text = "Selecciona la categoría:"
                setPadding(0, 12, 0, 4)
            })

            addView(spinner)
        }

        AlertDialog.Builder(this)
            .setTitle("Crear tarea")
            .setView(contenido)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Crear e iniciar") { _, _ ->

                val categoriaId =
                    categorias[spinner.selectedItemPosition].first

                val guardada = db.agregarTarea(nombre, categoriaId)

                if (guardada) {
                    val nuevaTarea = db.obtenerTareas().firstOrNull {
                        it.second.equals(nombre, ignoreCase = true)
                    }

                    if (nuevaTarea != null) {
                        tareaSeleccionadaId = nuevaTarea.first
                        nombreTareaSeleccionada = nuevaTarea.second
                        categoriaSeleccionada = nuevaTarea.third

                        actualizandoBusqueda = true
                        cajaBusqueda.setText(nuevaTarea.second)
                        cajaBusqueda.setSelection(cajaBusqueda.text.length)
                        actualizandoBusqueda = false

                        textoCategoria.text =
                            "Categoría: ${nuevaTarea.third}"

                        comenzarEvento(nuevaTarea.first)
                    } else {
                        Toast.makeText(
                            this,
                            "La tarea se guardó, pero no se pudo recuperar.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } else {
                    Toast.makeText(
                        this,
                        "No se pudo crear la tarea. Comprueba si el nombre ya existe.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            .show()
    }

    // REGISTRAR EL INICIO CON LA FECHA Y HORA DEL CELULAR
    private fun comenzarEvento(tareaId: Int) {
        if (db.obtenerEventoActivo() != null) {
            Toast.makeText(
                this,
                "Ya existe un evento en curso.",
                Toast.LENGTH_SHORT
            ).show()
            actualizarEstado()
            return
        }

        val inicio = LocalDateTime.now().toString()
        val id = db.iniciarEvento(tareaId, inicio)

        if (id != -1L) {
            Toast.makeText(
                this,
                "Evento iniciado",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            Toast.makeText(
                this,
                "No se pudo iniciar el evento. Comprueba si hay otro activo.",
                Toast.LENGTH_LONG
            ).show()
        }

        actualizarEstado()
        cargarEventos()
    }

    // REGISTRAR EL FIN Y CALCULAR LA DURACIÓN
    private fun finalizarActual() {
        val activo = db.obtenerEventoActivo()

        if (activo == null) {
            Toast.makeText(
                this,
                "No hay ningún evento en curso.",
                Toast.LENGTH_SHORT
            ).show()
            actualizarEstado()
            return
        }

        try {
            val inicio = LocalDateTime.parse(activo.third)
            val fin = LocalDateTime.now()
            val segundos = Duration.between(inicio, fin).seconds

            val correcto = db.finalizarEvento(
                eventoId = activo.first,
                fin = fin.toString(),
                duracionSegundos = segundos
            )

            if (correcto) {
                Toast.makeText(
                    this,
                    "Evento finalizado. Duración: ${formatearDuracion(segundos)}",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                Toast.makeText(
                    this,
                    "No se pudo finalizar el evento.",
                    Toast.LENGTH_LONG
                ).show()
            }
        } catch (_: Exception) {
            Toast.makeText(
                this,
                "No se pudo calcular la duración del evento.",
                Toast.LENGTH_LONG
            ).show()
        }

        actualizarEstado()
        cargarEventos()
        actualizarResumenSemanal()
    }

    // MOSTRAR EL EVENTO ACTIVO Y HABILITAR LOS BOTONES
    private fun actualizarEstado() {
        val activo = db.obtenerEventoActivo()

        if (activo == null) {
            textoEstado.text = "No hay eventos en curso."
            btnIniciar.isEnabled = true
            btnFinalizar.isEnabled = false
        } else {
            val tarea = db.obtenerTareas().firstOrNull {
                it.first == activo.second
            }

            val nombre = tarea?.second ?: "Tarea ${activo.second}"
            val categoria = tarea?.third ?: "Categoría no disponible"

            val inicio = try {
                LocalDateTime.parse(activo.third).format(formato)
            } catch (_: Exception) {
                activo.third
            }

            textoEstado.text =
                "EN CURSO\nTarea: $nombre · $categoria\nInicio: $inicio"

            btnIniciar.isEnabled = false
            btnFinalizar.isEnabled = true
        }
    }

    // HISTORIAL DE EVENTOS
    private fun cargarEventos() {
        contenedorEventos.removeAllViews()

        val eventos = db.obtenerEventos()

        if (eventos.isEmpty()) {
            contenedorEventos.addView(TextView(this).apply {
                text = "Todavía no hay eventos registrados."
                setPadding(4, 8, 4, 8)
            })
            return
        }

        eventos.forEach { evento ->
            val inicio = mostrarFecha(evento.inicio)
            val fin = evento.fin?.let { mostrarFecha(it) } ?: "En curso"

            val duracion = evento.duracionSegundos?.let {
                formatearDuracion(it)
            } ?: "En curso"

            contenedorEventos.addView(TextView(this).apply {
                text = """
                    ${evento.nombreTarea} · ${evento.nombreCategoria}
                    Inicio: $inicio
                    Fin: $fin
                    Duración: $duracion
                """.trimIndent()

                textSize = 14f
                setPadding(12, 12, 12, 12)
                setBackgroundColor(0xFFFFFFFF.toInt())
            })
        }
    }

    private fun mostrarFecha(fecha: String): String {
        return try {
            LocalDateTime.parse(fecha).format(formato)
        } catch (_: Exception) {
            fecha
        }
    }

    private fun formatearDuracion(segundos: Long): String {
        val horas = segundos / 3600
        val minutos = (segundos % 3600) / 60
        val seg = segundos % 60

        return "${horas}h ${minutos}m ${seg}s"
    }



    private fun actualizarResumenSemanal() {
        if (!::contenedorResumenSemanal.isInitialized) return

        contenedorResumenSemanal.removeAllViews()

        // La semana comienza el lunes a las 00:00.
        val hoy = LocalDate.now()
        val lunes = hoy.with(DayOfWeek.MONDAY)
        val proximoLunes = lunes.plusWeeks(1)

        val formatoFecha = DateTimeFormatter.ofPattern("dd/MM/yyyy")

        textoSemana.text =
            "Semana ${lunes.format(formatoFecha)} – " +
                    "${proximoLunes.minusDays(1).format(formatoFecha)}"

        val inicioSemana = "${lunes}T00:00:00"
        val inicioProximaSemana = "${proximoLunes}T00:00:00"

        // La consulta SQL agrupa los eventos finalizados por categoría.
        val resumen = db.obtenerResumenSemanal(
            inicioSemana,
            inicioProximaSemana
        )

        val segundosSemana = 7L * 24 * 60 * 60
        val totalRegistrado = resumen.sumOf { it.totalSegundos }
        val segundosSinRegistrar =
            (segundosSemana - totalRegistrado).coerceAtLeast(0L)

        resumen.forEach { categoria ->
            agregarFilaResumen(
                nombre = categoria.nombreCategoria,
                segundos = categoria.totalSegundos,
                totalSemana = segundosSemana
            )
        }

        agregarFilaResumen(
            nombre = "Sin registrar",
            segundos = segundosSinRegistrar,
            totalSemana = segundosSemana
        )
    }

    private fun agregarFilaResumen(
        nombre: String,
        segundos: Long,
        totalSemana: Long
    ) {
        val porcentaje =
            segundos.toDouble() / totalSemana.toDouble() * 100.0

        val horas = segundos / 3600
        val minutos = (segundos % 3600) / 60

        val fila = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 8, 0, 8)
        }

        fila.addView(TextView(this).apply {
            text = nombre
            textSize = 16f
        })

        fila.addView(TextView(this).apply {
            text = "${horas} h ${minutos} min · " +
                    String.format(Locale.US, "%.1f", porcentaje) + " %"
            textSize = 14f
        })

        val barra = ProgressBar(
            this,
            null,
            android.R.attr.progressBarStyleHorizontal
        ).apply {
            max = 1000
            progress = (porcentaje * 10).toInt().coerceIn(0, 1000)
        }

        fila.addView(
            barra,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        contenedorResumenSemanal.addView(fila)
    }

}

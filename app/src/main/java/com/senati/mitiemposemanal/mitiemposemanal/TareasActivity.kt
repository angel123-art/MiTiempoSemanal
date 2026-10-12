
package com.senati.mitiemposemanal.mitiemposemanal

import android.os.Bundle
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class TareasActivity : AppCompatActivity() {

    private lateinit var db: DBHelper
    private lateinit var contenedor: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tareas)

        db = DBHelper(this)
        contenedor = findViewById(R.id.contenedorTareas)

        findViewById<Button>(R.id.btnNuevaTarea)
            .setOnClickListener { mostrarFormulario() }

        findViewById<Button>(R.id.btnVolver)
            .setOnClickListener { finish() }

        cargarTareas()
    }

    private fun cargarTareas() {
        contenedor.removeAllViews()

        val tareas = db.obtenerTareas()

        if (tareas.isEmpty()) {
            contenedor.addView(TextView(this).apply {
                text = "Todavía no tienes tareas registradas."
                textSize = 16f
                setPadding(8, 20, 8, 20)
            })
            return
        }

        tareas.forEach { (id, nombre, categoria) ->
            val fila = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(10, 12, 10, 12)
            }

            fila.addView(TextView(this).apply {
                text = "$nombre · $categoria"
                textSize = 18f
            })

            val acciones = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.END
            }

            acciones.addView(Button(this).apply {
                text = "Editar"
                setOnClickListener {
                    mostrarFormulario(id, nombre, categoria)
                }
            })

            acciones.addView(Button(this).apply {
                text = "Eliminar"
                setOnClickListener {
                    confirmarEliminacion(id, nombre)
                }
            })

            fila.addView(acciones)
            contenedor.addView(fila)

            contenedor.addView(android.view.View(this).apply {
                setBackgroundColor(0xFFDDE3EA.toInt())
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 1
                )
            })
        }
    }

    private fun mostrarFormulario(
        id: Int? = null,
        nombreActual: String = "",
        categoriaActual: String = ""
    ) {
        val categorias = db.obtenerCategorias()

        if (categorias.isEmpty()) {
            Toast.makeText(
                this, "Primero registra una categoría",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val formulario = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 8, 24, 8)
        }

        val entrada = EditText(this).apply {
            hint = "Nombre de la tarea"
            setText(nombreActual)
            setSingleLine(true)
        }

        val selector = Spinner(this)
        val nombres = categorias.map { it.second }

        selector.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            nombres
        )

        val indice = categorias.indexOfFirst {
            it.second.equals(categoriaActual, ignoreCase = true)
        }
        if (indice >= 0) selector.setSelection(indice)

        formulario.addView(entrada)
        formulario.addView(TextView(this).apply {
            text = "Selecciona una categoría"
            setPadding(0, 16, 0, 4)
        })
        formulario.addView(selector)

        val dialogo = AlertDialog.Builder(this)
            .setTitle(if (id == null) "Nueva tarea" else "Editar tarea")
            .setView(formulario)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Guardar", null)
            .create()

        dialogo.setOnShowListener {
            dialogo.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener {
                    val nombre = entrada.text.toString().trim()

                    if (nombre.isBlank()) {
                        entrada.error = "Escribe el nombre de la tarea"
                        return@setOnClickListener
                    }

                    val categoriaId = categorias[selector.selectedItemPosition].first

                    val guardado = if (id == null) {
                        db.agregarTarea(nombre, categoriaId)
                    } else {
                        db.actualizarTarea(id, nombre, categoriaId)
                    }

                    if (guardado) {
                        dialogo.dismiss()
                        cargarTareas()
                        Toast.makeText(
                            this, "Tarea guardada correctamente",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            this,
                            "No se guardó: el nombre puede estar duplicado",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
        }

        dialogo.show()
    }

    private fun confirmarEliminacion(id: Int, nombre: String) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar tarea")
            .setMessage("¿Deseas eliminar \"$nombre\"?")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Eliminar") { _, _ ->
                if (db.eliminarTarea(id)) {
                    cargarTareas()
                    Toast.makeText(
                        this, "Tarea eliminada",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .show()
    }
}

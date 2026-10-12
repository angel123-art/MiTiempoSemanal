
package com.senati.mitiemposemanal.mitiemposemanal

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var db: DBHelper
    private lateinit var contenedor: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        db = DBHelper(this)

        // Crear la pantalla principal
        val pantalla = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 32, 24, 24)
        }

        // Título
        pantalla.addView(TextView(this).apply {
            text = "MiTiempoSemanal"
            textSize = 28f
            gravity = Gravity.CENTER
        })

        // Nombre y versión
        pantalla.addView(TextView(this).apply {
            text = "Angel Espinoza · v0.1.0 (1)"
            textSize = 16f
            gravity = Gravity.CENTER
            setPadding(0, 12, 0, 24)
        })

        // Botón para registrar categorías
        pantalla.addView(Button(this).apply {
            text = "+ Nueva categoría"
            setOnClickListener {
                mostrarFormulario()
            }
        })

        // Botón para abrir la pantalla de tareas
        pantalla.addView(Button(this).apply {
            text = "Mis tareas"
            setOnClickListener {
                startActivity(
                    Intent(
                        this@MainActivity,
                        TareasActivity::class.java
                    )
                )
            }
        })

        // Título de la lista de categorías
        pantalla.addView(TextView(this).apply {
            text = "Mis categorías"
            textSize = 22f
            setPadding(0, 24, 0, 12)
        })

        // Contenedor de categorías
        contenedor = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        // Lista desplazable
        val scroll = ScrollView(this)
        scroll.addView(contenedor)

        pantalla.addView(
            scroll,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        setContentView(pantalla)
        cargarCategorias()
    }

    override fun onResume() {
        super.onResume()

        // Actualizar la lista al regresar a la pantalla principal
        if (::db.isInitialized && ::contenedor.isInitialized) {
            cargarCategorias()
        }
    }

    // LISTAR CATEGORÍAS
    private fun cargarCategorias() {
        contenedor.removeAllViews()

        db.obtenerCategorias().forEach { (id, nombre) ->

            val fila = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            fila.addView(TextView(this).apply {
                text = "📁 $nombre"
                textSize = 18f
                setPadding(8, 16, 8, 16)

                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
            })

            // Botón editar
            fila.addView(Button(this).apply {
                text = "Editar"
                setOnClickListener {
                    mostrarFormulario(id, nombre)
                }
            })

            // Botón eliminar
            fila.addView(Button(this).apply {
                text = "Eliminar"
                setOnClickListener {
                    confirmarEliminacion(id, nombre)
                }
            })

            contenedor.addView(fila)
        }
    }

    // REGISTRAR O EDITAR CATEGORÍAS
    private fun mostrarFormulario(
        id: Int? = null,
        nombreActual: String = ""
    ) {
        val entrada = EditText(this).apply {
            hint = "Nombre de la categoría"
            setText(nombreActual)
            isSingleLine = true
        }

        AlertDialog.Builder(this)
            .setTitle(
                if (id == null) "Nueva categoría"
                else "Editar categoría"
            )
            .setView(entrada)
            .setPositiveButton("Guardar") { _, _ ->

                val nombre = entrada.text.toString().trim()

                val guardado = if (id == null) {
                    db.agregarCategoria(nombre)
                } else {
                    db.actualizarCategoria(id, nombre)
                }

                if (guardado) {
                    cargarCategorias()

                    Toast.makeText(
                        this,
                        "Categoría guardada correctamente",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        this,
                        "Nombre vacío, duplicado o error al guardar",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // CONFIRMAR Y ELIMINAR CATEGORÍAS
    private fun confirmarEliminacion(
        id: Int,
        nombre: String
    ) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar categoría")
            .setMessage(
                "¿Deseas eliminar la categoría $nombre?"
            )
            .setPositiveButton("Eliminar") { _, _ ->

                if (db.eliminarCategoria(id)) {
                    cargarCategorias()

                    Toast.makeText(
                        this,
                        "Categoría eliminada",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        this,
                        "No se puede eliminar: tiene tareas asociadas",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}

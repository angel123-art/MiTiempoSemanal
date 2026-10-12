
package com.senati.mitiemposemanal.mitiemposemanal

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DBHelper(context: Context) :
    SQLiteOpenHelper(context, "MiTiempoSemanal.db", null, 1) {

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE categoria (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                nombre TEXT NOT NULL COLLATE NOCASE UNIQUE
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE tarea (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                nombre TEXT NOT NULL COLLATE NOCASE UNIQUE,
                categoria_id INTEGER NOT NULL,
                FOREIGN KEY(categoria_id)
                    REFERENCES categoria(id)
            )
        """.trimIndent())

        listOf("Estudio", "Trabajo", "Descanso", "Ocio").forEach {
                nombre ->
            val valores = ContentValues().apply {
                put("nombre", nombre)
            }
            db.insert("categoria", null, valores)
        }
    }

    override fun onUpgrade(
        db: SQLiteDatabase,
        oldVersion: Int,
        newVersion: Int
    ) {
        // Reservado para futuras actualizaciones de la base de datos.
    }

    // LISTAR CATEGORÍAS
    fun obtenerCategorias(): List<Pair<Int, String>> {
        val resultado = mutableListOf<Pair<Int, String>>()

        readableDatabase.rawQuery(
            "SELECT id, nombre FROM categoria ORDER BY nombre",
            null
        ).use { cursor ->
            while (cursor.moveToNext()) {
                resultado.add(
                    cursor.getInt(0) to cursor.getString(1)
                )
            }
        }

        return resultado
    }

    // REGISTRAR CATEGORÍA
    fun agregarCategoria(nombre: String): Boolean {
        if (nombre.isBlank()) return false

        val valores = ContentValues().apply {
            put("nombre", nombre.trim())
        }

        return writableDatabase.insert(
            "categoria", null, valores
        ) != -1L
    }

    // EDITAR CATEGORÍA
    fun actualizarCategoria(id: Int, nombre: String): Boolean {
        if (nombre.isBlank()) return false

        val valores = ContentValues().apply {
            put("nombre", nombre.trim())
        }

        return writableDatabase.update(
            "categoria", valores, "id = ?", arrayOf(id.toString())
        ) > 0
    }

    // ELIMINAR CATEGORÍA SI NO TIENE TAREAS
    fun eliminarCategoria(id: Int): Boolean {
        val db = writableDatabase

        val cantidad = db.rawQuery(
            "SELECT COUNT(*) FROM tarea WHERE categoria_id = ?",
            arrayOf(id.toString())
        ).use { cursor ->
            cursor.moveToFirst()
            cursor.getInt(0)
        }

        if (cantidad > 0) return false

        return db.delete(
            "categoria", "id = ?", arrayOf(id.toString())
        ) > 0
    }

    // LISTAR TAREAS CON SU CATEGORÍA
    fun obtenerTareas(): List<Triple<Int, String, String>> {
        val resultado = mutableListOf<Triple<Int, String, String>>()

        readableDatabase.rawQuery("""
            SELECT t.id, t.nombre, c.nombre
            FROM tarea t
            INNER JOIN categoria c ON c.id = t.categoria_id
            ORDER BY t.nombre
        """.trimIndent(), null).use { cursor ->
            while (cursor.moveToNext()) {
                resultado.add(
                    Triple(
                        cursor.getInt(0),
                        cursor.getString(1),
                        cursor.getString(2)
                    )
                )
            }
        }

        return resultado
    }

    // REGISTRAR TAREA
    fun agregarTarea(nombre: String, categoriaId: Int): Boolean {
        if (nombre.isBlank()) return false

        val valores = ContentValues().apply {
            put("nombre", nombre.trim())
            put("categoria_id", categoriaId)
        }

        return writableDatabase.insert(
            "tarea", null, valores
        ) != -1L
    }

    // EDITAR TAREA
    fun actualizarTarea(
        id: Int,
        nombre: String,
        categoriaId: Int
    ): Boolean {
        if (nombre.isBlank()) return false

        val valores = ContentValues().apply {
            put("nombre", nombre.trim())
            put("categoria_id", categoriaId)
        }

        return writableDatabase.update(
            "tarea", valores, "id = ?", arrayOf(id.toString())
        ) > 0
    }

    // ELIMINAR TAREA
    fun eliminarTarea(id: Int): Boolean {
        return writableDatabase.delete(
            "tarea", "id = ?", arrayOf(id.toString())
        ) > 0
    }
}

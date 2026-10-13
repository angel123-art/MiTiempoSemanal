package com.senati.mitiemposemanal.mitiemposemanal

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
class DBHelper(context: Context) :
    SQLiteOpenHelper(context, "MiTiempoSemanal.db", null, 2) {

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

        listOf("Estudio", "Trabajo", "Descanso", "Ocio").forEach { nombre ->
            val valores = ContentValues().apply {
                put("nombre", nombre)
            }
            db.insert("categoria", null, valores)
        }

        crearTablaEvento(db)
    }

    // TABLA DE EVENTOS: SPRINT 2
    private fun crearTablaEvento(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE evento (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                tarea_id INTEGER NOT NULL,
                inicio TEXT NOT NULL,
                fin TEXT,
                duracion_segundos INTEGER,
                FOREIGN KEY(tarea_id)
                    REFERENCES tarea(id)
            )
        """.trimIndent())

        // Impide que haya más de un evento sin finalizar.
        db.execSQL("""
            CREATE UNIQUE INDEX indice_un_evento_activo
            ON evento(fin)
            WHERE fin IS NULL
        """.trimIndent())
    }

    // ACTUALIZACIÓN SIN ELIMINAR LOS DATOS DEL SPRINT 1
    override fun onUpgrade(
        db: SQLiteDatabase,
        oldVersion: Int,
        newVersion: Int
    ) {
        if (oldVersion < 2) {
            crearTablaEvento(db)
        }
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
            "categoria",
            valores,
            "id = ?",
            arrayOf(id.toString())
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
            "categoria",
            "id = ?",
            arrayOf(id.toString())
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
            "tarea",
            valores,
            "id = ?",
            arrayOf(id.toString())
        ) > 0
    }

    // ELIMINAR TAREA
    fun eliminarTarea(id: Int): Boolean {
        return writableDatabase.delete(
            "tarea",
            "id = ?",
            arrayOf(id.toString())
        ) > 0
    }

    // BUSCAR TAREAS POR NOMBRE: SPRINT 2
    fun buscarTareas(texto: String): List<Triple<Int, String, String>> {
        val resultado = mutableListOf<Triple<Int, String, String>>()

        if (texto.trim().length < 2) return resultado

        readableDatabase.rawQuery("""
            SELECT t.id, t.nombre, c.nombre
            FROM tarea t
            INNER JOIN categoria c ON c.id = t.categoria_id
            WHERE t.nombre LIKE '%' || ? || '%' COLLATE NOCASE
            ORDER BY t.nombre
        """.trimIndent(), arrayOf(texto.trim())).use { cursor ->

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

    // INICIAR EVENTO
    // inicio debe contener la fecha y hora completas del celular.
    fun iniciarEvento(tareaId: Int, inicio: String): Long {
        val valores = ContentValues().apply {
            put("tarea_id", tareaId)
            put("inicio", inicio)
            putNull("fin")
            putNull("duracion_segundos")
        }

        return writableDatabase.insert("evento", null, valores)
    }

    // OBTENER EL EVENTO ACTIVO
    fun obtenerEventoActivo(): Triple<Int, Int, String>? {
        readableDatabase.rawQuery("""
            SELECT id, tarea_id, inicio
            FROM evento
            WHERE fin IS NULL
            ORDER BY id DESC
            LIMIT 1
        """.trimIndent(), null).use { cursor ->

            if (cursor.moveToFirst()) {
                return Triple(
                    cursor.getInt(0),
                    cursor.getInt(1),
                    cursor.getString(2)
                )
            }
        }

        return null
    }

    // FINALIZAR EVENTO Y GUARDAR SU DURACIÓN
    fun finalizarEvento(
        eventoId: Int,
        fin: String,
        duracionSegundos: Long
    ): Boolean {
        if (duracionSegundos < 0) return false

        val valores = ContentValues().apply {
            put("fin", fin)
            put("duracion_segundos", duracionSegundos)
        }

        return writableDatabase.update(
            "evento",
            valores,
            "id = ? AND fin IS NULL",
            arrayOf(eventoId.toString())
        ) > 0
    }

    // LISTAR EVENTOS CON SU TAREA Y CATEGORÍA
    fun obtenerEventos(): List<EventoRegistro> {
        val resultado = mutableListOf<EventoRegistro>()

        readableDatabase.rawQuery("""
            SELECT e.id, e.tarea_id, t.nombre, c.nombre,
                   e.inicio, e.fin, e.duracion_segundos
            FROM evento e
            INNER JOIN tarea t ON t.id = e.tarea_id
            INNER JOIN categoria c ON c.id = t.categoria_id
            ORDER BY e.id DESC
        """.trimIndent(), null).use { cursor ->

            while (cursor.moveToNext()) {
                resultado.add(
                    EventoRegistro(
                        id = cursor.getInt(0),
                        tareaId = cursor.getInt(1),
                        nombreTarea = cursor.getString(2),
                        nombreCategoria = cursor.getString(3),
                        inicio = cursor.getString(4),
                        fin = if (cursor.isNull(5)) null else cursor.getString(5),
                        duracionSegundos = if (cursor.isNull(6)) {
                            null
                        } else {
                            cursor.getLong(6)
                        }
                    )
                )
            }
        }

        return resultado
    }
    fun obtenerResumenSemanal(
        inicioSemana: String,
        inicioProximaSemana: String
    ): List<ResumenCategoria> {

        val resultado = mutableListOf<ResumenCategoria>()

        readableDatabase.rawQuery(
            """
            SELECT
                c.id,
                c.nombre,
                COALESCE(SUM(e.duracion_segundos), 0) AS total_segundos
            FROM categoria c
            LEFT JOIN tarea t
                ON t.categoria_id = c.id
            LEFT JOIN evento e
                ON e.tarea_id = t.id
                AND e.fin IS NOT NULL
                AND e.inicio >= ?
                AND e.inicio < ?
            GROUP BY c.id, c.nombre
            ORDER BY c.nombre
            """.trimIndent(),
            arrayOf(inicioSemana, inicioProximaSemana)
        ).use { cursor ->

            while (cursor.moveToNext()) {
                resultado.add(
                    ResumenCategoria(
                        categoriaId = cursor.getInt(0),
                        nombreCategoria = cursor.getString(1),
                        totalSegundos = cursor.getLong(2)
                    )
                )
            }
        }

        return resultado
    }
}

// MODELO PARA MOSTRAR LOS EVENTOS
data class EventoRegistro(
    val id: Int,
    val tareaId: Int,
    val nombreTarea: String,
    val nombreCategoria: String,
    val inicio: String,
    val fin: String?,
    val duracionSegundos: Long?
)

data class ResumenCategoria(
    val categoriaId: Int,
    val nombreCategoria: String,
    val totalSegundos: Long
)


package com.senati.mitiemposemanal.mitiemposemanal

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
class DBHelper(context: Context) :
    SQLiteOpenHelper(context, "MiTiempoSemanal.db", null, 3) {

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
        crearTablasReportes(db)
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
    private fun crearTablasReportes(db: SQLiteDatabase) {
        db.execSQL("""
        CREATE TABLE IF NOT EXISTS reporte_semanal (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            inicio_semana TEXT NOT NULL,
            fin_semana TEXT NOT NULL,
            UNIQUE(inicio_semana, fin_semana)
        )
    """.trimIndent())

        db.execSQL("""
        CREATE TABLE IF NOT EXISTS reporte_categoria (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            reporte_id INTEGER NOT NULL,
            categoria_id INTEGER NOT NULL,
            nombre_categoria TEXT NOT NULL,
            segundos INTEGER NOT NULL DEFAULT 0,
            porcentaje REAL NOT NULL DEFAULT 0,
            UNIQUE(reporte_id, categoria_id),
            FOREIGN KEY(reporte_id)
                REFERENCES reporte_semanal(id),
            FOREIGN KEY(categoria_id)
                REFERENCES categoria(id)
        )
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

        if (oldVersion < 3) {
            crearTablasReportes(db)
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
    // SPRINT 4: CERRAR SEMANAS PENDIENTES AUTOMÁTICAMENTE
    fun cerrarSemanasPendientes(fechaActual: java.time.LocalDate) {
        val db = writableDatabase
        val hoyLunes = fechaActual.with(
            java.time.DayOfWeek.MONDAY
        )

        // Buscar la primera fecha de un evento registrado.
        val primeraFecha = db.rawQuery(
            "SELECT MIN(inicio) FROM evento",
            null
        ).use { cursor ->
            if (cursor.moveToFirst() && !cursor.isNull(0)) {
                cursor.getString(0).substring(0, 10)
            } else {
                null
            }
        } ?: return

        var lunes = try {
            java.time.LocalDate.parse(primeraFecha)
                .with(java.time.DayOfWeek.MONDAY)
        } catch (_: Exception) {
            return
        }

        // No generar el reporte de la semana actual.
        while (lunes.isBefore(hoyLunes)) {
            val siguienteLunes = lunes.plusWeeks(1)
            val inicio = "${lunes}T00:00:00"
            val fin = "${siguienteLunes}T00:00:00"

            val tieneEventos = db.rawQuery(
                """
                SELECT COUNT(*)
                FROM evento
                WHERE inicio >= ? AND inicio < ?
                  AND fin IS NOT NULL
                """.trimIndent(),
                arrayOf(inicio, fin)
            ).use { cursor ->
                cursor.moveToFirst()
                cursor.getInt(0) > 0
            }

            if (tieneEventos) {
                guardarReporteSemanal(lunes, siguienteLunes.minusDays(1))
            }

            lunes = siguienteLunes
        }
    }

    // GUARDAR EL REPORTE DE UNA SEMANA
    private fun guardarReporteSemanal(
        lunes: java.time.LocalDate,
        domingo: java.time.LocalDate
    ) {
        val db = writableDatabase
        val inicio = "${lunes}T00:00:00"
        val finExclusivo = "${domingo.plusDays(1)}T00:00:00"

        db.beginTransaction()
        try {
            // UNIQUE evita duplicar una semana.
            val valoresReporte = ContentValues().apply {
                put("inicio_semana", lunes.toString())
                put("fin_semana", domingo.toString())
            }

            val reporteId = db.insertWithOnConflict(
                "reporte_semanal",
                null,
                valoresReporte,
                SQLiteDatabase.CONFLICT_IGNORE
            )

            // Si ya existía, no volver a generarlo.
            if (reporteId == -1L) {
                db.setTransactionSuccessful()
                return
            }

            // Resumen calculado con la consulta SQL del Sprint 3.
            val resumen = obtenerResumenSemanal(inicio, finExclusivo)
            val totalSemana = 7.0 * 24 * 60 * 60

            resumen.forEach { categoria ->
                val valores = ContentValues().apply {
                    put("reporte_id", reporteId)
                    put("categoria_id", categoria.categoriaId)
                    put("nombre_categoria", categoria.nombreCategoria)
                    put("segundos", categoria.totalSegundos)
                    put(
                        "porcentaje",
                        categoria.totalSegundos.toDouble() /
                                totalSemana * 100.0
                    )
                }

                db.insertOrThrow(
                    "reporte_categoria",
                    null,
                    valores
                )
            }

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    // LISTAR LAS SEMANAS QUE YA TIENEN REPORTE
    fun obtenerReportesSemanales(): List<ReporteSemanal> {
        val resultado = mutableListOf<ReporteSemanal>()

        readableDatabase.rawQuery(
            """
            SELECT id, inicio_semana, fin_semana
            FROM reporte_semanal
            ORDER BY inicio_semana DESC
            """.trimIndent(),
            null
        ).use { cursor ->
            while (cursor.moveToNext()) {
                resultado.add(
                    ReporteSemanal(
                        id = cursor.getLong(0),
                        inicioSemana = cursor.getString(1),
                        finSemana = cursor.getString(2)
                    )
                )
            }
        }

        return resultado
    }

    // CONSULTAR LAS CATEGORÍAS GUARDADAS EN UN REPORTE
    fun obtenerCategoriasReporte(
        reporteId: Long
    ): List<ResumenCategoriaReporte> {
        val resultado = mutableListOf<ResumenCategoriaReporte>()

        readableDatabase.rawQuery(
            """
            SELECT categoria_id, nombre_categoria, segundos, porcentaje
            FROM reporte_categoria
            WHERE reporte_id = ?
            ORDER BY nombre_categoria
            """.trimIndent(),
            arrayOf(reporteId.toString())
        ).use { cursor ->
            while (cursor.moveToNext()) {
                resultado.add(
                    ResumenCategoriaReporte(
                        categoriaId = cursor.getInt(0),
                        nombreCategoria = cursor.getString(1),
                        totalSegundos = cursor.getLong(2),
                        porcentaje = cursor.getDouble(3)
                    )
                )
            }
        }

        return resultado
    }

    // CONSULTAR EVENTOS DE UNA SEMANA CERRADA
    fun obtenerEventosReporte(
        inicioSemana: String,
        finSemana: String
    ): List<EventoRegistro> {
        val resultado = mutableListOf<EventoRegistro>()

        readableDatabase.rawQuery(
            """
            SELECT e.id, e.tarea_id, t.nombre, c.nombre,
                   e.inicio, e.fin, e.duracion_segundos
            FROM evento e
            INNER JOIN tarea t ON t.id = e.tarea_id
            INNER JOIN categoria c ON c.id = t.categoria_id
            WHERE e.inicio >= ?
              AND e.inicio < ?
              AND e.fin IS NOT NULL
            ORDER BY e.inicio
            """.trimIndent(),
            arrayOf(
                "${inicioSemana}T00:00:00",
                "${java.time.LocalDate.parse(finSemana).plusDays(1)}T00:00:00"
            )
        ).use { cursor ->
            while (cursor.moveToNext()) {
                resultado.add(
                    EventoRegistro(
                        id = cursor.getInt(0),
                        tareaId = cursor.getInt(1),
                        nombreTarea = cursor.getString(2),
                        nombreCategoria = cursor.getString(3),
                        inicio = cursor.getString(4),
                        fin = cursor.getString(5),
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

data class ReporteSemanal(
    val id: Long,
    val inicioSemana: String,
    val finSemana: String
)

data class ResumenCategoriaReporte(
    val categoriaId: Int,
    val nombreCategoria: String,
    val totalSegundos: Long,
    val porcentaje: Double
)
package com.example.mitrivia

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray

data class Pregunta(
    val categoria: String,
    val dificultad: Int,
    val texto: String,
    val opciones: List<String>,
    val correcta: Int
)

class MainActivity : AppCompatActivity() {

    private val categorias = listOf("Videojuegos", "Fútbol", "Cine y series", "Ciencia y naturaleza")
    private val nombresDif = listOf("Fácil", "Medio", "Difícil")
    private val tiempoPorPregunta = 15

    private var todas: List<Pregunta> = emptyList()
    private var catElegida = "Mezcla"
    private var difElegida = 0
    private var ronda: List<Pregunta> = emptyList()
    private var indice = 0
    private var puntos = 0
    private var vidas = 3
    private var racha = 0
    private var cincuenta = 2
    private var saltos = 1
    private var respondida = false
    private val handler = Handler(Looper.getMainLooper())
    private var timer: CountDownTimer? = null

    private lateinit var contenedor: LinearLayout
    private lateinit var tiempoView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        todas = cargarPreguntas()

        contenedor = LinearLayout(this)
        contenedor.orientation = LinearLayout.VERTICAL
        contenedor.setPadding(48, 48, 48, 48)

        val scroll = ScrollView(this)
        scroll.addView(contenedor)
        setContentView(scroll)

        mostrarCategorias()
    }

    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel()
        handler.removeCallbacksAndMessages(null)
    }

    private fun cargarPreguntas(): List<Pregunta> {
        val lista = ArrayList<Pregunta>()
        val archivos: Array<String> = assets.list("") ?: emptyArray()
        for (nombre in archivos) {
            if (nombre.startsWith("preguntas") && nombre.endsWith(".json")) {
                try {
                    leerArchivo(nombre, lista)
                } catch (e: Exception) {
                    // si un archivo está mal armado, se saltea y el resto sigue andando
                }
            }
        }
        return lista
    }

    private fun leerArchivo(nombre: String, lista: MutableList<Pregunta>) {
        val json = assets.open(nombre).bufferedReader().use { it.readText() }
        val arr = JSONArray(json)
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val ops = o.getJSONArray("opciones")
            val opciones = ArrayList<String>()
            for (j in 0 until ops.length()) {
                opciones.add(ops.getString(j))
            }
            lista.add(
                Pregunta(
                    o.getString("categoria"),
                    o.getInt("dificultad"),
                    o.getString("pregunta"),
                    opciones,
                    o.getInt("correcta")
                )
            )
        }
    }

    private fun prefs() = getSharedPreferences("trivia", Context.MODE_PRIVATE)

    private fun record(dif: Int): Int = prefs().getInt("record_${catElegida}_$dif", 0)

    private fun texto(s: String, size: Float, margenArriba: Int = 0): TextView {
        val t = TextView(this)
        t.text = s
        t.textSize = size
        t.setPadding(0, margenArriba, 0, 0)
        contenedor.addView(t)
        return t
    }

    private fun boton(s: String, alClick: () -> Unit): Button {
        val b = Button(this)
        b.text = s
        b.setOnClickListener { alClick() }
        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        lp.topMargin = 16
        b.layoutParams = lp
        contenedor.addView(b)
        return b
    }

    private fun mostrarCategorias() {
        timer?.cancel()
        contenedor.removeAllViews()
        texto("Mi Trivia", 30f)
        texto(
            "Tenés 3 vidas, $tiempoPorPregunta segundos por pregunta y comodines. " +
                "Con 3 aciertos seguidos ganás 1 punto extra en cada acierto.",
            14f, 8
        )
        texto("Elegí una categoría", 18f, 24)
        for (c in categorias) {
            boton(c) {
                catElegida = c
                mostrarDificultad()
            }
        }
        boton("Mezcla (todas)") {
            catElegida = "Mezcla"
            mostrarDificultad()
        }
    }

    private fun mostrarDificultad() {
        contenedor.removeAllViews()
        texto(catElegida, 28f)
        texto("Elegí la dificultad", 18f, 24)
        for (d in 1..3) {
            boton("${nombresDif[d - 1]}  ·  vale $d  ·  récord ${record(d)}") {
                difElegida = d
                empezar()
            }
        }
        boton("Todas  ·  récord ${record(0)}") {
            difElegida = 0
            empezar()
        }
        boton("Volver") { mostrarCategorias() }
    }

    private fun empezar() {
        var f = todas
        if (catElegida != "Mezcla") {
            f = f.filter { it.categoria == catElegida }
        }
        if (difElegida != 0) {
            f = f.filter { it.dificultad == difElegida }
        }
        ronda = f.shuffled()
        indice = 0
        puntos = 0
        vidas = 3
        racha = 0
        cincuenta = 2
        saltos = 1
        mostrarPregunta()
    }

    private fun mostrarPregunta() {
        timer?.cancel()
        if (indice >= ronda.size || vidas <= 0) {
            mostrarFin()
            return
        }
        respondida = false
        val p = ronda[indice]
        contenedor.removeAllViews()

        texto("Vidas: " + "♥".repeat(vidas) + "   Puntos: $puntos   Racha: $racha", 18f)
        texto("${p.categoria} · ${nombresDif[p.dificultad - 1]} · ${indice + 1} de ${ronda.size}", 14f, 8)
        tiempoView = texto("Tiempo: $tiempoPorPregunta", 18f, 8)
        texto(p.texto, 24f, 32)

        val correctaTexto = p.opciones[p.correcta]
        val botones = ArrayList<Button>()
        for (op in p.opciones.shuffled()) {
            val b = boton(op) { }
            b.setBackgroundColor(Color.LTGRAY)
            b.setTextColor(Color.BLACK)
            b.setOnClickListener { responder(b, op == correctaTexto, p, botones, correctaTexto) }
            botones.add(b)
        }

        var usado50 = false
        val b50 = boton("50/50  (quedan $cincuenta)") { }
        b50.setOnClickListener {
            if (respondida || cincuenta <= 0 || usado50) return@setOnClickListener
            usado50 = true
            cincuenta--
            b50.text = "50/50  (quedan $cincuenta)"
            var ocultar = 2
            for (b in botones.shuffled()) {
                if (ocultar > 0 && b.text.toString() != correctaTexto) {
                    b.visibility = View.INVISIBLE
                    ocultar--
                }
            }
        }

        val bSaltar = boton("Saltar pregunta  (quedan $saltos)") { }
        bSaltar.setOnClickListener {
            if (respondida || saltos <= 0) return@setOnClickListener
            saltos--
            respondida = true
            indice++
            mostrarPregunta()
        }

        iniciarTimer(p, botones, correctaTexto)
    }

    private fun iniciarTimer(p: Pregunta, botones: List<Button>, correctaTexto: String) {
        timer?.cancel()
        timer = object : CountDownTimer(tiempoPorPregunta * 1000L, 1000L) {
            override fun onTick(ms: Long) {
                tiempoView.text = "Tiempo: ${(ms + 999) / 1000}"
            }

            override fun onFinish() {
                tiempoView.text = "Tiempo: 0"
                responder(null, false, p, botones, correctaTexto)
            }
        }
        timer?.start()
    }

    private fun responder(
        elegido: Button?,
        acierto: Boolean,
        p: Pregunta,
        botones: List<Button>,
        correctaTexto: String
    ) {
        if (respondida) return
        respondida = true
        timer?.cancel()
        if (acierto) {
            racha++
            val bonus = if (racha >= 3) 1 else 0
            puntos += p.dificultad + bonus
            elegido?.setBackgroundColor(Color.GREEN)
        } else {
            racha = 0
            vidas--
            elegido?.setBackgroundColor(Color.RED)
            for (b in botones) {
                if (b.text.toString() == correctaTexto) {
                    b.visibility = View.VISIBLE
                    b.setBackgroundColor(Color.GREEN)
                }
            }
        }
        handler.postDelayed({
            indice++
            mostrarPregunta()
        }, 1200)
    }

    private fun mostrarFin() {
        contenedor.removeAllViews()
        val clave = "record_${catElegida}_$difElegida"
        val anterior = prefs().getInt(clave, 0)
        val nuevo = puntos > anterior
        if (nuevo) {
            prefs().edit().putInt(clave, puntos).apply()
        }
        texto(if (vidas <= 0) "Te quedaste sin vidas" else "¡Terminaste las preguntas!", 26f)
        texto("Puntos: $puntos", 22f, 24)
        texto(if (nuevo) "¡Nuevo récord!" else "Tu récord: $anterior", 18f, 8)
        boton("Jugar de nuevo") { empezar() }
        boton("Elegir otra categoría") { mostrarCategorias() }
    }
}

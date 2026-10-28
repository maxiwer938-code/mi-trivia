package com.example.mitrivia

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

data class Pregunta(val texto: String, val opciones: List<String>, val correcta: Int)

class MainActivity : AppCompatActivity() {

    private val todas = listOf(
        Pregunta("¿Cuál es la capital de Argentina?", listOf("Córdoba", "Buenos Aires", "Rosario", "Mendoza"), 1),
        Pregunta("¿Cuántos planetas tiene el sistema solar?", listOf("7", "8", "9", "10"), 1),
        Pregunta("¿Quién pintó la Mona Lisa?", listOf("Picasso", "Van Gogh", "Leonardo da Vinci", "Dalí"), 2),
        Pregunta("¿Cuál es el océano más grande?", listOf("Atlántico", "Índico", "Ártico", "Pacífico"), 3),
        Pregunta("¿Cuántos jugadores juegan por equipo en la cancha en fútbol?", listOf("9", "10", "11", "12"), 2),
        Pregunta("¿En qué año ganó Argentina su tercer Mundial?", listOf("2014", "2018", "2022", "2010"), 2),
        Pregunta("¿Cuál es el animal terrestre más grande?", listOf("Elefante africano", "Jirafa", "Rinoceronte", "Oso"), 0),
        Pregunta("¿Cuántos colores tiene el arcoíris?", listOf("5", "6", "7", "8"), 2),
        Pregunta("¿Qué gas necesitamos respirar para vivir?", listOf("Nitrógeno", "Oxígeno", "Helio", "Hidrógeno"), 1),
        Pregunta("¿Cuál es el país más grande del mundo?", listOf("China", "Canadá", "Estados Unidos", "Rusia"), 3)
    )

    private var lista = todas.shuffled()
    private var indice = 0
    private var puntos = 0
    private var respondida = false

    private lateinit var titulo: TextView
    private lateinit var pregunta: TextView
    private lateinit var botones: List<Button>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(48, 48, 48, 48)

        titulo = TextView(this)
        titulo.textSize = 18f
        pregunta = TextView(this)
        pregunta.textSize = 26f
        pregunta.setPadding(0, 48, 0, 48)

        layout.addView(titulo)
        layout.addView(pregunta)

        botones = List(4) { i ->
            val b = Button(this)
            b.setOnClickListener { responder(i) }
            layout.addView(b)
            b
        }

        setContentView(layout)
        mostrar()
    }

    private fun mostrar() {
        respondida = false
        botones.forEach {
            it.setBackgroundColor(Color.LTGRAY)
            it.setTextColor(Color.BLACK)
            it.visibility = View.VISIBLE
        }
        if (indice >= lista.size) {
            titulo.text = "Fin del juego"
            pregunta.text = "Tu puntaje: $puntos de ${lista.size}"
            botones[0].text = "Jugar de nuevo"
            for (i in 1..3) botones[i].visibility = View.GONE
            return
        }
        val p = lista[indice]
        titulo.text = "Pregunta ${indice + 1} de ${lista.size}  |  Puntos: $puntos"
        pregunta.text = p.texto
        for (i in 0..3) botones[i].text = p.opciones[i]
    }

    private fun responder(i: Int) {
        if (indice >= lista.size) {
            lista = todas.shuffled()
            indice = 0
            puntos = 0
            mostrar()
            return
        }
        if (respondida) return
        respondida = true
        val p = lista[indice]
        if (i == p.correcta) {
            puntos++
            botones[i].setBackgroundColor(Color.GREEN)
        } else {
            botones[i].setBackgroundColor(Color.RED)
            botones[p.correcta].setBackgroundColor(Color.GREEN)
        }
        titulo.text = "Pregunta ${indice + 1} de ${lista.size}  |  Puntos: $puntos"
        Handler(Looper.getMainLooper()).postDelayed({
            indice++
            mostrar()
        }, 1000)
    }
}

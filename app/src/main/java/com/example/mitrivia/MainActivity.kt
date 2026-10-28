package com.example.mitrivia

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

data class Pregunta(
    val texto: String, 
    val opciones: List<String>, 
    val correcta: Int, 
    val imagenNombre: String? = null // Opcional: nombre del archivo en res/drawable sin la extensión
)

class MainActivity : AppCompatActivity() {

    private val todas = listOf(
        // Preguntas (pulpas con o sin imagen. Si querés imagen, le pasás el nombre del archivo al final, ej: "obelisco")
        Pregunta("¿Cuál es la capital de Argentina?", listOf("Córdoba", "Buenos Aires", "Rosario", "Mendoza"), 1),
        Pregunta("¿Cuántos planetas tiene el sistema solar?", listOf("7", "8", "9", "10"), 1),
        Pregunta("¿Quién pintó la Mona Lisa?", listOf("Picasso", "Van Gogh", "Leonardo da Vinci", "Dalí"), 2),
        Pregunta("¿Cuál es el océano más grande?", listOf("Atlántico", "Índico", "Ártico", "Pacífico"), 3),
        Pregunta("¿Cuántos jugadores juegan por equipo en la cancha en fútbol?", listOf("9", "10", "11", "12"), 2),
        Pregunta("¿En qué año ganó Argentina su tercer Mundial?", listOf("2014", "2018", "2022", "2010"), 2),
        Pregunta("¿Cuál es el animal terrestre más grande?", listOf("Elefante africano", "Jirafa", "Rinoceronte", "Oso"), 0),
        Pregunta("¿Cuántos colores tiene el arcoíris?", listOf("5", "6", "7", "8"), 2, "stainless-images-JzCf5Y3XmFU-unsplash"),
        Pregunta("¿Qué gas necesitamos respirar para vivir?", listOf("Nitrógeno", "Oxígeno", "Helio", "Hidrógeno"), 1),
        Pregunta("¿Cuál es el país más grande del mundo?", listOf("China", "Canadá", "Estados Unidos", "Rusia"), 3),
        
        Pregunta("¿Cuál es el elemento químico cuyo símbolo es Au?", listOf("Plata", "Oro", "Cobre", "Aluminio"), 1),
        Pregunta("¿En qué continente se encuentra Egipto?", listOf("África", "Asia", "Europa", "América"), 0),
        Pregunta("¿Cuál es la montaña más alta del mundo?", listOf("K2", "Aconcagua", "Everest", "Kilimanjaro"), 2),
        Pregunta("¿Cuántos huesos tiene el cuerpo humano adulto?", listOf("180", "206", "230", "250"), 1),
        Pregunta("¿Qué lenguaje de programación se usa principalmente para Android nativo?", listOf("Swift", "Kotlin", "Ruby", "PHP"), 1),
        Pregunta("¿Cuál es el planeta más cercano al Sol?", listOf("Venus", "Marte", "Mercurio", "Júpiter"), 2),
        Pregunta("¿Quién escribió 'Don Quijote de la Mancha'?", listOf("Gabriel García Márquez", "Jorge Luis Borges", "Miguel de Cervantes", "Pablo Neruda"), 2),
        Pregunta("¿Cuál es el río más largo del mundo?", listOf("Nilo", "Amazonas", "Misisipi", "Yangtsé"), 1),
        Pregunta("¿En qué año comenzó la Segunda Guerra Mundial?", listOf("1939", "1945", "1914", "1941"), 0),
        Pregunta("¿Cuál es la moneda oficial de Japón?", listOf("Yuan", "Dólar", "Yen", "Won"), 2),

        Pregunta("¿Cuál es el país con mayor población del mundo?", listOf("India", "China", "Estados Unidos", "Indonesia"), 0),
        Pregunta("¿Qué instrumento musical tiene 88 teclas por lo general?", listOf("Guitarra", "Violín", "Piano", "Batería"), 2),
        Pregunta("¿En qué año llegó el hombre a la Luna?", listOf("1965", "1969", "1972", "1958"), 1),
        Pregunta("¿Cuál es la capital de Australia?", listOf("Sídney", "Melbourne", "Canberra", "Brisbane"), 2),
        Pregunta("¿Cuál es el mamífero capaz de volar?", listOf("Avestruz", "Pingüino", "Murciélago", "Ardilla voladora"), 2),
        Pregunta("¿Quién formuló la teoría de la relatividad?", listOf("Isaac Newton", "Albert Einstein", "Galileo Galilei", "Nikola Tesla"), 1),
        Pregunta("¿Cuál es the idioma más hablado del mundo por número de hablantes nativos?", listOf("Inglés", "Español", "Mandarín", "Hindi"), 2),
        Pregunta("¿Cuántos lados tiene un hexágono?", listOf("5", "6", "7", "8"), 1),
        Pregunta("¿En qué país se originaron los Juegos Olímpicos?", listOf("Italia", "Grecia", "Egipto", "Francia"), 1),
        Pregunta("¿Cuál es el metal líquido a temperatura ambiente?", listOf("Hierro", "Mercurio", "Platino", "Plomo"), 1)
    )

    private var lista = todas.shuffled()
    private var indice = 0
    private var puntos = 0
    private var respondida = false

    private lateinit var titulo: TextView
    private lateinit var imagenPregunta: ImageView
    private lateinit var pregunta: TextView
    private lateinit var botones: List<Button>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(48, 48, 48, 48)

        titulo = TextView(this)
        titulo.textSize = 18f

        // Creamos el componente de imagen dinámico
        imagenPregunta = ImageView(this)
        imagenPregunta.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            450 // Altura en píxeles para la imagen
        ).apply {
            setMargins(0, 24, 0, 24)
        }
        imagenPregunta.scaleType = ImageView.ScaleType.CENTER_CROP
        imagenPregunta.visibility = View.GONE // Oculto por defecto hasta que toque una pregunta con imagen

        pregunta = TextView(this)
        pregunta.textSize = 24f
        pregunta.setPadding(0, 24, 0, 48)

        // Agregamos los elementos al orden visual del layout
        layout.addView(titulo)
        layout.addView(imagenPregunta)
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
            imagenPregunta.visibility = View.GONE
            pregunta.text = "Tu puntaje: $puntos de ${lista.size}"
            botones[0].text = "Jugar de nuevo"
            for (i in 1..3) botones[i].visibility = View.GONE
            return
        }

        val p = lista[indice]
        titulo.text = "Pregunta ${indice + 1} de ${lista.size}  |  Puntos: $puntos"
        pregunta.text = p.texto

        // Manejo de la imagen de la pregunta
        if (p.imagenNombre != null) {
            val resId = resources.getIdentifier(p.imagenNombre, "drawable", packageName)
            if (resId != 0) {
                imagenPregunta.setImageResource(resId)
                imagenPregunta.visibility = View.VISIBLE
            } else {
                imagenPregunta.visibility = View.GONE
            }
        } else {
            imagenPregunta.visibility = View.GONE
        }

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

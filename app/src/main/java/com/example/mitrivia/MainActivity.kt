package com.example.mitrivia

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.TimeZone
import org.json.JSONArray

data class Pregunta(
    val categoria: String,
    val dificultad: Int,
    val texto: String,
    val opciones: List<String>,
    val correcta: Int
)

private class Mision(val id: String, val desc: String, val meta: Int, val premio: Int)

private class Tema(val id: String, val nombre: String, val precio: Int, val fondo: Int)

private class Logro(val id: String, val emoji: String, val nombre: String, val desc: String)

class MainActivity : AppCompatActivity() {

    private val categorias = listOf(
        "Videojuegos", "Fútbol", "Cine y series", "Ciencia y naturaleza",
        "Historia", "Geografía", "Música", "Cultura argentina"
    )
    private val nombresDif = listOf("Fácil", "Medio", "Difícil")
    private val tiempoPorPregunta = 15
    private val tasa = 22050

    private val colores = mapOf(
        "Videojuegos" to Color.parseColor("#7E57C2"),
        "Fútbol" to Color.parseColor("#2E7D32"),
        "Cine y series" to Color.parseColor("#C62828"),
        "Ciencia y naturaleza" to Color.parseColor("#1565C0"),
        "Historia" to Color.parseColor("#E65100"),
        "Geografía" to Color.parseColor("#00796B"),
        "Música" to Color.parseColor("#AD1457"),
        "Cultura argentina" to Color.parseColor("#0288D1"),
        "Contrarreloj" to Color.parseColor("#8E24AA"),
        "Mezcla" to Color.parseColor("#455A64")
    )

    private val emojis = mapOf(
        "Videojuegos" to "🎮",
        "Fútbol" to "⚽",
        "Cine y series" to "🎬",
        "Ciencia y naturaleza" to "🔬",
        "Historia" to "🏛️",
        "Geografía" to "🌎",
        "Música" to "🎵",
        "Cultura argentina" to "🧉",
        "Mezcla" to "🎲"
    )

    private val temas: List<Tema> = listOf(
        Tema("clasico", "Clásico", 0, 0),
        Tema("medianoche", "🌙 Medianoche", 60, Color.parseColor("#0D1B2A")),
        Tema("bosque", "🌲 Bosque", 60, Color.parseColor("#0F2A1D")),
        Tema("atardecer", "🌇 Atardecer", 80, Color.parseColor("#2B1B3D")),
        Tema("dorado", "🏆 Dorado", 120, Color.parseColor("#2A2108"))
    )

    private val logros: List<Logro> = listOf(
        Logro("primera", "🎉", "Primera partida", "Terminá tu primera partida"),
        Logro("racha5", "🔥", "En llamas", "Conseguí 5 aciertos seguidos"),
        Logro("racha10", "🌋", "Imparable", "Conseguí 10 aciertos seguidos"),
        Logro("perfecta", "💎", "Partida perfecta", "Terminá una partida de 10 preguntas o más sin fallar ninguna"),
        Logro("todoterreno", "🧭", "Todoterreno", "Jugá una partida de cada categoría"),
        Logro("cien", "💯", "Cien aciertos", "Acertá 100 preguntas en total"),
        Logro("super15", "♾️", "Sobreviviente", "Acertá 15 preguntas en una partida de Supervivencia"),
        Logro("dia3", "📅", "Constante", "3 días seguidos con la pregunta del día"),
        Logro("dia7", "🗓️", "Hábito", "7 días seguidos con la pregunta del día"),
        Logro("contra15", "⚡", "Velocista", "Acertá 15 preguntas en una partida de Contrarreloj")
    ) + categorias.map {
        Logro("exp_$it", emojis[it] ?: "⭐", "Experto en $it", "Acertá 25 preguntas de $it")
    }

    private val frasesAcierto = listOf(
        "¡Bien ahí!", "¡Crack!", "¡Excelente!", "¡Eso es!", "¡Así se hace!",
        "¡Qué máquina!", "¡Bárbaro!", "¡Correcto!"
    )
    private val frasesError = listOf(
        "Uy, casi...", "Pasó cerca", "La próxima sale", "Se aprende fallando",
        "No pasa nada, seguimos", "Esa era difícil"
    )

    private var todas: List<Pregunta> = emptyList()
    private var datos: Map<String, List<String>> = emptyMap()
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
    private var pantalla = "inicio"
    private var ultimoAtras = 0L
    private var modoRepaso = false
    private var statusOriginal = 0
    private var modoContra = false
    private var timerContra: CountDownTimer? = null
    private var restanteContra = 0L
    private var restanteMs = 0L
    private var erroresContra = 0
    private var partidaXp = 0
    private var partidaMonedas = 0
    private val duracionContra = 60000L
    private lateinit var vidasView: TextView
    private lateinit var tiempoContraView: TextView
    private lateinit var barraContra: ProgressBar
    private var modoSuper = false
    private var aciertosPartida = 0
    private var sonido = true
    private var musica = true
    private var pista: AudioTrack? = null
    private var sonidoOk: ShortArray = ShortArray(0)
    private var sonidoMal: ShortArray = ShortArray(0)
    private val falladas = ArrayList<Pregunta>()
    private val comodines = ArrayList<View>()
    private val handler = Handler(Looper.getMainLooper())
    private var timer: CountDownTimer? = null

    private lateinit var scroll: ScrollView
    private lateinit var contenedor: LinearLayout
    private lateinit var tiempoView: TextView
    private lateinit var barra: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        todas = cargarPreguntas()
        datos = cargarDatos()
        sonido = prefs().getBoolean("sonido", true)
        musica = prefs().getBoolean("musica", true)
        sonidoOk = generarTono(listOf(523, 659, 784), 90, 0.5, false)
        sonidoMal = generarTono(listOf(196, 147), 180, 0.25, true)

        contenedor = LinearLayout(this)
        contenedor.orientation = LinearLayout.VERTICAL
        contenedor.setPadding(48, 48, 48, 48)

        scroll = ScrollView(this)
        scroll.addView(contenedor)
        setContentView(scroll)
        statusOriginal = window.statusBarColor
        aplicarTema()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                atras()
            }
        })

        mostrarCategorias()
    }

    private fun atras() {
        when (pantalla) {
            "inicio" -> confirmarSalida("Tocá otra vez para salir") { finish() }
            "pregunta" -> confirmarSalida("Tocá otra vez para dejar la partida") { mostrarCategorias() }
            "dificultad" -> mostrarElegirCategoria()
            "categorias", "fin", "fincontra" -> mostrarJugar()
            "contra" -> confirmarSalida("Tocá otra vez para dejar la partida") {
                timerContra?.cancel()
                modoContra = false
                mostrarCategorias()
            }
            else -> mostrarCategorias()
        }
    }

    private fun confirmarSalida(aviso: String, accion: () -> Unit) {
        val ahora = System.currentTimeMillis()
        if (ahora - ultimoAtras < 2000L) {
            ultimoAtras = 0L
            accion()
        } else {
            ultimoAtras = ahora
            Toast.makeText(this, aviso, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        iniciarMusica()
        if (modoContra && pantalla == "contra" && !respondida) {
            iniciarTimerContra()
        }
    }

    override fun onPause() {
        super.onPause()
        pausarMusica()
        timerContra?.cancel()
    }

    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel()
        handler.removeCallbacksAndMessages(null)
        timerContra?.cancel()
        try {
            pista?.release()
        } catch (e: Exception) {
            // nada
        }
        pista = null
    }

    private fun iniciarMusica() {
        if (!musica) return
        try {
            if (pista == null) {
                val bytes = assets.open("musica.pcm").use { it.readBytes() }
                val n = bytes.size / 2
                val muestras = ShortArray(n)
                ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(muestras)
                val atributos = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
                val formato = AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(22050)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
                val at = AudioTrack.Builder()
                    .setAudioAttributes(atributos)
                    .setAudioFormat(formato)
                    .setBufferSizeInBytes(bytes.size)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
                at.write(muestras, 0, n)
                at.setLoopPoints(0, n, -1)
                at.setVolume(0.3f)
                pista = at
            }
            pista?.play()
        } catch (e: Exception) {
            // si no hay música o falla, el juego sigue sin ella
        }
    }

    private fun pausarMusica() {
        try {
            pista?.pause()
        } catch (e: Exception) {
            // nada
        }
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

    private fun cargarDatos(): Map<String, List<String>> {
        val mapa = HashMap<String, MutableList<String>>()
        val archivos: Array<String> = assets.list("") ?: emptyArray()
        for (nombre in archivos) {
            if (nombre.startsWith("datos") && nombre.endsWith(".json")) {
                try {
                    val json = assets.open(nombre).bufferedReader().use { it.readText() }
                    val arr = JSONArray(json)
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        val cat = o.getString("categoria")
                        if (!mapa.containsKey(cat)) {
                            mapa[cat] = ArrayList()
                        }
                        mapa[cat]!!.add(o.getString("dato"))
                    }
                } catch (e: Exception) {
                    // si un archivo de datos está mal armado, se saltea
                }
            }
        }
        return mapa
    }

    private fun datoDe(cat: String): String? = datos[cat]?.random()

    private fun generarTono(notas: List<Int>, msPorNota: Int, volumen: Double, cuadrada: Boolean): ShortArray {
        val porNota = tasa * msPorNota / 1000
        val muestras = ShortArray(porNota * notas.size)
        var pos = 0
        for (f in notas) {
            for (i in 0 until porNota) {
                val t = i.toDouble() / tasa
                var v = Math.sin(2.0 * Math.PI * f * t)
                if (cuadrada) {
                    v = if (v >= 0.0) 1.0 else -1.0
                }
                val borde = minOf(i, porNota - i).toDouble() / (porNota * 0.2)
                val env = minOf(1.0, borde)
                muestras[pos] = (v * env * volumen * Short.MAX_VALUE).toInt().toShort()
                pos++
            }
        }
        return muestras
    }

    private fun sonar(muestras: ShortArray) {
        if (!sonido || muestras.isEmpty()) return
        try {
            val atributos = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            val formato = AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(tasa)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()
            val track = AudioTrack.Builder()
                .setAudioAttributes(atributos)
                .setAudioFormat(formato)
                .setBufferSizeInBytes(muestras.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
            track.write(muestras, 0, muestras.size)
            track.play()
            handler.postDelayed({
                try {
                    track.release()
                } catch (e: Exception) {
                    // nada
                }
            }, muestras.size * 1000L / tasa + 300)
        } catch (e: Exception) {
            // si el sonido falla, el juego sigue sin sonido
        }
    }

    private fun vibrar(ms: Long) {
        if (!sonido) return
        try {
            val v = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= 26) {
                v.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                v.vibrate(ms)
            }
        } catch (e: Exception) {
            // sin vibración el juego sigue igual
        }
    }

    private fun dp(n: Int): Int = (n * resources.displayMetrics.density).toInt()

    private fun prefs() = getSharedPreferences("trivia", Context.MODE_PRIVATE)

    private fun record(dif: Int): Int = prefs().getInt("record_${catElegida}_$dif", 0)

    private fun colorDe(cat: String): Int = colores[cat] ?: Color.DKGRAY

    private fun conEmoji(cat: String): String = (emojis[cat] ?: "") + "  " + cat

    private fun fondo(color: Int): GradientDrawable {
        val g = GradientDrawable()
        g.setColor(color)
        g.cornerRadius = 48f
        return g
    }

    private fun texto(s: String, size: Float, margenArriba: Int = 0): TextView {
        val t = TextView(this)
        t.text = s
        t.textSize = size
        t.setPadding(0, margenArriba, 0, 0)
        if (temaActivo().fondo != 0) {
            t.setTextColor(Color.parseColor("#ECEFF1"))
        }
        contenedor.addView(t)
        return t
    }

    private fun boton(
        s: String,
        color: Int = Color.parseColor("#455A64"),
        colorTexto: Int = Color.WHITE,
        alClick: () -> Unit
    ): Button {
        val b = Button(this)
        b.text = s
        b.background = fondo(color)
        b.setTextColor(colorTexto)
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

    private fun agregarLogo(): Boolean {
        try {
            val bmp = BitmapFactory.decodeStream(assets.open("logo.png"))
            val iv = ImageView(this)
            iv.setImageBitmap(bmp)
            iv.scaleType = ImageView.ScaleType.FIT_CENTER
            val lp = LinearLayout.LayoutParams(dp(190), dp(190))
            lp.gravity = Gravity.CENTER_HORIZONTAL
            iv.layoutParams = lp
            contenedor.addView(iv)
            return true
        } catch (e: Exception) {
            return false
        }
    }

    private fun animarEntrada() {
        for (i in 0 until contenedor.childCount) {
            val v = contenedor.getChildAt(i)
            v.alpha = 0f
            v.translationY = 40f
            v.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(260L)
                .setStartDelay(minOf(i, 12) * 40L)
                .start()
        }
    }

    private fun rebote(v: View?) {
        if (v == null) return
        val a = ObjectAnimator.ofPropertyValuesHolder(
            v,
            PropertyValuesHolder.ofFloat("scaleX", 1f, 1.08f, 1f),
            PropertyValuesHolder.ofFloat("scaleY", 1f, 1.08f, 1f)
        )
        a.duration = 350L
        a.start()
    }

    private fun temblor() {
        val a = ObjectAnimator.ofFloat(
            contenedor, "translationX",
            0f, -28f, 28f, -20f, 20f, -10f, 10f, 0f
        )
        a.duration = 420L
        a.start()
    }

    private fun colorBarra(ms: Long): Int {
        if (ms > 8000L) return Color.parseColor("#43A047")
        if (ms > 5000L) return Color.parseColor("#FFB300")
        return Color.parseColor("#E53935")
    }

    private fun compartirTexto(mensaje: String) {
        try {
            val i = Intent(Intent.ACTION_SEND)
            i.type = "text/plain"
            i.putExtra(Intent.EXTRA_TEXT, mensaje)
            startActivity(Intent.createChooser(i, "Compartir con"))
        } catch (e: Exception) {
            // si no hay con qué compartir, no pasa nada
        }
    }

    private fun diaHoy(): Int {
        val ahora = System.currentTimeMillis()
        return ((ahora + TimeZone.getDefault().getOffset(ahora)) / 86400000L).toInt()
    }

    private fun yaRespondioHoy(): Boolean = prefs().getInt("dia_ultimo", -1) == diaHoy()

    private fun rachaDias(): Int {
        val ultimo = prefs().getInt("dia_ultimo", -1)
        val hoy = diaHoy()
        if (ultimo == hoy || ultimo == hoy - 1) {
            return prefs().getInt("dia_racha", 0)
        }
        return 0
    }

    private fun registrarDia() {
        val hoy = diaHoy()
        val ultimo = prefs().getInt("dia_ultimo", -1)
        val nueva = if (ultimo == hoy - 1) prefs().getInt("dia_racha", 0) + 1 else 1
        prefs().edit().putInt("dia_ultimo", hoy).putInt("dia_racha", nueva).apply()
        if (nueva >= 3) desbloquear("dia3")
        if (nueva >= 7) desbloquear("dia7")
    }

    private fun mostrarDelDia() {
        pantalla = "dia"
        timer?.cancel()
        contenedor.removeAllViews()
        contenedor.translationX = 0f
        scroll.scrollTo(0, 0)
        texto("📅 Pregunta del día", 26f)
        texto("Racha: ${rachaDias()} días seguidos", 16f, 8).setTextColor(Color.parseColor("#FFB300"))

        if (todas.isEmpty()) {
            boton("Volver") { mostrarCategorias() }
            animarEntrada()
            return
        }
        if (yaRespondioHoy()) {
            texto("Ya respondiste la de hoy. ¡Volvé mañana por la próxima!", 18f, 24)
            boton("📤 Compartir mi racha", Color.parseColor("#2E7D32")) {
                compartirTexto("Llevo ${rachaDias()} días seguidos en la pregunta del día de Trivia Max 🔥 ¿Me alcanzás?")
            }
            boton("Volver") { mostrarCategorias() }
            animarEntrada()
            return
        }

        val ordenadas = todas.sortedBy { it.texto }
        val p = ordenadas[diaHoy() % ordenadas.size]
        texto("${conEmoji(p.categoria)} · ${nombresDif[p.dificultad - 1]}", 14f, 16)
            .setTextColor(colorDe(p.categoria))
        texto(p.texto, 24f, 24)

        val correctaTexto = p.opciones[p.correcta]
        var contestada = false
        var volver: Button? = null
        val botones = ArrayList<Button>()
        for (op in p.opciones.shuffled()) {
            val b = boton(op, Color.LTGRAY, Color.BLACK) { }
            b.setOnClickListener {
                if (contestada) return@setOnClickListener
                contestada = true
                val acierto = op == correctaTexto
                registrarDia()
                progresoMision("dia")
                if (acierto) {
                    b.background = fondo(Color.GREEN)
                    rebote(b)
                    darRecompensa(p.dificultad + 2)
                    sonar(sonidoOk)
                    vibrar(40L)
                } else {
                    b.background = fondo(Color.RED)
                    marcarFallada(p)
                    for (x in botones) {
                        if (x.text.toString() == correctaTexto) {
                            x.background = fondo(Color.GREEN)
                        }
                    }
                    temblor()
                    sonar(sonidoMal)
                    vibrar(220L)
                }
                volver?.let { contenedor.removeView(it) }
                val t = texto(if (acierto) frasesAcierto.random() else frasesError.random(), 22f, 24)
                t.setTextColor(if (acierto) Color.parseColor("#66BB6A") else Color.parseColor("#EF5350"))
                t.setTypeface(null, Typeface.BOLD)
                texto("🔥 Racha: ${rachaDias()} días seguidos", 18f, 8)
                val dato = datoDe(p.categoria)
                if (dato != null) {
                    texto("¿Sabías que...?", 16f, 20).setTextColor(colorDe(p.categoria))
                    texto(dato, 16f, 4)
                }
                boton("📤 Compartir mi racha", Color.parseColor("#2E7D32")) {
                    compartirTexto("Llevo ${rachaDias()} días seguidos en la pregunta del día de Trivia Max 🔥 ¿Me alcanzás?")
                }
                boton("Volver al menú") { mostrarCategorias() }
                scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
            }
            botones.add(b)
        }
        volver = boton("Volver") { mostrarCategorias() }
        animarEntrada()
    }

    private fun tiene(id: String): Boolean = prefs().getBoolean("logro_$id", false)

    private fun desbloquear(id: String) {
        if (tiene(id)) return
        prefs().edit().putBoolean("logro_$id", true).apply()
        val l = logros.firstOrNull { it.id == id } ?: return
        Toast.makeText(this, "🏅 Logro: ${l.nombre}", Toast.LENGTH_LONG).show()
    }

    private fun sumar(clave: String): Int {
        val v = prefs().getInt(clave, 0) + 1
        prefs().edit().putInt(clave, v).apply()
        return v
    }

    private fun registrarAcierto(p: Pregunta) {
        aciertosPartida++
        if (sumar("aciertos_total") >= 100) desbloquear("cien")
        if (sumar("ac_${p.categoria}") >= 25) desbloquear("exp_${p.categoria}")
        if (racha >= 5) desbloquear("racha5")
        if (racha >= 10) desbloquear("racha10")
        if (modoSuper && aciertosPartida >= 15) desbloquear("super15")
        if (modoContra && aciertosPartida >= 15) desbloquear("contra15")
        progresoMision("aciertos10")
        progresoMision("aciertos25")
        if (p.categoria == categoriaDelDia()) progresoMision("cat")
        if (racha >= 5) progresoMision("racha5")
    }

    private fun logrosFinPartida() {
        desbloquear("primera")
        if (!modoSuper) {
            if (catElegida != "Mezcla") {
                prefs().edit().putBoolean("jugada_$catElegida", true).apply()
                if (categorias.all { prefs().getBoolean("jugada_$it", false) }) {
                    desbloquear("todoterreno")
                }
            }
            if (vidas == 3 && falladas.isEmpty() && ronda.size >= 10) {
                desbloquear("perfecta")
            }
        }
    }

    private fun mostrarLogros() {
        pantalla = "logros"
        contenedor.removeAllViews()
        contenedor.translationX = 0f
        scroll.scrollTo(0, 0)
        val n = logros.count { tiene(it.id) }
        texto("🏅 Logros", 28f)
        texto("$n de ${logros.size} desbloqueados", 16f, 8).setTextColor(Color.parseColor("#FFB300"))
        for (l in logros) {
            val ok = tiene(l.id)
            val t = texto((if (ok) "✅ " else "🔒 ") + l.emoji + "  " + l.nombre, 18f, 20)
            t.setTextColor(if (ok) Color.parseColor("#66BB6A") else Color.GRAY)
            texto(l.desc, 14f, 2)
        }
        boton("Volver") { mostrarCategorias() }
        animarEntrada()
    }

    private fun empezarSuper() {
        modoSuper = true
        catElegida = "Mezcla"
        difElegida = 0
        empezar()
    }

    private fun mostrarCategorias() {
        pantalla = "inicio"
        timer?.cancel()
        timerContra?.cancel()
        modoContra = false
        contenedor.removeAllViews()
        contenedor.translationX = 0f
        scroll.scrollTo(0, 0)
        if (!agregarLogo()) {
            texto("Trivia Max", 30f)
        }
        agregarProgreso()
        boton("🎮 Jugar", Color.parseColor("#1E88E5")) { mostrarJugar() }
        val etiquetaDia = if (yaRespondioHoy()) "📅 Pregunta del día ✓" else "📅 Pregunta del día"
        boton("$etiquetaDia  ·  racha ${rachaDias()}", Color.parseColor("#F9A825"), Color.BLACK) {
            mostrarDelDia()
        }
        boton("🎯 Misiones  (${misionesCompletas()}/3)", Color.parseColor("#C2185B")) { mostrarMisiones() }
        boton("🏅 Logros  (${logros.count { tiene(it.id) }}/${logros.size})", Color.parseColor("#6D4C41")) {
            mostrarLogros()
        }
        boton("🛒 Tienda  ·  🪙 ${monedas()}", Color.parseColor("#00897B")) { mostrarTienda() }
        boton("⚙️ Configuración", Color.DKGRAY) { mostrarConfig() }
        animarEntrada()
    }

    private fun xpTotal(): Int = prefs().getInt("xp", 0)

    private fun monedas(): Int = prefs().getInt("monedas", 0)

    private fun umbral(n: Int): Int = 150 * n * (n - 1) / 2

    private fun nivelDe(xp: Int): Int {
        var n = 1
        while (xp >= umbral(n + 1)) {
            n++
        }
        return n
    }

    private fun darRecompensa(ganados: Int) {
        val antes = nivelDe(xpTotal())
        val xpNuevo = xpTotal() + ganados * 10
        var mon = monedas() + ganados
        val despues = nivelDe(xpNuevo)
        if (despues > antes) {
            val premio = 25 * (despues - antes)
            mon += premio
            Toast.makeText(this, "🎉 ¡Nivel $despues! +$premio monedas", Toast.LENGTH_LONG).show()
        }
        prefs().edit().putInt("xp", xpNuevo).putInt("monedas", mon).apply()
        partidaXp += ganados * 10
        partidaMonedas += ganados
    }

    private fun agregarProgreso() {
        val xp = xpTotal()
        val n = nivelDe(xp)
        val base = umbral(n)
        val siguiente = umbral(n + 1)
        texto("⭐ Nivel $n   ·   🪙 ${monedas()} monedas", 18f, 12).gravity = Gravity.CENTER_HORIZONTAL
        val b = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal)
        b.max = siguiente - base
        b.progress = xp - base
        b.progressTintList = ColorStateList.valueOf(Color.parseColor("#FFB300"))
        val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(8))
        lp.topMargin = 8
        b.layoutParams = lp
        contenedor.addView(b)
        texto("${xp - base} / ${siguiente - base} XP para el nivel ${n + 1}", 12f, 4).gravity = Gravity.CENTER_HORIZONTAL
    }

    private fun mostrarTienda() {
        pantalla = "tienda"
        timer?.cancel()
        contenedor.removeAllViews()
        contenedor.translationX = 0f
        scroll.scrollTo(0, 0)
        texto("🛒 Tienda", 28f)
        texto("Tus monedas: 🪙 ${monedas()}", 18f, 8).setTextColor(Color.parseColor("#FFB300"))
        texto(
            "Ganás monedas acertando preguntas y subiendo de nivel. " +
                "Lo que comprás lo usás durante una partida.",
            13f, 8
        )
        articulo("inv_5050", "🎯 50/50 extra", 15, "Elimina dos opciones incorrectas.")
        articulo("inv_tiempo", "⏱️ +10 segundos", 20, "Suma 10 segundos a la pregunta actual.")
        articulo("inv_vida", "❤️ Vida extra", 40, "Te da una vida más (máximo 5).")
        texto("🎨 Temas de color", 22f, 32)
        texto("Cambian el fondo de toda la app. Se compran una sola vez.", 13f, 4)
        for (t in temas) {
            val tengo = t.precio == 0 || prefs().getBoolean("tema_${t.id}", false)
            val activo = prefs().getString("tema_activo", "clasico") == t.id
            if (activo) {
                boton("✓ ${t.nombre}  (en uso)", Color.parseColor("#2E7D32")) { }
            } else if (tengo) {
                boton("Usar ${t.nombre}", Color.parseColor("#546E7A")) {
                    prefs().edit().putString("tema_activo", t.id).apply()
                    aplicarTema()
                    mostrarTienda()
                }
            } else {
                boton("Comprar ${t.nombre}  ·  ${t.precio} 🪙", Color.parseColor("#00897B")) {
                    if (monedas() < t.precio) {
                        Toast.makeText(this, "Te faltan monedas", Toast.LENGTH_SHORT).show()
                    } else {
                        prefs().edit()
                            .putInt("monedas", monedas() - t.precio)
                            .putBoolean("tema_${t.id}", true)
                            .putString("tema_activo", t.id)
                            .apply()
                        aplicarTema()
                        mostrarTienda()
                    }
                }
            }
        }
        boton("Volver") { mostrarCategorias() }
        animarEntrada()
    }

    private fun articulo(clave: String, nombre: String, precio: Int, desc: String) {
        texto("$nombre  (tenés ${prefs().getInt(clave, 0)})", 18f, 24)
        texto(desc, 13f, 2)
        boton("Comprar por $precio 🪙", Color.parseColor("#2E7D32")) {
            if (monedas() < precio) {
                Toast.makeText(this, "Te faltan monedas", Toast.LENGTH_SHORT).show()
            } else {
                prefs().edit()
                    .putInt("monedas", monedas() - precio)
                    .putInt(clave, prefs().getInt(clave, 0) + 1)
                    .apply()
                mostrarTienda()
            }
        }
    }

    private fun mostrarJugar() {
        pantalla = "jugar"
        timer?.cancel()
        timerContra?.cancel()
        modoContra = false
        contenedor.removeAllViews()
        contenedor.translationX = 0f
        scroll.scrollTo(0, 0)
        texto("🎮 Jugar", 28f)
        texto("Elegí cómo querés jugar", 16f, 8)
        boton("📚 Por categoría", Color.parseColor("#1E88E5")) { mostrarElegirCategoria() }
        texto("Elegí un tema y una dificultad. Tenés 3 vidas y $tiempoPorPregunta segundos por pregunta.", 13f, 6)
        boton("♾️ Supervivencia  ·  récord ${prefs().getInt("record_super", 0)}", Color.parseColor("#D84315")) {
            empezarSuper()
        }
        texto("Todas las categorías, de fácil a difícil, hasta quedarte sin vidas.", 13f, 6)
        boton("⏱️ Contrarreloj  ·  récord ${prefs().getInt("record_contra", 0)}", colorDe("Contrarreloj")) {
            empezarContra()
        }
        texto("60 segundos para acertar todas las que puedas. Cada error te resta 3 segundos.", 13f, 6)
        boton("📖 Repaso  ·  ${idsFalladas().size} pendientes", Color.parseColor("#5E35B1")) { empezarRepaso() }
        texto("Practicá solo las preguntas que fallaste, sin perder vidas.", 13f, 6)
        boton("Volver") { mostrarCategorias() }
        animarEntrada()
    }

    private fun mostrarElegirCategoria() {
        pantalla = "categorias"
        timer?.cancel()
        contenedor.removeAllViews()
        contenedor.translationX = 0f
        scroll.scrollTo(0, 0)
        texto("📚 Por categoría", 28f)
        texto(
            "Tenés 3 vidas, $tiempoPorPregunta segundos por pregunta y comodines. " +
                "Con 3 aciertos seguidos ganás 1 punto extra en cada acierto.",
            14f, 8
        )
        texto("Elegí una categoría", 18f, 24)
        for (c in categorias) {
            boton(conEmoji(c), colorDe(c)) {
                modoSuper = false
                catElegida = c
                mostrarDificultad()
            }
        }
        boton(conEmoji("Mezcla") + " (todas)", colorDe("Mezcla")) {
            modoSuper = false
            catElegida = "Mezcla"
            mostrarDificultad()
        }
        boton("Volver") { mostrarJugar() }
        animarEntrada()
    }

    private fun mostrarConfig() {
        pantalla = "config"
        timer?.cancel()
        contenedor.removeAllViews()
        contenedor.translationX = 0f
        scroll.scrollTo(0, 0)
        texto("⚙️ Configuración", 28f)
        boton("Sonido y vibración: " + (if (sonido) "SÍ" else "NO"), Color.DKGRAY) {
            sonido = !sonido
            prefs().edit().putBoolean("sonido", sonido).apply()
            mostrarConfig()
        }
        boton("Música de fondo: " + (if (musica) "SÍ" else "NO"), Color.DKGRAY) {
            musica = !musica
            prefs().edit().putBoolean("musica", musica).apply()
            if (musica) {
                iniciarMusica()
            } else {
                pausarMusica()
            }
            mostrarConfig()
        }

        texto("Cómo se juega", 18f, 28)
        texto(
            "• Tenés 3 vidas y $tiempoPorPregunta segundos por pregunta.\n" +
                "• Cada acierto suma puntos según la dificultad.\n" +
                "• Con 3 aciertos seguidos ganás 1 punto extra en cada acierto.\n" +
                "• Comodines: 50/50 (2 por partida) y saltar pregunta (1).\n" +
                "• Pregunta del día: una por día, y cuenta tu racha de días.\n" +
                "• Supervivencia: preguntas cada vez más difíciles hasta quedarte sin vidas.\n" +
                "• Contrarreloj: 60 segundos, cada error te resta 3.\n" +
                "• Acertando ganás monedas 🪙 y experiencia para subir de nivel; en la tienda comprás ayudas.",
            14f, 8
        )

        var version = ""
        try {
            version = packageManager.getPackageInfo(packageName, 0).versionName ?: ""
        } catch (e: Exception) {
            // si no se puede leer, no se muestra
        }
        if (version.isNotEmpty()) {
            texto("Versión $version", 13f, 20)
        }

        var seguro = false
        val bBorrar = boton("🗑️ Borrar récords, logros, monedas y racha", Color.parseColor("#B71C1C")) { }
        bBorrar.setOnClickListener {
            if (!seguro) {
                seguro = true
                bBorrar.text = "¿Seguro? Tocá de nuevo para borrar todo"
            } else {
                prefs().edit().clear().putBoolean("sonido", sonido).putBoolean("musica", musica).apply()
                aplicarTema()
                Toast.makeText(this, "Listo, todo borrado", Toast.LENGTH_SHORT).show()
                mostrarConfig()
            }
        }
        boton("Volver") { mostrarCategorias() }
        animarEntrada()
    }

    private fun mostrarDificultad() {
        pantalla = "dificultad"
        contenedor.removeAllViews()
        scroll.scrollTo(0, 0)
        texto(conEmoji(catElegida), 28f).setTextColor(colorDe(catElegida))
        texto("Elegí la dificultad", 18f, 24)
        for (d in 1..3) {
            boton("${nombresDif[d - 1]}  ·  vale $d  ·  récord ${record(d)}", colorDe(catElegida)) {
                difElegida = d
                empezar()
            }
        }
        boton("Todas  ·  récord ${record(0)}", colorDe(catElegida)) {
            difElegida = 0
            empezar()
        }
        boton("Volver") { mostrarElegirCategoria() }
        animarEntrada()
    }

    private fun empezar() {
        var f = todas
        if (catElegida != "Mezcla") {
            f = f.filter { it.categoria == catElegida }
        }
        if (difElegida != 0) {
            f = f.filter { it.dificultad == difElegida }
        }
        ronda = if (modoSuper) todas.shuffled().sortedBy { it.dificultad } else f.shuffled()
        indice = 0
        puntos = 0
        vidas = 3
        racha = 0
        aciertosPartida = 0
        modoRepaso = false
        modoContra = false
        partidaXp = 0
        partidaMonedas = 0
        cincuenta = 2
        saltos = 1
        falladas.clear()
        mostrarPregunta()
    }

    private fun mostrarPregunta() {
        timer?.cancel()
        if (indice >= ronda.size || vidas <= 0) {
            mostrarFin()
            return
        }
        pantalla = "pregunta"
        respondida = false
        comodines.clear()
        val p = ronda[indice]
        contenedor.removeAllViews()
        contenedor.translationX = 0f
        scroll.scrollTo(0, 0)

        vidasView = texto(textoCabecera(), 18f)
        val progreso = if (modoSuper) "Pregunta ${indice + 1}" else "${indice + 1} de ${ronda.size}"
        texto(
            "${conEmoji(p.categoria)} · ${nombresDif[p.dificultad - 1]} · $progreso",
            14f, 8
        ).setTextColor(colorDe(p.categoria))
        tiempoView = texto("Tiempo: $tiempoPorPregunta", 18f, 8)

        barra = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal)
        barra.max = tiempoPorPregunta * 1000
        barra.progress = tiempoPorPregunta * 1000
        barra.progressTintList = ColorStateList.valueOf(colorBarra(tiempoPorPregunta * 1000L))
        val lpBarra = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dp(10)
        )
        lpBarra.topMargin = 8
        barra.layoutParams = lpBarra
        contenedor.addView(barra)

        texto(p.texto, 24f, 32)

        val correctaTexto = p.opciones[p.correcta]
        val botones = ArrayList<Button>()
        for (op in p.opciones.shuffled()) {
            val b = boton(op, Color.LTGRAY, Color.BLACK) { }
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

        val inv5050 = prefs().getInt("inv_5050", 0)
        if (inv5050 > 0) {
            val bExtra = boton("🎯 50/50 extra  (tenés $inv5050)", Color.parseColor("#2E7D32")) { }
            bExtra.setOnClickListener {
                if (respondida || usado50) return@setOnClickListener
                val quedan = prefs().getInt("inv_5050", 0)
                if (quedan <= 0) return@setOnClickListener
                usado50 = true
                prefs().edit().putInt("inv_5050", quedan - 1).apply()
                bExtra.text = "🎯 50/50 extra  (tenés ${quedan - 1})"
                var ocultar = 2
                for (b in botones.shuffled()) {
                    if (ocultar > 0 && b.text.toString() != correctaTexto) {
                        b.visibility = View.INVISIBLE
                        ocultar--
                    }
                }
            }
            comodines.add(bExtra)
        }
        val invTiempo = prefs().getInt("inv_tiempo", 0)
        if (invTiempo > 0) {
            val bTiempo = boton("⏱️ +10 segundos  (tenés $invTiempo)", Color.parseColor("#2E7D32")) { }
            bTiempo.setOnClickListener {
                if (respondida) return@setOnClickListener
                val quedan = prefs().getInt("inv_tiempo", 0)
                if (quedan <= 0) return@setOnClickListener
                prefs().edit().putInt("inv_tiempo", quedan - 1).apply()
                bTiempo.text = "⏱️ +10 segundos  (tenés ${quedan - 1})"
                val total = restanteMs + 10000L
                barra.max = total.toInt()
                iniciarTimer(p, botones, correctaTexto, total)
            }
            comodines.add(bTiempo)
        }
        val invVida = prefs().getInt("inv_vida", 0)
        if (invVida > 0) {
            val bVida = boton("❤️ Vida extra  (tenés $invVida)", Color.parseColor("#2E7D32")) { }
            bVida.setOnClickListener {
                if (respondida || vidas >= 5) return@setOnClickListener
                val quedan = prefs().getInt("inv_vida", 0)
                if (quedan <= 0) return@setOnClickListener
                prefs().edit().putInt("inv_vida", quedan - 1).apply()
                vidas++
                bVida.text = "❤️ Vida extra  (tenés ${quedan - 1})"
                vidasView.text = textoCabecera()
            }
            comodines.add(bVida)
        }

        comodines.add(b50)
        comodines.add(bSaltar)

        animarEntrada()
        iniciarTimer(p, botones, correctaTexto)
    }

    private fun iniciarTimer(
        p: Pregunta,
        botones: List<Button>,
        correctaTexto: String,
        duracion: Long = tiempoPorPregunta * 1000L
    ) {
        restanteMs = duracion
        timer?.cancel()
        timer = object : CountDownTimer(duracion, 100L) {
            override fun onTick(ms: Long) {
                restanteMs = ms
                tiempoView.text = "Tiempo: ${(ms + 999) / 1000}"
                barra.progress = ms.toInt()
                barra.progressTintList = ColorStateList.valueOf(colorBarra(ms))
                if (ms < 5000L && (ms / 250L) % 2L == 1L) {
                    barra.alpha = 0.45f
                } else {
                    barra.alpha = 1f
                }
            }

            override fun onFinish() {
                tiempoView.text = "Tiempo: 0"
                barra.progress = 0
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
        barra.alpha = 1f
        val frase: String
        if (acierto) {
            racha++
            val bonus = if (racha >= 3) 1 else 0
            val ganados = p.dificultad + bonus
            puntos += ganados
            registrarAcierto(p)
            darRecompensa(ganados)
            if (modoRepaso) {
                marcarAprendida(p)
            }
            elegido?.background = fondo(Color.GREEN)
            frase = if (racha >= 3) {
                "🔥 ¡Racha de $racha! +$ganados puntos"
            } else {
                frasesAcierto.random() + "  +$ganados"
            }
            rebote(elegido)
            sonar(sonidoOk)
            vibrar(40L)
        } else {
            racha = 0
            if (!modoRepaso) {
                vidas--
            }
            falladas.add(p)
            marcarFallada(p)
            elegido?.background = fondo(Color.RED)
            for (b in botones) {
                if (b.text.toString() == correctaTexto) {
                    b.visibility = View.VISIBLE
                    b.background = fondo(Color.GREEN)
                }
            }
            frase = if (elegido == null) {
                "⏰ ¡Se acabó el tiempo!"
            } else {
                frasesError.random()
            }
            temblor()
            sonar(sonidoMal)
            vibrar(220L)
        }
        for (v in comodines) {
            v.visibility = View.GONE
        }
        mostrarDato(p, frase, acierto)
    }

    private fun mostrarDato(p: Pregunta, frase: String, acierto: Boolean) {
        val t = texto(frase, 22f, 24)
        t.setTextColor(if (acierto) Color.parseColor("#66BB6A") else Color.parseColor("#EF5350"))
        t.setTypeface(null, Typeface.BOLD)

        val dato = datoDe(p.categoria)
        if (dato != null) {
            texto("¿Sabías que...?", 16f, 20).setTextColor(colorDe(p.categoria))
            texto(dato, 16f, 4)
        }
        boton("Siguiente", colorDe(p.categoria)) {
            if (respondida) {
                respondida = false
                indice++
                mostrarPregunta()
            }
        }
        scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
    }

    private fun temaActivo(): Tema {
        val id = prefs().getString("tema_activo", "clasico")
        return temas.firstOrNull { it.id == id } ?: temas[0]
    }

    private fun aplicarTema() {
        val t = temaActivo()
        if (t.fondo == 0) {
            scroll.setBackgroundColor(Color.TRANSPARENT)
            window.statusBarColor = statusOriginal
        } else {
            scroll.setBackgroundColor(t.fondo)
            window.statusBarColor = t.fondo
        }
    }

    private fun textoCabecera(): String {
        if (modoRepaso) {
            return "📖 Repaso   Aciertos: $aciertosPartida   Pendientes: ${idsFalladas().size}"
        }
        return "Vidas: " + "♥".repeat(vidas) + "   Puntos: $puntos   Racha: $racha"
    }

    private fun idDe(p: Pregunta): String = p.texto.hashCode().toString()

    private fun idsFalladas(): Set<String> {
        val guardado = prefs().getStringSet("falladas_ids", null) ?: return emptySet()
        return HashSet(guardado)
    }

    private fun marcarFallada(p: Pregunta) {
        val nuevo = HashSet(idsFalladas())
        nuevo.add(idDe(p))
        prefs().edit().putStringSet("falladas_ids", nuevo).apply()
    }

    private fun marcarAprendida(p: Pregunta) {
        val nuevo = HashSet(idsFalladas())
        nuevo.remove(idDe(p))
        prefs().edit().putStringSet("falladas_ids", nuevo).apply()
    }

    private fun empezarRepaso() {
        val ids = idsFalladas()
        val lista = todas.filter { ids.contains(idDe(it)) }
        if (lista.isEmpty()) {
            Toast.makeText(this, "Todavía no tenés preguntas para repasar", Toast.LENGTH_SHORT).show()
            return
        }
        modoSuper = false
        modoContra = false
        modoRepaso = true
        catElegida = "Mezcla"
        difElegida = 0
        ronda = lista.shuffled()
        indice = 0
        puntos = 0
        vidas = 3
        racha = 0
        cincuenta = 2
        saltos = 1
        aciertosPartida = 0
        partidaXp = 0
        partidaMonedas = 0
        falladas.clear()
        mostrarPregunta()
    }

    private fun mostrarFinRepaso() {
        pantalla = "fin"
        contenedor.removeAllViews()
        contenedor.translationX = 0f
        scroll.scrollTo(0, 0)
        texto("📖 Repaso terminado", 26f)
        texto("Acertaste $aciertosPartida de ${ronda.size}", 20f, 24)
        texto("Te quedan ${idsFalladas().size} pendientes", 18f, 8)
        texto("Ganaste +$partidaXp XP y +$partidaMonedas 🪙", 14f, 8)
        boton("Repasar de nuevo", Color.parseColor("#5E35B1")) { empezarRepaso() }
        boton("Volver a Jugar") { mostrarJugar() }
        animarEntrada()
    }

    private fun categoriaDelDia(): String = categorias[diaHoy() % categorias.size]

    private fun poolMisiones(): List<Mision> = listOf(
        Mision("aciertos10", "Acertá 10 preguntas", 10, 15),
        Mision("aciertos25", "Acertá 25 preguntas", 25, 30),
        Mision("racha5", "Conseguí 5 aciertos seguidos", 1, 20),
        Mision("contra", "Jugá una partida de Contrarreloj", 1, 15),
        Mision("super", "Jugá una partida de Supervivencia", 1, 15),
        Mision("perfecta", "Terminá una partida con 10 aciertos o más sin fallar", 1, 30),
        Mision("cat", "Acertá 5 preguntas de ${categoriaDelDia()}", 5, 20),
        Mision("dia", "Respondé la pregunta del día", 1, 10),
        Mision("partidas3", "Jugá 3 partidas", 3, 15)
    )

    private fun misionesHoy(): List<Mision> =
        poolMisiones().shuffled(kotlin.random.Random(diaHoy())).take(3)

    private fun asegurarDiaMisiones() {
        val hoy = diaHoy()
        if (prefs().getInt("mis_dia", -1) != hoy) {
            val e = prefs().edit()
            for (m in poolMisiones()) {
                e.remove("mis_prog_${m.id}")
                e.remove("mis_ok_${m.id}")
            }
            e.putInt("mis_dia", hoy).apply()
        }
    }

    private fun misionesCompletas(): Int {
        asegurarDiaMisiones()
        return misionesHoy().count { prefs().getBoolean("mis_ok_${it.id}", false) }
    }

    private fun progresoMision(id: String) {
        asegurarDiaMisiones()
        val m = misionesHoy().firstOrNull { it.id == id } ?: return
        if (prefs().getBoolean("mis_ok_$id", false)) return
        val nuevo = prefs().getInt("mis_prog_$id", 0) + 1
        if (nuevo >= m.meta) {
            prefs().edit()
                .putInt("mis_prog_$id", m.meta)
                .putBoolean("mis_ok_$id", true)
                .putInt("monedas", monedas() + m.premio)
                .apply()
            Toast.makeText(this, "✅ Misión cumplida: +${m.premio} 🪙", Toast.LENGTH_LONG).show()
            if (misionesCompletas() == 3) {
                prefs().edit().putInt("monedas", monedas() + 25).apply()
                Toast.makeText(this, "🎉 ¡Completaste las 3 misiones! +25 🪙 de bonus", Toast.LENGTH_LONG).show()
            }
        } else {
            prefs().edit().putInt("mis_prog_$id", nuevo).apply()
        }
    }

    private fun mostrarMisiones() {
        pantalla = "misiones"
        timer?.cancel()
        contenedor.removeAllViews()
        contenedor.translationX = 0f
        scroll.scrollTo(0, 0)
        asegurarDiaMisiones()
        texto("🎯 Misiones del día", 28f)
        texto("Se renuevan todos los días. Si cumplís las 3, ganás +25 🪙 de bonus.", 13f, 8)
        for (m in misionesHoy()) {
            val ok = prefs().getBoolean("mis_ok_${m.id}", false)
            val prog = prefs().getInt("mis_prog_${m.id}", 0)
            val t = texto((if (ok) "✅ " else "⬜ ") + m.desc, 18f, 20)
            if (ok) {
                t.setTextColor(Color.parseColor("#66BB6A"))
            }
            texto("Progreso: ${minOf(prog, m.meta)} / ${m.meta}   ·   Premio: ${m.premio} 🪙", 13f, 2)
        }
        boton("Volver") { mostrarCategorias() }
        animarEntrada()
    }

    private fun colorContra(ms: Long): Int {
        if (ms > 20000L) return Color.parseColor("#43A047")
        if (ms > 10000L) return Color.parseColor("#FFB300")
        return Color.parseColor("#E53935")
    }

    private fun empezarContra() {
        if (todas.isEmpty()) return
        modoSuper = false
        modoRepaso = false
        modoContra = true
        catElegida = "Mezcla"
        difElegida = 0
        ronda = todas.shuffled()
        indice = 0
        puntos = 0
        racha = 0
        aciertosPartida = 0
        erroresContra = 0
        partidaXp = 0
        partidaMonedas = 0
        falladas.clear()
        restanteContra = duracionContra
        mostrarPreguntaContra()
    }

    private fun mostrarPreguntaContra() {
        if (!modoContra) return
        pantalla = "contra"
        if (indice >= ronda.size) {
            ronda = todas.shuffled()
            indice = 0
        }
        respondida = false
        val p = ronda[indice]
        contenedor.removeAllViews()
        contenedor.translationX = 0f
        scroll.scrollTo(0, 0)

        tiempoContraView = texto("", 18f)
        barraContra = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal)
        barraContra.max = duracionContra.toInt()
        val lpBarra = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(10))
        lpBarra.topMargin = 8
        barraContra.layoutParams = lpBarra
        contenedor.addView(barraContra)

        texto("${conEmoji(p.categoria)} · ${nombresDif[p.dificultad - 1]}", 14f, 12)
            .setTextColor(colorDe(p.categoria))
        texto(p.texto, 24f, 24)

        val correctaTexto = p.opciones[p.correcta]
        val botones = ArrayList<Button>()
        for (op in p.opciones.shuffled()) {
            val b = boton(op, Color.LTGRAY, Color.BLACK) { }
            b.setOnClickListener { responderContra(b, op == correctaTexto, p, botones, correctaTexto) }
            botones.add(b)
        }
        actualizarContraUI()
        iniciarTimerContra()
    }

    private fun actualizarContraUI() {
        tiempoContraView.text = "⏱️ ${(restanteContra + 999) / 1000} s   ·   Puntos: $puntos   ·   Aciertos: $aciertosPartida"
        barraContra.progress = restanteContra.toInt()
        barraContra.progressTintList = ColorStateList.valueOf(colorContra(restanteContra))
    }

    private fun iniciarTimerContra() {
        timerContra?.cancel()
        if (restanteContra <= 0L) return
        timerContra = object : CountDownTimer(restanteContra, 100L) {
            override fun onTick(ms: Long) {
                restanteContra = ms
                actualizarContraUI()
            }

            override fun onFinish() {
                restanteContra = 0L
                terminarContra()
            }
        }
        timerContra?.start()
    }

    private fun responderContra(
        elegido: Button,
        acierto: Boolean,
        p: Pregunta,
        botones: List<Button>,
        correctaTexto: String
    ) {
        if (respondida || !modoContra) return
        respondida = true
        if (acierto) {
            racha++
            val bonus = if (racha >= 3) 1 else 0
            val ganados = p.dificultad + bonus
            puntos += ganados
            registrarAcierto(p)
            darRecompensa(ganados)
            elegido.background = fondo(Color.GREEN)
            sonar(sonidoOk)
            vibrar(30L)
        } else {
            racha = 0
            erroresContra++
            falladas.add(p)
            marcarFallada(p)
            elegido.background = fondo(Color.RED)
            for (b in botones) {
                if (b.text.toString() == correctaTexto) {
                    b.background = fondo(Color.GREEN)
                }
            }
            sonar(sonidoMal)
            vibrar(150L)
            timerContra?.cancel()
            restanteContra = maxOf(0L, restanteContra - 3000L)
            if (restanteContra <= 0L) {
                handler.postDelayed({
                    if (modoContra) {
                        terminarContra()
                    }
                }, 450L)
                return
            }
            iniciarTimerContra()
        }
        handler.postDelayed({
            if (modoContra && pantalla == "contra") {
                indice++
                mostrarPreguntaContra()
            }
        }, 450L)
    }

    private fun terminarContra() {
        timerContra?.cancel()
        modoContra = false
        pantalla = "fincontra"
        contenedor.removeAllViews()
        contenedor.translationX = 0f
        scroll.scrollTo(0, 0)
        val anterior = prefs().getInt("record_contra", 0)
        val nuevo = puntos > anterior
        if (nuevo) {
            prefs().edit().putInt("record_contra", puntos).apply()
        }
        desbloquear("primera")
        progresoMision("contra")
        progresoMision("partidas3")
        texto("⏱️ ¡Se acabó el tiempo!", 26f)
        texto("Puntos: $puntos", 22f, 24)
        texto("Aciertos: $aciertosPartida  ·  Errores: $erroresContra", 18f, 8)
        texto(if (nuevo) "🏆 ¡Nuevo récord!" else "Tu récord: $anterior", 18f, 8)
        texto("Ganaste +$partidaXp XP y +$partidaMonedas 🪙", 14f, 8)
        boton("Jugar de nuevo", colorDe("Contrarreloj")) { empezarContra() }
        boton("📤 Compartir mi puntaje", Color.parseColor("#2E7D32")) {
            compartirTexto("Hice $puntos puntos en 60 segundos en Trivia Max ⏱️ ¿Me ganás?")
        }
        boton("Volver a Jugar") { mostrarJugar() }
        if (falladas.isNotEmpty()) {
            texto("Para repasar", 22f, 40)
            for (f in falladas) {
                texto("✗ ${f.texto}", 16f, 24)
                texto("Respuesta correcta: ${f.opciones[f.correcta]}", 16f, 4)
                    .setTextColor(Color.parseColor("#43A047"))
            }
        }
        animarEntrada()
    }

    private fun mostrarFin() {
        pantalla = "fin"
        if (modoRepaso) {
            mostrarFinRepaso()
            return
        }
        contenedor.removeAllViews()
        contenedor.translationX = 0f
        scroll.scrollTo(0, 0)
        val clave = if (modoSuper) "record_super" else "record_${catElegida}_$difElegida"
        val anterior = prefs().getInt(clave, 0)
        val nuevo = puntos > anterior
        if (nuevo) {
            prefs().edit().putInt(clave, puntos).apply()
        }
        logrosFinPartida()
        progresoMision("partidas3")
        if (modoSuper) progresoMision("super")
        if (aciertosPartida >= 10 && falladas.isEmpty()) progresoMision("perfecta")
        texto(if (vidas <= 0) "Te quedaste sin vidas" else "¡Terminaste las preguntas!", 26f)
        texto("Puntos: $puntos", 22f, 24)
        if (modoSuper) {
            texto("Aciertos: $aciertosPartida", 18f, 8)
        }
        texto("Ganaste +$partidaXp XP y +$partidaMonedas 🪙", 14f, 8)
        texto(if (nuevo) "🏆 ¡Nuevo récord!" else "Tu récord: $anterior", 18f, 8)
        boton("Jugar de nuevo", colorDe(catElegida)) { empezar() }
        boton("📤 Compartir mi puntaje", Color.parseColor("#2E7D32")) {
            val modoTxt = if (modoSuper) "Supervivencia" else catElegida
            compartirTexto("Hice $puntos puntos en Trivia Max ($modoTxt) 🔥 ¿Me ganás? ¡A jugar y ganar!")
        }
        boton("Elegir otro modo o categoría") { mostrarJugar() }

        if (falladas.isNotEmpty()) {
            texto("Para repasar", 22f, 40)
            for (f in falladas) {
                texto("✗ ${f.texto}", 16f, 24)
                texto("Respuesta correcta: ${f.opciones[f.correcta]}", 16f, 4)
                    .setTextColor(Color.parseColor("#43A047"))
            }
        }
        animarEntrada()
    }
}

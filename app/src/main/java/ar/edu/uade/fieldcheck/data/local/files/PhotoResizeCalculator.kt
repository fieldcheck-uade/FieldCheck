package ar.edu.uade.fieldcheck.data.local.files

/**
 * Cálculos de tamaño para reducir las fotos (RNF07). Kotlin puro, sin Android,
 * para poder probarlos con tests unitarios.
 */
object PhotoResizeCalculator {

    /** Lado mayor máximo de una foto guardada, en píxeles. */
    const val MAX_SIDE_PX = 1600

    /** Calidad JPEG (0–100). 80 casi no se distingue a simple vista y pesa mucho menos. */
    const val JPEG_QUALITY = 80

    /**
     * Factor de submuestreo para decodificar la foto (`BitmapFactory.Options.inSampleSize`).
     *
     * Es la mayor potencia de 2 que deja el lado mayor en [maxSide] o más. Decodificar ya
     * reducido evita cargar en memoria una foto de 12 MP entera (unos 48 MB) solo para achicarla.
     */
    fun sampleSize(width: Int, height: Int, maxSide: Int = MAX_SIDE_PX): Int {
        val longestSide = maxOf(width, height)
        var sampleSize = 1
        while (longestSide / (sampleSize * 2) >= maxSide) {
            sampleSize *= 2
        }
        return sampleSize
    }

    /**
     * Tamaño final de la foto: el lado mayor queda en [maxSide] y se mantiene la proporción.
     * Si la foto ya es más chica, no se agranda.
     */
    fun targetSize(width: Int, height: Int, maxSide: Int = MAX_SIDE_PX): Pair<Int, Int> {
        val longestSide = maxOf(width, height)
        if (longestSide <= maxSide) {
            return Pair(width, height)
        }
        val scale = maxSide.toDouble() / longestSide
        val targetWidth = maxOf(1, Math.round(width * scale).toInt())
        val targetHeight = maxOf(1, Math.round(height * scale).toInt())
        return Pair(targetWidth, targetHeight)
    }
}

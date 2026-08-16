package com.ame38.autocalendaralarms

// dark-leaning greys so white tag text stays readable - kept separate from
// the real per-calendar colors used for the "Colors" category and swatches
private val GREY_SHADES = intArrayOf(
    0xFF1F1F1F.toInt(),
    0xFF2E2E2E.toInt(),
    0xFF3D3D3D.toInt(),
    0xFF4C4C4C.toInt(),
    0xFF5B5B5B.toInt(),
    0xFF666666.toInt()
)

// same calendar always maps to the same shade, whether it's drawn in the
// filter row or as the tag under an individual event
fun calendarTagColor(calendarId: Long): Int {
    val index = ((calendarId % GREY_SHADES.size) + GREY_SHADES.size) % GREY_SHADES.size
    return GREY_SHADES[index.toInt()]
}

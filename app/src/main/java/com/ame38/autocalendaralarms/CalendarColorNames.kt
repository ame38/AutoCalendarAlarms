package com.ame38.autocalendaralarms

// Google's published "calendar" and "event" color palettes (colorId -> hex),
// merged into one hex -> name lookup since CALENDAR_COLOR/EVENT_COLOR from
// the provider can come from either palette depending on the account/sync source.
private val GOOGLE_COLOR_NAMES: Map<Int, String> = mapOf(
    // calendar palette
    0xFFAC725E.toInt() to "Cocoa",
    0xFFD06B64.toInt() to "Flamingo",
    0xFFF83A22.toInt() to "Tomato",
    0xFFFA573C.toInt() to "Tangerine",
    0xFFFF7537.toInt() to "Pumpkin",
    0xFFFFAD46.toInt() to "Mango",
    0xFF42D692.toInt() to "Eucalyptus",
    0xFF16A765.toInt() to "Basil",
    0xFF7BD148.toInt() to "Pistachio",
    0xFFB3DC6C.toInt() to "Avocado",
    0xFFFBE983.toInt() to "Citron",
    0xFFFAD165.toInt() to "Banana",
    0xFF92E1C0.toInt() to "Sage",
    0xFF9FE1E7.toInt() to "Peacock",
    0xFF9FC6E7.toInt() to "Cobalt",
    0xFF4986E7.toInt() to "Blueberry",
    0xFF9A9CFF.toInt() to "Lavender",
    0xFFB99AFF.toInt() to "Wisteria",
    0xFFC2C2C2.toInt() to "Graphite",
    0xFFCABDBF.toInt() to "Birch",
    0xFFCCA6AC.toInt() to "Beetroot",
    0xFFF691B2.toInt() to "Cherry Blossom",
    0xFFCD74E6.toInt() to "Grape",
    0xFFA47AE2.toInt() to "Amethyst",
    // event palette
    0xFFA4BDFC.toInt() to "Lavender",
    0xFF7AE7BF.toInt() to "Sage",
    0xFFDBADFF.toInt() to "Grape",
    0xFFFF887C.toInt() to "Flamingo",
    0xFFFBD75B.toInt() to "Banana",
    0xFFFFB878.toInt() to "Tangerine",
    0xFF46D6DB.toInt() to "Peacock",
    0xFFE1E1E1.toInt() to "Graphite",
    0xFF5484ED.toInt() to "Blueberry",
    0xFF51B749.toInt() to "Basil",
    0xFFDC2127.toInt() to "Tomato"
)

// falls back to the raw hex when a color doesn't match Google's known palette
// (e.g. a non-Google account synced with a custom color)
fun googleColorName(color: Int): String =
    GOOGLE_COLOR_NAMES[color] ?: String.format("#%06X", 0xFFFFFF and color)

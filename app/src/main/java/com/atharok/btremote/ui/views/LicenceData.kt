package com.atharok.btremote.ui.views

/** One piece of other people's work that ships inside the app (or is fetched by it), and its licence. */
data class Library(
    val name: String,
    val by: String,
    val licence: String,
    val link: String,
)

/** The address of the app's own source code. */
const val SOURCE_URL = "https://github.com/wanderwildwood/enkaku"

/** Everything the APK carries, or fetches, that this project did not write. */
val LIBRARIES: List<Library> =
    listOf(
        Library("BT Remote, the app this began from", "Atharok", "GPL-3.0-or-later", "https://gitlab.com/Atharok/BtRemote"),
        Library("Mudita Mindful Design (MMD)", "Mudita", "Apache-2.0", "https://github.com/mudita/MMD"),
        Library("Lato typeface, inside MMD", "tyPoland Lukasz Dziedzic", "OFL-1.1", "https://www.latofonts.com"),
        Library("Jetpack Compose, Activity, Lifecycle, Core, Navigation, DataStore, WorkManager and Glance", "Google", "Apache-2.0", "https://developer.android.com/jetpack"),
        Library("Koin", "Kotzilla and contributors", "Apache-2.0", "https://insert-koin.io"),
        Library("Kotlin standard library, coroutines and serialization", "JetBrains", "Apache-2.0", "https://kotlinlang.org"),
        Library("Material icons", "Google", "Apache-2.0", "https://fonts.google.com/icons"),
    )

/**
 * A licence text as the paragraphs a reader turns pages by: the texts are wrapped at about
 * seventy columns with a blank line between paragraphs; each paragraph is joined into one row.
 */
fun licenceParagraphs(text: String): List<String> =
    text
        .replace("\r\n", "\n")
        .split(Regex("\n[ \t]*\n"))
        .map { block -> block.lines().joinToString(" ") { it.trim() }.trim() }
        .filter { it.isNotEmpty() }

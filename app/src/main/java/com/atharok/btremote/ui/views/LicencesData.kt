package com.atharok.btremote.ui.views

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.atharok.btremote.R
import com.atharok.btremote.ui.components.LicenceComponent
import com.atharok.btremote.ui.components.LicenceText
import com.atharok.btremote.ui.components.LicencesData
import com.atharok.btremote.ui.components.LicencesLabels

/** The address of the app's own source code. */
const val SOURCE_URL = "https://github.com/wanderwildwood/enkaku"

/** What the APK carries that is not this app's own code, and the licence each project declares. */
val LICENCE_COMPONENTS: List<LicenceComponent> = listOf(
    LicenceComponent("BT Remote, the app this began from", "Atharok", "GPL-3.0-or-later", "https://gitlab.com/Atharok/BtRemote"),
    LicenceComponent("Mudita Mindful Design (MMD)", "Mudita", "Apache-2.0", "https://github.com/mudita/MMD"),
    LicenceComponent("Lato typeface, inside MMD", "tyPoland Lukasz Dziedzic", "OFL-1.1", "https://www.latofonts.com"),
    LicenceComponent("Jetpack Compose, Activity, Lifecycle, Core, Navigation, DataStore, WorkManager and Glance", "Google", "Apache-2.0", "https://developer.android.com/jetpack"),
    LicenceComponent("Koin", "Kotzilla and contributors", "Apache-2.0", "https://insert-koin.io"),
    LicenceComponent("Kotlin standard library, coroutines and serialization", "JetBrains", "Apache-2.0", "https://kotlinlang.org"),
    LicenceComponent("Material icons", "Google", "Apache-2.0", "https://fonts.google.com/icons"),
)

/** Each distinct licence text once: family id and the raw resource. */
val LICENCE_RAW: List<Pair<String, Int>> = listOf(
    "gpl" to R.raw.licence_gpl3,
    "apache" to R.raw.licence_apache2,
    "ofl" to R.raw.licence_ofl,
)

@Composable
fun rememberLicencesData(): LicencesData = LicencesData(
    appName = "Remote",
    sourceUrl = SOURCE_URL,
    licenceLine = stringResource(R.string.licences_licence),
    components = LICENCE_COMPONENTS,
    texts = LICENCE_RAW.map { (id, raw) ->
        LicenceText(id, stringResource(when (id) {
            "gpl" -> R.string.licences_full_gpl
            "apache" -> R.string.licences_full_apache
            "ofl" -> R.string.licences_full_ofl
            else -> R.string.licences_full_head
        }), raw)
    },
)

@Composable
fun rememberLicencesLabels(): LicencesLabels {
    val none = stringResource(R.string.about_no_browser_address, "@@")
    val noText = stringResource(R.string.licences_no_text, "@@")
    return LicencesLabels(
        title = stringResource(R.string.licences_title),
        back = stringResource(R.string.licences_back),
        sourceHeading = stringResource(R.string.licences_source_head),
        noText = { licence -> noText.replace("@@", licence) },
        noBrowser = { address -> none.replace("@@", address) },
    )
}

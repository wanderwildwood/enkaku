package com.atharok.btremote.ui.views

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.atharok.btremote.BuildConfig
import com.atharok.btremote.R
import com.atharok.btremote.ui.components.AboutData
import com.atharok.btremote.ui.components.AboutLabels
import com.atharok.btremote.ui.components.AboutSection
import com.atharok.btremote.ui.components.HouseAboutDialog

/**
 * This app's About: only the words and the address. The dialog, its drawers, its one Close button,
 * the Licenses drawer, the links and the llama are the shared HouseAbout.kt, the same in every app.
 *
 * The lines about what it sends are here because an app that pairs with the television and types
 * into it is exactly the kind a stranger should ask about before trusting it.
 */
@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val data = AboutData(
        title = stringResource(R.string.about_title, BuildConfig.VERSION_NAME),
        sections = listOf(
            AboutSection(null, stringResource(R.string.about_what)),
            AboutSection(stringResource(R.string.about_privacy_heading), stringResource(R.string.about_privacy_1)),
            AboutSection(null, stringResource(R.string.about_privacy_2)),
            AboutSection(null, stringResource(R.string.about_privacy_3)),
            AboutSection(null, stringResource(R.string.about_privacy_4)),
            AboutSection(null, stringResource(R.string.about_privacy_5)),
            AboutSection(stringResource(R.string.privacy_who_title), stringResource(R.string.privacy_who)),
            AboutSection(stringResource(R.string.privacy_contact_title), stringResource(R.string.privacy_contact)),
            AboutSection(stringResource(R.string.privacy_rights_title), stringResource(R.string.privacy_rights)),
            AboutSection(stringResource(R.string.about_terms_heading), stringResource(R.string.about_terms)),
            AboutSection(null, stringResource(R.string.about_after)),
            AboutSection(null, stringResource(R.string.about_bundled)),
        ),
        sourceUrl = "https://" + stringResource(R.string.about_source),
    )
    val labels = AboutLabels(
        close = stringResource(R.string.about_close),
        licences = stringResource(R.string.licences_title),
        feedTheLlamas = stringResource(R.string.about_feed_the_llamas),
        noBrowser = { address -> context.getString(R.string.about_no_browser_address, address) },
        writeTo = { address -> context.getString(R.string.about_write_to, address) },
        noMailApp = { address -> context.getString(R.string.about_no_mail_app, address) },
        openDrawer = { heading -> context.getString(R.string.about_open_drawer, heading) },
        closeDrawer = { heading -> context.getString(R.string.about_close_drawer, heading) },
        intro = stringResource(R.string.about_intro_head),
        contact = stringResource(R.string.about_contact_head),
        links = stringResource(R.string.about_links_head),
    )
    HouseAboutDialog(data, labels, R.drawable.llama, onDismiss)
}

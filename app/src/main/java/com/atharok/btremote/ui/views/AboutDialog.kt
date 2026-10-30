package com.atharok.btremote.ui.views

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mudita.mmd.components.buttons.OutlinedButtonMMD
import com.mudita.mmd.components.text.TextMMD
import com.atharok.btremote.BuildConfig
import com.atharok.btremote.ui.theme.EInkDialog
import android.content.Context
import android.content.Intent
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextDecoration
import com.atharok.btremote.ui.components.PagedColumn
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.atharok.btremote.R

/**
 * What this is, what it does with what it can see, and whose work it started from.
 *
 * The line about what it sends is here because an app that pairs with the television and types
 * into it is exactly the kind a stranger should ask about before trusting it.
 */
@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    // The words run past one screen, so they turn in pages like every list here; the dialog is
    // a fixed share of the screen so the page is always the same size.
    val pageHeight = (LocalConfiguration.current.screenHeightDp * 0.62f).dp
    EInkDialog(onDismiss = onDismiss) {
        TextMMD(
            text = stringResource(R.string.about_title, BuildConfig.VERSION_NAME),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
        )

        Spacer(Modifier.height(8.dp))
        PagedColumn(modifier = Modifier.fillMaxWidth().height(pageHeight)) {
            // A heading and its words are one row, so a heading never ends a page alone.
            item {
                Column {
                    AboutHeading(R.string.about_privacy_heading)
                    AboutParagraph(stringResource(R.string.about_privacy_1))
                }
            }
            // The rest of the Privacy words, a paragraph to a row, so a page ends on a whole one.
            item { AboutParagraph(stringResource(R.string.about_privacy_2)) }
            item { AboutParagraph(stringResource(R.string.about_privacy_3)) }
            item { AboutParagraph(stringResource(R.string.about_privacy_4)) }
            item { AboutParagraph(stringResource(R.string.about_privacy_5)) }
            item {
                Column {
                    AboutHeading(R.string.about_terms_heading)
                    AboutParagraph(stringResource(R.string.about_terms))
                }
            }
            item {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    TextMMD(text = stringResource(R.string.about_after), style = MaterialTheme.typography.labelSmall)
                    TextMMD(text = stringResource(R.string.about_bundled), style = MaterialTheme.typography.labelSmall)
                }
            }
            item {
                // The source, one press from the code the licence promises.
                AboutLink(
                    text = stringResource(R.string.about_source),
                    url = "https://" + stringResource(R.string.about_source),
                    context = context,
                )
            }
            item { LicencesLink() }
            item { Llama() }
        }

        Spacer(Modifier.height(12.dp))
        OutlinedButtonMMD(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth().height(48.dp),
        ) { TextMMD(text = stringResource(R.string.about_close), style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
private fun AboutHeading(@StringRes text: Int) {
    TextMMD(
        text = stringResource(text),
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
    )
}

@Composable
private fun AboutParagraph(text: String) {
    TextMMD(text = text, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(bottom = 6.dp))
}

/** A web address that opens when pressed, in a row 48 dp tall. A phone with no browser gets a word, not a crash. */
@Composable
private fun AboutLink(text: String, url: String, context: Context) {
    TextMMD(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        textDecoration = TextDecoration.Underline,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable { openLink(context, url, context.getString(R.string.about_no_browser_address, url.removePrefix("https://"))) }
            .wrapContentHeight(Alignment.CenterVertically),
    )
}

/**
 * A llama at the foot of the About, which opens the page a donation goes to.
 * The site's address sits at the start of the same line and opens the site; the llama and its words open the page.
 *
 * Three words rather than an address: a verb and an object, so what happens when you press
 * them is not a surprise even though the page is not named. The drawing is his own, and it is
 * ink rather than an emoji, which is a colour glyph and reaches the panel as a pale smudge.
 *
 * The Kompakt may have nothing registered for a web address, so the intent is allowed to fail
 * quietly rather than take the dialog down with it.
 */
@Composable
private fun Llama() {
    val context = LocalContext.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        TextMMD(
            text = "wanderthe.dev",
            style = MaterialTheme.typography.labelSmall,
            // The site's address opens the site, the way the llama beside it opens its page.
            modifier = Modifier
                .clickable {
                    openLink(context, "https://wanderthe.dev", context.getString(R.string.about_no_browser_address, "wanderthe.dev"))
                }
                .heightIn(min = 48.dp)
                .wrapContentHeight(Alignment.CenterVertically),
        )
        Spacer(Modifier.width(6.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clickable {
                    // Straight to the checkout. The Donate button on the site only leads
                    // here anyway, so the page in between is a press the reader does not need.
                    // The short square.link form, not the long checkout.square.site address it
                    // redirects to -- the short one is what the site itself links to, so a
                    // regenerated checkout follows it and a published app does not break.
                    openLink(context, "https://square.link/u/AGu8oT10", context.getString(R.string.about_no_browser_address, "square.link/u/AGu8oT10"))
                }
                .heightIn(min = 48.dp),
        ) {
            Image(
                painter = painterResource(R.drawable.llama),
                contentDescription = null,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(6.dp))
            TextMMD(text = stringResource(R.string.about_feed_the_llamas), style = MaterialTheme.typography.labelSmall)
        }
    }
}

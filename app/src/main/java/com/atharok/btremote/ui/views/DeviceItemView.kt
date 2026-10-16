package com.atharok.btremote.ui.views

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.atharok.btremote.R
import com.atharok.btremote.ui.components.BasicDropdownMenuItem
import com.atharok.btremote.ui.components.MoreOverflowMenu
import com.atharok.btremote.ui.components.TextMedium
import com.atharok.btremote.ui.components.TextNormal
import com.atharok.btremote.ui.components.TextNormalSecondary
import com.atharok.btremote.ui.theme.rimWidth

@Composable
fun DeviceItemView(
    name: String,
    macAddress: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.padding_large))
    ) {
        Column(
            modifier = Modifier,
            verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.padding_min))
        ) {
            TextMedium(
                text = name,
                maxLines = 1
            )
            TextNormalSecondary(
                text = macAddress,
                maxLines = 1
            )
        }
    }
}

@Composable
fun DeviceItemView(
    name: String,
    macAddress: String,
    isAutoConnectDeviceAddress: Boolean,
    autoConnect: () -> Unit,
    isFavoriteDevice: Boolean,
    onFavoriteDeviceChanged: () -> Unit,
    unpair: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        DeviceItemView(
            name, macAddress, Modifier.weight(1f)
        )

        if(isAutoConnectDeviceAddress) {
            Surface(
                modifier = Modifier.padding(
                    horizontal = dimensionResource(id = R.dimen.padding_small)
                ),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(rimWidth, MaterialTheme.colorScheme.onSurface)
            ) {
                TextNormal(
                    text = stringResource(id = R.string.automatic_shorten),
                    modifier = Modifier.padding(
                        horizontal = dimensionResource(id = R.dimen.padding_max),
                        vertical = dimensionResource(id = R.dimen.padding_small)
                    ),
                    maxLines = 1
                )
            }
        }

        Box(contentAlignment = Alignment.CenterEnd) {
            MoreOverflowMenu { closeDropdownMenu: () -> Unit ->

                // Auto Connect
                BasicDropdownMenuItem(
                    text = stringResource(id = R.string.automatic_connect),
                    onClick = {
                        closeDropdownMenu()
                        autoConnect()
                    }
                )

                // Favorite
                BasicDropdownMenuItem(
                    text = stringResource(
                        id = if(isFavoriteDevice) R.string.remove_from_favorites else R.string.add_to_favorites
                    ),
                    onClick = {
                        closeDropdownMenu()
                        onFavoriteDeviceChanged()
                    }
                )

                // Unpair
                BasicDropdownMenuItem(
                    text = stringResource(id = R.string.unpair),
                    onClick = {
                        closeDropdownMenu()
                        unpair()
                    }
                )
            }
        }
    }
}
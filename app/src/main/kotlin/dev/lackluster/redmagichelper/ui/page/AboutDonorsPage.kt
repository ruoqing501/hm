package dev.lackluster.redmagichelper.ui.page

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import dev.lackluster.hyperx.compose.base.BasePage
import dev.lackluster.hyperx.compose.base.BasePageDefaults
import dev.lackluster.hyperx.compose.base.IconSize
import dev.lackluster.hyperx.compose.base.ImageIcon
import dev.lackluster.hyperx.compose.preference.PreferenceGroup
import dev.lackluster.hyperx.compose.preference.TextPreference
import dev.lackluster.redmagichelper.R
import dev.lackluster.redmagichelper.ui.MainActivity

@Composable
fun AboutDonorsPage(navController: NavController, adjustPadding: PaddingValues, mode: BasePageDefaults.Mode) {
    BasePage(
        navController,
        adjustPadding,
        stringResource(R.string.ui_title_about_donors),
        MainActivity.blurEnabled,
        MainActivity.blurTintAlphaLight,
        MainActivity.blurTintAlphaDark,
        mode,
    ) {
        item {
            PreferenceGroup(
                title = stringResource(R.string.ui_title_about_donors),
                first = true,
                last = true
            ) {
                TextPreference(
                    icon = ImageIcon(
                        iconRes = R.drawable.xl,
                        iconSize = IconSize.App,
                        cornerRadius = 30.dp
                    ),
                    title = "杳如",
                    summary = stringResource(R.string.about_donor_amount, 60)
                )
            }
        }
    }
}

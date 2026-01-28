// Private Test Build  Not for Redistribution
/**
 * Auralis Music
 * License Screen for displaying License & Disclaimer
 */

package com.auralis.music.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.auralis.music.LocalPlayerAwareWindowInsets
import com.auralis.music.R
import com.auralis.music.ui.component.IconButton
import com.auralis.music.ui.utils.backToMain

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicenseScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    licenseType: String,
) {
    val title = when (licenseType) {
        "disclaimer" -> "Disclaimer & License Notice"
        else -> "License"
    }

    val licenseText = when (licenseType) {
        "disclaimer" -> DISCLAIMER_TEXT
        else -> ""
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(
                    WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
                )
            )
    ) {
        Spacer(
            Modifier.windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(
                    WindowInsetsSides.Top
                )
            )
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = licenseText,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )


        }
    }

    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain,
            ) {
                Icon(
                    painter = painterResource(R.drawable.arrow_back),
                    contentDescription = null,
                )
            }
        }
    )
}

private const val DISCLAIMER_TEXT = """LICENSE & DISCLAIMER NOTICE

1. Ownership & Copyright
This project, including but not limited to its source code, APK, binaries, assets, documentation, and related materials, is the exclusive intellectual property of the developer.
Copyright © [2026] [Ankit Barua].
All rights are reserved.
No ownership rights, license, or permissions are granted except as explicitly stated in writing by the developer.

2. Purpose & Intended Use
This project is developed solely for personal, private, educational, and learning purposes.
It is not intended for:
• Commercial use
• Business use
• Production deployment
• Public distribution
Any use beyond this scope is strictly unauthorized.

3. No License Grant 
No open-source, proprietary, implied, or statutory license is granted by default.
Access to the project, whether intentional or accidental, does not constitute permission to use, copy, modify, distribute, reverse-engineer, decompile, or derive works from it.

4. Unauthorized Access, Leak, or Distribution
If the source code or APK is:
• leaked,
• shared,
• uploaded,
• redistributed, or
• accessed by any third party
without the explicit written consent of the developer, such action shall be deemed unauthorized.
Any individual or entity engaging in such unauthorized access or distribution acts in their personal capacity and assumes full legal responsibility for their actions.
The developer shall not be held responsible or liable for any consequences arising from such unauthorized acts.

5. Liability Disclaimer
To the maximum extent permitted under applicable law, the project is provided "as is", without any warranties, express or implied, including but not limited to fitness for a particular purpose, merchantability, accuracy, or non-infringement.
The developer shall not be liable for:
• direct or indirect damages
• data loss
• financial loss
• system damage
• legal disputes
• regulatory violations
arising from unauthorized access, use, modification, or distribution of this project.

6. Third-Party Responsibility Clause 
Any third party who publishes, distributes, mirrors, modifies, or claims rights over this project or APK without authorization shall bear sole and exclusive responsibility for such actions, including any legal, civil, or criminal consequences.
No responsibility, endorsement, or association shall be attributed to the developer in such cases.

7. Future Licensing Clause (GPL / Open Source Safety)
The developer reserves the exclusive right to release this project under an open-source or other license only through an intentional, explicit, and documented public release.
Any future license shall:
• apply only from the date of intentional release, and
• shall not apply retroactively to unauthorized leaks or distributions.
Accidental access or leakage shall not be construed as a license grant under the GNU General Public License (GPL) or any other license.

8. Governing Law & Jurisdiction
This project and this notice shall be governed by and interpreted in accordance with the laws of India.
Any disputes arising in relation to this project shall be subject to the exclusive jurisdiction of the competent courts in India.
Where applicable internationally, this notice shall be interpreted in a manner consistent with international copyright conventions, including the Berne Convention."""








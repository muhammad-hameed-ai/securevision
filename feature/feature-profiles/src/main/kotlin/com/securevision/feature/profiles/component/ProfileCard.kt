package com.securevision.feature.profiles.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.securevision.core.model.AccessLevel
import com.securevision.core.model.EnrolledProfile
import com.securevision.core.ui.component.SVCard
import com.securevision.core.ui.theme.SecureVisionDimens
import com.securevision.core.ui.theme.SecureVisionTheme
import com.securevision.feature.profiles.R

/**
 * One enrolled person.
 *
 * The photo is the **aligned crop the embedder saw**, not a wider snapshot. That
 * is deliberate: a badly framed or rotated enrolment becomes visible here at a
 * glance, rather than only showing up later as scores that never quite match.
 *
 * @param profile The person to show.
 * @param onClick Opens the edit screen.
 * @param onDelete Requests removal; the caller confirms first.
 * @param modifier Modifier applied to the card.
 */
@Composable
fun ProfileCard(
    profile: EnrolledProfile,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SVCard(modifier = modifier, onClick = onClick) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SecureVisionDimens.spacingMedium),
        ) {
            ProfilePhoto(uri = profile.photoUri, isWatchlisted = profile.isWatchlisted)

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(SecureVisionDimens.spacingExtraSmall),
            ) {
                Text(
                    text = profile.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Text(
                    text = stringResource(R.string.profiles_age_years, profile.age),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(SecureVisionDimens.spacingSmall),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AccessLevelBadge(level = profile.accessLevel)

                    // Every profile in this table has an embedding by construction
                    // — enrolment cannot complete without one — so this states a
                    // fact rather than reporting a status that could be false.
                    Text(
                        text = stringResource(R.string.profiles_face_enrolled),
                        style = MaterialTheme.typography.labelSmall,
                        color = SecureVisionTheme.colors.known,
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Outlined.DeleteOutline,
                    contentDescription = stringResource(
                        R.string.profiles_delete_description,
                        profile.name,
                    ),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun ProfilePhoto(uri: String, isWatchlisted: Boolean) {
    Box {
        SubcomposeAsyncImage(
            model = uri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(PHOTO_SIZE)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            // A missing file must not leave a blank hole: enrolment photos live in
            // internal storage and can be cleared by "clear app data" while the
            // row survives in a backup-restored database.
            error = {
                Icon(
                    imageVector = Icons.Outlined.Person,
                    contentDescription = null,
                    modifier = Modifier.padding(SecureVisionDimens.spacingSmall),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
        )

        if (isWatchlisted) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = stringResource(R.string.profiles_watchlisted),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(WATCHLIST_BADGE_SIZE)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(2.dp),
                tint = SecureVisionTheme.colors.motion,
            )
        }
    }
}

/**
 * The operator's classification.
 *
 * Colour-coded, but never colour alone — the level is always spelled out, because
 * a badge distinguished only by hue is unreadable to a colour-blind operator and
 * meaningless in a monochrome screenshot pasted into an incident report.
 */
@Composable
private fun AccessLevelBadge(level: AccessLevel) {
    val palette = SecureVisionTheme.colors

    val tint: Color = when (level) {
        AccessLevel.STANDARD -> MaterialTheme.colorScheme.onSurfaceVariant
        AccessLevel.RESTRICTED -> palette.unknown
        AccessLevel.VIP -> palette.motion
    }

    val label = stringResource(
        when (level) {
            AccessLevel.STANDARD -> R.string.profiles_access_standard
            AccessLevel.RESTRICTED -> R.string.profiles_access_restricted
            AccessLevel.VIP -> R.string.profiles_access_vip
        },
    )

    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = tint,
        modifier = Modifier
            .clip(MaterialTheme.shapes.extraSmall)
            .background(tint.copy(alpha = BADGE_TINT_ALPHA))
            .padding(horizontal = 6.dp, vertical = 1.dp),
    )
}

private val PHOTO_SIZE = 56.dp
private val WATCHLIST_BADGE_SIZE = 18.dp
private const val BADGE_TINT_ALPHA = 0.16f

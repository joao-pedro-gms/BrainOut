// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
@file:Suppress("NewApi") // java.time.* precisa de API 26.

package pucgo.joaopedrogmsilva.brainout.feature.projects.ui.projectdetail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.CancellationException
import pucgo.joaopedrogmsilva.brainout.core.domain.usecase.DeadlineInfo
import pucgo.joaopedrogmsilva.brainout.core.ui.components.NeoActionButton
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.BrainOutTheme
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.LocalNeoColors
import pucgo.joaopedrogmsilva.brainout.core.ui.theme.NeoSizes
import pucgo.joaopedrogmsilva.brainout.feature.projects.R
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/** DatePicker representa datas em UTC, mas Task armazena o início do dia LOCAL. */
internal fun pickerDateToDeadline(
    millis: Long,
    zone: ZoneId = ZoneId.systemDefault(),
): Instant =
    Instant
        .ofEpochMilli(millis)
        .atZone(ZoneOffset.UTC)
        .toLocalDate()
        .atStartOfDay(zone)
        .toInstant()

internal fun deadlineToPickerDate(
    deadline: Instant,
    zone: ZoneId = ZoneId.systemDefault(),
): Long =
    deadline
        .atZone(zone)
        .toLocalDate()
        .atStartOfDay(ZoneOffset.UTC)
        .toInstant()
        .toEpochMilli()

@Composable
internal fun DeadlineField(
    dueDate: Instant?,
    onDateChange: (Instant?) -> Unit,
    evaluateDeadline: suspend (Instant?) -> DeadlineInfo,
) {
    var showPicker by rememberSaveable { mutableStateOf(false) }
    val pattern = stringResource(R.string.project_detail_new_task_due_date_format)
    val dateText = dueDate?.atZone(ZoneId.systemDefault())?.format(DateTimeFormatter.ofPattern(pattern))
    Column {
        OutlinedButton(
            onClick = { showPicker = true },
            // E4.4: 48dp mínimo para área de toque (WCAG 2.5.5) —
            // o `OutlinedButton` padrão do M3 marca 40dp.
            // Com prazo definido o campo vira bloco azul: a data passa a
            // ser visível de longe, e o azul informa (não avisa).
            colors =
                if (dueDate != null) {
                    ButtonDefaults.outlinedButtonColors(
                        containerColor = LocalNeoColors.current.infoContainer,
                        contentColor = LocalNeoColors.current.infoText,
                    )
                } else {
                    ButtonDefaults.outlinedButtonColors()
                },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = NeoSizes.touchMin)
                    .testTag(ProjectDetailTestTags.NEW_TASK_DUE_DATE_FIELD),
        ) {
            Text(
                stringResource(R.string.project_detail_new_task_due_date_label) + ": " +
                    (dateText ?: stringResource(R.string.project_detail_new_task_due_date_clear)),
            )
        }
        if (dueDate != null) {
            TextButton(
                onClick = { onDateChange(null) },
                // E4.4: 48dp mínimo para área de toque (WCAG 2.5.5).
                modifier =
                    Modifier
                        .heightIn(min = NeoSizes.touchMin)
                        .testTag("deadline_clear"),
            ) {
                Text(stringResource(R.string.project_detail_new_task_due_date_clear))
            }
            DeadlineHint(dueDate, evaluateDeadline)
        }
    }
    if (showPicker) {
        DeadlinePicker(dueDate, onDismiss = { showPicker = false }) {
            onDateChange(it)
            showPicker = false
        }
    }
}

@Composable
private fun DeadlineHint(
    dueDate: Instant,
    evaluateDeadline: suspend (Instant?) -> DeadlineInfo,
) {
    // Estado atrelado ao prazo: nunca mostra o resultado antigo durante nova consulta.
    var info by remember(dueDate) { mutableStateOf<DeadlineInfo?>(null) }
    var unavailable by remember(dueDate) { mutableStateOf(false) }
    LaunchedEffect(dueDate) {
        try {
            info = evaluateDeadline(dueDate)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            unavailable = true
        }
    }
    if (unavailable) Text(stringResource(R.string.deadline_holidays_unavailable))
    val holiday = info?.nextHoliday
    if (holiday != null) {
        // DEF-50 — o padrão curto vem do `strings.xml`: com "dd/MM"
        // fixo, a dica de feriado mostrava 25/12 ao lado de um campo
        // que em `en` mostrava 12/25/2026.
        val shortPattern = stringResource(R.string.project_detail_new_task_due_date_format_short)
        Text(
            stringResource(
                R.string.project_detail_new_task_holiday_hint,
                holiday.name,
                holiday.date.format(DateTimeFormatter.ofPattern(shortPattern)),
            ),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.testTag(ProjectDetailTestTags.NEW_TASK_HOLIDAY_HINT),
        )
    }
    if (info?.isBusinessDay == false) Text(stringResource(R.string.deadline_not_business_day))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeadlinePicker(
    dueDate: Instant?,
    onDismiss: () -> Unit,
    onConfirm: (Instant) -> Unit,
) {
    val state = rememberDatePickerState(initialSelectedDateMillis = dueDate?.let { deadlineToPickerDate(it) })
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            NeoActionButton(
                text = stringResource(R.string.project_detail_new_task_save),
                enabled = state.selectedDateMillis != null,
                onClick = { state.selectedDateMillis?.let { onConfirm(pickerDateToDeadline(it)) } },
                modifier = Modifier.testTag("deadline_picker_confirm"),
            )
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                // E4.4: 48dp mínimo para área de toque (WCAG 2.5.5).
                modifier = Modifier.heightIn(min = NeoSizes.touchMin),
            ) { Text(stringResource(R.string.project_detail_new_task_cancel)) }
        },
    ) { DatePicker(state = state) }
}

@Preview(showBackground = true)
@Composable
private fun DeadlineFieldPreview() {
    BrainOutTheme {
        androidx.compose.material3.Surface {
            DeadlineField(
                dueDate = null,
                onDateChange = {},
                evaluateDeadline = { DeadlineInfo(isBusinessDay = true, nextHoliday = null) },
            )
        }
    }
}

package com.github.avbox.osc.ui.page

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.avbox.osc.R
import com.github.avbox.osc.ui.components.AppTopBarScaffold
import com.github.avbox.osc.ui.components.TopBarActionBox
import com.github.avbox.osc.ui.components.SettingsCard
import com.github.avbox.osc.ui.components.SettingsCardPosition
import com.github.avbox.osc.ui.components.SettingsGroup
import com.github.avbox.osc.ui.components.SettingsOptionMenuRow
import com.github.avbox.osc.ui.components.SettingsSwitchRow
import com.github.avbox.osc.ui.components.SettingsTestButton
import com.github.avbox.osc.ui.components.SettingsTextFieldRow

private val SectionSpacing = 28.dp

private val CardContentPadding = 20.dp

private val FieldVerticalPadding = 4.dp

private val TestButtonSpacing = 12.dp

@Composable
fun ConfigTmdbScreen(
    state: ConfigTmdbState,
    actions: ConfigTmdbActions,
    testing: TmdbTest,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 24.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(SectionSpacing),
    ) {
        SettingsGroup(title = null) {
            SettingsCard(SettingsCardPosition.FIRST) {
                SettingsSwitchRow(
                    title = stringResource(R.string.tmdb_enable),
                    leadingIconRes = if (state.enabled) {
                        R.drawable.ic_config_tmdb_on
                    } else {
                        R.drawable.ic_config_tmdb_off
                    },
                    checked = state.enabled,
                    onCheckedChange = actions.setEnabled,
                )
            }
            SettingsCard(SettingsCardPosition.LAST) {
                SettingsOptionMenuRow(
                    title = stringResource(R.string.tmdb_poster_style),
                    leadingIconRes = R.drawable.ic_config_poster_style,
                    valueText = stringResource(state.posterStyle.labelRes()),
                    options = TmdbPosterStyle.entries.map { stringResource(it.labelRes()) },
                    selectedIndex = state.posterStyle.ordinal,
                    onSelect = { index -> actions.setPosterStyle(TmdbPosterStyle.of(index)) },
                )
            }
        }
        SettingsGroup(title = null) {
            SettingsCard(SettingsCardPosition.FIRST) {
                TmdbTextField(
                    label = stringResource(R.string.tmdb_api_key),
                    value = state.apiKey,
                    onValueChange = actions.setApiKey,
                    masked = true,
                    clearable = true,
                )
            }
            SettingsCard(SettingsCardPosition.MIDDLE) {
                TmdbTextField(
                    label = stringResource(R.string.tmdb_api_base),
                    value = state.apiBase,
                    onValueChange = actions.setApiBase,
                    placeholder = stringResource(R.string.tmdb_field_optional),
                    errorText = if (state.apiBaseError) {
                        stringResource(R.string.tmdb_api_base_invalid)
                    } else {
                        null
                    },
                    onFocusLost = actions.commitApiBase,
                )
            }
            SettingsCard(SettingsCardPosition.MIDDLE) {
                TmdbTextField(
                    label = stringResource(R.string.tmdb_image_base),
                    value = state.imageBase,
                    onValueChange = actions.setImageBase,
                    placeholder = stringResource(R.string.tmdb_field_optional),
                    errorText = if (state.imageBaseError) {
                        stringResource(R.string.tmdb_api_base_invalid)
                    } else {
                        null
                    },
                    onFocusLost = actions.commitImageBase,
                )
            }
            SettingsCard(SettingsCardPosition.LAST) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = CardContentPadding, vertical = TestButtonSpacing),
                    horizontalArrangement = Arrangement.spacedBy(TestButtonSpacing),
                ) {
                    SettingsTestButton(
                        text = stringResource(
                            if (testing == TmdbTest.Api) R.string.tmdb_test_running else R.string.tmdb_test_api,
                        ),
                        onClick = actions.testApi,
                        loading = testing == TmdbTest.Api,
                        modifier = Modifier.weight(1f),
                    )
                    SettingsTestButton(
                        text = stringResource(
                            if (testing == TmdbTest.Image) R.string.tmdb_test_running else R.string.tmdb_test_image,
                        ),
                        onClick = actions.testImage,
                        loading = testing == TmdbTest.Image,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun TmdbTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String? = null,
    errorText: String? = null,
    masked: Boolean = false,
    clearable: Boolean = false,
    onFocusLost: () -> Unit = {},
) {
    var focused by remember { mutableStateOf(false) }
    SettingsTextFieldRow(
        label = label,
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .padding(horizontal = CardContentPadding, vertical = FieldVerticalPadding)
            .onFocusChanged { focusState ->
                if (focused && !focusState.isFocused) onFocusLost()
                focused = focusState.isFocused
            },
        placeholder = placeholder,
        masked = masked,
        clearable = clearable,
        errorText = errorText,
    )
}

@Composable
fun TmdbSettingsScreen(
    onNavigateBack: () -> Unit,
    vm: ConfigManageViewModel = viewModel(factory = ConfigManageViewModel.defaultFactory()),
) {
    val context = LocalContext.current
    val state by vm.tmdbState().collectAsState()
    val testing by vm.tmdbTesting().collectAsState()
    val toastEvent by vm.toastEvent.collectAsState()

    LaunchedEffect(toastEvent) {
        toastEvent?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            vm.clearToast()
        }
    }

    AppTopBarScaffold(
        titleContent = {
            Text(
                text = stringResource(R.string.common_tmdb),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        navigationIcon = {
            TopBarActionBox(
                R.drawable.ic_arrow_left,
                stringResource(R.string.common_back),
                onClick = onNavigateBack,
            )
        },
    ) { topPad, _ ->
        ConfigTmdbScreen(
            state = state,
            actions = vm.tmdbActions(),
            testing = testing,
            modifier = Modifier.padding(top = topPad),
        )
    }
}

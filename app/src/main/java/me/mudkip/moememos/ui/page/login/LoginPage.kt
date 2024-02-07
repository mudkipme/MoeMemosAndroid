package me.mudkip.moememos.ui.page.login

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Login
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import androidx.navigation.NavHostController
import com.skydoves.sandwich.suspendOnSuccess
import kotlinx.coroutines.launch
import me.mudkip.moememos.R
import me.mudkip.moememos.ext.string
import me.mudkip.moememos.ext.suspendOnErrorMessage
import me.mudkip.moememos.ui.component.Markdown
import me.mudkip.moememos.ui.page.common.RouteName
import me.mudkip.moememos.viewmodel.LocalUserState

private enum class LoginMethod {
    USERNAME_AND_PASSWORD,
    ACCESS_TOKEN,
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LoginPage(
    navController: NavHostController
) {
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val coroutineScope = rememberCoroutineScope()
    val userStateViewModel = LocalUserState.current
    val snackbarState = remember { SnackbarHostState() }

    var loginMethodMenuExpanded by remember { mutableStateOf(false) }
    var loginMethod by remember { mutableStateOf(LoginMethod.USERNAME_AND_PASSWORD) }

    var username by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue())
    }

    var password by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue())
    }

    var host by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(userStateViewModel.host))
    }

    var accessToken by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue())
    }

    fun login() = coroutineScope.launch {
        if (host.text.isBlank()
            || (loginMethod == LoginMethod.USERNAME_AND_PASSWORD && (username.text.isBlank() || password.text.isEmpty()))
            || (loginMethod == LoginMethod.ACCESS_TOKEN && (accessToken.text.isBlank()))) {
            snackbarState.showSnackbar(R.string.fill_login_form.string)
            return@launch
        }

        val resp = when(loginMethod) {
            LoginMethod.USERNAME_AND_PASSWORD -> userStateViewModel.login(host.text.trim(), username.text.trim(), password.text)
            LoginMethod.ACCESS_TOKEN -> userStateViewModel.loginWithAccessToken(host.text.trim(), accessToken.text.trim())
        }

        resp.suspendOnSuccess {
            navController.popBackStack()
            navController.navigate(RouteName.MEMOS) {
                popUpTo(navController.graph.startDestinationId) {
                    inclusive = true
                }
            }
        }
        .suspendOnErrorMessage {
            snackbarState.showSnackbar(it)
        }
    }

    Scaffold(
        modifier = Modifier.imePadding(),
        snackbarHost = {
            SnackbarHost(hostState = snackbarState)
        },
        bottomBar = {
            BottomAppBar(
                actions = {
                    Box {
                        DropdownMenu(
                            expanded = loginMethodMenuExpanded,
                            onDismissRequest = { loginMethodMenuExpanded = false },
                            properties = PopupProperties(focusable = false)
                        ) {
                            DropdownMenuItem(
                                text = { Text(R.string.username_and_password.string) },
                                onClick = {
                                    loginMethod = LoginMethod.USERNAME_AND_PASSWORD
                                    loginMethodMenuExpanded = false
                                },
                                trailingIcon = {
                                    if (loginMethod == LoginMethod.USERNAME_AND_PASSWORD) {
                                        Icon(
                                            Icons.Outlined.Check,
                                            contentDescription = R.string.selected.string
                                        )
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(R.string.access_token.string) },
                                onClick = {
                                    loginMethod = LoginMethod.ACCESS_TOKEN
                                    loginMethodMenuExpanded = false
                                },
                                trailingIcon = {
                                    if (loginMethod == LoginMethod.ACCESS_TOKEN) {
                                        Icon(
                                            Icons.Outlined.Check,
                                            contentDescription = R.string.selected.string
                                        )
                                    }
                                }
                            )
                        }
                        TextButton(onClick = { loginMethodMenuExpanded = true }) {
                            Text(R.string.sign_in_method.string)
                        }
                    }
                },
                floatingActionButton = {
                    ExtendedFloatingActionButton(
                        onClick = { login() },
                        containerColor = BottomAppBarDefaults.bottomAppBarFabColor,
                        elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation(),
                        text = { Text(R.string.sign_in.string) },
                        icon = {
                            Icon(
                                Icons.AutoMirrored.Outlined.Login,
                                contentDescription = R.string.sign_in.string
                            )
                        }
                    )
                }
            )
        }
    ) { innerPadding ->
        Column(
            Modifier
                .padding(innerPadding)
                .fillMaxHeight()
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                R.string.moe_memos.string,
                modifier = Modifier.padding(bottom = 10.dp),
                style = MaterialTheme.typography.titleLarge
            )
            Markdown(
                R.string.input_login_information.string,
                modifier = Modifier.padding(bottom = 20.dp),
                textAlign = TextAlign.Center
            )

            Column(
                modifier = Modifier
                    .padding(horizontal = 30.dp)
                    .fillMaxWidth()
                    .bringIntoViewRequester(bringIntoViewRequester)
            ) {
                OutlinedTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusEvent { focusState ->
                            if (focusState.isFocused) {
                                coroutineScope.launch {
                                    bringIntoViewRequester.bringIntoView()
                                }
                            }
                        },
                    value = host,
                    onValueChange = { host = it },
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Computer,
                            contentDescription = R.string.address.string
                        )
                    },
                    label = {
                        Text(R.string.host.string)
                    },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        autoCorrect = false,
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Next
                    )
                )

                if (loginMethod == LoginMethod.USERNAME_AND_PASSWORD) {
                    OutlinedTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusEvent { focusState ->
                                if (focusState.isFocused) {
                                    coroutineScope.launch {
                                        bringIntoViewRequester.bringIntoView()
                                    }
                                }
                            },
                        value = username,
                        onValueChange = { username = it },
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Person,
                                contentDescription = R.string.username.string
                            )
                        },
                        label = { Text(R.string.username.string) },
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.None,
                            autoCorrect = false,
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        )
                    )

                    OutlinedTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusEvent { focusState ->
                                if (focusState.isFocused) {
                                    coroutineScope.launch {
                                        bringIntoViewRequester.bringIntoView()
                                    }
                                }
                            },
                        value = password,
                        onValueChange = { password = it },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Password,
                                contentDescription = R.string.password.string
                            )
                        },
                        label = { Text(R.string.password.string) },
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.None,
                            autoCorrect = false,
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Go
                        ),
                        keyboardActions = KeyboardActions(onGo = { login() })
                    )
                }

                if (loginMethod == LoginMethod.ACCESS_TOKEN) {
                    OutlinedTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusEvent { focusState ->
                                if (focusState.isFocused) {
                                    coroutineScope.launch {
                                        bringIntoViewRequester.bringIntoView()
                                    }
                                }
                            },
                        value = accessToken,
                        onValueChange = { accessToken = it },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.PermIdentity,
                                contentDescription = R.string.access_token.string
                            )
                        },
                        label = {
                            Text(R.string.access_token.string)
                        },
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.None,
                            autoCorrect = false,
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Go
                        ),
                        keyboardActions = KeyboardActions(onGo = { login() })
                    )
                }
            }
        }
    }
}
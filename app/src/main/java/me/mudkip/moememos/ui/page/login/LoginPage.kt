package me.mudkip.moememos.ui.page.login

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
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
import me.mudkip.moememos.ext.suspendOnErrorMessage
import me.mudkip.moememos.ui.page.common.RouteName
import me.mudkip.moememos.viewmodel.LocalUserState

private enum class LoginMethod {
    USERNAME_AND_PASSWORD,
    OPEN_API
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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

    var email by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue())
    }

    var password by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue())
    }

    var host by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(userStateViewModel.host))
    }

    fun login() = coroutineScope.launch {
        if (host.text.isBlank() || (loginMethod == LoginMethod.USERNAME_AND_PASSWORD && (email.text.isBlank() || password.text.isEmpty()))) {
            snackbarState.showSnackbar("Please fill the login form.")
            return@launch
        }

        val resp = when(loginMethod) {
            LoginMethod.USERNAME_AND_PASSWORD -> userStateViewModel.login(host.text, email.text, password.text)
            LoginMethod.OPEN_API -> userStateViewModel.login(host.text)
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
                                text = { Text("Username and password") },
                                onClick = {
                                    loginMethod = LoginMethod.USERNAME_AND_PASSWORD
                                    loginMethodMenuExpanded = false
                                },
                                trailingIcon = {
                                    if (loginMethod == LoginMethod.USERNAME_AND_PASSWORD) {
                                        Icon(Icons.Outlined.Check, contentDescription = "Selected")
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Open API") },
                                onClick = {
                                    loginMethod = LoginMethod.OPEN_API
                                    loginMethodMenuExpanded = false
                                },
                                trailingIcon = {
                                    if (loginMethod == LoginMethod.OPEN_API) {
                                        Icon(Icons.Outlined.Check, contentDescription = "Selected")
                                    }
                                }
                            )
                        }
                        TextButton(onClick = { loginMethodMenuExpanded = true }) {
                            Text("Sign in method")
                        }
                    }
                },
                floatingActionButton = {
                    ExtendedFloatingActionButton(
                        onClick = { login() },
                        containerColor = BottomAppBarDefaults.bottomAppBarFabColor,
                        elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation(),
                        text = { Text("Sign in") },
                        icon = { Icon(Icons.Outlined.Login, contentDescription = "Sign in") }
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
                "Moe Memos",
                modifier = Modifier.padding(bottom = 10.dp),
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                "Please input the login information of your \n✍️memos server.",
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
                    singleLine = loginMethod == LoginMethod.USERNAME_AND_PASSWORD,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Computer,
                            contentDescription = "Address"
                        )
                    },
                    label = {
                        if (loginMethod == LoginMethod.USERNAME_AND_PASSWORD) {
                            Text("Host")
                        } else {
                            Text("Open API")
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        autoCorrect = false,
                        keyboardType = KeyboardType.Uri,
                        imeAction = if (loginMethod == LoginMethod.OPEN_API) ImeAction.Go else ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(onGo = { login() })
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
                        value = email,
                        onValueChange = { email = it },
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Email,
                                contentDescription = "Email"
                            )
                        },
                        label = { Text("Email") },
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.None,
                            autoCorrect = false,
                            keyboardType = KeyboardType.Email,
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
                                contentDescription = "Password"
                            )
                        },
                        label = { Text("Password") },
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
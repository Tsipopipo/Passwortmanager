// MainActivity.kt – Final korrigierte Version (Material3-kompatibel, mit Drawer, Bearbeiten, Löschen, Kommentaren)
package com.example.passwortmanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

// Datenklasse für gespeicherte Zugangsdaten
// Enthält die Webseite, den Benutzernamen und das Passwort
data class Credential(var site: String, var username: String, var password: String)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Setzt das UI-Theme und ruft den App-Aufbau mit Navigation Drawer auf
            MaterialTheme {
                AppWithDrawer()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppWithDrawer() {
    // DrawerState speichert, ob das Menü offen oder geschlossen ist
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // ModalNavigationDrawer zeigt links ein aufklappbares Menü
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(24.dp))
                Text("Menü", modifier = Modifier.padding(16.dp), fontSize = 20.sp)
                NavigationDrawerItem(
                    label = { Text("Home") },
                    selected = false,
                    onClick = { scope.launch { drawerState.close() } }
                )
            }
        }
    ) {
        // Scaffold verwaltet die TopAppBar und den Inhaltsbereich
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Passwortmanager") },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menü öffnen")
                        }
                    }
                )
            }
        ) { innerPadding ->
            Surface(modifier = Modifier.padding(innerPadding)) {
                PasswordManagerScreen()
            }
        }
    }
}

@Composable
fun PasswordManagerScreen() {
    // Eingabefelder als Zustände speichern
    var site by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // Bearbeitungszustände: wird gerade ein Eintrag bearbeitet?
    var isEditing by remember { mutableStateOf(false) }
    var editingIndex by remember { mutableStateOf(-1) }

    // Liste aller gespeicherten Anmeldedaten
    val credentials = remember { mutableStateListOf<Credential>() }

    // UI-Layout
    Column(modifier = Modifier.padding(16.dp)) {

        // Eingabe: Webseite
        OutlinedTextField(
            value = site,
            onValueChange = { site = it },
            label = { Text("Webseite") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Eingabe: Benutzername
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Benutzername") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Eingabe: Passwort
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Passwort") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Speichern-Button oder Aktualisieren (bei Bearbeitung)
        Button(
            onClick = {
                if (site.isNotBlank() && username.isNotBlank() && password.isNotBlank()) {
                    if (isEditing && editingIndex >= 0) {
                        credentials[editingIndex] = Credential(site, username, password)
                        isEditing = false
                        editingIndex = -1
                    } else {
                        credentials.add(Credential(site, username, password))
                    }
                    site = ""
                    username = ""
                    password = ""
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isEditing) "Aktualisieren" else "Speichern")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Anzeige aller gespeicherten Zugangsdaten
        credentials.forEachIndexed { index, cred ->
            CredentialItem(
                credential = cred,
                onDelete = { credentials.removeAt(index) },
                onEdit = {
                    site = cred.site
                    username = cred.username
                    password = cred.password
                    editingIndex = index
                    isEditing = true
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun CredentialItem(credential: Credential, onDelete: () -> Unit, onEdit: () -> Unit) {
    // Zustand: Passwort sichtbar oder nicht
    var isPasswordVisible by remember { mutableStateOf(false) }

    // UI-Komponente für eine einzelne gespeicherte Anmeldedaten-Karte
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("🌐 ${credential.site}", style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp))
            Text("👤 ${credential.username}")
            Text(
                text = if (isPasswordVisible) "🔒 ${credential.password}" else "🔒 ******",
                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Button(onClick = { isPasswordVisible = !isPasswordVisible }) {
                    Text(if (isPasswordVisible) "Verstecken" else "Anzeigen")
                }
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Bearbeiten")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Löschen")
                    }
                }
            }
        }
    }
}
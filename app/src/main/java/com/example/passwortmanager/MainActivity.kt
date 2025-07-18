// MainActivity.kt – Passwortmanager mit erweiterten Funktionen und Fehlerfreiheit
package com.example.passwortmanager

// Imports für Android- und Jetpack Compose-Komponenten
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.security.SecureRandom

// Datenklasse für gespeicherte Zugangsdaten (Webseite, Benutzername, Passwort)
data class Credential(var site: String, var username: String, var password: String)

// Einstiegspunkt der App – Android Activity mit Compose
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Definiert den UI-Inhalt der App
        setContent {
            MaterialTheme {
                AppWithDrawer()  // Ruft die Haupt-UI mit Seitenmenü auf
            }
        }
    }
}

// Composable mit Navigation Drawer (Seitenmenü) + TopBar
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppWithDrawer() {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed) // Zustand des Drawers
    val scope = rememberCoroutineScope() // Coroutine für UI-Aktionen

    // Definiert das Seitenmenü (Drawer)
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
        // Scaffold mit TopBar und Inhalt
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
                PasswordManagerScreen() // Hauptbildschirm
            }
        }
    }
}

// Hauptbildschirm mit Passwortverwaltung
@Composable
fun PasswordManagerScreen() {
    // Zustände für Eingabefelder
    var site by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    var isEditing by remember { mutableStateOf(false) }     // Bearbeitungsmodus?
    var editingIndex by remember { mutableStateOf(-1) }     // Welcher Eintrag wird bearbeitet?
    val credentials = remember { mutableStateListOf<Credential>() }  // Liste aller gespeicherten Passwörter
    val context = LocalContext.current                      // Kontext z. B. für Toasts
    var searchQuery by remember { mutableStateOf("") }      // Suchfeld-Text

    Column(modifier = Modifier.padding(16.dp)) {

        // Suchfeld
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("🔍 Suche") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Eingabefeld: Webseite
        OutlinedTextField(
            value = site,
            onValueChange = { site = it },
            label = { Text("Webseite") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Eingabefeld: Benutzername
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Benutzername") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Eingabefeld: Passwort
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Passwort") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Button zum Generieren eines sicheren Passworts
        Button(
            onClick = {
                password = generateSecurePassword()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("🔐 Sicheres Passwort generieren")
        }
        Spacer(modifier = Modifier.height(12.dp))

        // Button zum Speichern oder Aktualisieren
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

        // Gefilterte Einträge anzeigen (nach Suchtext)
        credentials.filter {
            it.site.contains(searchQuery, ignoreCase = true) ||
                    it.username.contains(searchQuery, ignoreCase = true)
        }.forEachIndexed { index, cred ->
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

// Ein einzelner Eintrag in der Liste (Credential)
@Composable
fun CredentialItem(credential: Credential, onDelete: () -> Unit, onEdit: () -> Unit) {
    var isPasswordVisible by remember { mutableStateOf(false) } // Passwort sichtbar?
    var showDialog by remember { mutableStateOf(false) }         // Löschdialog aktiv?

    // Wenn Dialog aktiviert ist, zeige Bestätigungsdialog
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Eintrag löschen") },
            text = { Text("Möchtest du diesen Eintrag wirklich löschen?") },
            confirmButton = {
                TextButton(onClick = {
                    showDialog = false
                    onDelete()
                }) {
                    Text("Löschen")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Abbrechen")
                }
            }
        )
    }

    // Darstellung des Eintrags
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("🌐 ${credential.site}", style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp))
            Text("👤 ${credential.username}")
            Text(
                text = if (isPasswordVisible) "🔒 ${credential.password}" else "🔒 ******",
                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                // Passwort anzeigen/verstecken
                Button(onClick = { isPasswordVisible = !isPasswordVisible }) {
                    Text(if (isPasswordVisible) "Verstecken" else "Anzeigen")
                }
                // Icons zum Bearbeiten und Löschen
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Bearbeiten")
                    }
                    IconButton(onClick = { showDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Löschen")
                    }
                }
            }
        }
    }
}

// Funktion zum Generieren eines sicheren Passworts
fun generateSecurePassword(length: Int = 12): String {
    val charset = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#\$%^&*()_-+=<>?"
    val random = SecureRandom() // Sichere Zufallsquelle
    return (1..length)
        .map { charset[random.nextInt(charset.length)] }
        .joinToString("") // Gibt ein zufälliges Zeichen aus dem Charset zurück
}
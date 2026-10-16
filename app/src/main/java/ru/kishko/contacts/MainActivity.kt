package ru.kishko.contacts

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ContactItem(
    val id: String,
    val name: String,
    val phone: String?
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                ContactsScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var contacts by remember {
        mutableStateOf<List<ContactItem>>(emptyList())
    }

    var loading by remember { mutableStateOf(false) }

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var permissionDenied by rememberSaveable {
        mutableStateOf(false)
    }

    fun loadContacts() {
        if (
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CONTACTS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            hasPermission = false
            return
        }

        scope.launch {
            loading = true

            try {
                val result = withContext(Dispatchers.IO) {
                    readContacts(context)
                }

                contacts = result

                Toast.makeText(
                    context,
                    "Найдено ${result.size} контакт(ов)",
                    Toast.LENGTH_SHORT
                ).show()
            } catch (_: SecurityException) {
                hasPermission = false
                contacts = emptyList()

                Toast.makeText(
                    context,
                    "Нет разрешения на чтение контактов",
                    Toast.LENGTH_LONG
                ).show()
            } catch (_: Exception) {
                Toast.makeText(
                    context,
                    "Не удалось загрузить контакты",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                loading = false
            }
        }
    }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { granted ->
            hasPermission = granted
            permissionDenied = !granted

            if (granted) {
                loadContacts()
            } else {
                contacts = emptyList()

                Toast.makeText(
                    context,
                    "Для отображения контактов требуется разрешение",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    LaunchedEffect(Unit) {
        if (hasPermission) {
            loadContacts()
        } else {
            permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Список контактов") }
            )
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                when {
                    !hasPermission -> {
                        Text(
                            text = if (permissionDenied) {
                                "Доступ к контактам не предоставлен. " +
                                        "Разреши доступ, чтобы увидеть список."
                            } else {
                                "Для работы приложения нужен доступ " +
                                        "к контактам телефона."
                            },
                            style = MaterialTheme.typography.bodyLarge
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                permissionLauncher.launch(
                                    Manifest.permission.READ_CONTACTS
                                )
                            }
                        ) {
                            Text("Разрешить доступ")
                        }
                    }

                    loading -> {
                        Text("Загружаем контакты…")
                    }

                    else -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Найдено ${contacts.size} контакт(ов)",
                                style = MaterialTheme.typography.titleMedium
                            )

                            Button(
                                onClick = { loadContacts() }
                            ) {
                                Text("Обновить")
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (contacts.isEmpty()) {
                            Text("No contacts found")
                        } else {
                            LazyColumn(
                                verticalArrangement =
                                    Arrangement.spacedBy(8.dp)
                            ) {
                                items(
                                    items = contacts,
                                    key = { it.id }
                                ) { contact ->
                                    ContactCard(
                                        contact = contact,
                                        onClick = {
                                            val number = contact.phone

                                            if (number.isNullOrBlank()) {
                                                Toast.makeText(
                                                    context,
                                                    "У контакта нет номера телефона",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            } else {
                                                val intent = Intent(
                                                    Intent.ACTION_DIAL,
                                                    Uri.fromParts(
                                                        "tel",
                                                        number,
                                                        null
                                                    )
                                                )

                                                context.startActivity(intent)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactCard(
    contact: ContactItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = contact.name,
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = contact.phone ?: "Номер не указан",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun readContacts(
    context: Context
): List<ContactItem> {
    val result = mutableListOf<ContactItem>()
    val resolver = context.contentResolver

    val projection = arrayOf(
        ContactsContract.Contacts._ID,
        ContactsContract.Contacts.DISPLAY_NAME,
        ContactsContract.Contacts.HAS_PHONE_NUMBER
    )

    resolver.query(
        ContactsContract.Contacts.CONTENT_URI,
        projection,
        null,
        null,
        "${ContactsContract.Contacts.DISPLAY_NAME} COLLATE NOCASE ASC"
    )?.use { cursor ->

        val idIndex = cursor.getColumnIndexOrThrow(
            ContactsContract.Contacts._ID
        )

        val nameIndex = cursor.getColumnIndexOrThrow(
            ContactsContract.Contacts.DISPLAY_NAME
        )

        val phoneFlagIndex = cursor.getColumnIndexOrThrow(
            ContactsContract.Contacts.HAS_PHONE_NUMBER
        )

        while (cursor.moveToNext()) {
            val id = cursor.getString(idIndex)
            val name = cursor.getString(nameIndex)
                ?.takeIf { it.isNotBlank() }
                ?: "Без имени"

            val hasPhone = cursor.getInt(phoneFlagIndex) > 0

            val phone = if (hasPhone) {
                resolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    arrayOf(
                        ContactsContract.CommonDataKinds.Phone.NUMBER
                    ),
                    "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                    arrayOf(id),
                    null
                )?.use { phoneCursor ->
                    if (phoneCursor.moveToFirst()) {
                        phoneCursor.getString(
                            phoneCursor.getColumnIndexOrThrow(
                                ContactsContract.CommonDataKinds.Phone.NUMBER
                            )
                        )
                    } else {
                        null
                    }
                }
            } else {
                null
            }

            result.add(
                ContactItem(
                    id = id,
                    name = name,
                    phone = phone
                )
            )
        }
    }

    return result
}

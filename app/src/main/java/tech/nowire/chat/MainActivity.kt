package tech.nowire.chat

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.icu.text.SimpleDateFormat
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.TurnLeft
import androidx.compose.material.icons.sharp.MarkEmailRead
import androidx.compose.material.icons.sharp.PushPin
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.lightspark.composeqr.QrCodeView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import tech.nowire.chat.hcl.JnaSodium
import tech.nowire.chat.hcl.LibSodiumUtils
import tech.nowire.chat.hcl.Ratchet
import tech.nowire.chat.ui.theme.AppTheme
import tech.nowire.chat.ui.theme.bodyFontFamily
import java.io.File
import java.io.InputStream
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.cert.Certificate
import java.security.cert.CertificateFactory
import java.util.Calendar
import java.util.Date
import java.util.LinkedList
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.TrustManagerFactory
import kotlin.jvm.optionals.getOrNull
import kotlin.math.ceil

enum class MainDialogToShow {
    ONLINE_SETUP,
    OFFLINE_SETUP_IMPORT,
    OFFLINE_SETUP_GENERATE,
    NONE,
}

enum class AuthDialogToShow {
    RESET_PIN,
    NONE,
}

enum class ThemeColorMode {
    LIGHT_MODE,
    DARK_MODE,
    SYSTEM,
}

const val SERVER_HOST: String = "official.nowire.tech"
const val SERVER_PORT: Int = 25565

@Serializable
class SendQueueEntry(
    val encryptedMessage: ByteArray,
    val receiver: ByteArray,
    val id: String,
    val timestamp: Long,
)

class MainActivity : ComponentActivity() {

    private val isAuth = mutableStateOf(true)
    private val selectedConvo: MutableState<Chat?> = mutableStateOf(null)
    private var selectedConvoBroken: MutableState<Boolean>? = null
    private var selectedConvoMessages: SnapshotStateList<Message>? = null
    private var deliveryMessages: SnapshotStateMap<String, DeliveryStage>? = null

    private fun generateSalt(): String {
        val saltBytes = LibSodiumUtils.randomBuf(256)
        val salt = LibSodiumUtils.toBase64(saltBytes).get()
        return salt
    }

    companion object {
        lateinit var cert: Certificate
    }

    override fun onResume() {
        super.onResume()
        val prefs = getPreferences(Context.MODE_PRIVATE)
        val x = prefs.getLong("time_saved", 0L)
        val logoutDuration = prefs.getLong("logout_duration", 30_000)

        if (logoutDuration != -1L) {
            if (x == 0L || logoutDuration == 0L || System.currentTimeMillis() - x >= logoutDuration) {
                isAuth.value = true
                selectedConvo.value = null
                selectedConvoMessages = null
                selectedConvoBroken = null
            }
        }
    }

    override fun onPause() {
        super.onPause()

        val prefs = getPreferences(Context.MODE_PRIVATE)
        with(prefs.edit()) {
            putLong(
                "time_saved",
                if (isAuth.value) {
                    0L
                } else {
                    System.currentTimeMillis()
                }
            )
            apply()
        }
    }

    override fun onStop() {
        super.onStop()
        try {
            appState.saveAsEncryptedJson()
        } catch (e: Exception) {
        }
    }

    lateinit var appState: AppState

    @RequiresApi(Build.VERSION_CODES.O)
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        val certFactory = CertificateFactory.getInstance("X.509")
        val assetManager = applicationContext.assets
        val certInput: InputStream = assetManager.open("cert.pem")
        cert = certFactory.generateCertificate(certInput)
        certInput.close()

        val saltFile = File(filesDir, "salt.txt")
        var salt: String

        if (saltFile.exists()) {
            salt = saltFile.readText(charset = Charsets.UTF_8)
        } else {
            salt = generateSalt()
            saltFile.writeText(salt, charset = Charsets.UTF_8)
        }

        val appFile = File(filesDir, "stuff.dat")
        var appFileExists = appFile.exists()

        WindowCompat.setDecorFitsSystemWindows(window, false)

        val prefs = getPreferences(Context.MODE_PRIVATE)
        val themeColorMode = ThemeColorMode.valueOf(
            prefs.getString(
                "color_scheme_mode",
                ThemeColorMode.SYSTEM.name
            )!!
        )

        setContent {
            var modeSelection by remember { mutableStateOf(themeColorMode) }
            AppTheme(
                updateStatusBarColors = { isDark, color, darkScrim ->
                    val barStyle = if (isDark) {
                        SystemBarStyle.dark(color.toArgb())
                    } else {
                        SystemBarStyle.light(color.toArgb(), darkScrim.toArgb())
                    }
                    enableEdgeToEdge(barStyle, barStyle)
                },
                modeSelection = modeSelection
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    Box(Modifier.safeDrawingPadding()) {
                        var isAuth by remember { isAuth }
                        val shouldRecompose = remember { mutableIntStateOf(0) }
                        var showSettings by remember { mutableStateOf(false) }

                        key(shouldRecompose.intValue) {
                            if (isAuth) {
                                val pinBuffer = remember { mutableStateOf("") }

                                if (appFileExists) {
                                    var authDialogToShow by remember {
                                        mutableStateOf(
                                            AuthDialogToShow.NONE
                                        )
                                    }
                                    val isError = pinBuffer.value == "ERROR"

                                    Scaffold(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .navigationBarsPadding(),
                                        topBar = {
                                            TopAppBar(title = {
                                                Text(
                                                    stringResource(R.string.enter_login_pin),
                                                    style = MaterialTheme.typography.displaySmall
                                                )
                                            })
                                        }
                                    ) { innerPadding ->
                                        Column(
                                            modifier = Modifier
                                                .padding(innerPadding)
                                                .fillMaxSize(),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                        ) {
                                            Spacer(Modifier.weight(0.8f))
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth(0.6f),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    val config = LocalConfiguration.current

                                                    val pinBuffer by pinBuffer

                                                    val fs =
                                                        (config.screenHeightDp.dp.value * 0.06f).coerceAtLeast(
                                                            14f
                                                        ).sp
                                                    val fw = FontWeight.ExtraBold

                                                    val setColor = if (isError) {
                                                        MaterialTheme.colorScheme.error
                                                    } else {
                                                        MaterialTheme.colorScheme.onSurface
                                                    }

                                                    val notSetColor = if (isError) {
                                                        MaterialTheme.colorScheme.error
                                                    } else {
                                                        MaterialTheme.colorScheme.inverseOnSurface
                                                    }

                                                    Text(
                                                        "X", color = if (pinBuffer.isNotEmpty()) {
                                                            setColor
                                                        } else {
                                                            notSetColor
                                                        }, fontWeight = fw, fontSize = fs
                                                    )
                                                    Text(
                                                        "X", color = if (pinBuffer.length >= 2) {
                                                            setColor
                                                        } else {
                                                            notSetColor
                                                        }, fontWeight = fw, fontSize = fs
                                                    )
                                                    Text(
                                                        "X", color = if (pinBuffer.length >= 3) {
                                                            setColor
                                                        } else {
                                                            notSetColor
                                                        }, fontWeight = fw, fontSize = fs
                                                    )
                                                    Text(
                                                        "X", color = if (pinBuffer.length == 4) {
                                                            setColor
                                                        } else {
                                                            notSetColor
                                                        }, fontWeight = fw, fontSize = fs
                                                    )
                                                }
                                                if (isError) {
                                                    Text(
                                                        stringResource(R.string.wrong_pin),
                                                        color = MaterialTheme.colorScheme.error
                                                    )
                                                } else {
                                                    Text("")
                                                }
                                            }
                                            Spacer(Modifier.weight(1.0f))
                                            PinField(pinBuffer)
                                            Spacer(Modifier.weight(0.5f))
                                            Column(
                                                modifier = Modifier.fillMaxWidth(0.8f),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                TextButton(onClick = {
                                                    authDialogToShow = AuthDialogToShow.RESET_PIN
                                                }) {
                                                    Text(
                                                        stringResource(R.string.forgot_pin),
                                                        style = MaterialTheme.typography.bodyLarge
                                                    )
                                                }
                                            }

                                            Spacer(Modifier.weight(1.5f))

                                            if (pinBuffer.value.length == 4) {
                                                try {
                                                    val tmp = AppState.fromEncryptedJson(
                                                        LibSodiumUtils.stringToSymmetricKey(
                                                            pinBuffer.value,
                                                            salt
                                                        ).get(),
                                                        appFile,
                                                        appFile.readBytes(),
                                                    )
                                                    appState = tmp!!
                                                    isAuth = false
                                                } catch (e: Exception) {
                                                    e.printStackTrace()
                                                    pinBuffer.value = "ERROR"
                                                }
                                            }
                                        }

                                        if (authDialogToShow == AuthDialogToShow.RESET_PIN) {
                                            Dialog(
                                                onDismissRequest = {
                                                    authDialogToShow = AuthDialogToShow.NONE
                                                }
                                            ) {
                                                Column(
                                                    modifier = Modifier.fillMaxSize(),
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.Center,
                                                ) {
                                                    val configuration = LocalConfiguration.current

                                                    val screenHeight =
                                                        configuration.screenHeightDp.dp
                                                    val screenWidth = configuration.screenWidthDp.dp

                                                    val l =
                                                        (screenHeight.value * 0.03f).coerceAtLeast(
                                                            14f
                                                        ).dp
                                                    val spacerSize = l
                                                    val s = RoundedCornerShape(16.dp)

                                                    Column(
                                                        horizontalAlignment = Alignment.CenterHorizontally,
                                                        verticalArrangement = Arrangement.Center,
                                                        modifier = Modifier
                                                            .fillMaxWidth(0.8f)
                                                            .background(
                                                                MaterialTheme.colorScheme.surfaceContainer,
                                                                shape = s
                                                            )
                                                            .padding(32.dp)
                                                    ) {

                                                        Text(
                                                            stringResource(R.string.confirm_delete_app_data),
                                                            color = MaterialTheme.colorScheme.onSurface,
                                                            style = MaterialTheme.typography.bodyLarge,
                                                        )

                                                        Spacer(Modifier.size(spacerSize))

                                                        Row {
                                                            Button(
                                                                modifier = Modifier.padding(end = 8.dp),
                                                                onClick = {
                                                                    if (appFile.exists()) {
                                                                        appFile.delete()
                                                                    }

                                                                    salt = generateSalt()
                                                                    saltFile.writeText(
                                                                        salt,
                                                                        charset = Charsets.UTF_8
                                                                    )

                                                                    appFileExists = false
                                                                    selectedConvo.value = null
                                                                    selectedConvoMessages = null
                                                                    selectedConvoBroken = null
                                                                    shouldRecompose.value += 1
                                                                },
                                                                colors = ButtonDefaults.buttonColors(
                                                                    containerColor = MaterialTheme.colorScheme.error
                                                                )
                                                            ) {
                                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                                    Text(
                                                                        stringResource(R.string.yes),
                                                                        style = MaterialTheme.typography.bodyMedium
                                                                    )
                                                                    Icon(
                                                                        Icons.Filled.DeleteForever,
                                                                        null,
                                                                    )
                                                                }
                                                            }
                                                            Button(onClick = {
                                                                authDialogToShow =
                                                                    AuthDialogToShow.NONE
                                                            }) {
                                                                Text(
                                                                    stringResource(R.string.no),
                                                                    style = MaterialTheme.typography.bodyMedium
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                    }
                                } else {
                                    Scaffold(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .navigationBarsPadding(),
                                        topBar = {
                                            TopAppBar(title = {
                                                Text(
                                                    stringResource(R.string.set_login_pin),
                                                    style = MaterialTheme.typography.displaySmall
                                                )
                                            })
                                        }
                                    ) { innerPadding ->
                                        Column(
                                            modifier = Modifier
                                                .padding(innerPadding)
                                                .fillMaxSize(),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                        ) {
                                            Spacer(Modifier.weight(0.8f))
                                            Column(
                                                modifier = Modifier.fillMaxWidth(0.6f),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    val config = LocalConfiguration.current

                                                    val pinBuffer by pinBuffer

                                                    val fs =
                                                        (config.screenHeightDp.dp.value * 0.06f).coerceAtLeast(
                                                            14f
                                                        ).sp
                                                    val fw = FontWeight.ExtraBold

                                                    val setColor =
                                                        MaterialTheme.colorScheme.onSurface
                                                    val notSetColor =
                                                        MaterialTheme.colorScheme.inverseOnSurface

                                                    Text(
                                                        "X", color = if (pinBuffer.isNotEmpty()) {
                                                            setColor
                                                        } else {
                                                            notSetColor
                                                        }, fontWeight = fw, fontSize = fs
                                                    )
                                                    Text(
                                                        "X", color = if (pinBuffer.length >= 2) {
                                                            setColor
                                                        } else {
                                                            notSetColor
                                                        }, fontWeight = fw, fontSize = fs
                                                    )
                                                    Text(
                                                        "X", color = if (pinBuffer.length >= 3) {
                                                            setColor
                                                        } else {
                                                            notSetColor
                                                        }, fontWeight = fw, fontSize = fs
                                                    )
                                                    Text(
                                                        "X", color = if (pinBuffer.length == 4) {
                                                            setColor
                                                        } else {
                                                            notSetColor
                                                        }, fontWeight = fw, fontSize = fs
                                                    )
                                                }
                                            }
                                            Spacer(Modifier.weight(1.0f))
                                            PinField(pinBuffer)
                                            Spacer(Modifier.weight(1.5f))
                                            if (pinBuffer.value.length == 4) {
                                                Dialog(
                                                    onDismissRequest = { pinBuffer.value = "" }
                                                ) {
                                                    Column(
                                                        horizontalAlignment = Alignment.CenterHorizontally,
                                                        verticalArrangement = Arrangement.Center,
                                                        modifier = Modifier.fillMaxSize()
                                                    ) {
                                                        val configuration =
                                                            LocalConfiguration.current

                                                        val screenHeight =
                                                            configuration.screenHeightDp.dp
                                                        val screenWidth =
                                                            configuration.screenWidthDp.dp

                                                        val l =
                                                            (screenHeight.value * 0.03f).coerceAtLeast(
                                                                14f
                                                            ).dp
                                                        val spacerSize = l
                                                        val s = RoundedCornerShape(16.dp)
                                                        Column(
                                                            horizontalAlignment = Alignment.CenterHorizontally,
                                                            verticalArrangement = Arrangement.Center,
                                                            modifier = Modifier
                                                                .fillMaxWidth(0.8f)
                                                                .background(
                                                                    color = MaterialTheme.colorScheme.surfaceContainer,
                                                                    shape = s
                                                                )
                                                                .padding(32.dp)
                                                        ) {
                                                            Text(
                                                                stringResource(R.string.confirm_pin),
                                                                color = MaterialTheme.colorScheme.onSurface,
                                                                style = MaterialTheme.typography.bodyLarge
                                                            )
                                                            Spacer(Modifier.size(spacerSize))
                                                            Row {
                                                                Button(
                                                                    modifier = Modifier.padding(
                                                                        end = 8.dp
                                                                    ), onClick = {
                                                                        appFile.createNewFile()

                                                                        appState = AppState(
                                                                            mutableListOf(),
                                                                            LinkedList(),
                                                                            LibSodiumUtils.stringToSymmetricKey(
                                                                                pinBuffer.value,
                                                                                salt
                                                                            ).get(),
                                                                            appFile,
                                                                            HashMap()
                                                                        )
                                                                        appState.saveAsEncryptedJson()
                                                                        appFileExists = true
                                                                        selectedConvo.value = null
                                                                        selectedConvoMessages = null
                                                                        selectedConvoBroken = null
                                                                        isAuth = false
                                                                    }) {
                                                                    Text(
                                                                        stringResource(R.string.yes),
                                                                        style = MaterialTheme.typography.bodyMedium
                                                                    )
                                                                }
                                                                Button(onClick = {
                                                                    pinBuffer.value = ""
                                                                }) {
                                                                    Text(
                                                                        stringResource(R.string.no),
                                                                        style = MaterialTheme.typography.bodyMedium
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
                            } else {
                                val r = rememberCoroutineScope()
                                LaunchedEffect(Unit) {
                                    r.launch {
                                        while (isActive) {
                                            try {
//                                        Log.i("NETT", "CONNECTING")
                                                val (s, nonce) = createSslConnectionAsync()
                                                val mutex = Mutex()
                                                s.use {
//                                            Log.i("NETT", "CONNECTED")
                                                    try {
                                                        while (isActive) {
                                                            if (!ping(s, mutex)) {
                                                                throw Exception()
                                                            }
                                                            var doneAnything = false

                                                            var sent = 0

                                                            while (appState.sendQueue.isNotEmpty() && sent < 10) {
                                                                val toSend =
                                                                    appState.sendQueue.peek()!!
                                                                sent += 1
                                                                if (sendMessage(
                                                                        s,
                                                                        mutex,
                                                                        toSend.encryptedMessage,
                                                                        toSend.receiver
                                                                    )
                                                                ) {
                                                                    appState.sendQueue.poll()
                                                                    appState.deliveryStages.remove(toSend.id)
                                                                    deliveryMessages?.remove(toSend.id)
                                                                    doneAnything = true
                                                                } else {
                                                                    throw Exception()
                                                                }
                                                            }

                                                            if (doneAnything) {
                                                                appState.saveAsEncryptedJson()
                                                            }

                                                            delay(100)
//                                                    Log.i(
//                                                        "NETT",
//                                                        "$doneAnything ${s.isConnected} ${s.isClosed}"
//                                                    )

                                                            val newMessages =
                                                                getNewMessages(
                                                                    s,
                                                                    nonce,
                                                                    appState.chats.map {
                                                                        Pair(
                                                                            it.selfSignPublicKey,
                                                                            it.selfSignPrivateKey,
                                                                        )
                                                                    })
                                                            newMessages.forEach { msg ->
                                                                val selfSignPublicKey = msg.first
                                                                val encryptedMessage = msg.second

                                                                val chat = appState.chats.find {
                                                                    it.selfSignPublicKey.contentEquals(
                                                                        selfSignPublicKey
                                                                    )
                                                                }

                                                                val decryptedMessageWithIdJson =
                                                                    chat!!.recv.decryptString(
                                                                        encryptedMessage
                                                                    ).getOrNull()

                                                                if (decryptedMessageWithIdJson == null) {
                                                                    if (!chat.broken) {
                                                                        chat.broken = true
                                                                        if (selectedConvo.value?.id == chat.id) {
                                                                            selectedConvoBroken?.value =
                                                                                true
                                                                        }
                                                                        shouldRecompose.intValue += 1
                                                                    }
                                                                } else {
                                                                    val messageWithId =
                                                                        Json.decodeFromString<MessageTextWithId>(
                                                                            decryptedMessageWithIdJson
                                                                        )
                                                                    val m =
                                                                        Message(
                                                                            UUID.fromString(
                                                                                messageWithId.id
                                                                            ),
                                                                            messageWithId.messageText,
                                                                            System.currentTimeMillis(),
                                                                            chat.otherSignPublicKey,
                                                                            messageWithId.respondingTo?.let {
                                                                                UUID.fromString(
                                                                                    it
                                                                                )
                                                                            },
                                                                        )
                                                                    chat.messages.add(m)
                                                                    if (selectedConvo.value?.id == chat.id) {
                                                                        selectedConvoMessages?.add(m)
                                                                    } else {
                                                                        chat.amountUnread += 1
                                                                        shouldRecompose.intValue += 1
                                                                    }
                                                                }
                                                            }
                                                            if (newMessages.isNotEmpty()) {
                                                                appState.saveAsEncryptedJson()
                                                            }

                                                            delay(500)
                                                        }
                                                    } catch (e: Exception) {
                                                        e.printStackTrace()
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                            }
                                            delay(5000)
                                        }
                                    }
                                }

                                val dialogToShow: MutableState<MainDialogToShow> =
                                    remember { mutableStateOf(MainDialogToShow.NONE) }

                                if (showSettings) {
                                    SettingsScreen(prefs, updateColorScheme = {
                                        modeSelection = it
                                    }) {
                                        showSettings = false
                                    }
                                } else if (selectedConvo.value != null) {
                                    selectedConvoMessages = remember { mutableStateListOf() }
                                    if (deliveryMessages == null) {
                                        deliveryMessages = remember { mutableStateMapOf() }
                                        deliveryMessages?.putAll(appState.deliveryStages)
                                    }
                                    selectedConvoBroken =
                                        remember { mutableStateOf(selectedConvo.value!!.broken) }
                                    OpenedChat(
                                        appState,
                                        selectedConvo,
                                        selectedConvoMessages!!,
                                        deliveryMessages!!,
                                        selectedConvoBroken!!,
                                    )
                                } else {
                                    val selection = remember { mutableStateListOf<Long>() }

                                    fun unselect() {
                                        selection.clear()
                                    }

                                    var showDeletePrompt by remember { mutableStateOf(false) }

                                    if (showDeletePrompt) {
                                        Dialog(onDismissRequest = {
                                            showDeletePrompt = false
                                        }) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center,
                                                modifier = Modifier.fillMaxSize()
                                            ) {
                                                val configuration =
                                                    LocalConfiguration.current

                                                val screenHeight =
                                                    configuration.screenHeightDp.dp
                                                val screenWidth =
                                                    configuration.screenWidthDp.dp

                                                val l =
                                                    (screenHeight.value * 0.03f).coerceAtLeast(
                                                        14f
                                                    ).dp
                                                val spacerSize = l
                                                val s = RoundedCornerShape(16.dp)
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.Center,
                                                    modifier = Modifier
                                                        .fillMaxWidth(0.8f)
                                                        .background(
                                                            color = MaterialTheme.colorScheme.surfaceContainer,
                                                            shape = s
                                                        )
                                                        .padding(32.dp)
                                                ) {
                                                    Text(
                                                        stringResource(R.string.delete_selected_chats),
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        style = MaterialTheme.typography.bodyLarge
                                                    )
                                                    Spacer(Modifier.size(spacerSize))
                                                    Row {
                                                        Button(
                                                            modifier = Modifier.padding(end = 8.dp),
                                                            onClick = {
                                                            val anyRemoved =
                                                                appState.chats.removeIf {
                                                                    selection.contains(it.id)
                                                                }
                                                            if (anyRemoved) {
                                                                appState.saveAsEncryptedJson()
                                                            }
                                                            shouldRecompose.intValue += 1
                                                            unselect()
                                                            showDeletePrompt = false
                                                            },
                                                            colors = ButtonDefaults.buttonColors(
                                                                containerColor = MaterialTheme.colorScheme.error
                                                            )
                                                        ) {
                                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                                Text(
                                                                    stringResource(R.string.yes),
                                                                    style = MaterialTheme.typography.bodyMedium
                                                                )
                                                                Icon(
                                                                    Icons.Filled.DeleteForever,
                                                                    null,
                                                                )
                                                            }
                                                        }
                                                        Button(onClick = {
                                                            showDeletePrompt = false
                                                        }) {
                                                            Text(
                                                                stringResource(R.string.no),
                                                                style = MaterialTheme.typography.bodyMedium
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    if (!showDeletePrompt) {
                                        BackHandler(enabled = selection.isNotEmpty()) {
                                            unselect()
                                        }

                                        BackHandler(enabled = selection.isEmpty()) {
                                            isAuth = true
                                            selectedConvo.value = null
                                            selectedConvoMessages = null
                                            selectedConvoBroken = null
                                        }
                                    }

                                    Scaffold(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .navigationBarsPadding(),
                                        topBar = {
                                            TopAppBar(
                                                title = {
                                                    if (selection.isEmpty()) {
                                                        Text(
                                                            "NoWire",
                                                            style = MaterialTheme.typography.displaySmall
                                                        )
                                                    }
                                                },
                                                actions = {
                                                    if (selection.isNotEmpty()) {
                                                        IconButton(onClick = {
                                                            val chats = appState.chats.filter {
                                                                selection.contains(it.id)
                                                            }
                                                            val allAlreadyPinned =
                                                                chats.all { it.isPinned() }
                                                            if (allAlreadyPinned) {
                                                                chats.forEach { it.pinnedTime = 0 }
                                                            } else {
                                                                chats.stream()
                                                                    .filter { !it.isPinned() }
                                                                    .forEach {
                                                                        it.pinnedTime =
                                                                            System.currentTimeMillis()
                                                                    }
                                                            }

                                                            shouldRecompose.intValue += 1
                                                            unselect()
                                                        }) {
                                                            Icon(
                                                                Icons.Sharp.PushPin,
                                                                stringResource(R.string.icon_desc_pin_selected_chats)
                                                            )
                                                        }
                                                        IconButton(onClick = {
                                                            for (chat in appState.chats) {
                                                                if (selection.contains(chat.id)) {
                                                                    chat.amountUnread = 0
                                                                }
                                                            }
                                                            shouldRecompose.intValue += 1
                                                            unselect()
                                                        }) {
                                                            Icon(
                                                                Icons.Sharp.MarkEmailRead,
                                                                stringResource(R.string.icon_desc_mark_selected_chats_as_read)
                                                            )
                                                        }
                                                        IconButton(onClick = {
                                                            showDeletePrompt = true
                                                        }) {
                                                            Icon(
                                                                Icons.Filled.DeleteForever,
                                                                stringResource(R.string.icon_desc_delete_selected_chats)
                                                            )
                                                        }
                                                    } else {
                                                        IconButton(onClick = {
                                                            showSettings = true
                                                        }) {
                                                            Icon(
                                                                Icons.Filled.Settings,
                                                                stringResource(R.string.icon_desc_open_settings),
                                                            )
                                                        }
                                                    }
                                                },
                                                navigationIcon = {
                                                    if (selection.isNotEmpty()) {
                                                        IconButton(onClick = {
                                                            unselect()
                                                        }) {
                                                            Icon(
                                                                Icons.AutoMirrored.Filled.ArrowBack,
                                                                stringResource(R.string.icon_desc_unselect_chats)
                                                            )
                                                        }
                                                    }
                                                }
                                            )
                                        },
                                        floatingActionButton = {
                                            if (selection.isEmpty()) {
                                                NewChatButton(dialogToShow)
                                            }
                                        }
                                    ) { innerPadding ->
                                        Box(modifier = Modifier.padding(innerPadding)) {
                                            when (dialogToShow.value) {
                                                MainDialogToShow.ONLINE_SETUP -> OnlineSetupDialog(
                                                    appState,
                                                    shouldRecompose,
                                                    dialogToShow
                                                )

                                                MainDialogToShow.OFFLINE_SETUP_IMPORT -> {
                                                    var permGranted by remember {
                                                        mutableStateOf(
                                                            false
                                                        )
                                                    }

                                                    RequestCameraPermission {
                                                        permGranted = it
                                                        if (!it) {
                                                            dialogToShow.value =
                                                                MainDialogToShow.NONE
                                                        }
                                                    }

                                                    if (permGranted) {
                                                        OfflineSetupImportDialog(
                                                            appState,
                                                            shouldRecompose,
                                                            dialogToShow,
                                                        )
                                                    }
                                                }

                                                MainDialogToShow.OFFLINE_SETUP_GENERATE -> {
                                                    var permGranted by remember {
                                                        mutableStateOf(
                                                            false
                                                        )
                                                    }

                                                    RequestCameraPermission {
                                                        permGranted = it
                                                        if (!it) {
                                                            dialogToShow.value =
                                                                MainDialogToShow.NONE
                                                        }
                                                    }

                                                    if (permGranted) {
                                                        OfflineSetupGenerateDialog(
                                                            shouldRecompose,
                                                            appState,
                                                            dialogToShow
                                                        )
                                                    }
                                                }

                                                MainDialogToShow.NONE -> {}
                                            }
                                            ChatView(appState.chats, selectedConvo, selection)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun RequestCameraPermission(r: (Boolean) -> Unit) {
    val permState = rememberPermissionState(Manifest.permission.CAMERA)
    if (permState.status.isGranted) {
        r(true)
    } else {
        var tmp by remember { mutableStateOf(true) }
        val scope = rememberCoroutineScope()
        if (tmp) {
            val configuration = LocalConfiguration.current

            val screenHeight = configuration.screenHeightDp.dp
            val screenWidth = configuration.screenWidthDp.dp

            val l = (screenHeight.value * 0.03f).coerceAtLeast(14f).dp
            val spacerSize = l

            Dialog(onDismissRequest = {
                tmp = false
            }) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    val s = RoundedCornerShape(16.dp)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.surfaceContainer,
                                shape = s
                            )
                            .padding(32.dp)
                    ) {
                        Text(
                            stringResource(R.string.camera_permission_reason),
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(Modifier.size(spacerSize))
                        Row {
                            Button(onClick = {
                                scope.launch {
                                    permState.launchPermissionRequest()
                                }
                            }) {
                                Text(
                                    stringResource(R.string.confirm),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                            Spacer(Modifier.size(spacerSize / 2))
                            Button(onClick = {
                                tmp = false
                            }) {
                                Text(
                                    stringResource(R.string.cancel),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }

                }
            }
        } else {
            r(false)
        }
    }
}

@Composable
fun OfflineSetupImportDialog(
    appState: AppState,
    recompose: MutableState<Int>,
    show: MutableState<MainDialogToShow>
) {
    var show by show

    var otherPublicKeyB64 by remember { mutableStateOf("") }
    var rootRatchetKeyB64 by remember { mutableStateOf("") }

    val selfSignKeypair = remember { LibSodiumUtils.generateSignKeyPair() }

    Dialog(
        onDismissRequest = { show = MainDialogToShow.NONE },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        )
    ) {
        Box(modifier = Modifier.safeDrawingPadding()) {
            var stage by remember { mutableIntStateOf(0) }

            when (stage) {
                0 -> {
                    val scannedCode = remember { mutableStateOf("") }

                    if (scannedCode.value.isNotEmpty()) {
                        val obj = Json.decodeFromString<HashMap<String, String>>(scannedCode.value)

                        rootRatchetKeyB64 = obj["p1"]!!
                        otherPublicKeyB64 = obj["p2"]!!

                        stage = 1
                    } else {
                        QRCodeScanner(scannedCode)
                    }
                }

                1 -> {
                    val context = LocalContext.current
                    DisposableEffect(Unit) {
                        setBrightness(context, isFull = true)
                        onDispose {
                            setBrightness(context, isFull = false)
                        }
                    }

                    val asJson = Json.encodeToString(
                        mapOf(
                            Pair(
                                "r1",
                                LibSodiumUtils.toBase64(selfSignKeypair.signPublicKey).get()
                            )
                        )
                    )
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        BoxWithConstraints(
                            Modifier.background(Color.White),
                            contentAlignment = Alignment.Center,
                        ) {
                            val s = minOf(maxWidth, maxHeight)
                            QrCodeView(
                                data = asJson, modifier = Modifier
                                    .size(s)
                                    .padding(16.dp)
                            )
                        }
                        Spacer(Modifier.padding(32.dp))
                        Row {
                            Button(
                                onClick = { stage = 2 },
                                modifier = Modifier.padding(end = 16.dp)
                            ) { Text(stringResource(R.string.confirm)) }
                            Button(onClick = {
                                show = MainDialogToShow.NONE
                            }) { Text(stringResource(R.string.not_confirm)) }
                        }
                    }

                }

                2 -> {
                    var alias by remember { mutableStateOf("") }

                    Column {
                        Row {
                            Button(
                                onClick = {
                                    val rootRatchetKey =
                                        LibSodiumUtils.toBytes(rootRatchetKeyB64).get()
                                    val otherPublicKey =
                                        LibSodiumUtils.toBytes(otherPublicKeyB64).get()

                                    val rootRatchet = Ratchet(rootRatchetKey)

                                    val receiveRatchet =
                                        Ratchet(rootRatchet.next().get().symmetricKey)
                                    val sendRatchet = Ratchet(rootRatchet.next().get().symmetricKey)

                                    appState.chats.add(
                                        Chat(
                                            System.currentTimeMillis(),
                                            sendRatchet,
                                            receiveRatchet,
                                            otherPublicKey,
                                            alias,
                                            selfSignKeypair.signPublicKey,
                                            selfSignKeypair.signSecretKey,
                                            mutableListOf(),
                                            0,
                                            0,
                                            false,
                                            System.currentTimeMillis(),
                                        )
                                    )
                                    appState.saveAsEncryptedJson()
                                    show = MainDialogToShow.NONE
                                    recompose.value += 1
                                },
                                enabled = alias.isNotEmpty(),
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Text(stringResource(R.string.confirm))
                            }
                            Button(onClick = {
                                show = MainDialogToShow.NONE
                            }) { Text(stringResource(R.string.not_confirm)) }
                        }
                        TextField(
                            value = alias,
                            maxLines = 1,
                            onValueChange = { alias = it })
                    }
                }
            }
        }
    }
}

@Composable
fun OfflineSetupGenerateDialog(
    shouldRecompose: MutableState<Int>,
    appState: AppState,
    show: MutableState<MainDialogToShow>
) {
    var show by show

    val keyPair = remember {
        LibSodiumUtils.generateSignKeyPair()
    }

    val rootRatchetKey = remember {
        LibSodiumUtils.generateSymmetricKey()
    }

    val b64RootRatchetKey = remember {
        LibSodiumUtils.toBase64(rootRatchetKey).get()
    }

    val b64PublicKey = remember {
        LibSodiumUtils.toBase64(keyPair.signPublicKey).get()
    }

    val jsonObject = mapOf(Pair("p1", b64RootRatchetKey), Pair("p2", b64PublicKey))

    val asJson = Json.encodeToString(jsonObject)

    var otherSignPublicKeyB64 by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = { show = MainDialogToShow.NONE },
        DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false

        ),
    ) {
        Box(modifier = Modifier.safeDrawingPadding()) {
            var stage by remember { mutableIntStateOf(0) }

            when (stage) {
                0 -> {
                    val context = LocalContext.current
                    DisposableEffect(Unit) {
                        setBrightness(context, isFull = true)
                        onDispose {
                            setBrightness(context, isFull = false)
                        }
                    }
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        BoxWithConstraints(
                            Modifier.background(Color.White),
                            contentAlignment = Alignment.Center,
                        ) {
                            val s = minOf(maxWidth, maxHeight)
                            QrCodeView(
                                data = asJson, modifier = Modifier
                                    .size(s)
                                    .padding(16.dp)
                            )
                        }
                        Spacer(Modifier.padding(32.dp))
                        Row {
                            Button(
                                onClick = { stage = 1 },
                                modifier = Modifier.padding(end = 16.dp)
                            ) { Text(stringResource(R.string.confirm)) }
                            Button(onClick = {
                                show = MainDialogToShow.NONE
                            }) { Text(stringResource(R.string.not_confirm)) }
                        }
                    }
                }

                1 -> {
                    BackHandler {
                        show = MainDialogToShow.NONE
                    }

                    val scannedQrCode = remember { mutableStateOf("") }
                    if (scannedQrCode.value.isNotEmpty()) {
                        val m = Json.decodeFromString<HashMap<String, String>>(scannedQrCode.value)
                        otherSignPublicKeyB64 = m["r1"]!!
                        stage = 2
                    } else {
                        QRCodeScanner(scannedQrCode)
                    }
                }

                2 -> {
                    var alias by remember { mutableStateOf("") }

                    Column {
                        Row {
                            Button(
                                onClick = {
                                    val rootRatchet = Ratchet(rootRatchetKey)

                                    val sendKey = rootRatchet.next().get().symmetricKey
                                    val recvKey = rootRatchet.next().get().symmetricKey

                                    val otherPublicKey =
                                        LibSodiumUtils.toBytes(otherSignPublicKeyB64).get()

                                    appState.chats.add(
                                        Chat(
                                            System.currentTimeMillis(),
                                            Ratchet(sendKey),
                                            Ratchet(recvKey),
                                            otherPublicKey,
                                            alias,
                                            keyPair.signPublicKey,
                                            keyPair.signSecretKey,
                                            mutableListOf(),
                                            0,
                                            0,
                                            false,
                                            System.currentTimeMillis(),
                                        )
                                    )
                                    appState.saveAsEncryptedJson()
                                    show = MainDialogToShow.NONE
                                    shouldRecompose.value += 1
                                },
                                enabled = alias.isNotEmpty(),
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Text(stringResource(R.string.confirm))
                            }
                            Button(onClick = {
                                show = MainDialogToShow.NONE
                            }) { Text(stringResource(R.string.not_confirm)) }
                        }
                        TextField(
                            value = alias,
                            maxLines = 1,
                            onValueChange = { alias = it })
                    }
                }
            }

        }
    }
}

@Composable
fun OnlineSetupDialog(
    appState: AppState,
    recompose: MutableState<Int>,
    show: MutableState<MainDialogToShow>
) {
    var show by show

    var roomName by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var alias by remember { mutableStateOf("") }

    val shouldCancel = remember { AtomicBoolean(false) }

    val configuration = LocalConfiguration.current

    val screenHeight = configuration.screenHeightDp.dp
    val screenWidth = configuration.screenWidthDp.dp

    val spacerSize = (screenHeight.value * 0.03f).coerceAtLeast(14f).dp

    var stage by remember { mutableIntStateOf(0) }

    Dialog(
        onDismissRequest = {
            if (stage == 0) {
                show = MainDialogToShow.NONE
            } else {
                shouldCancel.set(true)
            }
        },
    ) {
        when (stage) {
            0 -> {
                Column {
                    TextField(
                        value = roomName,
                        modifier = Modifier.fillMaxWidth(),
                        onValueChange = { roomName = it },
                        maxLines = 1,
                        label = { Text(stringResource(R.string.room)) }
                    )
                    TextField(
                        value = pin,
                        modifier = Modifier.fillMaxWidth(),
                        onValueChange = { pin = it },
                        label = { Text("PIN") },
                        maxLines = 1
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                    TextField(
                        value = alias,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        maxLines = 1,
                        onValueChange = { alias = it },
                        label = { Text(stringResource(R.string.contact_name)) }
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = {
                                stage = 1
                            },
                            enabled = roomName.isNotBlank()
                                    && pin.isNotBlank()
                                    && alias.isNotBlank()
                        ) {
                            Text(stringResource(R.string.start))
                        }
                    }
                }
            }

            1 -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    var showNetworkErrorDialog by remember { mutableStateOf(false) }

                    if (showNetworkErrorDialog) {
                        val dismiss = {
                            showNetworkErrorDialog = false
                            show = MainDialogToShow.NONE
                        }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            val s = RoundedCornerShape(16.dp)
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .background(
                                        MaterialTheme.colorScheme.surfaceContainer,
                                        shape = s
                                    )
                                    .padding(32.dp)
                            ) {
                                Text(stringResource(R.string.network_error))
                                Spacer(Modifier.size(spacerSize))
                                Button(onClick = { dismiss() }) {
                                    Text(
                                        stringResource(R.string.confirm),
                                        style = MaterialTheme.typography.bodyLarge,
                                    )
                                }
                            }
                        }
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            val s = RoundedCornerShape(16.dp)
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .background(
                                        MaterialTheme.colorScheme.surfaceContainer,
                                        shape = s
                                    )
                                    .padding(32.dp)
                            ) {
                                Text(
                                    stringResource(R.string.waiting_for_other_party),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Spacer(Modifier.size(spacerSize))
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .background(
                                            MaterialTheme.colorScheme.surfaceContainerHigh,
                                            shape = s
                                        )
                                        .padding(16.dp)

                                ) {
                                    Text(
                                        "${stringResource(R.string.room)}: $roomName",
                                        color = MaterialTheme.colorScheme.onSurface,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Spacer(Modifier.size(spacerSize / 2))
                                    Text(
                                        "PIN: $pin",
                                        color = MaterialTheme.colorScheme.onSurface,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }

                                Spacer(Modifier.size(spacerSize))
                                CircularProgressIndicator(
                                    modifier = Modifier.width(64.dp),
                                    color = MaterialTheme.colorScheme.secondary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                )
                                Spacer(Modifier.size(spacerSize))

                                LaunchedEffect(show) {
                                    try {
                                        val rootRatchetKey = LibSodiumUtils.generateSymmetricKey()
                                        val skp = LibSodiumUtils.generateSignKeyPair()

                                        val (s, nonce) = createSslConnectionAsync(5000)
                                        try {
                                            if (isActive) {
                                                val roomJoinData = RoomJoinData(
                                                    LibSodiumUtils.toBase64(rootRatchetKey).get(),
                                                    LibSodiumUtils.toBase64(skp.signPublicKey).get()
                                                )

                                                val dataKey =
                                                    LibSodiumUtils.stringToSymmetricKey(pin, "")
                                                        .get()
                                                val encryptedJson = LibSodiumUtils.toBase64(
                                                    LibSodiumUtils.stringSymmetricEncrypt(
                                                        dataKey, Json.encodeToString(roomJoinData)
                                                    ).get()
                                                ).get()

                                                val packet = PacketInJoinOnBandExchange(
                                                    toSha256(roomName),
                                                    encryptedJson
                                                )
                                                val packetString =
                                                    "on_band_join\n" + Json.encodeToString(packet)

                                                withContext(Dispatchers.IO) {
                                                    s.outputStream.write(packetString.encodeToByteArray())
                                                    s.outputStream.flush()
                                                }

                                                while (isActive && !shouldCancel.get()) {
                                                    Log.i("onlnie setup", "in loop")
                                                    val chat = withContext(Dispatchers.IO) {
                                                        var resBuf = ByteArray(2048)
                                                        val n = s.inputStream.read(resBuf)
                                                        if (n == -1) {
                                                            return@withContext null
                                                        }
                                                        resBuf = resBuf.take(n).toByteArray()

                                                        val asStr = String(
                                                            resBuf,
                                                            charset = Charsets.UTF_8
                                                        )

                                                        if (asStr.startsWith("online_setup_ping")) {
                                                            return@withContext null
                                                        }

                                                        val deserialized =
                                                            Json.decodeFromString<PacketOutOnBandExchangeJoined>(
                                                                asStr
                                                            )

                                                        val decryptedData =
                                                            LibSodiumUtils.stringSymmetricDecrypt(
                                                                dataKey,
                                                                LibSodiumUtils.toBytes(deserialized.params_b64)
                                                                    .get()
                                                            ).getOrNull()

                                                        if (decryptedData != null) {
                                                            val asRoomJoinData =
                                                                Json.decodeFromString<RoomJoinData>(
                                                                    decryptedData
                                                                )

                                                            val rootRatchet =
                                                                if (deserialized.creator) {
                                                                    Ratchet(rootRatchetKey)
                                                                } else {
                                                                    Ratchet(
                                                                        LibSodiumUtils.toBytes(
                                                                            asRoomJoinData.root_ratchet_key_b64
                                                                        )
                                                                            .get()
                                                                    )
                                                                }

                                                            val r1 =
                                                                Ratchet(
                                                                    rootRatchet.next()
                                                                        .get().symmetricKey
                                                                )
                                                            val r2 =
                                                                Ratchet(
                                                                    rootRatchet.next()
                                                                        .get().symmetricKey
                                                                )

                                                            val sendRatchet: Ratchet
                                                            val recvRatchet: Ratchet

                                                            if (deserialized.creator) {
                                                                sendRatchet = r1
                                                                recvRatchet = r2
                                                            } else {
                                                                sendRatchet = r2
                                                                recvRatchet = r1
                                                            }
                                                            val chat = Chat(
                                                                System.currentTimeMillis(),
                                                                sendRatchet,
                                                                recvRatchet,
                                                                LibSodiumUtils.toBytes(
                                                                    asRoomJoinData.self_public_key_b64
                                                                )
                                                                    .get(),
                                                                alias,
                                                                skp.signPublicKey,
                                                                skp.signSecretKey,
                                                                mutableListOf(),
                                                                0,
                                                                0,
                                                                false,
                                                                System.currentTimeMillis(),
                                                            )
                                                            chat
                                                        } else {
                                                            null
                                                        }
                                                    }

                                                    if (chat != null) {
                                                        if (!chat.otherSignPublicKey.contentEquals(chat.selfSignPublicKey)) {
                                                            appState.chats.add(chat)
                                                            appState.saveAsEncryptedJson()
                                                            recompose.value += 1
                                                        }
                                                        break
                                                    }
                                                    delay(500)
                                                }

                                                withContext(Dispatchers.IO) {
                                                    s.outputStream.write("clear_my_room\n".encodeToByteArray())
                                                    s.outputStream.flush()
                                                }

                                                Log.i("onlnie setup", "closed")
                                            }
                                        } catch (e: Exception) {
                                            throw e
                                        } finally {
                                            withContext(Dispatchers.IO) {
                                                s.close()
                                            }
                                        }
                                        show = MainDialogToShow.NONE
                                    } catch (e: Exception) {
                                        Log.i("EEE", e.javaClass.name)
                                        showNetworkErrorDialog = true
                                    }
                                }

                                Spacer(Modifier.size(spacerSize))
                                Button(onClick = { shouldCancel.set(true) }) {
                                    Text(
                                        stringResource(R.string.cancel),
                                        style = MaterialTheme.typography.bodyMedium
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    prefs: SharedPreferences,
    updateColorScheme: (ThemeColorMode) -> Unit,
    backToMain: () -> Unit
) {

    BackHandler {
        backToMain()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(),
                title = {
                    Text(
                        stringResource(R.string.settings),
                        style = MaterialTheme.typography.headlineMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        backToMain()
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            stringResource(R.string.icon_desc_opened_chat_go_back)
                        )
                    }
                })
        },
        modifier = Modifier.navigationBarsPadding()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                stringResource(R.string.settings_design_section),
                style = MaterialTheme.typography.titleLarge
            )
            Row {
                var selectedThemeMode by remember {
                    mutableStateOf(
                        prefs.getString(
                            "color_scheme_mode",
                            ThemeColorMode.SYSTEM.name
                        )!!
                    )
                }

                val labelStyle = MaterialTheme.typography.bodyMedium

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    RadioButton(selectedThemeMode == ThemeColorMode.LIGHT_MODE.name, onClick = {
                        selectedThemeMode = ThemeColorMode.LIGHT_MODE.name
                        updateColorScheme(ThemeColorMode.LIGHT_MODE)
                    })
                    Text(stringResource(R.string.settings_theme_light), style = labelStyle)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    RadioButton(selectedThemeMode == ThemeColorMode.DARK_MODE.name, onClick = {
                        selectedThemeMode = ThemeColorMode.DARK_MODE.name
                        updateColorScheme(ThemeColorMode.DARK_MODE)
                    })
                    Text(stringResource(R.string.settings_theme_dark), style = labelStyle)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    RadioButton(selectedThemeMode == ThemeColorMode.SYSTEM.name, onClick = {
                        selectedThemeMode = ThemeColorMode.SYSTEM.name
                        updateColorScheme(ThemeColorMode.SYSTEM)
                    })
                    Text(stringResource(R.string.settings_theme_system), style = labelStyle)
                }

                LaunchedEffect(selectedThemeMode) {
                    with(prefs.edit()) {
                        putString("color_scheme_mode", selectedThemeMode)
                        apply()
                    }
                }
            }
        }
    }
}

@Composable
fun NewChatButton(show: MutableState<MainDialogToShow>) {
    var showDropDown by remember { mutableStateOf(false) }

    var show by show

    val pad = Modifier.padding(bottom = 16.dp)
    val shape = Modifier
        .clip(RoundedCornerShape(16.dp))
        .background(MaterialTheme.colorScheme.primaryContainer)
    val itemMod = pad.then(shape)

    Column {
        DropdownMenu(
            expanded = showDropDown,
            onDismissRequest = { showDropDown = !showDropDown },
            shadowElevation = 0.dp,
            containerColor = Color.Transparent,
        ) {
            DropdownMenuItem(
                text = {
                    Text(
                        stringResource(R.string.start_offline_setup),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                },
                trailingIcon = {
                    Icon(
                        Icons.Outlined.Lock,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        contentDescription = null,
                    )
                },
                onClick = {
                    showDropDown = false
                    show = MainDialogToShow.OFFLINE_SETUP_GENERATE
                },
                modifier = itemMod,
            )
            DropdownMenuItem(
                text = {
                    Text(
                        stringResource(R.string.import_offline_setup),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                },
                trailingIcon = {
                    Icon(
                        Icons.Outlined.Lock,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        contentDescription = null,
                    )
                },
                onClick = {
                    showDropDown = false
                    show = MainDialogToShow.OFFLINE_SETUP_IMPORT
                },
                modifier = itemMod,
            )
            DropdownMenuItem(
                text = {
                    Text(
                        stringResource(R.string.online_setup),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                },
                trailingIcon = {
                    Icon(
                        Icons.Outlined.Email,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        contentDescription = null,
                    )
                },
                onClick = {
                    showDropDown = false
                    show = MainDialogToShow.ONLINE_SETUP
                },
                modifier = itemMod.padding(bottom = 0.dp),
            )
        }

        FloatingActionButton(
            onClick = { showDropDown = true },
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ) {
            Icon(Icons.Filled.AddCircle, stringResource(R.string.icon_desc_add_chat_button))
        }
    }
}

@Serializable
data class SimpleDate(
    val dayOfMonth: Int,
    val month: Int,
    val year: Int,

    val hour: Int,
    val minute: Int
) {

    fun day(): Day = Day(dayOfMonth, month, year)
}

@Serializable
data class Day(
    val dayOfMonth: Int,
    val month: Int,
    val year: Int,
)

fun dt(msg: Message): SimpleDate {
    val c = Calendar.getInstance()
    c.timeInMillis = msg.timestamp

    val dayOfMonth = c.get(Calendar.DAY_OF_MONTH)
    val month = c.get(Calendar.MONTH)
    val year = c.get(Calendar.YEAR)

    val hour = c.get(Calendar.HOUR_OF_DAY)
    val minute = c.get(Calendar.MINUTE)

    return SimpleDate(dayOfMonth, month, year, hour, minute)
}

private val brokenChatMessage = UUID.fromString("1179979b-a2ba-4d36-a82f-74872ab5de24")

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageList(
    self: ByteArray,
    messages: SnapshotStateList<Message>,
    selectedMessages: SnapshotStateList<UUID>,
    deliveryStages: SnapshotStateMap<String, DeliveryStage>,
    respondingTo: MutableState<Message?>,
    alias: String,
) {
    val state = rememberLazyListState()
    val coScope = rememberCoroutineScope()
    var highlighted by remember { mutableStateOf<UUID?>(null) }
    val dateFmt = SimpleDateFormat.getDateInstance(SimpleDateFormat.LONG)
    val timeFmt = SimpleDateFormat.getTimeInstance(SimpleDateFormat.SHORT)

    Box {
        val userHasScrolled = remember { derivedStateOf { state.firstVisibleItemIndex } }.value > 0

        LaunchedEffect(messages.size) {
            if (!userHasScrolled) {
                state.scrollToItem(0)
            }
        }

        LazyColumn(
            reverseLayout = true,
            modifier = Modifier.fillMaxSize(),
            state = state,
        ) {
            items(
                count = messages.size,
                key = { messages.size - it - 1 }) {
                val me = messages.size - it - 1
                val prev = messages.getOrNull(me - 1)
                val t = messages[me]

                val date = dt(t)
                val showDateHeader = when {
                    prev == null -> 1
                    dt(prev).day() != date.day() -> 2
                    else -> 0
                }

                val senderChange =
                    prev != null && !prev.senderSignPublicKey.contentEquals(t.senderSignPublicKey)
                val sentByMe = self.contentEquals(t.senderSignPublicKey)

                val asDate = Date(t.timestamp)

                Column {
                    if (showDateHeader != 0) {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                            ) {
                                val formatted = dateFmt.format(asDate)

                                Text(
                                    text = formatted,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                    } else if (senderChange) {
                        Spacer(Modifier.padding(top = 8.dp))
                    }

                    val sw = LocalConfiguration.current.screenWidthDp
                    val textBoxShape = RoundedCornerShape(8.dp)

                    var highlightedColor by remember { mutableStateOf(Color.Transparent) }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(highlightedColor, shape = textBoxShape)
                    ) {
                    Box(contentAlignment = Alignment.CenterStart) {

                        var drag by remember { mutableFloatStateOf(0f) }

                        val dragMax = 90.0f

                        val ss = rememberDraggableState {
                            var m = it
                            if (drag >= dragMax * 2) {
                                m *= 0.3f
                            }
                            drag = (drag + m * 0.5f).coerceAtLeast(0.0f)
                        }


                        var fullRowMod = Modifier
                            .clip(textBoxShape)
                            .padding(horizontal = 16.dp, vertical = 2.dp)
                            .width(sw.dp)
                            .offset { IntOffset(x = ceil(drag).toInt(), y = 0) }
                            .combinedClickable(
                                onClick = {
                                    if (selectedMessages.isNotEmpty()) {
                                        if (selectedMessages.contains(t.id)) {
                                            selectedMessages.remove(t.id)
                                        } else {
                                            selectedMessages.add(t.id)
                                        }
                                    }
                                },
                                onLongClick = {
                                    selectedMessages.add(t.id)
                                }
                            )
                            .draggable(ss, Orientation.Horizontal, onDragStopped = {
                                drag = 0.0f
                            })

                        if (highlighted != null && highlighted == t.id) {
                            var color = MaterialTheme.colorScheme.tertiaryContainer
                            val amountSteps = 80
                            val highlightedDuration = 4000L
                            DisposableEffect(Unit) {
                                val job = coScope.launch {
                                    val alphaToRemove = 1.0f / amountSteps
                                    val delayPerStep = highlightedDuration / amountSteps
                                    for (i in 1..amountSteps) {
                                        val newAlpha =
                                            (color.alpha - alphaToRemove).coerceAtLeast(0.0f)
                                        if (newAlpha <= 0.3f) {
                                            break
                                        }
                                        color = color.copy(newAlpha)
                                        highlightedColor = color
                                        delay(delayPerStep)
                                    }
                                    highlightedColor = Color.Transparent
                                    highlighted = null
                                }

                                onDispose {
                                    highlightedColor = Color.Transparent
                                    job.cancel()
                                }
                            }
                        }

                        val l = LocalHapticFeedback.current

                        if (selectedMessages.contains(t.id)) {
                            fullRowMod = fullRowMod.background(
                                MaterialTheme.colorScheme.surfaceContainerHigh,
                                shape = textBoxShape
                            )
                        }
                        val iconStartDrag = 40.0f
                        if (drag >= iconStartDrag) {
                            val x = (drag - iconStartDrag).coerceAtLeast(0.0f)
                            var iconMod = Modifier.offset {
                                IntOffset(x = ceil(x.coerceAtMost(dragMax)).toInt(), y = 0)
                            }

                            if (x < dragMax) {
                                iconMod = iconMod.alpha(x / dragMax)
                            } else {
                                LaunchedEffect(Unit) {
                                    l.performHapticFeedback(HapticFeedbackType.LongPress)
                                    respondingTo.value = t
                                }
                            }

                            Icon(Icons.Outlined.TurnLeft, null, modifier = iconMod)
                        }
                        Row(
                            modifier = fullRowMod,
                            horizontalArrangement = if (sentByMe) Arrangement.End else Arrangement.Start
                        ) {
                            Column(
                                modifier = Modifier
                                    .widthIn(max = (sw * 0.7).dp)
                                    .width(IntrinsicSize.Max)
                                    .background(
                                        color = if (sentByMe) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                                        shape = textBoxShape,
                                    )
                            ) {
                                val r = t.respondingTo
                                if (r != null) {
                                    val sentByOtherColor = MaterialTheme.colorScheme.tertiary

                                    val rTo = messages.findLast { it.id == r }
                                    if (rTo != null) {
                                        val rToByMe = rTo.senderSignPublicKey.contentEquals(self)

                                        val col = if (rToByMe) {
                                            MaterialTheme.colorScheme.onSecondaryContainer
                                        } else {
                                            sentByOtherColor
                                        }

                                        val textStyle = MaterialTheme.typography.bodySmall

                                        Row(
                                            modifier = Modifier
                                                .padding(
                                                    top = 4.dp,
                                                    start = 8.dp,
                                                    end = 8.dp,
                                                )
                                                .fillMaxWidth()
                                                .height(IntrinsicSize.Max)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                                                .clickable {
                                                    val m = messages.indexOfLast {
                                                        it.id == r
                                                    }

                                                    val asScrollIdx = messages.size - m - 1

                                                    coScope.launch {
                                                        state.scrollToItem(asScrollIdx)
                                                        highlighted = r
                                                    }
                                                }
                                        ) {
                                            Box(
                                                Modifier
                                                    .padding(end = 8.dp)
                                                    .fillMaxHeight()
                                                    .width(4.dp)
                                                    .background(col)
                                            ) {}
                                            Column(
                                                Modifier
                                                    .padding(top = 4.dp, bottom = 4.dp, end = 8.dp)
                                                    .fillMaxHeight()
                                            ) {
                                                Text(
                                                    if (rToByMe) {
                                                        stringResource(R.string.you)
                                                    } else {
                                                        alias
                                                    }, style = textStyle.copy(color = col)
                                                )
                                                Text(
                                                    rTo.text,
                                                    style = textStyle.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                )
                                            }
                                        }
                                    }
                                }
                                Row(
                                    modifier = Modifier
                                        .padding(
                                            vertical = 4.dp,
                                            horizontal = 8.dp
                                        )
                                        .fillMaxWidth()
                                ) {
                                    val textColor =
                                        if (sentByMe) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface
                                    Text(
                                        t.text,
                                        color = textColor,
                                        style = MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier
                                            .padding(end = 8.dp)
                                            .weight(1.0f)
                                    )

                                    val formatted = timeFmt.format(asDate)

                                    Text(
                                        text = formatted,
                                        color = textColor,
                                        style = MaterialTheme.typography.labelSmall,
                                        softWrap = false,
                                        modifier = Modifier.align(Alignment.Bottom)
                                    )

                                    if (sentByMe) {
//                                        fun getIcon() = when (deliveryStages[t.id.toString()] ?: DeliveryStage.NOT_SENT) {
//                                            DeliveryStage.SENT -> Triple(0, false, Icons.Filled.Check)
//                                            DeliveryStage.NOT_SENT -> Triple(1, true, Icons.Outlined.AccessTime)
//                                        }
//
//                                        var iconData by remember { mutableStateOf(getIcon()) }
                                        val ic = when (deliveryStages[t.id.toString()]
                                            ?: DeliveryStage.SENT) {
                                            DeliveryStage.SENT -> Icons.Filled.Check
                                            DeliveryStage.NOT_SENT -> Icons.Outlined.AccessTime
                                        }
                                        Icon(
                                            ic, null, tint = textColor, modifier = Modifier
                                                .align(Alignment.Bottom)
                                                .padding(start = 8.dp)
                                                .size((MaterialTheme.typography.labelSmall.fontSize.value * 1.2).dp)
                                        )

//                                        if (iconData.second) {
//                                            LaunchedEffect(Unit) {
//                                                while (isActive) {
//                                                    val newIconData = getIcon()
//
//                                                    if (newIconData.first != iconData.first) {
//                                                        iconData = newIconData
//                                                    }
//
//                                                    delay(1000)
//                                                }
//                                            }
//                                        }

                                    }
                                }
                            }
                        }
                    }
                    }
                }
            }
        }
        if (userHasScrolled) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.End
            ) {
                IconButton(
                    onClick = {
                        coScope.launch {
                            state.scrollToItem(
                                0,
                                0
                            )
                        }
                    },

                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.8f),
                    )
                ) {
                    Icon(
                        Icons.Filled.ArrowDownward,
                        stringResource(R.string.icon_desc_message_list_scroll_back_to_bottom)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpenedChat(
    appState: AppState,
    selectedConvo: MutableState<Chat?>,
    messages: SnapshotStateList<Message>,
    deliveryStages: SnapshotStateMap<String, DeliveryStage>,
    selectedChatBroken: MutableState<Boolean>
) {
    fun goBack() {
        selectedConvo.value = null
    }

    val chat = selectedConvo.value ?: return

    val isBroken = selectedChatBroken.value

    var showError by remember { mutableStateOf(false) }
    var showDeletePrompt by remember { mutableStateOf(false) }

    val selectedMessages = remember { mutableStateListOf<UUID>() }

    fun clearSelection() {
        selectedMessages.clear()
    }

    LaunchedEffect(Unit) {
        messages.clear()
        messages.addAll(chat.messages)
    }

    val respondingTo: MutableState<Message?> = remember { mutableStateOf(null) }

    val focusRequester = remember { FocusRequester() }

    if (showDeletePrompt) {
        Dialog(onDismissRequest = {
            showDeletePrompt = false
        }) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                val configuration = LocalConfiguration.current

                val screenHeight = configuration.screenHeightDp.dp
                val screenWidth = configuration.screenWidthDp.dp

                val l = (screenHeight.value * 0.03f).coerceAtLeast(14f).dp
                val spacerSize = l
                val s = RoundedCornerShape(16.dp)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .background(MaterialTheme.colorScheme.surfaceContainer, shape = s)
                        .padding(32.dp)
                ) {
                    Text(
                        stringResource(R.string.delete_selected_messages),
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(Modifier.size(spacerSize))
                    Row {
                        Button(
                            modifier = Modifier.padding(end = 8.dp), onClick = {
                            val filter = { m: Message -> selectedMessages.contains(m.id) }

                            messages.removeIf(filter)
                            val anyRemoved =
                                selectedConvo.value?.messages?.removeIf(filter) ?: false
                            if (anyRemoved) {
                                appState.saveAsEncryptedJson()
                            }

                            clearSelection()
                            showDeletePrompt = false
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    stringResource(R.string.yes),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Icon(
                                    Icons.Filled.DeleteForever,
                                    null,
                                )
                            }
                        }
                        Button(onClick = {
                            showDeletePrompt = false
                        }) {
                            Text(
                                stringResource(R.string.no),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    } else {
        BackHandler {
            if (selectedMessages.isNotEmpty()) {
                clearSelection()
            } else {
                goBack()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors().run {
                    if (isBroken) {
                        copy(containerColor = MaterialTheme.colorScheme.errorContainer)
                    } else {
                        copy()
                    }
                },
                title = {
                    Text(
                        chat.alias,
                        style = MaterialTheme.typography.headlineMedium.copy(fontFamily = bodyFontFamily)
                    )
                },
                actions = {
                    if (selectedMessages.isNotEmpty()) {
                        IconButton(onClick = {
                            showDeletePrompt = true
                        }) {
                            Icon(
                                Icons.Filled.DeleteForever,
                                stringResource(R.string.icon_desc_delete_selected_messages)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (selectedMessages.isNotEmpty()) {
                            clearSelection()
                        } else {
                            goBack()
                        }
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            stringResource(R.string.icon_desc_opened_chat_go_back)
                        )
                    }
                })
        },
        modifier = Modifier.navigationBarsPadding()
    )
    { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            val selfKey = selectedConvo.value?.selfSignPublicKey
            if (selfKey != null) {
                Box(modifier = Modifier.weight(1.0f)) {
                    MessageList(
                        selfKey,
                        messages,
                        selectedMessages,
                        deliveryStages,
                        respondingTo,
                        chat.alias,
                    )
                }
            }
            if (selectedChatBroken.value) {
                Spacer(Modifier.padding(vertical = 8.dp))

                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Column(
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                shape = RoundedCornerShape(8.dp)
                            )
                    ) {
                        Text(
                            text = stringResource(R.string.network_error_chat_not_usable_anymore),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = stringResource(R.string.network_error_chat_unusable_please_recreate),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
            Spacer(Modifier.padding(vertical = 8.dp))
            var m = Modifier
                .fillMaxWidth()
                .width(IntrinsicSize.Max)
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .imePadding()

            val rowEnabled = selectedMessages.isEmpty()

            if (!rowEnabled) {
                m = m.alpha(0f)
            }
            Row(
                modifier = m,
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = if (respondingTo.value == null) Alignment.CenterVertically else Alignment.Bottom,
            ) {
                var messageText by remember { mutableStateOf("") }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1.0f)
                ) {
                    val respTo = respondingTo.value
                    if (respTo != null) {
                        val r = respTo.id
                        val sentByOtherColor = MaterialTheme.colorScheme.tertiary

                        val rTo = messages.findLast { it.id == r }
                        if (rTo != null) {
                            val rToByMe = rTo.senderSignPublicKey.contentEquals(selfKey)

                            val col = if (rToByMe) {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            } else {
                                sentByOtherColor
                            }

                            val textStyle = MaterialTheme.typography.bodySmall

                            Box(Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier
                                        .padding(
                                            top = 4.dp,
                                            bottom = 8.dp,
                                        )
                                        .fillMaxWidth()
                                        .height(IntrinsicSize.Max)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                                ) {
                                    Box(
                                        Modifier
                                            .padding(end = 8.dp)
                                            .fillMaxHeight()
                                            .width(4.dp)
                                            .background(col)
                                    ) {}
                                    Column(
                                        Modifier
                                            .padding(top = 4.dp, bottom = 4.dp, end = 8.dp)
                                            .fillMaxHeight()
                                    ) {
                                        Text(
                                            if (rToByMe) {
                                                stringResource(R.string.you)
                                            } else {
                                                chat.alias
                                            }, style = textStyle.copy(color = col)
                                        )
                                        Text(
                                            rTo.text,
                                            style = textStyle.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        )
                                    }
                                }
                                Icon(
                                    Icons.Outlined.Cancel,
                                    stringResource(R.string.icon_desc_cancel_respond),
                                    tint = Color.Gray,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(top = 8.dp, end = 4.dp)
                                        .size(16.dp)
                                        .clickable {
                                            respondingTo.value = null
                                        }
                                )
                            }
                        }
                    }

                    TextField(
                        enabled = rowEnabled && !selectedChatBroken.value,
                        value = messageText,
                        onValueChange = { messageText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                    )
                }

                if (rowEnabled) {
                    FloatingActionButton(
                        onClick = {
                            if (selectedChatBroken.value) {
                                return@FloatingActionButton
                            }

                            val m = messageText.trim()
                            if (m.isNotEmpty() && m.isNotBlank()) {
                                val msg = Message(
                                    UUID.randomUUID(),
                                    m,
                                    System.currentTimeMillis(),
                                    chat.selfSignPublicKey,
                                    respondingTo.value?.id
                                )

                                val messageWithIdJson = Json.encodeToString(
                                    MessageTextWithId(
                                        msg.id.toString(),
                                        msg.text,
                                        msg.respondingTo?.toString(),
                                    )
                                )

                                appState.sendQueue.offer(
                                    SendQueueEntry(
                                        chat.send.encryptString(messageWithIdJson).get(),
                                        chat.otherSignPublicKey,
                                        msg.id.toString(),
                                        msg.timestamp,
                                    )
                                )

                                chat.messages.add(msg)
                                deliveryStages[msg.id.toString()] = DeliveryStage.NOT_SENT
                                appState.deliveryStages[msg.id.toString()] = DeliveryStage.NOT_SENT
                                messages.add(msg)

                                messageText = ""
                                respondingTo.value = null
                                focusRequester.requestFocus()

                                appState.saveAsEncryptedJson()
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            stringResource(R.string.icon_desc_send_message_button)
                        )
                    }
                    LaunchedEffect(Unit) {
                        focusRequester.requestFocus()
                    }
                }
            }

            if (showError) {
                Dialog(onDismissRequest = { showError = false }) {
                    Text(stringResource(R.string.error_sending_message))
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatView(
    chats: List<Chat>,
    selectedConvo: MutableState<Chat?>,
    selection: SnapshotStateList<Long>,
    modifier: Modifier = Modifier
) {
    val chats = remember { chats.sortedWith(FullChatComparator) }

    val scrollState = rememberScrollState()

    val haptics = LocalHapticFeedback.current
    Column(modifier = modifier.verticalScroll(scrollState)) {
        if (chats.isEmpty()) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
            ) {
                Text(stringResource(R.string.no_chats_stored))
            }
        } else {
            for (chat in chats) {
                key(chat.id) {
                    ChatViewRow(
                        chat,
                        selection.contains(chat.id),
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = {
                                    if (selection.isNotEmpty()) {
                                        if (selection.contains(chat.id)) {
                                            selection.remove(chat.id)
                                        } else {
                                            selection.add(chat.id)
                                        }
                                    } else {
                                        selectedConvo.value = chat
                                        chat.amountUnread = 0
                                    }
                                },
                                onLongClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    selection.add(chat.id)
                                }
                            )
                    )
                }
            }
        }
    }
}

@Composable
fun ChatViewRow(chat: Chat, isSelected: Boolean, modifier: Modifier = Modifier) {
    var mod = modifier

    if (chat.broken) {
        mod = mod.then(Modifier.background(MaterialTheme.colorScheme.errorContainer))
    } else if (isSelected) {
        mod = mod.then(Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh))
    }

    Column(modifier = mod) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 8.dp, start = 8.dp, end = 8.dp)
        ) {
            Icon(
                Icons.Filled.AccountCircle,
                null,
                modifier = Modifier.size(48.dp)
            )
            Text(
                chat.alias, modifier = Modifier.padding(start = 8.dp),
                style = MaterialTheme.typography.titleLarge.copy(fontFamily = bodyFontFamily)
            )

            Spacer(Modifier.weight(1.0f))

            if (isSelected) {
                Icon(Icons.Filled.Check, null)
            } else {
                Icon(Icons.Filled.Check, null, modifier = Modifier.alpha(0f))
            }
            Spacer(Modifier.padding(horizontal = 8.dp))

            val textShape = CircleShape

            val boxMod = Modifier
                .clip(textShape)
                .background(MaterialTheme.colorScheme.secondaryContainer, shape = textShape)

            Box(
                modifier = if (chat.amountUnread > 0) boxMod else Modifier.alpha(0f),
                contentAlignment = Alignment.Center
            )
            {
                Text(
                    "${chat.amountUnread}",
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(vertical = 4.dp, horizontal = 12.dp)
                )
            }
            if (chat.isPinned()) {
                Icon(Icons.Sharp.PushPin, null)
            }
        }
        HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
fun PinField(pinBuffer: MutableState<String>) {
    var pinBuffer by pinBuffer

    fun enterChar(c: Char): () -> Unit = {
        if (pinBuffer == "ERROR") {
            pinBuffer = c.toString()
        } else if (pinBuffer.length < 4) {
            pinBuffer += c
        }
    }

    val configuration = LocalConfiguration.current

    val screenHeight = configuration.screenHeightDp.dp
    val screenWidth = configuration.screenWidthDp.dp

    val l = (screenHeight.value * 0.03f).coerceAtLeast(14f).dp
    val spacerSize = l

    Row {
        Column {
            PinInput(onClick = enterChar('1')) { PinText("1") }
            Spacer(Modifier.size(spacerSize))
            PinInput(onClick = enterChar('4')) { PinText("4") }
            Spacer(Modifier.size(spacerSize))
            PinInput(onClick = enterChar('7')) { PinText("7") }
        }
        Spacer(Modifier.size(spacerSize))
        Column {
            PinInput(onClick = enterChar('2')) { PinText("2") }
            Spacer(Modifier.size(spacerSize))
            PinInput(onClick = enterChar('5')) { PinText("5") }
            Spacer(Modifier.size(spacerSize))
            PinInput(onClick = enterChar('8')) { PinText("8") }
            Spacer(Modifier.size(spacerSize))
            PinInput(onClick = enterChar('0')) { PinText("0") }
        }
        Spacer(Modifier.size(spacerSize))
        Column {
            PinInput(onClick = enterChar('3')) { PinText("3") }
            Spacer(Modifier.size(spacerSize))
            PinInput(onClick = enterChar('6')) { PinText("6") }
            Spacer(Modifier.size(spacerSize))
            PinInput(onClick = enterChar('9')) { PinText("9") }
            Spacer(Modifier.size(spacerSize))
            PinInput(onClick = {
                if (pinBuffer == "ERROR") {
                    pinBuffer = ""
                } else if (pinBuffer.isNotEmpty()) {
                    pinBuffer = pinBuffer.substring(0, pinBuffer.length - 1)
                }
            }) {
                Icon(
                    Icons.AutoMirrored.Filled.Backspace,
                    stringResource(R.string.icon_desc_pin_field_backspace),
                    modifier = Modifier.size(l * 1.4f)
                )
            }
        }
    }
}

@Composable
fun PinText(x: String) {
    val configuration = LocalConfiguration.current

    val screenHeight = configuration.screenHeightDp.dp
    val screenWidth = configuration.screenWidthDp.dp

    Text(
        x,
        fontSize = (screenHeight.value * 0.05f).coerceAtLeast(14f).sp,
        fontWeight = FontWeight.ExtraBold
    )
}

@Composable
fun PinInput(
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    val configuration = LocalConfiguration.current

    val screenHeight = configuration.screenHeightDp.dp
    val screenWidth = configuration.screenWidthDp.dp
    val l = (screenHeight.value * 0.09f).coerceAtLeast(14f).dp

    val shape = CircleShape

    Box(
        Modifier
            .size(l)
            .clip(shape)
            .background(MaterialTheme.colorScheme.secondaryContainer, shape = shape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

private fun setBrightness(context: Context, isFull: Boolean) {
    val activity = context as? Activity ?: return
    val layoutParams: WindowManager.LayoutParams = activity.window.attributes
    layoutParams.screenBrightness = if (isFull) {
        1.0f
    } else {
        WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
    }
    activity.window.attributes = layoutParams
}

@Composable
fun QRCodeScanner(scannedCode: MutableState<String>) {
    val lifecycleOwner = LocalLifecycleOwner.current

    AndroidView(factory = { ctx ->
        val previewView = PreviewView(ctx)
        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().apply {
                setSurfaceProvider(previewView.surfaceProvider)
            }

            val analyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build().also {
                    it.setAnalyzer(
                        ContextCompat.getMainExecutor(ctx),
                        QRCodeAnalyzer { qrCode ->
                            scannedCode.value = qrCode
                        }
                    )
                }

            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                analyzer
            )
        }, ContextCompat.getMainExecutor(ctx))
        previewView
    }, modifier = Modifier.fillMaxSize())
}

fun createSslConnection(
    host: String = SERVER_HOST,
    port: Int = SERVER_PORT,
    timeout: Int?,
    cert: Certificate = MainActivity.cert
): SSLSocket {
    val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
    keyStore.load(null, null)
    keyStore.setCertificateEntry("server", cert)

    val trustManagerFactory = TrustManagerFactory.getInstance(
        TrustManagerFactory.getDefaultAlgorithm()
    )

    trustManagerFactory.init(keyStore)

    val sslContext = SSLContext.getInstance("TLS")
    sslContext.init(null, trustManagerFactory.trustManagers, SecureRandom())

    val socketFactory = sslContext.socketFactory
    val socket = socketFactory.createSocket(host, port) as SSLSocket
    socket.soTimeout = timeout ?: 0
    socket.startHandshake()

    return socket
}

suspend fun createSslConnectionAsync(timeout: Int? = 5000): Pair<SSLSocket, ByteArray> =
    withContext(Dispatchers.IO) {
        val s = createSslConnection(timeout = timeout)
        val nonce = ByteArray(JnaSodium.NONCE_BYTES)
        s.inputStream.read(nonce)
        Pair(s, nonce)
    }

suspend fun ping(socket: SSLSocket, mutex: Mutex): Boolean = withContext(Dispatchers.IO) {
    mutex.withLock {
        val confirm = System.currentTimeMillis().toString()
        socket.outputStream.write("ping\n$confirm".encodeToByteArray())
        socket.outputStream.flush()
        val buf = ByteArray(128)
        val n = socket.inputStream.read(buf)
        if (n > 0) {
            String(buf.take(n).toByteArray(), charset = Charsets.UTF_8) == confirm
        } else {
            false
        }
    }
}

suspend fun getNewMessages(
    socket: SSLSocket,
    nonce: ByteArray,
    pullUpdates: List<Pair<ByteArray, ByteArray>>,
): List<Pair<ByteArray, ByteArray>> = withContext(Dispatchers.IO) {
    val out = mutableListOf<Pair<ByteArray, ByteArray>>()
    pullUpdates.forEach { toPull ->
        try {
            val p = mapOf(
                Pair("from_b64", LibSodiumUtils.toBase64(toPull.first).get()),
                Pair(
                    "sig_b64", LibSodiumUtils.toBase64(
                        LibSodiumUtils.signBytes(
                            nonce,
                            toPull.second
                        ).get()
                    ).get()
                )
            )

            val packet = "get_messages\n" + Json.encodeToString(p)
            socket.outputStream.write(packet.encodeToByteArray())
            socket.outputStream.flush()

            var responseBuf = ByteArray(100_000)

            val n = socket.inputStream.read(responseBuf)

            if (n > 0) {
                responseBuf = responseBuf.take(n).toByteArray()

                val s = String(responseBuf, charset = Charsets.UTF_8)
                val deserialized = Json.decodeFromString<PacketOutMessages>(s)

                deserialized.messages.forEach {
                    out.add(Pair(toPull.first, LibSodiumUtils.toBytes(it.bin_b64).get()))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    out
}

suspend fun sendMessage(
    socket: SSLSocket,
    mutex: Mutex,
    encryptedMessage: ByteArray,
    receiver: ByteArray
): Boolean =
    withContext(Dispatchers.IO) {
        val receiverB64 = LibSodiumUtils.toBase64(receiver).get()
        val msgB64 = LibSodiumUtils.toBase64(encryptedMessage).get()

        val confirm = LibSodiumUtils.randomUniform(Int.MAX_VALUE).toString()

        val packet = PacketInSendMessage(receiverB64, msgB64, confirm)
        val packetBuf = "send_message\n" + Json.encodeToString(packet)

        mutex.withLock {
            socket.outputStream.write(packetBuf.encodeToByteArray())
            socket.outputStream.flush()

            val x = ByteArray(1024)
            val n = socket.inputStream.read(x)
            String(x.take(n).toByteArray(), charset = Charsets.UTF_8) == confirm
        }
    }

@Serializable
class RoomJoinData(val root_ratchet_key_b64: String, val self_public_key_b64: String)

@Serializable
class NetMessage(val bin_b64: String)

@Serializable
class PacketOutMessages(val messages: List<NetMessage>)

@Serializable
class PacketInJoinOnBandExchange(val exchange_id: String, val params_b64: String)

@Serializable
class PacketOutOnBandExchangeJoined(val creator: Boolean, val params_b64: String)

@Serializable
class PacketInSendMessage(val to_b64: String, val bin_b64: String, val confirm: String)

@Serializable
class MessageTextWithId(val id: String, val messageText: String, val respondingTo: String?)

const val SALT = "jl321jaoisokpfsaiudfzhiu1jk34eudz7aguiscihkaliudjhio21ljrhbfkasjdhsgakdjklasjd"

fun toSha256(s: String): String {
    val md = MessageDigest.getInstance("SHA-256")
    val digest = md.digest((s + SALT).encodeToByteArray())
    return digest.fold("") { acc, b ->
        acc + "%02x".format(b)
    }
}

package com.nuvio.tv.megaflix

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text

/** Opens a megaflix://youcine/<action> link (install, open, piloto) handled by YouCineLinkActivity. */
fun openYouCineAction(context: Context, action: String) {
    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse("megaflix://youcine/$action"))
                .setPackage(context.packageName)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}

/** Settings > Conta: the YouCine account, next to the Nuvio account. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun MegaFlixYouCineAccountCard() {
    val context = LocalContext.current
    var installed by remember { mutableStateOf(MegaFlixYouCine.isInstalled(context)) }
    var pilotOn by remember { mutableStateOf(PilotService.isEnabled(context)) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                installed = MegaFlixYouCine.isInstalled(context)
                pilotOn = PilotService.isEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF141414), RoundedCornerShape(8.dp))
            .padding(horizontal = 24.dp, vertical = 18.dp)
    ) {
        Text("Conta YouCine", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Text(
            text = when {
                !installed -> "O YouCine vem junto com o MegaFlix. Instale para usar como segunda opção de servidor."
                !pilotOn -> "Instalado. Entre na sua conta no YouCine e ligue o Piloto para abrir os títulos direto em tela cheia."
                else -> "Instalado e com o Piloto ligado. Para entrar ou trocar de conta, abra o YouCine."
            },
            color = Color(0xFFA0A0A0),
            fontSize = 15.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (!installed) {
                AccountButton("Instalar YouCine", primary = true) { openYouCineAction(context, "install") }
            } else {
                AccountButton("Entrar no YouCine", primary = true) { openYouCineAction(context, "open") }
                if (!pilotOn) AccountButton("Ligar o Piloto", primary = false) { openYouCineAction(context, "piloto") }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun AccountButton(text: String, primary: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = ButtonDefaults.shape(shape = RoundedCornerShape(4.dp)),
        colors = ButtonDefaults.colors(
            containerColor = if (primary) Color(0xFFFF8000) else Color(0xB36D6D6E),
            contentColor = if (primary) Color.Black else Color.White,
            focusedContainerColor = Color.White,
            focusedContentColor = Color.Black
        ),
        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 10.dp)
    ) {
        Text(text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

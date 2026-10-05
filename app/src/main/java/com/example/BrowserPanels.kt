package com.example

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

const val WEBRTC_BUILT_IN_LAB_HTML = """
<!DOCTYPE html>
<html lang="pt-BR">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>WebRTC & LabVM Diagnostics</title>
<style>
  body { background:#080E1E; color:#E2E8F0; font-family:system-ui,-apple-system,sans-serif; padding:16px; margin:0; }
  .card { background:#10192E; border:1px solid #1E293B; border-radius:12px; padding:14px; margin-bottom:14px; }
  h2 { color:#00E5FF; margin:0 0 8px 0; font-size:18px; }
  .badge { display:inline-block; padding:4px 8px; border-radius:6px; font-size:12px; font-weight:bold; background:#004B5E; color:#00E5FF; margin-right:6px; }
  button { background:#00E5FF; color:#002631; border:none; padding:10px 14px; border-radius:8px; font-weight:bold; margin:4px 4px 4px 0; cursor:pointer; }
  pre { background:#050914; padding:10px; border-radius:8px; overflow-x:auto; font-size:12px; color:#70F7D1; max-height:180px; }
  video { width:100%; max-height:200px; background:#000; border-radius:8px; margin-top:8px; }
</style>
</head>
<body>
  <div class="card">
    <h2>Diagnóstico WebRTC Nativo & ICE</h2>
    <p style="font-size:13px;color:#94A3B8;">Ambiente de verificação para WebRTC, STUN/ICE, DataChannel e Mídia em tempo real compatível com ARM LabVM.</p>
    <button onclick="runIceTest()">Testar STUN / ICE Loopback</button>
    <button onclick="startCamera()">Testar Câmera/Áudio</button>
    <button onclick="window.location.href='http://labvm.arm.com/lab'">Ir para ARM LabVM</button>
  </div>
  <div class="card">
    <h2>Status de APIs no WebView</h2>
    <div id="apiStatus">Verificando...</div>
  </div>
  <div class="card">
    <h2>Preview Local MediaStream</h2>
    <video id="localVideo" autoplay playsinline muted></video>
  </div>
  <div class="card">
    <h2>Logs de Negociação ICE / SDP</h2>
    <pre id="logBox">Pronto para iniciar diagnóstico...\n</pre>
  </div>
<script>
  const logBox = document.getElementById('logBox');
  function log(msg) {
    const t = new Date().toLocaleTimeString();
    logBox.textContent += '[' + t + '] ' + msg + '\n';
    console.log("[WebRTC-Lab] " + msg);
  }
  function checkApis() {
    const hasRTC = typeof RTCPeerConnection !== 'undefined';
    const hasMedia = !!(navigator.mediaDevices && navigator.mediaDevices.getUserMedia);
    const hasWS = typeof WebSocket !== 'undefined';
    document.getElementById('apiStatus').innerHTML =
      '<span class="badge">RTCPeerConnection: ' + (hasRTC ? 'ATIVO' : 'INATIVO') + '</span>' +
      '<span class="badge">getUserMedia: ' + (hasMedia ? 'ATIVO' : 'INATIVO') + '</span>' +
      '<span class="badge">WebSockets: ' + (hasWS ? 'ATIVO' : 'INATIVO') + '</span>';
    log('Verificação concluída: RTCPeerConnection=' + hasRTC + ', getUserMedia=' + hasMedia);
  }
  async function runIceTest() {
    log("Iniciando RTCPeerConnection com STUN Google...");
    try {
      const pc1 = new RTCPeerConnection({iceServers:[{urls:'stun:stun.l.google.com:19302'}]});
      const pc2 = new RTCPeerConnection();
      const dc = pc1.createDataChannel("labvm_channel");
      dc.onopen = () => {
        log("DataChannel ABERTO com sucesso! Enviando ping...");
        dc.send("Ping do LabVM Browser WebRTC!");
      };
      pc2.ondatachannel = (e) => {
        e.channel.onmessage = (m) => log("Recebido via P2P DataChannel: " + m.data);
      };
      pc1.onicecandidate = e => {
        if (e.candidate) {
          log("Candidato ICE coletado: " + e.candidate.candidate.split(' ')[4] + " (" + e.candidate.type + ")");
          pc2.addIceCandidate(e.candidate);
        }
      };
      pc2.onicecandidate = e => { if (e.candidate) pc1.addIceCandidate(e.candidate); };
      const offer = await pc1.createOffer();
      await pc1.setLocalDescription(offer);
      await pc2.setRemoteDescription(offer);
      const answer = await pc2.createAnswer();
      await pc2.setLocalDescription(answer);
      await pc1.setRemoteDescription(answer);
      log("SDP Offer/Answer negociados com sucesso!");
    } catch (err) {
      log("Erro no teste ICE: " + err.message);
    }
  }
  async function startCamera() {
    log("Solicitando câmera e microfone via getUserMedia...");
    try {
      const stream = await navigator.mediaDevices.getUserMedia({video:true, audio:true});
      document.getElementById('localVideo').srcObject = stream;
      log("Stream de vídeo/áudio capturado com sucesso (" + stream.getTracks().length + " tracks ativas).");
    } catch (err) {
      log("Aviso/Erro de mídia: " + err.message);
    }
  }
  checkApis();
</script>
</body>
</html>
"""

@Composable
fun ArmLabFeaturedCard(
    onOpenUrl: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("arm_labvm_featured_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Image(
                painter = painterResource(id = R.drawable.img_hero_lab_banner_1791239490220),
                contentDescription = "Banner ARM LabVM e WebRTC",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(148.dp)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(148.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.25f),
                                Color(0xFF080E1E).copy(alpha = 0.92f)
                            )
                        )
                    )
                    .padding(16.dp),
                contentAlignment = Alignment.BottomStart
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.primary)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "LINK DIRETO OFICIAL",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        Text(
                            text = "HTTP Cleartext & WebRTC Ativos",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF70F7D1)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "ARM LabVM Virtual Environment",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = ARM_LABVM_URL,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFFB8F3FF)
                    )
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Acesso imediato ao laboratório ARM com suporte a VM no navegador",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = { onOpenUrl(ARM_LABVM_URL) },
                modifier = Modifier.testTag("launch_arm_labvm_button")
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Abrir Lab")
            }
        }
    }
}

@Composable
fun BookmarksScreen(
    bookmarks: List<BookmarkEntity>,
    currentUrl: String,
    currentTitle: String,
    onOpenBookmark: (String, Boolean) -> Unit,
    onSaveBookmark: (Long, String, String, String, Boolean) -> Unit,
    onDeleteBookmark: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Todos") }
    var editingBookmark by remember { mutableStateOf<BookmarkEntity?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }

    val categories = remember(bookmarks) {
        listOf("Todos", "Fixados") + bookmarks.map { it.category }.distinct().sorted()
    }

    val filteredBookmarks = remember(bookmarks, searchQuery, selectedCategory) {
        bookmarks.filter { item ->
            val matchesQuery = searchQuery.isBlank() ||
                item.title.contains(searchQuery, ignoreCase = true) ||
                item.url.contains(searchQuery, ignoreCase = true) ||
                item.category.contains(searchQuery, ignoreCase = true)
            val matchesCategory = when (selectedCategory) {
                "Todos" -> true
                "Fixados" -> item.isPinned
                else -> item.category.equals(selectedCategory, ignoreCase = true)
            }
            matchesQuery && matchesCategory
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("bookmarks_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ArmLabFeaturedCard(onOpenUrl = { onOpenBookmark(it, false) })
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Gerenciador de Múltiplos Favoritos",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${bookmarks.size} favoritos salvos • Organize por categorias ou fixe no topo",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            editingBookmark = BookmarkEntity(
                                id = 0L,
                                title = currentTitle.ifBlank { "Página Atual" },
                                url = currentUrl.ifBlank { ARM_LABVM_URL },
                                category = "LabVM & Cloud",
                                isPinned = false
                            )
                            showEditDialog = true
                        },
                        modifier = Modifier.testTag("bookmark_current_page_btn")
                    ) {
                        Icon(Icons.Default.BookmarkAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Salvar Atual")
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_bookmarks_input"),
                    placeholder = { Text("Filtrar favoritos por nome, URL ou categoria...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Pesquisar") },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Limpar busca")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp)
                )
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = category },
                            label = { Text(category) },
                            leadingIcon = if (category == "Fixados") {
                                { Icon(Icons.Default.PushPin, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null
                        )
                    }
                }
            }

            if (filteredBookmarks.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.BookmarkBorder,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Nenhum favorito encontrado",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Adicione novos links usando o botão '+' ou salve o link http://labvm.arm.com/lab.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(filteredBookmarks, key = { it.id }) { bookmark ->
                    BookmarkItemCard(
                        bookmark = bookmark,
                        onOpen = { onOpenBookmark(bookmark.url, false) },
                        onOpenNewTab = { onOpenBookmark(bookmark.url, true) },
                        onTogglePin = {
                            onSaveBookmark(
                                bookmark.id,
                                bookmark.title,
                                bookmark.url,
                                bookmark.category,
                                !bookmark.isPinned
                            )
                        },
                        onEdit = {
                            editingBookmark = bookmark
                            showEditDialog = true
                        },
                        onDelete = { onDeleteBookmark(bookmark.id) }
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(72.dp)) }
        }

        FloatingActionButton(
            onClick = {
                editingBookmark = null
                showEditDialog = true
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("add_new_bookmark_fab"),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(Icons.Default.Add, contentDescription = "Adicionar Favorito")
        }
    }

    if (showEditDialog) {
        BookmarkEditorDialog(
            initial = editingBookmark,
            onDismiss = { showEditDialog = false },
            onConfirm = { id, title, url, category, isPinned ->
                onSaveBookmark(id, title, url, category, isPinned)
                showEditDialog = false
            }
        )
    }
}

@Composable
private fun BookmarkItemCard(
    bookmark: BookmarkEntity,
    onOpen: () -> Unit,
    onOpenNewTab: () -> Unit,
    onTogglePin: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen() }
            .testTag("bookmark_card_${bookmark.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (bookmark.isPinned) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (bookmark.url.contains("arm.com")) Icons.Default.Computer else Icons.Default.Language,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = bookmark.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = bookmark.category,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = bookmark.url,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onOpenNewTab) {
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Nova Aba")
                }
                IconButton(onClick = onTogglePin) {
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = "Fixar favorito",
                        tint = if (bookmark.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Editar favorito")
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Excluir favorito",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun BookmarkEditorDialog(
    initial: BookmarkEntity?,
    onDismiss: () -> Unit,
    onConfirm: (Long, String, String, String, Boolean) -> Unit
) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var url by remember { mutableStateOf(initial?.url ?: ARM_LABVM_URL) }
    var category by remember { mutableStateOf(initial?.category ?: "LabVM & Cloud") }
    var isPinned by remember { mutableStateOf(initial?.isPinned ?: true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (initial == null || initial.id == 0L) "Novo Favorito" else "Editar Favorito")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Título do Favorito") },
                    placeholder = { Text("Ex: ARM LabVM Oficial") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_bookmark_title_input")
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Endereço URL") },
                    placeholder = { Text(ARM_LABVM_URL) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_bookmark_url_input")
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Pasta / Categoria") },
                    placeholder = { Text("Ex: LabVM & Cloud, WebRTC, Dev") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_bookmark_category_input")
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Fixar no topo da barra rápida")
                    Switch(checked = isPinned, onCheckedChange = { isPinned = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (url.isNotBlank()) {
                        onConfirm(initial?.id ?: 0L, title, url, category, isPinned)
                    }
                },
                modifier = Modifier.testTag("dialog_bookmark_save_button")
            ) {
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun HistoryScreen(
    history: List<HistoryEntity>,
    onOpenUrl: (String, Boolean) -> Unit,
    onDeleteEntry: (Long) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()) }

    val filtered = remember(history, searchQuery) {
        if (searchQuery.isBlank()) history
        else history.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
                it.url.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("history_screen")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Histórico de Navegação",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${history.size} registros cronológicos armazenados no dispositivo",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (history.isNotEmpty()) {
                OutlinedButton(
                    onClick = onClearAll,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("clear_all_history_button")
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Limpar Tudo")
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_history_input"),
            placeholder = { Text("Pesquisar páginas visitadas no histórico...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Limpar")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (filtered.isEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Histórico vazio",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "As páginas visitadas no WebView aparecerão automaticamente aqui.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filtered, key = { it.id }) { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenUrl(item.url, false) }
                            .testTag("history_item_${item.id}"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = item.url,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = dateFormatter.format(Date(item.visitedAt)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { onOpenUrl(item.url, true) }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = "Abrir em nova aba"
                                )
                            }
                            IconButton(onClick = { onDeleteEntry(item.id) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Remover item do histórico",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WebRtcDiagnosticsScreen(
    hasCameraPermission: Boolean,
    hasAudioPermission: Boolean,
    webRtcAutoGrant: Boolean,
    webRtcEvents: List<String>,
    consoleLogs: List<ConsoleLogItem>,
    onRequestPermissions: () -> Unit,
    onLoadBuiltInWebRtcPage: () -> Unit,
    onOpenExternalUrl: (String) -> Unit,
    onClearConsole: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("webrtc_diagnostics_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "WebRTC & LabVM Studio",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Painel de controle para fluxos de mídia WebRTC (Câmera, Microfone, STUN/TURN ICE) e console JavaScript em tempo real.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Status de Permissões de Hardware (WebRTC)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                PermissionRowItem(
                    icon = Icons.Default.Videocam,
                    label = "Câmera de Vídeo (PermissionRequest.RESOURCE_VIDEO_CAPTURE)",
                    granted = hasCameraPermission
                )
                PermissionRowItem(
                    icon = Icons.Default.Mic,
                    label = "Microfone de Áudio (PermissionRequest.RESOURCE_AUDIO_CAPTURE)",
                    granted = hasAudioPermission
                )
                PermissionRowItem(
                    icon = Icons.Default.GraphicEq,
                    label = "Auto-Grant WebChromeClient WebRTC",
                    granted = webRtcAutoGrant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onRequestPermissions,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("request_webrtc_permissions_btn")
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Solicitar Câmera & Áudio")
                    }
                    OutlinedButton(
                        onClick = onLoadBuiltInWebRtcPage,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("run_builtin_webrtc_test_btn")
                    ) {
                        Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Abrir Teste ICE Local")
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Atalhos de Teste Rápido (LabVM & WebRTC)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AssistChip(
                        onClick = { onOpenExternalUrl(ARM_LABVM_URL) },
                        label = { Text("ARM LabVM (http://labvm.arm.com/lab)") },
                        leadingIcon = { Icon(Icons.Default.Computer, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                    AssistChip(
                        onClick = { onOpenExternalUrl("https://webrtc.github.io/samples/src/content/peerconnection/trickle-ice/") },
                        label = { Text("Trickle ICE") },
                        leadingIcon = { Icon(Icons.Default.GraphicEq, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                    AssistChip(
                        onClick = { onOpenExternalUrl("https://webrtc.github.io/samples/") },
                        label = { Text("WebRTC Samples") },
                        leadingIcon = { Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Eventos Recentes do Motor WebRTC",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                webRtcEvents.take(8).forEach { event ->
                    Text(
                        text = "• $event",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Terminal, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Console JavaScript do WebView (${consoleLogs.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    TextButton(onClick = onClearConsole) {
                        Text("Limpar")
                    }
                }
                if (consoleLogs.isEmpty()) {
                    Text(
                        text = "Nenhuma mensagem de console capturada ainda.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    consoleLogs.take(15).forEach { log ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black.copy(alpha = 0.25f))
                                .padding(8.dp)
                        ) {
                            Text(
                                text = "[${log.level}] ${log.message}",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = if (log.level == "ERROR") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                            )
                            if (log.sourceId.isNotBlank()) {
                                Text(
                                    text = log.sourceId,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionRowItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    granted: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(10.dp))
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (granted) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = if (granted) Color(0xFF1DE9B6) else Color(0xFFFFB300),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (granted) "Pronto" else "Pendente",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun BrowserSettingsScreen(
    settings: BrowserSettings,
    onUpdateSettings: ((BrowserSettings) -> BrowserSettings) -> Unit,
    onResetSettings: () -> Unit,
    onClearWebViewData: () -> Unit,
    modifier: Modifier = Modifier
) {
    var homeUrlText by remember(settings.homeUrl) { mutableStateOf(settings.homeUrl) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("settings_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Configurações do Navegador",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Personalize WebView, WebRTC, User-Agent, Privacidade e Página Inicial",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            OutlinedButton(
                onClick = onResetSettings,
                modifier = Modifier.testTag("reset_settings_button")
            ) {
                Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Padrão")
            }
        }

        // Home Page & Search Engine
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Página Inicial & Mecanismo de Busca", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = homeUrlText,
                    onValueChange = {
                        homeUrlText = it
                        onUpdateSettings { s -> s.copy(homeUrl = it) }
                    },
                    label = { Text("URL da Página Inicial (Home)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_home_url_input")
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(
                        onClick = {
                            homeUrlText = ARM_LABVM_URL
                            onUpdateSettings { it.copy(homeUrl = ARM_LABVM_URL) }
                        },
                        label = { Text("Usar ARM LabVM Oficial") }
                    )
                    AssistChip(
                        onClick = {
                            homeUrlText = "https://www.google.com"
                            onUpdateSettings { it.copy(homeUrl = "https://www.google.com") }
                        },
                        label = { Text("Usar Google") }
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Text("Mecanismo de Pesquisa na Barra de Endereços:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SearchEngine.entries.forEach { engine ->
                        FilterChip(
                            selected = settings.searchEngine == engine,
                            onClick = { onUpdateSettings { it.copy(searchEngine = engine) } },
                            label = { Text(engine.label) }
                        )
                    }
                }
            }
        }

        // WebView & WebRTC Engine Settings
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Motor WebView, WebRTC & Renderização", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                SettingToggleRow(
                    title = "JavaScript Habilitado",
                    subtitle = "Essencial para ARM LabVM, painéis VNC/Spice e WebRTC",
                    checked = settings.javaScriptEnabled,
                    onCheckedChange = { onUpdateSettings { s -> s.copy(javaScriptEnabled = it) } }
                )
                SettingToggleRow(
                    title = "DOM Storage & LocalStorage / IndexedDB",
                    subtitle = "Armazenamento de sessão para máquinas virtuais e web apps",
                    checked = settings.domStorageEnabled,
                    onCheckedChange = { onUpdateSettings { s -> s.copy(domStorageEnabled = it) } }
                )
                SettingToggleRow(
                    title = "Auto-Conceder Mídia WebRTC (Câmera/Áudio)",
                    subtitle = "Permite que sessões WebRTC acessem áudio/vídeo automaticamente",
                    checked = settings.webRtcAutoGrantMedia,
                    onCheckedChange = { onUpdateSettings { s -> s.copy(webRtcAutoGrantMedia = it) } }
                )
                SettingToggleRow(
                    title = "Modo Para Computador (Desktop Viewport)",
                    subtitle = "Ajusta largura da página para telas grandes (recomendado para LabVM)",
                    checked = settings.desktopMode,
                    onCheckedChange = { onUpdateSettings { s -> s.copy(desktopMode = it) } }
                )
                SettingToggleRow(
                    title = "Permitir Conteúdo Misto HTTP/HTTPS",
                    subtitle = "Necessário para carregar recursos de http://labvm.arm.com/lab sem bloqueio",
                    checked = settings.allowMixedContent,
                    onCheckedChange = { onUpdateSettings { s -> s.copy(allowMixedContent = it) } }
                )
                SettingToggleRow(
                    title = "Suporte a Gestos de Pinça / Zoom",
                    subtitle = "Permite ampliar terminais e áreas de trabalho remotas",
                    checked = settings.supportZoom,
                    onCheckedChange = { onUpdateSettings { s -> s.copy(supportZoom = it) } }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Text(
                    text = "Escala de Texto do WebView: ${settings.textZoomPercent}%",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Slider(
                    value = settings.textZoomPercent.toFloat(),
                    onValueChange = { onUpdateSettings { s -> s.copy(textZoomPercent = it.toInt()) } },
                    valueRange = 75f..150f,
                    steps = 4
                )

                Text(
                    text = "Identificação User-Agent:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    UserAgentPreset.entries.forEach { preset ->
                        FilterChip(
                            selected = settings.userAgentPreset == preset,
                            onClick = { onUpdateSettings { s -> s.copy(userAgentPreset = preset) } },
                            label = { Text(preset.label) }
                        )
                    }
                }
            }
        }

        // Privacy & Storage
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Privacidade, Cookies & Aparência", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                SettingToggleRow(
                    title = "Salvar Histórico de Navegação",
                    subtitle = "Registrar automaticamente URLs visitadas no banco local",
                    checked = settings.saveHistory,
                    onCheckedChange = { onUpdateSettings { s -> s.copy(saveHistory = it) } }
                )
                SettingToggleRow(
                    title = "Aceitar Cookies (1ª e 3ª Parte)",
                    subtitle = "Necessário para manter login ativo em sessões ARM LabVM",
                    checked = settings.acceptCookies,
                    onCheckedChange = { onUpdateSettings { s -> s.copy(acceptCookies = it) } }
                )
                SettingToggleRow(
                    title = "Tema Escuro Cyber-Lab",
                    subtitle = "Interface de alto contraste otimizada para laboratórios",
                    checked = settings.darkModeOverride,
                    onCheckedChange = { onUpdateSettings { s -> s.copy(darkModeOverride = it) } }
                )

                Spacer(modifier = Modifier.height(6.dp))
                Button(
                    onClick = onClearWebViewData,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("clear_webview_cache_button")
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Limpar Cache, Cookies e WebStorage")
                }
            }
        }
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

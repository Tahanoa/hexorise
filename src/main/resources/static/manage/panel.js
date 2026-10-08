'use strict';
const $ = id => document.getElementById(id);
let csrf, loadedRoom = '', refreshRunning = false, noticeTimer;
function notice(message, error = false) {
  $('notice').textContent = message; $('notice').classList.toggle('error', error); $('notice').hidden = false;
  clearTimeout(noticeTimer); noticeTimer = setTimeout(() => { $('notice').hidden = true; }, error ? 14000 : 7000);
}
async function api(path, method = 'GET', body) {
  const headers = { Accept: 'application/json' };
  if (method !== 'GET') {
    if (!csrf) csrf = await api('/csrf');
    headers[csrf.headerName] = csrf.token;
    if (body !== undefined) headers['Content-Type'] = 'application/json';
  }
  const response = await fetch('/api/v1' + path, { method, headers, credentials: 'same-origin', body: body === undefined ? undefined : JSON.stringify(body) });
  const data = await response.json().catch(() => ({}));
  if (!response.ok) {
    if (response.status === 403) csrf = null;
    throw new Error(response.status === 401 ? 'Sign in with your configured admin username and password.' : data.detail || data.message || data.error || `Request failed (${response.status}).`);
  }
  return data;
}
function validRoom() {
  const id = $('room-id').value.trim();
  if (!/^[A-Za-z0-9_-]{1,128}$/.test(id)) throw new Error('Enter a valid room ID.');
  return id;
}
function roomPath() { if (!loadedRoom) throw new Error('Load a room first.'); return '/rooms/' + encodeURIComponent(loadedRoom); }
function row(title, detail, action) {
  const li = document.createElement('li'), text = document.createElement('span'); text.textContent = title;
  if (detail) { const small = document.createElement('small'); small.textContent = detail; text.append(small); }
  li.append(text); if (action) li.append(action); return li;
}
function actionButton(label, callback, danger = false) {
  const button = document.createElement('button'); button.type = 'button'; button.textContent = label;
  button.className = 'secondary small' + (danger ? ' danger' : '');
  button.addEventListener('click', () => run(button, callback)); return button;
}
function fillList(id, items, emptyText) {
  const list = $(id); list.replaceChildren(...items);
  if (!items.length) { const li = row(emptyText); li.className = 'empty'; list.append(li); }
}
async function run(button, task) {
  if (button.disabled) return;
  button.disabled = true;
  try { await task(); } catch (error) { notice(error.message, true); }
  finally { button.disabled = false; }
}
function form(id, task) {
  $(id).addEventListener('submit', event => { event.preventDefault(); run($(id).querySelector('button[type=submit]'), task); });
}
async function refresh() {
  if (refreshRunning) return; refreshRunning = true;
  try {
    const [status, users, loops] = await Promise.all([api('/bot/status'), api('/bot/users'), api('/bot/loops')]);
    $('state').textContent = status.state; $('state').classList.toggle('online', status.state === 'READY');
    $('connection').textContent = status.state === 'READY' ? 'Connected to Highrise' : ['REJECTED', 'CONFIGURATION_ERROR'].includes(status.state) ? 'Check room access and API token in Connection' : 'Live actions need a ready bot';
    $('active-room').textContent = status.roomId || 'no configured room';
    $('queue').textContent = status.queuedMessages; $('members-count').textContent = users.length; $('loops-count').textContent = loops.length;
    $('members-label').textContent = users.length + ' synchronized';
    if (!$('room-id').value && status.roomId) $('room-id').value = status.roomId;
    $('room-users').replaceChildren(...users.map(user => { const option = document.createElement('option'); option.value = user.id; option.label = user.username; return option; }));
    fillList('member-list', users.map(user => row(user.username || user.id, user.id, actionButton('Select', () => { $('moderation-target').value = user.id; $('emote-target').value = user.id; }))), 'No synchronized members.');
    fillList('loop-list', loops.map(loop => row(loop.emoteName, `${loop.targetUserId || 'Bot'} · every ${loop.intervalSeconds}s`, actionButton('Stop', async () => { await api('/bot/emotes/stop', 'POST', { targetUserId: loop.targetUserId }); await refresh(); notice('Loop stopped.'); }))), 'No active loops.');
  } catch (error) { $('state').textContent = 'UNAVAILABLE'; $('state').classList.remove('online'); notice(error.message, true); }
  finally { refreshRunning = false; }
}
async function loadAdmins() {
  const path = roomPath(), room = loadedRoom, data = await api(path + '/admins');
  if (room !== loadedRoom) return;
  fillList('admin-list', data.map(admin => row(admin.userId, admin.role, actionButton('Remove', async () => {
    if (!confirm(`Remove ${admin.userId} from administrators of ${room}?`)) return;
    await api(path + '/admins/' + encodeURIComponent(admin.userId), 'DELETE'); await loadAdmins(); notice('Administrator removed.');
  }, true))), 'No administrators saved. Add the first OWNER here.');
}
async function loadRoom() {
  const room = validRoom(), settings = await api('/rooms/' + encodeURIComponent(room) + '/settings');
  loadedRoom = room; $('loaded-room').textContent = room;
  for (const [name, value] of Object.entries(settings)) {
    const input = $('settings-form').elements.namedItem(name); if (!input) continue;
    if (input.type === 'checkbox') input.checked = value; else input.value = value;
  }
  $('settings-fields').disabled = false; $('interval').value = settings.emoteLoopIntervalSeconds;
  await loadAdmins(); notice('Room loaded.');
}
$('load-room').addEventListener('click', () => run($('load-room'), loadRoom));
$('refresh').addEventListener('click', () => run($('refresh'), refresh));
form('settings-form', async () => {
  const body = {}, path = roomPath();
  for (const input of $('settings-form').querySelectorAll('[name]')) body[input.name] = input.type === 'checkbox' ? input.checked : input.type === 'number' ? Number(input.value) : input.value;
  await api(path + '/settings', 'PUT', body); await refresh(); notice('Room settings saved.');
});
form('emote-form', async () => {
  await api('/bot/emotes', 'POST', { selector: $('emote-id').value.trim() || $('emote-selector').value, targetUserId: $('emote-target').value.trim() || null, repeat: $('repeat').checked, intervalSeconds: Number($('interval').value) });
  await refresh(); notice('Emote accepted by Highrise.');
});
$('stop-all').addEventListener('click', () => run($('stop-all'), async () => { await api('/bot/emotes/stop-all', 'POST'); await refresh(); notice('All loops stopped.'); }));
$('moderation-action').addEventListener('change', () => {
  const action = $('moderation-action').value; $('duration').disabled = ['kick', 'unban'].includes(action);
  $('duration').value = action === 'mute' ? '60' : '';
});
form('moderation-form', async () => {
  const action = $('moderation-action').value, userId = $('moderation-target').value.trim();
  if (!confirm(`Apply ${action} to ${userId} in the configured bot room?`)) return;
  await api('/bot/moderation', 'POST', { userId, action, durationSeconds: $('duration').disabled || !$('duration').value ? null : Number($('duration').value) });
  await refresh(); notice('Moderation accepted by Highrise.');
});
form('admin-form', async () => {
  await api(roomPath() + '/admins/' + encodeURIComponent($('admin-user').value.trim()), 'PUT', { role: $('admin-role').value });
  await loadAdmins(); $('admin-user').value = ''; notice('Administrator saved.');
});
let emoteCatalog = [];
function renderCatalog() {
  const query = $('emote-search').value.trim().toLowerCase(), previous = $('emote-selector').value;
  const matches = emoteCatalog.filter(emote => `${emote.number} ${emote.name} ${emote.id}`.toLowerCase().includes(query));
  $('emote-selector').replaceChildren(...matches.map(emote => {
    const option = document.createElement('option'); option.value = String(emote.number);
    option.textContent = `${emote.number} · ${emote.name} · ${emote.id}`; return option;
  }));
  if (matches.some(emote => String(emote.number) === previous)) $('emote-selector').value = previous;
  $('catalog-count').textContent = `${matches.length} shown / ${emoteCatalog.length} catalog entries. Direct IDs support additional emotes.`;
}
$('emote-search').addEventListener('input', renderCatalog);
function connectionBody() {
  if (!$('connection-form').reportValidity()) throw new Error('Enter a valid room ID.');
  return { roomId: $('connect-room').value.trim(), apiToken: $('connect-token').value || null, autoConnect: $('auto-connect').checked };
}
async function loadConnection() {
  const settings = await api('/bot/connection');
  $('connect-room').value = settings.roomId || ''; $('auto-connect').checked = settings.autoConnect;
  $('connect-token').value = '';
  $('token-state').textContent = settings.tokenConfigured ? 'Token saved' : 'No token configured';
}
form('connection-form', async () => {
  await api('/bot/connect', 'POST', connectionBody()); await loadConnection(); await refresh();
  $('room-id').value = $('connect-room').value; await loadRoom();
  notice('Connection requested. Wait for READY before sending live commands.');
});
$('save-connection').addEventListener('click', () => run($('save-connection'), async () => {
  await api('/bot/connection', 'PUT', connectionBody()); await loadConnection(); notice('Connection settings saved.');
}));
$('disconnect').addEventListener('click', () => run($('disconnect'), async () => {
  await api('/bot/disconnect', 'POST'); await loadConnection(); await refresh(); notice('Disconnected. Automatic connection disabled.');
}));
$('forget-token').addEventListener('click', () => run($('forget-token'), async () => {
  await api('/bot/connection', 'DELETE'); await loadConnection(); await refresh(); notice('Token removed. Bot disconnected.');
}));
(async () => {
  try {
    emoteCatalog = await api('/emotes'); renderCatalog(); await loadConnection();
    await refresh(); if ($('room-id').value) await loadRoom();
  } catch (error) { notice(error.message, true); }
})();
setInterval(refresh, 5000);

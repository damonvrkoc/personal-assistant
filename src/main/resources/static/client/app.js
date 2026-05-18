const API_URL = '/api/status';
const AUTO_REFRESH_MS = 10_000;

let refreshTimer = null;

function badgeClass(value) {
  if (!value) return 'bad';
  const v = String(value).toUpperCase();
  if (v === 'UP' || v === 'CONNECTED' || v === 'TRUE') return 'ok';
  if (v === 'DOWN' || v === 'FAILED') return 'bad';
  if (v === 'NOT_CONFIGURED' || v === 'DISABLED' || v === 'N/A') return 'warn';
  return 'warn';
}

function row(label, value, asBadge) {
  const val = value == null ? '—' : String(value);
  const valueHtml = asBadge
    ? `<span class="badge ${badgeClass(val)}">${escapeHtml(val)}</span>`
    : `<span>${escapeHtml(val)}</span>`;
  return `<div class="row"><span>${escapeHtml(label)}</span>${valueHtml}</div>`;
}

function escapeHtml(text) {
  const div = document.createElement('div');
  div.textContent = text;
  return div.innerHTML;
}

function renderCard(title, rowsHtml) {
  return `<section class="card"><h2>${escapeHtml(title)}</h2>${rowsHtml}</section>`;
}

function renderStatus(data) {
  const app = data.application || {};
  const slack = (data.channels && data.channels.slack) || {};
  const whatsapp = (data.channels && data.channels.whatsapp) || {};
  const llm = data.llm || {};
  const neo4j = data.neo4j || {};
  const memory = data.memory || {};

  return [
    renderCard(
      'Application',
      row('Name', app.name) +
        row('Version', app.version) +
        row('Uptime', app.uptime)
    ),
    renderCard(
      'Slack',
      row('Enabled', slack.enabled) +
        row('Configured', slack.configured) +
        row('Socket mode', slack.socketMode, true)
    ),
    renderCard(
      'WhatsApp',
      row('Enabled', whatsapp.enabled) +
        row('Configured', whatsapp.configured)
    ),
    renderCard(
      'LLM',
      row('Provider', llm.provider) +
        row('Model', llm.model) +
        row('Base URL', llm.baseUrl) +
        row('API key set', llm.apiKeyConfigured)
    ),
    renderCard(
      'Neo4j',
      row('Configured', neo4j.configured) +
        row('Status', neo4j.status, true) +
        row('URI', neo4j.uri) +
        row('Last persistence', neo4j.lastPersistence, true) +
        (neo4j.detail ? row('Detail', neo4j.detail) : '')
    ),
    renderCard('Memory', row('Active conversations', memory.activeConversations)),
  ].join('');
}

function showError(message) {
  const el = document.getElementById('error');
  el.textContent = message;
  el.classList.remove('hidden');
}

function hideError() {
  document.getElementById('error').classList.add('hidden');
}

async function loadStatus() {
  const root = document.getElementById('status-root');
  try {
    const res = await fetch(API_URL);
    if (!res.ok) {
      throw new Error(`HTTP ${res.status}`);
    }
    const data = await res.json();
    hideError();
    root.innerHTML = renderStatus(data);
    document.getElementById('last-updated').textContent =
      'Updated ' + new Date().toLocaleTimeString();
  } catch (err) {
    showError('Failed to load status: ' + err.message);
    root.innerHTML = '<p class="muted">No data</p>';
  }
}

function setupAutoRefresh() {
  if (refreshTimer) {
    clearInterval(refreshTimer);
    refreshTimer = null;
  }
  if (document.getElementById('auto-refresh').checked) {
    refreshTimer = setInterval(loadStatus, AUTO_REFRESH_MS);
  }
}

document.getElementById('refresh-btn').addEventListener('click', loadStatus);
document.getElementById('auto-refresh').addEventListener('change', setupAutoRefresh);

loadStatus();

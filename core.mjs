export function safeFileName(value, fallback = "poe-chat") {
  const cleaned = String(value || "")
    .normalize("NFKD")
    .replace(/[<>:"/\\|?*\u0000-\u001f]/g, " ")
    .replace(/\s+/g, " ")
    .trim()
    .slice(0, 90)
    .replace(/[. ]+$/g, "");
  return cleaned || fallback;
}

export function normalizeTimestamp(value) {
  const number = Number(value);
  if (!Number.isFinite(number) || number <= 0) return null;
  if (number > 1e14) return Math.round(number / 1000);
  if (number > 1e11) return Math.round(number);
  return Math.round(number * 1000);
}

export function messageAuthor(message) {
  if (message?.isChatAnnouncement) return "Poe";
  return message?.authorUser?.fullName
    || message?.bot?.displayName
    || message?.authorNickname
    || "Unknown";
}

export function collectChatMessages(edges, chatCode, seen = new Map()) {
  let foundCreation = false;
  for (const edge of edges || []) {
    const message = edge?.node;
    if (!message || message?.chat?.chatCode !== chatCode) continue;
    const id = String(message.messageId ?? message.id ?? `${message.creationTime}:${message.text}`);
    if (!seen.has(id)) seen.set(id, message);
    if (message.isChatAnnouncement) foundCreation = true;
  }
  return { seen, foundCreation };
}

export function formatTranscript(messages, options = {}) {
  const sorted = [...messages].sort((a, b) => {
    const time = (normalizeTimestamp(a.creationTime) || 0) - (normalizeTimestamp(b.creationTime) || 0);
    if (time) return time;
    return Number(a.messageId || 0) - Number(b.messageId || 0);
  });
  const title = options.title || sorted.find(message => message?.chat?.title)?.chat?.title || "Poe chat";
  const lines = [
    title,
    "=".repeat(Math.min(72, Math.max(12, title.length))),
    options.sourceUrl ? `Source: ${options.sourceUrl}` : null,
    `Exported: ${(options.exportedAt || new Date()).toISOString()}`,
    `Messages: ${sorted.length}`,
    ""
  ].filter(line => line !== null);

  for (const message of sorted) {
    const timestamp = normalizeTimestamp(message.creationTime);
    const when = timestamp ? new Date(timestamp).toLocaleString(options.locale || undefined) : "Unknown time";
    lines.push(`[${when}] ${messageAuthor(message)}`, "", String(message.text || "").trim(), "", "---", "");
  }
  return { title, text: lines.join("\n").trimEnd() + "\n", count: sorted.length };
}

export function wordCount(text) {
  const matches = String(text || "").trim().match(/\S+/g);
  return matches ? matches.length : 0;
}

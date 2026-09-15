(async () => {
  "use strict";
  if (window.__downloadrRunning) {
    DownloadrNative.fail("An export is already running.");
    return;
  }
  window.__downloadrRunning = true;

  const FALLBACK = {
    formkey: "ereNdsRqhp2Rd3LEW",
    signature: "bR8qjbJA8tZobI7Iu",
    queryId: "a8790737547cc2763301232480b0d5cd1b6b5beb1a1ec6ae6d025b44fff90984",
    revision: "e4eb6fef396a4557b00ee0836afb5d87cd24ffae"
  };
  const code = location.pathname.split("/").filter(Boolean).pop();
  const timestamp = value => {
    value = Number(value);
    if (!Number.isFinite(value) || value <= 0) return null;
    if (value > 1e14) return Math.round(value / 1e3);
    if (value > 1e11) return Math.round(value);
    return Math.round(value * 1e3);
  };
  const author = message => message.isChatAnnouncement
    ? "Poe"
    : message.authorUser?.fullName || message.bot?.displayName || message.authorNickname || "Unknown";
  const report = (message, progress) => DownloadrNative.status(message, Math.round(progress));

  try {
    if (location.protocol !== "https:" || !/(^|\.)poe\.com$/i.test(location.hostname) || !/^\/chat\/[^/]+/.test(location.pathname)) {
      throw new Error("Open the Poe conversation you want to save first.");
    }

    const config = { ...FALLBACK };
    const discovered = new Set();
    report("Checking Poe's connection", 4);
    const scripts = [...document.scripts].map(script => script.src).filter(src => /poecdn\.net\/.+\.js/.test(src));
    for (const src of scripts) {
      try {
        const source = await fetch(src).then(response => response.ok ? response.text() : "");
        if (!source) continue;
        const query = source.match(/params:\{id:"([a-f0-9]{40,})",metadata:\{\},name:"useMessageCacheQuery"/);
        const revision = source.match(/"poe-revision":"([a-f0-9]{20,})"/);
        const formkey = source.match(/let\s+\w+="([A-Za-z0-9]{12,})",\w+=null,setClientFormkey/);
        const signature = source.match(/getAPISig=.*?window\.([A-Za-z0-9]{12,})\(/);
        if (query) { config.queryId = query[1]; discovered.add("query"); }
        if (revision) { config.revision = revision[1]; discovered.add("revision"); }
        if (formkey) { config.formkey = formkey[1]; discovered.add("formkey"); }
        if (signature) { config.signature = signature[1]; discovered.add("signature"); }
        if (discovered.size === 4) break;
      } catch (_) { }
    }

    const formkeyFunction = window[config.formkey];
    const signatureFunction = window[config.signature];
    if (typeof formkeyFunction !== "function" || typeof signatureFunction !== "function") {
      throw new Error("Poe changed its sign-in helper. Download the newest Downloadr build and try again.");
    }

    const formkey = formkeyFunction().slice(0, 32);
    const settings = await fetch("/api/settings", { credentials: "include" }).then(response => {
      if (!response.ok) throw new Error("Poe sign-in was not available. Refresh Poe and try again.");
      return response.json();
    });
    const channel = settings?.tchannelData?.channel;
    if (!channel) throw new Error("Poe did not provide an authenticated session. Sign in or refresh Poe and try again.");

    const seen = new Map();
    let cursor = null;
    let page = 0;
    let title = document.title.replace(/\s+-\s+Poe$/i, "") || "Poe chat";
    do {
      page += 1;
      report(`${seen.size.toLocaleString()} messages found`, Math.min(92, 8 + page * 2));
      const body = JSON.stringify({
        queryName: "useMessageCacheQuery",
        variables: { limit: 100, after: cursor, since: "0" },
        extensions: { hash: config.queryId }
      });
      const nonce = crypto.randomUUID?.() || `${Date.now()}${Math.random()}`;
      const response = await fetch("/api/gql_POST", {
        method: "POST",
        credentials: "include",
        headers: {
          "Content-Type": "application/json",
          "poegraphql": "0",
          "poe-queryname": "useMessageCacheQuery",
          "poe-formkey": formkey,
          "poe-tchannel": channel,
          "poe-tag-id": signatureFunction(nonce, formkey, body),
          "poe-revision": config.revision
        },
        body
      });
      const json = await response.json();
      if (!response.ok || json.errors?.length) throw new Error(json.errors?.[0]?.message || `Poe returned ${response.status}`);
      const connection = json?.data?.recentMessages;
      if (!connection?.edges) throw new Error("Poe's message format changed. Download the newest Downloadr build and try again.");

      for (const edge of connection.edges) {
        const message = edge?.node;
        if (message?.chat?.chatCode !== code) continue;
        const id = String(message.messageId ?? message.id ?? `${message.creationTime}:${message.text}`);
        seen.set(id, message);
        title = message.chat?.title || title;
      }
      cursor = connection.pageInfo?.endCursor || null;
      if (!connection.pageInfo?.hasNextPage || !cursor) break;
      if (page >= 500) throw new Error("Stopped after 50,000 account messages.");
      await new Promise(resolve => setTimeout(resolve, 30));
    } while (true);

    if (!seen.size) throw new Error("No messages were found for this chat.");
    const messages = [...seen.values()].sort((a, b) =>
      (timestamp(a.creationTime) || 0) - (timestamp(b.creationTime) || 0)
      || Number(a.messageId || 0) - Number(b.messageId || 0));
    const lines = [
      title,
      "=".repeat(Math.min(72, Math.max(12, title.length))),
      `Source: ${location.href}`,
      `Exported: ${new Date().toISOString()}`,
      `Messages: ${messages.length}`,
      ""
    ];
    for (const message of messages) {
      const time = timestamp(message.creationTime);
      lines.push(`[${time ? new Date(time).toLocaleString() : "Unknown time"}] ${author(message)}`, "", String(message.text || "").trim(), "", "---", "");
    }
    const transcript = lines.join("\n").trimEnd() + "\n";
    const bytes = new TextEncoder().encode(transcript);
    const exportId = `${Date.now()}-${Math.random()}`;
    const chunkSize = 24 * 1024;
    DownloadrNative.begin(exportId, bytes.length);
    for (let offset = 0; offset < bytes.length; offset += chunkSize) {
      const slice = bytes.subarray(offset, Math.min(offset + chunkSize, bytes.length));
      DownloadrNative.chunk(exportId, btoa(String.fromCharCode(...slice)));
      report("Sending to Android", 94 + 5 * Math.min(1, (offset + slice.length) / bytes.length));
    }
    DownloadrNative.finish(exportId, title, messages.length);
  } catch (error) {
    DownloadrNative.fail(error?.message || "Downloadr could not read this conversation.");
  } finally {
    window.__downloadrRunning = false;
  }
})();

import { safeFileName, wordCount } from "./core.mjs";

const helperButton = document.querySelector("#copyHelperButton");
const installButton = document.querySelector("#installButton");
const fileInput = document.querySelector("#fileInput");
const chatText = document.querySelector("#chatText");
const textStats = document.querySelector("#textStats");
const copyTextButton = document.querySelector("#copyTextButton");
const downloadTextButton = document.querySelector("#downloadTextButton");
const clearButton = document.querySelector("#clearButton");
const toast = document.querySelector("#toast");
let installPrompt = null;
let helperSource = "";
let currentName = "poe-chat";

function showToast(message) {
  toast.textContent = message;
  toast.classList.add("visible");
  window.clearTimeout(showToast.timer);
  showToast.timer = window.setTimeout(() => toast.classList.remove("visible"), 2400);
}

function updateStats() {
  textStats.textContent = `${wordCount(chatText.value).toLocaleString()} words · ${chatText.value.length.toLocaleString()} characters`;
}

async function copyText(value) {
  await navigator.clipboard.writeText(value);
}

function downloadText(value, name) {
  const blob = new Blob([value], { type: "text/plain;charset=utf-8" });
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = `${safeFileName(name)}.txt`;
  document.body.append(link);
  link.click();
  link.remove();
  window.setTimeout(() => URL.revokeObjectURL(url), 1000);
}

fetch("poe-helper.js")
  .then(response => {
    if (!response.ok) throw new Error("Helper unavailable");
    return response.text();
  })
  .then(source => { helperSource = `javascript:${source.trim()}`; })
  .catch(() => { helperButton.disabled = true; helperButton.textContent = "Helper unavailable"; });

helperButton.addEventListener("click", async () => {
  if (!helperSource) return showToast("Still preparing the button — try again in a moment.");
  try {
    await copyText(helperSource);
    showToast("Button code copied. Now edit a Chrome bookmark.");
    helperButton.textContent = "Copied ✓";
    window.setTimeout(() => { helperButton.innerHTML = "Copy button code <span>→</span>"; }, 2200);
  } catch {
    showToast("Chrome blocked clipboard access. Press and hold, then try again.");
  }
});

fileInput.addEventListener("change", async () => {
  const file = fileInput.files?.[0];
  if (!file) return;
  chatText.value = await file.text();
  currentName = file.name.replace(/\.(txt|md)$/i, "");
  updateStats();
  showToast("Text file opened.");
});

chatText.addEventListener("input", updateStats);
copyTextButton.addEventListener("click", async () => {
  if (!chatText.value) return showToast("There is no text to copy yet.");
  try { await copyText(chatText.value); showToast("Text copied."); }
  catch { showToast("That text is too large for this clipboard. Use Download .txt."); }
});
downloadTextButton.addEventListener("click", () => {
  if (!chatText.value) return showToast("There is no text to download yet.");
  downloadText(chatText.value, currentName);
});
clearButton.addEventListener("click", () => { chatText.value = ""; currentName = "poe-chat"; updateStats(); chatText.focus(); });

window.addEventListener("beforeinstallprompt", event => {
  event.preventDefault();
  installPrompt = event;
  installButton.hidden = false;
});
installButton.addEventListener("click", async () => {
  if (!installPrompt) return;
  await installPrompt.prompt();
  installPrompt = null;
  installButton.hidden = true;
});
window.addEventListener("appinstalled", () => showToast("Downloadr installed."));

if ("serviceWorker" in navigator) window.addEventListener("load", () => navigator.serviceWorker.register("sw.js"));
updateStats();

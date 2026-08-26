import test from "node:test";
import assert from "node:assert/strict";
import { collectChatMessages, formatTranscript, normalizeTimestamp, safeFileName, wordCount } from "../core.mjs";

test("collectChatMessages filters one chat and deduplicates pages", () => {
  const seen = new Map();
  const edge = message => ({ node: message });
  collectChatMessages([edge({ messageId: 2, text: "two", chat: { chatCode: "wanted" } }), edge({ messageId: 9, chat: { chatCode: "other" } })], "wanted", seen);
  const result = collectChatMessages([edge({ messageId: 2, text: "two", chat: { chatCode: "wanted" } }), edge({ messageId: 1, text: "one", isChatAnnouncement: true, chat: { chatCode: "wanted" } })], "wanted", seen);
  assert.equal(result.seen.size, 2);
  assert.equal(result.foundCreation, true);
});

test("formatTranscript sorts and labels messages", () => {
  const { text, count, title } = formatTranscript([
    { messageId: 2, creationTime: 2, text: "Answer", bot: { displayName: "Assistant" }, chat: { title: "Test chat" } },
    { messageId: 1, creationTime: 1, text: "Question", authorUser: { fullName: "Ben" }, chat: { title: "Test chat" } }
  ], { sourceUrl: "https://poe.com/chat/example", exportedAt: new Date("2026-08-26T12:00:00Z"), locale: "en-US" });
  assert.equal(count, 2);
  assert.equal(title, "Test chat");
  assert.ok(text.indexOf("Question") < text.indexOf("Answer"));
  assert.match(text, /\] Ben\n\nQuestion/);
  assert.match(text, /Messages: 2/);
});

test("utility normalization is safe and predictable", () => {
  assert.equal(safeFileName(' Bad: name/with*chars. '), "Bad name with chars");
  assert.equal(normalizeTimestamp(1_700_000_000), 1_700_000_000_000);
  assert.equal(normalizeTimestamp(1_700_000_000_000_000), 1_700_000_000_000);
  assert.equal(wordCount("  one\n two  three "), 3);
});

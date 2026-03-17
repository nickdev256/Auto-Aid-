import crypto from "crypto";

const algorithm = "aes-256-cbc";

/**
 * Safely load encryption key and IV from environment
 */
function getKeyAndIv() {
  const key = process.env.DATA_ENCRYPTION_KEY;
  const iv = process.env.DATA_ENCRYPTION_IV;

  if (!key || !iv) {
    throw new Error("❌ DATA_ENCRYPTION_KEY or DATA_ENCRYPTION_IV is missing in .env");
  }

  if (key.length !== 32) {
    throw new Error("❌ DATA_ENCRYPTION_KEY must be exactly 32 characters");
  }

  if (iv.length !== 16) {
    throw new Error("❌ DATA_ENCRYPTION_IV must be exactly 16 characters");
  }

  return {
    key: Buffer.from(key, "utf8"),
    iv: Buffer.from(iv, "utf8"),
  };
}

// ✅ Helps detect whether a string is likely hex cipher text
function looksLikeHex(str) {
  return typeof str === "string" && /^[0-9a-fA-F]+$/.test(str) && str.length >= 32 && str.length % 2 === 0;
}

// ✅ Optional marker to distinguish encrypted vs plain values
const PREFIX = "enc:";

/**
 * Encrypt sensitive string data before saving to DB
 */
export const encrypt = (text) => {
  if (!text || typeof text !== "string") return text;

  const trimmed = text.trim();
  if (!trimmed) return text;

  // ✅ avoid double-encrypt
  if (trimmed.startsWith(PREFIX)) return trimmed;

  const { key, iv } = getKeyAndIv();
  const cipher = crypto.createCipheriv(algorithm, key, iv);

  let encrypted = cipher.update(trimmed, "utf8", "hex");
  encrypted += cipher.final("hex");

  // ✅ store as enc:<hex> to detect later
  return `${PREFIX}${encrypted}`;
};

/**
 * Decrypt sensitive string data when reading from DB
 * ✅ MUST NEVER crash if old DB values are plain text
 */
export const decrypt = (encryptedText) => {
  try {
    if (!encryptedText || typeof encryptedText !== "string") return encryptedText;

    const raw = encryptedText.trim();
    if (!raw) return encryptedText;

    // ✅ If it's not prefixed AND doesn't look like hex, assume it's plain text (old data)
    const isPrefixed = raw.startsWith(PREFIX);
    const hexPart = isPrefixed ? raw.slice(PREFIX.length) : raw;

    if (!looksLikeHex(hexPart)) {
      return raw; // treat as plain text
    }

    const { key, iv } = getKeyAndIv();
    const decipher = crypto.createDecipheriv(algorithm, key, iv);

    let decrypted = decipher.update(hexPart, "hex", "utf8");
    decrypted += decipher.final("utf8");

    return decrypted;
  } catch (e) {
    // ✅ Never crash API because of bad/old values
    return encryptedText;
  }
};
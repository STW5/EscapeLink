const DEVICE_ID_KEY = "escapelink_device_id";

/**
 * Creates a collision-resistant client id without requiring an HTTPS context.
 * `crypto.randomUUID()` is unavailable on some mobile browsers over plain HTTP.
 */
export function createClientId(): string {
  if (typeof crypto !== "undefined") {
    if (typeof crypto.randomUUID === "function") {
      return crypto.randomUUID();
    }

    if (typeof crypto.getRandomValues === "function") {
      const bytes = crypto.getRandomValues(new Uint8Array(16));
      bytes[6] = (bytes[6] & 0x0f) | 0x40;
      bytes[8] = (bytes[8] & 0x3f) | 0x80;
      const hex = Array.from(bytes, (byte) =>
        byte.toString(16).padStart(2, "0")
      );
      return [
        hex.slice(0, 4).join(""),
        hex.slice(4, 6).join(""),
        hex.slice(6, 8).join(""),
        hex.slice(8, 10).join(""),
        hex.slice(10, 16).join(""),
      ].join("-");
    }
  }

  return `client-${Date.now().toString(36)}-${Math.random()
    .toString(36)
    .slice(2)}`;
}

/** Stable per-browser id sent to the backend, used only for audit/debugging. */
export function getDeviceId(): string {
  if (typeof window === "undefined") {
    return "server";
  }
  let id = window.localStorage.getItem(DEVICE_ID_KEY);
  if (!id) {
    id = createClientId();
    window.localStorage.setItem(DEVICE_ID_KEY, id);
  }
  return id;
}

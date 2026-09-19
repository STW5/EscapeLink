const DEVICE_ID_KEY = "escapelink_device_id";

/** Stable per-browser id sent to the backend, used only for audit/debugging. */
export function getDeviceId(): string {
  if (typeof window === "undefined") {
    return "server";
  }
  let id = window.localStorage.getItem(DEVICE_ID_KEY);
  if (!id) {
    id = crypto.randomUUID();
    window.localStorage.setItem(DEVICE_ID_KEY, id);
  }
  return id;
}

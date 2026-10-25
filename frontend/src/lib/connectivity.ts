"use client";

import { useEffect, useRef, useState } from "react";

/** Re-fetch authoritative state after a device returns from offline/background. */
export function useRefreshOnResume(refresh: () => void) {
  const refreshRef = useRef(refresh);

  useEffect(() => {
    refreshRef.current = refresh;
  });

  useEffect(() => {
    const refreshNow = () => refreshRef.current();
    const refreshWhenVisible = () => {
      if (document.visibilityState === "visible") {
        refreshNow();
      }
    };

    window.addEventListener("online", refreshNow);
    window.addEventListener("pageshow", refreshNow);
    document.addEventListener("visibilitychange", refreshWhenVisible);

    return () => {
      window.removeEventListener("online", refreshNow);
      window.removeEventListener("pageshow", refreshNow);
      document.removeEventListener("visibilitychange", refreshWhenVisible);
    };
  }, []);
}

export function useOnlineStatus() {
  const [online, setOnline] = useState(true);

  useEffect(() => {
    const update = () => setOnline(navigator.onLine);
    update();
    window.addEventListener("online", update);
    window.addEventListener("offline", update);
    return () => {
      window.removeEventListener("online", update);
      window.removeEventListener("offline", update);
    };
  }, []);

  return online;
}

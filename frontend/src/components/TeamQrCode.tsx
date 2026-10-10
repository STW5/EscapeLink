"use client";

import { useEffect, useState } from "react";
import QRCode from "qrcode";

export default function TeamQrCode({ inviteToken }: { inviteToken: string }) {
  const [dataUrl, setDataUrl] = useState<string | null>(null);
  const [joinUrl, setJoinUrl] = useState<string | null>(null);

  useEffect(() => {
    const url = `${window.location.origin}/join?token=${inviteToken}`;
    setJoinUrl(url);
    QRCode.toDataURL(url, { width: 200, margin: 1 })
      .then(setDataUrl)
      .catch(() => setDataUrl(null));
  }, [inviteToken]);

  return (
    <div className="flex flex-col items-center gap-2 p-3 rounded-lg bg-white">
      {dataUrl ? (
        // eslint-disable-next-line @next/next/no-img-element
        <img src={dataUrl} alt="팀 접속 QR 코드" width={160} height={160} />
      ) : (
        <div className="w-40 h-40 flex items-center justify-center text-neutral-400 text-xs">
          QR 생성 중...
        </div>
      )}
      {joinUrl && (
        <button
          onClick={() => navigator.clipboard.writeText(joinUrl)}
          className="text-xs text-neutral-600 underline"
        >
          접속 링크 복사
        </button>
      )}
    </div>
  );
}

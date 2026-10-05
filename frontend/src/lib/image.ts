const MAX_DIMENSION = 1080;
const MAX_BYTES = 2 * 1024 * 1024;
const OUTPUT_MIME = "image/jpeg";

/**
 * Resizes to a 1080px long edge and re-encodes as JPEG, stepping quality
 * down until the result fits under 2MB — the server re-validates regardless,
 * this is purely to keep uploads fast on event-venue wifi.
 */
export async function compressImage(file: File): Promise<File> {
  let bitmap: ImageBitmap;
  try {
    bitmap = await createImageBitmap(file);
  } catch {
    return file;
  }

  const scale = Math.min(1, MAX_DIMENSION / Math.max(bitmap.width, bitmap.height));
  const width = Math.round(bitmap.width * scale);
  const height = Math.round(bitmap.height * scale);

  const canvas = document.createElement("canvas");
  canvas.width = width;
  canvas.height = height;
  const ctx = canvas.getContext("2d");
  if (!ctx) {
    return file;
  }
  ctx.drawImage(bitmap, 0, 0, width, height);

  let quality = 0.9;
  let blob: Blob | null = null;
  for (let attempt = 0; attempt < 6; attempt++) {
    blob = await new Promise<Blob | null>((resolve) =>
      canvas.toBlob(resolve, OUTPUT_MIME, quality)
    );
    if (!blob || blob.size <= MAX_BYTES) break;
    quality -= 0.15;
  }

  if (!blob) {
    return file;
  }

  const baseName = file.name.replace(/\.[^.]+$/, "") || "upload";
  return new File([blob], `${baseName}.jpg`, { type: OUTPUT_MIME });
}

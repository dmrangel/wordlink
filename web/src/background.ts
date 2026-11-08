// Background photos, as app/.../data/Backgrounds.kt does on Android: scaled
// down once on import, and blurred once per blur setting by shrinking and
// box-blurring a small copy. Drawing it scaled up is then a single cheap
// texture, so the photo costs nothing per frame.

const MAX_SIDE = 1600

/** Decodes the picked file (EXIF rotation applied by the browser) into a JPEG data URL. */
export async function importPhoto(file: File): Promise<string> {
  const image = await decode(file)
  const scale = Math.min(1, MAX_SIDE / Math.max(image.width, image.height))
  const canvas = document.createElement('canvas')
  canvas.width = Math.max(1, Math.round(image.width * scale))
  canvas.height = Math.max(1, Math.round(image.height * scale))
  canvas.getContext('2d')!.drawImage(image, 0, 0, canvas.width, canvas.height)
  if ('close' in image) image.close()
  return canvas.toDataURL('image/jpeg', 0.85)
}

async function decode(file: Blob): Promise<ImageBitmap | HTMLImageElement> {
  if ('createImageBitmap' in window) {
    try {
      return await createImageBitmap(file, { imageOrientation: 'from-image' })
    } catch {
      // Fall through to <img>, which also handles some formats ImageBitmap refuses.
    }
  }
  const url = URL.createObjectURL(file)
  try {
    const img = new Image()
    img.src = url
    await img.decode()
    return img
  } finally {
    URL.revokeObjectURL(url)
  }
}

/** The photo blurred by [blur] (0 = sharp, 1 = strongest), as an image URL for CSS. */
export async function blurred(dataUrl: string, blur: number): Promise<string> {
  if (blur <= 0) return dataUrl
  const img = new Image()
  img.src = dataUrl
  await img.decode()
  const width = Math.max(16, Math.round(window.innerWidth / (4 + blur * 28)))
  const height = Math.max(16, Math.round((img.height * width) / img.width))
  const canvas = document.createElement('canvas')
  canvas.width = width
  canvas.height = height
  const ctx = canvas.getContext('2d')!
  ctx.drawImage(img, 0, 0, width, height)
  const data = ctx.getImageData(0, 0, width, height)
  boxBlur(data.data, width, height, 2, 3)
  ctx.putImageData(data, 0, 0)
  return canvas.toDataURL('image/png')
}

/** Separable box blur over RGBA pixels; three passes approximate a Gaussian. */
function boxBlur(pixels: Uint8ClampedArray, w: number, h: number, radius: number, passes: number): void {
  const scratch = new Uint8ClampedArray(pixels.length)
  for (let pass = 0; pass < passes; pass++) {
    blurLines(pixels, scratch, w, h, radius, true)
    blurLines(scratch, pixels, w, h, radius, false)
  }
}

function blurLines(src: Uint8ClampedArray, dst: Uint8ClampedArray, w: number, h: number, radius: number, horizontal: boolean): void {
  const lines = horizontal ? h : w
  const length = horizontal ? w : h
  const window = radius * 2 + 1
  for (let line = 0; line < lines; line++) {
    const at = (i: number) => {
      const c = Math.min(length - 1, Math.max(0, i))
      return (horizontal ? line * w + c : c * w + line) * 4
    }
    let r = 0
    let g = 0
    let b = 0
    for (let i = -radius; i <= radius; i++) {
      const p = at(i)
      r += src[p]
      g += src[p + 1]
      b += src[p + 2]
    }
    for (let i = 0; i < length; i++) {
      const o = at(i)
      dst[o] = r / window
      dst[o + 1] = g / window
      dst[o + 2] = b / window
      dst[o + 3] = 255
      const out = at(i - radius)
      const inp = at(i + radius + 1)
      r += src[inp] - src[out]
      g += src[inp + 1] - src[out + 1]
      b += src[inp + 2] - src[out + 2]
    }
  }
}

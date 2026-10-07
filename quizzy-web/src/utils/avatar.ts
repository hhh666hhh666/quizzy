/**
 * 头像相关的两件事：**把用户选的图压小**、**没设头像时按用户 id 生成一张**。
 *
 * 为什么在前端做、为什么生成的头像不落库：见 `docs/adr/0028`。要点是——
 * 上传走 JSON 里的 data URL（没有文件上传通道），所以图必须小到能塞进请求体；
 * 而「默认头像」干脆不存，每次按 id 算出来即可，等于零存储。
 */

/** 用户选的**原图**上限。超过就让他换一张，而不是先传上来再说。 */
const MAX_SOURCE_BYTES = 2 * 1024 * 1024

/** 压完的边长上限。256 的方块存成 webp 通常只有 10~30KB。 */
const MAX_EDGE = 256

/**
 * 把用户选的图片压成可直接提交的 data URL。
 *
 * 用 canvas 重绘一遍是刻意的：一来把体积压下来，二来**顺手洗掉原图的元信息**
 * （EXIF 里的拍摄地点之类），三来输出的字节由浏览器重新编码，形状可控。
 */
export async function toAvatarDataUrl(file: File): Promise<string> {
  if (!file.type.startsWith('image/')) {
    throw new Error('请选择图片文件')
  }
  if (file.size > MAX_SOURCE_BYTES) {
    throw new Error('原图不能超过 2MB，请先压缩或换一张')
  }

  const image = await loadImage(file)
  const scale = Math.min(1, MAX_EDGE / Math.max(image.width, image.height))
  const width = Math.max(1, Math.round(image.width * scale))
  const height = Math.max(1, Math.round(image.height * scale))

  const canvas = document.createElement('canvas')
  canvas.width = width
  canvas.height = height
  const ctx = canvas.getContext('2d')
  if (!ctx) {
    throw new Error('当前浏览器不支持图片压缩')
  }
  ctx.drawImage(image, 0, 0, width, height)

  // 优先 webp；浏览器不认时 toDataURL 会**静默**回落到 png，所以这里要验一下前缀再决定。
  const webp = canvas.toDataURL('image/webp', 0.85)
  return webp.startsWith('data:image/webp') ? webp : canvas.toDataURL('image/png')
}

function loadImage(file: File): Promise<HTMLImageElement> {
  return new Promise((resolve, reject) => {
    const url = URL.createObjectURL(file)
    const image = new Image()
    image.onload = () => {
      URL.revokeObjectURL(url)
      resolve(image)
    }
    image.onerror = () => {
      URL.revokeObjectURL(url)
      reject(new Error('这张图读不出来，换一张试试'))
    }
    image.src = url
  })
}

/**
 * 由用户 id 派生一个稳定的哈希（FNV-1a 的 32 位版，够用且不需要依赖）。
 */
function hashSeed(seed: string): number {
  let hash = 2166136261
  for (let i = 0; i < seed.length; i += 1) {
    hash ^= seed.charCodeAt(i)
    hash = Math.imul(hash, 16777619)
  }
  return hash >>> 0
}

export interface GeneratedAvatar {
  /** 取色用的色相，0~359 */
  hue: number
  /** 需要填色的格子（5×5 网格里的坐标），左右对称 */
  cells: { x: number; y: number }[]
}

/**
 * GitHub 风格的默认头像：5×5 网格、**只算左半边再镜像**，所以天然左右对称；
 * 色相由 id 的哈希决定。同一个用户每次算出来都一样，不需要存。
 */
export function generatedAvatar(seed: string): GeneratedAvatar {
  const hash = hashSeed(seed)
  const cells: { x: number; y: number }[] = []
  for (let y = 0; y < 5; y += 1) {
    for (let x = 0; x < 3; x += 1) {
      // 用不同位取 0/1，避免相邻格子总是同色导致图案太规整
      if (((hash >>> (y * 3 + x)) & 1) === 1) {
        cells.push({ x, y })
        if (x !== 2) {
          cells.push({ x: 4 - x, y })
        }
      }
    }
  }
  return { hue: hash % 360, cells }
}

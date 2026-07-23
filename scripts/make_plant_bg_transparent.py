"""
将 assets/plants/ 下所有 PNG 的 xuan_paper 背景(#FFFFF5)转为透明
保留植物线条，边缘做模糊过渡
"""
import os
from PIL import Image

PLANT_DIR = r'd:\buddySpace\moyuanCoze\app\src\main\assets\plants'
BG_COLOR = (255, 255, 245)  # xuan_paper
TOLERANCE = 30  # 颜色容差：离背景色越远越不透明

def bg_to_alpha(img):
    """将 RGB 图片的背景色转为透明，返回 RGBA 图片"""
    img = img.convert("RGBA")
    pixels = img.load()
    w, h = img.size

    for y in range(h):
        for x in range(w):
            r, g, b, a = pixels[x, y]
            if a == 0:
                continue
            # 计算到背景色的距离
            dr = r - BG_COLOR[0]
            dg = g - BG_COLOR[1]
            db = b - BG_COLOR[2]
            dist = (dr*dr + dg*dg + db*db) ** 0.5

            if dist < TOLERANCE:
                # 背景色附近：渐变透明
                alpha = int((dist / TOLERANCE) * 255)
                pixels[x, y] = (r, g, b, min(a, max(0, alpha)))
            # 其他颜色完全保留

    return img

def main():
    processed = 0
    skipped = 0
    for root, dirs, files in os.walk(PLANT_DIR):
        for f in files:
            if not f.endswith('.png'):
                continue
            path = os.path.join(root, f)
            img = Image.open(path)
            if img.mode == 'RGBA':
                # 检查是否四角都是透明的（已处理过）
                w, h = img.size
                corner_pixels = [img.getpixel((0, 0)), img.getpixel((w-1, 0)),
                                 img.getpixel((0, h-1)), img.getpixel((w-1, h-1))]
                if all(p[3] == 0 for p in corner_pixels):
                    skipped += 1
                    continue

            result = bg_to_alpha(img)
            result.save(path, "PNG")
            processed += 1

    print(f"处理完成: {processed} 个文件已转透明, {skipped} 个已跳过")

if __name__ == "__main__":
    main()

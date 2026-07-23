"""将白色背景图片转为透明背景PNG"""
from PIL import Image
import sys, os

if len(sys.argv) < 2:
    print("用法: python remove_white_bg.py <输入图片路径> [输出图片路径]")
    sys.exit(1)

src = sys.argv[1]
dst = sys.argv[2] if len(sys.argv) > 2 else src.replace('.png', '_transparent.png')

img = Image.open(src).convert('RGBA')
pixels = img.load()

# 将接近白色的像素设为透明
white_threshold = 200  # RGB各通道大于此值视为白色
for y in range(img.height):
    for x in range(img.width):
        r, g, b, a = pixels[x, y]
        if r > white_threshold and g > white_threshold and b > white_threshold:
            pixels[x, y] = (r, g, b, 0)  # 透明

os.makedirs(os.path.dirname(dst) if os.path.dirname(dst) else '.', exist_ok=True)
img.save(dst, 'PNG')
print(f"已保存: {dst}")

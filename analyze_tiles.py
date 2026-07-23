"""分析Cozy Tileset瓦片内容，生成类型映射"""
from PIL import Image
import json

img = Image.open('app/src/main/assets/garden/cozy_tiles.png')
w, h = img.size
TILE = 16

tile_info = []
for y in range(0, h, TILE):
    for x in range(0, w, TILE):
        tile = img.crop((x, y, x+16, y+16))
        px = tile.load()
        # 分析非透明像素
        total_r, total_g, total_b, total_px = 0, 0, 0, 0
        has_green, has_brown, has_blue, has_pink = 0, 0, 0, 0
        for ty in range(TILE):
            for tx in range(TILE):
                r, g, b, a = px[tx, ty]
                if a > 128:
                    total_px += 1
                    total_r += r; total_g += g; total_b += b
                    if g > r + 30 and g > b + 30: has_green += 1
                    if r > g + 30 and r > 150 and b < 150: has_pink += 1
                    if b > g + 30 and b > r + 30: has_blue += 1
                    if r > g + 10 and r < 180 and g < 160: has_brown += 1
        
        if total_px == 0: continue  # empty tile
        
        avg_r, avg_g, avg_b = total_r//total_px, total_g//total_px, total_b//total_px
        
        # 分类
        tile_type = "unknown"
        if has_blue > 3: tile_type = "water"
        elif has_green > total_px * 0.3: tile_type = "plant_green"  # grass/leaves
        elif has_green > 5 and has_brown > 3: tile_type = "tree_green"  # tree with trunk
        elif has_pink > 5: tile_type = "flower_pink"  # pink flowers
        elif has_brown > total_px * 0.4: 
            if total_px < 80: tile_type = "fence_post"  # fence/wood
            else: tile_type = "ground_brown"
        elif has_green > 5: tile_type = "grass"
        else: tile_type = "other"
        
        tile_info.append({
            "col": x // TILE,
            "row": y // TILE,
            "pixels": total_px,
            "avg_rgb": [avg_r, avg_g, avg_b],
            "has_green": has_green,
            "has_brown": has_brown,
            "has_blue": has_blue,
            "has_pink": has_pink,
            "type": tile_type
        })

# 分组输出
for t in tile_info:
    print(f"tile({t['col']:2d},{t['row']:2d}): px={t['pixels']:3d} rgb=({t['avg_rgb'][0]:3d},{t['avg_rgb'][1]:3d},{t['avg_rgb'][2]:3d}) "
          f"G={t['has_green']:3d} Bn={t['has_brown']:3d} Bl={t['has_blue']:3d} P={t['has_pink']:3d} -> {t['type']}")

# 输出JSON映射
with open('app/src/main/assets/garden/tile_map.json', 'w') as f:
    json.dump(tile_info, f, indent=1)
print(f"\n总瓦片数: {len(tile_info)}")

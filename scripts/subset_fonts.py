"""
字体子集化脚本
从应用中提取的所有汉字对 TTF 字体做子集化，保留常用 ASCII 和标点
"""
import subprocess
import os

FONT_DIR = os.path.join(os.path.dirname(os.path.dirname(__file__)),
                        "app", "src", "main", "res", "font")

# 应用中提取的 1028 个唯一汉字 + 标点符号
UNICODE_CHARS = (
    "一七万三上下不与专且世东丝两个中临为主之乐乘九书事二五些交亦人今仍从他仔们以件任会传但位低作你佳使供依侠倍值假做像儿先光免全八公六共关其典再冬冰冲决况冷凄准刀分切初到刻前力功加成动助胜化十千午半华卓单南博印原去又友双发取变古只可叶号各各合同名向否吴含启呀呕呼命和品响哑哇哎哗哪哭啊商喜器四因园围困国图圆在均块坦型城境增士壮声处外多大天太夫失头夹奇奈奉奋女好她如始姓委姿娇子存孙季学它完定宜宝实客家容寒寸对导小少尔尖尚尤就尺尽局居届屋屏属山岁岗岛峰崇嵌已市布师希常幅幕平年并幸幻几度广庄庆延开式引归当录形彩往征待很律後得循微心心忆志快忽怎怠总恒恢恨恩悟患情惊想意感态慢成战所手打执扣找批承把报披抽拆拾持指按挑挽据探接提播操支收改放故效教数文斋新断方于日旧时明是星春昨映昼晕晨晴晶暂暮暴更替最月有朋望朝期未末本机权杆材束来杯枝极林果枝标格根案梁梦棒棵植楚概模次欢止正此步段殊残殿母每比毕毛毫民气水永求汇江池沉没河油治沿注活流浅浇浓消润淡深混清渐温满源溪灭灯灰灵烂点热然照熟燕片物状独率玉王玩环现理生用田由电画界留的目直相省盼看真眠眼知石码研破硬确示社祖神种秘积稳空穿立端第等简算管箭篇米类粉粒精系纪约级红纹组细结给络绝统经绿维综绿编缺网置美群翻者而耐耗聚育能脚自至致与旧节花芽苗若苦英茂范草荒荷获菊菩萌落葡藏虽蛋行街衣表被里要见规觉解言计认让识词话该说请读调象貌责赏走起超路身转轻较载辞辰边达过迈运近返回述迷追退送适逆逐途通遇道遥遵那部都醒采释重野量金针钓银错键长门问闲间关附际陆阳阴限陌降随隐雅雨雪零雷需露非面章项顺风食首香马骨鲜鸟黄墨默龄"
    "·×！？：；。，、\"\"''（）【】《》——……～⚠✓✕"
    "0123456789"
    "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
    ".,!?:;()-[]{}@#$%^&*+=/<>~ "
    "Lv.%"
)

def subset_font(input_path, output_path, chars):
    """使用 pyftsubset 对字体做子集化"""
    # 写入临时字符文件
    chars_file = input_path + ".chars.txt"
    with open(chars_file, "w", encoding="utf-8") as f:
        f.write(chars)

    cmd = [
        "pyftsubset",
        input_path,
        f"--text-file={chars_file}",
        "--output-file=" + output_path,
        "--layout-features=",
        "--flavor=",  # 保持 TTF 格式
        "--with-zopfli"
    ]

    result = subprocess.run(cmd, capture_output=True, text=True)
    if result.returncode != 0:
        print(f"子集化失败: {input_path}")
        print(result.stderr)
        return False

    # 清理临时文件
    os.remove(chars_file)
    return True


# ── 主流程 ──

def main():
    # 京华老宋 Regular
    print("正在子集化: 京华老宋")
    ok = subset_font(
        os.path.join(FONT_DIR, "jinghua_laosong_regular.ttf"),
        os.path.join(FONT_DIR, "jinghua_laosong_regular.ttf"),
        UNICODE_CHARS
    )
    if ok:
        size = os.path.getsize(os.path.join(FONT_DIR, "jinghua_laosong_regular.ttf"))
        print(f"  完成: {size // 1024} KB")

    # 霞鹜文楷 Regular
    print("正在子集化: 霞鹜文楷")
    ok = subset_font(
        os.path.join(FONT_DIR, "lxgw_wenkai_regular.ttf"),
        os.path.join(FONT_DIR, "lxgw_wenkai_regular.ttf"),
        UNICODE_CHARS
    )
    if ok:
        size = os.path.getsize(os.path.join(FONT_DIR, "lxgw_wenkai_regular.ttf"))
        print(f"  完成: {size // 1024} KB")

    print("子集化完成！")


if __name__ == "__main__":
    main()

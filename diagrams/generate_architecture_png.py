"""生成系统总体功能架构 PNG 图 — 用于嵌入 Word 文档"""

from pathlib import Path

# 尝试用 Pillow 生成，如果没有则 fallback 到 SVG
try:
    from PIL import Image, ImageDraw, ImageFont
    HAS_PIL = True
except ImportError:
    HAS_PIL = False

OUTPUT_DIR = Path(__file__).resolve().parent / "output"
OUTPUT_PNG = OUTPUT_DIR / "func-architecture-v2.png"
OUTPUT_SVG = OUTPUT_DIR / "func-architecture-v2.svg"


def find_font(sizes: list[int], candidates: list[str]):
    """查找可用字体"""
    for name in candidates:
        for size in sizes:
            try:
                return ImageFont.truetype(name, size)
            except (OSError, IOError):
                continue
    return ImageFont.load_default()


def generate_png():
    W, H = 1200, 980
    img = Image.new("RGB", (W, H), "#FAFAFA")
    draw = ImageDraw.Draw(img)

    # 字体
    font_title = find_font([24, 22, 20], ["msyh.ttc", "msyh.ttf", "simhei.ttf", "simsun.ttc", "arial.ttf"])
    font_sub = find_font([17, 16, 15], ["msyh.ttc", "msyh.ttf", "simhei.ttf", "simsun.ttc", "arial.ttf"])
    font_item = find_font([13, 12, 11], ["msyh.ttc", "msyh.ttf", "simsun.ttc", "arial.ttf"])

    def rounded_rect(xy, fill, outline, radius=12):
        x1, y1, x2, y2 = xy
        draw.rounded_rectangle(xy, radius=radius, fill=fill, outline=outline, width=2)

    def text_center(xy, text, fill, font):
        x1, y1, x2, y2 = xy
        bbox = draw.textbbox((0, 0), text, font=font)
        tw = bbox[2] - bbox[0]
        th = bbox[3] - bbox[1]
        tx = x1 + (x2 - x1 - tw) / 2
        ty = y1 + (y2 - y1 - th) / 2
        draw.text((tx, ty), text, fill=fill, font=font)

    def multiline_text_center(xy, text, fill, font, line_spacing=4):
        lines = text.split("\n")
        x1, y1, x2, y2 = xy
        line_heights = []
        for line in lines:
            bbox = draw.textbbox((0, 0), line, font=font)
            line_heights.append(bbox[3] - bbox[1])
        total_h = sum(line_heights) + line_spacing * (len(lines) - 1)
        cy = y1 + (y2 - y1 - total_h) / 2
        for i, (line, lh) in enumerate(zip(lines, line_heights)):
            bbox = draw.textbbox((0, 0), line, font=font)
            lw = bbox[2] - bbox[0]
            tx = x1 + (x2 - x1 - lw) / 2
            draw.text((tx, cy), line, fill=fill, font=font)
            cy += lh + line_spacing

    # ═══ 根节点 ═══
    root_box = (350, 20, 850, 80)
    rounded_rect(root_box, "#1A237E", "#0D1642", 12)
    text_center(root_box, "公路桥梁初始检查信息系统", "#FFFFFF", font_title)

    root_center_x = 600
    root_bottom = 80

    # ═══ 六个子系统 ═══
    subsystems = [
        {
            "title": "3.1.1 系统管理子系统",
            "items": ["用户管理", "角色与权限管理\n(RBAC)", "数据字典维护", "数据备份与恢复", "操作日志管理"],
            "x": 30, "y": 150, "w": 560, "h": 240,
        },
        {
            "title": "3.1.2 桥梁基础信息管理子系统",
            "items": ["桥梁信息管理\n(基本状况卡片)", "桥梁档案管理\n(图纸/验收资料)", "桥梁照片管理\n(三张总体照片)"],
            "x": 610, "y": 150, "w": 560, "h": 240,
        },
        {
            "title": "3.1.3 检查任务管理子系统",
            "items": ["任务创建与分配", "任务执行与\n状态跟踪", "任务审核与退回"],
            "x": 30, "y": 420, "w": 560, "h": 240,
        },
        {
            "title": "3.1.4 检查数据采集子系统",
            "items": ["检测模板加载\n(按桥型+等级)", "检测数据录入\n(构件-检测项)", "数据校验与提交\n(完整性+偏差)", "交工验收数据导入"],
            "x": 610, "y": 420, "w": 560, "h": 240,
        },
        {
            "title": "3.1.5 缺损记录管理子系统",
            "items": ["缺损记录\nCRUD", "示意图标注\n(百分比坐标)", "缺损照片管理\n(整体+细节)", "缺损查询与统计\n(多维+可视化)"],
            "x": 30, "y": 690, "w": 560, "h": 240,
        },
        {
            "title": "3.1.6 报告生成子系统",
            "items": ["报告自动生成\n(JTG标准格式)", "报告审核与签名\n(在线预览)", "版本管理\n(V1.0→V2.0)", "报告修订与归档\n(锁定只读)"],
            "x": 610, "y": 690, "w": 560, "h": 240,
        },
    ]

    for sub in subsystems:
        # 子系统外框
        rounded_rect((sub["x"], sub["y"], sub["x"] + sub["w"], sub["y"] + sub["h"]),
                      "#E8EAF6", "#5C6BC0", 10)
        # 子系统标题
        title_box = (sub["x"] + 15, sub["y"] + 8, sub["x"] + sub["w"] - 15, sub["y"] + 45)
        rounded_rect(title_box, "#C5CAE9", "#5C6BC0", 6)
        text_center(title_box, sub["title"], "#1A237E", font_sub)

        # 内部功能项
        n = len(sub["items"])
        item_w = (sub["w"] - 40) // n - 10
        for i, item in enumerate(sub["items"]):
            ix = sub["x"] + 25 + i * (item_w + 10)
            iy = sub["y"] + 60
            item_box = (ix, iy, ix + item_w, iy + 160)
            rounded_rect(item_box, "#F5F5F5", "#9FA8DA", 6)
            multiline_text_center(item_box, item, "#333333", font_item)

    # ═══ 连线：根节点 → 六个子系统 ═══
    for sub in subsystems:
        cx = sub["x"] + sub["w"] // 2
        top = sub["y"]
        draw.line([(root_center_x, root_bottom), (root_center_x, root_bottom + 25),
                    (cx, root_bottom + 25), (cx, top)],
                   fill="#7986CB", width=2)
        # 箭头
        draw.polygon([(cx - 6, top - 2), (cx + 6, top - 2), (cx, top + 8)],
                      fill="#7986CB")

    img.save(str(OUTPUT_PNG), "PNG")
    print(f"Generated: {OUTPUT_PNG}")


def generate_svg_fallback():
    """生成 SVG 备用"""
    svg = '''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1200 980" width="1200" height="980">
  <style>
    text { font-family: "Microsoft YaHei", sans-serif; }
    .root { fill: #1A237E; stroke: #0D1642; stroke-width: 2; }
    .sub { fill: #E8EAF6; stroke: #5C6BC0; stroke-width: 2; }
    .sub-title { fill: #C5CAE9; stroke: #5C6BC0; stroke-width: 1.5; }
    .item { fill: #F5F5F5; stroke: #9FA8DA; stroke-width: 1.5; }
    .edge { stroke: #7986CB; stroke-width: 2; fill: none; }
  </style>
  <rect width="1200" height="980" fill="#FAFAFA"/>
  <!-- Root -->
  <rect x="350" y="20" width="500" height="60" rx="12" class="root"/>
  <text x="600" y="58" text-anchor="middle" fill="white" font-size="22" font-weight="bold">公路桥梁初始检查信息系统</text>
  <!-- Edges and subsystems omitted for brevity — use PNG version -->
  <text x="600" y="500" text-anchor="middle" fill="#999" font-size="18">请使用 PNG 版本或 func-architecture.drawio 文件</text>
</svg>'''
    OUTPUT_SVG.write_text(svg, encoding="utf-8")
    print(f"Generated fallback: {OUTPUT_SVG}")


if __name__ == "__main__":
    if HAS_PIL:
        try:
            generate_png()
        except Exception as e:
            print(f"PNG generation failed: {e}, generating SVG fallback...")
            generate_svg_fallback()
    else:
        print("Pillow not installed, generating SVG fallback...")
        generate_svg_fallback()

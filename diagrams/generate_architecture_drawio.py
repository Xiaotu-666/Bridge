"""生成系统总体功能架构图 — draw.io 格式 (.drawio)，可在 draw.io 中打开并导出为 .vsdx"""

import xml.etree.ElementTree as ET
from pathlib import Path

OUTPUT = Path(__file__).resolve().parent / "output" / "func-architecture.drawio"


def mx_cell(attrib: dict) -> ET.Element:
    return ET.Element("mxCell", {k: str(v) for k, v in attrib.items()})


def build_diagram():
    root = ET.Element("mxfile", {
        "host": "draw.io",
        "type": "device",
        "version": "24.0.0",
    })
    diagram = ET.SubElement(root, "diagram", {
        "id": "func-arch",
        "name": "系统总体功能架构",
    })
    graph = ET.SubElement(diagram, "mxGraphModel", {
        "dx": "0", "dy": "0", "grid": "1", "gridSize": "10",
        "guides": "1", "tooltips": "1", "connect": "1",
        "arrows": "1", "fold": "1", "page": "1",
        "pageScale": "1", "pageWidth": "1200", "pageHeight": "950",
        "math": "0", "shadow": "0",
    })
    root_cell = ET.SubElement(graph, "root")

    # 两个必需的默认 cell
    ET.SubElement(root_cell, "mxCell", {"id": "0"})
    ET.SubElement(root_cell, "mxCell", {"id": "1", "parent": "0"})

    cell_counter = [2]

    def new_id():
        cid = str(cell_counter[0])
        cell_counter[0] += 1
        return cid

    def add_box(parent, x, y, w, h, label, fill, stroke, font_color,
                font_size=12, bold=False, rounded=True):
        cid = new_id()
        style = (
            f"rounded={'1' if rounded else '0'};whiteSpace=wrap;html=1;"
            f"fillColor={fill};strokeColor={stroke};fontColor={font_color};"
            f"fontSize={font_size};fontFamily=Microsoft YaHei;"
            f"{'fontStyle=1;' if bold else ''}"
            f"arcSize=6;"
        )
        cell = ET.SubElement(root_cell, "mxCell", {
            "id": cid, "value": label, "style": style,
            "vertex": "1", "parent": parent,
        })
        ET.SubElement(cell, "mxGeometry", {
            "x": str(x), "y": str(y), "width": str(w), "height": str(h),
            "as": "geometry",
        })
        return cid

    def add_edge(parent, source, target, color="#7986CB"):
        cid = new_id()
        style = (
            f"edgeStyle=orthogonalEdgeStyle;rounded=0;orthogonalLoop=1;"
            f"jettySize=auto;html=1;strokeColor={color};strokeWidth=1.5;"
            f"endArrow=block;endFill=1;"
        )
        cell = ET.SubElement(root_cell, "mxCell", {
            "id": cid, "style": style, "edge": "1",
            "parent": parent, "source": source, "target": target,
        })
        ET.SubElement(cell, "mxGeometry", {"relative": "1", "as": "geometry"})
        return cid

    # ═══ 根节点 ═══
    root_id = add_box(
        "1", 400, 30, 400, 55,
        "公路桥梁初始检查信息系统",
        "#1A237E", "#0D1642", "#FFFFFF",
        font_size=18, bold=True,
    )

    # ═══ 六个子系统 ═══
    subsystems = [
        ("3.1.1 系统管理子系统", 40, 160,
         ["用户管理", "角色与权限管理", "数据字典维护",
          "数据备份与恢复", "操作日志管理"]),
        ("3.1.2 桥梁基础信息管理子系统", 620, 160,
         ["桥梁信息管理", "桥梁档案管理",
          "桥梁照片管理"]),
        ("3.1.3 检查任务管理子系统", 40, 420,
         ["任务创建与分配", "任务执行与\n状态跟踪",
          "任务审核与退回"]),
        ("3.1.4 检查数据采集子系统", 620, 420,
         ["检测模板加载", "检测数据录入",
          "数据校验与提交", "交工验收数据导入"]),
        ("3.1.5 缺损记录管理子系统", 40, 680,
         ["缺损记录管理", "示意图标注",
          "缺损照片管理", "缺损查询与统计"]),
        ("3.1.6 报告生成子系统", 620, 680,
         ["报告自动生成", "报告审核与签名",
          "版本管理", "报告修订与归档"]),
    ]

    sub_ids = []
    for title, sx, sy, items in subsystems:
        sid = add_box(
            "1", sx, sy, 540, 230,
            title,
            "#E8EAF6", "#5C6BC0", "#1A237E",
            font_size=14, bold=True,
        )
        sub_ids.append(sid)

        n = len(items)
        item_w = max(490 // n, 100)
        for i, item in enumerate(items):
            ix = sx + 25 + i * item_w
            iy = sy + 55
            iw = item_w - 12
            ih = 150
            add_box(
                "1", ix, iy, iw, ih,
                item,
                "#F5F5F5", "#9FA8DA", "#333333",
                font_size=10,
            )

    # ═══ 连线 ═══
    for sid in sub_ids:
        add_edge("1", root_id, sid)

    # ═══ 写入 ═══
    tree = ET.ElementTree(root)
    ET.indent(tree, space="  ")
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    tree.write(str(OUTPUT), encoding="utf-8", xml_declaration=True)
    print(f"Generated: {OUTPUT}")


if __name__ == "__main__":
    build_diagram()

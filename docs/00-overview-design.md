# 第3部分 系统设计

本概要设计以项目需求、需求分析文档、JTG 5120-2021、系统总体功能设计图和桥梁模型示例图为依据。系统采用B/S架构，划分为系统管理、基础信息管理、桥梁档案管理、初始检测、定期检测和查询统计六个子系统。

## 3.1 系统功能设计

功能模型严格对应系统总体功能设计图，六大子系统通过统一身份认证、业务数据库和文件存储协同工作。

## 3.2 桥梁结构模型增补设计

根据桥梁模型示例图，桥梁核心结构采用“部位-部件-桥梁类型部件配置-桥梁部件-桥梁具体部件-桥梁具体部件检查记录”分层主链。桥梁类型通过配置关联标准部件，桥梁依据配置生成桥梁部件，实际桥墩、支座或主梁作为桥梁具体部件归入桥梁部件；桥梁基本信息按版本与桥梁形成1:N关系。

## 3.3 实现技术

- 前端：Vue 3 + Vite + Element Plus + Pinia + Vue Router。
- 后端：Java 17 + Spring Boot + Spring Security + JWT + JdbcTemplate。
- 数据库：MySQL 8.0 + InnoDB + Flyway。
- 文件：数据库保存路径，文件保存在 uploads 目录或对象存储。


# 第二部分 系统设计

> 基于《JTG 5120-2021 公路桥涵养护规范》和用户功能需求，设计整个软件系统需要完成的功能和数据结构。

---

## 第一章 功能架构

整个《公路桥梁初始检查信息系统》功能架构如图2.1所示。

**图2.1 系统总体功能架构**

```
公路桥梁初始检查信息系统
│
├── 1. 系统设置子系统
│   ├── 1.1 角色设置及权限配置
│   ├── 1.2 用户管理（增删改查）
│   ├── 1.3 系统日志查看
│   └── 1.4 数据备份与恢复
│
├── 2. 基础信息管理子系统
│   ├── 2.1 路线信息管理（增删改查）
│   ├──
│   ├── 2.3 基础字典维护（路线等级/桥型/养护等级/技术等级等）
│   └── 2.4 桥梁基本信息管理（增删改查）
│
├── 3. 桥梁档案管理子系统
│   ├── 3.1 桥梁基本状况卡片管理
│   │   ├── 3.1.1 行政识别数据录入
│   │   ├── 3.1.2 桥梁技术指标录入
│   │   ├── 3.1.3 桥梁结构信息录入（上部/下部/基础/支座/附属设施）
│   │   ├── 3.1.4 档案资料管理（设计图/竣工图/验收文件等）
│   │   ├── 3.1.5 检测评定历史记录
│   │   └── 
│   ├── 3.2 桥梁档案查询（多条件组合）
│   └── 3.3 桥梁档案统计（按路线/桥型/等级）
│
├── 4. 初始检查管理子系统
│   ├── 4.1 初始检查记录录入
│   │   ├── 4.1.1 基本信息（桥梁引用/检查时间/气候/机构）
│   │   ├── 4.1.2 线形及几何参数（桥面高程/拱轴线/墩台高程/主缆线形等13项）
│   │   ├── 4.1.3 构件尺寸测量（主要承重构件尺寸）
│   │   ├── 4.1.4 材质强度检测（混凝土强度/回弹法等）
│   │   ├── 4.1.5 保护层厚度检测
│   │   ├── 4.1.6 缆索索力测量（斜拉索/吊杆索力）
│   │   ├── 4.1.7 静载/动载试验结果
│   │   └── 4.1.8 钢管混凝土密实度检测
│   ├── 4.2 初始检查记录查询
│   └── 4.3 初始检查记录报表打印
│
├── 5. 定期检查管理子系统
│   ├── 5.1 定期检查记录录入
│   │   ├── 5.1.1 基本信息（桥梁引用/检查时间/气候/桥型选择）
│   │   ├── 5.1.2 桥面系评分（桥面铺装/伸缩缝/排水系统/人行道/栏杆/照明/桥路连接处）
│   │   ├── 5.1.3 上部结构评分（按桥型：主梁/主拱圈/主缆/斜拉索/吊杆/系杆/桥塔等）
│   │   ├── 5.1.4 下部结构评分（桥墩/桥台/翼墙/锥坡）
│   │   ├── 5.1.5 附属设施评分（防撞/防雷/防抛网/检修设施/监测系统）
│   │   ├── 5.1.6 缺损详细记录（类型/位置/范围/照片）
│   │   ├── 
│   │   └── 5.1.8 桥梁技术状况等级评定（1类-5类）
│   ├── 5.2 定期检查记录查询
│   ├── 5.3 定期检查记录对比分析（历次对比）
│   └── 5.4 定期检查记录报表打印
│
└── 6. 查询统计与决策支持子系统
    ├── 6.1 综合查询（多条件组合：路线/桥梁/日期/桥型/技术等级）
    ├── 6.2 桥梁技术状况等级统计分析
    ├── 6.4 检查到期提醒
    └── 6.5 数据导出（Excel/PDF）
```

### 1.1 系统设置子系统

#### 1.1.1 角色设置及权限配置

**功能描述**
完成角色的录入、修改，并设置角色的操作权限，即该角色能够使用的系统功能。本功能主要分为：角色录入、角色修改、角色权限设置。

**操作数据描述**
每个角色必须唯一，采用角色代码作为唯一标识。可操作的数据包含角色代码、角色名称、角色权限。角色权限用一个字符串表达，每个权限（即每个功能）用一个数字表达，多个权限之间用"、"分割。

系统预置四类角色：
| 角色代码 | 角色名称 | 默认权限范围 |
|---------|---------|------------|
| admin | 系统管理员 | 全部功能（系统设置+全部业务模块） |
| engineer | 桥梁工程师 | 全部业务模块读写 + 评定审核 |
| inspector | 检测人员 | 检查记录录入/查询 + 桥梁档案查看 |
| viewer | 查询浏览者 | 全部查询统计（只读） |

#### 1.1.2 用户管理

**功能描述**
完成系统用户的添加、修改、删除和查询。每个用户必须关联到一个角色，系统通过角色间接控制用户的操作权限。用户登录时通过用户名和密码进行身份验证。

**操作数据描述**
用户信息包含：用户编号（自动生成）、用户名（唯一约束）、密码（pbkdf2哈希存储）、真实姓名、角色、所属单位、联系电话、邮箱、启用状态。用户新增时必须录入用户名和密码，用户修改时密码可选改。

---

### 1.2 基础信息管理子系统

#### 1.2.1 路线信息管理

**功能描述**
完成公路路线的录入、修改、删除和查询功能。路线是桥梁的归类维度，每条路线可以包含多座桥梁。

**操作数据描述**
路线信息包含：路线编号（主键，如"G101"）、路线名称（如"京沪线"）、路线等级（引用字典表route_grade编码）。路线编号必须唯一。

#### 1.2.2 管养单位管理

**功能描述**
完成管养单位的录入、修改、删除和查询功能。单位信息供桥梁、用户等实体关联引用。

**操作数据描述**
单位信息包含：单位编号（主键）、单位名称、单位类型（管养/设计/施工/监理/业主）、联系人、联系电话、地址。单位编号必须唯一。

#### 1.2.3 基础字典维护

**功能描述**
完成系统各类编码字典的统一维护，包括路线等级、桥型分类、养护检查等级、技术状况等级、结构部位、档案类型、完整程度、处治类别等的编码与名称对应关系。

**操作数据描述**
字典信息包含：字典条目编号（自增）、字典类别（dict_type，字符串）、编码（code，字符串）、名称（name，字符串）、排序号、启用状态。同一类别下编码不得重复。

已内置9大类字典数据，详见数据模型设计部分。

#### 1.2.4 桥梁基本信息管理

**功能描述**
完成桥梁基本信息的录入、修改、删除和查询功能。桥梁是整个系统的核心实体，后续的卡片、检查记录均以桥梁为单位进行关联。

**操作数据描述**
桥梁信息包含：桥梁编号（主键）、桥梁名称、所属路线、管养单位、桥位桩号、功能类型、桥梁全长、桥面总宽、车道宽度、人行道宽度、护栏高度、中央分隔带宽度、桥面净空（标准/实际）、通航净空、设计荷载、养护检查等级、建成时间、设计/施工/监理/业主单位、状态等。桥梁编号必须唯一。

---

### 1.3 桥梁档案管理子系统

#### 1.3.1 桥梁基本状况卡片管理

**功能描述**
以"一桥一档"为原则，基于《JTG 5120-2021》附录A桥梁基本状况卡片，完成桥梁全生命周期档案的电子化管理。该卡片包含行政识别数据、桥梁技术指标、桥梁结构信息、档案资料记录、检测评定历史和养护处治记录六大部分。

**操作数据描述**
（1）行政识别数据：行政区划代码、路线编号、路线名称、路线等级、桥梁编号、桥梁名称、桥位桩号、功能类型、设计荷载、建成时间、设计/施工/监理/业主/管养单位。
（2）桥梁技术指标：桥梁全长、桥面总宽、车道宽度、人行道宽度、护栏高度、中央分隔带宽度、桥面标准/实际净空、桥下通航净空、引道宽度、设计洪水频率/水位、历史洪水位、地震动峰值加速度、桥面高程。
（3）桥梁结构信息：按部位（上部结构/下部结构/桥面系/基础/支座/附属设施）记录各构件的形式和材料。典型构件包括：主梁、主拱圈、桥塔、主缆、斜拉索、吊杆、系杆、桥面铺装、伸缩缝、桥墩、桥台、锥坡、翼墙、基础、锚碇、支座、防撞设施等。
（4）档案资料：记录各类档案（设计图纸/设计文件/竣工图纸/施工文件/验收文件/行政审批/定期检查资料/特殊检查资料/维修加固资料/其他）的完整程度（全/不全/无）和存储形式（纸质/电子）。
（5）检测评定历史：按时间记录每次检测的类别、评定结果/结论、处治对策、下次检测时间。
（6）养护处治记录：按时间段记录处治类别（维修/加固/改造）、原因、范围、费用、经费来源、质量评定及相关单位。

---

### 1.4 初始检查管理子系统

#### 1.4.1 初始检查记录录入

**功能描述**
基于《JTG 5120-2021》附录B桥梁初始检查记录表，完成新建或改建桥梁交付使用后的首次全面检测数据电子化录入。

**操作数据描述**
（1）基本信息：引用桥梁（从桥梁表中选取）、检查编号（自编，如INI-2020-001）、被跨越道路名称、桥梁总长、最大跨径、上部结构形式、分联及跨径组合、施工方法、设计/施工/管养单位名称、交工时间、检查时间、检查时气候及环境温度、记录人、桥梁工程师、检查机构。
（2）检测明细项（22类）：桥面高程、拱轴线、主缆线形、墩台高程、墩台倾斜度、索塔水平变位/高程、拱桥桥台/锚碇水平位移、索夹螺栓紧固力、水中基础情况、斜拉索/吊杆索力、主要承重构件尺寸、材质强度、保护层厚度、钢管混凝土密实度、静载试验结果、动载试验结果等。
（3）检测项采用键值对形式灵活存储：检测项名称（固定选项列表）、检测值（文本/数值/JSON结构）、单位、备注。

#### 1.4.2 初始检查记录查询

**功能描述**
按桥梁名称、检查日期范围等条件组合查询初始检查记录列表，点击可查看详细检测项数据。

#### 1.4.3 初始检查报表打印

**功能描述**
将初始检查记录按规范附录B表格样式排版，生成可打印的报表（HTML格式，支持浏览器打印）。

---

### 1.5 定期检查管理子系统

#### 1.5.1 定期检查记录录入

**功能描述**
基于《JTG 5120-2021》附录C（C-1至C-6），按6种桥型（梁式桥、板拱/肋拱/箱形拱/双曲拱桥、刚架拱/桁架拱桥、钢-混凝土组合拱桥、斜拉桥、悬索桥）分别管理周期性检查数据。录入时需先选择桥型，系统展示该桥型对应的部件评分模板。

**操作数据描述**
（1）基本信息：引用桥梁、桥型编码、检查编号（自编，如REG-2023-001）、桥梁全长、主跨结构、最大跨径、管养单位、建成时间、上次修复时间、上次检查时间、本次检查时间、检查时气候及环境温度、记录人、负责人、下次检查时间。
（2）部件评分：按部位（桥面系/上部结构/下部结构/附属设施）对每个部件进行5级制评分（0完好-5危险），并记录缺损类型、缺损位置、缺损范围、缺损照片路径、养护建议（维修范围/方式/时间）、是否需要特殊检查、是否为最不利构件。
（3）桥梁技术状况评定：根据各部件评分，依据规范计算全桥技术状况等级（1类完好~5类危险）。

#### 1.5.2 定期检查记录查询与对比分析

**功能描述**
按桥梁名称、桥型、检查日期范围等条件组合查询定期检查记录列表。对同一桥梁的历史检查记录进行对比展示，支持查看技术状况等级变化趋势。

#### 1.5.3 定期检查报表打印

**功能描述**
将定期检查记录（含部件评分和缺损详情）按规范附录C表格样式排版，生成可打印报表。

---

### 1.6 查询统计与决策支持子系统

#### 1.6.1 综合查询

**功能描述**
提供跨模块的多条件组合查询功能，可同时按路线、桥梁名称/编号、检查类型（初始/定期）、检查日期范围、桥梁养护等级、桥型、技术状况等级等维度进行筛选，结果以分页列表展示。

#### 1.6.2 技术状况统计分析

**功能描述**
按桥梁技术状况等级（1类~5类）统计桥梁数量分布，支持按路线、桥型、时间范围等维度下钻分析，以表格形式展示统计数据。

#### 1.6.3 缺损病害汇总分析

**功能描述**
汇总统计定期检查中记录的缺损类型和频次，帮助掌握常见病害分布，辅助制定养护计划。

#### 1.6.4 检查到期提醒

**功能描述**
根据桥梁养护检查等级（Ⅰ/Ⅱ/Ⅲ级）和最近检查日期，自动计算下次检查到期时间，对即将到期或已逾期的检查进行提醒展示。

#### 1.6.5 数据导出

**功能描述**
将查询结果或统计报表导出为Excel文件或PDF格式，方便线下存档和汇报。


## 第二章 数据架构设计

> 根据系统功能需求，设计系统的数据存储结构，包含概念数据模型、逻辑模型和物理模型三个层次。

---

### 2.1 概念数据模型

概念数据模型是对现实世界业务数据的第一次抽象，采用实体-联系（E-R）方法描述系统中的实体、属性及其相互关系。

#### 2.1.1 核心实体清单

根据《JTG 5120-2021 公路桥涵养护规范》附录A（桥梁基本状况卡片）、附录B（桥梁初始检查记录表）、附录C（桥梁定期检查记录表）分析，识别出以下15个核心实体：

| 编号 | 实体名称 | 实体含义 | 主要属性 | 对应规范来源 |
|------|---------|---------|---------|------------|
| E01 | Route | 路线（公路线路） | 路线编号、路线名称、路线等级 | — |
| E02 | ManagementUnit | 管养单位 | 单位编号、单位名称、单位类型、联系人、电话 | — |
| E03 | Bridge | 桥梁基本信息 | 桥梁编号、桥梁名称、桥位桩号、桥梁全长、桥面总宽、建成时间、设计荷载、养护等级等（约30个属性） | 附录A(B) |
| E04 | BridgeCard | 桥梁基本状况卡片 | 卡片编号、桥梁ID（一桥一卡）、行政区划代码、填卡人、填卡日期 | 附录A |
| E05 | BridgeStructure | 桥梁结构信息 | 部位类型、构件名称、结构形式、材料 | 附录A(D) |
| E06 | ArchiveRecord | 档案资料记录 | 档案类型、完整程度（全/不全/无）、档案形式（纸质/电子） | 附录A(E) |
| E07 | InspectionHistory | 检测评定历史 | 评定时间、检测类别、评定结果、处治对策、下次检测时间 | 附录A(F) |
| E08 | MaintenanceRecord | 养护处治记录 | 时间段、处治类别、原因、范围、费用、经费来源 | 附录A(G) |
| E09 | InitialInspection | 初始检查记录 | 检查编号、桥梁ID、检查日期、气候温度、检查机构等（约20个属性） | 附录B |
| E10 | InitialInspectionItem | 初始检查检测项 | 检测项名称、检测值、单位、备注（22类检测项，键值对存储） | 附录B |
| E11 | RegularInspection | 定期检查记录 | 检查编号、桥梁ID、桥型编码、检查日期、技术状况等级、下次检查日期等 | 附录C |
| E12 | ComponentScore | 部件评分记录 | 部位名称、部件名称、评分值(0-5)、缺损类型/位置/范围、养护建议 | 附录C |
| E13 | DefectPhoto | 缺损照片 | 照片文件路径、照片说明 | 附录C |
| E14 | User | 系统用户 | 用户名、密码哈希、真实姓名、角色 | — |
| E15 | DictCode | 字典编码表 | 字典类别、编码、名称、排序号 | — |

#### 2.1.2 实体间E-R关系

```
Route(1) ────< 管辖 >────>> Bridge(N)
ManagementUnit(1) ──< 管养 >────>> Bridge(N)
Bridge(1) ────< 拥有 >────── BridgeCard(1)
BridgeCard(1) ────< 包含 >──────>> BridgeStructure(N)
BridgeCard(1) ────< 包含 >──────>> ArchiveRecord(N)
BridgeCard(1) ────< 记录 >──────>> InspectionHistory(N)
BridgeCard(1) ────< 记录 >──────>> MaintenanceRecord(N)
Bridge(1) ────< 进行 >──────>> InitialInspection(N)
InitialInspection(1) ──< 包含 >──────>> InitialInspectionItem(N)
Bridge(1) ────< 进行 >──────>> RegularInspection(N)
RegularInspection(1) ──< 包含 >──────>> ComponentScore(N)
ComponentScore(1) ──< 包含 >──────>> DefectPhoto(N)
ManagementUnit(1) ──< 管理 >──────>> User(N)
```

#### 2.1.3 实体关系矩阵

| 实体A | 关系名 | 实体B | 基数 | 说明 |
|--------|--------|--------|------|------|
| Route | 管辖 | Bridge | 1:N | 一条路线可有多座桥梁 |
| ManagementUnit | 管养 | Bridge | 1:N | 一个管养单位负责多座桥梁 |
| Bridge | 拥有 | BridgeCard | 1:1 | 每座桥一份基本状况卡片 |
| BridgeCard | 包含 | BridgeStructure | 1:N | 卡片记录多项结构信息 |
| BridgeCard | 包含 | ArchiveRecord | 1:N | 卡片记录多项档案资料 |
| BridgeCard | 记录 | InspectionHistory | 1:N | 卡片记录多次检测历史 |
| BridgeCard | 记录 | MaintenanceRecord | 1:N | 卡片记录多次养护处治 |
| Bridge | 进行 | InitialInspection | 1:N | 桥梁可进行多次初始检查 |
| InitialInspection | 包含 | InitialInspectionItem | 1:N | 一次检查包含多个检测项 |
| Bridge | 进行 | RegularInspection | 1:N | 桥梁可进行多次定期检查 |
| RegularInspection | 包含 | ComponentScore | 1:N | 一次检查包含多个部件评分 |
| ComponentScore | 包含 | DefectPhoto | 1:N | 一个部件评分可有多张照片 |
| ManagementUnit | 管理 | User | 1:N | 一个单位管理多名用户 |

---

### 2.2 逻辑模型设计

逻辑模型设计将概念模型中的实体和关系转换为关系数据库中的表结构，确定每个表包含的字段、数据类型、主键和外键约束。

#### 2.2.1 表结构清单

本系统共设计 **15张数据表**，分为五组：

| 分组 | 表名 | 中文名称 | 记录数(预估) | 说明 |
|------|------|---------|------------|------|
| 基础数据 | route | 路线信息表 | < 100 | 公路路线 |
| 基础数据 | management_unit | 管养单位表 | < 50 | 管理/设计/施工/监理单位 |
| 基础数据 | dict_code | 字典编码表 | < 200 | 系统各类编码字典 |
| 桥梁档案 | bridge | 桥梁基本信息表 | < 10,000 | 桥梁核心信息 |
| 桥梁档案 | bridge_card | 桥梁基本状况卡片表 | < 10,000 | 一桥一卡 |
| 桥梁档案 | bridge_structure | 桥梁结构信息表 | < 50,000 | 每桥约5-10项结构记录 |
| 桥梁档案 | archive_record | 档案资料记录表 | < 50,000 | 每桥约5-8项档案记录 |
| 桥梁档案 | inspection_history | 检测评定历史表 | < 50,000 | 每桥多次检测历史 |
| 桥梁档案 | maintenance_record | 养护处治记录表 | < 20,000 | 每桥数次养护记录 |
| 初始检查 | initial_inspection | 初始检查记录表 | < 10,000 | 每桥至少1次初始检查 |
| 初始检查 | initial_inspection_item | 初始检查检测项表 | < 100,000 | 每次检查约5-15个检测项 |
| 定期检查 | regular_inspection | 定期检查记录表 | < 50,000 | 每桥多次定期检查 |
| 定期检查 | component_score | 部件评分记录表 | < 300,000 | 每次检查约10-25个部件 |
| 定期检查 | defect_photo | 缺损照片表 | < 100,000 | 缺损照片文件引用 |
| 系统管理 | user | 系统用户表 | < 100 | 系统操作用户 |

#### 2.2.2 核心表关系模式定义

**表1：bridge（桥梁基本信息表）**

| 字段名 | 类型 | 允许空 | 约束 | 说明 |
|--------|------|--------|------|------|
| bridge_id | VARCHAR(20) | NOT NULL | PRIMARY KEY | 桥梁编号 |
| bridge_name | VARCHAR(100) | NOT NULL | | 桥梁名称 |
| route_id | VARCHAR(20) | | FOREIGN KEY → route | 所属路线编号 |
| unit_id | VARCHAR(20) | | FOREIGN KEY → management_unit | 管养单位编号 |
| pile_no | VARCHAR(50) | | | 桥位桩号 |
| functional_type | VARCHAR(20) | | | 功能类型(公路/公铁两用) |
| total_length | DECIMAL(8,2) | | | 桥梁全长(m) |
| deck_width | DECIMAL(6,2) | | | 桥面总宽(m) |
| lane_width | DECIMAL(6,2) | | | 车道宽度(m) |
| design_load | VARCHAR(50) | | | 设计荷载 |
| bridge_grade | VARCHAR(20) | | | 养护检查等级 |
| built_date | DATE | | | 建成时间 |
| design_unit_id | VARCHAR(20) | | FOREIGN KEY → management_unit | 设计单位 |
| construction_unit_id | VARCHAR(20) | | FOREIGN KEY → management_unit | 施工单位 |
| supervisor_unit_id | VARCHAR(20) | | FOREIGN KEY → management_unit | 监理单位 |
| owner_unit_id | VARCHAR(20) | | FOREIGN KEY → management_unit | 业主单位 |
| status | VARCHAR(10) | | DEFAULT 'active' | 状态(active/inactive) |

**表2：bridge_card（桥梁基本状况卡片表）**

| 字段名 | 类型 | 允许空 | 约束 | 说明 |
|--------|------|--------|------|------|
| card_id | VARCHAR(20) | NOT NULL | PRIMARY KEY | 卡片编号 |
| bridge_id | VARCHAR(20) | NOT NULL | FOREIGN KEY → bridge, UNIQUE | 桥梁编号（一桥一卡） |
| admin_code | VARCHAR(20) | | | 行政区划代码 |
| deck_elevation | TEXT | | | 桥面高程(JSON存储多测点) |
| fill_person | VARCHAR(50) | | | 填卡人 |
| fill_date | DATE | | | 填卡日期 |
| bridge_engineer | VARCHAR(50) | | | 桥梁工程师 |

**表3：bridge_structure（桥梁结构信息表）**

| 字段名 | 类型 | 允许空 | 约束 | 说明 |
|--------|------|--------|------|------|
| struct_id | INTEGER | NOT NULL | PRIMARY KEY AUTO | 结构编号 |
| card_id | VARCHAR(20) | NOT NULL | FOREIGN KEY → bridge_card | 所属卡片 |
| part_category | VARCHAR(20) | NOT NULL | | 部位大类（上部/下部/桥面系/基础/支座/附属） |
| component_name | VARCHAR(50) | NOT NULL | | 构件名称 |
| struct_form | VARCHAR(100) | | | 结构形式 |
| material | VARCHAR(100) | | | 材料 |

**表4：initial_inspection（初始检查记录表）**

| 字段名 | 类型 | 允许空 | 约束 | 说明 |
|--------|------|--------|------|------|
| inspect_id | VARCHAR(20) | NOT NULL | PRIMARY KEY | 检查编号 |
| bridge_id | VARCHAR(20) | NOT NULL | FOREIGN KEY → bridge | 桥梁编号 |
| across_name | VARCHAR(100) | | | 被跨越道路名称 |
| bridge_length | DECIMAL(8,2) | | | 桥梁全长(m) |
| max_span | DECIMAL(8,2) | | | 最大跨径(m) |
| struct_type | VARCHAR(100) | | | 上、下部结构形式 |
| construct_method | VARCHAR(100) | | | 桥梁施工方法 |
| completion_date | DATE | | | 交工时间 |
| inspect_date | DATE | NOT NULL | | 初始检查日期 |
| climate | VARCHAR(100) | | | 检查时气候 |
| temperature | DECIMAL(4,1) | | | 环境温度(℃) |
| recorder | VARCHAR(50) | | | 记录人 |
| engineer | VARCHAR(50) | | | 桥梁工程师 |
| inspect_org | VARCHAR(100) | | | 检查机构 |

**表5：initial_inspection_item（初始检查检测明细表）**

| 字段名 | 类型 | 允许空 | 约束 | 说明 |
|--------|------|--------|------|------|
| item_id | INTEGER | NOT NULL | PRIMARY KEY AUTO | 检测项编号 |
| inspect_id | VARCHAR(20) | NOT NULL | FOREIGN KEY → initial_inspection | 所属检查 |
| item_name | VARCHAR(100) | NOT NULL | | 检测项名称 |
| item_value | TEXT | | | 检测值（文本/数值/JSON） |
| unit | VARCHAR(20) | | | 单位 |
| remark | TEXT | | | 备注 |

**表6：regular_inspection（定期检查记录表）**

| 字段名 | 类型 | 允许空 | 约束 | 说明 |
|--------|------|--------|------|------|
| inspect_id | VARCHAR(20) | NOT NULL | PRIMARY KEY | 检查编号 |
| bridge_id | VARCHAR(20) | NOT NULL | FOREIGN KEY → bridge | 桥梁编号 |
| bridge_type_code | VARCHAR(20) | NOT NULL | | 桥型编码 |
| inspect_date | DATE | NOT NULL | | 检查日期 |
| climate | VARCHAR(100) | | | 检查时气候 |
| temperature | DECIMAL(4,1) | | | 环境温度(℃) |
| tech_level | INTEGER | | | 技术状况等级(1-5) |
| recorder | VARCHAR(50) | | | 记录人 |
| engineer | VARCHAR(50) | | | 负责人 |
| next_inspect_date | DATE | | | 下次检查日期 |

**表7：component_score（部件评分记录表）**

| 字段名 | 类型 | 允许空 | 约束 | 说明 |
|--------|------|--------|------|------|
| score_id | INTEGER | NOT NULL | PRIMARY KEY AUTO | 评分编号 |
| inspect_id | VARCHAR(20) | NOT NULL | FOREIGN KEY → regular_inspection | 所属检查 |
| part_name | VARCHAR(50) | NOT NULL | | 部位名称(桥面系/上部/下部/附属) |
| component_name | VARCHAR(50) | NOT NULL | | 部件名称 |
| score | INTEGER | | | 评分值(0-5) |
| defect_type | TEXT | | | 缺损类型 |
| defect_location | TEXT | | | 缺损位置 |
| defect_range | TEXT | | | 缺损范围 |
| maintain_suggestion | TEXT | | | 养护建议 |
| need_special_inspect | VARCHAR(10) | | | 是否需要特殊检查 |

**表8：user（系统用户表）**

| 字段名 | 类型 | 允许空 | 约束 | 说明 |
|--------|------|--------|------|------|
| user_id | INTEGER | NOT NULL | PRIMARY KEY AUTO | 用户编号 |
| username | VARCHAR(50) | NOT NULL | UNIQUE | 用户名 |
| password_hash | VARCHAR(256) | NOT NULL | | 密码哈希 |
| real_name | VARCHAR(50) | | | 真实姓名 |
| role | VARCHAR(20) | NOT NULL | | 角色(admin/engineer/inspector/viewer) |
| unit_id | VARCHAR(20) | | FOREIGN KEY → management_unit | 所属单位 |
| phone | VARCHAR(20) | | | 联系电话 |
| email | VARCHAR(100) | | | 邮箱 |
| is_active | INTEGER | DEFAULT 1 | | 是否启用 |

> **备注：** 其余7张表（route、management_unit、archive_record、inspection_history、maintenance_record、defect_photo、dict_code）的结构定义详见 `database/01-init-schema.sql` 文件。

#### 2.2.3 字典编码体系

系统内置9大类字典数据，通过dict_code表集中管理：

| 字典类别 | 编码示例 | 条目数 | 说明 |
|---------|---------|-------|------|
| route_grade | expressway/class1/class2/class3/class4 | 5 | 路线等级 |
| inspect_grade | 1/2/3 | 3 | 养护检查等级(Ⅰ/Ⅱ/Ⅲ级) |
| bridge_form | beam/arch/rigid_frame/steel_concrete/cable_stayed/suspension | 6 | 桥梁结构类型 |
| tech_level | 1/2/3/4/5 | 5 | 技术状况评定等级(1类-5类) |
| struct_part | deck_system/superstructure/substructure/foundation/bearing/ancillary | 6 | 结构部位分类 |
| archive_type | design_dwg/design_doc/asbuilt_dwg/...  | 9 | 档案资料类型 |
| completeness | full/partial/none | 3 | 资料完整程度 |
| maintenance_cat | repair/reinforce/rebuild | 3 | 养护处治类别 |
| user_role | admin/engineer/inspector/viewer | 4 | 用户角色 |

---

### 2.3 物理模型设计

物理模型设计确定数据在数据库中的具体存储方式，包括DBMS选型、建表DDL语句、索引设计和数据文件组织。

#### 2.3.1 数据库平台选择

本系统选用 **SQLite 3** 作为数据库平台，主要考虑因素：

| 考虑因素 | 说明 |
|---------|------|
| **轻量级** | SQLite是嵌入式数据库，无需独立服务器进程，零配置部署 |
| **便携性** | 数据库为单一文件（bridge_inspection.db），便于备份、迁移 |
| **课程适用** | 满足课程设计规模要求（<10万条级数据），性能完全够用 |
| **兼容性** | 通过SQLAlchemy ORM可无缝切换至MySQL/PostgreSQL |
| **标准化** | 支持标准SQL，含外键约束、事务、索引等完整特性 |

> 若后续有扩展需求（多用户并发、GIS空间数据），可通过修改`config.py`中的`SQLALCHEMY_DATABASE_URI`一行代码升级至MySQL/PostgreSQL，无需修改业务代码。

#### 2.3.2 建表DDL脚本

完整建表脚本位于 `database/01-init-schema.sql`，包含：
- 15张表的 CREATE TABLE 语句（含完整字段定义、主键、外键、默认值）
- 30+个索引定义（覆盖 route_id、bridge_id、inspect_date、tech_level、dict_type等高频查询字段）
- 44条字典基础数据的 INSERT 语句
- 外键约束全部通过 `FOREIGN KEY ... REFERENCES` 声明

示例：

```sql
CREATE TABLE IF NOT EXISTS bridge (
    bridge_id VARCHAR(20) PRIMARY KEY,
    bridge_name VARCHAR(100) NOT NULL,
    route_id VARCHAR(20),
    unit_id VARCHAR(20),
    total_length DECIMAL(8,2),
    deck_width DECIMAL(6,2),
    design_load VARCHAR(50),
    bridge_grade VARCHAR(20),
    built_date DATE,
    status VARCHAR(10) DEFAULT 'active',
    FOREIGN KEY (route_id) REFERENCES route(route_id),
    FOREIGN KEY (unit_id) REFERENCES management_unit(unit_id)
);

CREATE INDEX idx_bridge_route ON bridge(route_id);
CREATE INDEX idx_bridge_name ON bridge(bridge_name);
```

#### 2.3.3 索引设计

| 索引名称 | 所在表 | 索引字段 | 索引目的 |
|---------|--------|---------|---------|
| idx_bridge_route | bridge | route_id | 按路线查询桥梁 |
| idx_bridge_name | bridge | bridge_name | 桥梁名称模糊搜索 |
| idx_bridge_status | bridge | status | 过滤在用桥梁 |
| idx_card_bridge | bridge_card | bridge_id | 通过桥梁查卡片 |
| idx_initial_date | initial_inspection | inspect_date | 按日期排序查询 |
| idx_initial_bridge | initial_inspection | bridge_id | 查某桥梁的所有初始检查 |
| idx_regular_date | regular_inspection | inspect_date | 按日期排序查询 |
| idx_regular_bridge | regular_inspection | bridge_id | 查某桥梁的所有定期检查 |
| idx_regular_level | regular_inspection | tech_level | 按技术等级统计 |
| idx_regular_type | regular_inspection | bridge_type_code | 按桥型筛选 |
| idx_component_inspect | component_score | inspect_id | 一次检查的全部部件 |
| idx_component_part | component_score | part_name | 按部位筛选评分 |
| idx_dict_type | dict_code | dict_type | 按类别查询字典项 |

#### 2.3.4 示例数据

示例数据脚本位于 `database/02-seed-data.sql`，预置了以下数据用于系统演示：

- **管养单位** — 4条（管理所、设计院、建设公司、监理公司）
- **路线** — 3条（高速公路G101、省道S205、县道X032）
- **桥梁** — 3座（××河大桥450m、××中桥85m、××小桥25m）
- **桥梁基本状况卡片** — 3份
- **桥梁结构信息** — 9项（主梁/T梁/桥墩/支座等）
- **档案资料** — 7条（设计图纸、竣工图、验收文件等）
- **初始检查记录** — 2条（含XX河大桥和XX中桥的首次检测）
- **初始检查检测项** — 7条（拱轴线、墩台高程、材质强度、保护层厚度等）
- **定期检查记录** — 2条（XX河大桥2023定期检查、XX中桥2024定期检查）
- **部件评分** — 10条（桥面铺装/伸缩缝/主梁/桥墩/桥台/支座评分）

数据库文件已预初始化：`database/bridge_inspection.db`（可直接使用）。


# 公路桥梁初始检查信息系统 — 系统架构设计说明

## 1. 架构选型说明

### 1.1 C/S vs B/S 选型

本系统选择 **B/S（Browser/Server）架构**，理由如下：

| 因素 | B/S架构优势 |
|------|-----------|
| **部署维护** | 只需部署服务器端，客户端通过浏览器访问，无需安装额外软件，降低运维成本 |
| **多用户协作** | 检测人员、工程师、管理员可同时在不同地点通过浏览器使用系统 |
| **数据集中** | 所有桥梁检测数据集中存储在服务器，保证数据一致性和安全性 |
| **跨平台** | 支持Windows/Mac/Linux等任意操作系统，只需现代浏览器 |
| **扩展性** | 易于扩展为移动端应用或与其他系统对接 |

### 1.2 技术栈选择

| 层次 | 技术选型 | 版本 | 说明 |
|------|---------|------|------|
| **前端** | HTML5 + Bootstrap 5 | 5.3.0 | 响应式界面，支持PC和移动端 |
| **前端** | jQuery + Font Awesome | 6.0 | 交互增强与图标库 |
| **服务端** | Python Flask | 3.0+ | 轻量级Web框架，灵活高效 |
| **ORM** | Flask-SQLAlchemy | 3.1+ | 数据库ORM映射 |
| **认证** | Flask-Login | 0.6+ | 用户会话管理 |
| **数据库** | SQLite 3 | 内置 | 轻量级嵌入式数据库 |
| **版本控制** | Git | - | 代码版本管理 |

### 1.3 软件层次架构

```
┌─────────────────────────────────────────┐
│           表现层 (Presentation)          │
│   Jinja2模板 + Bootstrap 5 响应式界面    │
│   templates/  +  static/css/            │
├─────────────────────────────────────────┤
│           控制层 (Controller)            │
│   Flask Blueprint 路由分发               │
│   routes/ → auth/main/bridge/inspection │
├─────────────────────────────────────────┤
│           业务逻辑层 (Service)           │
│   桥梁档案管理 / 初始检查 / 定期检查     │
│   评分计算 / 统计查询                   │
├─────────────────────────────────────────┤
│           数据访问层 (DAO)               │
│   SQLAlchemy ORM + 模型定义             │
│   models/ → 15个模型类                  │
├─────────────────────────────────────────┤
│           数据存储层 (Data)              │
│   SQLite 数据库                         │
│   database/bridge_inspection.db         │
└─────────────────────────────────────────┘
```

## 2. 项目目录结构

```
bridge-inspection-system/
├── docs/                              # 设计文档
│   ├── 01-系统功能架构设计.md          # 功能架构设计
│   └── 02-数据模型设计.md              # 数据模型设计(概念+逻辑+物理)
│
├── database/                          # 数据库脚本
│   ├── 01-init-schema.sql             # 建表DDL + 索引 + 字典数据
│   ├── 02-seed-data.sql               # 示例数据
│   └── bridge_inspection.db           # SQLite数据库文件
│
├── src/                               # 源代码
│   ├── app.py                         # Flask应用工厂
│   ├── config.py                      # 配置模块
│   ├── run.py                         # 启动脚本
│   │
│   ├── models/
│   │   └── __init__.py                # 15个SQLAlchemy ORM模型
│   │
│   ├── routes/
│   │   ├── __init__.py
│   │   ├── auth_routes.py             # 用户认证
│   │   ├── main_routes.py             # 首页(统计仪表盘)
│   │   ├── bridge_routes.py           # 桥梁档案CRUD
│   │   └── inspection_routes.py       # 检查管理(初始+定期)
│   │
│   ├── templates/
│   │   ├── base.html                  # 基础模板(导航+页脚)
│   │   ├── login.html                 # 登录页
│   │   ├── index.html                 # 首页(统计+快速操作)
│   │   ├── bridge_list.html           # 桥梁列表
│   │   ├── bridge_detail.html         # 桥梁详情(含卡片)
│   │   ├── bridge_form.html           # 新增/编辑桥梁
│   │   ├── initial_list.html          # 初始检查记录列表
│   │   ├── initial_detail.html        # 初始检查详情+检测项
│   │   ├── initial_form.html          # 新增初始检查
│   │   ├── regular_list.html          # 定期检查记录列表
│   │   ├── regular_detail.html        # 定期检查详情+部件评分
│   │   ├── regular_form.html          # 新增定期检查
│   │   └── regular_stats.html         # 统计分析
│   │
│   └── static/
│       └── css/
│           └── style.css              # 自定义样式
│
├── backup/                            # 数据备份目录
└── README.md                          # 项目说明
```

## 3. 运行说明

### 3.1 环境要求
- Python 3.8+
- 依赖包：Flask, Flask-SQLAlchemy, Flask-Login, Werkzeug

### 3.2 安装与启动

```bash
# 安装依赖
pip install flask flask-sqlalchemy flask-login

# 进入源码目录
cd bridge-inspection-system/src

# 初始化数据库（首次运行）
python -c "from app import create_app, db; app=create_app(); app.app_context().push(); db.create_all()"

# 启动服务
python run.py

# 访问系统
# http://localhost:5000
# 默认管理员: admin / admin123
```

### 3.3 开发模式
- Debug模式默认开启（代码修改后自动重载）
- 数据库文件：`database/bridge_inspection.db`
- 端口：5000

## 4. 数据流说明

```
┌──────────┐    选择桥梁     ┌──────────────┐
│ 检测人员  │ ───────────→  │  初始检查录入  │
│          │ ←─────────── │  (初始检查表)  │
└──────────┘   记录保存     └──────┬───────┘
                                   │
                                   ▼
┌──────────┐    桥梁引用     ┌──────────────┐
│ 检测人员  │ ───────────→  │  定期检查录入  │
│          │ ←─────────── │  (部件评分表)  │
└──────────┘   记录保存     └──────┬───────┘
                                   │
                                   ▼
┌──────────┐    查询统计     ┌──────────────┐
│ 桥梁工程师 │ ───────────→  │  统计分析输出  │
│          │ ←─────────── │ (等级分布图)   │
└──────────┘   报表打印     └──────────────┘
```

## 5. 安全设计

1. **认证与授权**：基于Flask-Login的会话管理，密码使用pbkdf2哈希存储
2. **角色控制**：4种角色（admin/engineer/inspector/viewer）区分操作权限
3. **SQL注入防护**：采用SQLAlchemy ORM参数化查询
4. **XSS防护**：Jinja2模板自动转义输出内容

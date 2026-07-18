# 公路桥梁检查信息系统

本系统面向桥梁建档、初始检查和定期检查，不包含独立的养护处治业务系统。

## 核心功能

- 6种桥型、151条桥型—部位—部件关系
- 47个初检项目、220条桥型—初检项目关系
- 桥梁档案、初始检查、定期检查、病害和报告
- 系统管理员、桥梁工程师、检查人员、审核人员、查询人员五种角色门户
- 公开注册默认创建查询人员账号

## 启动

```powershell
cd bridge-inspection-system/backend
mvn.cmd -DskipTests package
java -jar target/bridge-inspection-backend-1.0.0.jar

cd bridge-inspection-system/frontend
npm.cmd run dev
```

后端：`http://localhost:8080`，前端：`http://localhost:5173`。

## 演示账号

| 账号 | 密码 | 角色 |
|---|---|---|
| admin | admin123 | 系统管理员 |
| zhang | admin123 | 桥梁工程师 |
| li | admin123 | 检查人员 |
| wang | admin123 | 审核人员 |
| zhao | admin123 | 查询人员 |

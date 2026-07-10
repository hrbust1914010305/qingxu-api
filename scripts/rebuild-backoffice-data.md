# 后台基础数据重建脚本使用文档

本目录提供两种后台基础数据重建方式：

- `rebuild-backoffice-data.ps1`：通过现有 HTTP 接口执行。
- `rebuild-backoffice-data.sql`：直接通过 PostgreSQL 执行。

如果接口方式遇到登录态、验证码、Cookie 或 Session 问题，建议使用 SQL 方式。

## 重建内容

- 保留用户 `admin2`。
- 保留系统角色 `admin`。
- 保留并创建默认用户角色 `default_user`，名称为 `默认用户`。
- 删除除 `admin2` 外的旧用户。
- 删除除 `admin`、`default_user` 外的旧业务角色。
- 删除旧业务部门，保留系统根目录和根临时部门。
- 创建 5 个业务部门、6 个业务角色、32 个内部用户。
- 32 个内部用户会显式分配业务角色；后续注册或后台创建用户未指定角色时，会自动分配 `default_user`。

## 默认用户角色

系统内置默认角色：

- 角色编码：`default_user`
- 角色名称：`默认用户`
- 用途：注册用户、后台创建用户在未指定角色时自动分配。
- 默认菜单：`Home` 首页。

该角色不计入“6 个业务角色”的统计。

## 接口方式

先预演，不会删除或创建数据：

```powershell
.\scripts\rebuild-backoffice-data.ps1 -Cookie "QINGXU_SESSION=your-session-value"
```

确认 `target/backoffice-rebuild/before.json` 后正式执行：

```powershell
.\scripts\rebuild-backoffice-data.ps1 -Cookie "QINGXU_SESSION=your-session-value" -Execute
```

如需给 6 个业务角色分配菜单：

```powershell
.\scripts\rebuild-backoffice-data.ps1 `
  -Cookie "QINGXU_SESSION=your-session-value" `
  -BusinessRoleMenuIds 1,2,3 `
  -Execute
```

## SQL 方式

脚本路径：

```text
scripts/rebuild-backoffice-data.sql
```

如果本机已安装 `psql`：

```powershell
psql "postgresql://qingxu:123456@localhost:5432/qingxu" -f .\scripts\rebuild-backoffice-data.sql
```

如果本机没有 `psql`，使用仓库里的 JDBC 执行脚本：

```powershell
.\scripts\run-sql-jdbc.ps1 `
  -Url "jdbc:postgresql://localhost:5432/qingxu" `
  -Username "qingxu" `
  -Password "123456" `
  -SqlFile ".\scripts\rebuild-backoffice-data.sql"
```

SQL 脚本会在一个事务中执行，任意一步失败都会回滚。

## 默认账号数据

新建 32 个内部用户的默认密码：

```text
Qingxu@123456
```

这些用户会设置：

- `status = ACTIVE`
- `need_password_change = TRUE`
- 每人至少 1 个部门关系
- 每人至少 1 个业务角色关系

## 验证点

执行后建议检查：

- 活跃业务用户数为 32，另有保留账号 `admin2`。
- 业务角色数为 6，另有系统角色 `admin` 和默认角色 `default_user`。
- 业务部门数为 5。
- `default_user` 存在且状态为 `ACTIVE`。
- 新注册用户或后台创建且未指定角色的用户会绑定 `default_user`。
- 用户列表按部门筛选正常。
- 角色列表能看到新增研发岗位角色。
- 部门树显示新部门结构。

## 乱码处理

脚本和文档均按 UTF-8 保存。PowerShell 查看 JSON 或文档时建议使用：

```powershell
Get-Content .\target\backoffice-rebuild\before.json -Encoding UTF8
```

如果终端仍显示乱码，优先确认终端编码：

```powershell
chcp 65001
```

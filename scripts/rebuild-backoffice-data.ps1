param(
    [string]$BaseUrl = "http://localhost:8081",
    [string]$Username = "admin2",
    [string]$Password,
    [string]$CaptchaKey,
    [string]$Captcha,
    [string]$Cookie,
    [string]$InitialPassword = "Qingxu@123456",
    [long[]]$BusinessRoleMenuIds = @(),
    [switch]$Execute
)

$ErrorActionPreference = "Stop"
$Session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$BaseUri = [Uri]$BaseUrl.TrimEnd("/")
$OutDir = Join-Path (Resolve-Path ".").Path "target\backoffice-rebuild"
$CookieHeader = $null

function U([string]$Value) {
    return [System.Text.RegularExpressions.Regex]::Unescape($Value)
}

$DeptProduct = U "\u4ea7\u54c1\u7814\u53d1\u90e8"
$DeptQa = U "\u6d4b\u8bd5\u8d28\u91cf\u90e8"
$DeptOps = U "\u8fd0\u8425\u4ea4\u4ed8\u90e8"
$DeptSales = U "\u5e02\u573a\u9500\u552e\u90e8"
$DeptAdmin = U "\u7efc\u5408\u7ba1\u7406\u90e8"

$BusinessDepartments = @(
    @{ name = $DeptProduct; sortOrder = 10; description = "Product research and technology delivery team" },
    @{ name = $DeptQa; sortOrder = 20; description = "Quality assurance, testing and release gate team" },
    @{ name = $DeptOps; sortOrder = 30; description = "Customer operations, launch delivery and support team" },
    @{ name = $DeptSales; sortOrder = 40; description = "Marketing, sales and business collaboration team" },
    @{ name = $DeptAdmin; sortOrder = 50; description = "Administration, HR, finance and general support team" }
)

$BusinessRoles = @(
    @{ code = "rd_manager"; name = (U "\u7814\u53d1\u8d1f\u8d23\u4eba"); sortOrder = 10; description = "R&D team management, technical planning and delivery quality" },
    @{ code = "backend_engineer"; name = (U "\u540e\u7aef\u7814\u53d1\u5de5\u7a0b\u5e08"); sortOrder = 20; description = "Backend services, APIs and data processing" },
    @{ code = "frontend_engineer"; name = (U "\u524d\u7aef\u7814\u53d1\u5de5\u7a0b\u5e08"); sortOrder = 30; description = "Frontend pages, components and interaction experience" },
    @{ code = "qa_engineer"; name = (U "\u6d4b\u8bd5\u5de5\u7a0b\u5e08"); sortOrder = 40; description = "Test design, quality verification and defect follow-up" },
    @{ code = "ops_specialist"; name = (U "\u8fd0\u8425\u4ea4\u4ed8\u4e13\u5458"); sortOrder = 50; description = "Customer operations, project delivery and support response" },
    @{ code = "sales_admin"; name = (U "\u9500\u552e/\u7efc\u5408\u7ba1\u7406"); sortOrder = 60; description = "Sales collaboration, customer follow-up and general administration" }
)

$DefaultUserRole = @{
    code = "default_user"
    name = (U "\u9ed8\u8ba4\u7528\u6237")
    sortOrder = 900
    description = (U "\u6ce8\u518c\u6216\u540e\u53f0\u521b\u5efa\u7528\u6237\u672a\u6307\u5b9a\u89d2\u8272\u65f6\u81ea\u52a8\u5206\u914d\u7684\u9ed8\u8ba4\u89d2\u8272")
}

$SeedUsers = @(
    @{ username = "rd001"; realname = "Lin Zhiyuan"; dept = $DeptProduct; roles = @("rd_manager") },
    @{ username = "rd002"; realname = "Zhou Mingzhe"; dept = $DeptProduct; roles = @("rd_manager") },
    @{ username = "be001"; realname = "Chen Jingxing"; dept = $DeptProduct; roles = @("backend_engineer") },
    @{ username = "be002"; realname = "Xu Wenbo"; dept = $DeptProduct; roles = @("backend_engineer") },
    @{ username = "be003"; realname = "Gao Yiming"; dept = $DeptProduct; roles = @("backend_engineer") },
    @{ username = "be004"; realname = "Luo Chengyu"; dept = $DeptProduct; roles = @("backend_engineer") },
    @{ username = "be005"; realname = "Tang Siyuan"; dept = $DeptProduct; roles = @("backend_engineer") },
    @{ username = "fe001"; realname = "Shen Ruolin"; dept = $DeptProduct; roles = @("frontend_engineer") },
    @{ username = "fe002"; realname = "Lu Qingyang"; dept = $DeptProduct; roles = @("frontend_engineer") },
    @{ username = "fe003"; realname = "Han Yutong"; dept = $DeptProduct; roles = @("frontend_engineer") },
    @{ username = "fe004"; realname = "Zhao Yanqi"; dept = $DeptProduct; roles = @("frontend_engineer") },
    @{ username = "fe005"; realname = "Song Zhixia"; dept = $DeptProduct; roles = @("frontend_engineer") },
    @{ username = "qa001"; realname = "Wang Jianing"; dept = $DeptQa; roles = @("qa_engineer") },
    @{ username = "qa002"; realname = "Li Muyang"; dept = $DeptQa; roles = @("qa_engineer") },
    @{ username = "qa003"; realname = "Zheng Anqi"; dept = $DeptQa; roles = @("qa_engineer") },
    @{ username = "qa004"; realname = "He Siqi"; dept = $DeptQa; roles = @("qa_engineer") },
    @{ username = "qa005"; realname = "Jiang Ruoxi"; dept = $DeptQa; roles = @("qa_engineer") },
    @{ username = "ops001"; realname = "Wu Yihang"; dept = $DeptOps; roles = @("ops_specialist") },
    @{ username = "ops002"; realname = "Feng Xingchen"; dept = $DeptOps; roles = @("ops_specialist") },
    @{ username = "ops003"; realname = "Ma Yutong"; dept = $DeptOps; roles = @("ops_specialist") },
    @{ username = "ops004"; realname = "Zhu Chenxi"; dept = $DeptOps; roles = @("ops_specialist") },
    @{ username = "ops005"; realname = "Hu Jiashu"; dept = $DeptOps; roles = @("ops_specialist") },
    @{ username = "ops006"; realname = "Sun Zhuoran"; dept = $DeptOps; roles = @("ops_specialist") },
    @{ username = "sales001"; realname = "Liu Zihan"; dept = $DeptSales; roles = @("sales_admin") },
    @{ username = "sales002"; realname = "Yuan Kexin"; dept = $DeptSales; roles = @("sales_admin") },
    @{ username = "sales003"; realname = "Peng Haoran"; dept = $DeptSales; roles = @("sales_admin") },
    @{ username = "sales004"; realname = "Cao Yufei"; dept = $DeptSales; roles = @("sales_admin") },
    @{ username = "sales005"; realname = "Deng Qihang"; dept = $DeptSales; roles = @("sales_admin") },
    @{ username = "adm001"; realname = "Ye Anran"; dept = $DeptAdmin; roles = @("sales_admin") },
    @{ username = "adm002"; realname = "Liang Shuyao"; dept = $DeptAdmin; roles = @("sales_admin") },
    @{ username = "adm003"; realname = "Jin Zeyu"; dept = $DeptAdmin; roles = @("sales_admin") },
    @{ username = "adm004"; realname = "Xue Zixuan"; dept = $DeptAdmin; roles = @("sales_admin") }
)

function Step([string]$Message) {
    Write-Host "==> $Message"
}

function ReadUtf8ResponseBody($Response) {
    if ($Response.RawContentStream) {
        $Response.RawContentStream.Position = 0
        $memory = New-Object System.IO.MemoryStream
        $Response.RawContentStream.CopyTo($memory)
        return [System.Text.Encoding]::UTF8.GetString($memory.ToArray())
    }
    return [string]$Response.Content
}

function ReadUtf8ErrorBody($ErrorResponse) {
    if (-not $ErrorResponse) {
        return $null
    }
    $stream = $ErrorResponse.GetResponseStream()
    if (-not $stream) {
        return $null
    }
    $memory = New-Object System.IO.MemoryStream
    $stream.CopyTo($memory)
    return [System.Text.Encoding]::UTF8.GetString($memory.ToArray())
}

function Api([string]$Method, [string]$Path, $Body = $null) {
    $headers = @{ Accept = "application/json" }
    if ($CookieHeader) {
        $headers["Cookie"] = $CookieHeader
    }

    $params = @{
        Method = $Method
        Uri = "$($BaseUri.AbsoluteUri.TrimEnd('/'))$Path"
        WebSession = $Session
        Headers = $headers
        UseBasicParsing = $true
    }
    if ($null -ne $Body) {
        $params.ContentType = "application/json; charset=utf-8"
        $params.Body = $Body | ConvertTo-Json -Depth 30 -Compress
    }
    try {
        $response = Invoke-WebRequest @params
    } catch {
        $details = $_.Exception.Message
        if ($_.Exception.Response) {
            $errorBody = ReadUtf8ErrorBody $_.Exception.Response
            if ($errorBody) {
                $details = $errorBody
            }
        }
        throw "API request failed: $Method ${Path}`n$details"
    }
    $content = ReadUtf8ResponseBody $response
    if (-not $content) {
        return $null
    }
    $json = $content | ConvertFrom-Json
    if ($null -ne $json.code -and "$($json.code)" -ne "0") {
        throw "API returned error for $Method ${Path}: [$($json.code)] $($json.message)"
    }
    return $json.data
}

function SaveSnapshot([string]$Name, $Data) {
    if (-not (Test-Path $OutDir)) {
        New-Item -ItemType Directory -Force -Path $OutDir | Out-Null
    }
    $path = Join-Path $OutDir "$Name.json"
    $Data | ConvertTo-Json -Depth 30 | Set-Content -Path $path -Encoding UTF8
    Write-Host "Snapshot saved: $path"
}

function Login {
    $script:CookieHeader = $null
    if ($Cookie) {
        $value = $Cookie
        if ($Cookie -match "QINGXU_SESSION=([^;]+)") {
            $value = $Matches[1]
        }
        $script:CookieHeader = "QINGXU_SESSION=$value"
        $Session.Cookies.Add($BaseUri, (New-Object System.Net.Cookie("QINGXU_SESSION", $value, "/", $BaseUri.Host)))
        return
    }
    if (-not $Password -or -not $CaptchaKey -or -not $Captcha) {
        throw "Provide either -Cookie 'QINGXU_SESSION=...' or -Password, -CaptchaKey and -Captcha."
    }
    Step "Login $Username"
    Api POST "/api/auth/login" @{ username = $Username; password = $Password; captchaKey = $CaptchaKey; captcha = $Captcha } | Out-Null
}

function ValidateSession {
    try {
        $currentUser = Api GET "/api/auth/current-user"
    } catch {
        throw @"
Session validation failed.

The backend returned unauthorized for the supplied QINGXU_SESSION.
Check these points:
1. Copy the cookie named QINGXU_SESSION from the backend origin used by this script: $($BaseUri.AbsoluteUri.TrimEnd('/')).
2. Make sure the admin2 browser session is still logged in and not expired.
3. If the frontend calls a different backend host or port, pass the matching -BaseUrl.
4. Copy the full cookie value, or pass the whole header fragment: QINGXU_SESSION=...

Original error:
$($_.Exception.Message)
"@
    }

    Write-Host "Authenticated as: $($currentUser.username) (id=$($currentUser.id))"
    if ($currentUser.username -ne "admin2") {
        throw "Authenticated user must be admin2, got $($currentUser.username)."
    }
}

function Users {
    return @((Api GET "/api/user?page=1&pageSize=500").records)
}

function Roles {
    return @((Api GET "/api/role/list?current=1&pageSize=500").records)
}

function DeptTree {
    return @(Api GET "/api/department/tree")
}

function FlattenDept($Nodes, [int]$Depth = 0) {
    $items = @()
    foreach ($node in @($Nodes)) {
        $items += [pscustomobject]@{
            id = [long]$node.id
            parentId = [long]$node.parentId
            name = [string]$node.name
            deptType = [string]$node.deptType
            depth = $Depth
        }
        if ($node.children) {
            $items += FlattenDept @($node.children) ($Depth + 1)
        }
    }
    return $items
}

function FirstWhere($Items, [string]$Property, $Value) {
    return @($Items | Where-Object { $_.$Property -eq $Value })[0]
}

function RootDeptId($Tree) {
    $roots = @(FlattenDept $Tree | Where-Object { $_.parentId -eq 0 -and $_.deptType -eq "DIRECTORY" })
    if ($roots.Count -lt 1) {
        throw "No root DIRECTORY department found."
    }
    return [long]$roots[0].id
}

function ResetAdmin2($AllUsers, $AllRoles) {
    $admin2 = FirstWhere $AllUsers "username" "admin2"
    $admin = FirstWhere $AllRoles "code" "admin"
    if (-not $admin2) {
        throw "admin2 user was not found."
    }
    if (-not $admin) {
        throw "admin role was not found."
    }
    Write-Host "Ensure admin2 keeps admin role."
    if ($Execute) {
        Api PUT "/api/user/roles" @{ userIds = @([long]$admin2.id); roleIds = @([long]$admin.id) } | Out-Null
        Api PUT "/api/user/$($admin2.id)" @{ deptIds = @() } | Out-Null
    }
}

function RemoveOldUsers($AllUsers) {
    $old = @($AllUsers | Where-Object { $_.username -ne "admin2" })
    if ($old.Count -eq 0) {
        Write-Host "No old users to delete."
        return
    }
    $ids = ($old | ForEach-Object { $_.id }) -join ","
    Write-Host "Users to delete: $($old.Count) ($ids)"
    if ($Execute) {
        Api DELETE "/api/user/$ids" | Out-Null
    }
}

function RemoveOldRoles($AllRoles) {
    foreach ($role in @($AllRoles | Where-Object { $_.code -ne "admin" -and $_.code -ne $DefaultUserRole.code })) {
        Write-Host "Role to delete: $($role.code) ($($role.id))"
        if ($Execute) {
            Api DELETE "/api/role/$($role.id)" | Out-Null
        }
    }
}

function EnsureDefaultUserRole {
    Write-Host "Ensure default user role: $($DefaultUserRole.code)"
    if (-not $Execute) {
        return
    }

    $roles = Roles
    $role = FirstWhere $roles "code" $DefaultUserRole.code
    if (-not $role) {
        $role = Api POST "/api/role" @{
            code = $DefaultUserRole.code
            name = $DefaultUserRole.name
            status = "ACTIVE"
            description = $DefaultUserRole.description
            remark = "Created by rebuild-backoffice-data.ps1"
            sortOrder = $DefaultUserRole.sortOrder
        }
    }

    Api PUT "/api/role/$($role.id)/menus" @{ menuIds = @(1) } | Out-Null
}

function RemoveOldDepartments($Tree) {
    $old = @(
        FlattenDept $Tree |
            Where-Object { -not ($_.parentId -eq 0 -and ($_.deptType -eq "DIRECTORY" -or $_.deptType -eq "TEMPORARY")) } |
            Sort-Object depth -Descending
    )
    foreach ($dept in $old) {
        Write-Host "Department to delete: $($dept.name) ($($dept.id))"
        if ($Execute) {
            Api DELETE "/api/department/$($dept.id)" | Out-Null
        }
    }
}

function CreateDepartments([long]$RootId) {
    $ids = @{}
    foreach ($dept in $BusinessDepartments) {
        Write-Host "Department to create: $($dept.name)"
        if ($Execute) {
            $data = Api POST "/api/department" @{
                parentId = $RootId
                name = $dept.name
                leaderId = $null
                leader = $null
                phone = $null
                email = $null
                sortOrder = $dept.sortOrder
                status = "ACTIVE"
                description = $dept.description
            }
            $ids[$dept.name] = [long]$data.id
        }
    }
    return $ids
}

function CreateRoles {
    $ids = @{}
    foreach ($role in $BusinessRoles) {
        Write-Host "Role to create: $($role.code)"
        if ($Execute) {
            $data = Api POST "/api/role" @{
                code = $role.code
                name = $role.name
                status = "ACTIVE"
                description = $role.description
                remark = "Created by rebuild-backoffice-data.ps1"
                sortOrder = $role.sortOrder
            }
            $ids[$role.code] = [long]$data.id
            if ($BusinessRoleMenuIds.Count -gt 0) {
                Api PUT "/api/role/$($data.id)/menus" @{ menuIds = @($BusinessRoleMenuIds | ForEach-Object { [long]$_ }) } | Out-Null
            }
        }
    }
    return $ids
}

function CreateUsers($DeptIds, $RoleIds) {
    $index = 1
    foreach ($user in $SeedUsers) {
        if (-not $DeptIds.ContainsKey($user.dept)) {
            throw "Missing department id: $($user.dept)"
        }
        $userRoleIds = @()
        foreach ($code in $user.roles) {
            if (-not $RoleIds.ContainsKey($code)) {
                throw "Missing role id: $code"
            }
            $userRoleIds += [long]$RoleIds[$code]
        }
        $phone = "1390001{0:D4}" -f $index
        Write-Host "User to create: $($user.username) -> $($user.dept) / $($user.roles -join ',')"
        if ($Execute) {
            Api POST "/api/user" @{
                username = $user.username
                nickname = $user.realname
                realname = $user.realname
                avatar = $null
                phone = $phone
                email = "$($user.username)@qingxu.example"
                userType = "INTERNAL"
                password = $InitialPassword
                deptIds = @([long]$DeptIds[$user.dept])
                roleIds = @($userRoleIds)
            } | Out-Null
        }
        $index++
    }
}

function VerifyImport {
    $allUsers = Users
    $allRoles = Roles
    $tree = DeptTree
    $flat = @(FlattenDept $tree)
    $businessUsers = @($allUsers | Where-Object { $_.username -ne "admin2" })
    $businessRoles = @($allRoles | Where-Object { $BusinessRoles.code -contains $_.code })
    $businessDepts = @($flat | Where-Object { $BusinessDepartments.name -contains $_.name })
    $rootTempIds = @($flat | Where-Object { $_.parentId -eq 0 -and $_.deptType -eq "TEMPORARY" } | ForEach-Object { [long]$_.id })
    $usersWithoutDept = @($businessUsers | Where-Object { -not $_.deptIds -or $_.deptIds.Count -lt 1 })
    $usersWithoutRole = @($businessUsers | Where-Object { -not $_.roleIds -or $_.roleIds.Count -lt 1 })
    $usersInRootTemp = @($businessUsers | Where-Object {
        @(@($_.deptIds | ForEach-Object { [long]$_ }) | Where-Object { $rootTempIds -contains $_ }).Count -gt 0
    })

    $summary = [pscustomobject]@{
        activeUsersExcludingAdmin2 = $businessUsers.Count
        businessRoleCount = $businessRoles.Count
        businessDepartmentCount = $businessDepts.Count
        usersWithoutDept = @($usersWithoutDept | ForEach-Object { $_.username })
        usersWithoutRole = @($usersWithoutRole | ForEach-Object { $_.username })
        usersInRootTemp = @($usersInRootTemp | ForEach-Object { $_.username })
    }
    SaveSnapshot "after" @{ summary = $summary; users = $allUsers; roles = $allRoles; departments = $tree }

    if ($summary.activeUsersExcludingAdmin2 -ne 32) { throw "Expected 32 business users, got $($summary.activeUsersExcludingAdmin2)." }
    if ($summary.businessRoleCount -ne 6) { throw "Expected 6 business roles, got $($summary.businessRoleCount)." }
    if ($summary.businessDepartmentCount -ne 5) { throw "Expected 5 business departments, got $($summary.businessDepartmentCount)." }
    if ($summary.usersWithoutDept.Count -gt 0) { throw "Users without department: $($summary.usersWithoutDept -join ', ')." }
    if ($summary.usersWithoutRole.Count -gt 0) { throw "Users without role: $($summary.usersWithoutRole -join ', ')." }
    if ($summary.usersInRootTemp.Count -gt 0) { throw "Users in root temp department: $($summary.usersInRootTemp -join ', ')." }
    Write-Host "Verification passed."
}

Step "Mode: $(if ($Execute) { 'EXECUTE' } else { 'DRY-RUN' })"
if (-not $Execute) {
    Write-Host "Dry-run only. Add -Execute to apply deletions and creations."
}

Login
ValidateSession

Step "Load before snapshot"
$beforeUsers = Users
$beforeRoles = Roles
$beforeTree = DeptTree
SaveSnapshot "before" @{ users = $beforeUsers; roles = $beforeRoles; departments = $beforeTree }

ResetAdmin2 $beforeUsers $beforeRoles
RemoveOldUsers $beforeUsers
$rolesForCleanup = if ($Execute) { Roles } else { $beforeRoles }
RemoveOldRoles $rolesForCleanup
EnsureDefaultUserRole

$treeBeforeDeptCleanup = if ($Execute) { DeptTree } else { $beforeTree }
$rootId = RootDeptId $treeBeforeDeptCleanup
RemoveOldDepartments $treeBeforeDeptCleanup

Step "Create departments, roles and users"
$deptIds = CreateDepartments $rootId
$roleIds = CreateRoles

if ($Execute) {
    CreateUsers $deptIds $roleIds
    Step "Verify import result"
    VerifyImport
} else {
    Write-Host "Dry-run planned departments: $($BusinessDepartments.Count)"
    Write-Host "Dry-run planned business roles: $($BusinessRoles.Count)"
    Write-Host "Dry-run planned users: $($SeedUsers.Count)"
}

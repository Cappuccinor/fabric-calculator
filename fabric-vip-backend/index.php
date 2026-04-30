<?php
require __DIR__ . '/config.php';
session_start();

function h(string $value): string
{
    return htmlspecialchars($value, ENT_QUOTES, 'UTF-8');
}

if (isset($_GET['logout'])) {
    session_destroy();
    header('Location: index.php');
    exit;
}

$error = '';
if ($_SERVER['REQUEST_METHOD'] === 'POST' && ($_POST['action'] ?? '') === 'login') {
    if (($_POST['user'] ?? '') === ADMIN_USER && ($_POST['pass'] ?? '') === ADMIN_PASS) {
        $_SESSION['admin'] = true;
        header('Location: index.php');
        exit;
    }
    $error = '用户名或密码错误';
}

if (empty($_SESSION['admin'])) {
    ?>
    <!doctype html>
    <html lang="zh-CN">
    <head>
        <meta charset="utf-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <title>胚布计算器会员后台</title>
        <link rel="stylesheet" href="style.css">
    </head>
    <body>
    <main class="login">
        <h1>会员后台</h1>
        <?php if ($error): ?><p class="error"><?= h($error) ?></p><?php endif; ?>
        <form method="post">
            <input type="hidden" name="action" value="login">
            <label>用户名<input name="user" autocomplete="username"></label>
            <label>密码<input name="pass" type="password" autocomplete="current-password"></label>
            <button>登录</button>
        </form>
    </main>
    </body>
    </html>
    <?php
    exit;
}

if ($_SERVER['REQUEST_METHOD'] === 'POST' && ($_POST['action'] ?? '') === 'save_user') {
    $phone = trim($_POST['phone'] ?? '');
    $name = trim($_POST['name'] ?? '');
    $password = (string)($_POST['password'] ?? '');
    $expire = trim($_POST['vip_expire_at'] ?? '');
    $note = trim($_POST['note'] ?? '');
    if ($phone !== '') {
        $expireValue = $expire !== '' ? $expire . ' 23:59:59' : null;
        if ($password !== '') {
            $hash = password_hash($password, PASSWORD_DEFAULT);
            $stmt = db()->prepare(
                "INSERT INTO users (phone, password_hash, name, vip_expire_at, note)
                 VALUES (?, ?, ?, ?, ?)
                 ON DUPLICATE KEY UPDATE password_hash = VALUES(password_hash), name = VALUES(name), vip_expire_at = VALUES(vip_expire_at), note = VALUES(note)"
            );
            $stmt->execute([$phone, $hash, $name, $expireValue, $note]);
        } else {
            $stmt = db()->prepare(
                "INSERT INTO users (phone, name, vip_expire_at, note)
                 VALUES (?, ?, ?, ?)
                 ON DUPLICATE KEY UPDATE name = VALUES(name), vip_expire_at = VALUES(vip_expire_at), note = VALUES(note)"
            );
            $stmt->execute([$phone, $name, $expireValue, $note]);
        }
    }
    header('Location: index.php');
    exit;
}

if ($_SERVER['REQUEST_METHOD'] === 'POST' && ($_POST['action'] ?? '') === 'mark_paid') {
    $orderNo = trim($_POST['order_no'] ?? '');
    if ($orderNo !== '') {
        $stmt = db()->prepare("SELECT o.*, u.vip_expire_at, p.duration_days
            FROM orders o
            JOIN users u ON u.id = o.user_id
            JOIN plans p ON p.code = o.plan_code
            WHERE o.order_no = ? AND o.status = 'pending'
            LIMIT 1");
        $stmt->execute([$orderNo]);
        $order = $stmt->fetch();
        if ($order) {
            $base = !empty($order['vip_expire_at']) && strtotime($order['vip_expire_at']) > time()
                ? strtotime($order['vip_expire_at'])
                : time();
            $newExpire = date('Y-m-d H:i:s', $base + ((int)$order['duration_days'] * 86400));
            db()->beginTransaction();
            db()->prepare("UPDATE users SET vip_expire_at = ? WHERE id = ?")->execute([$newExpire, (int)$order['user_id']]);
            db()->prepare("UPDATE orders SET status = 'paid', paid_at = NOW() WHERE order_no = ?")->execute([$orderNo]);
            db()->commit();
        }
    }
    header('Location: index.php');
    exit;
}

if ($_SERVER['REQUEST_METHOD'] === 'POST' && ($_POST['action'] ?? '') === 'renew_user') {
    $phone = trim($_POST['phone'] ?? '');
    if ($phone !== '') {
        $stmt = db()->prepare("SELECT id, vip_expire_at FROM users WHERE phone = ? LIMIT 1");
        $stmt->execute([$phone]);
        $user = $stmt->fetch();
        if ($user) {
            $base = !empty($user['vip_expire_at']) && strtotime($user['vip_expire_at']) > time()
                ? strtotime($user['vip_expire_at'])
                : time();
            $newExpire = date('Y-m-d H:i:s', $base + 365 * 86400);
            db()->prepare("UPDATE users SET vip_expire_at = ?, note = CONCAT(note, ' 续费一年') WHERE id = ?")
                ->execute([$newExpire, (int)$user['id']]);
        }
    }
    header('Location: index.php');
    exit;
}

if ($_SERVER['REQUEST_METHOD'] === 'POST' && ($_POST['action'] ?? '') === 'delete_user') {
    $phone = trim($_POST['phone'] ?? '');
    if ($phone !== '') {
        $stmt = db()->prepare("SELECT id FROM users WHERE phone = ? LIMIT 1");
        $stmt->execute([$phone]);
        $user = $stmt->fetch();
        if ($user) {
            db()->prepare("DELETE FROM orders WHERE user_id = ?")->execute([(int)$user['id']]);
            db()->prepare("DELETE FROM users WHERE id = ?")->execute([(int)$user['id']]);
        }
    }
    header('Location: index.php');
    exit;
}

$keyword = trim($_GET['q'] ?? '');
if ($keyword !== '') {
    $stmt = db()->prepare("SELECT * FROM users WHERE phone LIKE ? OR name LIKE ? ORDER BY updated_at DESC LIMIT 200");
    $like = '%' . $keyword . '%';
    $stmt->execute([$like, $like]);
    $users = $stmt->fetchAll();
} else {
    $users = db()->query("SELECT * FROM users ORDER BY updated_at DESC LIMIT 200")->fetchAll();
}
$orders = db()->query("SELECT o.*, u.phone, p.name AS plan_name
    FROM orders o
    JOIN users u ON u.id = o.user_id
    JOIN plans p ON p.code = o.plan_code
    ORDER BY o.created_at DESC
    LIMIT 100")->fetchAll();
?>
<!doctype html>
<html lang="zh-CN">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>胚布计算器会员后台</title>
    <link rel="stylesheet" href="style.css">
</head>
<body>
<main class="page">
    <header>
        <div>
            <h1>胚布计算器会员后台</h1>
            <p>手动添加客户，设置会员到期时间。</p>
        </div>
        <a href="?logout=1">退出</a>
    </header>

    <section class="panel">
        <h2>开通/修改会员</h2>
        <form method="post" class="grid">
            <input type="hidden" name="action" value="save_user">
            <label>手机号<input name="phone" required placeholder="客户手机号"></label>
            <label>登录密码<input name="password" placeholder="修改密码时填写"></label>
            <label>客户名称<input name="name" placeholder="可选"></label>
            <label>会员到期<input name="vip_expire_at" type="date"></label>
            <label>备注<input name="note" placeholder="付款记录、来源等"></label>
            <button>保存会员</button>
        </form>
    </section>

    <section class="panel">
        <form method="get" class="search">
            <input name="q" value="<?= h($keyword) ?>" placeholder="搜索手机号或名称">
            <button>搜索</button>
            <a href="index.php">全部</a>
        </form>
        <table>
            <thead>
            <tr>
                <th>手机号</th>
                <th>名称</th>
                <th>会员到期</th>
                <th>试用</th>
                <th>状态</th>
                <th>备注</th>
                <th>操作</th>
            </tr>
            </thead>
            <tbody>
            <?php foreach ($users as $user):
                $active = !empty($user['vip_expire_at']) && strtotime($user['vip_expire_at']) >= time();
                ?>
                <tr>
                    <td><?= h($user['phone']) ?></td>
                    <td><?= h($user['name']) ?></td>
                    <td><?= h($user['vip_expire_at'] ?? '') ?></td>
                    <td><?= (int)($user['trial_used'] ?? 0) ?>/5</td>
                    <td><span class="<?= $active ? 'ok' : 'off' ?>"><?= $active ? '有效' : '未开通/已过期' ?></span></td>
                    <td><?= h($user['note']) ?></td>
                    <td>
                        <button type="button" onclick="fillUser('<?= h($user['phone']) ?>','<?= h($user['name']) ?>','<?= h(substr((string)($user['vip_expire_at'] ?? ''), 0, 10)) ?>','<?= h($user['note']) ?>')">更改</button>
                        <form method="post" style="display:inline">
                            <input type="hidden" name="action" value="renew_user">
                            <input type="hidden" name="phone" value="<?= h($user['phone']) ?>">
                            <button>续费一年</button>
                        </form>
                        <form method="post" style="display:inline" onsubmit="return confirm('确定删除 <?= h($user['phone']) ?> 吗？删除后订单也会删除。')">
                            <input type="hidden" name="action" value="delete_user">
                            <input type="hidden" name="phone" value="<?= h($user['phone']) ?>">
                            <button>删除</button>
                        </form>
                    </td>
                </tr>
            <?php endforeach; ?>
            </tbody>
        </table>
    </section>

    <section class="panel">
        <h2>会员订单</h2>
        <table>
            <thead>
            <tr>
                <th>订单号</th>
                <th>手机号</th>
                <th>套餐</th>
                <th>金额</th>
                <th>状态</th>
                <th>创建时间</th>
                <th>操作</th>
            </tr>
            </thead>
            <tbody>
            <?php foreach ($orders as $order): ?>
                <tr>
                    <td><?= h($order['order_no']) ?></td>
                    <td><?= h($order['phone']) ?></td>
                    <td><?= h($order['plan_name']) ?></td>
                    <td><?= h((string)$order['amount']) ?></td>
                    <td><?= h($order['status']) ?></td>
                    <td><?= h($order['created_at']) ?></td>
                    <td>
                        <?php if ($order['status'] === 'pending'): ?>
                            <form method="post" style="display:inline">
                                <input type="hidden" name="action" value="mark_paid">
                                <input type="hidden" name="order_no" value="<?= h($order['order_no']) ?>">
                                <button>确认收款并开通</button>
                            </form>
                        <?php endif; ?>
                    </td>
                </tr>
            <?php endforeach; ?>
            </tbody>
        </table>
    </section>
</main>
<script>
function fillUser(phone, name, expire, note) {
    document.querySelector('[name="phone"]').value = phone || '';
    document.querySelector('[name="password"]').value = '';
    document.querySelector('[name="name"]').value = name || '';
    document.querySelector('[name="vip_expire_at"]').value = expire || '';
    document.querySelector('[name="note"]').value = note || '';
    window.scrollTo({ top: 0, behavior: 'smooth' });
}
</script>
</body>
</html>

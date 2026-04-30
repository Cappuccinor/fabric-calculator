<?php
require __DIR__ . '/config.php';

$sql = [
    "CREATE TABLE IF NOT EXISTS users (
        id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
        phone VARCHAR(32) NOT NULL UNIQUE,
        password_hash VARCHAR(255) NOT NULL DEFAULT '',
        trial_used INT UNSIGNED NOT NULL DEFAULT 0,
        name VARCHAR(80) NOT NULL DEFAULT '',
        vip_expire_at DATETIME NULL,
        note VARCHAR(255) NOT NULL DEFAULT '',
        created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
        updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4",
    "CREATE TABLE IF NOT EXISTS device_checks (
        id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
        user_id INT UNSIGNED NULL,
        phone VARCHAR(32) NOT NULL DEFAULT '',
        device_id VARCHAR(120) NOT NULL DEFAULT '',
        app_version VARCHAR(40) NOT NULL DEFAULT '',
        checked_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
        ip VARCHAR(64) NOT NULL DEFAULT '',
        INDEX idx_phone (phone),
        INDEX idx_checked_at (checked_at)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
];

foreach ($sql as $statement) {
    db()->exec($statement);
}

$columns = db()->query("SHOW COLUMNS FROM users")->fetchAll();
$existing = array_column($columns, 'Field');
if (!in_array('password_hash', $existing, true)) {
    db()->exec("ALTER TABLE users ADD password_hash VARCHAR(255) NOT NULL DEFAULT '' AFTER phone");
}
if (!in_array('trial_used', $existing, true)) {
    db()->exec("ALTER TABLE users ADD trial_used INT UNSIGNED NOT NULL DEFAULT 0 AFTER password_hash");
}
?>
<!doctype html>
<html lang="zh-CN">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>安装完成</title>
    <style>
        body { font-family: Arial, sans-serif; padding: 32px; background: #f7faf8; color: #17201f; }
        .box { max-width: 720px; margin: 0 auto; background: white; padding: 24px; border: 1px solid #dbe5e1; }
        code { background: #eef5f2; padding: 2px 6px; }
    </style>
</head>
<body>
<div class="box">
    <h2>会员后台数据库已安装</h2>
    <p>现在可以打开 <code>/index.php</code> 登录后台。</p>
    <p>安装确认后，建议在宝塔文件管理里删除 <code>install.php</code>。</p>
</div>
</body>
</html>

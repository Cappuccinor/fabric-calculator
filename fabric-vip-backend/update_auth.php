<?php
require __DIR__ . '/config.php';

$columns = db()->query("SHOW COLUMNS FROM users")->fetchAll();
$existing = array_column($columns, 'Field');
if (!in_array('password_hash', $existing, true)) {
    db()->exec("ALTER TABLE users ADD password_hash VARCHAR(255) NOT NULL DEFAULT '' AFTER phone");
}
if (!in_array('trial_used', $existing, true)) {
    db()->exec("ALTER TABLE users ADD trial_used INT UNSIGNED NOT NULL DEFAULT 0 AFTER password_hash");
}

db()->exec("CREATE TABLE IF NOT EXISTS password_reset_requests (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    phone VARCHAR(32) NOT NULL,
    ip VARCHAR(64) NOT NULL DEFAULT '',
    status VARCHAR(20) NOT NULL DEFAULT 'pending',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_phone (phone),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

db()->exec("CREATE TABLE IF NOT EXISTS sms_codes (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    phone VARCHAR(32) NOT NULL,
    code VARCHAR(12) NOT NULL,
    purpose VARCHAR(30) NOT NULL DEFAULT 'login',
    used TINYINT UNSIGNED NOT NULL DEFAULT 0,
    expires_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ip VARCHAR(64) NOT NULL DEFAULT '',
    INDEX idx_phone (phone),
    INDEX idx_expires_at (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

db()->exec("CREATE TABLE IF NOT EXISTS plans (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(40) NOT NULL UNIQUE,
    name VARCHAR(80) NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    duration_days INT UNSIGNED NOT NULL,
    enabled TINYINT UNSIGNED NOT NULL DEFAULT 1
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

db()->exec("CREATE TABLE IF NOT EXISTS orders (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    order_no VARCHAR(40) NOT NULL UNIQUE,
    user_id INT UNSIGNED NOT NULL,
    plan_code VARCHAR(40) NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'pending',
    pay_channel VARCHAR(30) NOT NULL DEFAULT 'manual',
    paid_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

$yearPrice = defined('PLAN_YEAR_PRICE') ? PLAN_YEAR_PRICE : 59.00;
$yearDays = defined('PLAN_YEAR_DAYS') ? PLAN_YEAR_DAYS : 365;
$stmt = db()->prepare("INSERT INTO plans (code, name, price, duration_days, enabled)
    VALUES ('year_59', '年会员', ?, ?, 1)
    ON DUPLICATE KEY UPDATE name = VALUES(name), price = VALUES(price), duration_days = VALUES(duration_days), enabled = 1");
$stmt->execute([$yearPrice, $yearDays]);
?>
<!doctype html>
<html lang="zh-CN">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>升级完成</title>
    <style>
        body { font-family: Arial, sans-serif; padding: 32px; background: #f7faf8; color: #17201f; }
        .box { max-width: 720px; margin: 0 auto; background: white; padding: 24px; border: 1px solid #dbe5e1; }
        code { background: #eef5f2; padding: 2px 6px; }
    </style>
</head>
<body>
<div class="box">
    <h2>账号密码与试用次数升级完成</h2>
    <p>现在可以删除 <code>update_auth.php</code>。</p>
</div>
</body>
</html>

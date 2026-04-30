<?php
require dirname(__DIR__) . '/config.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    json_response(['ok' => false, 'message' => 'POST only'], 405);
}

$data = request_json();
$phone = trim((string)($data['phone'] ?? ''));
$password = (string)($data['password'] ?? '');
$deviceId = trim((string)($data['deviceId'] ?? ''));
$appVersion = trim((string)($data['appVersion'] ?? ''));

if ($phone === '') {
    json_response(['ok' => false, 'message' => 'phone required'], 400);
}
if ($password === '') {
    json_response(['ok' => false, 'message' => 'password required'], 400);
}

$stmt = db()->prepare("SELECT * FROM users WHERE phone = ? LIMIT 1");
$stmt->execute([$phone]);
$user = $stmt->fetch();

if (!$user || empty($user['password_hash']) || !password_verify($password, $user['password_hash'])) {
    json_response(['ok' => false, 'message' => '手机号或密码错误'], 401);
}

$vip = false;
$expireAt = null;
$userId = null;
$userId = (int)$user['id'];
$expireAt = $user['vip_expire_at'];
$vip = !empty($expireAt) && strtotime($expireAt) >= time();
$trialRemaining = max(0, 5 - (int)$user['trial_used']);

$log = db()->prepare("INSERT INTO device_checks (user_id, phone, device_id, app_version, ip) VALUES (?, ?, ?, ?, ?)");
$log->execute([$userId, $phone, $deviceId, $appVersion, $_SERVER['REMOTE_ADDR'] ?? '']);

json_response([
    'ok' => true,
    'vip' => $vip,
    'expireAt' => $expireAt,
    'trialRemaining' => $trialRemaining,
    'serverTime' => date('Y-m-d H:i:s'),
]);

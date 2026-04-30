<?php
require dirname(__DIR__) . '/config.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    json_response(['ok' => false, 'message' => 'POST only'], 405);
}

$data = request_json();
$phone = trim((string)($data['phone'] ?? ''));
$code = trim((string)($data['code'] ?? ''));

if (!preg_match('/^1[3-9]\d{9}$/', $phone) || !preg_match('/^\d{6}$/', $code)) {
    json_response(['ok' => false, 'message' => '手机号或验证码格式不正确'], 400);
}

$stmt = db()->prepare("SELECT id FROM sms_codes WHERE phone = ? AND code = ? AND used = 0 AND expires_at >= NOW() ORDER BY id DESC LIMIT 1");
$stmt->execute([$phone, $code]);
$sms = $stmt->fetch();
if (!$sms) {
    json_response(['ok' => false, 'message' => '验证码错误或已过期'], 401);
}
db()->prepare("UPDATE sms_codes SET used = 1 WHERE id = ?")->execute([(int)$sms['id']]);

$stmt = db()->prepare("SELECT * FROM users WHERE phone = ? LIMIT 1");
$stmt->execute([$phone]);
$user = $stmt->fetch();
if (!$user) {
    $stmt = db()->prepare("INSERT INTO users (phone, trial_used, name, note) VALUES (?, 0, '', '短信注册')");
    $stmt->execute([$phone]);
    $stmt = db()->prepare("SELECT * FROM users WHERE phone = ? LIMIT 1");
    $stmt->execute([$phone]);
    $user = $stmt->fetch();
}

$vip = !empty($user['vip_expire_at']) && strtotime($user['vip_expire_at']) >= time();
$trialRemaining = max(0, 5 - (int)$user['trial_used']);

json_response([
    'ok' => true,
    'vip' => $vip,
    'expireAt' => $user['vip_expire_at'],
    'trialRemaining' => $trialRemaining,
    'serverTime' => date('Y-m-d H:i:s'),
]);

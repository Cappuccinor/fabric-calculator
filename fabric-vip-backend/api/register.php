<?php
require dirname(__DIR__) . '/config.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    json_response(['ok' => false, 'message' => 'POST only'], 405);
}

$data = request_json();
$phone = trim((string)($data['phone'] ?? ''));
$password = (string)($data['password'] ?? '');

if (!preg_match('/^1[3-9]\d{9}$/', $phone)) {
    json_response(['ok' => false, 'message' => '请输入正确的手机号'], 400);
}
if (strlen($password) < 6) {
    json_response(['ok' => false, 'message' => '密码至少 6 位'], 400);
}

$stmt = db()->prepare("SELECT id FROM users WHERE phone = ? LIMIT 1");
$stmt->execute([$phone]);
if ($stmt->fetch()) {
    json_response(['ok' => false, 'message' => '这个手机号已经注册，请直接登录'], 409);
}

$hash = password_hash($password, PASSWORD_DEFAULT);
$stmt = db()->prepare("INSERT INTO users (phone, password_hash, trial_used, name, note) VALUES (?, ?, 0, '', '用户自助注册')");
$stmt->execute([$phone, $hash]);

json_response([
    'ok' => true,
    'vip' => false,
    'expireAt' => null,
    'trialRemaining' => 5,
    'message' => '注册成功，已赠送 5 次免费试用',
]);

<?php
require dirname(__DIR__) . '/config.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    json_response(['ok' => false, 'message' => 'POST only'], 405);
}

$data = request_json();
$phone = trim((string)($data['phone'] ?? ''));
$purpose = trim((string)($data['purpose'] ?? 'login'));

if (!preg_match('/^1[3-9]\d{9}$/', $phone)) {
    json_response(['ok' => false, 'message' => '请输入正确的手机号'], 400);
}

$recent = db()->prepare("SELECT COUNT(*) AS total FROM sms_codes WHERE phone = ? AND created_at >= DATE_SUB(NOW(), INTERVAL 60 SECOND)");
$recent->execute([$phone]);
if ((int)$recent->fetch()['total'] > 0) {
    json_response(['ok' => false, 'message' => '验证码发送太频繁，请稍后再试'], 429);
}

$code = (string)random_int(100000, 999999);
$stmt = db()->prepare("INSERT INTO sms_codes (phone, code, purpose, expires_at, ip) VALUES (?, ?, ?, DATE_ADD(NOW(), INTERVAL 10 MINUTE), ?)");
$stmt->execute([$phone, $code, $purpose, $_SERVER['REMOTE_ADDR'] ?? '']);

$smsEnabled = defined('SMS_ENABLED') ? SMS_ENABLED : false;
if (!$smsEnabled) {
    json_response([
        'ok' => false,
        'message' => '短信服务还没有配置。请先在 config.php 配置阿里云短信。',
        'devCode' => $code,
    ], 503);
}

// TODO: Configure Alibaba Cloud SMS SDK/API here after SMS signature and template are approved.
json_response(['ok' => false, 'message' => '短信接口配置位已预留，请接入阿里云短信 API'], 503);

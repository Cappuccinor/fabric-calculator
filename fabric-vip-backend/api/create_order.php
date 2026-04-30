<?php
require dirname(__DIR__) . '/config.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    json_response(['ok' => false, 'message' => 'POST only'], 405);
}

$data = request_json();
$phone = trim((string)($data['phone'] ?? ''));
$code = trim((string)($data['planCode'] ?? 'year_59'));

if (!preg_match('/^1[3-9]\d{9}$/', $phone)) {
    json_response(['ok' => false, 'message' => '请先登录账号'], 400);
}

$stmt = db()->prepare("SELECT id FROM users WHERE phone = ? LIMIT 1");
$stmt->execute([$phone]);
$user = $stmt->fetch();
if (!$user) {
    json_response(['ok' => false, 'message' => '账号不存在，请先短信登录/注册'], 404);
}

$stmt = db()->prepare("SELECT * FROM plans WHERE code = ? AND enabled = 1 LIMIT 1");
$stmt->execute([$code]);
$plan = $stmt->fetch();
if (!$plan) {
    json_response(['ok' => false, 'message' => '套餐不存在'], 404);
}

$orderNo = 'FV' . date('YmdHis') . random_int(1000, 9999);
$stmt = db()->prepare("INSERT INTO orders (order_no, user_id, plan_code, amount, status, pay_channel) VALUES (?, ?, ?, ?, 'pending', 'manual')");
$stmt->execute([$orderNo, (int)$user['id'], $plan['code'], $plan['price']]);

json_response([
    'ok' => true,
    'orderNo' => $orderNo,
    'planName' => $plan['name'],
    'amount' => (float)$plan['price'],
    'message' => '订单已生成，请联系管理员付款开通',
]);

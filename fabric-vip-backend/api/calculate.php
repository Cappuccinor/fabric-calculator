<?php
require dirname(__DIR__) . '/config.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    json_response(['ok' => false, 'message' => 'POST only'], 405);
}

$data = request_json();
$phone = trim((string)($data['phone'] ?? ''));
$password = (string)($data['password'] ?? '');
if ($phone === '') {
    json_response(['ok' => false, 'message' => 'phone required'], 400);
}
if ($password === '') {
    json_response(['ok' => false, 'message' => 'password required'], 400);
}

$stmt = db()->prepare("SELECT id, password_hash, vip_expire_at, trial_used FROM users WHERE phone = ? LIMIT 1");
$stmt->execute([$phone]);
$user = $stmt->fetch();
if (!$user || empty($user['password_hash']) || !password_verify($password, $user['password_hash'])) {
    json_response(['ok' => false, 'message' => '手机号或密码错误'], 401);
}

$vip = $user && !empty($user['vip_expire_at']) && strtotime($user['vip_expire_at']) >= time();
$trialUsed = (int)$user['trial_used'];
$trialRemaining = max(0, 5 - $trialUsed);
if (!$vip && $trialRemaining <= 0) {
    json_response(['ok' => false, 'message' => '免费试用次数已用完，请开通会员'], 403);
}

function n(array $data, string $key, float $default = 0): float
{
    return isset($data[$key]) && is_numeric($data[$key]) ? (float)$data[$key] : $default;
}

function yarn(float $density, float $count, float $width, float $factor, float $price, float $tax): array
{
    if ($density <= 0 || $count <= 0 || $width <= 0 || $factor <= 0 || $price <= 0) {
        return ['weight' => 0, 'taxed' => 0, 'untaxed' => 0];
    }
    $weight = $density / $count * $width * $factor;
    $taxed = $weight * $price / 100000;
    return ['weight' => $weight, 'taxed' => $taxed, 'untaxed' => $taxed * $tax];
}

function weighted(float $total, float $w1, float $p1, float $w2, float $p2, float $w3, float $p3): float
{
    if ($total <= 0) return 0;
    return ($w1 * $p1 + $w2 * $p2 + $w3 * $p3) / $total;
}

function machine(float $fee, float $weftD, float $weft2D, float $taxed, float $untaxed): array
{
    $processing = ($weftD + $weft2D) * $fee;
    return ['taxed' => $taxed + $processing, 'untaxed' => $untaxed + $processing];
}

$width = n($data, 'greyWidth');
$finishedWidth = n($data, 'finishedWidth');
$tax = n($data, 'taxFactor', 0.96);
$factor = n($data, 'coefficient', 0.064);
$weftD = n($data, 'weftDensity');
$weft2D = n($data, 'weft2Density');

$warp = yarn(n($data, 'warpDensity'), n($data, 'warpCount'), $width, $factor, n($data, 'warpPrice'), $tax);
$weft = yarn($weftD, n($data, 'weftCount'), $width, $factor, n($data, 'weftPrice'), $tax);
$weft2 = yarn($weft2D, n($data, 'weft2Count'), $width, $factor, n($data, 'weft2Price'), $tax);

$yarnTaxed = $warp['taxed'] + $weft['taxed'] + $weft2['taxed'];
$yarnUntaxed = $warp['untaxed'] + $weft['untaxed'] + $weft2['untaxed'];
$meterWeight = ($warp['weight'] + $weft['weight'] + $weft2['weight']) * 10;
$greyGsm = $width > 0 ? $meterWeight / ($width * 2.54) * 100 : 0;
$finishedGsm = $finishedWidth > 0 ? $meterWeight / $finishedWidth * 100 : 0;
$totalYarnWeight = $warp['weight'] + $weft['weight'] + $weft2['weight'];
$hemp = weighted($totalYarnWeight, $warp['weight'], n($data, 'warpHemp'), $weft['weight'], n($data, 'weftHemp'), $weft2['weight'], n($data, 'weft2Hemp'));
$tencel = weighted($totalYarnWeight, $warp['weight'], n($data, 'warpTencel'), $weft['weight'], n($data, 'weftTencel'), $weft2['weight'], n($data, 'weft2Tencel'));
$cotton = max(0, 100 - $hemp - $tencel);

if (!$vip) {
    $update = db()->prepare("UPDATE users SET trial_used = trial_used + 1 WHERE id = ?");
    $update->execute([(int)$user['id']]);
    $trialRemaining--;
}

json_response([
    'ok' => true,
    'vip' => $vip,
    'trialRemaining' => max(0, $trialRemaining),
    'plain' => machine(n($data, 'plainFee'), $weftD, $weft2D, $yarnTaxed, $yarnUntaxed),
    'small' => machine(n($data, 'smallFee'), $weftD, $weft2D, $yarnTaxed, $yarnUntaxed),
    'large' => machine(n($data, 'largeFee'), $weftD, $weft2D, $yarnTaxed, $yarnUntaxed),
    'meterWeight' => $meterWeight,
    'greyGsm' => $greyGsm,
    'finishedGsm' => $finishedGsm,
    'cotton' => $cotton,
    'hemp' => $hemp,
    'tencel' => $tencel,
    'serverTime' => date('Y-m-d H:i:s'),
]);

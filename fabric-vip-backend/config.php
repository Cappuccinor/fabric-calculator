<?php
// Rename this file values after uploading. Keep this file private.
const DB_HOST = '127.0.0.1';
const DB_NAME = 'fabric_vip';
const DB_USER = 'fabric_vip';
const DB_PASS = 'CHANGE_DATABASE_PASSWORD';

const ADMIN_USER = 'admin';
const ADMIN_PASS = 'CHANGE_ADMIN_PASSWORD';
const OFFLINE_GRACE_DAYS = 0;

const SMS_ENABLED = false;
const SMS_ACCESS_KEY_ID = 'CHANGE_ALIYUN_ACCESS_KEY_ID';
const SMS_ACCESS_KEY_SECRET = 'CHANGE_ALIYUN_ACCESS_KEY_SECRET';
const SMS_SIGN_NAME = 'CHANGE_SMS_SIGN_NAME';
const SMS_TEMPLATE_CODE = 'CHANGE_SMS_TEMPLATE_CODE';

const PLAN_YEAR_PRICE = 59.00;
const PLAN_YEAR_DAYS = 365;
const CONTACT_TEXT = '客服微信：Cappuccinor123';

function db(): PDO
{
    static $pdo = null;
    if ($pdo instanceof PDO) {
        return $pdo;
    }
    $dsn = 'mysql:host=' . DB_HOST . ';dbname=' . DB_NAME . ';charset=utf8mb4';
    $pdo = new PDO($dsn, DB_USER, DB_PASS, [
        PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
        PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
    ]);
    return $pdo;
}

function json_response(array $data, int $status = 200): void
{
    http_response_code($status);
    header('Content-Type: application/json; charset=utf-8');
    echo json_encode($data, JSON_UNESCAPED_UNICODE);
    exit;
}

function request_json(): array
{
    $raw = file_get_contents('php://input');
    if (!$raw) {
        return [];
    }
    $data = json_decode($raw, true);
    return is_array($data) ? $data : [];
}

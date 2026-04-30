<?php
require dirname(__DIR__) . '/config.php';

$contact = defined('CONTACT_TEXT') ? CONTACT_TEXT : '请联系管理员开通会员';
json_response([
    'ok' => true,
    'contact' => $contact,
]);

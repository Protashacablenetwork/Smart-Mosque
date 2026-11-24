<?php
/**
 * Database Connection for Masjid & Society Management (InfinityFree / cPanel MySQL)
 * Update these constants with your InfinityFree MySQL database details:
 * DB_HOST: e.g. sql100.infinityfree.com
 * DB_USER: e.g. if0_38xxxxxx
 * DB_PASS: Your cPanel account password
 * DB_NAME: e.g. if0_38xxxxxx_masjid_db
 */
define('DB_HOST', 'sql300.infinityfree.com');
define('DB_USER', 'root');
define('DB_PASS', '');
define('DB_NAME', 'masjid_society_db');

try {
    $pdo = new PDO("mysql:host=" . DB_HOST . ";dbname=" . DB_NAME . ";charset=utf8mb4", DB_USER, DB_PASS, [
        PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
        PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
        PDO::ATTR_EMULATE_PREPARES => false,
    ]);
} catch (PDOException $e) {
    // Fallback error message in Bangla
    die("<h3>ডাটাবেজ সংযোগ ব্যর্থ হয়েছে! অনুগ্রহ করে db.php ফাইলে সঠিক ইনফিনিটিফ্রি (InfinityFree) মাইএসকিউএল তথ্য দিন।</h3><p>ত্রুটি: " . htmlspecialchars($e->getMessage()) . "</p>");
}
?>

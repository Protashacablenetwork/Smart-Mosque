<?php
require_once 'db.php';

// Handle POST actions
$msg = '';
$error = '';

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $action = $_POST['action'] ?? '';
    
    if ($action === 'add_payment') {
        $id = 'PAY-' . time();
        $family_id = $_POST['family_id'] ?? '';
        $amount = floatval($_POST['amount'] ?? 0);
        $payment_date = $_POST['payment_date'] ?? date('Y-m-d');
        $payment_type = $_POST['payment_type'] ?? 'DUES';
        $receipt_no = $_POST['receipt_no'] ?? ('REC-' . rand(1000,9999));
        $note = $_POST['note'] ?? '';
        
        if ($family_id && $amount > 0) {
            $stmt = $pdo->prepare("INSERT INTO payments (id, family_id, amount, payment_date, payment_type, receipt_no, note) VALUES (?, ?, ?, ?, ?, ?, ?)");
            $stmt->execute([$id, $family_id, $amount, $payment_date, $payment_type, $receipt_no, $note]);
            $msg = "সফলভাবে চাঁদা বা পেমেন্ট জমা করা হয়েছে!";
        } else {
            $error = "পরিবার এবং সঠিক পরিমাণ প্রদান করুন।";
        }
    } elseif ($action === 'add_family') {
        $id = 'FAM-' . rand(100,999);
        $family_code = $_POST['family_code'] ?? $id;
        $family_name = $_POST['family_name'] ?? '';
        $address = $_POST['address'] ?? '';
        $phone = $_POST['phone'] ?? '';
        $monthly_due = floatval($_POST['monthly_due_amount'] ?? 300);
        
        if ($family_name) {
            $stmt = $pdo->prepare("INSERT INTO families (id, family_code, family_name, address, phone, monthly_due_amount) VALUES (?, ?, ?, ?, ?, ?)");
            $stmt->execute([$id, $family_code, $family_name, $address, $phone, $monthly_due]);
            $msg = "নতুন পরিবার সফলভাবে যুক্ত করা হয়েছে!";
        } else {
            $error = "পরিবারের নাম আবশ্যক।";
        }
    }
}

// Fetch Data
$families = $pdo->query("SELECT * FROM families ORDER BY family_code ASC")->fetchAll();
$payments = $pdo->query("SELECT p.*, f.family_name, f.family_code FROM payments p JOIN families f ON p.family_id = f.id ORDER BY p.payment_date DESC")->fetchAll();
$notices = $pdo->query("SELECT * FROM notices ORDER BY date DESC")->fetchAll();

$totalCollection = array_sum(array_column($payments, 'amount'));
$totalFamiliesCount = count($families);
$selectedYear = intval($_GET['year'] ?? date('Y'));
$months = ['জানু', 'ফেব্রু', 'মার্চ', 'এপ্রিল', 'মে', 'জুন', 'জুলাই', 'আগস্ট', 'সেপ্টেম্বর', 'অক্টোবর', 'নভেম্বর', 'ডিসেম্বর'];
?>
<!DOCTYPE html>
<html lang="bn" dir="ltr">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>মসজিদ ও সমাজ ব্যবস্থাপনা - ওয়েব অ্যাপ</title>
    <!-- Bootstrap 5 CDN -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet">
    <!-- FontAwesome Icons -->
    <link href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.2/css/all.min.css" rel="stylesheet">
    <!-- Google Fonts (Noto Sans Bengali) -->
    <link href="https://fonts.googleapis.com/css2?family=Noto+Sans+Bengali:wght@400;500;600;700&display=swap" rel="stylesheet">
    <style>
        body {
            font-family: 'Noto Sans Bengali', sans-serif;
            background-color: #f4f8f5;
            color: #122019;
        }
        .navbar-custom {
            background-color: #0a7d52;
        }
        .card-custom {
            border: none;
            border-radius: 12px;
            box-shadow: 0 4px 12px rgba(10, 125, 82, 0.08);
        }
        .btn-success-custom {
            background-color: #0a7d52;
            border-color: #0a7d52;
            color: #fff;
        }
        .btn-success-custom:hover {
            background-color: #0d9d68;
            border-color: #0d9d68;
            color: #fff;
        }
        .table-responsive {
            background: #fff;
            border-radius: 12px;
            padding: 1rem;
            box-shadow: 0 2px 8px rgba(0,0,0,0.05);
        }
        .table th {
            background-color: #e7f4ed !important;
            color: #0a7d52;
            text-align: center;
            vertical-align: middle;
            font-size: 0.9rem;
        }
        .table td {
            text-align: center;
            vertical-align: middle;
            font-size: 0.9rem;
        }
        .table td:nth-child(2) {
            text-align: left;
        }
    </style>
</head>
<body>

<!-- Navbar -->
<nav class="navbar navbar-expand-lg navbar-dark navbar-custom shadow-sm">
    <div class="container">
        <a class="navbar-brand fw-bold" href="#"><i class="fa-solid fa-mosque me-2"></i> মসজিদ ও সমাজ ব্যবস্থাপনা</a>
        <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#navbarNav">
            <span class="navbar-toggler-icon"></span>
        </button>
        <div class="collapse navbar-collapse justify-content-end" id="navbarNav">
            <ul class="navbar-nav">
                <li class="nav-item"><a class="nav-link active" href="#dashboard"><i class="fa-solid fa-chart-pie me-1"></i> ড্যাশবোর্ড</a></li>
                <li class="nav-item"><a class="nav-link" href="#masikKhata"><i class="fa-solid fa-book-open me-1"></i> মাসিক চাঁদার খাতা</a></li>
                <li class="nav-item"><a class="nav-link" href="#families"><i class="fa-solid fa-users me-1"></i> পরিবার তালিকা</a></li>
                <li class="nav-item"><a class="nav-link" href="#payments"><i class="fa-solid fa-wallet me-1"></i> লেনদেন</a></li>
            </ul>
        </div>
    </div>
</nav>

<div class="container my-4">

    <?php if ($msg): ?>
        <div class="alert alert-success alert-dismissible fade show" role="alert">
            <i class="fa-solid fa-check-circle me-2"></i> <?= htmlspecialchars($msg) ?>
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        </div>
    <?php endif; ?>

    <?php if ($error): ?>
        <div class="alert alert-danger alert-dismissible fade show" role="alert">
            <i class="fa-solid fa-triangle-exclamation me-2"></i> <?= htmlspecialchars($error) ?>
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        </div>
    <?php endif; ?>

    <!-- Summary Stats -->
    <div class="row g-3 mb-4" id="dashboard">
        <div class="col-md-4">
            <div class="card card-custom bg-white p-3 border-start border-4 border-success">
                <div class="text-muted small">মোট পরিবার</div>
                <div class="fs-3 fw-bold text-success"><?= $totalFamiliesCount ?> টি</div>
            </div>
        </div>
        <div class="col-md-4">
            <div class="card card-custom bg-white p-3 border-start border-4 border-primary">
                <div class="text-muted small">মোট চাঁদা আদায়</div>
                <div class="fs-3 fw-bold text-primary">৳ <?= number_format($totalCollection, 2) ?></div>
            </div>
        </div>
        <div class="col-md-4">
            <div class="card card-custom bg-white p-3 border-start border-4 border-warning">
                <div class="text-muted small">বর্তমান সাল</div>
                <div class="fs-3 fw-bold text-dark"><?= $selectedYear ?> খ্রি:</div>
            </div>
        </div>
    </div>

    <!-- Quick Action Bar -->
    <div class="card card-custom p-3 mb-4 bg-white">
        <div class="d-flex flex-wrap gap-2 justify-content-between align-items-center">
            <h5 class="mb-0 text-success fw-bold"><i class="fa-solid fa-bolt me-2"></i> দ্রুত কাজ</h5>
            <div class="d-flex gap-2">
                <button class="btn btn-success-custom btn-sm" data-bs-toggle="modal" data-bs-target="#paymentModal"><i class="fa-solid fa-plus me-1"></i> চাঁদা/পেমেন্ট এন্ট্রি</button>
                <button class="btn btn-outline-success btn-sm" data-bs-toggle="modal" data-bs-target="#familyModal"><i class="fa-solid fa-user-plus me-1"></i> নতুন পরিবার যোগ করুন</button>
            </div>
        </div>
    </div>

    <!-- 1. MASIK CHANDA LEDGER (মাসিক চাঁদা আদায়ের খাতা) -->
    <div class="card card-custom p-4 mb-4" id="masikKhata">
        <div class="d-flex justify-content-between align-items-center mb-3 flex-wrap gap-2">
            <div>
                <h4 class="fw-bold mb-0 text-success"><i class="fa-solid fa-book me-2"></i> মাসিক চাঁদা আদায়ের খাতা: সাল <?= $selectedYear ?></h4>
                <p class="text-muted small mb-0">কোন সদস্য কোন মাসে জমা দিয়েছেন তার অফিশিয়াল রেজিস্টার খাতা</p>
            </div>
            <form method="GET" class="d-flex align-items-center gap-2">
                <label class="small fw-bold">বছর:</label>
                <select name="year" class="form-select form-select-sm" onchange="this.form.submit()">
                    <?php for($yr = 2025; $yr <= 2028; $yr++): ?>
                        <option value="<?= $yr ?>" <?= $selectedYear == $yr ? 'selected' : '' ?>><?= $yr ?></option>
                    <?php endfor; ?>
                </select>
            </form>
        </div>

        <div class="table-responsive">
            <table class="table table-bordered table-hover align-middle">
                <thead>
                    <tr>
                        <th style="width: 50px;">ক্রম</th>
                        <th>দাতার নাম</th>
                        <th style="width: 110px;">মোবাইল</th>
                        <th style="width: 90px;">মাসিক চাঁদা</th>
                        <?php foreach($months as $mName): ?>
                            <th><?= $mName ?></th>
                        <?php endforeach; ?>
                        <th style="width: 100px;">মোট</th>
                    </tr>
                </thead>
                <tbody>
                    <?php
                    $serial = 1;
                    $monthlyColumnTotals = array_fill(1, 12, 0.0);
                    $grandTotalAllFamilies = 0.0;

                    foreach($families as $fam):
                        $famId = $fam['id'];
                        // Fetch payments for this family in selected year
                        $stmt = $pdo->prepare("SELECT payment_date, amount FROM payments WHERE family_id = ? AND payment_date LIKE ?");
                        $stmt->execute([$famId, "$selectedYear%"]);
                        $famPayments = $stmt->fetchAll();

                        $paidPerMonth = array_fill(1, 12, 0.0);
                        $famTotalYear = 0.0;
                        foreach($famPayments as $fp) {
                            $mNum = intval(substr($fp['payment_date'], 5, 2));
                            if ($mNum >= 1 && $mNum <= 12) {
                                $paidPerMonth[$mNum] += floatval($fp['amount']);
                                $famTotalYear += floatval($fp['amount']);
                                $monthlyColumnTotals[$mNum] += floatval($fp['amount']);
                            }
                        }
                        $grandTotalAllFamilies += $famTotalYear;
                    ?>
                    <tr>
                        <td class="fw-bold"><?= $serial++ ?></td>
                        <td class="text-start fw-semibold"><?= htmlspecialchars($fam['family_name']) ?><br><small class="text-muted"><?= htmlspecialchars($fam['family_code']) ?></small></td>
                        <td><?= htmlspecialchars($fam['phone'] ?: '-') ?></td>
                        <td>৳<?= number_format($fam['monthly_due_amount'], 0) ?></td>
                        <?php for($m = 1; $m <= 12; $m++): ?>
                            <td>
                                <?php if($paidPerMonth[$m] > 0): ?>
                                    <span class="badge bg-success bg-opacity-10 text-success fw-bold p-1">৳<?= number_format($paidPerMonth[$m], 0) ?></span>
                                <?php else: ?>
                                    <span class="text-muted small">-</span>
                                <?php endif; ?>
                            </td>
                        <?php endfor; ?>
                        <td class="fw-bold text-success">৳<?= number_format($famTotalYear, 0) ?></td>
                    </tr>
                    <?php endforeach; if(empty($families)): ?>
                    <tr>
                        <td colspan="17" class="text-center text-muted py-4">কোনো পরিবার পাওয়া যায়নি।</td>
                    </tr>
                    <?php endif; ?>
                </tbody>
                <tfoot class="table-success fw-bold">
                    <tr>
                        <td colspan="4" class="text-end">মোট আদায়:</td>
                        <?php for($m = 1; $m <= 12; $m++): ?>
                            <td>৳<?= number_format($monthlyColumnTotals[$m], 0) ?></td>
                        <?php endfor; ?>
                        <td>৳<?= number_format($grandTotalAllFamilies, 0) ?></td>
                    </tr>
                </tfoot>
            </table>
        </div>
        <div class="mt-3 text-muted small">
            <i class="fa-solid fa-circle-info me-1"></i> নিয়ম: প্রতি মাসে চাঁদা পেলে সেই মাসের ঘরে টাকার অঙ্ক যোগ হয়। বছর শেষে ডান পাশে মোট লিখে খাতা মিলিয়ে নিন।
        </div>
    </div>

    <!-- 2. FAMILIES LIST -->
    <div class="card card-custom p-4 mb-4" id="families">
        <h4 class="fw-bold mb-3 text-success"><i class="fa-solid fa-users me-2"></i> নিবন্ধিত পরিবারসমূহ</h4>
        <div class="row g-3">
            <?php foreach($families as $fam): ?>
            <div class="col-md-4">
                <div class="card border p-3 h-100 shadow-sm">
                    <div class="d-flex justify-content-between align-items-start mb-2">
                        <h5 class="fw-bold mb-0"><?= htmlspecialchars($fam['family_name']) ?></h5>
                        <span class="badge bg-success"><?= htmlspecialchars($fam['family_code']) ?></span>
                    </div>
                    <p class="text-muted small mb-1"><i class="fa-solid fa-location-dot me-1"></i> <?= htmlspecialchars($fam['address'] ?: 'ঠিকানা নাই') ?></p>
                    <p class="text-muted small mb-2"><i class="fa-solid fa-phone me-1"></i> <?= htmlspecialchars($fam['phone'] ?: 'মোবাইল নাই') ?></p>
                    <div class="mt-auto pt-2 border-top d-flex justify-content-between align-items-center">
                        <span class="small text-secondary">মাসিক চাঁদা: <strong>৳<?= number_format($fam['monthly_due_amount'], 0) ?></strong></span>
                    </div>
                </div>
            </div>
            <?php endforeach; ?>
        </div>
    </div>

    <!-- 3. RECENT PAYMENTS -->
    <div class="card card-custom p-4 mb-4" id="payments">
        <h4 class="fw-bold mb-3 text-success"><i class="fa-solid fa-wallet me-2"></i> সাম্প্রতিক লেনদেন ও চাঁদা আদায়</h4>
        <div class="table-responsive">
            <table class="table table-striped align-middle">
                <thead>
                    <tr>
                        <th>তারিখ</th>
                        <th>পরিবার</th>
                        <th>রসিদ নং</th>
                        <th>খাত</th>
                        <th>পরিমাণ</th>
                        <th>নোট</th>
                    </tr>
                </thead>
                <tbody>
                    <?php foreach($payments as $pay): ?>
                    <tr>
                        <td><?= htmlspecialchars($pay['payment_date']) ?></td>
                        <td class="fw-semibold"><?= htmlspecialchars($pay['family_name']) ?> <small class="text-muted">(<?= htmlspecialchars($pay['family_code']) ?>)</small></td>
                        <td><code><?= htmlspecialchars($pay['receipt_no']) ?></code></td>
                        <td><span class="badge bg-secondary"><?= htmlspecialchars($pay['payment_type']) ?></span></td>
                        <td class="fw-bold text-success">৳<?= number_format($pay['amount'], 2) ?></td>
                        <td><?= htmlspecialchars($pay['note'] ?: '-') ?></td>
                    </tr>
                    <?php endforeach; if(empty($payments)): ?>
                    <tr><td colspan="6" class="text-center text-muted">কোনো লেনদেন নেই।</td></tr>
                    <?php endif; ?>
                </tbody>
            </table>
        </div>
    </div>

</div>

<!-- Footer -->
<footer class="text-center py-4 text-muted small bg-white border-top">
    <p class="mb-0">মসজিদ ও সমাজ ব্যবস্থাপনা সফটওয়্যার • ইনফিনিটিফ্রি (InfinityFree) ও লোকাল সার্ভার kompatible</p>
</footer>

<!-- Modals -->
<!-- Payment Modal -->
<div class="modal fade" id="paymentModal" tabindex="-1">
    <div class="modal-dialog">
        <form method="POST" class="modal-content">
            <input type="hidden" name="action" value="add_payment">
            <div class="modal-header bg-success text-white">
                <h5 class="modal-title"><i class="fa-solid fa-coins me-2"></i> চাঁদা বা পেমেন্ট এন্ট্রি</h5>
                <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
            </div>
            <div class="modal-body">
                <div class="mb-3">
                    <label class="form-label fw-bold">পরিবার নির্বাচন করুন *</label>
                    <select name="family_id" class="form-select" required>
                        <option value="">-- পরিবার বা দাতা সিলেক্ট করুন --</option>
                        <?php foreach($families as $fam): ?>
                            <option value="<?= $fam['id'] ?>"><?= htmlspecialchars($fam['family_name']) ?> (<?= htmlspecialchars($fam['family_code']) ?>)</option>
                        <?php endforeach; ?>
                    </select>
                </div>
                <div class="mb-3">
                    <label class="form-label fw-bold">পরিমাণ (টাকা) *</label>
                    <input type="number" step="0.01" name="amount" class="form-control" value="300" required>
                </div>
                <div class="mb-3">
                    <label class="form-label fw-bold">তারিখ *</label>
                    <input type="date" name="payment_date" class="form-control" value="<?= date('Y-m-d') ?>" required>
                </div>
                <div class="mb-3">
                    <label class="form-label fw-bold">পেমেন্টের ধরণ</label>
                    <select name="payment_type" class="form-select">
                        <option value="DUES">মাসিক চাঁদা (DUES)</option>
                        <option value="DONATION">সাধারণ অনুদান (DONATION)</option>
                        <option value="ZAKAT">যাকাত (ZAKAT)</option>
                    </select>
                </div>
                <div class="mb-3">
                    <label class="form-label fw-bold">রসিদ নম্বর</label>
                    <input type="text" name="receipt_no" class="form-control" value="REC-<?= rand(100,999) ?>">
                </div>
                <div class="mb-3">
                    <label class="form-label fw-bold">নোট / বিবরণ</label>
                    <input type="text" name="note" class="form-control" placeholder="যেমন: জানুয়ারি ও ফেব্রুয়ারি মাসের চাঁদা">
                </div>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">বাতিল</button>
                <button type="submit" class="btn btn-success-custom">জমা দিন</button>
            </div>
        </form>
    </div>
</div>

<!-- Family Modal -->
<div class="modal fade" id="familyModal" tabindex="-1">
    <div class="modal-dialog">
        <form method="POST" class="modal-content">
            <input type="hidden" name="action" value="add_family">
            <div class="modal-header bg-success text-white">
                <h5 class="modal-title"><i class="fa-solid fa-user-plus me-2"></i> নতুন পরিবার যোগ করুন</h5>
                <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
            </div>
            <div class="modal-body">
                <div class="mb-3">
                    <label class="form-label fw-bold">পরিবারের নাম / প্রধানের নাম *</label>
                    <input type="text" name="family_name" class="form-control" placeholder="যেমন: আব্দুল মালেক পরিবার" required>
                </div>
                <div class="mb-3">
                    <label class="form-label fw-bold">পরিবার কোড</label>
                    <input type="text" name="family_code" class="form-control" value="FAM-<?= rand(100,999) ?>">
                </div>
                <div class="mb-3">
                    <label class="form-label fw-bold">মোবাইল নম্বর</label>
                    <input type="text" name="phone" class="form-control" placeholder="017xxxxxxxx">
                </div>
                <div class="mb-3">
                    <label class="form-label fw-bold">ঠিকানা</label>
                    <input type="text" name="address" class="form-control" placeholder="গ্রাম / পাড়া / মহল্লা">
                </div>
                <div class="mb-3">
                    <label class="form-label fw-bold">মাসিক নির্ধারিত চাঁদা (টাকা)</label>
                    <input type="number" name="monthly_due_amount" class="form-control" value="300">
                </div>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">বাতিল</button>
                <button type="submit" class="btn btn-success-custom">পরিবার সংরক্ষণ করুন</button>
            </div>
        </form>
    </div>
</div>

<!-- Bootstrap JS -->
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>

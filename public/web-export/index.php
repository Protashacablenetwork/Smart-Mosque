<?php
/**
 * ==================================================================
 * মসজিদ ও সমাজ ব্যবস্থাপনা - হুবহু অ্যান্ড্রয়েড অ্যাপ ডিজাইন (ফায়ারবেস ক্লাউড)
 * ==================================================================
 */
?>
<!DOCTYPE html>
<html lang="bn" dir="ltr">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
    <title>দত্ত পাড়া জামে মসজিদ ও সমাজ ব্যবস্থাপনা</title>
    <!-- Bootstrap 5 CDN -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet">
    <!-- FontAwesome Icons -->
    <link href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.2/css/all.min.css" rel="stylesheet">
    <!-- Google Fonts (Noto Sans Bengali) -->
    <link href="https://fonts.googleapis.com/css2?family=Noto+Sans+Bengali:wght@400;500;600;700&display=swap" rel="stylesheet">
    <style>
        :root {
            --green: #0a7d52;
            --green-light: #e7f4ed;
            --gold: #b8862a;
            --bg: #f1f5f2;
            --ink: #122019;
        }
        body {
            font-family: 'Noto Sans Bengali', sans-serif;
            background-color: var(--bg);
            color: var(--ink);
            margin: 0;
            padding-bottom: 70px;
        }
        /* Mobile App Container Frame for Web */
        .app-container {
            max-width: 600px;
            margin: 0 auto;
            background: #f4f8f5;
            min-height: 100vh;
            position: relative;
            box-shadow: 0 0 20px rgba(0,0,0,0.08);
        }
        /* Top App Header */
        .app-header {
            background-color: var(--green);
            color: white;
            padding: 16px;
            border-bottom-left-radius: 24px;
            border-bottom-right-radius: 24px;
        }
        /* Cards */
        .card-custom {
            border: none;
            border-radius: 16px;
            box-shadow: 0 2px 10px rgba(10,125,82,0.06);
            background: #ffffff;
        }
        /* Feature Grid Item */
        .feature-card {
            background: #ffffff;
            border-radius: 14px;
            padding: 14px 8px;
            text-align: center;
            border: 1px solid #e3ebe6;
            transition: all 0.2s;
            cursor: pointer;
            height: 100%;
        }
        .feature-card:hover {
            border-color: var(--green);
            transform: translateY(-2px);
        }
        .feature-icon {
            width: 44px;
            height: 44px;
            border-radius: 12px;
            background: var(--green-light);
            color: var(--green);
            display: inline-flex;
            align-items: center;
            justify-content: center;
            font-size: 18px;
            margin-bottom: 8px;
        }
        /* Bottom Navigation Bar */
        .bottom-nav {
            position: fixed;
            bottom: 0;
            left: 0;
            right: 0;
            max-width: 600px;
            margin: 0 auto;
            background: #0a7d52;
            display: flex;
            justify-content: space-around;
            padding: 8px 0;
            border-top-left-radius: 20px;
            border-top-right-radius: 20px;
            box-shadow: 0 -4px 16px rgba(0,0,0,0.1);
            z-index: 1000;
        }
        .bottom-nav-item {
            color: rgba(255,255,255,0.7);
            text-align: center;
            font-size: 11px;
            text-decoration: none;
            flex: 1;
            transition: color 0.2s;
        }
        .bottom-nav-item i {
            font-size: 18px;
            display: block;
            margin-bottom: 2px;
        }
        .bottom-nav-item.active, .bottom-nav-item:hover {
            color: #ffca28;
        }
        .table th { background-color: var(--green-light) !important; color: var(--green); text-align: center; font-size: 0.85rem; }
        .table td { text-align: center; font-size: 0.85rem; vertical-align: middle; }
        .table td:nth-child(2) { text-align: left; }
    </style>
</head>
<body>

<div class="app-container">

    <!-- Top App Header -->
    <div class="app-header">
        <div class="d-flex justify-content-between align-items-center mb-2">
            <div class="d-flex align-items-center gap-2">
                <button class="btn btn-sm text-white p-0" data-bs-toggle="offcanvas" data-bs-target="#sidebarMenu"><i class="fa-solid fa-bars fs-4"></i></button>
                <h5 class="mb-0 fw-bold"><i class="fa-solid fa-mosque me-1"></i> দত্ত পাড়া জামে মসজিদ...</h5>
            </div>
            <div class="d-flex gap-2">
                <button class="btn btn-sm btn-light rounded-circle text-success shadow-sm" style="width: 36px; height: 36px;"><i class="fa-solid fa-store"></i></button>
                <button class="btn btn-sm btn-light rounded-circle text-success shadow-sm" style="width: 36px; height: 36px;"><i class="fa-solid fa-bell"></i></button>
            </div>
        </div>
        <div class="small opacity-75">আসসালামু আলাইকুম, Md Riad</div>
    </div>

    <!-- Main Content Area -->
    <div class="p-3" id="mainContentArea">

        <!-- 1. HOME VIEW (Default App Home) -->
        <div id="viewHome" class="app-view">
            <!-- Weather & Prayer Countdown Card -->
            <div class="card card-custom p-3 mb-3 text-white" style="background: linear-gradient(135deg, #0a7d52 0%, #114c35 100%);">
                <div class="d-flex justify-content-between align-items-center mb-2">
                    <div>
                        <div class="small opacity-75">২৪ নভেম্বর, ২০২৬ • ৯ অগ্রহায়ণ</div>
                        <div class="small opacity-75">১৪ জমাদিউস সানি, ১৪৪৮ হিজরি</div>
                    </div>
                    <div class="text-end">
                        <div class="fw-bold"><i class="fa-solid fa-sun text-warning me-1"></i> Sherpur</div>
                        <div class="small">১৯° clear sky</div>
                    </div>
                </div>
                <hr class="my-2 opacity-25">
                <div class="row align-items-center">
                    <div class="col-7">
                        <div class="small opacity-75">পরবর্তী জামাত</div>
                        <div class="fs-4 fw-bold">ফজর (৫:১৪ পূর্বাহ্ণ)</div>
                    </div>
                    <div class="col-5 text-end">
                        <span class="badge bg-warning text-dark fw-bold px-2 py-1">বাকি: ৫:৪৯:৩৯</span>
                    </div>
                </div>
            </div>

            <!-- Notice Banner -->
            <div class="card card-custom p-3 mb-3 border-start border-4 border-warning">
                <div class="d-flex align-items-center justify-content-between">
                    <div class="d-flex align-items-center gap-2">
                        <i class="fa-solid fa-bullhorn text-warning fs-4"></i>
                        <div>
                            <h6 class="fw-bold mb-0">নোটিশ এবং ইভেন্ট</h6>
                            <small class="text-muted" id="latestNoticeText">আসন্ন জুম্মার বয়ান ও বিশেষ দোয়া...</small>
                        </div>
                    </div>
                    <button class="btn btn-sm btn-link text-success fw-bold text-decoration-none" onclick="switchView('notices')">সব দেখুন <i class="fa-solid fa-arrow-right"></i></button>
                </div>
            </div>

            <!-- Iman & Amal Section -->
            <div class="card card-custom p-3 mb-3">
                <div class="d-flex justify-content-between align-items-center mb-3">
                    <h6 class="fw-bold text-success mb-0"><i class="fa-solid fa-star me-1"></i> ঈমান ও আমল</h6>
                    <span class="badge bg-success bg-opacity-10 text-success">আমল ট্র্যাকার ০%</span>
                </div>
                <div class="row g-2">
                    <div class="col-3"><div class="feature-card" onclick="switchView('ledger')"><div class="feature-icon"><i class="fa-solid fa-book"></i></div><div class="small fw-semibold">খাতা</div></div></div>
                    <div class="col-3"><div class="feature-card" onclick="switchView('families')"><div class="feature-icon"><i class="fa-solid fa-users"></i></div><div class="small fw-semibold">পরিবার</div></div></div>
                    <div class="col-3"><div class="feature-card" onclick="switchView('qurbani')"><div class="feature-icon"><i class="fa-solid fa-cow"></i></div><div class="small fw-semibold">কুরবানি</div></div></div>
                    <div class="col-3"><div class="feature-card" onclick="switchView('dashboard')"><div class="feature-icon"><i class="fa-solid fa-wallet"></i></div><div class="small fw-semibold">হিসাব</div></div></div>
                </div>
            </div>

            <!-- Admin Features Grid -->
            <div class="card card-custom p-3 mb-3">
                <h6 class="fw-bold text-success mb-3"><i class="fa-solid fa-grid-2 me-1"></i> অ্যাডমিন ফিচার্স</h6>
                <div class="row g-2">
                    <div class="col-3"><div class="feature-card" onclick="switchView('families')"><div class="feature-icon"><i class="fa-solid fa-user-group"></i></div><div class="small fw-semibold">সদস্যবৃন্দ</div></div></div>
                    <div class="col-3"><div class="feature-card" onclick="switchView('ledger')"><div class="feature-icon"><i class="fa-solid fa-money-bill-wave"></i></div><div class="small fw-semibold">মাসিক চাঁদা</div></div></div>
                    <div class="col-3"><div class="feature-card" onclick="switchView('dashboard')"><div class="feature-icon"><i class="fa-solid fa-chart-line"></i></div><div class="small fw-semibold">রিপোর্ট</div></div></div>
                    <div class="col-3"><div class="feature-card" onclick="switchView('notices')"><div class="feature-icon"><i class="fa-solid fa-bell"></i></div><div class="small fw-semibold">নোটিশ</div></div></div>
                </div>
            </div>
        </div>

        <!-- 2. DASHBOARD VIEW (হিসাব ও ড্যাশবোর্ড) -->
        <div id="viewDashboard" class="app-view d-none">
            <h4 class="fw-bold text-success mb-3"><i class="fa-solid fa-chart-pie me-2"></i> আর্থিক ড্যাশবোর্ড ও হিসাব</h4>
            <div class="row g-3 mb-4">
                <div class="col-6">
                    <div class="card card-custom p-3 border-start border-4 border-success">
                        <div class="text-muted small">মোট পরিবার</div>
                        <div class="fs-4 fw-bold text-success" id="statFamilies">লোড হচ্ছে...</div>
                    </div>
                </div>
                <div class="col-6">
                    <div class="card card-custom p-3 border-start border-4 border-primary">
                        <div class="text-muted small">মোট চাঁদা আদায়</div>
                        <div class="fs-4 fw-bold text-primary" id="statCollection">লোড হচ্ছে...</div>
                    </div>
                </div>
            </div>

            <div class="card card-custom p-3 mb-3">
                <div class="d-flex justify-content-between align-items-center mb-3">
                    <h5 class="fw-bold text-success mb-0"><i class="fa-solid fa-wallet me-2"></i> সাম্প্রতিক লেনদেন</h5>
                    <button class="btn btn-sm btn-success-custom" data-bs-toggle="modal" data-bs-target="#paymentModal"><i class="fa-solid fa-plus"></i> জমা</button>
                </div>
                <div class="table-responsive">
                    <table class="table table-striped align-middle">
                        <thead><tr><th>তারিখ</th><th>পরিবার</th><th>পরিমাণ</th></tr></thead>
                        <tbody id="recentTransactionsBody"><tr><td colspan="3" class="text-center text-muted">লোড হচ্ছে...</td></tr></tbody>
                    </table>
                </div>
            </div>
        </div>

        <!-- 3. LEDGER VIEW (মাসিক চাঁদা আদায়ের খাতা) -->
        <div id="viewLedger" class="app-view d-none">
            <div class="card card-custom p-3 mb-3">
                <div class="d-flex justify-content-between align-items-center mb-2">
                    <h5 class="fw-bold text-success mb-0"><i class="fa-solid fa-book me-2"></i> মাসিক চাঁদার খাতা</h5>
                    <select id="ledgerYearSelect" class="form-select form-select-sm w-auto" onchange="renderApp()">
                        <option value="2025">2025</option>
                        <option value="2026" selected>2026</option>
                        <option value="2027">2027</option>
                    </select>
                </div>
                <div class="table-responsive">
                    <table class="table table-bordered table-hover align-middle">
                        <thead>
                            <tr>
                                <th>ক্রম</th><th>দাতার নাম</th><th>মাসিক</th>
                                <th>জানু</th><th>ফেব্রু</th><th>মার্চ</th><th>এপ্রিল</th><th>মে</th><th>জুন</th>
                                <th>জুলাই</th><th>আগস্ট</th><th>সেপ্টেম্বর</th><th>অক্টোবর</th><th>নভেম্বর</th><th>ডিসেম্বর</th>
                                <th>মোট</th>
                            </tr>
                        </thead>
                        <tbody id="ledgerTableBody"><tr><td colspan="16" class="text-center text-muted">লোড হচ্ছে...</td></tr></tbody>
                    </table>
                </div>
            </div>
        </div>

        <!-- 4. FAMILIES VIEW (পরিবার) -->
        <div id="viewFamilies" class="app-view d-none">
            <div class="d-flex justify-content-between align-items-center mb-3">
                <h4 class="fw-bold text-success mb-0"><i class="fa-solid fa-users me-2"></i> পরিবার তালিকা</h4>
                <button class="btn btn-sm btn-success-custom" data-bs-toggle="modal" data-bs-target="#familyModal"><i class="fa-solid fa-plus"></i> যোগ করুন</button>
            </div>
            <div class="row g-3" id="familiesGrid"><div class="col-12 text-center text-muted">লোড হচ্ছে...</div></div>
        </div>

        <!-- 5. QURBANI & NOTICES VIEW (আমার মসজিদ) -->
        <div id="viewMasjid" class="app-view d-none">
            <h4 class="fw-bold text-success mb-3"><i class="fa-solid fa-mosque me-2"></i> আমার মসজিদ ও কুরবানি</h4>
            <div class="card card-custom p-3 mb-3">
                <h6 class="fw-bold text-success mb-2">কুরবানি পশু তালিকা</h6>
                <div id="qurbaniCowsGrid"><div class="text-muted small">লোড হচ্ছে...</div></div>
            </div>
        </div>

    </div>

    <!-- Bottom Navigation Bar (Matching Android App) -->
    <div class="bottom-nav">
        <a href="#" class="bottom-nav-item" onclick="switchView('settings')"><i class="fa-solid fa-gear"></i>সেটিংস</a>
        <a href="#" class="bottom-nav-item" onclick="switchView('dashboard')"><i class="fa-solid fa-chart-pie"></i>ড্যাশবোর্ড</a>
        <a href="#" class="bottom-nav-item active" onclick="switchView('home')"><i class="fa-solid fa-house"></i>হোম</a>
        <a href="#" class="bottom-nav-item" onclick="switchView('ledger')"><i class="fa-solid fa-wallet"></i>হিসাব</a>
        <a href="#" class="bottom-nav-item" onclick="switchView('masjid')"><i class="fa-solid fa-mosque"></i>আমার মসজিদ</a>
    </div>

</div>

<!-- Sidebar Drawer -->
<div class="offcanvas offcanvas-start" tabindex="-1" id="sidebarMenu">
    <div class="offcanvas-header bg-success text-white">
        <h5 class="offcanvas-title fw-bold"><i class="fa-solid fa-mosque me-2"></i> মসজিদ ও সমাজ</h5>
        <button type="button" class="btn-close btn-close-white" data-bs-dismiss="offcanvas"></button>
    </div>
    <div class="offcanvas-body">
        <div class="text-center mb-4">
            <div class="bg-success text-white rounded-circle d-inline-flex align-items-center justify-content-center fw-bold fs-3 mb-2" style="width: 64px; height: 64px;">M</div>
            <h5 class="fw-bold mb-0">Md Riad</h5>
            <small class="text-muted">mdriad@gmail.com</small>
        </div>
        <div class="list-group list-group-flush">
            <button class="list-group-item list-group-item-action" data-bs-dismiss="offcanvas" onclick="switchView('home')"><i class="fa-solid fa-house me-2 text-success"></i> হোম</button>
            <button class="list-group-item list-group-item-action" data-bs-dismiss="offcanvas" onclick="switchView('dashboard')"><i class="fa-solid fa-chart-pie me-2 text-success"></i> ড্যাশবোর্ড</button>
            <button class="list-group-item list-group-item-action" data-bs-dismiss="offcanvas" onclick="switchView('ledger')"><i class="fa-solid fa-book me-2 text-success"></i> মাসিক চাঁদার খাতা</button>
            <button class="list-group-item list-group-item-action" data-bs-dismiss="offcanvas" onclick="switchView('families')"><i class="fa-solid fa-users me-2 text-success"></i> পরিবার তালিকা</button>
            <button class="list-group-item list-group-item-action text-danger" onclick="alert('লগআউট সফল!')"><i class="fa-solid fa-right-from-bracket me-2"></i> লগ-আউট</button>
        </div>
    </div>
</div>

<!-- Modals -->
<div class="modal fade" id="paymentModal" tabindex="-1">
    <div class="modal-dialog">
        <form id="paymentForm" class="modal-content" onsubmit="handlePaymentSubmit(event)">
            <div class="modal-header bg-success text-white">
                <h5 class="modal-title"><i class="fa-solid fa-coins me-2"></i> চাঁদা জমা দিন</h5>
                <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
            </div>
            <div class="modal-body">
                <div class="mb-3"><label class="form-label fw-bold">পরিবার *</label><select id="payFamilySelect" class="form-select" required><option value="">-- সিলেক্ট করুন --</option></select></div>
                <div class="mb-3"><label class="form-label fw-bold">পরিমাণ (৳) *</label><input type="number" id="payAmount" class="form-control" value="300" required></div>
                <div class="mb-3"><label class="form-label fw-bold">তারিখ *</label><input type="date" id="payDate" class="form-control" required></div>
                <div class="mb-3"><label class="form-label fw-bold">নোট</label><input type="text" id="payNote" class="form-control" placeholder="যেমন: জানুয়ারি চাঁদা"></div>
            </div>
            <div class="modal-footer"><button type="button" class="btn btn-secondary" data-bs-dismiss="modal">বাতিল</button><button type="submit" class="btn btn-success-custom">জমা দিন</button></div>
        </form>
    </div>
</div>

<div class="modal fade" id="familyModal" tabindex="-1">
    <div class="modal-dialog">
        <form id="familyForm" class="modal-content" onsubmit="handleFamilySubmit(event)">
            <div class="modal-header bg-success text-white">
                <h5 class="modal-title"><i class="fa-solid fa-user-plus me-2"></i> নতুন পরিবার</h5>
                <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
            </div>
            <div class="modal-body">
                <div class="mb-3"><label class="form-label fw-bold">পরিবারের নাম *</label><input type="text" id="famName" class="form-control" required></div>
                <div class="mb-3"><label class="form-label fw-bold">মোবাইল</label><input type="text" id="famPhone" class="form-control"></div>
                <div class="mb-3"><label class="form-label fw-bold">ঠিকানা</label><input type="text" id="famAddress" class="form-control"></div>
                <div class="mb-3"><label class="form-label fw-bold">মাসিক চাঁদা (৳)</label><input type="number" id="famDue" class="form-control" value="300"></div>
            </div>
            <div class="modal-footer"><button type="button" class="btn btn-secondary" data-bs-dismiss="modal">বাতিল</button><button type="submit" class="btn btn-success-custom">সংরক্ষণ</button></div>
        </form>
    </div>
</div>

<!-- Firebase SDKs -->
<script type="module">
    import { initializeApp } from "https://www.gstatic.com/firebasejs/10.8.0/firebase-app.js";
    import { getFirestore, collection, getDocs, addDoc, doc, setDoc } from "https://www.gstatic.com/firebasejs/10.8.0/firebase-firestore.js";

    const firebaseConfig = {
        apiKey: "AIzaSyBjthQBdWVcKvdfru_M_4iIwV4wQm3_DLw",
        projectId: "gen-lang-client-0609088306",
        storageBucket: "gen-lang-client-0609088306.firebasestorage.app",
        messagingSenderId: "739614097113",
        appId: "1:739614097113:web:masjidapp"
    };

    const app = initializeApp(firebaseConfig);
    const db = getFirestore(app);

    window.db = db;
    window.firebaseCollections = { collection, getDocs, addDoc, doc, setDoc };

    document.getElementById('payDate').valueAsDate = new Date();
    window.loadFirebaseData();
</script>

<script>
    let globalFamilies = [];
    let globalPayments = [];
    let globalNotices = [];
    let globalCows = [];

    window.switchView = function(viewName) {
        document.querySelectorAll('.app-view').forEach(el => el.classList.add('d-none'));
        const target = document.getElementById('view' + viewName.charAt(0).toUpperCase() + viewName.slice(1));
        if (target) target.classList.remove('d-none');

        // Update bottom nav active state
        document.querySelectorAll('.bottom-nav-item').forEach(el => el.classList.remove('active'));
        event && event.currentTarget && event.currentTarget.classList.add('active');
        window.scrollTo(0, 0);
    };

    window.loadFirebaseData = async function() {
        try {
            const { collection, getDocs } = window.firebaseCollections;
            const db = window.db;

            const famSnap = await getDocs(collection(db, "families"));
            globalFamilies = [];
            famSnap.forEach(doc => { globalFamilies.push({ id: doc.id, ...doc.data() }); });

            if (globalFamilies.length === 0) {
                await seedDefaultData();
                return;
            }

            const paySnap = await getDocs(collection(db, "payments"));
            globalPayments = [];
            paySnap.forEach(doc => { globalPayments.push({ id: doc.id, ...doc.data() }); });

            const notSnap = await getDocs(collection(db, "notices"));
            globalNotices = [];
            notSnap.forEach(doc => { globalNotices.push({ id: doc.id, ...doc.data() }); });

            const cowSnap = await getDocs(collection(db, "qurbani_cows"));
            globalCows = [];
            cowSnap.forEach(doc => { globalCows.push({ id: doc.id, ...doc.data() }); });

            renderApp();
        } catch (err) {
            console.error(err);
        }
    };

    async function seedDefaultData() {
        const { setDoc, doc, collection, addDoc } = window.firebaseCollections;
        const db = window.db;

        const defaultFamilies = [
            { id: 'FAM-001', family_code: 'FAM-001', family_name: 'মুহাম্মদ রিয়াদ পরিবার', address: 'দত্ত পাড়া, শেরপুর', phone: '01711223344', monthly_due_amount: 500 },
            { id: 'FAM-002', family_code: 'FAM-002', family_name: 'আব্দুল করিম পরিবার', address: 'উত্তর পাড়া, শেরপুর', phone: '01822334455', monthly_due_amount: 400 },
            { id: 'FAM-003', family_code: 'FAM-003', family_name: 'হাফিজুর রহমান পরিবার', address: 'মধ্য বাজার, শেরপুর', phone: '01933445566', monthly_due_amount: 300 }
        ];
        for (const fam of defaultFamilies) { await setDoc(doc(db, "families", fam.id), fam); }

        const defaultPayments = [
            { family_id: 'FAM-001', amount: 500, payment_date: '2026-01-10', payment_type: 'DUES', receipt_no: 'REC-001', note: 'জানুয়ারি চাঁদা' },
            { family_id: 'FAM-001', amount: 500, payment_date: '2026-02-12', payment_type: 'DUES', receipt_no: 'REC-002', note: 'ফেব্রুয়ারি চাঁদা' }
        ];
        for (const pay of defaultPayments) { await addDoc(collection(db, "payments"), pay); }

        const defaultNotices = [{ title: 'আসন্ন জুম্মার বয়ান', content: 'আগামী জুম্মার নামাজে বিশেষ দোয়া হবে।', date: '২০২৬-১১-২৪' }];
        for (const not of defaultNotices) { await addDoc(collection(db, "notices"), not); }

        const defaultCows = [{ year: 2026, cow_name: 'গরু #১ (লাল বাহাদুর)', total_shares: 7, total_cost: 140000 }];
        for (const cow of defaultCows) { await addDoc(collection(db, "qurbani_cows"), cow); }

        window.loadFirebaseData();
    }

    window.renderApp = function() {
        const selectedYear = document.getElementById('ledgerYearSelect').value;

        // Populate family select
        const famSelect = document.getElementById('payFamilySelect');
        famSelect.innerHTML = '<option value="">-- সিলেক্ট করুন --</option>';
        globalFamilies.forEach(fam => {
            famSelect.innerHTML += `<option value="${fam.id}">${fam.family_name}</option>`;
        });

        // Families grid
        const grid = document.getElementById('familiesGrid');
        grid.innerHTML = '';
        globalFamilies.forEach(fam => {
            grid.innerHTML += `
                <div class="col-md-6">
                    <div class="card border p-3 h-100 shadow-sm">
                        <h6 class="fw-bold mb-1">${fam.family_name}</h6>
                        <p class="text-muted small mb-1"><i class="fa-solid fa-location-dot me-1"></i> ${fam.address || 'ঠিকানা নাই'}</p>
                        <p class="text-muted small mb-0"><i class="fa-solid fa-phone me-1"></i> ${fam.phone || 'মোবাইল নাই'}</p>
                    </div>
                </div>
            `;
        });

        // Ledger table
        const tbody = document.getElementById('ledgerTableBody');
        tbody.innerHTML = '';
        let grandTotal = 0;

        globalFamilies.forEach((fam, index) => {
            let paidMonths = Array(13).fill(0);
            let famTotalYear = 0;

            globalPayments.forEach(pay => {
                if (pay.family_id === fam.id && pay.payment_date && pay.payment_date.startsWith(selectedYear)) {
                    let m = parseInt(pay.payment_date.substring(5, 7), 10);
                    if (m >= 1 && m <= 12) {
                        let amt = parseFloat(pay.amount) || 0;
                        paidMonths[m] += amt;
                        famTotalYear += amt;
                    }
                }
            });

            grandTotal += famTotalYear;

            let monthsHtml = '';
            for (let m = 1; m <= 12; m++) {
                monthsHtml += `<td>${paidMonths[m] > 0 ? '৳' + paidMonths[m] : '-'}</td>`;
            }

            tbody.innerHTML += `
                <tr>
                    <td class="fw-bold">${index + 1}</td>
                    <td class="text-start fw-semibold">${fam.family_name}</td>
                    <td>৳${fam.monthly_due_amount || 300}</td>
                    ${monthsHtml}
                    <td class="fw-bold text-success">৳${famTotalYear}</td>
                </tr>
            `;
        });

        // Recent transactions
        const txBody = document.getElementById('recentTransactionsBody');
        txBody.innerHTML = '';
        globalPayments.slice(0, 5).forEach(pay => {
            const fam = globalFamilies.find(f => f.id === pay.family_id) || { family_name: 'অজানা' };
            txBody.innerHTML += `<tr><td>${pay.payment_date}</td><td>${fam.family_name}</td><td class="fw-bold text-success">৳${pay.amount}</td></tr>`;
        });

        // Qurbani cows
        const cowGrid = document.getElementById('qurbaniCowsGrid');
        cowGrid.innerHTML = '';
        globalCows.forEach(cow => {
            cowGrid.innerHTML += `<div class="p-2 border rounded mb-2 bg-light"><div class="fw-bold text-success">${cow.cow_name}</div><small class="text-muted">অংশ: ${cow.total_shares} টি | খরচ: ৳${cow.total_cost}</small></div>`;
        });

        // Stats
        document.getElementById('statFamilies').innerText = globalFamilies.length + ' টি';
        document.getElementById('statCollection').innerText = '৳ ' + grandTotal.toLocaleString();
        if (globalNotices.length > 0) {
            document.getElementById('latestNoticeText').innerText = globalNotices[0].title;
        }
    };

    window.handlePaymentSubmit = async function(e) {
        e.preventDefault();
        const { collection, addDoc } = window.firebaseCollections;
        const db = window.db;
        const data = {
            family_id: document.getElementById('payFamilySelect').value,
            amount: parseFloat(document.getElementById('payAmount').value) || 0,
            payment_date: document.getElementById('payDate').value,
            payment_type: 'DUES',
            receipt_no: 'REC-' + Math.floor(1000 + Math.random() * 9000),
            note: document.getElementById('payNote').value
        };
        await addDoc(collection(db, "payments"), data);
        bootstrap.Modal.getInstance(document.getElementById('paymentModal')).hide();
        document.getElementById('paymentForm').reset();
        document.getElementById('payDate').valueAsDate = new Date();
        loadFirebaseData();
        alert("সফলভাবে চাঁদা জমা হয়েছে!");
    };

    window.handleFamilySubmit = async function(e) {
        e.preventDefault();
        const { setDoc, doc } = window.firebaseCollections;
        const db = window.db;
        const famId = 'FAM-' + Math.floor(100 + Math.random() * 900);
        const data = {
            id: famId,
            family_code: famId,
            family_name: document.getElementById('famName').value,
            phone: document.getElementById('famPhone').value,
            address: document.getElementById('famAddress').value,
            monthly_due_amount: parseFloat(document.getElementById('famDue').value) || 300
        };
        await setDoc(doc(db, "families", famId), data);
        bootstrap.Modal.getInstance(document.getElementById('familyModal')).hide();
        document.getElementById('familyForm').reset();
        loadFirebaseData();
        alert("পরিবার সংরক্ষণ করা হয়েছে!");
    };
</script>
<!-- Bootstrap JS -->
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>

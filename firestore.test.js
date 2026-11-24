const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ADMIN_UID = "admin_ali";
const ADMIN_EMAIL = "aliakbor1624@gmail.com";
const ACCOUNTANT_UID = "acc_hasan";
const ACCOUNTANT_EMAIL = "hasan@example.com";
const MANAGER_UID = "mgr_tariq";
const MANAGER_EMAIL = "tariq@example.com";
const VIEWER_UID = "view_nabil";
const VIEWER_EMAIL = "nabil@example.com";
const MEMBER_UID = "member_karim";
const MEMBER_EMAIL = "karim@example.com";
const OTHER_MEMBER_UID = "member_salim";
const OTHER_MEMBER_EMAIL = "salim@example.com";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function adminContext() {
  return testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  });
}

function spoofedEmailUnverifiedContext() {
  return testEnv.authenticatedContext("spoof_uid", {
    email: ADMIN_EMAIL,
    email_verified: false,
  });
}

function ctxFor(uid, email) {
  return testEnv.authenticatedContext(uid, {
    email,
    email_verified: true,
  });
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated users can read public_summary, mosque_schedule, notices ONLY; cannot write or access private collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertSucceeds(unauthDb.collection("public_summary").doc("overview").get());
  await assertSucceeds(unauthDb.collection("mosque_schedule").doc("main").get());
  await assertSucceeds(unauthDb.collection("notices").get());
  await assertSucceeds(unauthDb.collection("events").get());

  // Cannot read private/PII collections
  await assertFails(unauthDb.collection("families").get());
  await assertFails(unauthDb.collection("members").get());
  await assertFails(unauthDb.collection("payments").get());
  await assertFails(unauthDb.collection("donations").get());
  await assertFails(unauthDb.collection("expenses").get());
  await assertFails(unauthDb.collection("bank_accounts").get());
  await assertFails(unauthDb.collection("budgets").get());
  await assertFails(unauthDb.collection("meetings").get());
  await assertFails(unauthDb.collection("committees").get());
  await assertFails(unauthDb.collection("assets").get());
  await assertFails(unauthDb.collection("documents").get());
  await assertFails(unauthDb.collection("welfare_records").get());
  await assertFails(unauthDb.collection("complaints").get());
  await assertFails(unauthDb.collection("audit_logs").get());

  // Cannot write anywhere
  await assertFails(
    unauthDb.collection("mosque_schedule").doc("main").set({
      mosqueName: "Hacked Masjid",
      fajrTime: "05:00",
      dhuhrTime: "13:15",
      asrTime: "16:30",
      maghribTime: "17:45",
      ishaTime: "19:30",
      jumuahTime: "13:30",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Email spoofing with unverified email_verified=false is denied admin access", async () => {
  const spoofDb = spoofedEmailUnverifiedContext().firestore();
  await assertFails(spoofDb.collection("families").get());
  await assertFails(
    spoofDb.collection("profiles").doc("spoof_uid").set({
      userId: "spoof_uid",
      fullName: "Spoof",
      email: ADMIN_EMAIL,
      role: "super_admin",
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Member Portal isolation: linked member sees ONLY own family, members, payments, distributions, and own complaints", async () => {
  const adminDb = adminContext().firestore();
  const memberDb = ctxFor(MEMBER_UID, MEMBER_EMAIL).firestore();
  const otherDb = ctxFor(OTHER_MEMBER_UID, OTHER_MEMBER_EMAIL).firestore();
  const past = new Date(Date.now() - 10000);

  // Admin creates two families (one linked to MEMBER_EMAIL, one to OTHER_MEMBER_EMAIL)
  await assertSucceeds(
    adminDb.collection("families").doc("fam_karim").set({
      familyName: "Karim Family",
      phone: "+8801711000011",
      familyCode: "FAM-011",
      linkedEmail: MEMBER_EMAIL,
      active: true,
      createdBy: ADMIN_UID,
      createdAt: past,
      updatedAt: past,
    })
  );
  await assertSucceeds(
    adminDb.collection("families").doc("fam_salim").set({
      familyName: "Salim Family",
      phone: "+8801711000022",
      familyCode: "FAM-022",
      linkedEmail: OTHER_MEMBER_EMAIL,
      active: true,
      createdBy: ADMIN_UID,
      createdAt: past,
      updatedAt: past,
    })
  );
  await assertSucceeds(
    adminDb.collection("members").doc("mem_k1").set({
      familyId: "fam_karim",
      familyName: "Karim Family",
      memberName: "Karim Uddin",
      head: true,
      active: true,
      createdBy: ADMIN_UID,
      createdAt: past,
      updatedAt: past,
    })
  );
  await assertSucceeds(
    adminDb.collection("members").doc("mem_s1").set({
      familyId: "fam_salim",
      familyName: "Salim Family",
      memberName: "Salim Mia",
      head: true,
      active: true,
      createdBy: ADMIN_UID,
      createdAt: past,
      updatedAt: past,
    })
  );
  await assertSucceeds(
    adminDb.collection("payments").doc("pay_k1").set({
      familyId: "fam_karim",
      familyName: "Karim Family",
      amount: 500,
      paymentMethod: "bkash",
      receiptNo: "RCP-K1",
      paymentDate: "2026-11-20",
      createdBy: ADMIN_UID,
      createdAt: past,
      updatedAt: past,
    })
  );

  // Member creates own profile as 'member'
  await assertSucceeds(
    memberDb.collection("profiles").doc(MEMBER_UID).set({
      userId: MEMBER_UID,
      fullName: "Karim",
      email: MEMBER_EMAIL,
      role: "member",
      createdAt: past,
      updatedAt: past,
    })
  );
  // Cannot self-promote
  await assertFails(
    memberDb.collection("profiles").doc(MEMBER_UID).update({
      role: "accountant",
      updatedAt: new Date(Date.now() - 500),
    })
  );
  // CAN link own profile to fam_karim because fam_karim.linkedEmail == MEMBER_EMAIL
  await assertSucceeds(
    memberDb.collection("profiles").doc(MEMBER_UID).update({
      linkedFamilyId: "fam_karim",
      updatedAt: new Date(Date.now() - 400),
    })
  );
  // CANNOT link own profile to fam_salim
  await assertFails(
    memberDb.collection("profiles").doc(MEMBER_UID).update({
      linkedFamilyId: "fam_salim",
      updatedAt: new Date(Date.now() - 300),
    })
  );

  // Member queries own linked family + members + payments
  await assertSucceeds(
    memberDb.collection("families").where("linkedEmail", "==", MEMBER_EMAIL).get()
  );
  await assertSucceeds(memberDb.collection("families").doc("fam_karim").get());
  await assertSucceeds(
    memberDb.collection("members").where("familyId", "==", "fam_karim").get()
  );
  await assertSucceeds(
    memberDb.collection("payments").where("familyId", "==", "fam_karim").get()
  );

  // Member CANNOT read other families or unfiltered collections
  await assertFails(memberDb.collection("families").doc("fam_salim").get());
  await assertFails(memberDb.collection("families").get());
  await assertFails(
    memberDb.collection("members").where("familyId", "==", "fam_salim").get()
  );
  await assertFails(memberDb.collection("donations").get());
  await assertFails(memberDb.collection("bank_accounts").get());

  // Member can submit complaint, read own, cannot read other's or update status
  await assertSucceeds(
    memberDb.collection("complaints").doc("cmp_1").set({
      authorUid: MEMBER_UID,
      authorName: "Karim",
      familyId: "fam_karim",
      subject: "Wudu area water pressure",
      message: "Please check tap #3 in wudu khana.",
      status: "open",
      createdAt: past,
      updatedAt: past,
    })
  );
  await assertSucceeds(
    memberDb.collection("complaints").where("authorUid", "==", MEMBER_UID).get()
  );
  await assertFails(
    memberDb.collection("complaints").doc("cmp_1").update({
      status: "resolved",
      updatedAt: new Date(Date.now() - 200),
    })
  );
  await assertFails(
    otherDb.collection("complaints").doc("cmp_1").get()
  );
});

test("Granular RBAC: Accountant vs Manager vs Viewer permissions", async () => {
  const adminDb = adminContext().firestore();
  const accDb = ctxFor(ACCOUNTANT_UID, ACCOUNTANT_EMAIL).firestore();
  const mgrDb = ctxFor(MANAGER_UID, MANAGER_EMAIL).firestore();
  const viewDb = ctxFor(VIEWER_UID, VIEWER_EMAIL).firestore();
  const past = new Date(Date.now() - 10000);

  // Assign profiles via admin
  for (const [uid, email, role, name] of [
    [ACCOUNTANT_UID, ACCOUNTANT_EMAIL, "accountant", "Hasan Accountant"],
    [MANAGER_UID, MANAGER_EMAIL, "manager", "Tariq Manager"],
    [VIEWER_UID, VIEWER_EMAIL, "viewer", "Nabil Viewer"],
  ]) {
    await accDb; // noop
    await assertSucceeds(
      ctxFor(uid, email).firestore().collection("profiles").doc(uid).set({
        userId: uid,
        fullName: name,
        email,
        role: "member",
        createdAt: past,
        updatedAt: past,
      })
    );
    await assertSucceeds(
      adminDb.collection("profiles").doc(uid).update({
        role,
        updatedAt: new Date(Date.now() - 5000),
      })
    );
  }

  // Manager CAN update mosque_schedule and manage families, meetings, notices, qurbani
  await assertSucceeds(
    mgrDb.collection("mosque_schedule").doc("main").set({
      mosqueName: "Baitul Aman Jame Masjid",
      fajrTime: "05:15 AM",
      dhuhrTime: "01:15 PM",
      asrTime: "04:15 PM",
      maghribTime: "05:30 PM",
      ishaTime: "07:30 PM",
      jumuahTime: "01:30 PM",
      khutbahTopic: "Importance of Zakat",
      imamName: "Maulana Yusuf",
      updatedBy: MANAGER_UID,
      updatedAt: past,
    })
  );
  await assertSucceeds(
    mgrDb.collection("meetings").doc("mtg_1").set({
      title: "Monthly Committee Meeting",
      meetingDate: "2026-11-28",
      decisions: "Approve winter blanket distribution",
      assignedTo: "Tariq Manager",
      status: "scheduled",
      createdBy: MANAGER_UID,
      createdAt: past,
      updatedAt: past,
    })
  );
  // Manager CANNOT create bank_accounts or donations
  await assertFails(
    mgrDb.collection("bank_accounts").doc("acc_1").set({
      accountName: "Main Cash",
      accountType: "cash",
      balance: 10000,
      active: true,
      createdBy: MANAGER_UID,
      createdAt: past,
      updatedAt: past,
    })
  );

  // Accountant CAN manage bank_accounts, donations, budgets, expenses
  await assertSucceeds(
    accDb.collection("bank_accounts").doc("acc_1").set({
      accountName: "Main Cash Box",
      accountType: "cash",
      balance: 25000,
      active: true,
      createdBy: ACCOUNTANT_UID,
      createdAt: past,
      updatedAt: past,
    })
  );
  await assertSucceeds(
    accDb.collection("donations").doc("don_1").set({
      donorName: "Anonymous",
      anonymous: true,
      fundType: "zakat",
      amount: 5000,
      paymentMethod: "bkash",
      transactionRef: "TRX998877",
      accountId: "acc_1",
      accountName: "Main Cash Box",
      receiptNo: "DON-202611-001",
      donationDate: "2026-11-24",
      createdBy: ACCOUNTANT_UID,
      createdAt: past,
      updatedAt: past,
    })
  );
  await assertSucceeds(
    accDb.collection("budgets").doc("bdg_1").set({
      category: "Utilities & Maintenance",
      periodLabel: "2026-11",
      budgetAmount: 12000,
      createdBy: ACCOUNTANT_UID,
      createdAt: past,
      updatedAt: past,
    })
  );
  // Accountant CANNOT edit mosque_schedule or meetings
  await assertFails(
    accDb.collection("meetings").doc("mtg_2").set({
      title: "Unauthorized",
      meetingDate: "2026-11-29",
      decisions: "None",
      status: "scheduled",
      createdBy: ACCOUNTANT_UID,
      createdAt: past,
      updatedAt: past,
    })
  );

  // Viewer CAN read staff collections, CANNOT write anywhere
  await assertSucceeds(viewDb.collection("bank_accounts").get());
  await assertSucceeds(viewDb.collection("donations").get());
  await assertSucceeds(viewDb.collection("meetings").get());
  await assertFails(
    viewDb.collection("expenses").doc("exp_v").set({
      title: "Hack",
      category: "Other",
      amount: 10,
      expenseDate: "2026-11-24",
      createdBy: VIEWER_UID,
      createdAt: past,
      updatedAt: past,
    })
  );

  // Welfare & Audit Log with beforeSummary/afterSummary
  await assertSucceeds(
    mgrDb.collection("welfare_records").doc("wel_1").set({
      recordType: "burial",
      beneficiaryName: "Late Haji Malek",
      plotNumber: "Row B - Plot 14",
      amount: 0,
      recordDate: "2026-11-24",
      note: "Graveyard allocation approved",
      createdBy: MANAGER_UID,
      createdAt: past,
      updatedAt: past,
    })
  );
  await assertSucceeds(
    accDb.collection("audit_logs").doc("log_1").set({
      userId: ACCOUNTANT_UID,
      userEmail: ACCOUNTANT_EMAIL,
      action: "RECORD_DONATION",
      tableName: "donations",
      recordId: "don_1",
      summary: "Zakat 5000",
      beforeSummary: "Balance: 25000",
      afterSummary: "Balance: 30000",
      createdAt: past,
    })
  );
});

test("New Mosque Governance collections: sunriseTime, meeting location/agenda, committees, events, assets, documents", async () => {
  const adminDb = adminContext().firestore();
  const accDb = ctxFor(ACCOUNTANT_UID, ACCOUNTANT_EMAIL).firestore();
  const mgrDb = ctxFor(MANAGER_UID, MANAGER_EMAIL).firestore();
  const viewDb = ctxFor(VIEWER_UID, VIEWER_EMAIL).firestore();
  const memberDb = ctxFor(MEMBER_UID, MEMBER_EMAIL).firestore();
  const past = new Date(Date.now() - 10000);

  for (const [uid, email, role, name] of [
    [ACCOUNTANT_UID, ACCOUNTANT_EMAIL, "accountant", "Hasan Accountant"],
    [MANAGER_UID, MANAGER_EMAIL, "manager", "Tariq Manager"],
    [VIEWER_UID, VIEWER_EMAIL, "viewer", "Nabil Viewer"],
    [MEMBER_UID, MEMBER_EMAIL, "member", "Karim Member"],
  ]) {
    await assertSucceeds(
      ctxFor(uid, email).firestore().collection("profiles").doc(uid).set({
        userId: uid,
        fullName: name,
        email,
        role: "member",
        createdAt: past,
        updatedAt: past,
      })
    );
    if (role !== "member") {
      await assertSucceeds(
        adminDb.collection("profiles").doc(uid).update({
          role,
          updatedAt: new Date(Date.now() - 5000),
        })
      );
    }
  }

  // Manager saves mosque_schedule with sunriseTime & meeting with location + agenda
  await assertSucceeds(
    mgrDb.collection("mosque_schedule").doc("main").set({
      mosqueName: "Baitul Aman",
      fajrTime: "05:10",
      sunriseTime: "06:22",
      dhuhrTime: "13:15",
      asrTime: "16:15",
      maghribTime: "17:30",
      ishaTime: "19:30",
      jumuahTime: "13:30",
      updatedBy: MANAGER_UID,
      updatedAt: past,
    })
  );
  await assertSucceeds(
    mgrDb.collection("meetings").doc("mtg_ext").set({
      title: "Ramadan Preparation",
      meetingDate: "2026-12-01",
      meetingTime: "20:00",
      location: "Mosque Hall",
      agenda: "Taraweeh schedule and Sehri arrangements",
      decisions: "Finalize Imam roster",
      status: "scheduled",
      createdBy: MANAGER_UID,
      createdAt: past,
      updatedAt: past,
    })
  );

  // Committees: ops manager can create/update/delete, staff reader can read, accountant/member cannot write
  await assertSucceeds(
    mgrDb.collection("committees").doc("com_1").set({
      name: "Haji Abdur Rahman",
      role: "President",
      phone: "01711000001",
      term: "2026-2028",
      active: true,
      createdBy: MANAGER_UID,
      createdAt: past,
      updatedAt: past,
    })
  );
  await assertSucceeds(viewDb.collection("committees").get());
  await assertFails(memberDb.collection("committees").get());
  await assertFails(
    accDb.collection("committees").doc("com_2").set({
      name: "Unauthorized",
      role: "Member",
      active: true,
      createdBy: ACCOUNTANT_UID,
      createdAt: past,
      updatedAt: past,
    })
  );

  // Events: ops manager writes, public/member can read, invalid enum fails
  await assertSucceeds(
    mgrDb.collection("events").doc("evt_1").set({
      title: "Monthly Tafsir Mahfil",
      eventDate: "2026-12-05",
      eventTime: "19:30",
      location: "Main Prayer Hall",
      eventType: "religious",
      description: "Open community tafsir session after Isha.",
      rsvpEnabled: true,
      createdBy: MANAGER_UID,
      createdAt: past,
      updatedAt: past,
    })
  );
  await assertSucceeds(memberDb.collection("events").get());
  await assertFails(
    mgrDb.collection("events").doc("evt_bad").set({
      title: "Bad Event",
      eventDate: "2026-12-05",
      eventType: "invalid_type",
      createdBy: MANAGER_UID,
      createdAt: past,
      updatedAt: past,
    })
  );

  // Assets & Documents: staff writer creates/updates, staff reader reads, member cannot read, full admin only deletes
  await assertSucceeds(
    accDb.collection("assets").doc("ast_1").set({
      name: "Sound System Amplifier",
      category: "Electronics",
      value: 18500,
      purchaseDate: "2025-05-10",
      condition: "good",
      location: "Mimbar Room",
      createdBy: ACCOUNTANT_UID,
      createdAt: past,
      updatedAt: past,
    })
  );
  await assertFails(
    accDb.collection("assets").doc("ast_bad").set({
      name: "Negative Asset",
      category: "Other",
      value: -50,
      condition: "good",
      createdBy: ACCOUNTANT_UID,
      createdAt: past,
      updatedAt: past,
    })
  );
  await assertSucceeds(viewDb.collection("assets").get());
  await assertFails(memberDb.collection("assets").get());
  await assertFails(accDb.collection("assets").doc("ast_1").delete());
  await assertSucceeds(adminDb.collection("assets").doc("ast_1").delete());

  await assertSucceeds(
    mgrDb.collection("documents").doc("doc_1").set({
      title: "Waqf Deed Copy",
      category: "Legal",
      docDate: "2024-01-15",
      fileName: "waqf_deed_2024.pdf",
      note: "Stored in mosque office cabinet",
      createdBy: MANAGER_UID,
      createdAt: past,
      updatedAt: past,
    })
  );
  await assertSucceeds(viewDb.collection("documents").get());
  await assertFails(memberDb.collection("documents").get());
  await assertFails(mgrDb.collection("documents").doc("doc_1").delete());
  await assertSucceeds(adminDb.collection("documents").doc("doc_1").delete());
});

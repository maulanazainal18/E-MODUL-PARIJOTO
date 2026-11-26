const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ADMIN_UID = "admin_001";
const ADMIN_EMAIL = "maulanazainal18@gmail.com";
const GURU_UID = "guru_001";
const SISWA_A_UID = "siswa_a_001";
const SISWA_B_UID = "siswa_b_002";

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

function guruContext() {
  return testEnv.authenticatedContext(GURU_UID, {
    email: "guru@sman1muria.sch.id",
    email_verified: true,
  });
}

function siswaContext(uid = SISWA_A_UID) {
  return testEnv.authenticatedContext(uid, {
    email: `${uid}@siswa.sman1muria.sch.id`,
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

test("Unauthenticated users: denied all reads and writes", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(SISWA_A_UID).get());
  await assertFails(unauthDb.collection("classes").where("teacherUid", "==", GURU_UID).get());
  await assertFails(unauthDb.collection("modules").where("published", "==", true).get());
  await assertFails(unauthDb.collection("grades").where("studentUid", "==", SISWA_A_UID).get());
});

test("Users RBAC: siswa cannot self-escalate to ADMIN or read other users PII", async () => {
  const now = new Date(Date.now() - 2000);
  const siswaADb = siswaContext(SISWA_A_UID).firestore();
  const siswaBDb = siswaContext(SISWA_B_UID).firestore();

  // Siswa A cannot self-register as ADMIN
  await assertFails(
    siswaADb.collection("users").doc(SISWA_A_UID).set({
      uid: SISWA_A_UID,
      nama: "Hacker",
      email: "hacker@test.com",
      role: "ADMIN",
      accountStatus: "ACTIVE",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Siswa A can register as SISWA
  await assertSucceeds(
    siswaADb.collection("users").doc(SISWA_A_UID).set({
      uid: SISWA_A_UID,
      nama: "Andi Pratama",
      email: "andi@siswa.sch.id",
      role: "SISWA",
      accountStatus: "ACTIVE",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Siswa A cannot escalate own role via update
  await assertFails(
    siswaADb.collection("users").doc(SISWA_A_UID).update({
      role: "GURU",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Siswa A can update allowed profile fields (nama, classId)
  await assertSucceeds(
    siswaADb.collection("users").doc(SISWA_A_UID).update({
      nama: "Andi Muria",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Siswa B cannot read Siswa A's PII document
  await assertFails(siswaBDb.collection("users").doc(SISWA_A_UID).get());

  // Spoofed unverified admin email cannot list all users
  const spoofDb = testEnv.authenticatedContext("spoof_1", {
    email: ADMIN_EMAIL,
    email_verified: false,
  }).firestore();
  await assertFails(spoofDb.collection("users").get());

  // Verified bootstrapped admin can list users and promote Guru
  const adminDb = adminContext().firestore();
  await assertSucceeds(adminDb.collection("users").get());
  await assertSucceeds(
    adminDb.collection("users").doc(SISWA_A_UID).update({
      role: "GURU",
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Classes, ClassCodes & Enrollments: teacher creates, siswa joins via code, cross-user isolated", async () => {
  const now = new Date(Date.now() - 5000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("users").doc(GURU_UID).set({
      uid: GURU_UID,
      nama: "Ahzami, S.Pd., Gr.",
      email: "guru@sman1muria.sch.id",
      role: "GURU",
      accountStatus: "ACTIVE",
      createdAt: now,
      updatedAt: now,
    });
  });

  const guruDb = guruContext().firestore();
  const siswaADb = siswaContext(SISWA_A_UID).firestore();
  const siswaBDb = siswaContext(SISWA_B_UID).firestore();

  // Siswa cannot create a class
  await assertFails(
    siswaADb.collection("classes").doc("cls_fake").set({
      teacherUid: SISWA_A_UID,
      teacherName: "Siswa",
      name: "Kelas Palsu",
      gradeLevel: "X SMA",
      phase: "E",
      classCode: "FAKE11",
      academicYear: "2026/2027",
      subjectId: "subj_bio_x",
      archived: false,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Guru creates class and class_code
  await assertSucceeds(
    guruDb.collection("classes").doc("cls_x1").set({
      teacherUid: GURU_UID,
      teacherName: "Ahzami, S.Pd., Gr.",
      name: "X IPA 1 - Biologi Muria",
      gradeLevel: "X SMA/MA",
      phase: "E",
      classCode: "PRJ10A",
      academicYear: "2026/2027",
      subjectId: "subj_bio_x",
      description: "Kelas Eksplorasi Etnosains Parijoto",
      archived: false,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    guruDb.collection("class_codes").doc("PRJ10A").set({
      classId: "cls_x1",
      className: "X IPA 1 - Biologi Muria",
      teacherUid: GURU_UID,
      teacherName: "Ahzami, S.Pd., Gr.",
      gradeLevel: "X SMA/MA",
      academicYear: "2026/2027",
      active: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Siswa A looks up class code via doc get and enrolls
  await assertSucceeds(siswaADb.collection("class_codes").doc("PRJ10A").get());
  await assertSucceeds(
    siswaADb.collection("enrollments").doc(`cls_x1_${SISWA_A_UID}`).set({
      classId: "cls_x1",
      className: "X IPA 1 - Biologi Muria",
      classCode: "PRJ10A",
      teacherUid: GURU_UID,
      studentUid: SISWA_A_UID,
      studentName: "Andi Pratama",
      status: "ACTIVE",
      progressPercent: 10,
      completedLessons: ["m1_l1"],
      createdAt: now,
      updatedAt: now,
    })
  );

  // Siswa A can update own progress, cannot read Siswa B's enrollment
  await assertSucceeds(
    siswaADb.collection("enrollments").doc(`cls_x1_${SISWA_A_UID}`).update({
      progressPercent: 25,
      completedLessons: ["m1_l1", "m1_l2"],
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(siswaBDb.collection("enrollments").doc(`cls_x1_${SISWA_A_UID}`).get());

  // Teacher can list own enrollments
  await assertSucceeds(
    guruDb.collection("enrollments").where("teacherUid", "==", GURU_UID).get()
  );
});

test("Quiz Answer Keys: blocked from students until evaluation closed & discussion enabled", async () => {
  const now = new Date(Date.now() - 5000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("users").doc(GURU_UID).set({
      uid: GURU_UID,
      nama: "Guru Biologi",
      email: "guru@sman1muria.sch.id",
      role: "GURU",
      accountStatus: "ACTIVE",
      createdAt: now,
      updatedAt: now,
    });
    await ctx.firestore().collection("quizzes").doc("post_test_1").set({
      classId: "cls_x1",
      moduleId: "modul_2",
      ownerUid: GURU_UID,
      title: "Evaluasi Akhir (Post-Test)",
      category: "POST_TEST",
      durationMinutes: 45,
      questionCount: 8,
      closed: false,
      showDiscussion: false,
      published: true,
      createdAt: now,
      updatedAt: now,
    });
    await ctx.firestore().collection("quizzes").doc("post_test_1")
      .collection("answer_keys").doc("master").set({
        ownerUid: GURU_UID,
        keys: { q1: "C", q2: "B" },
        explanations: { q1: "Paradoks konservasi", q2: "Ornitokori 30%" },
        createdAt: now,
        updatedAt: now,
      });
  });

  const siswaDb = siswaContext(SISWA_A_UID).firestore();
  const guruDb = guruContext().firestore();

  // Student CANNOT read answer_keys while quiz is open
  await assertFails(
    siswaDb.collection("quizzes").doc("post_test_1").collection("answer_keys").doc("master").get()
  );

  // Guru CAN read answer_keys
  await assertSucceeds(
    guruDb.collection("quizzes").doc("post_test_1").collection("answer_keys").doc("master").get()
  );

  // Guru closes quiz and enables discussion
  await assertSucceeds(
    guruDb.collection("quizzes").doc("post_test_1").update({
      closed: true,
      showDiscussion: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Now student CAN read answer_keys for discussion
  await assertSucceeds(
    siswaDb.collection("quizzes").doc("post_test_1").collection("answer_keys").doc("master").get()
  );
});

test("Grades & Quiz Attempts: siswa cannot modify grades or overwrite submitted attempts", async () => {
  const now = new Date(Date.now() - 5000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("users").doc(GURU_UID).set({
      uid: GURU_UID,
      nama: "Guru Biologi",
      email: "guru@sman1muria.sch.id",
      role: "GURU",
      accountStatus: "ACTIVE",
      createdAt: now,
      updatedAt: now,
    });
  });

  const siswaDb = siswaContext(SISWA_A_UID).firestore();
  const guruDb = guruContext().firestore();

  // Siswa cannot create or edit grades
  await assertFails(
    siswaDb.collection("grades").doc("grade_fake").set({
      classId: "cls_x1",
      teacherUid: GURU_UID,
      studentUid: SISWA_A_UID,
      studentName: "Andi",
      assessmentType: "PENGETAHUAN",
      sourceId: "post_test_1",
      sourceTitle: "Post-Test",
      finalScore: 100,
      published: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Guru creates unpublished grade -> student cannot read it yet
  await assertSucceeds(
    guruDb.collection("grades").doc("grade_1").set({
      classId: "cls_x1",
      teacherUid: GURU_UID,
      studentUid: SISWA_A_UID,
      studentName: "Andi",
      assessmentType: "PENGETAHUAN",
      sourceId: "post_test_1",
      sourceTitle: "Post-Test",
      knowledgeScore: 85,
      essayScore: 88,
      finalScore: 86.5,
      feedback: "Analisis ekofisiologi sangat baik.",
      published: false,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertFails(siswaDb.collection("grades").doc("grade_1").get());

  // Guru publishes grade -> student can read published grade, still cannot update
  await assertSucceeds(
    guruDb.collection("grades").doc("grade_1").update({
      published: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertSucceeds(
    siswaDb.collection("grades").where("studentUid", "==", SISWA_A_UID).where("published", "==", true).get()
  );
  await assertFails(
    siswaDb.collection("grades").doc("grade_1").update({
      finalScore: 100,
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Submissions & Reflections: isolated by studentUid and teacherUid", async () => {
  const now = new Date(Date.now() - 5000);
  const siswaADb = siswaContext(SISWA_A_UID).firestore();
  const siswaBDb = siswaContext(SISWA_B_UID).firestore();

  await assertSucceeds(
    siswaADb.collection("submissions").doc("sub_1").set({
      assignmentId: "lkm_1",
      classId: "cls_x1",
      teacherUid: GURU_UID,
      studentUid: SISWA_A_UID,
      studentName: "Andi",
      pblPhase: 3,
      problemAnalysis: "Eksploitasi Parijoto liar merusak meristem apikal.",
      hypothesis: "Panen parsial 30% menjaga ornitokori.",
      status: "SUBMITTED",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Ghost field rejected
  await assertFails(
    siswaADb.collection("submissions").doc("sub_ghost").set({
      assignmentId: "lkm_1",
      classId: "cls_x1",
      teacherUid: GURU_UID,
      studentUid: SISWA_A_UID,
      studentName: "Andi",
      pblPhase: 3,
      status: "SUBMITTED",
      ghostVerified: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Siswa B cannot read Siswa A submission
  await assertFails(siswaBDb.collection("submissions").doc("sub_1").get());
});

test("Phase 3 Multi-User (1 Admin, 1 Guru, 2 Siswa): class creation, join, duplicate prevention, teacher profile, deactivation & removal", async () => {
  const now = new Date(Date.now() - 5000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("users").doc(GURU_UID).set({
      uid: GURU_UID,
      nama: "Ahzami, S.Pd., Gr.",
      email: "guru@sman1muria.sch.id",
      role: "GURU",
      accountStatus: "ACTIVE",
      createdAt: now,
      updatedAt: now,
    });
    await db.collection("modules").doc("modul_1_etnosains").set({
      subjectId: "subj_bio_x",
      moduleNumber: 1,
      title: "Konsep Keanekaragaman Hayati & Etnosains Parijoto",
      subtitle: "Pertemuan 1: Eksplorasi Keanekaragaman Hayati Muria",
      description: "Orientasi masalah & identifikasi karakter Parijoto",
      meetingLabel: "Pertemuan 1",
      durationMinutes: 90,
      ownerUid: GURU_UID,
      published: true,
      createdAt: now,
      updatedAt: now,
    });
  });

  const adminDb = adminContext().firestore();
  const guruDb = guruContext().firestore();
  const siswaADb = siswaContext(SISWA_A_UID).firestore();
  const siswaBDb = siswaContext(SISWA_B_UID).firestore();

  // 1. Admin creates a class assigned to Guru
  await assertSucceeds(
    adminDb.collection("classes").doc("cls_admin_x2").set({
      teacherUid: GURU_UID,
      teacherName: "Ahzami, S.Pd., Gr.",
      name: "X IPA 2 - Muria",
      gradeLevel: "X SMA/MA",
      phase: "E",
      classCode: "PRJ202",
      academicYear: "2026/2027",
      subjectId: "subj_bio_x",
      description: "Kelas Biologi Fase E oleh Admin",
      archived: false,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    adminDb.collection("class_codes").doc("PRJ202").set({
      classId: "cls_admin_x2",
      className: "X IPA 2 - Muria",
      teacherUid: GURU_UID,
      teacherName: "Ahzami, S.Pd., Gr.",
      gradeLevel: "X SMA/MA",
      academicYear: "2026/2027",
      active: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  // 2. Guru updates meeting schedule/module (Pertemuan 1)
  await assertSucceeds(
    guruDb.collection("modules").doc("modul_1_etnosains").update({
      meetingLabel: "Pertemuan 1 (2 JP)",
      durationMinutes: 90,
      updatedAt: new Date(Date.now() - 2000),
    })
  );

  // 3. Siswa A & Siswa B both join via code PRJ202
  await assertSucceeds(siswaADb.collection("class_codes").doc("PRJ202").get());
  await assertSucceeds(siswaBDb.collection("class_codes").doc("PRJ202").get());

  await assertSucceeds(
    siswaADb.collection("enrollments").doc(`cls_admin_x2_${SISWA_A_UID}`).set({
      classId: "cls_admin_x2",
      className: "X IPA 2 - Muria",
      classCode: "PRJ202",
      teacherUid: GURU_UID,
      studentUid: SISWA_A_UID,
      studentName: "Andi Pratama",
      status: "ACTIVE",
      progressPercent: 0,
      completedLessons: [],
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    siswaBDb.collection("enrollments").doc(`cls_admin_x2_${SISWA_B_UID}`).set({
      classId: "cls_admin_x2",
      className: "X IPA 2 - Muria",
      classCode: "PRJ202",
      teacherUid: GURU_UID,
      studentUid: SISWA_B_UID,
      studentName: "Siti Rahma",
      status: "ACTIVE",
      progressPercent: 0,
      completedLessons: [],
      createdAt: now,
      updatedAt: now,
    })
  );

  // 4. Duplicate enrollment with random doc ID is blocked by rules
  await assertFails(
    siswaADb.collection("enrollments").doc(`cls_admin_x2_${SISWA_A_UID}_dup`).set({
      classId: "cls_admin_x2",
      className: "X IPA 2 - Muria",
      classCode: "PRJ202",
      teacherUid: GURU_UID,
      studentUid: SISWA_A_UID,
      studentName: "Andi Pratama",
      status: "ACTIVE",
      progressPercent: 0,
      completedLessons: [],
      createdAt: now,
      updatedAt: now,
    })
  );

  // 5. Enrolled students can read class details (Teacher Profile & Learning Info) & published modules
  await assertSucceeds(siswaADb.collection("classes").doc("cls_admin_x2").get());
  await assertSucceeds(siswaBDb.collection("classes").doc("cls_admin_x2").get());
  await assertSucceeds(siswaADb.collection("modules").where("published", "==", true).get());
  // Students cannot edit modules
  await assertFails(
    siswaADb.collection("modules").doc("modul_1_etnosains").update({
      meetingLabel: "Hacked",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // 6. Guru deactivates Siswa B -> Siswa B loses class read access & cannot self-reactivate
  await assertSucceeds(
    guruDb.collection("enrollments").doc(`cls_admin_x2_${SISWA_B_UID}`).update({
      status: "REMOVED",
      updatedAt: new Date(Date.now() - 1500),
    })
  );
  await assertFails(siswaBDb.collection("classes").doc("cls_admin_x2").get());
  await assertFails(
    siswaBDb.collection("enrollments").doc(`cls_admin_x2_${SISWA_B_UID}`).update({
      status: "ACTIVE",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // 7. Guru reactivates Siswa B -> Siswa B regains class read access, then Guru removes Siswa B permanently
  await assertSucceeds(
    guruDb.collection("enrollments").doc(`cls_admin_x2_${SISWA_B_UID}`).update({
      status: "ACTIVE",
      updatedAt: new Date(Date.now() - 800),
    })
  );
  await assertSucceeds(siswaBDb.collection("classes").doc("cls_admin_x2").get());
  await assertSucceeds(
    guruDb.collection("enrollments").doc(`cls_admin_x2_${SISWA_B_UID}`).delete()
  );
});

test("Phase 4 Learning Progress & Teacher Lessons: bookmarks, fontScale bounds, cross-user isolation, teacher lesson CRUD", async () => {
  const now = new Date(Date.now() - 5000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("users").doc(GURU_UID).set({
      uid: GURU_UID,
      nama: "Ahzami, S.Pd., Gr.",
      email: "guru@sman1muria.sch.id",
      role: "GURU",
      accountStatus: "ACTIVE",
      createdAt: now,
      updatedAt: now,
    });
  });

  const siswaADb = siswaContext(SISWA_A_UID).firestore();
  const siswaBDb = siswaContext(SISWA_B_UID).firestore();
  const guruDb = guruContext().firestore();

  // 1. Siswa A saves reading progress, bookmark, and text scale
  await assertSucceeds(
    siswaADb.collection("learning_progress").doc(SISWA_A_UID).set({
      studentUid: SISWA_A_UID,
      completedLessons: ["m1_l01"],
      bookmarkedLessons: ["m1_l12", "m2_l16"],
      lessonReadProgress: { m1_l01: 100, m1_l12: 65 },
      lastLessonByModule: { modul_1_etnosains: "m1_l12" },
      readerFontScale: 1.15,
      createdAt: now,
      updatedAt: now,
    })
  );

  // 2. Out-of-bounds readerFontScale (>1.5) rejected
  await assertFails(
    siswaADb.collection("learning_progress").doc(SISWA_A_UID).update({
      readerFontScale: 2.5,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // 3. Siswa B cannot read or write Siswa A's learning_progress
  await assertFails(siswaBDb.collection("learning_progress").doc(SISWA_A_UID).get());
  await assertFails(
    siswaBDb.collection("learning_progress").doc(SISWA_A_UID).update({
      bookmarkedLessons: [],
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // 4. Guru creates, edits (as draft), and deletes a lesson; Siswa cannot read Guru's draft lesson
  await assertSucceeds(
    guruDb.collection("lessons").doc("les_custom_1").set({
      moduleId: "modul_1_etnosains",
      orderIndex: 16,
      title: "16. Uji Lapangan Lereng Muria",
      learningObjective: "Mengamati kondisi mikroklimat inang",
      explanation: "Pembahasan lanjutan pengamatan lapangan.",
      summary: "Mikroklimat menentukan kepadatan epifit.",
      ownerUid: GURU_UID,
      published: false,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertFails(siswaADb.collection("lessons").doc("les_custom_1").get());
  await assertSucceeds(guruDb.collection("lessons").where("ownerUid", "==", GURU_UID).get());
  await assertSucceeds(
    guruDb.collection("lessons").doc("les_custom_1").update({
      published: true,
      title: "16. Uji Lapangan Lereng Muria (Revisi)",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertSucceeds(siswaADb.collection("lessons").doc("les_custom_1").get());
  await assertSucceeds(guruDb.collection("lessons").doc("les_custom_1").delete());
});

test("Phase 5 PBL System: group leadership/roles, discussions, submission revision loop, class presentations & peer reviews", async () => {
  const now = new Date(Date.now() - 5000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("users").doc(GURU_UID).set({
      uid: GURU_UID,
      nama: "Ahzami, S.Pd., Gr.",
      email: "guru@sman1muria.sch.id",
      role: "GURU",
      accountStatus: "ACTIVE",
      createdAt: now,
      updatedAt: now,
    });
    await db.collection("classes").doc("cls_pbl_1").set({
      teacherUid: GURU_UID,
      teacherName: "Ahzami, S.Pd., Gr.",
      name: "X IPA 1 PBL",
      gradeLevel: "X SMA/MA",
      phase: "E",
      classCode: "PBL101",
      academicYear: "2026/2027",
      subjectId: "subj_bio_x",
      archived: false,
      createdAt: now,
      updatedAt: now,
    });
    await db.collection("enrollments").doc(`cls_pbl_1_${SISWA_A_UID}`).set({
      classId: "cls_pbl_1",
      className: "X IPA 1 PBL",
      classCode: "PBL101",
      teacherUid: GURU_UID,
      studentUid: SISWA_A_UID,
      studentName: "Andi Pratama",
      status: "ACTIVE",
      progressPercent: 50,
      completedLessons: [],
      createdAt: now,
      updatedAt: now,
    });
    await db.collection("enrollments").doc(`cls_pbl_1_${SISWA_B_UID}`).set({
      classId: "cls_pbl_1",
      className: "X IPA 1 PBL",
      classCode: "PBL101",
      teacherUid: GURU_UID,
      studentUid: SISWA_B_UID,
      studentName: "Siti Rahma",
      status: "ACTIVE",
      progressPercent: 40,
      completedLessons: [],
      createdAt: now,
      updatedAt: now,
    });
  });

  const guruDb = guruContext().firestore();
  const siswaADb = siswaContext(SISWA_A_UID).firestore();
  const siswaBDb = siswaContext(SISWA_B_UID).firestore();
  const outsiderDb = siswaContext("outsider_99").firestore();

  // 1. Siswa A creates PBL group as leader, Siswa B joins, Siswa A assigns roles & transfers leadership
  await assertSucceeds(
    siswaADb.collection("groups").doc("grp_muria_1").set({
      classId: "cls_pbl_1",
      teacherUid: GURU_UID,
      name: "Kelompok Epifit 1",
      topic: "Konservasi Parijoto Gunung Muria",
      memberUids: [SISWA_A_UID],
      memberNames: ["Andi Pratama"],
      memberRoles: { [SISWA_A_UID]: "Ketua Kelompok" },
      leaderUid: SISWA_A_UID,
      pblPhase: 1,
      openToJoin: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    siswaBDb.collection("groups").doc("grp_muria_1").update({
      memberUids: [SISWA_A_UID, SISWA_B_UID],
      memberNames: ["Andi Pratama", "Siti Rahma"],
      memberRoles: { [SISWA_A_UID]: "Ketua Kelompok", [SISWA_B_UID]: "Analis Data" },
      updatedAt: new Date(Date.now() - 3000),
    })
  );

  // Non-leader Siswa B CANNOT change leaderUid
  await assertFails(
    siswaBDb.collection("groups").doc("grp_muria_1").update({
      leaderUid: SISWA_B_UID,
      updatedAt: new Date(Date.now() - 2500),
    })
  );

  // Leader Siswa A CAN transfer leadership to member Siswa B and save discussionSummary
  await assertSucceeds(
    siswaADb.collection("groups").doc("grp_muria_1").update({
      leaderUid: SISWA_B_UID,
      discussionSummary: "Hasil diskusi: panen parsial 30% menjaga ornitokori.",
      updatedAt: new Date(Date.now() - 2000),
    })
  );

  // 2. Group discussions: Siswa A & Guru post messages, outsider denied
  await assertSucceeds(
    siswaADb.collection("group_discussions").doc("msg_1").set({
      groupId: "grp_muria_1",
      classId: "cls_pbl_1",
      teacherUid: GURU_UID,
      senderUid: SISWA_A_UID,
      senderName: "Andi Pratama",
      senderRole: "SISWA",
      pblPhase: 3,
      message: "Data suhu 2023 naik menjadi 28,4 C dan kelembapan turun ke 52%.",
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    guruDb.collection("group_discussions").doc("msg_2").set({
      groupId: "grp_muria_1",
      classId: "cls_pbl_1",
      teacherUid: GURU_UID,
      senderUid: GURU_UID,
      senderName: "Ahzami, S.Pd., Gr.",
      senderRole: "GURU",
      pblPhase: 3,
      message: "Bagus! Kaitkan penurunan kelembapan 32% dengan fotoinhibisi & plasmolisis.",
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertFails(
    outsiderDb.collection("group_discussions").where("groupId", "==", "grp_muria_1").get()
  );

  // 3. Submission draft privacy, teacher revision loop, and classmate presentation visibility
  await assertSucceeds(
    siswaADb.collection("submissions").doc(`asg_proyek_cer_${SISWA_A_UID}`).set({
      assignmentId: "asg_proyek_cer",
      classId: "cls_pbl_1",
      teacherUid: GURU_UID,
      studentUid: SISWA_A_UID,
      studentName: "Andi Pratama",
      groupId: "grp_muria_1",
      pblPhase: 5,
      cerClaim: "Konservasi in-situ 30% menjaga populasi Parijoto.",
      status: "DRAFT",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Classmate Siswa B CANNOT read Siswa A's DRAFT
  await assertFails(
    siswaBDb.collection("submissions").doc(`asg_proyek_cer_${SISWA_A_UID}`).get()
  );

  // Siswa A submits -> Teacher returns for revision with teacherFeedback -> Siswa B still CANNOT read REVISION_REQUIRED
  await assertSucceeds(
    siswaADb.collection("submissions").doc(`asg_proyek_cer_${SISWA_A_UID}`).update({
      status: "SUBMITTED",
      updatedAt: new Date(Date.now() - 1800),
    })
  );
  await assertSucceeds(
    guruDb.collection("submissions").doc(`asg_proyek_cer_${SISWA_A_UID}`).update({
      status: "REVISION_REQUIRED",
      teacherFeedback: "Tambahkan data flavonoid Vifta (2021): 81,60 mg QE/g pada Evidence.",
      updatedAt: new Date(Date.now() - 1500),
    })
  );
  await assertFails(
    siswaBDb.collection("submissions").doc(`asg_proyek_cer_${SISWA_A_UID}`).get()
  );

  // Siswa A CANNOT tamper with teacherFeedback, but CAN revise and resubmit
  await assertFails(
    siswaADb.collection("submissions").doc(`asg_proyek_cer_${SISWA_A_UID}`).update({
      teacherFeedback: "Nilai sempurna",
      updatedAt: new Date(Date.now() - 1200),
    })
  );
  await assertSucceeds(
    siswaADb.collection("submissions").doc(`asg_proyek_cer_${SISWA_A_UID}`).update({
      cerEvidence: "Kadar flavonoid Muria 81,60 mg QE/g (Vifta dkk., 2021) dan penurunan epifit 42->11 rumpun/ha.",
      status: "SUBMITTED",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Now classmate Siswa B CAN view class presentations (SUBMITTED/GRADED) & submit peer review
  await assertSucceeds(
    siswaBDb.collection("submissions")
      .where("classId", "==", "cls_pbl_1")
      .where("status", "in", ["SUBMITTED", "GRADED"])
      .get()
  );

  // Self peer review blocked
  await assertFails(
    siswaADb.collection("peer_reviews").doc(`asg_proyek_cer_${SISWA_A_UID}_${SISWA_A_UID}`).set({
      submissionId: `asg_proyek_cer_${SISWA_A_UID}`,
      assignmentId: "asg_proyek_cer",
      classId: "cls_pbl_1",
      teacherUid: GURU_UID,
      reviewerUid: SISWA_A_UID,
      reviewerName: "Andi Pratama",
      targetStudentUid: SISWA_A_UID,
      targetStudentName: "Andi Pratama",
      clarityScore: 4,
      evidenceScore: 4,
      solutionScore: 4,
      comment: "Karya saya sendiri paling bagus.",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Classmate Siswa B submits valid peer review
  await assertSucceeds(
    siswaBDb.collection("peer_reviews").doc(`asg_proyek_cer_${SISWA_A_UID}_${SISWA_B_UID}`).set({
      submissionId: `asg_proyek_cer_${SISWA_A_UID}`,
      assignmentId: "asg_proyek_cer",
      classId: "cls_pbl_1",
      teacherUid: GURU_UID,
      reviewerUid: SISWA_B_UID,
      reviewerName: "Siti Rahma",
      targetStudentUid: SISWA_A_UID,
      targetStudentName: "Andi Pratama",
      clarityScore: 4,
      evidenceScore: 4,
      solutionScore: 3,
      comment: "Data Vifta 2021 dan argumen CER sangat jelas serta terstruktur.",
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("Phase 6 Quiz & Evaluation: hidden draft questions, answer_keys & unpublished grades protection, autosave IN_PROGRESS -> SUBMITTED lock, and teacher grading", async () => {
  const now = new Date(Date.now() - 5000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("users").doc(GURU_UID).set({
      uid: GURU_UID,
      nama: "Ahzami, S.Pd., Gr.",
      email: "guru@sman1muria.sch.id",
      role: "GURU",
      accountStatus: "ACTIVE",
      createdAt: now,
      updatedAt: now,
    });
  });

  const guruDb = guruContext().firestore();
  const siswaADb = siswaContext(SISWA_A_UID).firestore();
  const siswaBDb = siswaContext(SISWA_B_UID).firestore();

  // 1. Teacher creates Evaluasi Akhir quiz (resultsPublished=false, closed=false, showDiscussion=false) + draft question (published=false) + opened question (published=true) + protected answer_key
  await assertSucceeds(
    guruDb.collection("quizzes").doc("quiz_post_test").set({
      classId: "DEFAULT",
      moduleId: "modul_2_konservasi",
      ownerUid: GURU_UID,
      title: "Evaluasi Akhir (Post-Test HOTS & Uraian)",
      category: "POST_TEST",
      durationMinutes: 45,
      questionCount: 8,
      maxAttempts: 1,
      shuffleQuestions: true,
      shuffleOptions: true,
      openForStudents: true,
      scheduleLabel: "Pertemuan 2 • Terbuka",
      scheduleStartMillis: 0,
      scheduleEndMillis: 0,
      resultsPublished: false,
      closed: false,
      showDiscussion: false,
      published: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    guruDb.collection("questions").doc("q_draft_hidden").set({
      quizId: "quiz_post_test",
      orderIndex: 9,
      type: "MULTIPLE_CHOICE",
      prompt: "Soal rahasia yang belum dibuka guru",
      optionA: "Opsi A",
      optionB: "Opsi B",
      hotsCategory: "HOTS Kasus",
      maxPoints: 20,
      ownerUid: GURU_UID,
      published: false,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    guruDb.collection("questions").doc("q_open_visible").set({
      quizId: "quiz_post_test",
      orderIndex: 1,
      type: "ESSAY",
      prompt: "Jelaskan strategi konservasi ex-situ Parijoto!",
      stimulusText: "Kasus eksploitasi liar Muria",
      hotsCategory: "Uraian HOTS",
      maxPoints: 50,
      ownerUid: GURU_UID,
      published: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    guruDb.collection("quizzes").doc("quiz_post_test").collection("answer_keys").doc("master").set({
      ownerUid: GURU_UID,
      keys: { q_draft_hidden: "A" },
      explanations: { q_draft_hidden: "Pembahasan soal tersembunyi." },
      createdAt: now,
      updatedAt: now,
    })
  );

  // Student CANNOT read unpublished question or answer_keys
  await assertFails(siswaADb.collection("questions").doc("q_draft_hidden").get());
  await assertSucceeds(siswaADb.collection("questions").doc("q_open_visible").get());
  await assertFails(siswaADb.collection("quizzes").doc("quiz_post_test").collection("answer_keys").doc("master").get());

  // 2. Student autosaves IN_PROGRESS attempt -> updates answers -> submits (SUBMITTED)
  await assertSucceeds(
    siswaADb.collection("quiz_attempts").doc(`quiz_post_test_${SISWA_A_UID}_att1`).set({
      quizId: "quiz_post_test",
      classId: "cls_1",
      teacherUid: GURU_UID,
      studentUid: SISWA_A_UID,
      studentName: "Andi Pratama",
      attemptNumber: 1,
      answers: { q1: "C" },
      essayAnswers: {},
      questionOrder: ["q_open_visible"],
      remainingSeconds: 2640,
      autoScore: 0,
      essayScore: 0,
      totalScore: 0,
      correctCount: 0,
      totalMcqCount: 1,
      resultsPublished: false,
      status: "IN_PROGRESS",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Student autosaves essay answer while IN_PROGRESS
  await assertSucceeds(
    siswaADb.collection("quiz_attempts").doc(`quiz_post_test_${SISWA_A_UID}_att1`).update({
      essayAnswers: { q_open_visible: "Kultur jaringan memanfaatkan sifat totipotensi sel." },
      remainingSeconds: 2400,
      updatedAt: new Date(Date.now() - 3000),
    })
  );

  // Student CANNOT inject teacherFeedback or essayItemScores during IN_PROGRESS
  await assertFails(
    siswaADb.collection("quiz_attempts").doc(`quiz_post_test_${SISWA_A_UID}_att1`).update({
      essayItemScores: { q_open_visible: 4 },
      updatedAt: new Date(Date.now() - 2500),
    })
  );

  // Student submits attempt
  await assertSucceeds(
    siswaADb.collection("quiz_attempts").doc(`quiz_post_test_${SISWA_A_UID}_att1`).update({
      status: "SUBMITTED",
      updatedAt: new Date(Date.now() - 2000),
    })
  );

  // Once SUBMITTED, student CANNOT modify answers anymore; Siswa B cannot read Siswa A attempt
  await assertFails(
    siswaADb.collection("quiz_attempts").doc(`quiz_post_test_${SISWA_A_UID}_att1`).update({
      answers: { q1: "A" },
      updatedAt: new Date(Date.now() - 1500),
    })
  );
  await assertFails(
    siswaBDb.collection("quiz_attempts").doc(`quiz_post_test_${SISWA_A_UID}_att1`).get()
  );

  // 3. Teacher grades essay manually and publishes results + closes quiz with discussion
  await assertSucceeds(
    guruDb.collection("quiz_attempts").doc(`quiz_post_test_${SISWA_A_UID}_att1`).update({
      autoScore: 100,
      essayItemScores: { q_open_visible: 4 },
      essayScore: 100,
      totalScore: 100,
      correctCount: 1,
      resultsPublished: true,
      teacherFeedback: "Penjelasan totipotensi dan kultur jaringan sangat tepat.",
      status: "GRADED",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Even when closed & showDiscussion are true, if resultsPublished is false, answer_keys stay protected!
  await assertSucceeds(
    guruDb.collection("quizzes").doc("quiz_post_test").update({
      closed: true,
      showDiscussion: true,
      resultsPublished: false,
      updatedAt: new Date(Date.now() - 800),
    })
  );
  await assertFails(siswaADb.collection("quizzes").doc("quiz_post_test").collection("answer_keys").doc("master").get());

  // Once teacher sets resultsPublished: true, student can read answer_keys for discussion
  await assertSucceeds(
    guruDb.collection("quizzes").doc("quiz_post_test").update({
      resultsPublished: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertSucceeds(siswaADb.collection("quizzes").doc("quiz_post_test").collection("answer_keys").doc("master").get());
});

test("Phase 7 Reports: promoted admin lists all classes/grades, teacher scoped, student sees published only", async () => {
  const now = new Date(Date.now() - 5000);
  const PROMOTED_ADMIN_UID = "admin_promoted_002";
  const GURU_B_UID = "guru_b_002";

  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("admins").doc(PROMOTED_ADMIN_UID).set({
      email: "wakasek@sman1muria.sch.id",
      grantedAt: now,
    });
    await db.collection("classes").doc("cls_a").set({
      teacherUid: GURU_UID,
      teacherName: "Guru A",
      name: "X IPA 1",
      gradeLevel: "X",
      phase: "E",
      classCode: "PRJ11A",
      academicYear: "2026/2027",
      subjectId: "subj_bio_x",
      archived: false,
      createdAt: now,
      updatedAt: now,
    });
    await db.collection("classes").doc("cls_b").set({
      teacherUid: GURU_B_UID,
      teacherName: "Guru B",
      name: "X IPA 2",
      gradeLevel: "X",
      phase: "E",
      classCode: "PRJ22B",
      academicYear: "2026/2027",
      subjectId: "subj_bio_x",
      archived: false,
      createdAt: now,
      updatedAt: now,
    });
    await db.collection("grades").doc("grd_pub_s1").set({
      classId: "cls_a",
      teacherUid: GURU_UID,
      studentUid: SISWA_A_UID,
      studentName: "Andi Pratama",
      assessmentType: "PROYEK",
      sourceId: "asg_proyek_cer",
      sourceTitle: "Proyek Kampanye Digital CER",
      finalScore: 91.5,
      published: true,
      createdAt: now,
      updatedAt: now,
    });
    await db.collection("grades").doc("grd_draft_s1").set({
      classId: "cls_a",
      teacherUid: GURU_UID,
      studentUid: SISWA_A_UID,
      studentName: "Andi Pratama",
      assessmentType: "PENGETAHUAN",
      sourceId: "quiz_post_test",
      sourceTitle: "Evaluasi Akhir",
      finalScore: 84.0,
      published: false,
      createdAt: now,
      updatedAt: now,
    });
  });

  const promotedAdminDb = testEnv.authenticatedContext(PROMOTED_ADMIN_UID, {
    email: "wakasek@sman1muria.sch.id",
    email_verified: true,
  }).firestore();
  const guruADb = guruContext().firestore();
  const siswaADb = siswaContext(SISWA_A_UID).firestore();

  // Promoted admin can list ALL classes, enrollments, assignments, quizzes, and grades
  await assertSucceeds(promotedAdminDb.collection("classes").get());
  await assertSucceeds(promotedAdminDb.collection("grades").get());
  await assertSucceeds(promotedAdminDb.collection("submissions").get());
  await assertSucceeds(promotedAdminDb.collection("assignments").get());
  await assertSucceeds(promotedAdminDb.collection("quizzes").get());

  // Teacher A CANNOT list all classes or Teacher B's grades
  await assertFails(guruADb.collection("classes").get());
  await assertFails(guruADb.collection("grades").where("teacherUid", "==", GURU_B_UID).get());
  await assertSucceeds(guruADb.collection("grades").where("teacherUid", "==", GURU_UID).get());

  // Student A can read own published grade (including PROYEK), CANNOT read draft grade
  await assertSucceeds(siswaADb.collection("grades").doc("grd_pub_s1").get());
  await assertFails(siswaADb.collection("grades").doc("grd_draft_s1").get());
  await assertFails(siswaADb.collection("grades").where("studentUid", "==", SISWA_A_UID).get());
  await assertSucceeds(
    siswaADb.collection("grades")
      .where("studentUid", "==", SISWA_A_UID)
      .where("published", "==", true)
      .get()
  );
});

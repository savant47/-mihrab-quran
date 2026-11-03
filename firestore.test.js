const { initializeTestEnvironment, assertFails, assertSucceeds } = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;

before(async () => {
  testEnv = await initializeTestEnvironment({
    projectId: "gen-lang-client-0751808412",
    firestore: {
      rules: fs.readFileSync("firestore.rules", "utf8"),
      host: "127.0.0.1",
      port: 8085
    }
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

test("unauthenticated user cannot read user profile", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc("user1").get());
});

test("user can read and write their own profile", async () => {
  const aliceDb = testEnv.authenticatedContext("user1", { email: "alice@example.com" }).firestore();
  await assertSucceeds(aliceDb.collection("users").doc("user1").set({
    uid: "user1",
    email: "alice@example.com",
    role: "user",
    currentKhatmaNumber: 1,
    currentSurah: "البقرة",
    currentAyah: 1,
    currentPage: 1,
    completedRubCount: 0,
    progressPercentage: 0.0
  }));
  await assertSucceeds(aliceDb.collection("users").doc("user1").get());
});

test("user cannot read another user's profile", async () => {
  const aliceDb = testEnv.authenticatedContext("user1", { email: "alice@example.com" }).firestore();
  await assertFails(aliceDb.collection("users").doc("user2").get());
});

test("master admin savant47@gmail.com can read any user's profile", async () => {
  const adminDb = testEnv.authenticatedContext("admin_uid", { email: "savant47@gmail.com" }).firestore();
  await assertSucceeds(adminDb.collection("users").doc("user1").get());
});

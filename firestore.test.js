const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = "demo-no-project";
const ALICE_UID = "alice_founder_123";
const BOB_UID = "bob_intruder_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

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

test("Unauthenticated user: cannot read dojos or students", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("dojos").get());
  await assertFails(unauthDb.collection("students").get());
});

test("Authenticated user: cannot read another owner's organisation or dojos", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("organisations").doc("org_1").set({
      ownerUid: ALICE_UID,
      name: "Shotokan World",
      martialArtStyle: "Karate",
      createdAt: new Date(),
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("organisations").doc("org_1").get());
});

test("Authenticated owner: can create organisation and query own dojos", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("organisations").doc("org_alice").set({
      ownerUid: ALICE_UID,
      name: "JFSKIF Chennai",
      shortName: "JFSKIF",
      martialArtStyle: "Shotokan Karate",
      country: "India",
      language: "English",
      topSenseiName: "Shihan Jagadish",
      createdAt: new Date(),
    })
  );

  await assertSucceeds(
    aliceDb.collection("dojos").doc("dojo_1").set({
      orgId: "org_alice",
      ownerUid: ALICE_UID,
      name: "Chennai Central Dojo",
      status: "active",
      createdAt: new Date(),
    })
  );

  await assertSucceeds(
    aliceDb.collection("dojos").where("ownerUid", "==", ALICE_UID).get()
  );
});

test("Validation rejects invalid status or missing required fields", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  // Missing required martialArtStyle
  await assertFails(
    aliceDb.collection("organisations").doc("org_bad").set({
      ownerUid: ALICE_UID,
      name: "Bad Dojo",
      createdAt: new Date(),
    })
  );

  // Invalid dojo status
  await assertFails(
    aliceDb.collection("dojos").doc("dojo_bad").set({
      orgId: "org_alice",
      ownerUid: ALICE_UID,
      name: "Bad Dojo",
      status: "unknown_status",
      createdAt: new Date(),
    })
  );
});

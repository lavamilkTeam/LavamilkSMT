import { test } from "node:test";
import assert from "node:assert/strict";
import {
  parsePlacementCsv,
  encodePlacementCsv,
  samplePlacements,
  nextPending,
} from "./model.ts";

test("CSV round-trip preserves quotes, commas, Unicode, coordinates and board side", () => {
  const rows = [
    {
      ...samplePlacements[0],
      part: '10k,"precision"\n电阻',
      side: "Bottom" as const,
    },
  ];
  assert.deepEqual(
    parsePlacementCsv("\uFEFF" + encodePlacementCsv(rows)),
    rows,
  );
});
test("invalid imports fail before replacing a job", () => {
  assert.throws(() => parsePlacementCsv("id,part,x\nR1,test,1"), /表头/);
  assert.throws(
    () =>
      parsePlacementCsv(
        encodePlacementCsv([samplePlacements[0], samplePlacements[0]]),
      ),
    /重复/,
  );
  assert.throws(
    () =>
      parsePlacementCsv(
        "id,part,package,x,y,rotation,side\nR1,10k,0603,NaN,2,0,Top",
      ),
    /有限数字/,
  );
  assert.throws(
    () =>
      parsePlacementCsv(
        "id,part,package,x,y,rotation,side\nR1,10k,0603,,2,0,Top",
      ),
    /有限数字/,
  );
  assert.throws(
    () =>
      parsePlacementCsv(
        "id,part,package,x,y,rotation,side\nR1,10k,0603,1,2,0,left",
      ),
    /side/,
  );
  assert.throws(
    () => parsePlacementCsv('id,part,package,x,y,rotation,side\n"R1'),
    /未闭合/,
  );
});
test("simulation skips disabled and completed placements", () => {
  const rows = samplePlacements.map((p) => ({ ...p }));
  rows[1].enabled = false;
  assert.equal(nextPending(rows, ["R1"])?.id, "R3");
  assert.equal(
    nextPending(
      rows,
      rows.map((p) => p.id),
    ),
    undefined,
  );
});

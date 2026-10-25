const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const ts = require('typescript');
const source = fs.readFileSync(path.join(__dirname, '../features/work/utils/notification-time.ts'), 'utf8');
const compiled = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } });
const helpers = {};
new Function('exports', compiled.outputText)(helpers);
test('chosen timezone is independent from the process timezone', () => {
  assert.equal(helpers.notificationInstant('2026-10-26T09:00', 'Asia/Seoul'), '2026-10-26T00:00:00.000Z');
  assert.equal(helpers.notificationInstant('2026-10-26T09:00', 'America/New_York'), '2026-10-26T13:00:00.000Z');
  assert.equal(helpers.notificationLocalTime('2026-10-26T00:00:00Z', 'America/New_York'), '2026-10-25T20:00');
});
test('DST overlap takes first offset, gap and impossible dates are rejected', () => {
  assert.equal(helpers.notificationInstant('2026-11-01T01:30', 'America/New_York'), '2026-11-01T05:30:00.000Z');
  assert.throws(() => helpers.notificationInstant('2027-03-14T02:30', 'America/New_York'), /존재하지 않는/);
  assert.throws(() => helpers.notificationInstant('2026-02-30T12:00', 'UTC'), /유효한/);
});
test('fractional timezone offsets and midnight round-trip', () => {
  for (const zone of ['Asia/Kathmandu', 'Australia/Lord_Howe', 'Pacific/Chatham', 'UTC']) {
    const instant = helpers.notificationInstant('2026-10-26T00:00', zone);
    assert.equal(helpers.notificationLocalTime(instant, zone), '2026-10-26T00:00');
  }
});

const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const ts = require('typescript');
const source = fs.readFileSync(path.join(__dirname, '../features/work/utils/work-insights.ts'), 'utf8');
const compiled = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } });
const helpers = {};
new Function('exports', compiled.outputText)(helpers);
test('calendar dates remain exact across leap day, DST and year boundary', () => {
  assert.deepEqual(helpers.calendarDates('2024-02-28', '2024-03-01'), ['2024-02-28','2024-02-29','2024-03-01']);
  assert.equal(helpers.addCalendarDays('2026-03-08', 1), '2026-03-09');
  assert.equal(helpers.addCalendarDays('2026-12-31', 1), '2027-01-01');
  assert.deepEqual(helpers.calendarDates('2026-01-02','2026-01-01'), []);
});
test('gantt clips ranges and marks one-sided dates without inventing duration', () => {
  assert.deepEqual(helpers.schedulePosition('2026-01-01','2026-01-15','2026-01-11','2026-01-20'), {left:0,width:50,partial:false});
  assert.deepEqual(helpers.schedulePosition(null,'2026-01-15','2026-01-11','2026-01-20'), {left:40,width:10,partial:true});
  assert.equal(helpers.schedulePosition(null,null,'2026-01-11','2026-01-20'), null);
  assert.equal(helpers.schedulePosition('2026-01-01','2026-01-02','2026-01-11','2026-01-20'), null);
  assert.match(helpers.scheduleLabel(null,'2026-01-15'), /시작 미정/);
});

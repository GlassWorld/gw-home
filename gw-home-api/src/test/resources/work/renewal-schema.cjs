// 리뉴얼 설치·마이그레이션을 격리 PostgreSQL에서 검증한다. 실제 DB에는 연결하지 않는다.
const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const { PGlite } = require(path.resolve(process.argv[2]));
const root = path.resolve(__dirname, '../../../../..');
const read = file => fs.readFileSync(path.join(root, file), 'utf8').replace(/\r\n/g, '\n').trim();
const ddlRoot = 'gw-home-infra-db/src/main/resources/sql/ddl/';
const ddl = file => read(ddlRoot + file);
const migrate = ddl('work/project-renewal-migrate.sql');
const verify = ddl('work/project-renewal-verify.sql');
const notification = ddl('work/tb_wrk_ntf.sql');
const completion = ddl('work/tb_wrk_todo_completion_migrate.sql').replace(/^BEGIN;\n/m, '').replace(/\nCOMMIT;$/, '');
const aggregate = read('docs/all-ddl.sql');
const archived = ['tb_wrk_dly_rpt', 'tb_wrk_dly_rpt_wrk_unit', 'tb_wrk_wkl_rpt', 'tb_wrk_git_acct', 'tb_wrk_git_prj', 'tb_wrk_unit_git_cfg', 'tb_wrk_unit_git_prj'];
const required = ['tb_mbr_acct', 'tb_work_unit', 'tb_wrk_todo', 'tb_wrk_ntf_stg', 'tb_wrk_todo_rmd', 'tb_wrk_ntf_dlvr'];
const withoutComments = sql => sql.replace(/--[^\n]*/g, '');
// 달러 인용을 모르는 클라이언트를 재현한다. 이 파일에서 쓰는 일반 문자열·식별자 인용만 처리한다.
function splitClientStatements(sql) {
  const source = withoutComments(sql);
  const statements = [];
  let start = 0;
  let quote = null;
  for (let index = 0; index < source.length; index++) {
    const character = source[index];
    if (quote) {
      if (character === quote && source[index + 1] === quote) index++;
      else if (character === quote) quote = null;
    } else if (character === "'" || character === '"') quote = character;
    else if (character === ';') {
      const statement = source.slice(start, index).trim();
      if (statement) statements.push(statement);
      start = index + 1;
    }
  }
  assert.equal(quote, null, '일반 문자열 또는 식별자 인용이 닫혀 있어야 한다');
  const tail = source.slice(start).trim();
  if (tail) statements.push(tail);
  return statements;
}
// 하나의 연결에서 문장별로 전송하여 트랜잭션과 사전 검사도 동일하게 유지되는지 확인한다.
async function executeClientScript(db, sql) {
  for (const statement of splitClientStatements(sql)) await db.query(statement);
}
const one = async (db, sql) => (await db.query(sql)).rows[0];
const failedChecks = async db => (await db.query(verify)).rows.filter(row => row.required && !row.ok);
const ready = async db => assert.deepEqual(await failedChecks(db), []);
const rollbackFailure = async (db, sql, expected) => {
  await assert.rejects(db.exec(sql), expected);
  await db.exec('ROLLBACK');
};
// 기존 완료 컬럼이 없는 업무 스키마를 실제 기반 DDL로 구성한다.
async function base(db, { old = false, schema = 'home' } = {}) {
  assert.ok(['home', 'public'].includes(schema));
  await db.exec('CREATE SCHEMA IF NOT EXISTS ' + schema + '; SET search_path TO ' + schema);
  await db.exec(ddl('account/tb_mbr_acct.sql'));
  await db.exec(ddl('work/tb_work_unit.sql'));
  let todo = ddl('work/tb_wrk_todo.sql');
  if (old) todo = todo.split('\n').filter(line => !line.includes('cmpl_at') && !line.includes('idx_wrk_todo_owner_schedule')).join('\n');
  await db.exec(todo);
}
// 보존 대상의 모든 필드를 비교하여 마이그레이션이 기존 행을 변경하지 않는지 확인한다.
async function snapshot(db, tables, omitCompletion = false) {
  const result = {};
  for (const table of tables) {
    const projection = 'to_jsonb(t)' + (omitCompletion ? " - 'cmpl_at'" : '');
    result[table] = (await db.query('SELECT ' + projection + ' AS row FROM home.' + table + ' t ORDER BY t.ctid')).rows;
  }
  return result;
}
async function seed(db) {
  await db.exec("INSERT INTO home.tb_mbr_acct(mbr_acct_idx,lgn_id,pwd,email,created_by) VALUES(1,'owner','test','owner@example.test','owner');");
  await db.exec("INSERT INTO home.tb_work_unit(work_unit_idx,mbr_acct_idx,ttl,created_by) VALUES(1,1,'기존 업무','owner');");
  await db.exec("INSERT INTO home.tb_wrk_todo(wrk_todo_idx,work_unit_idx,mbr_acct_idx,ttl,sts,created_by,updated_at) VALUES(1,1,1,'완료일 미상','DONE','owner','2025-01-01T00:00:00Z'),(2,1,1,'예약 대상','TODO','owner','2025-01-02T00:00:00Z');");
}
let passed = 0;
async function check(name, action) {
  const db = new PGlite();
  try { await action(db); passed++; console.log('PASS ' + name); }
  finally { await db.close(); }
}
(async () => {
  await check('canonical DDL synchronization and no destructive statements', async () => {
    assert.ok(migrate.includes(completion));
    assert.ok(migrate.includes(notification));
    assert.ok(aggregate.includes(notification));
    assert.ok(aggregate.includes(ddl('work/tb_wrk_todo.sql')));
    assert.doesNotMatch(withoutComments(migrate), /\b(?:DROP|TRUNCATE|DELETE|UPDATE|INSERT)\b/i);
    assert.doesNotMatch(withoutComments(aggregate), /(?:^|;)\s*(?:DROP|TRUNCATE|DELETE)\b/im);
    assert.doesNotMatch(withoutComments(verify), /\b(?:CREATE|ALTER|DROP|TRUNCATE|DELETE|UPDATE|INSERT)\b/i);
  });
  await check('missing schema is reported and rejected without creating objects', async db => {
    assert.equal((await db.query(verify)).rows.length, 78);
    assert.equal((await failedChecks(db)).length, 71);
    await rollbackFailure(db, migrate, /home 스키마가 없습니다/);
    assert.equal((await one(db, "SELECT to_regnamespace('home') AS name")).name, null);
  });
  await check('fresh domain DDL has complete types, indexes and column descriptions', async db => {
    await base(db);
    await db.exec(notification);
    await ready(db);
    const missingComments = (await db.query("SELECT a.attname FROM pg_attribute a WHERE a.attrelid IN ('home.tb_wrk_ntf_stg'::regclass,'home.tb_wrk_todo_rmd'::regclass,'home.tb_wrk_ntf_dlvr'::regclass) AND a.attnum > 0 AND NOT a.attisdropped AND col_description(a.attrelid,a.attnum) IS NULL")).rows;
    assert.deepEqual(missingComments, []);
    await db.exec(migrate);
    await ready(db);
  });
  await check('existing data and unknown completion dates survive migration and rerun', async db => {
    await base(db, { old: true });
    await seed(db);
    // 이번 적용 파일이 참조하지 않아야 하는 보고서·Git 테이블에 보존 확인용 행을 둔다.
    for (const table of archived) await db.exec('CREATE TABLE home.' + table + "(payload TEXT); INSERT INTO home." + table + " VALUES('보존할 기존 데이터');");
    const oldTables = required.slice(0, 3).concat(archived);
    const before = await snapshot(db, oldTables, true);
    await db.exec('SET search_path TO public');
    await db.exec(migrate);
    await ready(db);
    assert.deepEqual(await snapshot(db, oldTables, true), before);
    assert.equal((await one(db, 'SELECT cmpl_at FROM home.tb_wrk_todo WHERE wrk_todo_idx=1')).cmpl_at, null);
    assert.equal((await one(db, "SELECT to_regclass('public.tb_wrk_ntf_stg') AS name")).name, null);
    await db.exec("UPDATE home.tb_wrk_todo SET cmpl_at='2026-10-25T00:00:00Z' WHERE wrk_todo_idx=1;");
    await db.exec("INSERT INTO home.tb_wrk_ntf_stg(mbr_acct_idx,created_by) VALUES(1,'owner'); INSERT INTO home.tb_wrk_todo_rmd(mbr_acct_idx,wrk_todo_idx,send_at,created_by) VALUES(1,2,'2026-10-26T00:00:00Z','owner'); INSERT INTO home.tb_wrk_ntf_dlvr(mbr_acct_idx,wrk_todo_rmd_idx,kind,dedup_key,stg_ver,rmd_ver,due_at,next_try_at,created_by) VALUES(1,1,'REMINDER','reminder:1',1,1,'2026-10-26T00:00:00Z','2026-10-26T00:00:00Z','owner');");
    const after = await snapshot(db, required.concat(archived));
    await db.exec(migrate);
    await ready(db);
    assert.deepEqual(await snapshot(db, required.concat(archived)), after);
    assert.equal((await one(db, 'SELECT dgst_enabled OR rmd_enabled AS enabled FROM home.tb_wrk_ntf_stg')).enabled, false);
  });
  await check('public tables cannot satisfy missing home prerequisites', async db => {
    await base(db, { old: true, schema: 'public' });
    await db.exec('CREATE SCHEMA home');
    await rollbackFailure(db, migrate, /기반 테이블이 없습니다/);
    assert.equal((await one(db, "SELECT to_regclass('home.tb_wrk_ntf_stg') AS name")).name, null);
    assert.equal((await one(db, "SELECT count(*) AS count FROM information_schema.columns WHERE table_schema='public' AND column_name='cmpl_at'")).count, 0);
  });
  await check('later DDL failure rolls back completion column and earlier indexes', async db => {
    await base(db, { old: true });
    await seed(db);
    await db.exec('CREATE TABLE home.tb_wrk_ntf_stg(wrk_ntf_stg_idx BIGSERIAL PRIMARY KEY)');
    const before = await snapshot(db, required.slice(0, 3));
    await rollbackFailure(db, migrate, /does not exist/);
    assert.equal((await one(db, "SELECT count(*) AS count FROM information_schema.columns WHERE table_schema='home' AND column_name='cmpl_at'")).count, 0);
    assert.equal((await one(db, "SELECT to_regclass('home.idx_wrk_todo_owner_schedule') AS name")).name, null);
    assert.deepEqual(await snapshot(db, required.slice(0, 3)), before);
  });
  await check('incompatible existing completion type is detected and not converted', async db => {
    await base(db, { old: true });
    await db.exec('ALTER TABLE home.tb_wrk_todo ADD COLUMN cmpl_at TEXT');
    await rollbackFailure(db, migrate, /타입이 TIMESTAMPTZ가 아닙니다/);
    assert.ok((await failedChecks(db)).some(row => row.object_name === 'tb_wrk_todo.cmpl_at'));
  });
  await check('aggregate creates an empty home schema and refuses to reset existing data', async db => {
    await db.exec(aggregate);
    await ready(db);
    await seed(db);
    const before = await snapshot(db, required);
    await rollbackFailure(db, aggregate, /home 스키마가 비어 있지 않습니다/);
    assert.deepEqual(await snapshot(db, required), before);
    await db.exec(migrate);
    await ready(db);
  });
  await check('verification detects a missing required index', async db => {
    await base(db);
    await db.exec(notification);
    await db.exec('DROP INDEX home.idx_wrk_ntf_dlvr_due');
    assert.deepEqual((await failedChecks(db)).map(row => row.object_name), ['idx_wrk_ntf_dlvr_due']);
  });
  await check('legacy dollar block reproduces reported client splitting error', async db => {
    const legacy = 'DO $$\nDECLARE\n    missing_tables TEXT;\nBEGIN\n    PERFORM 1;\nEND $$;';
    await assert.rejects(db.query(splitClientStatements(legacy)[0]), /unterminated dollar-quoted string/);
  });
  await check('migration and rerun work through a client without dollar quoting support', async db => {
    assert.doesNotMatch(migrate, /\$\$/);
    await base(db, { old: true });
    await seed(db);
    const before = await snapshot(db, required.slice(0, 3), true);
    await executeClientScript(db, migrate);
    await ready(db);
    assert.deepEqual(await snapshot(db, required.slice(0, 3), true), before);
    const after = await snapshot(db, required);
    await executeClientScript(db, migrate);
    assert.deepEqual(await snapshot(db, required), after);
  });
  await check('aggregate preserves its existing-data guard with client statement splitting', async db => {
    assert.doesNotMatch(aggregate, /\$\$/);
    await executeClientScript(db, aggregate);
    await ready(db);
    await seed(db);
    const before = await snapshot(db, required);
    await assert.rejects(executeClientScript(db, aggregate), /home 스키마가 비어 있지 않습니다/);
    await db.exec('ROLLBACK');
    assert.deepEqual(await snapshot(db, required), before);
  });
  console.log('Renewal schema scenarios passed: ' + passed);
})().catch(error => { console.error(error); process.exitCode = 1; });

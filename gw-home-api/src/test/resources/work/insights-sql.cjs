// 실제 MyBatis 바인딩 SQL을 격리된 PostgreSQL 엔진에서 검증한다. 운영 DB에는 연결하지 않는다.
const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const { PGlite } = require(path.resolve(process.argv[2]));
const root = path.resolve(__dirname, '../../../../..');
const statements = JSON.parse(fs.readFileSync(path.join(root, 'gw-home-api/build/insights-sql.json'), 'utf8'));
const isoDate = value => value instanceof Date ? value.toISOString().slice(0,10) : String(value).slice(0,10);
(async () => {
  const db = new PGlite();
  const defaults = {
    req: { mbrAcctIdx: 7, today: '2026-10-25', group: 'ALL', size: 100, offset: 0, workUnitUuid: '11111111-1111-4111-8111-111111111111', keyword: '검색', fromDate: '2026-10-01', toDate: '2026-10-31' },
    fromDate: '2026-10-01', toDate: '2026-10-31', fromInstant: '2026-10-01T00:00:00+09:00', beforeInstant: '2026-11-01T00:00:00+09:00', timeZone: 'Asia/Seoul', bucket: 'day'
  };
  const run = (method, variant = 'active', overrides = {}) => {
    const params = { ...defaults, ...overrides, req: { ...defaults.req, ...overrides.req } };
    const statement = statements[variant + '.' + method]; const values = []; let index = 0;
    const sql = statement.sql.replace(/\?/g, () => {
      const binding = statement.bindings[index++];
      values.push(binding.property.split('.').reduce((value, key) => value?.[key], params) ?? null);
      const cast = binding.type === 'java.time.LocalDate' ? '::date' : binding.type === 'java.time.OffsetDateTime' ? '::timestamptz' : '';
      return '$' + index + cast;
    });
    return db.query(sql, values);
  };
  await db.exec('CREATE TABLE tb_mbr_acct(mbr_acct_idx BIGINT PRIMARY KEY); INSERT INTO tb_mbr_acct VALUES (7),(8);');
  for (const file of ['tb_work_unit.sql','tb_wrk_todo.sql']) await db.exec(fs.readFileSync(path.join(root,'gw-home-infra-db/src/main/resources/sql/ddl/work',file),'utf8'));
  await db.exec("INSERT INTO tb_work_unit(work_unit_idx,work_unit_uuid,mbr_acct_idx,ttl,created_by,use_yn) VALUES (1,'11111111-1111-4111-8111-111111111111',7,'내 업무','me','Y'),(2,'22222222-2222-4222-8222-222222222222',8,'타인','other','Y'),(3,'33333333-3333-4333-8333-333333333333',7,'비활성','me','N'),(4,'44444444-4444-4444-8444-444444444444',7,'빈 업무','me','Y');");
  await db.exec("INSERT INTO tb_wrk_todo(wrk_todo_idx,work_unit_idx,mbr_acct_idx,ttl,created_by,prnt_wrk_todo_idx,sts,strt_dt,due_dt,cmpl_at) VALUES " +
    "(15,1,7,'조상','me',NULL,'DONE','2026-09-01','2026-09-15','2026-10-15T00:00Z')," +
    "(1,1,7,'상위','me',15,'DONE','2026-09-01','2026-09-30','2026-10-15T00:00Z')," +
    "(2,1,7,'시작 경계 검색','me',1,'DONE','2026-10-01','2026-10-01','2026-09-30T15:00Z')," +
    "(3,1,7,'지연','me',1,'TODO','2026-10-01','2026-10-24',NULL)," +
    "(4,1,7,'완료일 미상','me',NULL,'DONE',NULL,NULL,NULL)," +
    "(5,1,7,'일정 없음','me',NULL,'TODO',NULL,NULL,NULL)," +
    "(6,1,7,'시작만','me',NULL,'TODO','2026-10-20',NULL,NULL)," +
    "(7,1,7,'마감만','me',NULL,'TODO',NULL,'2026-10-27',NULL)," +
    "(8,1,7,'기간 직전','me',NULL,'DONE','2026-10-01','2026-10-01','2026-09-30T14:59:59Z')," +
    "(9,1,7,'다음 기간 경계','me',NULL,'DONE','2026-10-01','2026-10-30','2026-10-31T15:00Z')," +
    "(10,3,7,'비활성 완료','me',NULL,'DONE',NULL,NULL,'2026-10-15T00:00Z')," +
    "(11,2,8,'타인 완료','other',NULL,'DONE',NULL,NULL,'2026-10-15T00:00Z')," +
    "(12,1,7,'삭제 완료','me',NULL,'DONE',NULL,NULL,'2026-10-15T00:00Z')," +
    "(13,1,7,'기간 가로지름','me',NULL,'TODO','2026-09-01','2026-11-01',NULL)," +
    "(14,1,7,'삭제된 자식','me',5,'TODO',NULL,NULL,NULL); UPDATE tb_wrk_todo SET del_at=now() WHERE wrk_todo_idx IN (12,14);");
  let passed = 0;
  const check = async (name, action) => {
    await db.exec('BEGIN');
    try { await action(); passed++; console.log('PASS ' + name); }
    finally { await db.exec('ROLLBACK'); }
  };
  await check('owner, active, deleted and leaf counts agree with inbox', async () => {
    const unit = (await run('selectUnitInsights')).rows[0];
    const inbox = (await run('selectMyWorkTodoSummary')).rows[0];
    for (const key of ['total_count','today_count','upcoming_count','overdue_count','completed_count','unscheduled_count','unknown_completion_count']) assert.equal(Number(unit[key]),Number(inbox[key]),key);
    assert.equal(Number(unit.total_count),9); assert.equal(Number(unit.completed_count),4);
    assert.equal(Number(unit.unknown_completion_count),1); assert.equal(Number(unit.period_completed_count),1);
    assert.equal((await run('selectUnitInsights','all')).rows.length,2);
    assert.equal((await run('selectUnitInsights','unit',{req:{workUnitUuid:'22222222-2222-4222-8222-222222222222'}})).rows.length,0);
  });
  await check('completion timezone and half-open period boundaries', async () => {
    let rows = (await run('selectCompletionBuckets')).rows;
    assert.equal(rows.length,1); assert.equal(Number(rows[0].completed_count),1);
    assert.equal(isoDate(rows[0].bucket_start),'2026-10-01');
    rows = (await run('selectCompletionBuckets','active',{timeZone:'UTC',fromInstant:'2026-10-01T00:00Z',beforeInstant:'2026-11-01T00:00Z'})).rows;
    assert.equal(Number(rows[0].completed_count),1); assert.equal(isoDate(rows[0].bucket_start),'2026-10-31');
  });
  await check('gantt includes ancestors, undated and one-sided rows without duplicates', async () => {
    const rows = (await run('selectGanttRows')).rows;
    assert.equal(rows.length,11); assert.equal(new Set(rows.map(row=>row.uuid)).size,11);
    assert.equal(rows.filter(row=>row.context_only).length,2);
    assert(rows.some(row=>row.ttl==='일정 없음')); assert(rows.some(row=>row.ttl==='기간 가로지름'));
    assert.equal((await run('selectGanttRows','unit',{req:{workUnitUuid:'22222222-2222-4222-8222-222222222222'}})).rows.length,0);
  });
  await check('cancellation and recompletion move only the current valid completion', async () => {
    await db.exec("UPDATE tb_wrk_todo SET sts='TODO',cmpl_at=NULL WHERE wrk_todo_idx=2");
    assert.equal((await run('selectCompletionBuckets')).rows.length,0);
    await db.exec("UPDATE tb_wrk_todo SET sts='DONE',cmpl_at='2026-10-20T15:00Z' WHERE wrk_todo_idx=2");
    assert.equal(isoDate((await run('selectCompletionBuckets')).rows[0].bucket_start),'2026-10-21');
  });
  await check('leaf change removes parent from current completion statistics', async () => {
    await db.exec("INSERT INTO tb_wrk_todo(wrk_todo_idx,work_unit_idx,mbr_acct_idx,prnt_wrk_todo_idx,ttl,created_by) VALUES(20,1,7,2,'새 자식','me')");
    assert.equal((await run('selectCompletionBuckets')).rows.length,0);
    assert.equal(Number((await run('selectUnitInsights')).rows[0].total_count),9);
  });
  await check('Monday week and calendar month buckets', async () => {
    assert.equal(isoDate((await run('selectCompletionBuckets','active',{bucket:'week'})).rows[0].bucket_start),'2026-09-28');
    assert.equal(isoDate((await run('selectCompletionBuckets','active',{bucket:'month'})).rows[0].bucket_start),'2026-10-01');
  });
  await check('shared source keeps inbox keyword, schedule and paging contracts', async () => {
    assert.equal(Number((await run('selectMyWorkTodoCount','keyword')).rows[0].count),1);
    const summary = (await run('selectMyWorkTodoSummary','scheduled')).rows[0];
    assert.equal(Number(summary.total_count),7);
    const rows = (await run('selectMyWorkTodos','scheduled')).rows;
    assert.equal(rows.length,7); assert.equal(new Set(rows.map(row=>row.uuid)).size,7);
    assert.equal((await run('selectMyWorkTodos','active',{req:{mbrAcctIdx:999}})).rows.length,0);
  });
  await check('empty and deleted work units do not leak rows', async () => {
    assert.equal((await run('selectUnitInsights','unit',{req:{workUnitUuid:'44444444-4444-4444-8444-444444444444'}})).rows.length,0);
    await db.exec('UPDATE tb_work_unit SET del_at=now() WHERE work_unit_idx=1');
    assert.equal((await run('selectGanttRows')).rows.length,0);
    assert.equal((await run('selectCompletionBuckets')).rows.length,0);
  });
  console.log(passed + ' PostgreSQL insight scenarios passed');
  await db.close();
})().catch(error => { console.error(error); process.exit(1); });

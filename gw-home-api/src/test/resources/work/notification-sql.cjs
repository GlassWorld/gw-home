// MyBatis가 실제로 생성한 SQL을 격리된 PostgreSQL WASM에서 검증한다. 운영 DB에는 연결하지 않는다.
const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const { PGlite } = require(path.resolve(process.argv[2]));
const root = path.resolve(__dirname, '../../../../..');
const statements = JSON.parse(fs.readFileSync(path.join(root, 'gw-home-api/build/notification-sql.json'), 'utf8'));
const ddl = name => fs.readFileSync(path.join(root, 'gw-home-infra-db/src/main/resources/sql/ddl/work', name), 'utf8');
const camel = row => Object.fromEntries(Object.entries(row).map(([key, value]) => [key.replace(/_([a-z])/g, (_, letter) => letter.toUpperCase()), value]));
(async () => {
  const db = new PGlite();
  const run = (name, params = {}) => {
    const statement = statements[name]; const values = []; let index = 0;
    const sql = statement.sql.replace(/\?/g, () => {
      const binding = statement.bindings[index++];
      const value = binding.property.split('.').reduce((object, key) => object?.[key], params);
      values.push(value ?? null);
      // JDBC의 LocalDate 타입 바인딩과 같은 형식으로 파라미터 타입을 지정한다.
      return binding.property === 'req.today' ? '$' + index + '::date' : '$' + index;
    });
    return db.query(sql, values);
  };
  await db.exec("CREATE TABLE tb_mbr_acct(mbr_acct_idx BIGINT PRIMARY KEY,acct_stat TEXT DEFAULT 'ACTIVE',del_at TIMESTAMPTZ); INSERT INTO tb_mbr_acct(mbr_acct_idx) VALUES(7),(8);");
  await db.exec(ddl('tb_work_unit.sql')); await db.exec(ddl('tb_wrk_todo.sql'));
  await db.exec(ddl('tb_wrk_ntf.sql')); await db.exec(ddl('tb_wrk_ntf.sql'));
  await db.exec("INSERT INTO tb_work_unit(work_unit_idx,mbr_acct_idx,ttl,created_by) VALUES(1,7,'mine','me'),(2,8,'other','other');");
  await db.exec("INSERT INTO tb_wrk_todo(wrk_todo_idx,work_unit_idx,mbr_acct_idx,ttl,created_by,prnt_wrk_todo_idx,sts) VALUES(1,1,7,'leaf','me',NULL,'TODO'),(2,1,7,'parent','me',NULL,'TODO'),(3,1,7,'child','me',2,'TODO'),(4,2,8,'other','other',NULL,'TODO'),(5,1,7,'done','me',NULL,'DONE'),(6,1,7,'deleted','me',NULL,'TODO'); UPDATE tb_wrk_todo SET del_at=now() WHERE wrk_todo_idx=6;");
  await run('insertDefaultSetting', { owner: 7, email: 'me@example.test', actor: 'me' });
  await run('insertDefaultSetting', { owner: 7, email: 'ignored@example.test', actor: 'me' });
  let setting = camel((await run('selectSetting', { owner: 7 })).rows[0]);
  assert.equal(setting.rcptEmail, 'me@example.test');
  Object.assign(setting, { vrfdAt: new Date(), rmdEnabled: true, dgstEnabled: true, updatedBy: 'me', nextDgstAt: new Date(Date.now() - 60000) });
  await run('updateSetting', { s: setting });
  const sync = async () => { for (const name of ['updateInvalidReminders', 'updateChangedSchedules', 'updateInvalidDeliveries', 'insertReminderDeliveries']) await run(name, { owner: 7, actor: 'me' }); };
  const seed = async (todoId = 1, sendAt = new Date(Date.now() + 3600000)) => {
    await run('insertReminder', { r: { mbrAcctIdx: 7, wrkTodoIdx: todoId, sendAt, createdBy: 'me' } });
    await sync();
    return (await run('selectReminder', { owner: 7, todoId })).rows[0];
  };
  const deliveries = async () => (await run('selectHistory', { owner: 7, size: 100, offset: 0 })).rows;
  let passed = 0;
  const check = async (name, action) => {
    await db.exec('BEGIN');
    try { await action(); passed++; console.log('PASS ' + name); }
    finally { await db.exec('ROLLBACK'); }
  };
  await check('owner isolation and composite foreign keys', async () => {
    const other = (await db.query('SELECT wrk_todo_uuid FROM tb_wrk_todo WHERE wrk_todo_idx=4')).rows[0].wrk_todo_uuid;
    assert.equal((await run('selectTarget', { owner: 7, todoUuid: other })).rows.length, 0);
    assert.equal((await run('selectOwnerLock', { owner: 7 })).rows[0].int8, 7);
    await assert.rejects(seed(4), /foreign key/);
  });
  await check('repeated synchronization creates exactly one delivery', async () => {
    await seed(); await sync(); await sync();
    assert.equal((await deliveries()).length, 1);
    assert.equal(Number((await run('selectHistoryCount', { owner: 8 })).rows[0].count), 0);
  });
  for (const [name, change] of [
    ['completion', "UPDATE tb_wrk_todo SET sts='DONE' WHERE wrk_todo_idx=1"],
    ['deletion', "UPDATE tb_wrk_todo SET del_at=now() WHERE wrk_todo_idx=1"],
    ['parent transition', "UPDATE tb_wrk_todo SET prnt_wrk_todo_idx=1 WHERE wrk_todo_idx=3"],
    ['inactive work unit', "UPDATE tb_work_unit SET use_yn='N' WHERE work_unit_idx=1"]
  ]) await check(name + ' cancels reservation and pending delivery', async () => {
    await seed(); await db.exec(change); await sync();
    assert.equal((await run('selectReminder', { owner: 7, todoId: 1 })).rows[0].enabled, false);
    assert.equal((await deliveries())[0].sts, 'CANCELLED');
    if (name === 'completion') {
      await db.exec("UPDATE tb_wrk_todo SET sts='TODO' WHERE wrk_todo_idx=1"); await sync();
      assert.equal((await deliveries()).length, 1); assert.equal((await deliveries())[0].sts, 'CANCELLED');
    }
  });
  await check('date changes replace future version without shifting requested instant', async () => {
    const before = await seed();
    await db.exec("UPDATE tb_wrk_todo SET due_dt=current_date+3 WHERE wrk_todo_idx=1"); await sync();
    const after = (await run('selectReminder', { owner: 7, todoId: 1 })).rows[0];
    assert.equal(after.send_at.getTime(), before.send_at.getTime()); assert.equal(after.ver, before.ver + 1);
    assert.deepEqual(new Set((await deliveries()).map(item => item.sts)), new Set(['PENDING', 'CANCELLED']));
  });
  await check('settings versions invalidate old delivery and opt-out cancels all', async () => {
    await seed(); setting.ver = 2; await run('updateSetting', { s: setting });
    await run('updateReminderVersions', { owner: 7, disable: false, actor: 'me' }); await sync();
    assert.equal((await deliveries()).filter(item => item.sts === 'PENDING').length, 1);
    await run('updateReminderVersions', { owner: 7, disable: true, actor: 'me' }); await sync();
    assert((await deliveries()).every(item => item.sts === 'CANCELLED')); setting.ver = 1;
  });
  await check('claims are exclusive and old token cannot start sending', async () => {
    await seed(1, new Date(Date.now() - 60000));
    const first = (await run('selectClaimDeliveries')).rows[0];
    assert.equal((await run('selectClaimDeliveries')).rows.length, 0);
    await db.exec("UPDATE tb_wrk_ntf_dlvr SET lease_until=now()-interval '1 minute'"); await run('updateExpiredClaims');
    const second = (await run('selectClaimDeliveries')).rows[0];
    assert.notEqual(second.claim_token, first.claim_token);
    assert.equal((await run('updateStartSending', { id: first.idx, token: first.claim_token })).affectedRows, 0);
    assert.equal((await run('updateStartSending', { id: second.idx, token: second.claim_token })).affectedRows, 1);
    await run('updateDeliveryResult', { id: second.idx, token: second.claim_token, status: 'RETRY', code: 'SMTP_TEMPORARY', nextTry: new Date(Date.now() + 60000) });
    assert.equal((await run('selectClaimDeliveries')).rows.length, 0);
    const row = (await run('selectDelivery', { id: second.idx })).rows[0]; assert.equal(row.attempts, 1); assert.equal(row.sts, 'RETRY');
  });
  await check('interrupted sending is UNKNOWN and never claimed again', async () => {
    await seed(1, new Date(Date.now() - 60000));
    const claim = (await run('selectClaimDeliveries')).rows[0];
    await run('updateStartSending', { id: claim.idx, token: claim.claim_token });
    await db.exec("UPDATE tb_wrk_ntf_dlvr SET lease_until=now()-interval '1 minute'"); await run('updateUnknownSends');
    assert.equal((await deliveries())[0].sts, 'UNKNOWN'); assert.equal((await run('selectClaimDeliveries')).rows.length, 0);
  });
  await check('explicit cancellation fences an already claimed delivery', async () => {
    await seed(1, new Date(Date.now() - 60000));
    const claim = (await run('selectClaimDeliveries')).rows[0];
    await run('updateCancelReminder', { owner: 7, todoId: 1, actor: 'me' }); await sync();
    assert.equal((await run('updateStartSending', { id: claim.idx, token: claim.claim_token })).affectedRows, 0);
    assert.equal((await deliveries())[0].sts, 'CANCELLED');
  });
  await check('expired delivery and disabled account cannot be sent', async () => {
    await seed(1, new Date(Date.now() - 90000000)); await run('updateExpiredDeliveries');
    assert.equal((await deliveries())[0].result_cd, 'EXPIRED');
    await db.exec("UPDATE tb_mbr_acct SET acct_stat='INACTIVE' WHERE mbr_acct_idx=7");
    assert.equal((await run('selectActiveAccount', { owner: 7 })).rows[0].exists, false);
  });
  await check('digest is unique per local day and shared leaf groups exclude other owners', async () => {
    assert.equal((await run('selectDueSettings')).rows.length, 1);
    const params = { s: setting, key: 'D:2026-10-25', dueAt: new Date(Date.now() - 60000) };
    await run('insertDigest', params); await run('insertDigest', params); assert.equal((await deliveries()).length, 1);
    const claim = (await run('selectClaimDeliveries')).rows[0]; await run('updateStartSending', { id: claim.idx, token: claim.claim_token });
    await run('updateDeliveryResult', { id: claim.idx, token: claim.claim_token, status: 'SENT', code: 'SMTP_ACCEPTED' });
    await run('insertDigest', params); assert.equal((await deliveries())[0].sts, 'SENT');
    await db.exec("UPDATE tb_wrk_todo SET strt_dt='2026-10-25',due_dt='2026-10-25'; UPDATE tb_wrk_todo SET due_dt='2026-10-24' WHERE wrk_todo_idx=3;");
    const todos = (await run('selectDigestTodos', { req: { mbrAcctIdx: 7, today: '2026-10-25' } })).rows;
    assert.deepEqual(new Set(todos.map(row => row.idx)), new Set([1, 3]));
    assert.deepEqual(new Set(todos.map(row => row.todo_group)), new Set(['TODAY', 'OVERDUE']));
  });
  await db.close(); console.log('PASS ' + passed + ' PostgreSQL notification scenarios (isolated PGlite).');
})().catch(error => { console.error(error); process.exitCode = 1; });

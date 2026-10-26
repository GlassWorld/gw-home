// 실제 MyBatis SQL을 격리된 PostgreSQL 엔진에서 실행한다.
const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const { PGlite } = require(path.resolve(process.argv[2]));
const root = path.resolve(__dirname, '../../../../..');
const read = file => fs.readFileSync(path.join(root, file), 'utf8').replace(/\r\n/g, '\n').trim();
const statements = JSON.parse(read('gw-home-api/build/life-daily-sql.json'));
const ddl = read('gw-home-infra-db/src/main/resources/sql/ddl/life/tb_lf_daily.sql');
const migration = read('gw-home-infra-db/src/main/resources/sql/ddl/life/life-daily-migrate.sql');
const uuid = 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa';
(async () => {
  const database = new PGlite();
  try {
    assert(migration.includes(ddl)); assert(read('docs/all-ddl.sql').includes(ddl));
    await database.exec('CREATE SCHEMA home; SET search_path TO home; CREATE TABLE tb_mbr_acct(mbr_acct_idx BIGINT PRIMARY KEY, del_at TIMESTAMPTZ); INSERT INTO tb_mbr_acct VALUES (7,NULL),(8,NULL);');
    await database.exec(migration); await database.exec(migration);
    const defaults = { owner: 7, mbrAcctIdx: 7, uuid, ttl: '산책', startDt: '2024-02-28', start: '2024-02-01', end: '2024-02-29', date: '2024-02-29', today: '2024-02-29', completed: true, createdBy: 'me', updatedBy: 'me', loginId: 'me', title: '산책 수정' };
    const run = (method, overrides = {}) => {
      const params = { ...defaults, ...overrides }; const values = []; let index = 0;
      const statement = statements[method];
      const sql = statement.sql.replace(/\?/g, () => {
        const key = statement.bindings[index++]; values.push(params[key] ?? null);
        const cast = ['start', 'end', 'date', 'today', 'startDt'].includes(key) ? '::date' : key === 'completed' ? '::boolean' : '';
        return `$${index}${cast}`;
      });
      return database.query(sql, values);
    };
    await run('insertItem');
    assert.equal((await run('selectItems')).rows.length, 1);
    assert.equal((await run('selectItems', { owner: 8 })).rows.length, 0);
    assert.equal((await run('selectItem', { owner: 8 })).rows.length, 0);
    assert.equal((await run('saveCheck', { owner: 8 })).affectedRows, 0);
    assert.equal((await run('updateTitle', { owner: 8 })).affectedRows, 0);
    assert.equal((await run('endItem', { owner: 8 })).affectedRows, 0);
    console.log('PASS owner isolation across all mutations and reads');
    await run('saveCheck'); await run('saveCheck');
    assert.equal((await database.query('SELECT count(*)::int AS count FROM tb_lf_daily_check')).rows[0].count, 1);
    await run('saveCheck', { date: '2024-02-28' });
    await run('saveCheck', { completed: false });
    assert.deepEqual((await run('selectChecks')).rows.map(row => row.check_dt.toISOString().slice(0, 10)), ['2024-02-28']);
    await run('saveCheck');
    assert.equal((await run('selectChecks')).rows.length, 2);
    console.log('PASS repeated save, independent dates, cancel and restore');
    assert.equal((await run('saveCheck', { date: '2024-02-27' })).affectedRows, 0);
    await run('endItem');
    assert.equal((await run('selectChecks')).rows.length, 2);
    assert.equal((await run('saveCheck', { date: '2024-03-01' })).affectedRows, 0);
    assert.equal((await run('selectItems', { start: '2024-03-01', end: '2024-03-31' })).rows.length, 0);
    assert.equal((await run('updateTitle')).affectedRows, 0);
    await run('saveCheck', { date: '2024-02-28', completed: false });
    assert.equal((await run('selectChecks')).rows.length, 1);
    console.log('PASS leap day, start/end boundary and preserved editable history');
    await assert.rejects(database.query("INSERT INTO tb_lf_daily_check(lf_daily_idx,mbr_acct_idx,check_dt,created_by) VALUES(1,8,'2024-02-28','other')"));
    console.log('PASS ownership foreign key, migration repeatability and DDL sync');
  } finally { await database.close(); }
})().catch(error => { console.error(error); process.exitCode = 1; });

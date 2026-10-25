// 실제 MyBatis SQL과 배포 스크립트를 격리된 PostgreSQL 엔진에서 검증한다.
const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const { PGlite } = require(path.resolve(process.argv[2]));
const root = path.resolve(__dirname, '../../../../..');
const read = file => fs.readFileSync(path.join(root, file), 'utf8').replace(/\r\n/g, '\n').trim();
const statements = JSON.parse(read('gw-home-api/build/life-todo-sql.json'));
const ddl = read('gw-home-infra-db/src/main/resources/sql/ddl/life/tb_lf_todo.sql');
const migration = read('gw-home-infra-db/src/main/resources/sql/ddl/life/life-todos-migrate.sql');
const category = 'bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb';
const foreignCategory = 'cccccccc-cccc-4ccc-8ccc-cccccccccccc';
const uuid = index => `aaaaaaaa-aaaa-4aaa-8aaa-${String(index).padStart(12, '0')}`;

(async () => {
  const db = new PGlite();
  let passed = 0;
  try {
    assert(migration.includes(ddl), '마이그레이션은 기준 DDL과 같아야 한다');
    assert(read('docs/all-ddl.sql').includes(ddl), '집계 DDL은 최신 스키마를 포함해야 한다');
    await db.exec('CREATE SCHEMA home; SET search_path TO home; CREATE TABLE tb_mbr_acct(mbr_acct_idx BIGINT PRIMARY KEY, del_at TIMESTAMPTZ); INSERT INTO tb_mbr_acct VALUES (7,NULL),(8,NULL);');
    await db.exec(migration);
    const defaults = { req: { mbrAcctIdx: 7, today: '2026-10-25', group: 'ALL', size: 20, offset: 0 },
      owner: 7, today: '2026-10-25', uuid: uuid(1), mbrAcctIdx: 7, lfTodoCatIdx: 1,
      ttl: '새 할 일', memo: null, dueDt: null, createdBy: 'me', updatedBy: 'me', loginId: 'me',
      completed: true, name: '집안일', nm: '집안일', exceptUuid: null, category: 1 };
    const run = (method, variant = 'all', overrides = {}) => {
      const parameters = { ...defaults, ...overrides, req: { ...defaults.req, ...overrides.req } };
      parameters.today = parameters.req.today;
      const statement = statements[`${variant}.${method}`];
      let index = 0;
      const values = [];
      const sql = statement.sql.replace(/\?/g, () => {
        const binding = statement.bindings[index++];
        values.push(binding.property.split('.').reduce((value, key) => value?.[key], parameters) ?? null);
        const cast = ['today', 'req.today', 'dueDt'].includes(binding.property) ? '::date' : binding.property === 'completed' ? '::boolean' : '';
        return `$${index}${cast}`;
      });
      return db.query(sql, values);
    };
    const check = async (name, action) => {
      await db.exec('BEGIN');
      try { await action(); passed++; console.log('PASS ' + name); }
      finally { await db.exec('ROLLBACK'); }
    };
    await run('insertCategory', 'all', { uuid: category });
    await run('insertCategory', 'all', { uuid: foreignCategory, mbrAcctIdx: 8, createdBy: 'other' });
    for (const [index, title, due, owner, categoryId] of [
      [1, '장보기 10%', '2026-10-25', 7, 1], [2, '청소', '2026-10-24', 7, 1],
      [3, '병원 예약', '2026-10-26', 7, 1], [4, '완료 항목', '2026-10-24', 7, 1],
      [5, '언젠가 할 일', null, 7, null], [6, '삭제 항목', '2026-10-25', 7, 1], [7, '다른 사용자', '2026-10-25', 8, 2]
    ]) await run('insertTodo', 'all', { uuid: uuid(index), ttl: title, dueDt: due, mbrAcctIdx: owner, lfTodoCatIdx: categoryId });
    await db.exec(`UPDATE tb_lf_todo SET cmpl_at='2026-10-23T12:00:00Z' WHERE lf_todo_uuid='${uuid(4)}'; UPDATE tb_lf_todo SET del_at=now() WHERE lf_todo_uuid='${uuid(6)}';`);

    await check('owner isolation, deleted exclusion and all five date groups', async () => {
      const summary = (await run('selectSummary')).rows[0];
      assert.equal(Number(summary.total_count), 5);
      for (const key of ['today_count', 'upcoming_count', 'overdue_count', 'completed_count', 'unscheduled_count']) assert.equal(Number(summary[key]), 1, key);
      const todos = (await run('selectTodos')).rows;
      assert.equal(todos.length, 5); assert.equal(todos.at(-1).todo_group, 'COMPLETED');
      assert(todos.every(todo => todo.mbr_acct_idx === 7));
      assert.equal((await run('selectTodo', 'all', { uuid: uuid(7) })).rows.length, 0);
    });
    await check('date filter and summary use the same source without hiding other counts', async () => {
      const parameters = { req: { group: 'TODAY' } };
      assert.equal((await run('selectTodos', 'today', parameters)).rows[0].uuid, uuid(1));
      assert.equal(Number((await run('selectCount', 'today', parameters)).rows[0].count), 1);
      assert.equal(Number((await run('selectSummary', 'today', parameters)).rows[0].total_count), 5);
    });
    await check('category, uncategorized and literal keyword filters', async () => {
      assert.equal((await run('selectTodos', 'category', { req: { categoryUuid: category } })).rows.length, 4);
      assert.equal((await run('selectTodos', 'category', { req: { categoryUuid: foreignCategory } })).rows.length, 0);
      assert.equal((await run('selectTodos', 'uncategorized')).rows[0].uuid, uuid(5));
      assert.equal((await run('selectTodos', 'keyword', { req: { keyword: '%' } })).rows.length, 1);
    });
    await check('paging is stable and count is not limited by page size', async () => {
      const all = (await run('selectTodos')).rows;
      const page = (await run('selectTodos', 'all', { req: { size: 2, offset: 2 } })).rows;
      assert.deepEqual(page.map(todo => todo.uuid), all.slice(2, 4).map(todo => todo.uuid));
      assert.equal(Number((await run('selectCount', 'all', { req: { size: 2, offset: 2 } })).rows[0].count), 5);
    });
    await check('completion is idempotent, undo clears it and recompletion records a new time', async () => {
      const completed = await run('selectTodo', 'all', { uuid: uuid(4) });
      await run('updateCompletion', 'all', { uuid: uuid(4) });
      const repeated = (await run('selectTodo', 'all', { uuid: uuid(4) })).rows[0];
      assert.equal(String(repeated.cmpl_at), String(completed.rows[0].cmpl_at));
      await run('updateCompletion', 'all', { uuid: uuid(4), completed: false });
      assert.equal((await run('selectTodo', 'all', { uuid: uuid(4) })).rows[0].cmpl_at, null);
      await run('updateCompletion', 'all', { uuid: uuid(4) });
      assert.notEqual(String((await run('selectTodo', 'all', { uuid: uuid(4) })).rows[0].cmpl_at), String(repeated.cmpl_at));
    });
    await check('editing preserves completion and can remove category and date', async () => {
      await run('updateTodo', 'all', { uuid: uuid(4), ttl: '수정', lfTodoCatIdx: null, dueDt: null, memo: '메모' });
      const todo = (await run('selectTodo', 'all', { uuid: uuid(4) })).rows[0];
      assert.equal(todo.ttl, '수정'); assert.equal(todo.category_uuid, null); assert.equal(todo.due_dt, null);
      assert.equal(todo.todo_group, 'COMPLETED'); assert(todo.cmpl_at);
    });
    await check('foreign mutations cannot change another users todos or categories', async () => {
      await run('updateTodo', 'all', { uuid: uuid(7) });
      await run('updateCompletion', 'all', { uuid: uuid(7) });
      await run('deleteTodo', 'all', { uuid: uuid(7) });
      await run('updateCategory', 'all', { uuid: foreignCategory, nm: '탈취' });
      await run('deleteCategory', 'all', { uuid: foreignCategory });
      assert.equal((await run('selectTodo', 'all', { uuid: uuid(7), owner: 8 })).rows[0].ttl, '다른 사용자');
      assert.equal((await run('selectCategory', 'all', { uuid: foreignCategory, owner: 8 })).rows[0].nm, '집안일');
    });
    await check('deleting category keeps todos and completion as uncategorized', async () => {
      await run('lockOwner'); await run('clearCategory'); await run('deleteCategory', 'all', { uuid: category });
      assert.equal((await run('selectCategories')).rows.length, 0);
      const todos = (await run('selectTodos', 'uncategorized')).rows;
      assert.equal(todos.length, 5); assert(todos.every(todo => todo.category_uuid === null));
      assert.equal(todos.filter(todo => todo.cmpl_at).length, 1);
    });
    await check('duplicate category detection supports rename and other owners', async () => {
      assert.equal((await run('categoryNameExists')).rows[0].exists, true);
      assert.equal((await run('categoryNameExists', 'rename', { exceptUuid: category })).rows[0].exists, false);
      await run('updateCategory', 'all', { uuid: category, nm: '가사' });
      assert.equal((await run('selectCategory', 'all', { uuid: category })).rows[0].nm, '가사');
    });
    await check('database rejects a foreign category even without service validation', async () => {
      await assert.rejects(run('insertTodo', 'all', { uuid: uuid(20), lfTodoCatIdx: 2 }), /foreign key/);
    });
    await check('soft deletion removes the item from every read and summary', async () => {
      await run('deleteTodo');
      assert.equal((await run('selectTodo')).rows.length, 0);
      assert.equal(Number((await run('selectSummary')).rows[0].total_count), 4);
    });
    const before = (await run('selectTodos')).rows;
    await db.exec(migration);
    assert.deepEqual((await run('selectTodos')).rows, before);
    console.log(`PASS migration rerun preserves data; ${passed + 1} scenarios passed`);
  } finally { await db.close(); }
})().catch(error => { console.error(error); process.exitCode = 1; });

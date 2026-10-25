// 실제 MyBatis SQL을 격리 PostgreSQL 엔진에서 실행하여 검색·삭제·페이징 계약을 검증한다.
const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const { PGlite } = require(path.resolve(process.argv[2]));
const root = path.resolve(__dirname, '../../../../..');
const statements = JSON.parse(fs.readFileSync(path.join(root, 'gw-home-api/build/list-refactor-sql.json'), 'utf8'));

(async () => {
  const db = new PGlite();
  try {
    for (const file of ['board/tb_brd_ctgr.sql', 'board/tb_brd_pst.sql', 'profile/tb_mbr_prfl.sql',
      'favorite/tb_fav.sql', 'comment/tb_brd_cmt.sql', 'notice/tb_ntc.sql',
      'account/tb_mbr_acct.sql', 'account/tb_mbr_acct_add_otp.sql', 'account/tb_mbr_acct_add_otp_required.sql',
      'file/tb_file.sql', 'board/tb_brd_pst_file.sql']) {
      let ddl = fs.readFileSync(path.join(root, 'gw-home-infra-db/src/main/resources/sql/ddl', file), 'utf8');
      // 기존 OTP 스크립트에는 적용·롤백 SQL이 함께 있으므로 적용 구문만 fixture에 반영한다.
      if (file.includes('add_otp')) ddl = ddl.split(/--[^\r\n]*롤백/)[0];
      await db.exec(ddl);
    }
    await db.exec(`
      INSERT INTO tb_brd_ctgr(brd_ctgr_idx,brd_ctgr_uuid,nm,created_by) VALUES
        (1,'10000000-0000-0000-0000-000000000001','one','writer'),
        (2,'10000000-0000-0000-0000-000000000002','two','writer');
      INSERT INTO tb_mbr_prfl(mbr_acct_idx,nick_nm,created_by) VALUES (10,'needle author','writer'),(20,'other','writer');
      INSERT INTO tb_mbr_acct(mbr_acct_idx,lgn_id,pwd,email,role,acct_stat,created_by,created_at,del_at) VALUES
        (10,'owner10','fixture','owner10@example.test','USER','ACTIVE','owner','2026-10-01T00:00Z',NULL),
        (20,'owner20','fixture','owner20@example.test','USER','ACTIVE','owner','2026-10-01T00:00Z',NULL),
        (40,'admin40','fixture','admin40@example.test','ADMIN','LOCKED','owner','2026-10-01T00:00Z',NULL),
        (50,'owner50','fixture','owner50@example.test','USER','ACTIVE','owner','2026-10-01T00:00Z',now());
      INSERT INTO tb_file(file_idx,file_uuid,orgnl_nm,strg_nm,file_path,file_url,mime_type,file_size,upldr_type,created_by,del_at) VALUES
        (1,'aaaaaaaa-0000-0000-0000-000000000001','one','one','/one','/one','text/plain',1,'BOARD','owner',NULL),
        (2,'aaaaaaaa-0000-0000-0000-000000000002','two','two','/two','/two','text/plain',2,'BOARD','owner',NULL),
        (3,'aaaaaaaa-0000-0000-0000-000000000003','deleted','deleted','/deleted','/deleted','text/plain',3,'BOARD','owner',now());
      INSERT INTO tb_brd_pst_file(brd_pst_idx,file_idx,file_role,sort_ord,created_by) VALUES(1,1,'ATTACHMENT',0,'owner');
      INSERT INTO tb_brd_pst(brd_pst_idx,brd_pst_uuid,brd_ctgr_idx,mbr_acct_idx,ttl,cntnt,created_by,created_at,del_at) VALUES
        (1,'20000000-0000-0000-0000-000000000001',1,10,'needle title','body','writer','2026-10-01T00:00Z',NULL),
        (2,'20000000-0000-0000-0000-000000000002',2,20,'plain','needle body','writer','2026-10-02T00:00Z',NULL),
        (3,'20000000-0000-0000-0000-000000000003',NULL,30,'needle orphan','body','writer','2026-10-03T00:00Z',NULL),
        (4,'20000000-0000-0000-0000-000000000004',1,10,'needle deleted','needle','writer','2026-10-04T00:00Z',now()),
        (5,'20000000-0000-0000-0000-000000000005',1,20,'plain latest','body','writer','2026-10-05T00:00Z',NULL);
      INSERT INTO tb_fav(trgt_type,trgt_idx,mbr_acct_idx,created_by) VALUES
        ('BOARD_POST',1,10,'writer'),('BOARD_POST',1,20,'writer'),('COMMENT',1,30,'writer');
      INSERT INTO tb_brd_cmt(brd_pst_idx,mbr_acct_idx,cntnt,created_by,del_at) VALUES
        (1,10,'active','writer',NULL),(1,20,'deleted','writer',now());
      INSERT INTO tb_ntc(ntc_idx,ntc_uuid,ttl,cntnt,view_cnt,created_by,created_at,del_at) VALUES
        (1,'30000000-0000-0000-0000-000000000001','needle title','body',7,'writer','2026-10-01T00:00Z',NULL),
        (2,'30000000-0000-0000-0000-000000000002','plain','needle body',0,'writer','2026-10-02T00:00Z',NULL),
        (3,'30000000-0000-0000-0000-000000000003','needle deleted','body',0,'writer','2026-10-03T00:00Z',now()),
        (4,'30000000-0000-0000-0000-000000000004','needle latest','body',0,'writer','2026-10-04T00:00Z',NULL);
    `);
    const run = key => {
      const statement = statements[key];
      let index = 0;
      // JDBC setString과 동일하게 문자열 타입을 지정해 CONCAT의 다형 인자를 해석한다.
      return db.query(statement.sql.replace(/\?/g, () => {
        const cast = typeof statement.values[index] === 'string' ? '::varchar' : '';
        return '$' + (++index) + cast;
      }), statement.values);
    };
    let passed = 0;
    const boardCases = {
      none: [[5,3,2,1],4], title: [[3,1],2], content: [[2],1], author: [[1],1], all: [[3,2,1],3],
      unknown: [[3,1],2], category: [[1],1], wildcard: [[5,3,2,1],4], quoted: [[],0], page: [[2,1],4], large: [[],4]
    };
    for (const [variant,[ids,count]] of Object.entries(boardCases)) {
      const rows = (await run('board.' + variant + '.selectBoardPostList')).rows;
      assert.deepEqual(rows.map(row => Number(row.uuid.slice(-1))), ids, 'board ' + variant);
      assert.equal(Number((await run('board.' + variant + '.countBoardPostList')).rows[0].count), count);
      if (ids.includes(1)) {
        const row = rows.find(row => row.uuid.endsWith('1'));
        assert.equal(Number(row.fav_cnt), 2); assert.equal(Number(row.cmt_cnt), 1);
      }
      passed++;
    }
    const noticeCases = { none: [[4,2,1],3], keyword: [[4,1],2], blank: [[4,2,1],3], wildcard: [[4,2,1],3],
      quoted: [[],0], page: [[1],3], large: [[],3] };
    for (const [variant,[ids,count]] of Object.entries(noticeCases)) {
      const rows = (await run('notice.' + variant + '.selectNoticeList')).rows;
      assert.deepEqual(rows.map(row => Number(row.uuid.slice(-1))), ids, 'notice ' + variant);
      assert.equal(Number((await run('notice.' + variant + '.countNoticeList')).rows[0].count), count);
      passed++;
    }
    const boardDetail = (await run('board.active.selectBoardPostByUuid')).rows;
    assert.deepEqual(boardDetail, (await run('board.active.selectBoardPostByIdx')).rows);
    assert.equal(boardDetail[0].ttl, 'needle title');
    assert.equal(Number(boardDetail[0].fav_cnt), 2); assert.equal(Number(boardDetail[0].cmt_cnt), 1);
    passed++;
    const noticeDetail = (await run('notice.active.selectNoticeByUuid')).rows;
    for (const method of ['selectNoticeByIdx','selectNoticeForAdminByUuid']) {
      assert.deepEqual((await run('notice.active.' + method)).rows, noticeDetail);
    }
    assert.equal(noticeDetail[0].view_cnt, 7); assert.equal(noticeDetail[0].created_by, 'writer');
    passed++;
    for (const key of Object.keys(statements).filter(key => /^(board|notice)\.deleted\./.test(key))) {
      assert.equal((await run(key)).rows.length, 0, key);
    }
    passed++;
    assert.deepEqual((await run('notice.dashboard')).rows.map(row => Number(row.uuid.slice(-1))), [4,2]);
    passed++;
    for (const [variant,ids] of Object.entries({active:[5,3,2,1],deleted:[5,4,3,2,1],keyword:[3,2,1]})) {
      const rows = (await run('admin.' + variant + '.selectBoardPostList')).rows;
      assert.deepEqual(rows.map(row => Number(row.idx)),ids,'admin '+variant);
      assert.equal(Number((await run('admin.' + variant + '.countBoardPostList')).rows[0].count),ids.length);
      assert.equal(rows.find(row => Number(row.idx)===3).ctgr_nm,null);
      passed++;
    }
    // 정렬값이 모두 같은 상태에서 연속 페이지가 겹치거나 누락되지 않는지 확인한다.
    await db.exec('BEGIN');
    try {
      await db.exec("UPDATE tb_brd_pst SET ttl='same'; UPDATE tb_ntc SET ttl='same'");
      for (const [domain,expected] of Object.entries({board:[5,3,2,1],notice:[4,2,1],admin:[5,3,2,1]})) {
        const rows = [...(await run(domain+'.tie.1')).rows,...(await run(domain+'.tie.2')).rows];
        assert.deepEqual(rows.map(row=>Number(row.uuid.slice(-1))),expected,domain+' stable pages');
        passed++;
      }
      for (const domain of ['member','account']) {
        const rows = [...(await run(domain+'.tie.1')).rows,...(await run(domain+'.tie.2')).rows];
        assert.deepEqual(rows.map(row=>row.lgn_id),['admin40','owner20','owner10']);
        assert(rows.every(row=>!Object.hasOwn(row,'pwd')&&!Object.hasOwn(row,'otp_secret')));
        passed++;
      }
      assert.equal(Number((await run('account.count')).rows[0].count),3);
    } finally { await db.exec('ROLLBACK'); }
    for (const domain of ['board','notice']) {
      const table=domain==='board'?'tb_brd_pst':'tb_ntc';
      const key=domain==='board'?'brd_pst_idx':'ntc_idx';
      const before=(await db.query(`SELECT view_cnt FROM ${table} WHERE ${key}=1`)).rows[0].view_cnt;
      assert.equal((await run(domain+'.increment')).affectedRows,1);
      assert.equal((await run(domain+'.increment')).affectedRows,1);
      assert.equal((await run(domain+'.incrementDeleted')).affectedRows,0);
      assert.equal((await db.query(`SELECT view_cnt FROM ${table} WHERE ${key}=1`)).rows[0].view_cnt,before+2);
      passed++;
    }
    const files=(await run('file.batch')).rows;
    assert.deepEqual(files.map(row=>Number(row.idx)).sort(),[1,2]);
    assert.equal((await run('file.empty')).rows.length,0);
    passed++;
    await db.exec('BEGIN');
    try {
      await run('attachments.delete'); await run('attachments.insert');
      const rows=(await db.query('SELECT file_idx,sort_ord,created_by FROM tb_brd_pst_file WHERE brd_pst_idx=1 AND del_at IS NULL ORDER BY sort_ord')).rows;
      assert.deepEqual(rows.map(row=>[Number(row.file_idx),row.sort_ord,row.created_by]),[[2,0,'owner'],[1,1,'owner']]);
      assert.equal(Number((await db.query('SELECT COUNT(*) FROM tb_brd_pst_file WHERE del_at IS NOT NULL')).rows[0].count),1);
      passed++;
    } finally { await db.exec('ROLLBACK'); }
    // 일괄 INSERT 중 한 행이 실패해도 같은 트랜잭션의 기존 연결 삭제는 복구된다.
    await db.exec('BEGIN');
    try {
      await run('attachments.delete');
      await assert.rejects(run('attachments.invalid'), error=>error.code==='23502');
    } finally { await db.exec('ROLLBACK'); }
    const restored=(await db.query('SELECT file_idx FROM tb_brd_pst_file WHERE brd_pst_idx=1 AND del_at IS NULL')).rows;
    assert.deepEqual(restored.map(row=>Number(row.file_idx)),[1]);
    passed++;
    console.log(`PASS ${passed} list/detail SQL scenarios`);
  } finally {
    await db.close();
  }
})().catch(error => { console.error(error); process.exitCode = 1; });

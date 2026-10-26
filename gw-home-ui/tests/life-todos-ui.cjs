// 임시 API와 가상 계정으로 일상 CRUD·카테고리·반응형 화면을 검증한다.
const fs = require('node:fs');
const path = require('node:path');
const http = require('node:http');
const assert = require('node:assert/strict');
const { spawn } = require('node:child_process');
const { randomUUID } = require('node:crypto');
const { chromium } = require(path.resolve(process.argv[2]));
const root = path.resolve(__dirname, '../..');
const output = path.join(root, 'gw-home-api/build/verification/life-screens');
fs.mkdirSync(output, { recursive: true });
const today = '2026-10-25';
let categories = [{ category_uuid: randomUUID(), name: '집안일' }];
let todos = [];
let listMode = 'normal';
let failSave = false;
let saveDelay = 0;
let lastSave;
const calls = [];
const groupOf = todo => todo.completed ? 'COMPLETED' : !todo.due_date ? 'UNSCHEDULED' : todo.due_date < today ? 'OVERDUE' : todo.due_date === today ? 'TODAY' : 'UPCOMING';
const item = (title, date = null, category = null) => ({ todo_uuid: randomUUID(), title, memo: null, category_uuid: category,
  due_date: date, completed: false, completed_at: null, created_at: '2026-10-25T00:00:00Z', updated_at: '2026-10-25T00:00:00Z' });
todos = [item('집안 정리와 장보기 목록을 확인하고 필요한 물건을 준비하기 '.repeat(3), today, categories[0].category_uuid), item('병원 예약', '2026-10-24'), item('개인 용무 메모')];
const mapped = todo => ({ ...todo, category_name: categories.find(category => category.category_uuid === todo.category_uuid)?.name ?? null, group: groupOf(todo) });
function filtered(url) {
  return todos.filter(todo => (!url.searchParams.get('categoryUuid') || todo.category_uuid === url.searchParams.get('categoryUuid'))
    && (url.searchParams.get('uncategorized') !== 'true' || !todo.category_uuid)
    && (!url.searchParams.get('keyword') || (todo.title + (todo.memo || '')).includes(url.searchParams.get('keyword'))));
}
function summary(items) {
  const count = group => items.filter(todo => groupOf(todo) === group).length;
  return { total_count: items.length, today_count: count('TODAY'), upcoming_count: count('UPCOMING'), overdue_count: count('OVERDUE'),
    completed_count: count('COMPLETED'), unscheduled_count: count('UNSCHEDULED'), progress_rate: items.length ? Math.round(count('COMPLETED') / items.length * 100) : 0, as_of_date: today };
}
function dataFor(request, url, body) {
  const pathname = url.pathname;
  if (pathname === '/api/v1/accounts/me') return { member_account_uuid: 'fixture-account', login_id: 'fixture', email: 'fixture@example.test', role: 'USER', created_at: '2026-01-01T00:00Z' };
  if (pathname === '/api/v1/profiles/me') return { member_profile_uuid: 'fixture-profile', nickname: '로컬 검증', created_at: '2026-01-01T00:00Z' };
  if (pathname.endsWith('/navigation-favorites')) return { favorite_menus: ['/work/todos', '/life/todos'] };
  if (pathname === '/api/v1/work-todos/summary') return { ...summary([]), today_count: 9, time_zone: 'Asia/Seoul' };
  if (pathname === '/api/v1/work-todos' || pathname === '/api/v1/boards') return { content: [], page: 1, size: 20, total_count: 0, total_pages: 0 };
  if (!pathname.startsWith('/api/v1/life-todos')) return [];
  const parts = pathname.slice('/api/v1/life-todos'.length).split('/').filter(Boolean);
  if (parts[0] === 'categories') {
    if (request.method === 'GET') return categories;
    if (request.method === 'POST') { const category = { category_uuid: randomUUID(), name: body.name }; categories.push(category); return category; }
    const category = categories.find(category => category.category_uuid === parts[1]);
    if (request.method === 'PUT') { category.name = body.name; return category; }
    if (request.method === 'DELETE') {
      categories = categories.filter(category => category.category_uuid !== parts[1]);
      todos.forEach(todo => { if (todo.category_uuid === parts[1]) todo.category_uuid = null; }); return null;
    }
  }
  if (parts[0] === 'summary') return summary(listMode === 'empty' ? [] : filtered(url));
  if (request.method === 'GET') {
    let found = listMode === 'empty' ? [] : filtered(url);
    const group = url.searchParams.get('group');
    if (group && group !== 'ALL') found = found.filter(todo => groupOf(todo) === group);
    const page = Number(url.searchParams.get('page') || 1), size = Number(url.searchParams.get('size') || 20);
    return { content: found.slice((page - 1) * size, page * size).map(mapped), page, size, total_count: found.length, total_pages: Math.ceil(found.length / size) };
  }
  if (request.method === 'POST') { lastSave = body; const todo = { ...item(body.title), ...body }; todos.unshift(todo); return mapped(todo); }
  const todo = todos.find(todo => todo.todo_uuid === parts[0]);
  if (parts[1] === 'completion') { todo.completed = body.completed; todo.completed_at = body.completed ? '2026-10-25T12:00Z' : null; return mapped(todo); }
  if (request.method === 'PUT') { lastSave = body; Object.assign(todo, body); return mapped(todo); }
  if (request.method === 'DELETE') { todos = todos.filter(todo => todo.todo_uuid !== parts[0]); return null; }
}
const fixture = http.createServer(async (request, response) => {
  response.setHeader('Access-Control-Allow-Origin', 'http://127.0.0.1:19098');
  response.setHeader('Access-Control-Allow-Headers', 'authorization,content-type');
  response.setHeader('Access-Control-Allow-Methods', 'GET,POST,PUT,DELETE,OPTIONS');
  response.setHeader('Content-Type', 'application/json');
  if (request.method === 'OPTIONS') { response.writeHead(204); return response.end(); }
  const url = new URL(request.url, 'http://127.0.0.1:19097');
  calls.push(request.method + ' ' + url.pathname);
  let raw = ''; for await (const chunk of request) raw += chunk;
  const body = raw ? JSON.parse(raw) : {};
  const isList = request.method === 'GET' && ['/api/v1/life-todos', '/api/v1/life-todos/summary'].includes(url.pathname);
  const isWrite = ['POST', 'PUT'].includes(request.method) && url.pathname === '/api/v1/life-todos';
  const failed = (listMode === 'error' && isList) || (failSave && isWrite);
  const data = failed ? null : dataFor(request, url, body);
  setTimeout(() => { response.writeHead(failed ? 500 : 200); response.end(JSON.stringify({ success: !failed, data, message: failed ? '검증용 요청 실패' : null })); }, isWrite ? saveDelay : isList && listMode === 'slow' ? 900 : 0);
});

(async () => {
  let browser, server;
  let serverLog = '';
  try {
    await new Promise((resolve, reject) => fixture.once('error', reject).listen(19097, '127.0.0.1', resolve));
    server = spawn(process.execPath, [path.join(root, `gw-home-api/build/verification/${process.env.LIFE_UI_OUTPUT || 'life-output'}/server/index.mjs`)], {
      cwd: path.join(root, 'gw-home-ui'), windowsHide: true, stdio: ['ignore', 'pipe', 'pipe'],
      env: { ...process.env, NITRO_HOST: '127.0.0.1', NITRO_PORT: '19098', PORT: '19098', NUXT_PUBLIC_API_BASE: 'http://127.0.0.1:19097' }
    });
    server.stdout.on('data', chunk => { serverLog += chunk; }); server.stderr.on('data', chunk => { serverLog += chunk; });
    server.on('error', error => { serverLog += error.message; });
    let ready = false;
    for (let attempt = 0; attempt < 80; attempt++) { try { await fetch('http://127.0.0.1:19098/login'); ready = true; break; } catch { await new Promise(resolve => setTimeout(resolve, 100)); } }
    assert(ready, serverLog);
    browser = await chromium.launch({ channel: 'chrome', headless: true });
    const context = await browser.newContext({ timezoneId: 'Asia/Seoul', colorScheme: 'dark', hasTouch: true });
    await context.addCookies([{ name: 'gw-home-access-token', value: 'local-fixture-only', url: 'http://127.0.0.1:19098' }]);
    const page = await context.newPage();
    const errors = []; page.on('pageerror', error => errors.push(error.message));
    const noOverflow = async () => { const result = await page.evaluate(() => ({ width: innerWidth, scroll: document.documentElement.scrollWidth })); assert(result.scroll <= result.width, JSON.stringify(result)); };
    const load = async () => { await page.goto('http://127.0.0.1:19098/life/todos?group=ALL'); await page.locator('.life-todos__card').first().waitFor(); };
    const dialog = () => page.getByRole('dialog');
    const card = title => page.locator('.life-todos__card').filter({ has: page.getByRole('heading', { name: title, exact: true }) });
    for (const width of [360, 390, 768, 1440]) {
      await page.setViewportSize({ width, height: width < 768 ? 844 : 1000 }); await load(); await noOverflow();
      await page.screenshot({ path: path.join(output, `life-${width}.png`), fullPage: true });
    }
    console.log('PASS 360/390/768/1440px layout and long titles');
    await page.setViewportSize({ width: 390, height: 844 });
    await page.getByRole('button', { name: '+ 일상 할 일 추가', exact: true }).click();
    await dialog().getByLabel(/^제목/).fill('제목만 입력한 일상');
    saveDelay = 700;
    await dialog().getByRole('button', { name: '추가', exact: true }).click();
    assert(await dialog().getByLabel(/^제목/).isDisabled());
    await page.keyboard.press('Escape'); assert(await dialog().isVisible());
    await dialog().waitFor({ state: 'hidden' }); saveDelay = 0;
    await card('제목만 입력한 일상').waitFor();
    assert.equal(lastSave.category_uuid, null); assert.equal(lastSave.due_date, null); assert(!('work_unit_uuid' in lastSave));
    console.log('PASS title-only creation and save-in-progress protection');

    await page.getByRole('button', { name: '카테고리 관리', exact: true }).click();
    await dialog().getByLabel('새 카테고리', { exact: true }).fill('개인 용무');
    await dialog().getByRole('button', { name: '카테고리 추가', exact: true }).click();
    await dialog().getByRole('button', { name: '개인 용무 이름 변경', exact: true }).click();
    await dialog().getByLabel('카테고리 이름 변경', { exact: true }).fill('생활 용무');
    await dialog().getByRole('button', { name: '이름 저장', exact: true }).click();
    await dialog().getByRole('button', { name: '생활 용무 이름 변경', exact: true }).waitFor();
    await dialog().getByRole('button', { name: '닫기', exact: true }).last().click();
    await card('제목만 입력한 일상').getByRole('button', { name: '제목만 입력한 일상 수정', exact: true }).click();
    await dialog().getByLabel(/^생활 카테고리/).selectOption({ label: '생활 용무' });
    await dialog().getByLabel(/^날짜/).fill(today); await dialog().getByLabel(/^메모/).fill('필요한 준비물을 확인하기');
    await dialog().getByRole('button', { name: '수정', exact: true }).click(); await dialog().waitFor({ state: 'hidden' });
    await card('제목만 입력한 일상').getByText('생활 용무', { exact: true }).waitFor();
    await card('제목만 입력한 일상').getByRole('button', { name: '제목만 입력한 일상 완료', exact: true }).click();
    await card('제목만 입력한 일상').getByRole('button', { name: '제목만 입력한 일상 완료 취소', exact: true }).waitFor();
    await page.getByRole('navigation', { name: '일상 할 일 분류' }).getByRole('button', { name: /^완료/ }).click();
    await card('제목만 입력한 일상').getByRole('button', { name: '제목만 입력한 일상 완료 취소', exact: true }).click();
    await page.getByText('표시할 일상 할 일이 없습니다.', { exact: true }).waitFor();
    await page.getByRole('navigation', { name: '일상 할 일 분류' }).getByRole('button', { name: /^전체/ }).click();
    console.log('PASS category create/rename, edit, complete and undo in filtered list');

    await page.getByRole('button', { name: '카테고리 관리', exact: true }).click();
    await dialog().getByRole('button', { name: '생활 용무 카테고리 삭제', exact: true }).click();
    await dialog().getByRole('button', { name: '카테고리 삭제 확인', exact: true }).click();
    await dialog().getByRole('button', { name: '생활 용무 카테고리 삭제', exact: true }).waitFor({ state: 'hidden' });
    await dialog().getByRole('button', { name: '닫기', exact: true }).last().click();
    await card('제목만 입력한 일상').getByText('미분류', { exact: true }).waitFor();
    await page.getByLabel(/^생활 카테고리/).selectOption('UNCATEGORIZED');
    await page.getByLabel('검색', { exact: true }).fill('제목만');
    await page.getByRole('button', { name: '조회', exact: true }).click();
    await page.waitForFunction(() => document.querySelectorAll('.life-todos__card').length === 1);
    await card('제목만 입력한 일상').getByRole('button', { name: '제목만 입력한 일상 삭제', exact: true }).click();
    await dialog().getByRole('button', { name: '삭제', exact: true }).click(); await dialog().waitFor({ state: 'hidden' });
    await page.getByText('표시할 일상 할 일이 없습니다.', { exact: true }).waitFor();
    console.log('PASS category deletion preserves todos, uncategorized/search filters and todo deletion');

    await load(); failSave = true;
    await page.getByRole('button', { name: '+ 일상 할 일 추가', exact: true }).click();
    await dialog().getByLabel(/^제목/).fill('실패해도 남는 내용');
    await dialog().getByRole('button', { name: '추가', exact: true }).click();
    await dialog().getByRole('alert').waitFor();
    assert.equal(await dialog().getByLabel(/^제목/).inputValue(), '실패해도 남는 내용');
    await page.keyboard.press('Escape'); await dialog().waitFor({ state: 'hidden' }); failSave = false;
    listMode = 'error'; await page.reload(); await page.getByRole('button', { name: '다시 시도', exact: true }).waitFor();
    listMode = 'normal'; await page.getByRole('button', { name: '다시 시도', exact: true }).click(); await page.locator('.life-todos__card').first().waitFor();
    listMode = 'empty'; await page.reload(); await page.getByText('표시할 일상 할 일이 없습니다.', { exact: true }).waitFor();
    listMode = 'slow'; await page.reload(); await page.getByText('일상 할 일을 불러오는 중입니다.', { exact: true }).waitFor(); await page.locator('.life-todos__card').first().waitFor(); listMode = 'normal';
    console.log('PASS error retry, empty/loading states and preservation after save failure');

    await page.setViewportSize({ width: 390, height: 430 });
    await page.getByRole('button', { name: '+ 일상 할 일 추가', exact: true }).click();
    await dialog().getByLabel(/^메모/).fill('긴 메모 '.repeat(500));
    const saveBox = await dialog().getByRole('button', { name: '추가', exact: true }).boundingBox();
    assert(saveBox && saveBox.y >= 0 && saveBox.y + saveBox.height <= 430);
    assert(await dialog().locator('.base-modal__body').evaluate(element => element.scrollHeight > element.clientHeight));
    await page.screenshot({ path: path.join(output, 'life-short-modal.png') });
    await page.keyboard.press('Escape'); await dialog().waitFor({ state: 'hidden' });
    console.log('PASS 430px-high modal scrolling and visible actions');

    // 마지막 페이지의 유일한 항목을 지우면 유효한 이전 페이지로 돌아와야 한다.
    const extraCount = 21 - todos.length;
    for (let index = 1; index <= extraCount; index++) todos.push(item(`페이징 확인 ${index}`));
    await load();
    await page.getByRole('navigation', { name: '페이지 이동' }).getByRole('button', { name: '다음', exact: true }).click();
    await page.waitForFunction(() => document.querySelectorAll('.life-todos__card').length === 1);
    await page.locator('.life-todos__card').getByRole('button', { name: / 삭제$/ }).click();
    await dialog().getByRole('button', { name: '삭제', exact: true }).click();
    await dialog().waitFor({ state: 'hidden' });
    await page.waitForFunction(() => document.querySelectorAll('.life-todos__card').length === 20);
    assert.equal(await page.getByRole('navigation', { name: '페이지 이동' }).count(), 0);
    todos = todos.filter(todo => !todo.title.startsWith('페이징 확인'));
    console.log('PASS last-page deletion returns to a valid page');

    await page.setViewportSize({ width: 1440, height: 1000 });
    await page.goto('http://127.0.0.1:19098/dashboard');
    await page.getByRole('region', { name: '업무 할 일 요약' }).getByRole('link', { name: '오늘 업무 할 일 9건 보기', exact: true }).waitFor();
    const lifePanel = page.getByRole('region', { name: '일상 할 일 요약' });
    await lifePanel.getByRole('link', { name: /^오늘 일상 할 일/ }).waitFor(); await noOverflow();
    await page.screenshot({ path: path.join(output, 'dashboard-1440.png'), fullPage: true });
    await page.setViewportSize({ width: 390, height: 844 }); await noOverflow();
    await page.screenshot({ path: path.join(output, 'dashboard-390.png'), fullPage: true });
    listMode = 'error'; await page.reload();
    await page.getByRole('region', { name: '업무 할 일 요약' }).getByRole('link', { name: '오늘 업무 할 일 9건 보기', exact: true }).waitFor();
    await lifePanel.getByRole('button', { name: '다시 시도', exact: true }).waitFor();
    listMode = 'normal'; await lifePanel.getByRole('button', { name: '다시 시도', exact: true }).click();
    await lifePanel.getByRole('link', { name: /^오늘 일상 할 일/ }).click();
    await page.waitForURL('**/life/todos?group=TODAY');
    assert.equal(await page.getByRole('navigation', { name: '일상 할 일 분류' }).getByRole('button', { name: /^오늘/ }).getAttribute('aria-pressed'), 'true');
    await page.goto('http://127.0.0.1:19098/work/todos?group=TODAY'); await page.getByRole('heading', { name: '업무 할 일', exact: true }).waitFor();
    assert.equal(await page.getByRole('navigation', { name: '할 일 분류' }).getByRole('button', { name: /^오늘/ }).getAttribute('aria-pressed'), 'true');
    assert.deepEqual(errors, []); assert(calls.every(call => !call.includes('weekly-reports')));
    console.log('PASS separate dashboard summaries, today links, work label and no runtime errors');
  } finally { if (browser) await browser.close(); if (server) server.kill(); fixture.close(); }
})().catch(error => { console.error(error); process.exitCode = 1; });

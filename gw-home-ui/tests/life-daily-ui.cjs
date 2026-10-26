// 가상 API로 날짜 독립성·저장 실패·반응형·키보드 조작을 검증한다.
const fs = require('node:fs');
const path = require('node:path');
const http = require('node:http');
const assert = require('node:assert/strict');
const { spawn } = require('node:child_process');
const { randomUUID } = require('node:crypto');
const { chromium } = require(path.resolve(process.argv[2]));
const root = path.resolve(__dirname, '../..');
const output = path.join(root, 'gw-home-api/build/verification/daily-screens');
fs.mkdirSync(output, { recursive: true });
const today = '2026-10-27';
let items = [{ item_uuid: randomUUID(), title: '물 마시기', start_date: '2024-01-01', end_date: null },
  { item_uuid: randomUUID(), title: '매일 저녁 동네를 천천히 걸으며 하루를 정리하기 '.repeat(4), start_date: '2026-10-01', end_date: null }];
let checks = [{ item_uuid: items[0].item_uuid, date: '2026-10-26' }];
let failRead = false, failWrite = false, delayWrite = 0, delayMonth = '';
let writes = 0;
function dataFor(request, url, body) {
  const pathname = url.pathname;
  if (pathname === '/api/v1/accounts/me') return { member_account_uuid: 'fixture-account', login_id: 'fixture', email: 'fixture@example.test', role: 'USER', created_at: '2026-01-01T00:00Z' };
  if (pathname === '/api/v1/profiles/me') return { member_profile_uuid: 'fixture-profile', nickname: '로컬 검증', created_at: '2026-01-01T00:00Z' };
  if (pathname.endsWith('/navigation-favorites')) return { favorite_menus: ['/life/todos'] };
  if (pathname === '/api/v1/life-todos/categories') return [];
  if (pathname === '/api/v1/life-todos/summary') return { total_count: 0, today_count: 0, upcoming_count: 0, overdue_count: 0, completed_count: 0, unscheduled_count: 0, progress_rate: 0, as_of_date: today };
  if (pathname === '/api/v1/life-todos') return { content: [], page: 1, size: 20, total_count: 0, total_pages: 0 };
  if (!pathname.startsWith('/api/v1/life-daily')) return [];
  if (request.method === 'GET') {
    const month = url.searchParams.get('month');
    return { month, today, time_zone: 'Asia/Seoul', items: items.filter(item => item.start_date <= month + '-31' && (!item.end_date || item.end_date >= month + '-01')), checks: checks.filter(check => check.date.startsWith(month)) };
  }
  writes++;
  if (request.method === 'POST') { items.push({ item_uuid: randomUUID(), title: body.title, start_date: today, end_date: null }); return null; }
  const parts = pathname.split('/');
  const item = items.find(item => item.item_uuid === parts[4]);
  if (parts[5] === 'checks') {
    checks = checks.filter(check => check.item_uuid !== item.item_uuid || check.date !== parts[6]);
    if (body.completed) checks.push({ item_uuid: item.item_uuid, date: parts[6] });
  } else if (parts[5] === 'end') item.end_date = today;
  else item.title = body.title;
  return null;
}
const fixture = http.createServer(async (request, response) => {
  response.setHeader('Access-Control-Allow-Origin', 'http://127.0.0.1:19108');
  response.setHeader('Access-Control-Allow-Headers', 'authorization,content-type');
  response.setHeader('Access-Control-Allow-Methods', 'GET,POST,PUT,OPTIONS');
  response.setHeader('Content-Type', 'application/json');
  if (request.method === 'OPTIONS') { response.writeHead(204); return response.end(); }
  const url = new URL(request.url, 'http://127.0.0.1:19107');
  let raw = ''; for await (const chunk of request) raw += chunk;
  const daily = url.pathname.startsWith('/api/v1/life-daily');
  const writing = daily && request.method !== 'GET';
  const failed = daily && (writing ? failWrite : failRead);
  const data = failed ? null : dataFor(request, url, raw ? JSON.parse(raw) : {});
  setTimeout(() => { response.writeHead(failed ? 500 : 200); response.end(JSON.stringify({ success: !failed, data, message: failed ? '검증용 요청 실패' : null })); }, writing ? delayWrite : url.searchParams.get('month') === delayMonth ? 700 : 0);
});
(async () => {
  let browser, server;
  try {
    await new Promise(resolve => fixture.listen(19107, '127.0.0.1', resolve));
    server = spawn(process.execPath, [path.join(root, 'gw-home-api/build/verification/daily-output/server/index.mjs')], { windowsHide: true,
      env: { ...process.env, NITRO_HOST: '127.0.0.1', NITRO_PORT: '19108', PORT: '19108', NUXT_PUBLIC_API_BASE: 'http://127.0.0.1:19107' }, stdio: 'ignore' });
    for (let attempt = 0; attempt < 80; attempt++) { try { await fetch('http://127.0.0.1:19108/login'); break; } catch { await new Promise(resolve => setTimeout(resolve, 100)); } }
    browser = await chromium.launch({ channel: 'chrome', headless: true });
    const context = await browser.newContext({ timezoneId: 'Asia/Seoul', hasTouch: true });
    await context.addCookies([{ name: 'gw-home-access-token', value: 'local-fixture-only', url: 'http://127.0.0.1:19108' }]);
    const page = await context.newPage();
    await page.clock.install({ time: new Date('2026-10-27T12:00:00+09:00') });
    const errors = []; page.on('pageerror', error => errors.push(error.message));
    const checkbox = () => page.getByRole('checkbox', { name: '물 마시기', exact: true });
    const waitReady = () => page.locator('.daily-checklist__items[aria-busy="false"]').waitFor();
    const select = async date => { await page.getByRole('button', { name: new RegExp('^' + date) }).click(); };
    await page.goto('http://127.0.0.1:19108/life/todos'); await checkbox().waitFor(); await waitReady();
    for (const width of [360, 390, 768, 1440]) {
      await page.setViewportSize({ width, height: 844 });
      assert(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth));
      await page.screenshot({ path: path.join(output, `daily-${width}.png`), fullPage: true });
    }
    console.log('PASS calendar and long checklist at 360/390/768/1440px');
    assert.equal(await checkbox().getAttribute('aria-checked'), 'false');
    delayWrite = 600; const before = writes; await checkbox().click();
    assert(await checkbox().isDisabled()); assert(await page.getByRole('button', { name: '다음 달', exact: true }).isDisabled());
    await waitReady(); delayWrite = 0; assert.equal(writes, before + 1);
    assert.equal(await checkbox().getAttribute('aria-checked'), 'true');
    await select('2026-10-26'); assert.equal(await checkbox().getAttribute('aria-checked'), 'true');
    await checkbox().focus(); await page.keyboard.press('Space'); await waitReady();
    assert.equal(await checkbox().getAttribute('aria-checked'), 'false');
    await select(today); assert.equal(await checkbox().getAttribute('aria-checked'), 'true');
    await page.reload(); await checkbox().waitFor(); await waitReady(); assert.equal(await checkbox().getAttribute('aria-checked'), 'true');
    console.log('PASS independent dates, keyboard toggle, duplicate guard and persisted reload');
    failWrite = true; await checkbox().click(); await waitReady();
    await page.getByText('검증용 요청 실패', { exact: true }).waitFor(); assert.equal(await checkbox().getAttribute('aria-checked'), 'true');
    failWrite = false; await checkbox().click(); await waitReady();
    await select('2026-10-28'); assert(await checkbox().isDisabled());
    await page.getByRole('button', { name: '오늘', exact: true }).click(); await waitReady();
    console.log('PASS failure preserves checkbox and future dates cannot be checked');
    await page.getByLabel('매일 할 일', { exact: true }).fill('산책 20분');
    failWrite = true; await page.getByRole('button', { name: '추가', exact: true }).click(); await waitReady();
    assert.equal(await page.getByLabel('매일 할 일', { exact: true }).inputValue(), '산책 20분');
    failWrite = false; await page.getByRole('button', { name: '추가', exact: true }).click(); await waitReady();
    await page.getByRole('checkbox', { name: '산책 20분', exact: true }).waitFor();
    await select('2026-10-26'); assert.equal(await page.getByRole('checkbox', { name: '산책 20분', exact: true }).count(), 0);
    await select(today);
    await page.getByRole('button', { name: '산책 20분 제목 수정', exact: true }).click();
    const dialog = page.getByRole('dialog'); await dialog.getByLabel('제목', { exact: true }).fill('저녁 산책');
    await page.setViewportSize({ width: 390, height: 430 });
    await page.screenshot({ path: path.join(output, 'daily-edit-390x430.png'), fullPage: true });
    await dialog.getByRole('button', { name: '저장', exact: true }).click(); await dialog.waitFor({ state: 'hidden' }); await waitReady();
    await page.getByRole('button', { name: '저녁 산책 반복 종료', exact: true }).click();
    await dialog.getByRole('button', { name: '반복 종료', exact: true }).click(); await dialog.waitFor({ state: 'hidden' }); await waitReady();
    await page.getByRole('checkbox', { name: '저녁 산책', exact: true }).click(); await waitReady();
    await select('2026-10-28'); assert.equal(await page.getByRole('checkbox', { name: '저녁 산책', exact: true }).count(), 0);
    console.log('PASS title-only create, failure input retention, rename and end preserves today');
    await page.setViewportSize({ width: 1440, height: 900 });
    delayMonth = '2026-09'; await page.getByRole('button', { name: '이전 달', exact: true }).click();
    await page.getByRole('button', { name: '다음 달', exact: true }).click(); await waitReady();
    await new Promise(resolve => setTimeout(resolve, 900));
    assert.equal(await page.locator('.daily-calendar__toolbar h2').textContent(), '2026년 10월'); delayMonth = '';
    failRead = true; await page.getByRole('button', { name: '다음 달', exact: true }).click();
    await page.getByRole('button', { name: '다시 불러오기', exact: true }).waitFor();
    failRead = false; await page.getByRole('button', { name: '다시 불러오기', exact: true }).click(); await waitReady();
    items = []; await page.getByRole('button', { name: '오늘', exact: true }).click(); await waitReady();
    await page.getByText('이 날짜에 할 일이 없습니다.', { exact: true }).waitFor();
    await page.getByRole('button', { name: '일반 할 일', exact: true }).click();
    await page.getByRole('button', { name: '+ 일상 할 일 추가', exact: true }).waitFor();
    assert.deepEqual(errors, []);
    console.log('PASS stale month response, retry, empty state, normal todo access and no runtime errors');
  } finally { if (browser) await browser.close(); if (server) server.kill(); fixture.close(); }
})().catch(error => { console.error(error); process.exitCode = 1; });

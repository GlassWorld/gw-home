// 로컬 임시 API로 반응형 화면·상태·상세 이동을 검증한다. 실계정이나 운영 API를 사용하지 않는다.
const fs = require('node:fs');
const path = require('node:path');
const http = require('node:http');
const assert = require('node:assert/strict');
const { spawn } = require('node:child_process');
const { chromium } = require(path.resolve(process.argv[2]));
const root = path.resolve(__dirname, '..');
const output = path.resolve(root, '../gw-home-api/build/verification/screens');
fs.mkdirSync(output, { recursive: true });
const unitId = '11111111-1111-4111-8111-111111111111';
const todoId = 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa';
let state = 'normal';
let latestQuery;
const calls = [];
const longTitle = '모바일에서도 긴 제목과 내용이 자연스럽게 줄바꿈되는 업무 일정 확인 '.repeat(3);
const add = (date, days) => new Date(Date.parse(date + 'T00:00:00Z') + days * 86400000).toISOString().slice(0,10);
const summary = { total_count:9, today_count:2, upcoming_count:1, overdue_count:1, completed_count:4, unscheduled_count:1, unknown_completion_count:1, progress_rate:44, as_of_date:'2026-10-25', time_zone:'Asia/Seoul' };
const options = [{ work_unit_uuid:unitId, title:'프로젝트 리뉴얼 · 모바일 업무 환경', status:'IN_PROGRESS', use_yn:'Y' },{ work_unit_uuid:'33333333-3333-4333-8333-333333333333',title:'지난 업무',status:'DONE',use_yn:'N' }];
function responseFor(url) {
  const pathname = url.pathname;
  if (pathname === '/api/v1/accounts/me') return { member_account_uuid:'fixture-account', login_id:'fixture', email:'fixture@example.test', role:'USER', created_at:'2026-01-01T00:00Z' };
  if (pathname === '/api/v1/profiles/me') return { member_profile_uuid:'fixture-profile', nickname:'로컬 검증', created_at:'2026-01-01T00:00Z' };
  if (pathname.endsWith('/navigation-favorites')) return { favorite_menus:[] };
  if (pathname.endsWith('/options')) return options;
  if (pathname.endsWith('/gantt') || pathname.endsWith('/statistics')) {
    latestQuery = Object.fromEntries(url.searchParams);
    const from = url.searchParams.get('fromDate'), to = url.searchParams.get('toDate');
    const period = { from_date:from, to_date:to, time_zone:url.searchParams.get('timeZone'), as_of_date:'2026-10-25', include_inactive:url.searchParams.get('includeInactive')==='true', work_unit_uuid:url.searchParams.get('workUnitUuid') };
    const units = [{ work_unit_uuid:unitId, title:options[0].title, status:'IN_PROGRESS', active:true, start_date:from, due_date:to, partial_schedule:true, summary, period_completed_count:3 }];
    if (pathname.endsWith('/gantt')) return {
      period, units: state==='empty'?[]:units, rows:state==='empty'?[]:[
        { todo_uuid:'parent', work_unit_uuid:unitId, title:'상위 일정', start_date:from, due_date:to, status:'IN_PROGRESS', progress_rate:50, leaf:false, completed:false, delayed:false, context_only:false, depth:0 },
        { todo_uuid:todoId, work_unit_uuid:unitId, parent_todo_uuid:'parent', title:longTitle, start_date:from, due_date:add(from,3), status:'DONE', progress_rate:100, leaf:true, completed:true, delayed:false, context_only:false, depth:1 },
        { todo_uuid:'overdue', work_unit_uuid:unitId, title:'마감일을 확인해야 하는 할 일', start_date:null, due_date:add(from,1), status:'DELAYED', progress_rate:0, leaf:true, completed:false, delayed:true, context_only:false, depth:0 },
        { todo_uuid:'undated', work_unit_uuid:unitId, title:'아직 일정이 없는 할 일', start_date:null, due_date:null, status:'TODO', progress_rate:0, leaf:true, completed:false, delayed:false, context_only:false, depth:0 }
      ]
    };
    const count = Math.min(366,(Date.parse(to)-Date.parse(from))/86400000+1);
    const bucket = url.searchParams.get('bucket') || 'day';
    return { period, bucket, summary:state==='empty'?Object.fromEntries(Object.entries(summary).map(([key,value])=>[key,typeof value==='number'?0:value])):summary,
      period_completed_count:state==='empty'?0:3, units:state==='empty'?[]:units,
      trend:Array.from({length:count},(_,index)=>({start_date:add(from,index),end_date:add(from,index),completed_count:state==='empty'?0:index===2?2:index===5?1:0})) };
  }
  if (pathname === '/api/v1/work-units/' + unitId) return { ...options[0], description:'테스트용 업무', git_projects:[] };
  if (pathname === '/api/v1/work-units/' + unitId + '/todos') return { work_unit_uuid:unitId, summary:{status:'DONE',progress_rate:100,item_count:1,leaf_count:1,completed_leaf_count:1,delayed_count:0},todos:[
    {todo_uuid:todoId,title:longTitle,status:'DONE',progress_rate:100,start_date:'2026-10-25',due_date:'2026-10-28',leaf:true,completed:true,delayed:false,depth:0,sort_order:0,children:[],created_at:'2026-10-01T00:00Z',updated_at:'2026-10-25T00:00Z',completed_at:'2026-10-25T00:00Z'}
  ] };
  if (pathname.endsWith('/settings')) return { email:'',verified:false,time_zone:'Asia/Seoul',digest_time:'09:00',digest_cycle:'DAILY',digest_enabled:false,reminder_enabled:false,server_ready:false,next_digest_at:null };
  if (pathname.endsWith('/reminder')) return { todo_uuid:todoId,enabled:false,send_at:null,time_zone:'Asia/Seoul' };
  return [];
}
const fixture = http.createServer((req,res) => {
  res.setHeader('Access-Control-Allow-Origin','http://127.0.0.1:19096');
  res.setHeader('Access-Control-Allow-Headers','authorization,content-type'); res.setHeader('Content-Type','application/json');
  if (req.method === 'OPTIONS') { res.writeHead(204); return res.end(); }
  const url = new URL(req.url,'http://127.0.0.1:19095'); calls.push(req.method+' '+url.pathname);
  const fail = state==='error' && /\/(gantt|statistics)$/.test(url.pathname);
  const body = fail ? {success:false,message:'일시적으로 조회할 수 없습니다.'} : {success:true,data:responseFor(url),message:null};
  setTimeout(() => { res.writeHead(fail?500:200); res.end(JSON.stringify(body)); },state==='slow' && /\/(gantt|statistics)$/.test(url.pathname)?1500:0);
});
(async () => {
  let browser, server;
  const errors = [];
  try {
    await new Promise((resolve,reject) => fixture.once('error',reject).listen(19095,'127.0.0.1',resolve));
    let serverLog = '';
    server = spawn(process.execPath,[process.argv[3] ? path.resolve(process.argv[3]) : '.output/server/index.mjs'],{cwd:root,env:{...process.env,NITRO_HOST:'127.0.0.1',NITRO_PORT:'19096',PORT:'19096',NUXT_PUBLIC_API_BASE:'http://127.0.0.1:19095'},windowsHide:true,stdio:['ignore','pipe','pipe']});
    server.stdout.on('data',chunk=>{serverLog+=chunk;}); server.stderr.on('data',chunk=>{serverLog+=chunk;});
    server.on('error',error=>{serverLog+=error.message;});
    let ready=false;
    for(let attempt=0;attempt<60;attempt++){ try{await fetch('http://127.0.0.1:19096/login');ready=true;break;}catch{await new Promise(resolve=>setTimeout(resolve,100));} }
    assert(ready,'preview server ready: '+serverLog);
    browser = await chromium.launch({channel:'chrome',headless:true});
    const context = await browser.newContext({timezoneId:'Asia/Seoul',colorScheme:'dark'});
    await context.addCookies([{name:'gw-home-access-token',value:'local-fixture-only',url:'http://127.0.0.1:19096'}]);
    const page = await context.newPage();
    page.on('pageerror',error=>errors.push(error.message));
    const loaded = async route => { await page.goto('http://127.0.0.1:19096/work/'+route); await page.locator(route==='gantt'?'.gantt-unit':'.insight-metrics').waitFor(); };
    const noOverflow = async () => {
      const size=await page.evaluate(()=>({scroll:document.documentElement.scrollWidth,width:window.innerWidth}));
      assert(size.scroll<=size.width,'page overflows '+JSON.stringify(size));
    };
    for(const width of [360,390,768,1440]) {
      await page.setViewportSize({width,height:width<768?844:1000});
      await loaded('gantt'); await noOverflow();
      if(width<768) {
        assert.equal(await page.getByRole('button',{name:'일정 목록',exact:true}).getAttribute('aria-pressed'),'true');
        assert.equal(await page.locator('.insight-filters').getAttribute('open'),null);
      }
      await page.screenshot({path:path.join(output,'gantt-'+width+'.png'),fullPage:true});
      await page.getByRole('button',{name:'간트 보기',exact:true}).click(); await noOverflow();
      const scroll=page.locator('.gantt-scroll');
      await scroll.evaluate(element=>{element.scrollLeft=500;});
      await page.screenshot({path:path.join(output,'gantt-chart-'+width+'.png'),fullPage:true});
      await loaded('statistics'); await noOverflow();
      await page.screenshot({path:path.join(output,'statistics-'+width+'.png'),fullPage:true});
      console.log('PASS dark layout '+width+'px');
    }
    await page.setViewportSize({width:390,height:844}); await loaded('statistics');
    await page.locator('.insight-filter-toggle').click();
    await page.getByLabel('완료 기간 시작').fill('2026-10-01'); await page.getByLabel('완료 기간 종료').fill('2026-10-31');
    await page.getByLabel('완료 추이 단위').selectOption('week');
    await page.getByLabel('비활성 업무 포함').uncheck(); await page.getByRole('button',{name:'조회',exact:true}).click();
    await page.locator('.insight-metrics').waitFor();
    assert.equal(latestQuery.bucket,'week'); assert.equal(latestQuery.includeInactive,'false');
    assert.equal(latestQuery.fromDate,'2026-10-01');
    await page.getByText('완료 건수 표로 보기',{exact:true}).click(); assert(await page.locator('table tbody tr').count()>0);
    await noOverflow();
    assert.equal(await page.getByLabel('시간대', {exact:true}).count(), 0);
    assert.equal(latestQuery.timeZone, 'Asia/Seoul');
    console.log('PASS filter binding, table and default timezone');
    state='empty'; await page.goto('http://127.0.0.1:19096/work/gantt'); await page.getByText('표시할 일정이 없습니다.',{exact:true}).waitFor();
    state='error'; await page.reload(); await page.getByRole('alert').filter({hasText:'일시적으로'}).waitFor();
    state='normal'; await page.getByRole('button',{name:'다시 조회',exact:true}).click(); await page.locator('.gantt-unit').waitFor();
    state='slow'; await page.reload(); await page.getByText('일정을 불러오는 중입니다.',{exact:true}).waitFor(); await page.locator('.gantt-unit').waitFor(); state='normal';
    console.log('PASS empty, error retry and loading states');
    await page.locator('.schedule-item').filter({hasText:longTitle}).click();
    await page.getByRole('dialog').waitFor();
    assert(page.url().includes('todo='+todoId));
    await noOverflow();
    assert.equal(await page.locator('html').evaluate(element=>getComputedStyle(element).colorScheme),'dark');
    assert.equal(calls.filter(call=>call.startsWith('POST ')).length,0);
    assert.deepEqual(errors,[]);
    console.log('PASS deep link, mobile modal, dark mode, no mail or write requests, no runtime errors');
  } finally {
    if(browser) await browser.close();
    if(server) server.kill();
    await new Promise(resolve=>fixture.close(resolve));
  }
})().catch(error=>{console.error(error);process.exitCode=1;});

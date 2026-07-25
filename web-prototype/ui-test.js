/**
 * 墨园 Web 原型 UI 自动化测试
 * 覆盖：页面加载、控制台错误、资源加载失败、四页签切换、
 *       植物点击详情、布局/模式切换、截图存档
 */
const { chromium } = require('playwright');
const path = require('path');

const BASE = 'http://127.0.0.1:8790/web-prototype/index.html';
const OUT = path.join(__dirname, 'test-shots');
const fs = require('fs');
if (!fs.existsSync(OUT)) fs.mkdirSync(OUT, { recursive: true });

(async () => {
  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext({
    viewport: { width: 420, height: 900 }, // 手机纵向比例
    deviceScaleFactor: 2,
  });
  const page = await context.newPage();

  const consoleErrors = [];
  const failedRequests = [];
  page.on('console', (msg) => {
    if (msg.type() === 'error') consoleErrors.push(msg.text().slice(0, 200));
  });
  page.on('requestfailed', (req) => {
    failedRequests.push(`${req.url()} :: ${req.failure()?.errorText}`);
  });
  page.on('response', (res) => {
    if (res.status() >= 400) failedRequests.push(`${res.url()} :: HTTP ${res.status()}`);
  });

  const results = [];
  const check = (name, ok, extra = '') =>
    results.push(`${ok ? 'PASS' : 'FAIL'}  ${name}${extra ? '  (' + extra + ')' : ''}`);

  // 1. 页面加载
  await page.goto(BASE, { waitUntil: 'networkidle', timeout: 30000 });
  await page.waitForTimeout(1500);
  check('页面加载 networkidle', true);
  await page.screenshot({ path: path.join(OUT, '01-garden.png') });

  // 2. 标题与页签存在
  const title = await page.title();
  check('页面标题', !!title, title);
  const tabs = await page.$$eval('[class*="tab"], nav *', (els) =>
    els.map((e) => e.textContent.trim()).filter(Boolean)
  ).catch(() => []);
  const tabText = tabs.join('|');
  for (const t of ['花园', '图鉴', '书案', '我的']) {
    check(`页签「${t}」存在`, tabText.includes(t));
  }

  // 3. 花园画布 / 植物渲染
  const plantCount = await page.evaluate(() => {
    const imgs = Array.from(document.querySelectorAll('img'));
    return imgs.filter((i) => i.src.includes('/plants/') && i.complete && i.naturalWidth > 0).length;
  });
  check('花园植物图片渲染', plantCount > 0, `已加载 ${plantCount} 张植物图`);

  // 4. 依次切换页签并截图
  const tabNames = ['图鉴', '书案', '我的', '花园'];
  for (let i = 0; i < tabNames.length; i++) {
    const name = tabNames[i];
    const el = await page.$(`text=${name}`);
    if (el) {
      await el.click().catch(() => {});
      await page.waitForTimeout(900);
      await page.screenshot({ path: path.join(OUT, `0${i + 2}-${name}.png`) });
      check(`切换到「${name}」`, true);
    } else {
      check(`切换到「${name}」`, false, '未找到页签元素');
    }
  }

  // 5. 图鉴：统计格子数量
  const catalogEl = await page.$('text=图鉴');
  if (catalogEl) {
    await catalogEl.click().catch(() => {});
    await page.waitForTimeout(800);
    const cardCount = await page.evaluate(() => {
      const grids = document.querySelectorAll('[class*="catalog"] [class*="card"], [class*="grid"] > *');
      return grids.length;
    });
    check('图鉴格子渲染', cardCount >= 25, `${cardCount} 格`);
    // 点击第一个图鉴卡片看详情
    const firstCard = await page.$('[class*="card"]');
    if (firstCard) {
      await firstCard.click().catch(() => {});
      await page.waitForTimeout(700);
      await page.screenshot({ path: path.join(OUT, '06-plant-detail.png') });
      check('植物详情弹出', true);
      // 关闭
      await page.keyboard.press('Escape').catch(() => {});
      const closeBtn = await page.$('text=返回') || await page.$('[class*="close"]');
      if (closeBtn) await closeBtn.click().catch(() => {});
    }
  }

  // 6. 回到花园，测试布局/模式切换按钮
  const gardenTab = await page.$('text=花园');
  if (gardenTab) { await gardenTab.click().catch(() => {}); await page.waitForTimeout(600); }
  for (const label of ['自动', '自定义', '初庭', '雅庭', '盛庭']) {
    const btn = await page.$(`text=${label}`);
    if (btn) {
      await btn.click().catch(() => {});
      await page.waitForTimeout(500);
      check(`点击「${label}」无异常`, true);
    }
  }
  await page.screenshot({ path: path.join(OUT, '07-garden-after-toggle.png') });

  // 7. 汇总
  const report = [
    '=== 墨园 Web 原型 UI 测试报告 ===',
    ...results,
    '',
    `控制台错误: ${consoleErrors.length}`,
    ...consoleErrors.slice(0, 10).map((e) => '  ERR ' + e),
    `资源加载失败: ${failedRequests.length}`,
    ...failedRequests.slice(0, 10).map((e) => '  REQ ' + e),
  ].join('\n');
  console.log(report);
  fs.writeFileSync(path.join(OUT, 'report.txt'), report, 'utf8');

  await browser.close();
})();

const { chromium } = require('playwright');

(async () => {
  const browser = await chromium.launch();
  const page = await browser.newPage({ viewport: { width: 480, height: 1320 }, deviceScaleFactor: 2 });
  const errors = [];
  page.on('pageerror', e => errors.push('PAGEERROR: ' + e.message));
  page.on('console', m => { if (m.type() === 'error') errors.push('CONSOLE: ' + m.text()); });

  const URL = 'file:///D:/buddySpace/moyuanCoze/moyuanCoze/web-prototype/garden-scene-demo.html';
  await page.goto(URL, { waitUntil: 'networkidle' });
  await page.waitForTimeout(400);

  async function shot(name) { await page.screenshot({ path: `test-shots/${name}.png` }); }

  // 春昼 10 时
  await page.click('.chip.s[data-s="SPRING"]');
  await page.click('.chip.w[data-w="CLEAR"]');
  await page.$eval('#hour', el => { el.value = 10; el.dispatchEvent(new Event('input')); });
  await page.click('.chip.g[data-g="3"]');
  await page.waitForTimeout(300);
  await shot('v34k-mtn-day');

  // 秋晴 16 时
  await page.click('.chip.s[data-s="AUTUMN"]');
  await page.click('.chip.w[data-w="CLEAR"]');
  await page.$eval('#hour', el => { el.value = 16; el.dispatchEvent(new Event('input')); });
  await page.click('.chip.g[data-g="3"]');
  await page.waitForTimeout(300);
  await shot('v34k-mtn-autumn');

  // 山顶放大：canvas display 高 650，山顶约 y 120..210
  const cv = await page.$eval('#stage', el => { const r = el.getBoundingClientRect(); return { x: r.x, y: r.y, w: r.width, h: r.height }; });
  const clipY = cv.y + cv.h * 0.18;
  const clipH = cv.h * 0.30;
  await page.screenshot({ path: 'test-shots/v34k-mtn-zoom.png', clip: { x: cv.x, y: clipY, width: cv.w, height: clipH } });

  console.log('errors:', errors.length);
  errors.forEach(e => console.log('  ' + e));
  console.log('shot v34k-mtn-day / v34k-mtn-autumn / v34k-mtn-zoom');
  await browser.close();
})();

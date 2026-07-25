const { chromium } = require('playwright');

(async () => {
  const browser = await chromium.launch();
  const page = await browser.newPage({ viewport: { width: 480, height: 1320 }, deviceScaleFactor: 2 });
  const errors = [];
  page.on('pageerror', e => errors.push('PAGEERROR: ' + e.message));
  page.on('console', m => { if (m.type() === 'error') errors.push('CONSOLE: ' + m.text()); });
  await page.goto('http://localhost:8790/web-prototype/garden-scene-demo.html', { waitUntil: 'networkidle' });
  await page.waitForSelector('#stage');
  await page.waitForTimeout(1200);

  // 春 晴 昼 10点 —— 远山轮廓最清楚的视角
  await page.click('.chip.s[data-s="SPRING"]');
  await page.click('.chip.w[data-w="CLEAR"]');
  await page.$eval('#hour', el => { el.value = 10; el.dispatchEvent(new Event('input')); });
  await page.waitForTimeout(900);
  await page.screenshot({ path: 'test-shots/v34j-mtn-day.png' });
  console.log('shot v34j-mtn-day');

  // 秋 晴 暮 —— 验证另一季远山轮廓同样清晰
  await page.click('.chip.s[data-s="AUTUMN"]');
  await page.$eval('#hour', el => { el.value = 16; el.dispatchEvent(new Event('input')); });
  await page.waitForTimeout(700);
  await page.screenshot({ path: 'test-shots/v34j-mtn-autumn.png' });
  console.log('shot v34j-mtn-autumn');

  // 远山轮廓局部放大（春昼）：上 1/3 画面，确认山脊描边清晰、山脚与地平线衔接不变
  await page.click('.chip.s[data-s="SPRING"]');
  await page.$eval('#hour', el => { el.value = 10; el.dispatchEvent(new Event('input')); });
  await page.waitForTimeout(700);
  await page.screenshot({ path: 'test-shots/v34j-mtn-zoom.png', clip: { x: 0, y: 70, width: 480, height: 300 } });
  console.log('shot v34j-mtn-zoom');

  console.log('ERRORS:', errors.length ? errors.join('\n') : 'none');
  await browser.close();
})();

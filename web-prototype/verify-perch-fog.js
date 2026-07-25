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

  // 雾场景截图（春/雾/昼 10 点）— 验证 v3.4i 雾重做效果
  await page.click('.chip.s[data-s="SPRING"]');
  await page.click('.chip.w[data-w="FOG"]');
  await page.$eval('#hour', el => { el.value = 10; el.dispatchEvent(new Event('input')); });
  await page.waitForTimeout(900);
  await page.screenshot({ path: 'test-shots/v34i-fog.png' });
  console.log('shot v34i-fog');

  console.log('ERRORS:', errors.length ? errors.join('\n') : 'none');
  await browser.close();
})();

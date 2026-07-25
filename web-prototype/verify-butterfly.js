const { chromium } = require('playwright');

(async () => {
  const browser = await chromium.launch();
  const page = await browser.newPage({ viewport: { width: 480, height: 1320 }, deviceScaleFactor: 2 });
  const errors = [];
  page.on('pageerror', e => errors.push('PAGEERROR: ' + e.message));
  page.on('console', m => { if (m.type() === 'error') errors.push('CONSOLE: ' + m.text()); });
  await page.goto('http://localhost:8790/web-prototype/garden-scene-demo.html', { waitUntil: 'networkidle' });
  await page.waitForSelector('#stage');
  await page.waitForTimeout(1500);

  // 春昼：蝴蝶在画面中下部，裁一块看身形
  await page.click('.chip.s[data-s="SPRING"]');
  await page.click('.chip.w[data-w="CLEAR"]');
  await page.$eval('#hour', el => { el.value = 10; el.dispatchEvent(new Event('input')); });
  await page.waitForTimeout(900);

  const box = await page.evaluate(() => {
    const b = document.querySelector('canvas').getBoundingClientRect();
    return { left: b.left, top: b.top, width: b.width, height: b.height };
  });
  const sx = box.width / 800, sy = box.height / 1300;
  // 蝴蝶活动区大致 x:0.14W~0.74W, y:0.59H~0.71H → 裁中部
  const ax = 800 * 0.30, ay = 1300 * 0.66, R = 120;
  await page.screenshot({
    path: 'test-shots/v34h-butterflies.png',
    clip: { x: box.left + (ax - R) * sx, y: box.top + (ay - R) * sy, width: R * 2 * sx, height: R * 2 * sy }
  });
  console.log('shot v34h-butterflies');
  console.log('ERRORS:', errors.length ? errors.join('\n') : 'none');
  await browser.close();
})();

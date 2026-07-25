const { chromium } = require('playwright');

(async () => {
  const browser = await chromium.launch();
  const page = await browser.newPage({ viewport: { width: 480, height: 1320 }, deviceScaleFactor: 2 });
  const errors = [];
  page.on('pageerror', e => errors.push('PAGEERROR: ' + e.message));
  page.on('console', m => { if (m.type() === 'error') errors.push('CONSOLE: ' + m.text()); });

  await page.goto('http://localhost:8790/web-prototype/garden-scene-demo.html', { waitUntil: 'networkidle' });
  await page.waitForSelector('#stage');
  await page.waitForTimeout(1500); // let assets + a few frames settle

  async function shot(name) {
    await page.waitForTimeout(700);
    await page.screenshot({ path: `test-shots/v34-${name}.png` });
    console.log('shot', name);
  }

  // 1) 默认 春 晴 昼
  await shot('spring-day');

  // 2) 秋 晴 暮（落日）
  await page.click('.chip.s[data-s="AUTUMN"]');
  await page.click('.chip.w[data-w="CLEAR"]');
  await page.$eval('#hour', el => { el.value = 17.5; el.dispatchEvent(new Event('input')); });
  await shot('autumn-dusk');

  // 3) 冬 雪 夜
  await page.click('.chip.s[data-s="WINTER"]');
  await page.click('.chip.w[data-w="SNOW"]');
  await page.$eval('#hour', el => { el.value = 21; el.dispatchEvent(new Event('input')); });
  await shot('winter-snow-night');

  // 4) 盛庭 5x5 春 昼
  await page.click('.chip.s[data-s="SPRING"]');
  await page.click('.chip.w[data-w="CLEAR"]');
  await page.$eval('#hour', el => { el.value = 10; el.dispatchEvent(new Event('input')); });
  await page.click('.chip.g[data-g="5"]');
  await shot('spring-day-grid5');

  // 5) 夏 夜 流萤（验证木灯笼 + 月洞门夜景）
  await page.click('.chip.s[data-s="SUMMER"]');
  await page.click('.chip.w[data-w="CLEAR"]');
  await page.$eval('#hour', el => { el.value = 20.5; el.dispatchEvent(new Event('input')); });
  await page.click('.chip.g[data-g="4"]');
  await shot('summer-night');

  // 6) 春 雾 昼（v3.4i 雾重做后验证）
  await page.click('.chip.s[data-s="SPRING"]');
  await page.click('.chip.w[data-w="FOG"]');
  await page.$eval('#hour', el => { el.value = 10; el.dispatchEvent(new Event('input')); });
  await shot('spring-fog-day');

  console.log('ERRORS:', errors.length ? errors.join('\n') : 'none');
  await browser.close();
})();

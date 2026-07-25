const { chromium } = require('playwright');

(async () => {
  const browser = await chromium.launch();
  const page = await browser.newPage({ viewport: { width: 480, height: 1000 } });
  const errors = [];
  page.on('console', m => { if (m.type() === 'error') errors.push(m.text()); });
  page.on('pageerror', e => errors.push(String(e)));

  await page.goto('http://127.0.0.1:8790/web-prototype/garden-scene-demo.html', { waitUntil: 'networkidle' });
  await page.waitForTimeout(2500);

  const setSeason = s => page.click(`.chip.s[data-s="${s}"]`);
  const setWeather = w => page.click(`.chip.w[data-w="${w}"]`);
  const setHour = async h => { await page.fill('#hour', String(h)); await page.dispatchEvent('#hour', 'input'); };

  const shots = [
    { name: 'v31-spring-day',   setup: async () => { await setSeason('SPRING'); await setWeather('CLEAR'); await setHour(10); } },
    { name: 'v31-summer-day',   setup: async () => { await setSeason('SUMMER'); await setHour(11); } },
    { name: 'v31-autumn-day',   setup: async () => { await setSeason('AUTUMN'); await setHour(15); } },
    { name: 'v31-winter-day',   setup: async () => { await setSeason('WINTER'); await setHour(11); } },
    { name: 'v31-summer-night', setup: async () => { await setSeason('SUMMER'); await setHour(20.5); } },
    { name: 'v31-winter-snow-night', setup: async () => { await setSeason('WINTER'); await setWeather('SNOW'); await setHour(21); } },
  ];

  for (const s of shots) {
    await s.setup();
    await page.waitForTimeout(1200);
    await page.screenshot({ path: `test-shots/${s.name}.png`, clip: { x: 0, y: 0, width: 480, height: 900 } });
    console.log('shot:', s.name);
  }

  // 指针滑过交互：在花园区域来回移动，验证无报错
  await setSeason('SPRING'); await setWeather('CLEAR'); await setHour(10);
  const box = await page.locator('#stage').boundingBox();
  for (let i = 0; i <= 20; i++) {
    await page.mouse.move(box.x + box.width * (0.25 + 0.5 * (i / 20)), box.y + box.height * 0.62);
    await page.waitForTimeout(40);
  }
  await page.screenshot({ path: 'test-shots/v31-pointer-sway.png', clip: { x: 0, y: 0, width: 480, height: 900 } });
  console.log('shot: v31-pointer-sway (pointer interaction OK)');

  console.log(errors.length ? 'ERRORS:\n' + errors.join('\n') : 'NO CONSOLE ERRORS');
  await browser.close();
})();

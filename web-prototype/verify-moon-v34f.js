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

  // 进入夜晚，确保月亮被绘制
  await page.click('.chip.s[data-s="SUMMER"]');
  await page.click('.chip.w[data-w="CLEAR"]');
  await page.$eval('#hour', el => { el.value = 21; el.dispatchEvent(new Event('input')); });
  await page.waitForTimeout(400);

  const phases = [
    { sel: '0.12', name: 'crescent' },
    { sel: '0.40', name: 'waxing' },
    { sel: '0.50', name: 'full' },
    { sel: '0.60', name: 'waning' },
  ];

  for (const { sel, name } of phases) {
    await page.click(`.chip.p[data-p="${sel}"]`);
    await page.waitForTimeout(450);

    const analysis = await page.evaluate(() => {
      const cv = document.querySelector('canvas');
      const ctx = cv.getContext('2d');
      const W = cv.width, H = cv.height;
      const mx = W * 0.18, my = H * 0.13, r = 30;
      const img = ctx.getImageData(0, 0, W, H).data;
      const lum = (x, y) => { const i = (y * W + x) * 4; return 0.299 * img[i] + 0.587 * img[i + 1] + 0.114 * img[i + 2]; };
      const out = [];
      for (let y = Math.floor(my - r - 2); y <= Math.ceil(my + r + 2); y += 2) {
        const xs = [];
        for (let x = Math.floor(mx - r - 2); x <= Math.ceil(mx + r + 2); x++) {
          if (lum(x, y) > 150) xs.push(x);
        }
        if (xs.length) out.push(`y=${y}:[${xs[0]}..${xs[xs.length - 1]}] len=${xs.length}`);
      }
      return out;
    });
    console.log(`phase ${sel} (${name}):`, analysis.join('  '));

    const box = await page.evaluate(() => {
      const b = document.querySelector('canvas').getBoundingClientRect();
      return { left: b.left, top: b.top, width: b.width, height: b.height };
    });
    const sx = box.width / 800, sy = box.height / 1300;
    const mx = 800 * 0.18, my = 1300 * 0.13, r = 30;
    await page.screenshot({
      path: `test-shots/v34f-moon-${name}.png`,
      clip: { x: box.left + (mx - r - 4) * sx, y: box.top + (my - r - 4) * sy, width: (r * 2 + 8) * sx, height: (r * 2 + 8) * sy }
    });
    console.log('shot v34f-moon-' + name);
  }

  console.log('ERRORS:', errors.length ? errors.join('\n') : 'none');
  await browser.close();
})();

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

  // 白天场景：看小太阳 + 更清晰的远山
  await page.click('.chip.s[data-s="SPRING"]');
  await page.click('.chip.w[data-w="CLEAR"]');
  await page.$eval('#hour', el => { el.value = 10; el.dispatchEvent(new Event('input')); });
  await page.waitForTimeout(600);

  // 裁太阳区域（白天太阳在右上 cx=W*0.80, cy=H*0.085）
  const box = await page.evaluate(() => {
    const b = document.querySelector('canvas').getBoundingClientRect();
    return { left: b.left, top: b.top, width: b.width, height: b.height };
  });
  const sx = box.width / 800, sy = box.height / 1300;
  const cx = 800 * 0.80, cy = 1300 * 0.085, R = 70; // 含光晕
  await page.screenshot({
    path: 'test-shots/v34g-sun-day.png',
    clip: { x: box.left + (cx - R) * sx, y: box.top + (cy - R) * sy, width: R * 2 * sx, height: R * 2 * sy }
  });
  console.log('shot v34g-sun-day');

  // 硬边扫描：确认远山 alpha 提升未引入新水平带
  const scan = await page.evaluate(() => {
    const cv = document.querySelector('canvas');
    const ctx = cv.getContext('2d');
    const W = cv.width, H = cv.height;
    const img = ctx.getImageData(0, 0, W, H).data;
    const lum = (x, y) => { const i = (y * W + x) * 4; return 0.299 * img[i] + 0.587 * img[i + 1] + 0.114 * img[i + 2]; };
    let bands = [];
    let prev = null;
    for (let y = 100; y < 700; y += 1) {
      let spread = 0, rowsum = 0, n = 0;
      for (let x = 0; x < W; x += 4) { const v = lum(x, y); rowsum += v; n++; }
      const rowAvg = rowsum / n;
      if (prev !== null) {
        const rowDiff = Math.abs(rowAvg - prev);
        // 水平硬带特征：行内均匀(spread 小) 且 行间突变(rowDiff 大)
        if (rowDiff > 30) bands.push({ y, rowDiff: Math.round(rowDiff) });
      }
      prev = rowAvg;
    }
    return bands;
  });
  console.log('HARD EDGE BANDS (y,rowDiff>30):', scan.length ? JSON.stringify(scan) : 'CLEAN');

  console.log('ERRORS:', errors.length ? errors.join('\n') : 'none');
  await browser.close();
})();

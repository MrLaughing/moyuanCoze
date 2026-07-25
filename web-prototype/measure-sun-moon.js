const { chromium } = require('playwright');

(async () => {
  const browser = await chromium.launch();
  const page = await browser.newPage({ viewport: { width: 480, height: 1320 }, deviceScaleFactor: 2 });
  await page.goto('http://localhost:8790/web-prototype/garden-scene-demo.html', { waitUntil: 'networkidle' });
  await page.waitForSelector('#stage');
  await page.waitForTimeout(1500);

  async function probe(setup, cx, cy, metric, label) {
    await page.evaluate(setup);
    await page.waitForTimeout(700);
    const r = await page.evaluate(({ cx, cy, metric }) => {
      const cv = document.querySelector('canvas');
      const ctx = cv.getContext('2d');
      const W = cv.width, H = cv.height;
      const img = ctx.getImageData(0, 0, W, H).data;
      const val = (x, y) => { const i = (y * W + x) * 4;
        if (metric === 'warm') return img[i] - img[i + 2];
        return 0.299 * img[i] + 0.587 * img[i + 1] + 0.114 * img[i + 2]; };
      const y = Math.round(cy);
      const bg = (val(Math.min(W - 1, cx + 200), y) + val(Math.max(0, cx - 200), y)) / 2;
      const thr = bg + (metric === 'warm' ? 60 : 45);
      let xL = cx, xR = cx;
      for (let x = cx; x >= 0; x--) { if (val(x, y) > thr) xL = x; else break; }
      for (let x = cx; x < W; x++) { if (val(x, y) > thr) xR = x; else break; }
      return { bg: Math.round(bg), halfW: Math.round((xR - xL) / 2) };
    }, { cx, cy, metric });
    console.log(`${label}: center(${cx},${cy}) bg=${r.bg}, half-width ≈ ${r.halfW}px → 直径 ≈ ${r.halfW * 2}px`);
    return r;
  }

  // hour=10 → sun 实际中心 cx=800*(0.80+0.06*sin(0.364π))≈684, cy=1300*(0.085-0.02*sin(0.364π))≈87
  await probe(() => { state.season = 'SPRING'; state.weather = 'CLEAR'; state.hour = 10; }, 684, 87, 'warm', 'SUN (day)  ');
  await probe(() => { state.season = 'SUMMER'; state.weather = 'CLEAR'; state.hour = 21; state.phase = 0.50; }, 144, 169, 'lum',  'MOON(full)');

  await browser.close();
})();

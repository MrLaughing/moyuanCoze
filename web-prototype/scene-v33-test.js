const { chromium } = require('playwright');
(async () => {
  const browser = await chromium.launch();
  const page = await browser.newPage({viewport:{width:400,height:780}});
  await page.goto('http://127.0.0.1:8790/web-prototype/garden-scene-demo.html');
  await page.waitForTimeout(2500);
  const setSeason = s => page.click(`.chip.s[data-s="${s}"]`);
  const setWeather = w => page.click(`.chip.w[data-w="${w}"]`);
  const setGrid = g => page.click(`.chip.g[data-g="${g}"]`);

  // 3×3 春昼
  await setSeason('SPRING'); await setWeather('CLEAR'); await setGrid('3');
  await page.waitForTimeout(800);
  await page.screenshot({path:'D:/buddySpace/moyuanCoze/moyuanCoze/web-prototype/test-shots/v33-grid3-spring.png'});
  // 3×3 夏夜
  await setSeason('SUMMER');
  await page.evaluate(()=>{ state.hour=20.5; });
  await page.waitForTimeout(800);
  await page.screenshot({path:'D:/buddySpace/moyuanCoze/moyuanCoze/web-prototype/test-shots/v33-grid3-summer-night.png'});
  // 5×5 冬雪夜
  await setSeason('WINTER'); await setWeather('SNOW'); await setGrid('5');
  await page.evaluate(()=>{ state.hour=21; });
  await page.waitForTimeout(2200);
  await page.screenshot({path:'D:/buddySpace/moyuanCoze/moyuanCoze/web-prototype/test-shots/v33-grid5-winter-snow-night.png'});
  // 4×4 春昼（雅庭）
  await setSeason('SPRING'); await setWeather('CLEAR'); await setGrid('4');
  await page.evaluate(()=>{ state.hour=15; });
  await page.waitForTimeout(900);
  await page.screenshot({path:'D:/buddySpace/moyuanCoze/moyuanCoze/web-prototype/test-shots/v33-grid4-spring.png'});
  console.log('shots done');
  await browser.close();
})();

const CDP = require('chrome-remote-interface');
const fs = require('fs');
const path = require('path');
const { spawnSync } = require('child_process');

const URL = 'http://127.0.0.1:8790/web-prototype/garden-scene-demo.html';

(async () => {
  // 获取已启动的 chrome 实例
  const ports = [9222,9223,9224,9225];
  let target=null;
  for(const p of ports){
    try{
      const r = await (await fetch(`http://127.0.0.1:${p}/json/version`)).json();
      target = {host:'127.0.0.1', port:p, wsURL:r.webSocketDebuggerUrl};
      break;
    }catch(e){}
  }
  if(!target){ console.error('no chrome debug'); process.exit(2); }

  const client = await CDP({target});
  const {Network,Page,Runtime,Input,Emulation} = client;
  await Promise.all([Network.enable(),Page.enable(),Runtime.enable()]);
  await Page.navigate({url:URL});
  await Page.loadEventFired();
  await new Promise(r=>setTimeout(r,1800));
  // 设置夏季夜晚
  await Runtime.evaluate({expression:`
    state.season='SUMMER'; state.hour=20.5; state.weather='CLEAR'; state.grid=4;
    document.querySelectorAll('.chip').forEach(b=>b.classList.remove('on'));
    document.querySelector('[data-g=\"4\"]').classList.add('on');
  `});
  // 跑几帧
  await new Promise(r=>setTimeout(r,700));
  const evalRes = await Runtime.evaluate({expression:`
    (function(){
      const W = window.innerWidth, H = window.innerHeight;
      // 取 anchor
      const n = state.grid;
      const cells = layoutCells(n, W, H);
      const ac = cells[(n-1)*n+0];
      const lx = ac.x - ac.stepX*2.0, ly = ac.y + ac.stepY*1.1;
      return JSON.stringify({W,H, ac:{x:ac.x,y:ac.y, s:ac.persp, sx:ac.stepX, sy:ac.stepY}, lantern:{lx,ly, sScale:ac.persp*1.55}});
    })();
  `, returnByValue:true});
  console.log('LANTERN POSITION:', evalRes.result.value);
  await client.close();
})();

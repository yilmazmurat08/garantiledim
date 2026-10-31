const { chromium } = require('playwright');
const { spawn } = require('child_process');
const [,, M, lang, mode, out] = process.argv;   // mode: "preview" | "video"
(async () => {
  const b = await chromium.launch({ args: ['--allow-file-access-from-files'] });
  const p = await b.newPage({ viewport: { width: 1920, height: 1080 } });
  await p.goto('file://' + M + '/video.html?lang=' + lang);
  await p.waitForSelector('body[data-ready]'); await p.waitForTimeout(300);
  if (mode === 'preview') {
    const times = [1.2, 3.5, 5, 7, 10, 13, 16.5, 22.8, 29, 34.5, 39, 43.5];
    for (const t of times) { await p.evaluate(t => render(t), t); await p.screenshot({ path: `${out}/pv-${lang}-${t}.jpg`, type: 'jpeg', quality: 80 }); }
  } else {
    const fps = 30, dur = 46;
    const ff = spawn('ffmpeg', ['-y', '-loglevel', 'error', '-f', 'image2pipe', '-framerate', String(fps), '-i', '-', '-c:v', 'libx264', '-preset', 'medium', '-crf', '18', '-pix_fmt', 'yuv420p', out]);
    for (let i = 0; i < fps * dur; i++) {
      await p.evaluate(t => render(t), i / fps);
      const buf = await p.screenshot({ type: 'jpeg', quality: 93 });
      if (!ff.stdin.write(buf)) await new Promise(r => ff.stdin.once('drain', r));
      if (i % 150 === 0) console.log('frame', i);
    }
    ff.stdin.end(); await new Promise(r => ff.on('close', r));
  }
  await b.close();
})();

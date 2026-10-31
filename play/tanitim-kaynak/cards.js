const { chromium } = require('playwright');
const M = process.argv[2];
const T = {
 tr: [
  ['01','01-ana-sayfa.png','GARANTİLEDİM','İade ve garanti <em>tek yerde</em>','Hangi ürünün süresi ne zaman bitiyor, bir bakışta gör.'],
  ['02','03-detay.png','SON GÜNÜ KAÇIRMA','Kalan süre <em>her an</em> gözünün önünde','İade ve garanti için geri sayım ve ilerleme çubuğu.'],
  ['03','06-ajanda.png','HATIRLATMA','Süre dolmadan <em>haber ver</em>','Bitişten günler önce bildirim al, saatini sen seç.'],
  ['04','08-yeni-urun.png','FİŞ & FATURA','Fişini çek, <em>faturanı sakla</em>','Fotoğraf ya da PDF, orijinal haliyle güvende.'],
  ['05','02-belgelerim.png','HIZLI ERİŞİM','Ara, filtrele, <em>hemen bul</em>','Yaklaşan, aktif ya da süresi dolmuş tüm ürünlerin.'],
  ['06','13-premium-nofiyat.png','PREMIUM','Premium ile <em>sınırsız</em>','Sınırsız ürün ve yedekleme · tek seferlik ödeme, abonelik yok.'],
  ['07','07-profil.png','GİZLİLİK','Verilerin <em>sadece telefonunda</em>','Hesap yok, reklam yok, takip yok. Türkçe ve İngilizce.'],
 ],
 en: [
  ['01','21-en-ana-sayfa.png','GARANTILEDIM','Returns & warranties <em>in one place</em>','See at a glance when each period ends.'],
  ['02','23-en-detay.png','NEVER MISS A DEADLINE','Time left, <em>always in sight</em>','A countdown and progress bar for every return and warranty.'],
  ['03','24-en-ajanda.png','REMINDERS','Get reminded <em>before it ends</em>','Notifications days before the end date, at the time you choose.'],
  ['04','22-en-belgelerim.png','QUICK ACCESS','Search, filter, <em>find it fast</em>','Upcoming, active or expired — all your products.'],
  ['05','25-en-premium-nofiyat.png','PREMIUM','Go <em>unlimited</em> with Premium','Unlimited products and backup · one-time payment, no subscription.'],
  ['06','20-en-profil.png','PRIVACY','Your data stays <em>on your phone</em>','No account, no ads, no tracking. English and Turkish.'],
 ],
};
(async () => {
  const b = await chromium.launch({ args: ['--allow-file-access-from-files'] });
  const p = await b.newPage({ viewport: { width: 1080, height: 1920 } });
  for (const lang of Object.keys(T)) for (const [n, img, k, h, s] of T[lang]) {
    const u = new URL('file://' + M + '/card.html');
    u.searchParams.set('img', img); u.searchParams.set('k', k); u.searchParams.set('h', h); u.searchParams.set('s', s);
    await p.goto(u.href); await p.waitForSelector('body[data-ready]'); await p.waitForTimeout(150);
    await p.screenshot({ path: `${M}/out/${lang}-${n}.png` });
  }
  await b.close();
})();

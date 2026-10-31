# Tanıtım görselleri ve videosunun kaynakları

Uygulamanın gerçek ekran görüntülerinden (CI'daki `ci-screenshots` dalı) üretilir.

- `card.html` + `cards.js`: 1080x1920 başlıklı mağaza görselleri (`play/tanitim-gorselleri-tr`, `-en`)
- `video.html` + `frames.js`: 1920x1080, 30 fps, 46 sn tanıtım videosu; kare kare Chromium ile çizilir, ffmpeg ile H.264'e çevrilir
- `music.py`: videonun özgün, tamamen sentezlenmiş müziği (120 BPM, fragman tarzı; telif içermez)

Çalıştırmak için bir klasöre şunlar konur: bu dosyalar, `app/src/main/res/font/poppins_*.ttf`, `design/logo.svg` ve `shots/` altında ekran görüntüleri. Sonra:

```
node cards.js <klasör>
node frames.js <klasör> tr video video-tr-sessiz.mp4
python3 music.py muzik.wav
ffmpeg -i video-tr-sessiz.mp4 -i muzik.wav -c:v copy -c:a aac -b:a 256k -shortest Garantiledim-tanitim-tr.mp4
```

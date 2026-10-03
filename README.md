# Garantiledim

Satın alınan ürünlerin iade hakkı ve garanti sürelerini tek yerde takip eden Android uygulaması. Hesap ya da sunucu gerektirmez; tüm veriler cihazda tutulur.

- Ürün gereksinimleri ve tasarım kuralları: [`SPEC.md`](SPEC.md)
- Tasarım referansları ve logo: [`design/`](design)

## Projeyi açma

1. Android Studio'da **File → Open** ile bu klasörü aç. Gradle eşitlemesi gerekli SDK ve kütüphaneleri indirir.
2. Android Studio sürüm yükseltmesi önerirse (Android Gradle Plugin, Kotlin) kabul edebilirsin.
3. Bir emülatör ya da telefon seçip **Run** ile çalıştır. En düşük sürüm Android 8.0 (API 26).

Birim testleri: `./gradlew test`

## Yapı

```
app/src/main/java/com/garantiledim/app/
├── domain/   Süre hesaplama, sıralama, filtreler, Türkçe tarih biçimleri (Android'den bağımsız, testli)
├── data/     Room veritabanı, DataStore ayarları, fiş/fatura dosya işlemleri
└── ui/       Compose ekranları, tema, ortak bileşenler, gezinme
```

## Durum

- [x] Tasarım sistemi, logo ve uygulama ikonu
- [x] Ana Sayfa, Garanti Belgelerim, Ürün ekle/düzenle, Ürün detayı, Ajanda, Profil & Ayarlar
- [x] Bir üründe iade ve garanti süresini birlikte takip etme
- [x] Fiş/fatura (fotoğraf veya PDF) ekleme, önizleme ve İndirilenler'e kaydetme
- [ ] Bildirimler (WorkManager)
- [ ] Premium (deneme, abonelik, yedekleme)

Yazı tipi: Poppins, SIL Open Font License 1.1 ([`licenses/Poppins-OFL.txt`](licenses/Poppins-OFL.txt)).

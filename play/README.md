# Play Console kurulumu

Garantiledim'i Play Console'a eklerken kullanılacak metinler, görseller ve form cevapları. Cevaplar uygulamanın gerçek davranışına göre yazıldı: hesap yok, reklam yok, veriler yalnızca telefonda. İnternet yalnızca Google Play Faturalandırma (Premium satın alma) tarafından kullanılır.

## Uygulama oluştur

| Alan | Değer |
|---|---|
| Uygulama adı | Garantiledim |
| Varsayılan dil | Türkçe – tr-TR |
| Uygulama mı, oyun mu | Uygulama |
| Ücretsiz mi, ücretli mi | Ücretsiz (Premium uygulama içi satın almadır; ücretsiz uygulama sonradan ücretliye çevrilemez) |
| Paket adı | com.garantiledim.app (AAB'den otomatik gelir) |

## Mağaza girişi

**Kısa açıklama** (en fazla 80 karakter):

```
İade ve garanti sürelerini takip et, süre dolmadan hatırlatma al.
```

**Tam açıklama** (en fazla 4000 karakter):

```
Aldığın ürünün iade hakkı ne zaman bitiyor? Garantisi hâlâ devam ediyor mu? Fatura nerede? Garantiledim bu soruları tek yerde cevaplar.

Ürünü ve teslim tarihini gir; Garantiledim 14 günlük iade (cayma) hakkını ve 2 yıllık garanti süresini senin için hesaplar, süre dolmadan seni uyarır. Faturanın fotoğrafını ya da PDF'ini ekle, ihtiyaç duyduğunda orijinal haliyle aç veya İndirilenler klasörüne kaydet.

NELER YAPABİLİRSİN
• Bir üründe iade hakkını ve garantiyi birlikte takip et
• Süreler teslim tarihinden otomatik hesaplanır; mağazanın farklı iade süresi ya da ek garanti için tarihi elle değiştirebilirsin
• Fişi kamerayla çek ya da galeriden, dosyalardan fotoğraf veya PDF fatura ekle; dosya sıkıştırılmadan, orijinal haliyle saklanır
• Faturayı tek dokunuşla İndirilenler klasörüne kaydet
• Bitişe birkaç gün kala her gün, son gün ve süre dolduğunda bildirim al; hatırlatma başlangıcını ve saatini sen seç
• Ajanda ekranında yaklaşan tüm bitiş tarihlerini haftalara göre gör
• Ürünlerini ara, yaklaşan, aktif ya da süresi dolmuş olarak filtrele

PREMIUM
3 ürüne kadar ücretsiz. Garantiledim Premium ile:
• Sınırsız ürün ve hatırlatma
• Ürünlerini, faturalarını ve fotoğraflarını tek dosyaya yedekle, yeni telefona geri yükle
• Tek seferlik ödeme: abonelik yok, otomatik yenileme yok

GİZLİLİĞİN SENDE
• Hesap açman gerekmez
• Bilgilerin ve belgelerin yalnızca telefonunda saklanır; uygulamanın sunucusu yok
• Reklam yok, takip yok

Türkiye'deki tüketici mevzuatına göre mesafeli satışta cayma hakkı teslimden itibaren 14 gün, yasal garanti en az 2 yıldır. Garantiledim bu süreleri varsayılan olarak kullanır; hukuki tavsiye yerine geçmez.

Sorun ya da önerin için: yilmazmurat08@gmail.com
```

**Görseller**

| Alan | Dosya |
|---|---|
| Uygulama simgesi (512×512) | `design/play-store-ikon-512.png` |
| Öne çıkan görsel (1024×500) | `play/tanitim-gorseli-1024x500.png` |
| Telefon ekran görüntüleri (2–8 adet) | `play/ekran-goruntuleri/` içindeki dosyalar, numara sırasıyla |

Ekran görüntüleri 1200×2400 boyutundadır. Play, uzun kenarın kısa kenarın iki katını geçmemesini istediği için emülatör görüntüleri yanlardan arka plan rengiyle genişletildi.

**Kategori ve iletişim**

| Alan | Değer |
|---|---|
| Uygulama kategorisi | Verimlilik |
| Etiketler | Alışveriş, Organizasyon (isteğe bağlı) |
| E-posta | yilmazmurat08@gmail.com |
| Web sitesi | Boş bırakılabilir |
| Telefon | Boş bırakılabilir |

## Uygulama içeriği (Politika > Uygulama içeriği)

**Gizlilik politikası**

```
https://github.com/yilmazmurat08/garantiledim/blob/main/app/src/main/assets/legal/gizlilik-politikasi.md
```

Bu bağlantı, bu değişiklikler `main` dalına birleştirildikten sonra güncel metni gösterir. Depo herkese açık kalmalıdır; depo gizli yapılırsa bağlantı açılmaz ve Play uygulamayı reddeder.

**Uygulama erişimi:** "Tüm işlevler özel erişim gerekmeden kullanılabilir" (giriş yok).

**Reklamlar:** "Hayır, uygulamamda reklam yok".

**İçerik derecelendirmesi:** Anketi doldururken kategori olarak "Diğer tüm uygulama türleri" ya da "Yardımcı program, Verimlilik, İletişim veya Diğer" seçilir. Şiddet, cinsellik, kumar, uyuşturucu, kullanıcılar arası iletişim ve konum paylaşımı sorularına "Hayır"; **dijital ürün satın alma** sorusuna "Evet" (Premium uygulama içi satın alma). Beklenen sonuç: 3+ / Herkes, "Uygulama içi satın alma" etiketiyle.

**Hedef kitle:** 18 ve üzeri. Uygulama çocuklara yönelik değildir; daha küçük yaş grupları seçilirse Aileler politikası ve ek denetimler devreye girer.

**Veri güvenliği:**

Premium için eklenen Google Play Faturalandırma kitaplığı internet izni ister ve kendi çalışmasıyla ilgili teknik tanılama bilgilerini Google'a gönderir. Uygulamanın kendisi hiçbir kullanıcı verisini cihaz dışına göndermez. Google, bu kitaplığın gönderdiği tanılama bilgilerinin formda beyan edilip edilmeyeceğini açıkça yazmıyor. Bu yüzden ihtiyatlı cevap öneriliyor (fazla beyan sorun çıkarmaz, eksik beyan çıkarabilir):

| Soru | Cevap |
|---|---|
| Uygulamanız gerekli kullanıcı verisi türlerinden herhangi birini topluyor veya paylaşıyor mu? | Evet |
| Toplanan veriler aktarım sırasında şifreleniyor mu? | Evet |
| Kullanıcılar verilerinin silinmesini isteyebilir mi? | Hayır (geliştiricide veri yok; tüm kayıtlar telefonda, uygulama silinince gider) |
| Veri türü | Uygulama bilgileri ve performansı → **Tanılama** |
| Toplanıyor mu / paylaşılıyor mu | Toplanıyor, paylaşılmıyor |
| Geçici olarak mı işleniyor | Hayır |
| Zorunlu mu, isteğe bağlı mı | Zorunlu |
| Amaç | Uygulama işlevselliği, Analiz |

Satın alma geçmişi beyan edilmez: ödeme Google Play'de yapılır, uygulama yalnızca "Premium satın alındı mı" bilgisini telefonda tutar, hiçbir sunucuya göndermez. Girilen ürünler, fotoğraflar ve faturalar da cihazdan çıkmaz. Android'in Google yedeklemesi ve kullanıcının kendi seçtiği yere kaydettiği yedek dosyası geliştiriciye veri göndermez.

**Devlet uygulaması:** Hayır. **Finansal özellikler:** "Uygulamamda finansal özellik yok". **Sağlık:** Sağlık özelliği yok. **Haber uygulaması:** Hayır.

## Premium (uygulama içi ürün)

Play Console'da uygulama içi ürün oluşturmak için önce Faturalandırma izni içeren bir AAB'nin (Premium'u içeren ilk sürüm) herhangi bir test kanalına yüklenmiş olması gerekir.

1. **Para kazanma > Ürünler > Uygulama içi ürünler > Ürün oluştur**
2. Ürün kimliği: `garantiledim_premium` (koddaki kimlik; sonradan değiştirilemez, birebir yazılmalı)
3. Ad: `Garantiledim Premium`
4. Açıklama: `Sınırsız ürün ve hatırlatma, yedekleme ve geri yükleme. Tek seferlik ödeme.`
5. Fiyat: Türkiye için **₺709,99** (aşağıdaki hesap). Diğer ülkelerin fiyatlarını Play otomatik çevirir. Fiyat uygulamaya Play'den gelir, kodda yazılı değildir; sonradan Play Console'dan değiştirilebilir.
6. Ürünü **Etkinleştir**.

**Fiyat hesabı (hedef: satış başına net ~₺500)**

Play'de girilen Türkiye fiyatı KDV dahildir. KDV'yi (%20) Google tahsil edip öder, kalan tutardan Google'ın hizmet ücreti kesilir:

| Durum | Formül | Önerilen fiyat | Sana kalan |
|---|---|---|---|
| %15 hizmet ücreti (yıllık ilk 1 milyon $; Play Console'da **15% hizmet ücreti kademesine** kayıt gerekir) | fiyat ÷ 1,20 × 0,85 | **₺709,99** | ~₺503 |
| %15 + %7,5 dijital hizmet vergisi de senden kesilirse | fiyat ÷ 1,20 × (0,85 − 0,075) | ₺779,99 | ~₺504 |
| %30 hizmet ücreti (15% kademesine kayıt yoksa) | fiyat ÷ 1,20 × 0,70 | ₺859,99 | ~₺502 |

15% kademesine kayıt: Play Console > Ayarlar > **Hesap grubu** (Account group) oluştur; ücretsiz ve tek seferliktir. Kendi gelir vergin bu hesaba dahil değildir; kesin oranlar için mali müşavirine danış. Ödemeler Google'dan dolar/TL kuruna göre aylık gelir.

**Lisans anahtarı (zorunlu):** Play Console > **Para kazanma kurulumu** > Lisanslama bölümündeki "Base64 kodlu RSA genel anahtarı"nı kopyala ve depoda `gradle.properties` dosyasındaki `garantiledim.playLicenseKey=` satırına tek satır olarak yapıştır (ya da GitHub sırrı `PLAY_LICENSE_KEY`). Bu genel anahtardır, gizli değildir. Uygulama satın almaların gerçekten Google Play'den geldiğini bu anahtarla doğrular; anahtar olmadan Play AAB'si derlenmez.

Test satın alması: Play Console > Ayarlar > **Lisans testi** listesine kendi Gmail adresini ekle. Bu hesapla kapalı testten kurulan uygulamada satın alma gerçek ücret alınmadan "test kartı" ile yapılır.

## Sürüm oluşturma

1. Play Console > Test ve yayınla > Kurulum > **Uygulama imzalama**: Google'ın uygulama imzalama anahtarını oluşturmasını kabul et (varsayılan seçenek). AAB'yi imzalayan anahtarımız yalnızca **yükleme anahtarıdır**.
2. **Kapalı test** kanalı oluştur (yeni kişisel geliştirici hesaplarında üretime çıkmadan önce en az 12 test kullanıcısının 14 gün boyunca kesintisiz katılması gerekir). Test kullanıcılarının Gmail adreslerini bir e-posta listesine ekle.
3. Yeni sürüm > `garantiledim-<sürüm>.aab` dosyasını yükle.
4. Aynı sürümün **Uygulama paketleri** sayfasından "Yerel kod hata ayıklama sembolleri" değil, **ReTrace eşleme dosyası** olarak `mapping.txt` dosyasını yükle (çökme raporları okunur olsun diye).
5. Sürüm notu örneği:

```
<tr-TR>
Garantiledim'in ilk sürümü: iade ve garanti sürelerini takip et, faturalarını sakla, süre dolmadan hatırlatma al.
</tr-TR>
```

6. Sürümü incelemeye gönder. Test kullanıcıları, Play Console'un verdiği katılım bağlantısıyla uygulamayı Play Store'dan kurar.

Sonraki her AAB'nin sürüm kodu (versionCode) bir öncekinden büyük olmalıdır. Sürüm kodu derleme numarasından gelir (README > Play Store sürümü).

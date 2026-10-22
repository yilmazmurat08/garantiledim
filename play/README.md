# Play Console kurulumu

Garantiledim'i Play Console'a eklerken kullanılacak metinler, görseller ve form cevapları. Cevaplar uygulamanın gerçek davranışına göre yazıldı: hesap yok, internet izni yok, reklam yok, veriler yalnızca telefonda.

## Uygulama oluştur

| Alan | Değer |
|---|---|
| Uygulama adı | Garantiledim |
| Varsayılan dil | Türkçe – tr-TR |
| Uygulama mı, oyun mu | Uygulama |
| Ücretsiz mi, ücretli mi | Ücretsiz (Premium ileride uygulama içi satın alma olarak eklenir; ücretsiz uygulama sonradan ücretliye çevrilemez, bu yüzden "Ücretsiz" seçilir) |
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

GİZLİLİĞİN SENDE
• Hesap açman gerekmez
• Uygulamanın internet izni yoktur; bilgilerin ve belgelerin yalnızca telefonunda saklanır
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

**İçerik derecelendirmesi:** Anketi doldururken kategori olarak "Diğer tüm uygulama türleri" ya da "Yardımcı program, Verimlilik, İletişim veya Diğer" seçilir. Şiddet, cinsellik, kumar, uyuşturucu, kullanıcılar arası iletişim, konum paylaşımı ve dijital satın alma sorularının hepsine "Hayır". Beklenen sonuç: 3+ / Herkes.

**Hedef kitle:** 18 ve üzeri. Uygulama çocuklara yönelik değildir; daha küçük yaş grupları seçilirse Aileler politikası ve ek denetimler devreye girer.

**Veri güvenliği:**

| Soru | Cevap |
|---|---|
| Uygulamanız gerekli kullanıcı verisi türlerinden herhangi birini topluyor veya paylaşıyor mu? | Hayır |

Açıklama: Uygulamanın internet izni yoktur; girilen bilgiler, fotoğraflar ve faturalar cihazdan dışarı gönderilmez. Play'in tanımına göre yalnızca cihazda işlenen veriler "toplanan veri" sayılmaz. Android'in kendi Google yedeklemesi geliştiriciye veri göndermez ve bu formda beyan edilmez. "Hayır" seçilince şifreleme ve silme soruları sorulmaz.

**Devlet uygulaması:** Hayır. **Finansal özellikler:** "Uygulamamda finansal özellik yok". **Sağlık:** Sağlık özelliği yok. **Haber uygulaması:** Hayır.

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

# GARANTİLEDİM — Android Uygulama Spesifikasyonu

Bu doküman Gemini AI'a (veya Android Studio içindeki Gemini'ye, ya da başka bir kod üreten yapay zekaya) verilip "GARANTİLEDİM" uygulamasının üretilmesi için hazırlanmıştır. Aşağıdaki her bölüm doğrudan bir prompt olarak kullanılabilir.

Görsel referans: `design/referans-tasarim.jpg`. Tüm ekranların tasarımı GARANTİLEDİM Tasarım tuvalindedir; bu belgedeki renk ve ölçüler o tasarımla birebir aynıdır.

## 1. Uygulama hakkında

- **Ad:** GARANTİLEDİM
- **Platform:** Android (Kotlin), Play Store'a yayınlanacak
- **Amaç:** Kullanıcının satın aldığı ürünlerin cayma/iade hakkı süresini ve garanti bitiş tarihini tek yerde takip etmesini, süre dolmadan bildirim almasını sağlamak
- **Mimari:** Hesap ve uygulamaya ait bir sunucu gerektirmez, tüm veriler cihazda tutulur (local-first). İleride eklenecek yedekleme de kullanıcının kendi Google Drive hesabını kullanır, ayrı bir sunucu kurulmaz (bkz. madde 10)
- **Önerilen teknoloji:** Kotlin + Jetpack Compose, Room (yerel veritabanı), WorkManager (zamanlanmış bildirimler), Material Design 3, Coil (görsel yükleme)
- **Minimum Android sürümü:** API 26 (Android 8.0) — `java.time` sınıfları desugaring olmadan kullanılabilir

## 2. Tasarım sistemi

Uygulama tek temalıdır: koyu mor zemin üzerinde pembe ve lavanta vurgular, pastel renkli öneri kartları. Sistemin açık/koyu ayarından bağımsız olarak hep bu tema kullanılır.

### 2.1 Renk paleti

| Rol | Renk | Kullanım |
|---|---|---|
| Arka plan | #1C0D33 | Tüm ekranların zemini |
| Yüzey (kart) | #2A1747 | Karşılama kartı, ürün kartları, alt menü, giriş alanları |
| Yüzey yüksek | #362057 | Liste ayırıcıları, ikon butonu zemini |
| Çizgi | #4A3270 | Alan ve çip kenarlıkları, ilerleme çubuğu zemini |
| Metin | #F7F2FF | Birincil metin |
| Metin ikincil | #C7B6E6 | Alt başlık, mağaza · kategori, tarih |
| Metin soluk | #B9A6DC | Bölüm etiketleri, yardımcı metin, placeholder |
| Pembe (ana eylem) | #FF7AB8 | Kaydet, FAB, seçili çip, aktif menü sekmesi, "Tümü" bağlantısı |
| Lavanta (ikincil) | #A78BFA | Pasif menü ikonları, seçili kategori, ikincil vurgu |
| Pastel pembe | #F9B4D6 | "Yeni Garanti Ekle" kartı (iç kutu #FCD3E7, ikon #B8336F) |
| Pastel lavanta | #C4AAFA | "Hatırlatıcı Ayarla" kartı (iç kutu #D9C9FC, ikon #6B3FD1) |
| Pastel şeftali | #F7B48E | "Belgeleri Düzenle" kartı (iç kutu #FACBB0, ikon #A2471F) |
| Pastel üstü metin | #2A0F3D | Pastel kartlar ve pembe butonlar üzerindeki yazı |

Kategori görsel kutuları (ürün fotoğrafı yoksa): Elektronik zemin #3A2360 / ikon #C4AAFA, Giyim zemin #45203F / ikon #F9B4D6, Beyaz eşya ve Diğer zemin #452A2E / ikon #F7B48E.

### 2.2 Kalan süre durum renkleri

| Durum | Koşul | Renk | Rozet |
|---|---|---|---|
| Bol süre | 14 gün ve üstü | #5EDB9F | Yazı rengi, zemin aynı rengin %16'sı |
| Yaklaşıyor | 4–13 gün | #FFC266 | " |
| Son günler | 0–3 gün (hatırlatmalar başladı) | #FF6B8B | " |
| Süre doldu | Bitiş tarihi geçti | #FF6B8B | "Süre doldu" yazar, ürün görseli %55 opaklıkla soluk gösterilir |

Tüm metin/zemin ikilileri WCAG AA kontrast oranını (4.5:1) geçer.

### 2.3 Yazı tipi

Poppins (Google Fonts, Compose'da downloadable font olarak):

| Stil | Boyut / satır | Ağırlık |
|---|---|---|
| Ekran başlığı | 22 / 30 sp | 600 |
| Karşılama başlığı | 20 / 28 sp | 600 |
| Bölüm başlığı | 18 / 24 sp | 600 |
| Kart başlığı | 15 / 20 sp | 600–700 |
| Gövde | 14 / 20 sp | 400 |
| Açıklama | 12 / 16 sp | 400 |
| Rozet, küçük etiket | 11 / 16 sp | 600 |
| Alt menü etiketi | 11 / 13 sp | 500 |

### 2.4 Şekil ve ölçüler

- Köşe yarıçapları: alt menü 24dp, karşılama kartı 20dp, kartlar 18dp, giriş alanları ve butonlar 14dp, çipler ve rozetler tam yuvarlak
- Ekran kenar boşluğu 20dp, kartlar arası boşluk 12dp, bölümler arası 22dp
- Dokunma alanları en az 44dp
- Kenarlık yerine tonlu yüzeyler kullanılır; kenarlık sadece giriş alanlarında, pasif çiplerde ve alt menüde (#4A3270, 1dp)
- İkonlar çizgi (outline) stilinde, 1.8dp kalınlık, yuvarlak uçlu

## 3. İkon ve logo

Referans: `design/logo-referans.jpg` (orijinal), `design/logo-rozet.png` (yalnızca rozet, şeffaf arka planlı kesim).

- **Motif:** Kalite/garanti rozeti (tırtıklı kenarlı madalyon ve altında iki kurdele) ve rozetin önünde, sağ üste taşan büyük onay işareti. Rozet ve onay işareti lavanta-pembe tonlarında (#C9B5F7 → #F2C6E6), onay işaretinin yüzü beyaza yakın
- **Zemin:** Koyu mor daire (#2B1B4F → #3D2470 hafif geçişli), dışında lavanta → pembe parlak halka
- **Yazı (wordmark):** "Garantiledim", kalın geometrik sans (Poppins 700 ile uyumlu). Açık zeminde #1E1838, koyu zeminde #F7F2FF
- **Kullanım yerleri:**
  - Uygulama ikonu (launcher): Adaptive icon; arka plan katmanı düz #2A1747, ön plan katmanı rozet dairesi (66dp güvenli alan içinde)
  - Açılış ekranı: Android 12+ SplashScreen API ile #1C0D33 zemin üzerinde rozet; uygulama içi açılışta rozetin altında "Garantiledim" yazısı ve "Garanti sürelerini takipte kal!"
  - Play Store ikonu: 512×512 PNG
- **Gerekli dosya:** Play Store ve launcher için logonun en az 1024×1024 yüksek çözünürlüklü ya da vektör (SVG) sürümü gerekir. Mevcut referans görsel 512×279 boyutunda; içindeki rozet yaklaşık 160 piksel, bu yüzden ikon olarak doğrudan kullanılırsa bulanık görünür

## 4. Ekranlar

### 4.0 Gezinme

Alt menüde dört sekme (yüzey #2A1747, 24dp yuvarlak, ekranın altında 12dp boşlukla yüzer):

1. **Ana Sayfa** (ev ikonu)
2. **Garanti Belgelerim** (belge ikonu)
3. **Ajanda ve Hatırlatıcılar** (onaylı takvim ikonu)
4. **Profil & Ayarlar** (ayar ikonu)

Aktif sekme pembe (#FF7AB8) ikon + yazı ve üstünde 28×3dp pembe çizgi; pasif sekmeler lavanta (#A78BFA). Ürün ekleme ve ürün detay ekranları alt menüsüz, üstte geri butonlu tam ekranlardır.

### 4.1 Ana Sayfa

- **Karşılama kartı:** Profil fotoğrafı (yoksa adın baş harfi; pembe halkalı yuvarlak), "Merhaba, {ad}!" (ad girilmemişse "Merhaba!"), altında "Garanti sürelerini takipte kal!". Sağda zil butonu; 0–3 gün kalan ya da süresi yeni dolmuş ürün varsa üstünde pembe nokta görünür. Zile basınca Ajanda sekmesi açılır
- **Önerilen İşlemler:** Yatay kaydırılabilen üç pastel kart. Her kartın üst kenarına taşan küçük yuvarlak ikon rozeti, içinde büyük ikonlu bir kutu, başlık ve kısa açıklama:
  - "Yeni Garanti Ekle — Hızlıca yeni ürün ve belgesini kaydet." (pastel pembe) → ürün ekleme ekranı
  - "Hatırlatıcı Ayarla — Yaklaşan bitiş tarihleri için alarm" (pastel lavanta) → Ajanda sekmesi
  - "Belgeleri Düzenle — Tüm kayıtları kolayca filtrele" (pastel şeftali) → Garanti Belgelerim sekmesi
- **Günün Garantileri:** Sağında "Tümü" bağlantısı (Garanti Belgelerim'i açar). İki sütunlu kart ızgarası, en fazla 6 ürün. Önce süresi devam eden ürünler bitiş tarihine en yakından uzağa, ardından son 30 günde süresi dolanlar. Her kart:
  - Üstte mağazanın baş harfli küçük yuvarlağı + mağaza adı
  - Ürün fotoğrafı (yoksa kategori renginde ikonlu kutu)
  - Ürün adı
  - Renkli kalan süre rozeti (madde 6'daki metin biçimiyle, örn. "2 gün kaldı", "2 ay kaldı", "Süre doldu")
  - Saat ikonu + "İade · 5 Eki 2026" / "Garanti · 12 Ara 2026"
  - Karta dokunma → ürün detay ekranı
- **Boş durum:** Hiç ürün yoksa "Günün Garantileri" yerine "İlk ürününü ekle" çağrısı ve pembe buton gösterilir

### 4.2 Garanti Belgelerim

- Başlık "Garanti Belgelerim", altında "{X} ürün takip ediliyor"
- Arama alanı ("Ürün veya mağaza ara"), ürün adı ve mağazada arar
- Filtre çipleri: **Tümü / Yaklaşan** (0–13 gün) **/ Aktif** (süresi devam eden) **/ Süresi dolan**. Seçili çip pembe dolgulu
- Liste (LazyColumn, her öğeye `key = product.id`). Sıralama: süresi devam edenler bitiş tarihine göre artan; süresi dolanlar listenin sonunda "Süresi dolanlar" ara başlığıyla, en yeni dolan üstte
- Her satır: kategori renginde ikon kutusu, ürün adı, "mağaza · kategori", lavanta renkte "İade hakkı · 5 Eki 2026" satırı, sağda renkli kısa rozet ("2 gün", "2 ay", "2 yıl 8 ay", "Süre doldu")
- Sağ altta, alt menünün üstünde pembe + FAB (58dp, 18dp yuvarlak) → ürün ekleme ekranı
- Satıra dokunma → ürün detay ekranı
- Boş durumlar: hiç ürün yoksa ekleme çağrısı; arama/filtre sonucu boşsa "Bu filtreye uyan ürün yok"

### 4.3 Yeni Garanti Ekle (ve düzenleme)

Üstte geri butonu ve başlık. Alanlar yukarıdan aşağıya:

- **Ürün adı \*** (zorunlu; boşsa alanın altında hata gösterilir, kaydedilmez)
- **Mağaza**
- **Satın alma tarihi** ve **Teslim tarihi** yan yana, tarih seçicili. Teslim tarihi boş bırakılırsa satın alma tarihiyle aynı kabul edilir. Teslim tarihi satın alma tarihinden önce olamaz (hata gösterilir)
- **Kategori** çipleri: Elektronik / Beyaz eşya / Giyim / Diğer (seçili çip lavanta dolgulu)
- **Süre türü** segmented control: İade hakkı / Garanti (seçili taraf pembe dolgulu)
- **Bitiş tarihi (opsiyonel)** — etiket süre türüne göre "İade son günü (opsiyonel)" / "Garanti bitişi (opsiyonel)" olur. Boşken placeholder'da otomatik hesaplanan tarih görünür ("Otomatik: 05.10.2026"). Altında yardım metni: "Boş bırakırsan teslim tarihinden itibaren 14 gün (garantide 2 yıl) sayılır. Mağazanın süresi farklıysa buraya yaz." Bitiş tarihi teslim tarihinden önce olamaz
- **Fiş / fatura:** Kesik çizgili iki buton:
  - "Galeriden / dosyadan" → `ActivityResultContracts.OpenDocument` ile `image/*` ve `application/pdf` (e-faturalar genelde PDF gelir)
  - "Kamera ile çek" → `ActivityResultContracts.TakePicture`; çekimden önce uygulamanın kendi dizininde boş bir dosya oluşturulup `FileProvider` URI'si verilir
  - Seçilen içerik URI'si geçicidir; hemen uygulamanın kendi dizinine (`filesDir/receipts/`) **orijinal haliyle, sıkıştırılmadan** kopyalanır. Önizleme için ayrıca küçük bir thumbnail üretilir (`filesDir/thumbs/`; PDF'te `PdfRenderer` ile ilk sayfa)
  - Altında yardım metni: "Fotoğraf veya PDF fatura eklenebilir. Orijinal dosya bozulmadan saklanır."
- **Ürün fotoğrafı ekle (opsiyonel)** → aynı galeri/kamera seçimi; kartlarda ürün görseli olarak kullanılır, aynı şekilde kopyalanıp thumbnail'i üretilir
- Altta sabit, tam genişlikte pembe **Kaydet** butonu

Düzenleme modunda aynı ekran mevcut değerlerle dolu açılır. Kayıt değiştiğinde ürünün bildirim durumu (madde 7) sıfırlanır.

### 4.4 Ürün Detayı

Üstte geri butonu, ortada "Ürün Detayı", sağda düzenle (kalem) butonu. Gösterilenler:

- Büyük ürün görseli (yoksa kategori renginde ikonlu kutu; süre dolmuşsa soluk)
- Ürün adı, "mağaza · kategori"
- **Süre kartı:** Etiket süre türüne göre "İade son günü" / "Garanti bitişi", altında tarih ve gün adı ("5 Ekim 2026, Pazartesi"), sağda renkli kalan süre rozeti, altında durum renginde geçen süre ilerleme çubuğu ve açıklama ("14 günlük iade süresinin 12 günü geçti.")
- 2×2 bilgi kutuları: Satın alma, Teslim, Süre türü, Hatırlatma ("3 gün önce")
- **Fiş/fatura satırı:** Thumbnail (PDF için belge ikonu + "PDF"), dosya adı, "PDF · 1,2 MB · orijinal" ve pembe kenarlıklı **İndir** butonu. İndir, **orijinal tam çözünürlüklü dosyayı** cihazın İndirilenler klasörüne kopyalar:
  - Android 10 (API 29) ve üstü: `MediaStore.Downloads` ile, izin gerekmez
  - Android 8–9 (API 26–28): `WRITE_EXTERNAL_STORAGE` izni (`android:maxSdkVersion="28"`) istenip `Environment.DIRECTORY_DOWNLOADS`'a yazılır
  - Başarılı olunca "Fatura İndirilenler klasörüne kaydedildi" mesajı gösterilir
- Fiş eklenmemişse bu satır yerine "Fiş / fatura ekle" butonu
- Ürünü silme: düzenleme ekranının en altında, onay diyaloğuyla. Silinince dosyaları ve thumbnail'leri de silinir, bildirim kaydı temizlenir

### 4.5 Ajanda ve Hatırlatıcılar

- Başlık "Ajanda ve Hatırlatıcılar"
- Üstte pastel lavanta **Hatırlatma ayarı** kartı: zil ikonu, "Bitişten 3 gün önce başlar, her gün 10:00'da", koyu "Ayarla" butonu (Profil & Ayarlar'daki hatırlatma ayarlarını açar)
- Süresi devam eden ürünlerin zaman çizelgesi, bitiş tarihine göre artan, gruplar: **Bu hafta**, **Bu ay**, **Daha sonra**
- Her satır: solda durum renginde tarih kutusu (gün + kısaltılmış ay, örn. "5 EKİ"), ürün adı, "İade son günü · Pazartesi" / "Garanti bitişi · 2027", sağda kısa rozet. Dokunma → ürün detay
- Bildirim izni reddedilmişse en üstte uyarı kartı: "Bildirimler kapalı, hatırlatma alamazsın" + "Ayarlara git" butonu

### 4.6 Profil & Ayarlar

- **Profil kartı:** Fotoğraf (pembe halkalı), ad, "Bilgilerin sadece bu cihazda saklanır", düzenle butonu (ad ve fotoğraf). Hesap yoktur; ad ve fotoğraf yalnızca karşılama için kullanılır
- **Hatırlatmalar:** Hatırlatma başlangıcı (varsayılan "Bitişten 3 gün önce"), Bildirim saati (varsayılan 10:00), Bildirim izni durumu (Açık yeşil / Kapalı pembe, kapalıysa dokununca sistem ayarına gider)
- **Varsayılan süreler:** İade hakkı 14 gün, Garanti 2 yıl (bilgi amaçlı)
- **Diğer:** Yedekleme ve Premium ("Yakında" etiketi; madde 10'da tasarlanacak), Hakkında (sürüm)

## 5. Veri modeli

```kotlin
@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val store: String,
    val category: Category,
    val purchaseDate: LocalDate,
    val deliveryDate: LocalDate,              // boş bırakılırsa purchaseDate ile aynı kaydedilir
    val durationType: DurationType,
    val manualEndDate: LocalDate? = null,     // kullanıcı elle girdiyse
    val receiptPath: String? = null,          // filesDir/receipts/... orijinal dosya
    val receiptThumbPath: String? = null,     // filesDir/thumbs/... önizleme
    val receiptFileName: String? = null,      // kullanıcıya gösterilen ve indirmede kullanılan ad
    val receiptMimeType: String? = null,      // image/jpeg, application/pdf ...
    val receiptSizeBytes: Long? = null,
    val photoPath: String? = null,            // ürün fotoğrafı (opsiyonel)
    val photoThumbPath: String? = null,
    val lastReminderDate: LocalDate? = null,  // aynı gün iki kez hatırlatma gönderilmesin
    val expiredNotified: Boolean = false,     // "süresi doldu" bildirimi bir kez gönderilsin
    val createdAt: Long = System.currentTimeMillis()
)

enum class Category { ELEKTRONIK, BEYAZ_ESYA, GIYIM, DIGER }
enum class DurationType { IADE, GARANTI }
enum class Urgency { SUCCESS, WARNING, DANGER, EXPIRED }
```

- `LocalDate` alanları için Room `TypeConverter` yazılır (`toEpochDay()` ↔ `LocalDate.ofEpochDay()`); enum'lar isimleriyle saklanır
- Dosya alanlarında içerik URI'si değil, uygulamanın kendi dizinindeki dosya yolu tutulur (dış URI'lere erişim izni zamanla kaybolur)
- Kullanıcı adı, profil fotoğrafı yolu, hatırlatma başlangıcı (gün) ve bildirim saati DataStore'da (Preferences) tutulur

## 6. Süre hesaplama mantığı

Yasal süreler teslim tarihinden başlar (madde 8), bu yüzden hesaplama `deliveryDate` üzerinden yapılır.

```kotlin
fun calculateEndDate(product: Product): LocalDate {
    product.manualEndDate?.let { return it }
    return when (product.durationType) {
        DurationType.IADE -> product.deliveryDate.plusDays(14)
        DurationType.GARANTI -> product.deliveryDate.plusYears(2)
    }
}

fun daysRemaining(endDate: LocalDate, today: LocalDate = LocalDate.now()): Long =
    ChronoUnit.DAYS.between(today, endDate)

fun urgencyLevel(days: Long): Urgency = when {
    days < 0 -> Urgency.EXPIRED
    days <= 3 -> Urgency.DANGER
    days < 14 -> Urgency.WARNING
    else -> Urgency.SUCCESS
}

// Rozet metni: "Bugün son gün", "1 gün kaldı" … "60 gün kaldı", sonra "2 ay kaldı", "1 yıl 3 ay kaldı", "Süre doldu"
fun remainingLabel(endDate: LocalDate, today: LocalDate = LocalDate.now()): String {
    val days = daysRemaining(endDate, today)
    if (days < 0) return "Süre doldu"
    if (days == 0L) return "Bugün son gün"
    if (days <= 60) return "$days gün kaldı"
    val p = Period.between(today, endDate)
    return when {
        p.years > 0 && p.months > 0 -> "${p.years} yıl ${p.months} ay kaldı"
        p.years > 0 -> "${p.years} yıl kaldı"
        else -> "${p.months} ay kaldı"
    }
}
```

Liste ekranındaki kısa rozetlerde aynı metin "kaldı" eki olmadan kullanılır ("2 gün", "2 ay").

## 7. Bildirim sistemi

- **Tek bir günlük iş:** WorkManager ile `PeriodicWorkRequest` (24 saat), benzersiz adla (`enqueueUniquePeriodicWork`, `ExistingPeriodicWorkPolicy.UPDATE`) zamanlanır ve ilk çalışması ayarlardaki bildirim saatine (varsayılan 10:00) denk gelecek şekilde `initialDelay` verilir. Her ürün için ayrı iş zamanlanmaz; iş çalışınca tüm ürünleri tek sorguda kontrol eder. Uygulama açılışında ve bildirim saati değiştiğinde yeniden zamanlanır
- Her ürün için `days = daysRemaining(...)` hesaplanır ve `lastReminderDate` bugün değilse:
  - `1 ≤ days ≤ hatırlatma başlangıcı` (varsayılan 3): "{ürün adı} için {İade hakkı/Garanti} süresinin bitmesine {days} gün kaldı"
  - `days == 0`: "{ürün adı} için {İade hakkı/Garanti} süresi bugün doluyor"
  - Gönderildikten sonra `lastReminderDate = bugün` yazılır (iş aynı gün iki kez çalışırsa tekrar gönderilmez)
- `days < 0` ve `expiredNotified == false`: "{ürün adı} için {İade hakkı/Garanti} süresi doldu" bir kez gönderilir, `expiredNotified = true` yazılır. Kullanıcı bitiş tarihini ileri alırsa bu alan sıfırlanır
- Telefon birkaç gün kapalı kaldıysa iş açılışta bir kez çalışır ve yalnızca güncel durumu bildirir; kaçırılan günler için geriye dönük bildirim gönderilmez
- Bildirime dokunma ilgili ürünün detay ekranını açar (bildirim id'si = ürün id'si, böylece aynı ürünün eski bildirimi güncellenir)
- Android 13+ için `POST_NOTIFICATIONS` izni ilk ürün kaydedildiğinde istenir; reddedilirse uygulama çalışmaya devam eder, Ajanda'da uyarı kartı gösterilir

## 8. Türkiye tüketici hukuku bağlamı

- Mesafeli satışta (online alışveriş) yasal cayma hakkı: malın **teslim tarihinden** itibaren **14 gün**
- Yasal garanti süresi: malın **teslim tarihinden** itibaren en az **2 yıl**
- Mağazadan (yüz yüze) alışverişte yasal cayma hakkı yoktur; iade mağazanın kendi politikasına bağlıdır. Bu durumda kullanıcı mağazanın verdiği iade süresini opsiyonel bitiş tarihi alanına girer
- Bu süreler varsayılandır; ek garanti veya farklı iade süresi için kullanıcı madde 4.3'teki opsiyonel bitiş tarihi alanını kullanır

## 9. Performans ve kararlılık

Çökmeleri önlemek için:
- Tüm dosya ve tarih işlemleri (fiş kopyalama, thumbnail üretme, tarih ayrıştırma, indirme) try-catch ile korunmalı; hatalı veri uygulamayı kilitlememeli, kullanıcıya anlaşılır bir mesaj gösterilmeli
- Room sorguları ve dosya işlemleri ana iş parçacığında değil, coroutine + `Dispatchers.IO` üzerinde çalışmalı
- Bildirim izni (Android 13+) ve eski sürümlerdeki depolama izni reddedilirse uygulama çökmeden nazikçe uyarı göstermeli. Galeri/dosya seçimi (`OpenDocument`) ve kamera (`TakePicture`, manifest'te `CAMERA` izni bildirilmeden) ek izin gerektirmez
- Boş liste, eksik fiş, silinmiş dosya, geçersiz tarih gibi durumlar için her ekranda boş/hata durumu tasarlanmalı

Scroll akıcılığı için:
- Listeler LazyColumn / LazyVerticalGrid ile, her öğeye sabit bir key (`product.id`) verilmeli
- Liste ve kartlarda her zaman thumbnail dosyası Coil ile yüklenmeli; orijinal dosya değiştirilmeden tam çözünürlükte saklanır ve indirme her zaman orijinalden yapılır
- Room verileri Flow ile gözlemlenmeli, UI otomatik güncellensin
- Kalan gün ve durum hesapları ViewModel'de yapılıp UI'a hazır, immutable (`@Immutable`) UI modelleriyle verilmeli ki Compose gereksiz yeniden oluşturma yapmasın
- Gün değiştiğinde (gece yarısı veya uygulama ön plana geldiğinde) kalan gün değerleri yeniden hesaplanmalı

## 10. Fiyatlandırma (freemium + deneme)

> Bu bölüm son adımda yeniden tasarlanacak. Aşağıdakiler şimdilik taslaktır.

**Kalıcı ücretsiz:**
- 3 ürüne kadar takip
- Sadece bu 3 ürün için bildirim (madde 7'deki hatırlatma + süre doldu bildirimi)
- Manuel fiş/fatura ekleme

**15 günlük ücretsiz deneme (yeni kullanıcıda otomatik başlar):**
- Deneme boyunca tüm premium özellikler açık: sınırsız ürün, yedekleme, PDF dışa aktarma
- Deneme bitince abone olunmazsa kalıcı ücretsiz katmana döner
- 3'ten fazla eklenen ürünler **silinmez, kilitlenir** (salt-okunur listelenir); premium'a geçilince tekrar açılır — veri kaybı kullanıcıda kötü izlenim bırakır. Kilit durumu veritabanında saklanmaz, abonelik durumuna ve ürünlerin eklenme sırasına göre çalışma anında hesaplanır

**Premium (abonelik veya tek seferlik):**
- Sınırsız ürün takibi ve bildirim
- Yedekleme: kullanıcının kendi Google Drive'ındaki uygulamaya özel klasöre (appDataFolder) şifreli yedek; uygulamaya ait sunucu kullanılmaz (madde 1 ile uyumlu)
- Özelleştirilebilir hatırlatma başlangıcı (ücretsiz katmanda 3 gün sabit)
- PDF rapor / dışa aktarma

**Fiyatlar (başlangıç noktası, Play Console bölgesel fiyatlandırmasıyla teyit edilmeli):**
- Aylık: ₺39,99
- Yıllık: ₺299,99 (aylık ödemeye göre ~%37 indirim)
- Tek seferlik "ömür boyu": ₺599,99

Reklamsız, sade abonelik modeli önerilir — güven odaklı bu tür bir uygulamada reklam marka algısını zedeler. TL'deki enflasyon nedeniyle fiyatların periyodik gözden geçirilmesi gerekir.

**Play Store uyumluluğu:** Google Play, deneme süreli aboneliklerde kullanıcı onay vermeden önce süreyi, ücreti, neyin dahil olduğunu ve otomatik ücretli aboneliğe ne zaman geçeceğini açıkça göstermeyi şart koşuyor; aksi halde uygulama reddedilebilir. Yükseltme/deneme başlatma ekranında bu bilgiler net görünmeli.

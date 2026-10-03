# GARANTİLEDİM — Android Uygulama Spesifikasyonu

Bu doküman Gemini AI'a (veya Android Studio içindeki Gemini'ye, ya da başka bir kod üreten yapay zekaya) verilip "GARANTİLEDİM" uygulamasının üretilmesi için hazırlanmıştır. Aşağıdaki her bölüm doğrudan bir prompt olarak kullanılabilir.

## 1. Uygulama hakkında

- **Ad:** GARANTİLEDİM
- **Platform:** Android (Kotlin), Play Store'a yayınlanacak
- **Amaç:** Kullanıcının satın aldığı ürünlerin cayma/iade hakkı süresini ve garanti bitiş tarihini tek yerde takip etmesini, süre dolmadan bildirim almasını sağlamak
- **Mimari:** Hesap/sunucu gerektirmez, tamamen cihaz üzerinde çalışır (local-first)
- **Önerilen teknoloji:** Kotlin + Jetpack Compose, Room (yerel veritabanı), WorkManager (zamanlanmış bildirimler), Material Design 3

## 2. Renk paleti

| Rol | Açık mod | Koyu mod |
|---|---|---|
| Ana renk (primary) | #3B5BDB | #6B84E8 |
| Arka plan | #F6F7FB | #17181D |
| Kart yüzeyi | #FFFFFF | #1F2128 |
| Metin (birincil) | #16181D | #F2F3F7 |
| Metin (ikincil) | #6B7280 | #9CA3AF |
| Başarılı / bol süre | #2F9E64 | #4CC98A |
| Uyarı / yaklaşan süre | #E8A23B | #F2B75C |
| Tehlike / son günler | #E2483D | #F27168 |

Tasarım dili: düz (flat) yüzeyler, gradyan yok, ince kenarlıklar yerine tonlu kartlar (kenarlıksız, surface rengiyle ayrışan), yuvarlatılmış köşeler (kartlar 12dp, butonlar 8dp, uygulama ikonu 26dp).

## 3. İkon ve logo

Motif: kalkan (koruma/garanti) içinde onay işareti (takip edildi/güvence altında). Mavi (#3B5BDB) yuvarlak köşeli kare zemin üzerinde beyaz kalkan, kalkanın içinde mavi onay işareti. Play Store ikonu için bu motif adaptive icon foreground olarak kullanılabilir; arka plan katmanı düz #3B5BDB.

## 4. Ekranlar

### 4.1 Ana liste ekranı
- Üstte vurgu renkli özet kartı: "Garantilerim" + "X ürün takip ediliyor"
- Ürün kartları listesi (LazyColumn), **en yakın bitiş tarihine göre sıralı**
- Her kart: kategori ikonu (renkli daire rozet), ürün adı, "mağaza · kategori", süre türü etiketi (İade hakkı / Garanti), renkli "kalan gün" rozeti
  - Rozet rengi: 3 günden az kaldıysa kırmızı, 14 günden az kaldıysa sarı, aksi halde yeşil
- Sağ altta + FAB butonu → ürün ekleme ekranını açar
- Karta dokunma → ürün detay ekranını açar

### 4.2 Ürün ekleme ekranı
Alanlar:
- Ürün adı (zorunlu, boşsa hata gösterip kaydetmez)
- Mağaza
- Satın alma tarihi (tarih seçici)
- Kategori (Elektronik / Beyaz eşya / Giyim / Diğer)
- **Garanti bitiş tarihi (opsiyonel)** — kullanıcı elle girebilir. Boş bırakılırsa süre türüne göre otomatik hesaplanır (bkz. madde 6). Bazı firmalar yasal süreden daha uzun ek garanti verdiği için bu alan elle değiştirilebilir olmalı
- Süre türü (segmented control: İade hakkı / Garanti)
- Fiş/fatura ekleme: "Ekle" butonuna basınca **galeriden seç** veya **kamera ile çek** seçenekleri çıkar (ActivityResultContracts.GetContent ve TakePicture ile). Seçilen dosya **orijinal çözünürlüğüyle** uygulamanın kendi dosya dizininde saklanır; liste ve detay ekranında hızlı gösterim için ayrıca küçük bir önizleme (thumbnail) üretilip ayrı önbelleğe alınır
- Kaydet butonu

### 4.3 Ürün detay ekranı
Karta dokununca açılır, gösterilenler:
- Ürün adı, kategori ikonu, "mağaza · kategori"
- Satın alma tarihi
- Süre türü
- Bitiş tarihi (etiket süre türüne göre değişir: "İade son günü" / "Garanti bitişi")
- Kalan süre (renkli rozet)
- Eklenen fiş/fatura: dosya adı + küçük önizleme (thumbnail) + **"İndir" butonu** — önizleme sadece ekranda hızlı göstermek içindir; İndir butonu **orijinal, tam çözünürlüklü dosyayı** cihazın indirilenler klasörüne kopyalar (garanti belgesi kaybolunca okunaklı halde lazım olabilir)

## 5. Veri modeli (örnek)

```kotlin
data class Product(
    val id: Long = 0,
    val name: String,
    val store: String,
    val category: Category,
    val purchaseDate: LocalDate,
    val durationType: DurationType,
    val manualEndDate: LocalDate? = null,
    val receiptUri: String? = null
)

enum class Category { ELEKTRONIK, BEYAZ_ESYA, GIYIM, DIGER }
enum class DurationType { IADE, GARANTI }
```

## 6. Süre hesaplama mantığı

```kotlin
fun calculateEndDate(product: Product): LocalDate {
    product.manualEndDate?.let { return it }
    return when (product.durationType) {
        DurationType.IADE -> product.purchaseDate.plusDays(14)
        DurationType.GARANTI -> product.purchaseDate.plusYears(2)
    }
}

fun daysRemaining(endDate: LocalDate): Long =
    ChronoUnit.DAYS.between(LocalDate.now(), endDate)

fun urgencyLevel(days: Long): Urgency = when {
    days < 3 -> Urgency.DANGER
    days < 14 -> Urgency.WARNING
    else -> Urgency.SUCCESS
}
```

## 7. Bildirim sistemi

- WorkManager ile her ürün için günlük kontrol zamanlanır
- Bitişe **3 gün kala** hatırlatma bildirimleri başlar, **her gün** tekrarlanır
- Son gün: "{ürün adı} için {İade hakkı/Garanti} süresi bugün doluyor" bildirimi
- Süre dolduktan sonra: "{ürün adı} için {İade hakkı/Garanti} süresi doldu" bildirimi bir kez gönderilir, tekrarlanmaz

## 8. Türkiye tüketici hukuku bağlamı

- Online alışverişte yasal cayma hakkı: teslim tarihinden itibaren **14 gün**
- Çoğu üründe yasal garanti süresi: **2 yıl**
- Bu süreler varsayılan değerlerdir; kullanıcı madde 4.2'deki opsiyonel alanla ek garanti durumunda bunu değiştirebilir

## 10. Fiyatlandırma (freemium + deneme)

**Kalıcı ücretsiz:**
- 3 ürüne kadar takip
- Sadece bu 3 ürün için bildirim (bitişe 3 gün kala başlayıp günlük tekrarlayan + süre doldu bildirimi)
- Manuel fiş/fatura ekleme

**15 günlük ücretsiz deneme (yeni kullanıcıda otomatik başlar):**
- Deneme boyunca tüm premium özellikler açık: sınırsız ürün, bulut depolama, PDF dışa aktarma
- Deneme bitince abone olunmazsa kalıcı ücretsiz katmana döner
- 3'ten fazla eklenen ürünler **silinmez, kilitlenir** (salt-okunur listelenir); premium'a geçilince tekrar açılır — veri kaybı kullanıcıda kötü izlenim bırakır

**Premium (abonelik veya tek seferlik):**
- Sınırsız ürün takibi ve bildirim
- Bulut depolama / yedekleme
- Özelleştirilebilir hatırlatma süresi
- PDF rapor / dışa aktarma

**Fiyatlar (başlangıç noktası, Play Console bölgesel fiyatlandırmasıyla teyit edilmeli):**
- Aylık: ₺39,99
- Yıllık: ₺299,99 (aylık ödemeye göre ~%37 indirim)
- Tek seferlik "ömür boyu": ₺599,99

Reklamsız, sade abonelik modeli önerilir — güven odaklı bu tür bir uygulamada reklam marka algısını zedeler. TL'deki enflasyon nedeniyle fiyatların periyodik gözden geçirilmesi gerekir.

**Play Store uyumluluğu:** Google Play, deneme süreli aboneliklerde kullanıcı onay vermeden önce süreyi, ücreti, neyin dahil olduğunu ve otomatik ücretli aboneliğe ne zaman geçeceğini açıkça göstermeyi şart koşuyor; aksi halde uygulama reddedilebilir. Yükseltme/deneme başlatma ekranında bu bilgiler net görünmeli.

## 9. Performans ve kararlılık

Çökmeleri önlemek için:
- Tüm dosya ve tarih işlemleri (fiş okuma, tarih ayrıştırma) try-catch ile korunmalı, hatalı veri uygulamayı kilitlememeli
- Room sorguları ve dosya işlemleri ana iş parçacığında değil, coroutine + Dispatchers.IO üzerinde çalışmalı
- Bildirim (Android 13+) ve kamera/galeri izinleri reddedilirse uygulama çökmeden nazikçe uyarı göstermeli
- Boş liste, eksik fiş, geçersiz tarih gibi durumlar için her ekranda boş/hata durumu tasarlanmalı

Scroll akıcılığı için:
- Liste LazyColumn ile, her öğeye sabit bir key (örn. product.id) verilmeli — gereksiz yeniden çizimleri önler
- Fiş küçük resimleri Coil gibi bir kütüphaneyle önbelleğe alınmış küçük boyutta yüklenmeli — bu sadece liste/detay önizlemesi içindir, orijinal dosya değişmeden tam çözünürlükte saklanır ve indirme her zaman bu orijinalden yapılır
- Room verileri Flow ile gözlemlenmeli, UI otomatik güncellensin; ağır sorgular tek seferlik değil akış olarak gelsin
- Kart bileşenleri stabil (immutable) veri sınıflarıyla oluşturulmalı ki Compose gereksiz yeniden oluşturma yapmasın.

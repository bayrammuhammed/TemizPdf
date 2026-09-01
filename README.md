# Temiz PDF - Reklamsız Android Belge Stüdyosu 🚀

Sıfır reklam, %100 çevrimdışı ve gizlilik odaklı modern Android PDF uygulaması.

---

## 🌟 Temel Özellikler

1. **Gelişmiş PDF Okuyucu (Reader):**
   - Donanım hızlandırmalı ultra akıcı sayfa render'ı (`android.graphics.pdf.PdfRenderer`).
   - Gece / Karanlık Okuma Modu (Ters renk filtresiyle göz yormaz).
   - Çift parmakla yakınlaştırma & kaydırma (Pinch to Zoom & Pan).
   - Hızlı sayfa atlama çubuğu ve görsel ızgara (Grid thumbnail) görünümü.
   - WhatsApp, Telegram veya dosya yöneticisinden doğrudan varsayılan PDF açıcı olarak çalışma.

2. **Yapay Zeka Destekli Belge Tarayıcı (Scanner):**
   - Google ML Kit Document Scanner API entegrasyonu.
   - Otomatik köşe ve kenar algılama, perspektif düzeltme, gölge temizleme.
   - Siyah-Beyaz, Gri ve Sihirli Renk filtreleri.
   - Taranan sayfaları tek dokunuşla yüksek kaliteli PDF olarak kaydetme.

3. **PDF Araç Kutusu (Tools):**
   - **PDF Birleştir (Merge):** Birden fazla PDF belgesini sırasını ayarlayarak tek bir dosyada toplama.
   - **PDF Böl (Split):** Belirli sayfaları seçerek bağımsız yeni bir PDF oluşturma.
   - **Sayfa Düzenle & Döndür (Organize):** Sayfaları 90° döndürme, sırasını değiştirme veya istenmeyen sayfaları silme.
   - **Fotoğrafları PDF Yap (Images to PDF):** Galeriden seçilen fotoğrafları tek tıkla PDF yapma.

4. **Gizlilik & Güvenlik:**
   - %100 Yerel ve Çevrimdışı: Hiçbir sunucuya veri gitmez.
   - Sıfır reklam, sıfır izleyici, sıfır veri toplama.

---

## 🛠️ Mimari & Teknolojiler
- **Dil:** Kotlin 2.0.21
- **Arayüz:** Jetpack Compose + Material 3 (Material You)
- **PDF Motoru:** Android PdfRenderer + Apache PDFBox for Android
- **Tarayıcı:** Google Play Services ML Kit Document Scanner
- **Resim Yükleme:** Coil Compose
- **Navigasyon:** Jetpack Navigation Compose

---

## 📱 Android Studio'da Çalıştırma

Projeyi Android Studio'da açmak için:
```bash
open -a "Android Studio" /Users/muhammedbayram/AndroidStudioProjects/TemizPdf
```
Android Studio açıldığında üstteki **Run (▶)** düğmesine basarak fiziksel telefonunuzda veya emülatörde hemen kullanabilirsiniz!

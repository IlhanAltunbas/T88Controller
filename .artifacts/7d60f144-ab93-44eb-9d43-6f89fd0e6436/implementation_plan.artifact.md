# Sistem Sekmesi ve Gelişmiş Kontrol Protokolü Entegrasyonu

Bu plan, uygulamanın donanım dökümanlarındaki tüm kritik özelliklerini (Kamera, Master Mute, Kademeli Ses) kapsayacak şekilde genişletilmesini ve bir "Sistem" sekmesi eklenmesini içerir.

## Yapılacak Değişiklikler

### 1. Domain Katmanı (Yeni UseCase'ler)
- **[NEW] [SetMasterOutputMuteUseCase.kt](file:///C:/Users/ilhan/AndroidStudioProjects/T88Controller/shared/src/commonMain/kotlin/com/ilhanaltunbas/t88controller/domain/usecase/SetMasterOutputMuteUseCase.kt)**: Tüm çıkışları susturma/açma (0x03).
- **[NEW] [SetCameraPositionUseCase.kt](file:///C:/Users/ilhan/AndroidStudioProjects/T88Controller/shared/src/commonMain/kotlin/com/ilhanaltunbas/t88controller/domain/usecase/SetCameraPositionUseCase.kt)**: Kamerayı belirli bir kanal konumuna döndürme (0x0A).
- **[NEW] [SetRelativeVolumeUseCase.kt](file:///C:/Users/ilhan/AndroidStudioProjects/T88Controller/shared/src/commonMain/kotlin/com/ilhanaltunbas/t88controller/domain/usecase/SetRelativeVolumeUseCase.kt)**: Sesi +/- 1dB değiştirme (0x05).

### 2. Data Katmanı
- **[MODIFY] [MatrixMessage.kt](file:///C:/Users/ilhan/AndroidStudioProjects/T88Controller/shared/src/commonMain/kotlin/com/ilhanaltunbas/t88controller/data/remote/ProtocolParser.kt)**: `MasterMuteUpdate` ve `CameraUpdate` mesajlarını ekle.
- **[MODIFY] [ProtocolParser.kt](file:///C:/Users/ilhan/AndroidStudioProjects/T88Controller/shared/src/commonMain/kotlin/com/ilhanaltunbas/t88controller/data/remote/ProtocolParser.kt)**: 0x0A (Kamera) ve 0x03 (Master Mute) yanıtlarını ayrıştır.
- **[MODIFY] [AudioMatrixRepository.kt](file:///C:/Users/ilhan/AndroidStudioProjects/T88Controller/shared/src/commonMain/kotlin/com/ilhanaltunbas/t88controller/domain/repository/AudioMatrixRepository.kt)**: Yeni metodları ekle ve `isMasterMuted` StateFlow'u ekle.
- **[MODIFY] [AudioMatrixRepositoryImpl.kt](file:///C:/Users/ilhan/AndroidStudioProjects/T88Controller/shared/src/commonMain/kotlin/com/ilhanaltunbas/t88controller/data/repository/AudioMatrixRepositoryImpl.kt)**: Yeni protokol paketlerini implemente et.

### 3. Presentation Katmanı
- **[MODIFY] [MainScreen.kt](file:///C:/Users/ilhan/AndroidStudioProjects/T88Controller/shared/src/commonMain/kotlin/com/ilhanaltunbas/t88controller/presentation/ui/MainScreen.kt)**: `BottomNavItem` listesine **SYSTEM** sekmesini ekle.
- **[NEW] [SystemScreen.kt](file:///C:/Users/ilhan/AndroidStudioProjects/T88Controller/shared/src/commonMain/kotlin/com/ilhanaltunbas/t88controller/presentation/ui/SystemScreen.kt)**:
    - Kamera 1-8 seçim butonları.
    - Master Mute büyük anahtarı.
    - IP Adresi ve Port değiştirme (Giriş ekranına dönmeden cihaz değiştirme imkanı).
- **[NEW] [SystemViewModel.kt](file:///C:/Users/ilhan/AndroidStudioProjects/T88Controller/shared/src/commonMain/kotlin/com/ilhanaltunbas/t88controller/presentation/viewmodel/SystemViewModel.kt)**: Sistem ayarlarını ve kamera komutlarını yönet.
- **[MODIFY] [MixerScreen.kt](file:///C:/Users/ilhan/AndroidStudioProjects/T88Controller/shared/src/commonMain/kotlin/com/ilhanaltunbas/t88controller/presentation/ui/MixerScreen.kt)**: Fader yanlarına +/- 1dB hassas ayar butonları ekle.

### 4. Bağımlılık Yönetimi
- **[MODIFY] [AppModule.kt](file:///C:/Users/ilhan/AndroidStudioProjects/T88Controller/shared/src/commonMain/kotlin/com/ilhanaltunbas/t88controller/di/AppModule.kt)**: Yeni UseCase ve ViewModelleri kaydet.

---

## Kritik Uyarılar
> [!IMPORTANT]
> **Bağlantı Değişimi:** Sistem sekmesinden IP/Port değiştiğinde mevcut soketin kapatılıp yenisinin açılması (Re-connect) süreci kullanıcıya kesinti olarak yansıtılmalı (Loading simgesi).

## Doğrulama Planı
- **Kamera Testi:** 0x0A paketinin loglarda doğru oluştuğu (Header + 0x36 + 0x0A + 0x01 + Channel + EE) doğrulanacak.
- **Master Mute:** Tüm kanalların aynı anda sustuğu simüle edilecek.
- **IP Değişimi:** Yeni girilen IP ile `ConnectToDeviceUseCase`'in tetiklendiği kontrol edilecek.

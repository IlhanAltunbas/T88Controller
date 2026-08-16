# Geliştirme Görev Listesi - Sistem Sekmesi ve Gelişmiş Protokol

- [x] Domain Katmanı: Yeni UseCase'lerin oluşturulması
    - [x] `SetMasterOutputMuteUseCase`
    - [x] `SetCameraPositionUseCase`
    - [x] `SetRelativeVolumeUseCase`
- [x] Data Katmanı: Protokol ve Repository güncellemeleri
    - [x] `MatrixMessage` ve `ProtocolParser` (0x0A, 0x03 Master, 0x05)
    - [x] `AudioMatrixRepository` ve `Impl` (Yeni State ve Metodlar)
- [x] Presentation Katmanı: ViewModel ve DI
    - [x] `SystemViewModel` oluşturulması
    - [x] `AppModule` güncellenmesi
- [x] Kullanıcı Arayüzü (UI) Geliştirmeleri
    - [x] `MainScreen`: BottomNavItem listesine SYSTEM eklenmesi
    - [x] `SystemScreen`: Kamera, Master Mute, IP/Port ayarları
    - [x] `MixerScreen`: +/- 1dB hassas ayar butonları
- [x] Doğrulama ve Test

package com.ilhanaltunbas.t88controller.di

import com.ilhanaltunbas.t88controller.data.remote.TcpSocketClient
import com.ilhanaltunbas.t88controller.data.repository.AudioMatrixRepositoryImpl
import com.ilhanaltunbas.t88controller.domain.repository.AudioMatrixRepository
import com.ilhanaltunbas.t88controller.domain.usecase.*
import com.ilhanaltunbas.t88controller.presentation.viewmodel.*
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule = module {
    singleOf(::TcpSocketClient)
    singleOf(::AudioMatrixRepositoryImpl) { bind<AudioMatrixRepository>() }

    factoryOf(::SetAbsoluteVolumeUseCase)
    factoryOf(::SetRelativeVolumeUseCase)
    factoryOf(::SetMuteUseCase)
    factoryOf(::SetMasterOutputMuteUseCase)
    factoryOf(::SetPhantomPowerUseCase)
    factoryOf(::SetLineMicModeUseCase)
    factoryOf(::SetFeedbackSuppressionUseCase)
    factoryOf(::SetCameraPositionUseCase)
    factoryOf(::SetRoutingUseCase)
    factoryOf(::RecallSceneUseCase)
    factoryOf(::ConnectToDeviceUseCase)
    factoryOf(::GetVolumeUseCase)
    factoryOf(::GetMuteStateUseCase)
    factoryOf(::GetLineMicModeUseCase)
    factoryOf(::GetPhantomPowerStateUseCase)
    factoryOf(::GetFeedbackSuppressionUseCase)
    factoryOf(::GetRoutingUseCase)
    factoryOf(::GetCurrentPresetUseCase)
    factoryOf(::ObserveConnectionStatusUseCase)
    factoryOf(::ObserveInputChannelsUseCase)
    factoryOf(::ObserveOutputChannelsUseCase)
    factoryOf(::ObserveActiveRoutesUseCase)
    factoryOf(::ObserveCurrentPresetUseCase)
    factoryOf(::ObserveMasterMuteUseCase)
    factoryOf(::DisconnectDeviceUseCase)
    
    factory { 
        MixerUseCases(
            get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get()
        )
    }

    viewModelOf(::ConnectionViewModel)
    factory { MixerViewModel(get()) }
    viewModelOf(::MatrixViewModel)
    viewModelOf(::ScenesViewModel)
    viewModelOf(::SystemViewModel)
}
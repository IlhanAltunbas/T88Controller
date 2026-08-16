package com.ilhanaltunbas.t88controller.domain.usecase

data class MixerUseCases(
    val setAbsoluteVolume: SetAbsoluteVolumeUseCase,
    val setMute: SetMuteUseCase,
    val setPhantomPower: SetPhantomPowerUseCase,
    val setLineMicMode: SetLineMicModeUseCase,
    val setRelativeVolume: SetRelativeVolumeUseCase,
    val setFeedbackSuppression: SetFeedbackSuppressionUseCase,
    val observeInputChannels: ObserveInputChannelsUseCase,
    val observeOutputChannels: ObserveOutputChannelsUseCase,
    val getVolume: GetVolumeUseCase,
    val getMuteState: GetMuteStateUseCase,
    val getLineMicMode: GetLineMicModeUseCase,
    val getPhantomPowerState: GetPhantomPowerStateUseCase,
    val getFeedbackSuppression: GetFeedbackSuppressionUseCase,
    val observeConnectionStatus: ObserveConnectionStatusUseCase
)
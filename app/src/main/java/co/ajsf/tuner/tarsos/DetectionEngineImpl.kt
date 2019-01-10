package co.ajsf.tuner.tarsos

import be.tarsos.dsp.AudioDispatcher
import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.io.android.AudioDispatcherFactory
import be.tarsos.dsp.pitch.PitchDetectionHandler
import be.tarsos.dsp.pitch.PitchDetectionResult
import be.tarsos.dsp.pitch.PitchProcessor
import co.ajsf.tuner.frequencyDetection.DetectionEngine
import co.ajsf.tuner.frequencyDetection.DetectionHandler
import co.ajsf.tuner.mapper.TarsosResponseToModelMapper
import co.ajsf.tuner.mapper.mapTarsosResponseToDetectionResult

typealias TarsosResponse = Pair<PitchDetectionResult, AudioEvent>

class DetectionEngineImpl(
    private val dispatcher: AudioDispatcher,
    private val mapper: TarsosResponseToModelMapper
) : DetectionEngine {

    private var audioThread: Thread? = null
    private var detectionHandler: DetectionHandler? = null

    private val pdh = PitchDetectionHandler { res, event ->
        handleDetection(res, event)
    }

    private val pitchProcessor = PitchProcessor(
        PitchProcessor.PitchEstimationAlgorithm.MPM, 22050f, 1024, pdh
    )

    override fun listen(detectionHandler: DetectionHandler) {
        this.detectionHandler = detectionHandler
        dispatcher.addAudioProcessor(pitchProcessor)
        audioThread = Thread(dispatcher, "Audio Thread")
        audioThread?.start()
    }

    override fun stopListening() {
        dispatcher.removeAudioProcessor(pitchProcessor)
        dispatcher.stop()
        audioThread?.interrupt()
        audioThread = null
        detectionHandler = null
    }

    fun handleDetection(pitchDetectionResult: PitchDetectionResult, audioEvent: AudioEvent) {
        val detectionResult = mapper.invoke(pitchDetectionResult to audioEvent)
        detectionHandler?.invoke(detectionResult)
    }

    companion object {
        fun builder(): DetectionEngineImpl {
            return DetectionEngineImpl(
                AudioDispatcherFactory
                    .fromDefaultMicrophone(22050, 1024, 0), ::mapTarsosResponseToDetectionResult
            )
        }
    }
}
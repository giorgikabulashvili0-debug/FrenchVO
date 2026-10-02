from pathlib import Path
import sherpa_onnx
import wave
root=Path('app/src/main/assets/tom')
tts=sherpa_onnx.OfflineTts(sherpa_onnx.OfflineTtsConfig(model=sherpa_onnx.OfflineTtsModelConfig(vits=sherpa_onnx.OfflineTtsVitsModelConfig(model=str(root/'fr_FR-tom-medium.onnx'),tokens=str(root/'tokens.txt'),data_dir=str(root/'espeak-ng-data')),num_threads=2)))
audio=tts.generate('Bonjour, je suis Tom. Pourquoi les humains ont-ils peur du noir ?',sid=0,speed=1.0)
assert len(audio.samples)>1000 and audio.sample_rate==44100
assert max(abs(float(x)) for x in audio.samples)>0.01
sherpa_onnx.write_wave('tom_test.wav',audio.samples,audio.sample_rate)
with wave.open('tom_test.wav','rb') as f:
    assert f.getnchannels()==1 and f.getnframes()>1000
print('French Tom voice generated a nonempty WAV at 44100 Hz')

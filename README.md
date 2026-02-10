# nb_mxsynths
mx synths as an nb voice

this is more or less port of infinitedigits' mx.synths (thanks zack!) with the following changes:

added:
- save and load presets
- dynamic parameter menu (synth specific parameters are displayed)
- modulation via `nb:modulate(val)` -> pairs well with sidvagn. I surfaced the `mod amt` parameter, so it's midi mappable, which means modulation can be used with scripts that don't use `nb:modulate`.
- fx via fx mod
- 6 voice limit

removed:
- portamento
- polyperc (already exists as separate nb voice)
- internal fx bus

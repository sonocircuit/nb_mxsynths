# nb_mxsynths
mx synths as an nb voice

this is more or less port of infinitedigits' mx.synths with the following changes:

added:
- save and load presets
- dynamic parameter menu (synth specific parameters are displayed)
- modulation via `nb:modulate(val)` -> pairs well with sidvagn
- fx via fx mod
- 6 voice limit

removed:
- portamento
- polyperc (already exists as separate nb voice)
- internal fx bus
  
_NOTE: this is still under development. synthdef tweaks will be made._

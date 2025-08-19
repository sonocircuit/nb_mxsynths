// mx synths - nb editon v.0.1 @sonoCircuit
// synthdefs taken from mx_synths @inifinitedigits

NB_mxSynths {

	*initClass {

		var synthParams, synthGroup, synthVoices;
		var numVoices = 6;

		synthParams = Dictionary.newFrom([
			\amp, 0.8,
			\pan, 0,
			\sub, 0,
			\mod1, 0,
			\mod2, 0,
			\mod3, 0,
			\mod4, 0,
			\attack, 0.01,
			\decay, 0.6,
			\sustain, 0.4,
			\release, 2.2,
			\sendA, 0,
			\sendB, 0
		]);

		StartUp.add {

			var s = Server.default;

			s.waitForBoot {

				// make groups
				synthGroup = Group.new(s);
				synthVoices = Array.fill(numVoices, { Group.new(synthGroup) });


				// add synthdefs
				SynthDef(\mx_synthy, {
					arg out = 0, sendABus = 0, sendBBus = 0, sendA = 0, sendB = 0,
					hz = 220, amp = 1.0, vel = 1.0, pan = 0, sub = 0,
					gate = 1, attack = 0.01, decay = 0.2, sustain = 0.9, release = 5,
					mod1 = 0, mod2 = 0, mod3 = 0, mod4 = 0;

					var env = EnvGen.ar(Env.adsr(attack, decay, sustain, release), gate, doneAction: 2);

					var stereo = LinLin.kr(Lag.kr(mod1), -1, 1, 0, 1);
					var lowcut = LinExp.kr(Lag.kr(mod2), -1, 1, 40, 11000);
					var res = LinExp.kr(Lag.kr(mod3), -1, 1, 1.8, 0.2);
					var detune = LinExp.kr(Lag.kr(mod4), -1, 1, 0.0001, 0.3);

					var snd = Pan2.ar(Pulse.ar(hz/2, LinLin.kr(LFTri.kr(0.5), -1, 1, 0.2, 0.8)) * Lag.kr(sub, 1));
					snd = snd + Mix.ar({
						arg i;
						var snd2;
						snd2 = SawDPW.ar((hz.cpsmidi + (detune * (i * 2 - 1))).midicps);
						snd2 = RLPF.ar(snd2, LinExp.kr(
							SinOsc.kr(rrand(1/30, 1/10), rrand(0, 2 * pi)), -1, 1, lowcut, 12000), res);
						snd2 = DelayC.ar(snd2, rrand(0.01, 0.03), LFNoise1.kr(Rand(5, 10), 0.01, 0.02) / 15);
						Pan2.ar(snd2, VarLag.kr(LFNoise0.kr(1/3), 3, warp:\sine) * stereo)
					}!2);
					snd = Balance2.ar(snd[0], snd[1], Lag.kr(pan, 0.1));
					snd = snd * env * amp * vel * -12.dbamp;

					Out.ar(out, snd);
					Out.ar(sendABus, sendA * snd);
					Out.ar(sendBBus, sendB * snd);
				}).add;


				SynthDef(\mx_icarus,{
					arg out = 0, sendABus = 0, sendBBus = 0, sendA = 0, sendB = 0,
					hz = 220, amp = 1.0, vel = 1.0, pan = 0, sub = 0,
					gate = 1, attack = 0.01, decay = 0.2, sustain = 0.9, release = 5,
					mod1 = 0, mod2 = 0, mod3 = 0, mod4 = 0;

					var bass, basshz, feedback = 0.5, delaytime = 0.25;
					var ender, snd, local, in, ampcheck, env, detuning = 0.1, pwmcenter = 0.5, pwmwidth = 0.4, pwmfreq = 1.5;

					env = EnvGen.ar(Env.adsr(attack, decay, sustain, release), gate, doneAction: 2);

					feedback = LinLin.kr(Lag.kr(mod1), -1, 1, 0.1, 2);
					delaytime = LinLin.kr(Lag.kr(mod2), -1, 1, 0.05, 0.6);
					pwmwidth = LinLin.kr(Lag.kr(mod3), -1, 1, 0.1, 0.9);
					detuning = LinExp.kr(Lag.kr(mod4), -1, 1, 0.01, 1);

					snd = Mix.new({VarSaw.ar(
						hz + (SinOsc.kr(LFNoise0.kr(1), Rand(0,3))*
							(((hz).cpsmidi + 1).midicps - (hz)) * detuning),
						width:LFTri.kr(pwmfreq + rrand(0.1, 0.3), mul:pwmwidth/2, add:pwmcenter),
						mul:0.25
					)}!2);

					snd = snd + Mix.new({VarSaw.ar(
						hz/2 + (SinOsc.kr(LFNoise0.kr(1), Rand(0, 3)) *
							(((hz/2).cpsmidi + 1).midicps - (hz/2)) * detuning),
						width:LFTri.kr(pwmfreq + rrand(0.1, 0.3), mul:pwmwidth/2, add:pwmcenter),
						mul:0.15
					)}!2);

					basshz = hz;
					basshz = Select.kr(basshz > 90, [basshz, basshz/2]);

					bass = PulseDPW.ar(basshz, width:SinOsc.kr(1/3).range(0.2, 0.4));
					bass = bass + LPF.ar(WhiteNoise.ar(SinOsc.kr(1/rrand(3, 4)).range(1, rrand(3, 4))), 2 * basshz);
					bass = Pan2.ar(bass, LFTri.kr(1/6.12).range(-0.2, 0.2));
					bass = HPF.ar(bass, 20);
					bass = LPF.ar(bass, SinOsc.kr(0.1).range(2, 5) * basshz);

					ampcheck = Amplitude.kr(Mix.ar(snd));
					snd = snd * (ampcheck > 0.02); // noise gate
					local = LocalIn.ar(2);
					local = OnePole.ar(local, 0.4);
					local = OnePole.ar(local, -0.08);
					local = Rotate2.ar(local[0], local[1], 0.2);
					local = DelayC.ar(local, 0.5, delaytime);
					local = LeakDC.ar(local);
					local = ((local + snd) * 1.25).softclip;

					LocalOut.ar(local * Lag.kr(feedback, 1));

					snd = Balance2.ar(local[0], local[1], pan);
					snd = snd + (SinOsc.kr(0.123, Rand(0, 3)).range(0.2, 1.0) * bass * sub);
					snd = snd * env * amp * vel * -12.dbamp;

					Out.ar(out, snd);
					Out.ar(sendABus, sendA * snd);
					Out.ar(sendBBus, sendB * snd);
				}).add;


				SynthDef(\mx_casio, {
					arg out = 0, sendABus = 0, sendBBus = 0, sendA = 0, sendB = 0,
					hz = 220, amp = 1.0, vel = 1.0, pan = 0, sub = 0,
					gate = 1, attack = 0.01, decay = 0.2, sustain = 0.9, release = 5,
					mod1 = 0, mod2 = 0, mod3 = 0, mod4 = 0;

					var env = EnvGen.ar(Env.adsr(attack, decay, sustain, release), gate, doneAction: 2);

					var artifacts = LinLin.kr(Lag.kr(mod1), -1, 1, 1, 10);
					var phasing = LinExp.kr(Lag.kr(mod2), -1, 1, 0.125, 8);
					var res = LinExp.kr(Lag.kr(mod3), -1, 1, 0.1, 10);
					var detuning = LinExp.kr(Lag.kr(mod4), -1, 1, 0.000001, 0.02);

					var freq = [hz * (1 - detuning), hz * (1 + detuning)];
					var freqBase = freq;
					var freqRes = SinOsc.kr(Rand(0.01, 0.2), 0).range(freqBase/2, freqBase*2) * res;
					var pdbase = Impulse.ar(freqBase);
					var pd = Phasor.ar(pdbase, 2 * pi * freqBase / 48000 * phasing, 0, 2pi);
					var pdres = Phasor.ar(pdbase, 2 * pi * freqRes / 48000 * phasing, 0, 2pi);
					var pdi = LinLin.ar((2pi - pd).max(0), 0, 2pi, 0, 1);

					var snd = Lag.ar(SinOsc.ar(0, pdres) * pdi, 1 / freqBase);
					snd = LPF.ar(snd, Clip.kr(hz * artifacts, 20, 18000));
					snd = Pan2.ar(snd, Lag.kr(pan, 0.1));
					snd = snd * env * amp * vel * -9.dbamp;

					Out.ar(out, snd);
					Out.ar(sendABus, sendA * snd);
					Out.ar(sendBBus, sendB * snd);
				}).add;

				SynthDef(\mx_malone,{
					arg out = 0, sendABus = 0, sendBBus = 0, sendA = 0, sendB = 0,
					hz = 220, amp = 1.0, vel = 1.0, pan = 0, sub = 0,
					gate = 1, attack = 0.01, decay = 0.2, sustain = 0.9, release = 5,
					mod1 = 0, mod2 = 0, mod3 = 0, mod4 = 0;

					var snd, env, basshz, bass, detuning, pw, res, filt, detuningSpeed;

					env = EnvGen.ar(Env.adsr(attack, decay, sustain, release), gate, doneAction: 2);

					detuningSpeed = LinExp.kr(Lag.kr(mod1), -1, 1, 0.1, 10);
					detuning = LinExp.kr(Lag.kr(mod2),-1, 1, 0.002, 0.8);
					filt = LinLin.kr(Lag.kr(mod3), -1, 1, 2, 10);
					res = LinExp.kr(Lag.kr(mod4), -1, 1, 4, 4, 0.2);

					basshz = Select.kr(hz > 90, [hz, hz/2]);

					bass = PulseDPW.ar(basshz, width:SinOsc.kr(1/3).range(0.2, 0.4));
					bass = bass + LPF.ar(WhiteNoise.ar(SinOsc.kr(1 / rrand(3, 4)).range(1, rrand(3, 4))), 2 * basshz);
					bass = Pan2.ar(bass, LFTri.kr(1 / 6.12).range(-0.2, 0.2));
					bass = HPF.ar(bass, 20);
					bass = LPF.ar(bass, SinOsc.kr(0.1).range(2, 5) * basshz);

					snd = Mix.ar(Array.fill(2, {
						arg i;
						var hz_, snd_;
						hz_ = ((2 * hz).cpsmidi + SinOsc.kr(detuningSpeed *
							Rand(0.1, 0.5), Rand(0, pi)).range(detuning.neg, detuning)).midicps;
						snd_ = PulseDPW.ar(hz_, 0.17);
						snd_ = snd_ + PulseDPW.ar(hz_/2, 0.17);
						snd_ = snd_ + PulseDPW.ar(hz_*2, 0.17);
						snd_ = snd_ + LFTri.ar(hz_/4);
						snd_ = RLPF.ar(snd_, Clip.kr(hz_ * filt, hz_ * 1.5, 16000),
							Clip.kr(LFTri.kr([0.5,0.45]).range(0.3,1) * res, 0.2, 2));
						Pan2.ar(snd_ ,VarLag.kr(LFNoise0.kr(1/3), 3, warp:\sine)) / 10
					}));

					snd = snd + (SinOsc.kr(0.123).range(0.2, 1.0) * bass * sub);

					snd = Balance2.ar(snd[0], snd[1],Lag.kr(pan, 0.1));
					snd = snd * env * amp * vel * -12.dbamp;

					Out.ar(out, snd);
					Out.ar(sendABus, sendA * snd);
					Out.ar(sendBBus, sendB * snd);
				}).add;


				SynthDef(\mx_toshiya,{
					arg out = 0, sendABus = 0, sendBBus = 0, sendA = 0, sendB = 0,
					hz = 220, amp = 1.0, vel = 1.0, pan = 0, sub = 0,
					gate = 1, attack = 0.01, decay = 0.2, sustain = 0.9, release = 5,
					mod1 = 0, mod2 = 0, mod3 = 0, mod4 = 0;

					var env = EnvGen.ar(Env.adsr(attack, decay, sustain, release), gate, doneAction: 2);

					var detune = LinExp.kr(Lag.kr(mod1), -1, 1, 0.001, 0.1);
					var klankyvol = LinLin.kr(Lag.kr(mod2), -1, 1, 0, 2);
					var lowcut = LinExp.kr(Lag.kr(mod3), -1, 1, 25, 11000);
					var chorus = LinExp.kr(Lag.kr(mod4), -1, 1, 0.2, 5);

					var snd = Pan2.ar(SinOsc.ar(hz/2,
						LinLin.kr(LFTri.kr(0.5), -1, 1, 0.2, 0.8)) / 12 * amp, SinOsc.kr(0.1, mul:0.2)) * Lag.kr(sub, 1);

					snd = snd + Mix.ar({
						arg i;
						var snd2;
						snd2 = SinOsc.ar((hz.cpsmidi + (detune * (i * 2 - 1))).midicps);
						snd2 = LPF.ar(snd2, LinExp.kr(SinOsc.kr(rrand(1/30,1/10), rrand(0, 2 * pi)), -1, 1, lowcut, 12000), 2);
						snd2 = DelayC.ar(snd2, rrand(0.01, 0.03),
							LFNoise1.kr(Rand(5, 10), 0.01, 0.02) / NRand(10, 20, 3) * chorus);
						Pan2.ar(snd2, VarLag.kr(LFNoise0.kr(1/3), 3, warp:\sine))
					}!2);

					snd = snd + (Amplitude.kr(snd) * VarLag.kr(LFNoise0.kr(1), 1, warp:\sine).range(0.1, 1.0) *
						klankyvol * Klank.ar(`[[hz, hz * 2 + 2, hz*4 + 5, hz*8 + 2], nil, [1, 1, 1, 1]], PinkNoise.ar([0.007, 0.007])));
					snd = Balance2.ar(snd[0], snd[1], Lag.kr(pan, 0.1));
					snd = snd * env * amp * vel * -18.dbamp;

					Out.ar(out, snd);
					Out.ar(sendABus, sendA * snd);
					Out.ar(sendBBus, sendB * snd);
				}).add;

				// https://github.com/catfact/zebra/blob/master/lib/Engine_DreadMoon.sc#L20-L41
				SynthDef(\mx_piano,{
					arg out = 0, sendABus = 0, sendBBus = 0, sendA = 0, sendB = 0,
					hz = 220, amp = 1.0, vel = 1.0, pan = 0, sub = 0,
					gate = 1, attack = 0.01, decay = 0.2, sustain = 0.9, release = 5,
					mod1 = 0, mod2 = 0, mod3 = 0, mod4 = 0;

					var snd, note, env, damp = 0;
					var noise, string, delaytime, lpf, noise_env, damp_mul;
					var noise_hz, tune_up, tune_down, string_decay, lpf_rq,
					lpf_ratio = 2.0, hpf_hz = 40, damp_time = 0.1;

					env = EnvGen.ar(Env.adsr(0.01, 0, 1, release), gate, doneAction: 2);

					// mods
					string_decay = LinLin.kr(Lag.kr(mod1), -1, 1, 0.2, 8);
					noise_hz = LinExp.kr(Lag.kr(mod2), -1, 1, 400, 16000);
					lpf_rq = LinLin.kr(Lag.kr(mod3), -1, 1, 4, 0.4);
					tune_up = 1 + LinLin.kr(Lag.kr(mod4), -1, 1, 0.0001, 0.0005 * 4);
					tune_down = 1 - LinLin.kr(Lag.kr(mod4), -1, 1, 0.00005, 0.0004 * 4);

					damp_mul = LagUD.ar(K2A.ar(1.0 - damp), 0, damp_time);

					noise_env = Decay2.ar(Impulse.ar(0));
					noise = LFNoise2.ar(noise_hz) * noise_env;

					delaytime = 1.0 / (hz * [tune_up, tune_down]);
					string = Mix.new(CombL.ar(noise, delaytime, delaytime, string_decay * damp_mul));

					snd = RLPF.ar(string, lpf_ratio * hz, lpf_rq);
					snd = HPF.ar(snd, hpf_hz);
					snd = Pan2.ar(snd,Lag.kr(pan, 0.1));

					snd = snd * env * amp * vel * -12.dbamp;

					Out.ar(out, snd);
					Out.ar(sendABus, sendA * snd);
					Out.ar(sendBBus, sendB * snd);
				}).add;


				// port of STK's Rhodey (yamaha DX7-style Fender Rhodes) https://sccode.org/1-522
				SynthDef(\mx_epiano, {
					arg out = 0, sendABus = 0, sendBBus = 0, sendA = 0, sendB = 0,
					hz = 220, amp = 1.0, vel = 1.0, pan = 0, sub = 0,
					gate = 1, attack = 0.01, decay = 0.2, sustain = 0.9, release = 5,
					mod1 = 0, mod2 = 0, mod3 = 0, mod4 = 0;

					var env = EnvGen.ar(Env.adsr(attack, decay, sustain, release), gate, doneAction: 2);

					var mix = LinLin.kr(Lag.kr(mod1), -1, 1, 0.0, 0.4);
					var modIndex = LinExp.kr(Lag.kr(mod2), -1, 1, 0.01, 4);
					var lfoSpeed = LinLin.kr(Lag.kr(mod3), -1, 1, 0, 6);
					var lfoDepth = LinExp.kr(Lag.kr(mod4), -1, 1, 0.01, 1);

					var env1 = EnvGen.ar(Env.adsr(0.001, 1.25, 0.5, release, curve: \lin),gate);
					var env2 = EnvGen.ar(Env.adsr(0.001, 1.00, 0.5, release, curve: \lin),gate);
					var env3 = EnvGen.ar(Env.adsr(0.001, 1.50, 0.5, release, curve: \lin),gate);

					var osc4 = SinOsc.ar(hz) * 2pi * 2 * 0.535887 * modIndex * env3 * vel;
					var osc3 = SinOsc.ar(hz/2, osc4) * env3 * vel;
					var osc2 = SinOsc.ar(hz * 30) * 2pi * 0.05 * env2 * vel;
					var osc1 = SinOsc.ar(hz/2, osc2) * env1 * vel;

					var snd = Mix((osc3 * (1 - mix)) + (osc1 * mix));
					snd = snd * (SinOsc.ar(lfoSpeed) * lfoDepth + 1);
					snd = Pan2.ar(snd, Lag.kr(pan, 0.1));
					snd = snd * env * amp * -12.dbamp;

					Out.ar(out, snd);
					Out.ar(sendABus, sendA * snd);
					Out.ar(sendBBus, sendB * snd);
				}).add;


				SynthDef(\mx_mdapiano,{
					arg out = 0, sendABus = 0, sendBBus = 0, sendA = 0, sendB = 0,
					hz = 220, amp = 1.0, vel = 1.0, pan = 0, sub = 0,
					gate = 1, attack = 0.01, decay = 0.2, sustain = 0.9, release = 5,
					mod1 = 0, mod2 = 0, mod3 = 0, mod4 = 0;

					var env = EnvGen.ar(Env.adsr(0.01, 0, 1.0, release), gate, doneAction: 2);
					var tuning = LinLin.kr(Lag.kr(mod1), -1, 1, 0, 1);
					var snd = MdaPiano.ar(
						freq: hz,
						gate: gate,
						decay: decay,
						velmuff: vel,
						release: release,
						stereo: LinLin.kr(Lag.kr(mod2), -1, 1, 0.3, 1),
						vel: vel.linlin(0, 1, 0, 110) + Rand(0, 10),
						tune: Rand(0.5 * tuning.neg, 0.5 * tuning)
					);

					snd = Vibrato.ar(
						snd,
						rate:LinExp.kr(Lag.kr(mod3), -1, 1, 0.01, 16),
						depth:LinExp.kr(Lag.kr(mod4), -1, 1, 0.01, 1)
					);

					snd = Pan2.ar(snd, Lag.kr(pan, 0.1));
					snd = snd * env * amp * vel * -9.dbamp;

					Out.ar(out, snd);
					Out.ar(sendABus, sendA * snd);
					Out.ar(sendBBus, sendB * snd);
				}).add;


				// http://sccode.org/1-51n
				SynthDef(\mx_kalimba,{
					arg out = 0, sendABus = 0, sendBBus = 0, sendA = 0, sendB = 0,
					hz = 220, amp = 1.0, vel = 1.0, pan = 0, sub = 0,
					gate = 1, attack = 0.01, decay = 0.2, sustain = 0.9, release = 5,
					mod1 = 0, mod2 = 0, mod3 = 0, mod4 = 0;

					var env = EnvGen.ar(Env.perc(attack, decay), gate, doneAction: 2);

					var osc = HPF.ar(LPF.ar(DPW3Tri.ar(hz).tanh, 380), 60);
					var click = DynKlank.ar(`[
						[240 * ExpRand(0.97, 1.02), 2020 * ExpRand(0.97, 1.02), 3151 * ExpRand(0.97, 1.02)],
						[-9, 0, -5].dbamp,
						[0.8, 0.07, 0.08]
					], BPF.ar(PinkNoise.ar, Rand(5500, 8500), Rand(0.05, 0.2)) * EnvGen.ar(Env.perc(0.001, 0.01)));

					var mix = LinLin.kr(Lag.kr(mod1), -1, 1, 0.01, 0.4);
					var snd = (osc * mix) + (click * (1 - mix));

					snd = Splay.ar(snd, center:Rand(-1, 1) * LinLin.kr(Lag.kr(mod2), -1, 1, 0, 1));
					snd = Vibrato.ar(snd, LinExp.kr(Lag.kr(mod3), -1, 1, 0.01, 16), LinExp.kr(Lag.kr(mod4), -1, 1, 0.01, 1));
					snd = Balance2.ar(snd[0], snd[1], Lag.kr(pan, 0.1));
					snd = snd * env * amp * vel;

					Out.ar(out, snd);
					Out.ar(sendABus, sendA * snd);
					Out.ar(sendBBus, sendB * snd);
				}).add;



				SynthDef(\mx_aaaaaa,{
					arg out = 0, sendABus = 0, sendBBus = 0, sendA = 0, sendB = 0,
					hz = 220, amp = 1.0, vel = 1.0, pan = 0, sub = 0,
					gate = 1, attack = 0.01, decay = 0.2, sustain = 0.9, release = 5,
					mod1 = 0, mod2 = 0, mod3 = 0, mod4 = 0;

					var saw, wiggle, snd, env;
					// frequencies drawn from https://slideplayer.com/slide/15020921/
					var f1a = [290, 420, 580, 720, 690, 550, 400, 280];
					var f2a = [750, 1000, 790, 1100, 1600, 1750, 1900, 2200];
					var f3a = [2300, 2350, 2400, 2500, 2600, 2700, 2800, 3300];
					var f4a = [3500, 3500, 3500, 3500, 3500, 3500, 3500, 3500];
					var f1b = [390, 435, 590, 850, 860, 600, 420, 360];
					var f2b = [900, 1100, 850, 1200, 2200, 2350, 2500, 2750];
					var f3b = [2850, 2900, 3000, 3000, 3100, 3200, 3300, 3800];
					var f4b = [4000, 4000, 4000, 4000, 4000, 4000, 4000, 4000];
					var f1c = [420, 590, 640, 1100, 1000, 700, 575, 375];
					var f2c = [1200, 1300, 1100, 1300, 2500, 2700, 2800, 3200];
					var f3c = [3200, 3250, 3300, 3400, 3500, 3600, 3700, 4200];
					var f4c = [4500, 4500, 4500, 4500, 4500, 4500, 4500, 4500];
					var f1, f2, f3, f4;
					var a1, a2, a3, a4;
					var q1, q2, q3, q4;
					var voice, vowel, tilt, cons, detune, focus, div, reso;

					mod1 = Lag.kr(mod1); mod2 = Lag.kr(mod2); mod3 = Lag.kr(mod3); mod4 = Lag.kr(mod4);

					voice = Select.kr( (mod1 > -0.99), [hz.explin(100, 1000, 0, 2), LinLin.kr(mod1, -1, 1, 0, 2)]);
					vowel = LinLin.kr(mod2, -1, 1, 0, 7);
					tilt = LinLin.kr(mod2, -1, 1, 0.3, 0.6) * LinLin.kr(mod4, -1, 1, 0.6, 1.1);
					reso = LinLin.kr(mod4, -1, 1, 0.1, 0.23);
					detune = LinLin.kr(mod3, -1, 1, 0, 0.015);
					focus = -1 * LinLin.kr(mod3, -1, 1, 0, 1);
					div = LinLin.kr(mod3, -1, 1, 1, 7).sqrt;
					cons = mod4.linlin(-1, 1, -0.5, 0.8);

					f1 = LinSelectX.kr(voice, LinSelectX.kr(vowel, [f1a, f1b, f1c].flop));
					f2 = LinSelectX.kr(voice, LinSelectX.kr(vowel, [f2a, f2b, f2c].flop));
					f3 = LinSelectX.kr(voice, LinSelectX.kr(vowel, [f3a, f3b, f3c].flop));
					f4 = LinSelectX.kr(voice, LinSelectX.kr(vowel, [f4a, f4b, f4c].flop));
					a1 = 1;
					a2 = tilt;
					a3 = tilt ** 1.5;
					a4 = tilt ** 2;
					q1 = reso;
					q2 = q1/1.5;
					q3 = q2/1.5;
					q4 = reso/10;

					env = EnvGen.ar(Env.adsr(attack,decay,sustain,release), gate, doneAction: 2);

					saw = VarSaw.ar(hz * (1 + (detune * [-1, 0.7, -0.3, 0, 0.3, -0.7, 1])), width: 0).collect({ |item, index|
						Pan2.ar(item, index.linlin(0, 6, -1, 1) * SinOsc.kr(Rand.new(0.1, 0.3)) * focus)
					});

					wiggle = EnvGen.kr(Env.perc(attackTime: 0.0, releaseTime: 0.15), doneAction: Done.none);

					snd = HPF.ar(
						Mix.new(BBandPass.ar(saw, ([
							f1,
							f2 * (1 + (cons * wiggle)),
							f3,
							f4]!2).flop,
						([q1, q2, q3, q4]!2).flop) * ([a1, a2, a3, a4]!2).flop),
						20);

					snd = Balance2.ar(snd[0], snd[1], Lag.kr(pan, 0.1)).tanh;
					snd = snd * env * amp * vel * -3.dbamp;

					Out.ar(out, snd);
					Out.ar(sendABus, sendA * snd);
					Out.ar(sendBBus, sendB * snd);
				}).add;


				SynthDef(\mx_triangles,{
					arg out = 0, sendABus = 0, sendBBus = 0, sendA = 0, sendB = 0,
					hz = 220, amp = 1.0, vel = 1.0, pan = 0, sub = 0,
					gate = 1, attack = 0.01, decay = 0.2, sustain = 0.9, release = 5,
					mod1 = 0, mod2 = 0, mod3 = 0, mod4 = 0;

					var snd, env, bellow_env, bellow,
					detune_cents, detune_semitones,
					f_cents, freq_a, freq_b, decimation_bits, decimation_rate,
					noise_level, vibrato_rate, vibrato_depth;

					env = EnvGen.ar(Env.adsr(attack + 0.05, decay, sustain, release), gate, doneAction: 2);

					mod1 = Lag.kr(mod1); mod2 = Lag.kr(mod2); mod3 = Lag.kr(mod3); mod4 = Lag.kr(mod4);

					bellow = LinLin.kr(mod1, -1, 1, 0, 1);
					decimation_bits = LinLin.kr(mod2, -1, 1, 24, 4);
					decimation_rate = LinLin.kr(mod2, -1, 1, 44100, 8000);
					noise_level = LinLin.kr(mod2, -1, 1, 0, 0.5);
					detune_semitones = LinLin.kr(mod3, -1, 1, -24, 24);
					vibrato_rate = LinLin.kr(mod4, -1, 1, 0, 5);
					vibrato_depth = LinExp.kr(mod4, -1, 1, 0.01, 0.5);

					freq_a = hz;
					freq_b = (hz.cpsmidi + detune_semitones.round).midicps;
					bellow_env = EnvGen.kr(Env.step([bellow, 1 - bellow], [attack, release], 1), gate: gate);

					snd = Mix.new([
						SelectX.ar(bellow_env.lag, [
							DPW3Tri.ar(freq_a),
							DPW3Tri.ar(freq_b)
						]),
						PinkNoise.ar(noise_level),
					]);

					snd = Vibrato.ar(snd, vibrato_rate, vibrato_depth);
					snd = Decimator.ar(snd, decimation_rate, decimation_bits);
					snd = Pan2.ar(snd, Lag.kr(pan, 0.1));
					snd = snd * env * amp * vel * -12.dbamp;

					Out.ar(out, snd);
					Out.ar(sendABus, sendA * snd);
					Out.ar(sendBBus, sendB * snd);
				}).add;


				// add OSC msg
				OSCFunc.new({ |msg, time, addr, recvPort|
					var synDef = msg[1].asSymbol;
					var i = msg[2].asInteger;
					var hz = msg[3].asFloat;
					var vel = msg[4].asFloat;
					synthVoices[i].set(\gate, -1.05);
					Synth.new(synDef,
						[
							\hz, hz,
							\vel, vel,
							\sendABus, ~sendA ? Server.default.outputBus,
							\sendBBus, ~sendB ? Server.default.outputBus,
					    ] ++ synthParams.getPairs, target: synthVoices[i]
                    );
				}, "/nb_mxsynths/note_on");

				OSCFunc.new({ |msg, time, addr, recvPort|
					var i = msg[1].asInteger;
					synthVoices[i].set(\gate, 0);
				}, "/nb_mxsynths/note_off");

				OSCFunc.new({ |msg, time, addr, recvPort|
					var key = msg[1].asSymbol;
					var val = msg[2].asFloat;
					numVoices.do({ arg i;
						synthVoices[i].set(key, val);
					});
					synthParams[key] = val;
				}, "/nb_mxsynths/set_param");

				OSCFunc.new({ |msg, time, addr, recvPort|
					synthGroup.set(\gate, -1.05);
				}, "/nb_mxsynths/panic");

				OSCFunc.new({ |msg, time, addr, recvPort|
					synthGroup.free;
				}, "/nb_mxsynths/free");

			}
		}
	}
}
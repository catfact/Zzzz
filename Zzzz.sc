Zzzz {
	var <>s;
	var <>g;
	var <>b;

	var <>nframes;
	var <>buf;
	var <>out;

	var <>z; // last state;
	var <>h; // state history

	*new { ^super.new.init }

	init {
		var now;
		s = Server.default;
		s.postln;
		s.sampleRate.postln;

		nframes = s.sampleRate * 60 * 8;
		buf = Buffer.alloc(s, nframes);

		g = Group.new(s);
		b = Bus.audio(s, 2);

		out = SynthDef.new(\clamp, {
			arg in, out, pre=1, post=1, lim=0.97, limdur=0.04;
			var snd = Limiter.ar(In.ar(in, 2) * pre, lim, limdur).clip(-0.99, 0.99);
			Out.ar(out, snd *post);
		}).play(target:g, args:[\in, b.index, \out, 0], addAction:\addAfter);

		SynthDef.new(\rec1, { arg buf, ch=0, rec=1, pre=0;
			var snd = LeakDC.ar(SoundIn.ar(ch));
			RecordBuf.ar(snd, buf, recLevel:rec, preLevel:pre, loop:0, doneAction:2);
		}).send(s);

		SynthDef.new(\play, {
			arg buf, out=0, amp=1, pan=0, rate=1,
			start=0, dur=1, atk=0.3, rel=0.4, lpfmin=20, lpfmax=10000;
			var env, snd;
			snd = PlayBuf.ar(1, buf, rate, startPos:start, loop:0, doneAction:2);
			dur = (dur/rate.abs).min(BufDur.kr(buf)-start);
			atk = dur*atk;
			rel = dur*rel;
			env = EnvGen.ar(Env.linen(atk, dur-(atk+rel), rel, curve:\sine), doneAction:2);
			snd = LPF.ar(snd, env.linlin(0, 1, lpfmin, lpfmax));
			Out.ar(out, Pan2.ar(amp * env * snd, pan));
		}).send(s);

		now = SystemClock.seconds;
		z = (cap0:now, start:now, dur:1, rat:1);
		h = List.new;
	}

	cap {
		arg ch=1;
		z.cap0 = SystemClock.seconds;
		"cap: ".post; z.postln;
		Synth.new(\rec1, [\ch, ch, \buf, buf.bufnum], g, \addBefore);
	}

	dub { arg ch=1;
		Synth.new(\rec1, [\ch, ch, \buf, buf.bufnum, \pre, 1], g);
		z.cap0 = SystemClock.seconds;
		"dub: ".post; z.postln;
	}

	back { arg win, rate=1, amp=0.25;
		var now, start, end;
		now = SystemClock.seconds;
		end = now - z.cap0;
		if (win.isNil, {
			start = 0;
		}, {
			start = (end - win).max(0);
		});
		z.start = start;
		z.dur = end-start;
		z.rat = rate;
		"back ".post; z.postln;
		h.add(z.copy);
		Synth.new(\play, [\buf, buf.bufnum, \out, b.index, \amp, amp,
			\start, z.start, \dur, z.dur, \rate, z.rat], g);
	}

	since {
		arg rate=1, amp=0.25;
		var now = SystemClock.seconds;
		this.back(now - (z.start + z.dur));

	}


	again { arg rate = nil;
		if (rate.notNil, {
			z.rat = rate;
		}, {
			rate = z.rat;
		});
		"again ".post; z.postln;
		Synth.new(\play, [\buf, buf.bufnum, \out, b.index,
			\start, z.start, \dur, z.dur, \rate, rate], g);
	}

}


ZzzzWorch {

}
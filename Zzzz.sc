Zzzz {
}

ZzzzBaskil {
	var <>s;
	var <>g;
	var <>b;

	var <>nframes;
	var <>buf;
	var <>out;

	var <>z; // last state;
	var <>h; // state history
	var <>rs; // sequencing routines

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

		out = SynthDef.new(\ZzzzBaskil_clamp, {
			arg in, out, pre=1, post=1, lim=0.97, limdur=0.04;
			var snd = Limiter.ar(In.ar(in, 2) * pre, lim, limdur).clip(-0.99, 0.99);
			Out.ar(out, snd *post);
		}).play(target:g, args:[\in, b.index, \out, 0], addAction:\addAfter);

		SynthDef.new(\ZzzzBaskil_rec1, { arg buf, ch=0, rec=1, pre=0, atk=1;
			var recenv = Line.kr(0, 1, atk);
			var snd = LeakDC.ar(SoundIn.ar(ch));
			RecordBuf.ar(snd, buf, recLevel:rec*recenv, preLevel:pre, loop:0, doneAction:2);
		}).send(s);

		SynthDef.new(\ZzzzBaskil_play, {
			arg buf, out=0, amp=1, pan=0, rate=1,
			start=0, dur=1, atk=0.3, rel=0.4, lpfmin=20, lpfmax=10000, hpf=10;
			var env, snd;
			snd = PlayBuf.ar(1, buf, rate, startPos:start, loop:0, doneAction:2);
			dur = (dur/rate.abs).min(BufDur.kr(buf)-start);
			atk = dur*atk;
			rel = dur*rel;
			env = EnvGen.ar(Env.linen(atk, dur-(atk+rel), rel, curve:\sine), doneAction:2);
			snd = LPF.ar(snd, env.linlin(0, 1, lpfmin, lpfmax));
			snd = HPF.ar(snd, hpf);
			Out.ar(out, Pan2.ar(amp * env * snd, pan));
		}).send(s);

		now = SystemClock.seconds;
		z = (cap0:now, start:now, dur:1, rat:1);
		h = List.new;
		rs = List.new;
	}

	cap {
		arg ch=1;
		z.cap0 = SystemClock.seconds;
		"cap: ".post; z.postln;
		Synth.new(\ZzzzBaskil_rec1, [\ch, ch, \buf, buf.bufnum], g, \addBefore);
	}

	dub {
		arg ch=1;
		Synth.new(\ZzzzBaskil_rec1, [\ch, ch, \buf, buf.bufnum, \pre, 1], g);
		z.cap0 = SystemClock.seconds;
		"dub: ".post; z.postln;
	}

	back {
		arg rate=1, amp= -12, hpf=10, win;
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
		Synth.new(\ZzzzBaskil_play, [
			\buf, buf.bufnum, \out, b.index, \amp, amp.dbamp,
			\start, z.start, \dur, z.dur, \rate, z.rat
		], g);
	}

	since {
		arg rate=1, amp= -12;
		var now = SystemClock.seconds;
		this.back(rate, amp, win:now - (z.start + z.dur));
	}

	again {
		arg rate = nil, amp= -12;
		if (rate.notNil, {
			z.rat = rate;
		}, {
			rate = z.rat;
		});
		"again ".post; z.postln;
		Synth.new(\ZzzzBaskil_play, [
			\buf, buf.bufnum, \out, b.index, \amp, amp.dbamp,
			\start, z.start, \dur, z.dur, \rate, z.rat
		], g);
	}

	redos {
		arg rmul=1, dtmul=1, amp= -12;
		var r = Routine {
			inf.do {
				h.do({
					arg ev;
					ev.postln;
					Synth.new(\ZzzzBaskil_play, [\buf, buf.bufnum, \out, b.index, \amp, amp.dbamp,
						\pan , 0.5.rand2,
						\start, ev.start, \dur, ev.dur, \rate, ev.rat * rmul], g);
					(ev.dur * dtmul).wait;
				});
			}
		}.play;
		rs.add(r);

	}

	stoprs {
		rs.do({ arg r; r.stop; });
	}

}


ZzzzWorch {
	var <>buf;

}

ZzzzRakt {
	classvar <didInit;
	classvar <>h, <t0;
	classvar <win, <txt;

	*initClass { didInit = false; }

	*init {
		if (didInit.not, {
			this.initDoc;
			this.initWin;
			didInit = true;
		});
	}

	*initDoc {
		postln("ZzzzRakt.initDoc");
		h = List.new;
		t0 = SystemClock.seconds;
		Document.globalKeyDownAction_({
			arg doc, char, mod, uni, key;
			var t = SystemClock.seconds;
			[char, key].postln;
			h.add([t-t0, key]);
			//this.fresh;
			t0 = t;
		});
	}

	*initWin {
		var h=256, w=256;
		postln("ZzzzRakt.initWin");
		win = Window("-",  Rect(128, 64, w, h));
		txt = StaticText(win, Rect(0, 0, w, h));
		txt.background_(Color.black);
		txt.stringColor_(Color.white);
		win.alwaysOnTop_(true);
		win.front;
	}

	*fresh {
		var i, n, str, key, ch;
		n = h.size;
		n.postln;
		str = "";
		i=(n-11).max(0);
		while({i<n}, {
			key = h[i][1];
			ch = switch(key,
				{65362}, {"↑"},
				{65364}, {"↓"},
				{65361}, {"←"},
				{65363}, {"→"},
				{65505}, {"⇧"},
				{65506}, {"⇧"},
				{65506}, {"⇧"},
				{65293}, {"¬"},
				{""++key.asAscii}
			);
			str = str ++ ch ++ "\t" ++ h[i][0] ++ "\n";
			i = i + 1;
		});
		txt.string_(str);
		//h.postln;
		nil
	}
}
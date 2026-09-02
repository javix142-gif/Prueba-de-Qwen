#!/usr/bin/env bash
set -euo pipefail
A="${1:-app/src/main/assets/audio}"
mkdir -p "$A"
make_track(){
  local name="$1" f1="$2" f2="$3" trem="$4"
  ffmpeg -hide_banner -loglevel error -y \
    -f lavfi -i "sine=frequency=${f1}:duration=8:sample_rate=44100" \
    -f lavfi -i "sine=frequency=${f2}:duration=8:sample_rate=44100" \
    -f lavfi -i "anoisesrc=color=brown:duration=8:sample_rate=44100" \
    -filter_complex "[0:a]volume=0.055,afade=t=in:d=0.7,afade=t=out:st=7.3:d=0.7[a0];[1:a]volume=0.025,tremolo=f=${trem}:d=0.35,afade=t=in:d=1.2,afade=t=out:st=7:d=1[a1];[2:a]lowpass=f=900,volume=0.012[a2];[a0][a1][a2]amix=inputs=3:normalize=0" \
    -c:a libvorbis -q:a 2 "$A/${name}.ogg"
}
make_sfx(){
  local name="$1" freq="$2" dur="$3" st
  st="$(python3 - <<PY
print(max(0,float('$dur')-0.08))
PY
)"
  ffmpeg -hide_banner -loglevel error -y -f lavfi -i "sine=frequency=${freq}:duration=${dur}:sample_rate=44100" \
    -af "afade=t=out:st=${st}:d=0.08,volume=0.22" -c:a libvorbis -q:a 3 "$A/${name}.ogg"
}
make_track title 196 293 0.20
make_track liria 220 330 0.25
make_track liria_attack 110 165 0.55
make_track cyrion 174 261 0.18
make_track aureval 247 370 0.22
make_track vesperia 196 392 0.32
make_track serath 262 393 0.15
make_track keldran 123 185 0.28
make_track depths 98 147 0.12
make_track boss 92 138 0.65
make_sfx swing 420 0.18
make_sfx heavy 185 0.28
make_sfx hit 120 0.16
make_sfx crit 710 0.22
make_sfx skill 520 0.30
make_sfx ultimate 880 0.42
make_sfx dash 300 0.15
make_sfx hurt 90 0.22
make_sfx pickup 960 0.16

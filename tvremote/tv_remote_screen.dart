import 'package:bonsoir/bonsoir.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../providers/remote_provider.dart';
import '../providers/remote_state.dart';
import '../providers/settings_provider.dart';

class C {
  static const bg = Color(0xFF111015);
  static const top = Color(0xFF343339);
  static const card = Color(0xFF2E2D33);
  static const card2 = Color(0xFF3A393F);
  static const border = Color(0xFF55535C);
  static const accent = Color(0xFFFF9900);
  static const accentSoft = Color(0xFFFFC56D);
  static const text = Color(0xFFF2F0F4);
  static const muted = Color(0xFFAAA7AE);
  static const disabled = Color(0xFF5C5A61);
  static const danger = Color(0xFFEA4A45);
  static const green = Color(0xFF55C64C);
}

class TVRemoteScreen extends ConsumerWidget {
  const TVRemoteScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state = ref.watch(remoteNotifierProvider);
    return Scaffold(
      backgroundColor: C.bg,
      body: SafeArea(
        child: switch (state) {
          Scanning(discoveredTvs: final tvs) => _DiscoveryView(tvs: tvs),
          PairingRequired(tv: final tv, error: final error) => _PairView(tv: tv, error: error),
          Connecting(tv: final tv) => _ConnectingView(tv: tv),
          Connected(tv: final tv) => _RemoteView(tv: tv),
          Disconnected(reason: final reason) => _DisconnectedView(reason: reason),
        },
      ),
    );
  }
}

class _DiscoveryView extends ConsumerWidget {
  final List<BonsoirService> tvs;
  const _DiscoveryView({required this.tvs});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final n = ref.read(remoteNotifierProvider.notifier);
    return Column(
      children: [
        _Header(
          title: 'Buscar Smart TV',
          subtitle: 'Android TV / Google TV · ideal para Caixun',
          connected: false,
          onReconnect: n.restartScanning,
        ),
        Expanded(
          child: RefreshIndicator(
            onRefresh: () async => n.restartScanning(),
            color: C.accent,
            backgroundColor: C.card,
            child: ListView(
              physics: const AlwaysScrollableScrollPhysics(),
              padding: const EdgeInsets.fromLTRB(20, 28, 20, 32),
              children: [
                const Icon(Icons.tv_rounded, size: 58, color: C.accent),
                const SizedBox(height: 14),
                const Text(
                  'Selecciona tu televisor',
                  textAlign: TextAlign.center,
                  style: TextStyle(color: C.text, fontSize: 24, fontWeight: FontWeight.w700),
                ),
                const SizedBox(height: 8),
                const Text(
                  'El teléfono y la TV deben estar conectados a la misma red Wi‑Fi. La app busca el servicio remoto nativo de Android/Google TV.',
                  textAlign: TextAlign.center,
                  style: TextStyle(color: C.muted, fontSize: 14, height: 1.45),
                ),
                const SizedBox(height: 28),
                if (tvs.isEmpty)
                  Container(
                    padding: const EdgeInsets.all(24),
                    decoration: BoxDecoration(
                      color: C.card,
                      borderRadius: BorderRadius.circular(20),
                      border: Border.all(color: C.border),
                    ),
                    child: const Column(
                      children: [
                        SizedBox(
                          width: 30,
                          height: 30,
                          child: CircularProgressIndicator(strokeWidth: 3, color: C.accent),
                        ),
                        SizedBox(height: 16),
                        Text('Buscando televisores…', style: TextStyle(color: C.text, fontWeight: FontWeight.w600)),
                        SizedBox(height: 8),
                        Text(
                          'Si tu Caixun no aparece, verifica que sea Google TV o Android TV y que esté encendido.',
                          textAlign: TextAlign.center,
                          style: TextStyle(color: C.muted, height: 1.35),
                        ),
                      ],
                    ),
                  )
                else
                  ...tvs.map((tv) => Padding(
                        padding: const EdgeInsets.only(bottom: 12),
                        child: Material(
                          color: C.card,
                          borderRadius: BorderRadius.circular(18),
                          child: InkWell(
                            borderRadius: BorderRadius.circular(18),
                            onTap: () => n.connectToTv(tv),
                            child: Container(
                              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 15),
                              decoration: BoxDecoration(
                                borderRadius: BorderRadius.circular(18),
                                border: Border.all(color: C.border),
                              ),
                              child: Row(
                                children: [
                                  Container(
                                    width: 48,
                                    height: 48,
                                    decoration: const BoxDecoration(color: C.card2, shape: BoxShape.circle),
                                    child: const Icon(Icons.tv, color: C.accent),
                                  ),
                                  const SizedBox(width: 14),
                                  Expanded(
                                    child: Column(
                                      crossAxisAlignment: CrossAxisAlignment.start,
                                      children: [
                                        Text(tv.name, style: const TextStyle(color: C.text, fontSize: 16, fontWeight: FontWeight.w700)),
                                        const SizedBox(height: 3),
                                        Text(tv.host ?? 'TV encontrada en la red local', style: const TextStyle(color: C.muted, fontSize: 12)),
                                      ],
                                    ),
                                  ),
                                  const Icon(Icons.chevron_right_rounded, color: C.accent),
                                ],
                              ),
                            ),
                          ),
                        ),
                      )),
                const SizedBox(height: 18),
                OutlinedButton.icon(
                  onPressed: n.restartScanning,
                  icon: const Icon(Icons.refresh_rounded),
                  label: const Text('Volver a buscar'),
                  style: OutlinedButton.styleFrom(
                    foregroundColor: C.accent,
                    side: const BorderSide(color: C.accent),
                    padding: const EdgeInsets.symmetric(vertical: 14),
                  ),
                ),
              ],
            ),
          ),
        ),
      ],
    );
  }
}

class _PairView extends ConsumerStatefulWidget {
  final BonsoirService tv;
  final String? error;
  const _PairView({required this.tv, this.error});

  @override
  ConsumerState<_PairView> createState() => _PairViewState();
}

class _PairViewState extends ConsumerState<_PairView> {
  final controller = TextEditingController();

  @override
  void dispose() {
    controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final n = ref.read(remoteNotifierProvider.notifier);
    return Column(
      children: [
        _Header(title: widget.tv.name, subtitle: 'Emparejamiento seguro', connected: false, onReconnect: n.cancelPairing),
        Expanded(
          child: ListView(
            padding: const EdgeInsets.all(24),
            children: [
              const SizedBox(height: 30),
              const Icon(Icons.phonelink_lock_rounded, color: C.accent, size: 62),
              const SizedBox(height: 22),
              const Text('Mira la pantalla de la TV', textAlign: TextAlign.center, style: TextStyle(color: C.text, fontSize: 23, fontWeight: FontWeight.w700)),
              const SizedBox(height: 10),
              const Text(
                'La TV mostrará un código de 6 caracteres. Escríbelo aquí para autorizar este teléfono. Solo se hace una vez.',
                textAlign: TextAlign.center,
                style: TextStyle(color: C.muted, fontSize: 14, height: 1.45),
              ),
              const SizedBox(height: 28),
              TextField(
                controller: controller,
                textCapitalization: TextCapitalization.characters,
                textAlign: TextAlign.center,
                maxLength: 6,
                style: const TextStyle(color: C.text, fontSize: 28, fontWeight: FontWeight.w800, letterSpacing: 8),
                decoration: InputDecoration(
                  counterText: '',
                  hintText: 'A1B2C3',
                  hintStyle: const TextStyle(color: C.disabled, letterSpacing: 7),
                  filled: true,
                  fillColor: C.card,
                  enabledBorder: OutlineInputBorder(borderRadius: BorderRadius.circular(18), borderSide: const BorderSide(color: C.border)),
                  focusedBorder: OutlineInputBorder(borderRadius: BorderRadius.circular(18), borderSide: const BorderSide(color: C.accent, width: 2)),
                ),
              ),
              if (widget.error != null) ...[
                const SizedBox(height: 10),
                Text(widget.error!, textAlign: TextAlign.center, style: const TextStyle(color: C.danger)),
              ],
              const SizedBox(height: 18),
              FilledButton(
                onPressed: () {
                  final pin = controller.text.trim().toUpperCase();
                  if (pin.length == 6) n.submitPin(pin);
                },
                style: FilledButton.styleFrom(backgroundColor: C.accent, foregroundColor: Colors.black, padding: const EdgeInsets.symmetric(vertical: 15)),
                child: const Text('EMPAREJAR', style: TextStyle(fontWeight: FontWeight.w800, letterSpacing: .8)),
              ),
              const SizedBox(height: 12),
              TextButton(onPressed: n.cancelPairing, child: const Text('Cancelar', style: TextStyle(color: C.muted))),
            ],
          ),
        ),
      ],
    );
  }
}

class _ConnectingView extends StatelessWidget {
  final BonsoirService tv;
  const _ConnectingView({required this.tv});

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        _Header(title: tv.name, subtitle: 'Conectando…', connected: false),
        const Expanded(
          child: Center(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                CircularProgressIndicator(color: C.accent),
                SizedBox(height: 18),
                Text('Abriendo control remoto seguro', style: TextStyle(color: C.text, fontSize: 16, fontWeight: FontWeight.w600)),
              ],
            ),
          ),
        ),
      ],
    );
  }
}

class _DisconnectedView extends ConsumerWidget {
  final String? reason;
  const _DisconnectedView({this.reason});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final n = ref.read(remoteNotifierProvider.notifier);
    return Column(
      children: [
        const _Header(title: 'Sin conexión', subtitle: 'Control remoto', connected: false),
        Expanded(
          child: Center(
            child: Padding(
              padding: const EdgeInsets.all(28),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  const Icon(Icons.link_off_rounded, color: C.danger, size: 58),
                  const SizedBox(height: 18),
                  const Text('Se perdió la conexión con la TV', textAlign: TextAlign.center, style: TextStyle(color: C.text, fontSize: 21, fontWeight: FontWeight.w700)),
                  if (reason != null) ...[
                    const SizedBox(height: 10),
                    Text(reason!, textAlign: TextAlign.center, style: const TextStyle(color: C.muted, height: 1.4)),
                  ],
                  const SizedBox(height: 24),
                  FilledButton.icon(
                    onPressed: n.resetAndScan,
                    icon: const Icon(Icons.refresh),
                    label: const Text('BUSCAR TV'),
                    style: FilledButton.styleFrom(backgroundColor: C.accent, foregroundColor: Colors.black),
                  ),
                ],
              ),
            ),
          ),
        ),
      ],
    );
  }
}

class _RemoteView extends ConsumerStatefulWidget {
  final BonsoirService tv;
  const _RemoteView({required this.tv});

  @override
  ConsumerState<_RemoteView> createState() => _RemoteViewState();
}

class _RemoteViewState extends ConsumerState<_RemoteView> {
  int mode = 0;

  void key(int code) {
    if (ref.read(settingsProvider).hapticsEnabled) HapticFeedback.selectionClick();
    ref.read(remoteNotifierProvider.notifier).sendKeyPress(code);
  }

  @override
  Widget build(BuildContext context) {
    final n = ref.read(remoteNotifierProvider.notifier);
    return Column(
      children: [
        _Header(
          title: widget.tv.name,
          subtitle: 'Conectado',
          connected: true,
          onReconnect: n.resetAndScan,
        ),
        Expanded(
          child: SingleChildScrollView(
            padding: const EdgeInsets.fromLTRB(20, 18, 20, 28),
            child: Column(
              children: [
                Row(
                  children: [
                    _RoundButton(icon: Icons.power_settings_new_rounded, iconColor: C.danger, size: 62, onTap: () => key(26)),
                    const Spacer(),
                    _ModeSwitch(mode: mode, onChanged: (m) => setState(() => mode = m)),
                    const Spacer(),
                    _RoundButton(icon: Icons.mic_rounded, size: 62, onTap: () => key(84)),
                  ],
                ),
                const SizedBox(height: 18),
                AnimatedSwitcher(
                  duration: const Duration(milliseconds: 180),
                  child: switch (mode) {
                    0 => _DPad(key: key),
                    1 => _TouchPad(key: key),
                    _ => _NumberPad(key: key),
                  },
                ),
                const SizedBox(height: 22),
                Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    _VerticalRocker(
                      topText: '+',
                      centerText: 'VOL',
                      bottomText: '−',
                      onTop: () => key(24),
                      onBottom: () => key(25),
                      onTopStart: () => n.startVolumeRepeat(24),
                      onBottomStart: () => n.startVolumeRepeat(25),
                      onEnd: n.stopVolumeRepeat,
                    ),
                    const Spacer(),
                    Column(
                      children: [
                        Row(
                          children: [
                            _RoundButton(icon: Icons.reply_rounded, size: 70, onTap: () => key(4)),
                            const SizedBox(width: 64),
                            _RoundButton(icon: Icons.home_rounded, size: 70, onTap: () => key(3)),
                          ],
                        ),
                        const SizedBox(height: 14),
                        Row(
                          children: [
                            _RoundButton(icon: Icons.volume_off_rounded, size: 64, onTap: () => key(164)),
                            const SizedBox(width: 72),
                            _RoundButton(icon: Icons.play_arrow_rounded, size: 64, onTap: () => key(85)),
                          ],
                        ),
                      ],
                    ),
                    const Spacer(),
                    _VerticalRocker(
                      topText: '▲',
                      centerText: 'CH',
                      bottomText: '▼',
                      disabledStyle: false,
                      onTop: () => key(166),
                      onBottom: () => key(167),
                    ),
                  ],
                ),
                const SizedBox(height: 22),
                Row(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    _OutlineCircle(icon: Icons.keyboard_alt_outlined, onTap: () => setState(() => mode = 2)),
                    const SizedBox(width: 20),
                    _ColorKeysButton(key: key),
                    const SizedBox(width: 20),
                    _OutlineCircle(icon: Icons.input_rounded, onTap: () => key(178)),
                  ],
                ),
                const SizedBox(height: 22),
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    _TextPill(text: 'Menu', onTap: () => key(82)),
                    _TextPill(text: 'Info', onTap: () => key(165)),
                    _TextPill(text: 'Guía', onTap: () => key(172)),
                    _TextPill(text: 'Exit', onTap: () => key(4)),
                  ],
                ),
              ],
            ),
          ),
        ),
        const _BottomBar(),
      ],
    );
  }
}

class _Header extends StatelessWidget {
  final String title;
  final String subtitle;
  final bool connected;
  final VoidCallback? onReconnect;
  const _Header({required this.title, required this.subtitle, required this.connected, this.onReconnect});

  @override
  Widget build(BuildContext context) {
    return Container(
      height: 86,
      padding: const EdgeInsets.symmetric(horizontal: 22),
      color: C.top,
      child: Row(
        children: [
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
            decoration: BoxDecoration(
              color: const Color(0xFFFFD68B),
              borderRadius: BorderRadius.circular(11),
              border: Border.all(color: C.accent),
            ),
            child: const Row(
              children: [
                Icon(Icons.tv_rounded, size: 17, color: Colors.black87),
                SizedBox(width: 6),
                Text('CAIXUN+', style: TextStyle(color: Colors.black87, fontWeight: FontWeight.w900, fontSize: 12)),
              ],
            ),
          ),
          const SizedBox(width: 18),
          Expanded(
            child: Container(
              height: 54,
              padding: const EdgeInsets.symmetric(horizontal: 15),
              decoration: BoxDecoration(
                color: const Color(0xFF525157),
                borderRadius: BorderRadius.circular(28),
                border: Border.all(color: const Color(0xFF69686D)),
              ),
              child: Row(
                children: [
                  Container(width: 10, height: 10, decoration: BoxDecoration(color: connected ? C.green : C.accent, shape: BoxShape.circle)),
                  const SizedBox(width: 10),
                  Expanded(
                    child: Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(title, maxLines: 1, overflow: TextOverflow.ellipsis, style: const TextStyle(color: C.text, fontSize: 16, fontWeight: FontWeight.w800)),
                        Text(subtitle, maxLines: 1, overflow: TextOverflow.ellipsis, style: const TextStyle(color: Color(0xFFD1CED4), fontSize: 10)),
                      ],
                    ),
                  ),
                  if (connected) const Icon(Icons.keyboard_arrow_down_rounded, color: C.text),
                ],
              ),
            ),
          ),
          const SizedBox(width: 14),
          IconButton(onPressed: onReconnect, icon: const Icon(Icons.cast_rounded, color: C.text, size: 28)),
        ],
      ),
    );
  }
}

class _ModeSwitch extends StatelessWidget {
  final int mode;
  final ValueChanged<int> onChanged;
  const _ModeSwitch({required this.mode, required this.onChanged});

  @override
  Widget build(BuildContext context) {
    final icons = [Icons.control_camera_rounded, Icons.touch_app_outlined, Icons.dialpad_rounded];
    return Container(
      height: 56,
      padding: const EdgeInsets.all(2),
      decoration: BoxDecoration(color: C.card, borderRadius: BorderRadius.circular(28)),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: List.generate(3, (i) {
          final active = mode == i;
          return GestureDetector(
            onTap: () => onChanged(i),
            child: AnimatedContainer(
              duration: const Duration(milliseconds: 160),
              width: 64,
              height: 52,
              decoration: BoxDecoration(color: active ? C.accent : Colors.transparent, borderRadius: BorderRadius.circular(26)),
              child: Icon(icons[i], color: active ? Colors.white : C.muted, size: 26),
            ),
          );
        }),
      ),
    );
  }
}

class _DPad extends StatelessWidget {
  final ValueChanged<int> key;
  const _DPad({required this.key, super.key});

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      key: const ValueKey('dpad'),
      height: 300,
      child: Stack(
        alignment: Alignment.center,
        children: [
          Container(
            width: 300,
            height: 300,
            decoration: BoxDecoration(shape: BoxShape.circle, color: C.card, border: Border.all(color: C.accentSoft, width: 1.5)),
          ),
          _Arrow(icon: Icons.keyboard_arrow_up_rounded, alignment: const Alignment(0, -0.72), onTap: () => key(19)),
          _Arrow(icon: Icons.keyboard_arrow_down_rounded, alignment: const Alignment(0, 0.72), onTap: () => key(20)),
          _Arrow(icon: Icons.keyboard_arrow_left_rounded, alignment: const Alignment(-0.72, 0), onTap: () => key(21)),
          _Arrow(icon: Icons.keyboard_arrow_right_rounded, alignment: const Alignment(0.72, 0), onTap: () => key(22)),
          GestureDetector(
            onTap: () => key(23),
            child: Container(
              width: 146,
              height: 146,
              decoration: BoxDecoration(shape: BoxShape.circle, color: C.card2, border: Border.all(color: C.accentSoft, width: 1.5)),
              child: const Center(child: Text('OK', style: TextStyle(color: C.accent, fontSize: 24, fontWeight: FontWeight.w900))),
            ),
          ),
        ],
      ),
    );
  }
}

class _Arrow extends StatelessWidget {
  final IconData icon;
  final Alignment alignment;
  final VoidCallback onTap;
  const _Arrow({required this.icon, required this.alignment, required this.onTap});

  @override
  Widget build(BuildContext context) {
    return Align(
      alignment: alignment,
      child: IconButton(onPressed: onTap, icon: Icon(icon, size: 34, color: C.accent)),
    );
  }
}

class _TouchPad extends StatefulWidget {
  final ValueChanged<int> key;
  const _TouchPad({required this.key, super.key});
  @override
  State<_TouchPad> createState() => _TouchPadState();
}

class _TouchPadState extends State<_TouchPad> {
  Offset start = Offset.zero;
  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      key: const ValueKey('touch'),
      onTap: () => widget.key(23),
      onPanStart: (d) => start = d.localPosition,
      onPanEnd: (d) {
        final v = d.velocity.pixelsPerSecond;
        if (v.distance < 180) return;
        if (v.dx.abs() > v.dy.abs()) {
          widget.key(v.dx > 0 ? 22 : 21);
        } else {
          widget.key(v.dy > 0 ? 20 : 19);
        }
      },
      child: Container(
        height: 300,
        width: double.infinity,
        decoration: BoxDecoration(color: C.card, borderRadius: BorderRadius.circular(24), border: Border.all(color: C.accentSoft, width: 1.5)),
        child: Stack(
          children: [
            ...List.generate(28, (i) {
              final row = i ~/ 7;
              final col = i % 7;
              return Positioned(
                left: 42.0 + col * 48,
                top: 48.0 + row * 62,
                child: Container(width: 11, height: 11, decoration: const BoxDecoration(color: Color(0xFF5A5960), shape: BoxShape.circle)),
              );
            }),
            const Center(child: Text('Desliza o toca para seleccionar.', style: TextStyle(color: C.text, fontSize: 20, fontWeight: FontWeight.w700))),
          ],
        ),
      ),
    );
  }
}

class _NumberPad extends StatelessWidget {
  final ValueChanged<int> key;
  const _NumberPad({required this.key, super.key});

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      key: const ValueKey('numbers'),
      height: 340,
      child: Column(
        children: [
          for (final row in [[1,2,3],[4,5,6],[7,8,9]]) ...[
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceEvenly,
              children: row.map((n) => _NumberButton(number: n, onTap: () => key(7 + n))).toList(),
            ),
            const SizedBox(height: 14),
          ],
          _NumberButton(number: 0, onTap: () => key(7)),
        ],
      ),
    );
  }
}

class _NumberButton extends StatelessWidget {
  final int number;
  final VoidCallback onTap;
  const _NumberButton({required this.number, required this.onTap});

  @override
  Widget build(BuildContext context) {
    return InkWell(
      borderRadius: BorderRadius.circular(40),
      onTap: onTap,
      child: Container(
        width: 108,
        height: 68,
        decoration: BoxDecoration(borderRadius: BorderRadius.circular(40), border: Border.all(color: C.accentSoft, width: 1.5)),
        child: Center(child: Text('$number', style: const TextStyle(color: C.text, fontSize: 23, fontWeight: FontWeight.w700))),
      ),
    );
  }
}

class _VerticalRocker extends StatelessWidget {
  final String topText;
  final String centerText;
  final String bottomText;
  final VoidCallback onTop;
  final VoidCallback onBottom;
  final VoidCallback? onTopStart;
  final VoidCallback? onBottomStart;
  final VoidCallback? onEnd;
  final bool disabledStyle;
  const _VerticalRocker({
    required this.topText,
    required this.centerText,
    required this.bottomText,
    required this.onTop,
    required this.onBottom,
    this.onTopStart,
    this.onBottomStart,
    this.onEnd,
    this.disabledStyle = false,
  });

  Widget segment(String text, VoidCallback tap, VoidCallback? start) => Expanded(
        child: GestureDetector(
          onTap: tap,
          onLongPressStart: start == null ? null : (_) => start(),
          onLongPressEnd: onEnd == null ? null : (_) => onEnd!(),
          child: Center(child: Text(text, style: TextStyle(color: disabledStyle ? C.disabled : C.text, fontSize: 24, fontWeight: FontWeight.w700))),
        ),
      );

  @override
  Widget build(BuildContext context) {
    return Container(
      width: 88,
      height: 190,
      decoration: BoxDecoration(color: C.card, borderRadius: BorderRadius.circular(44)),
      child: Column(
        children: [
          segment(topText, onTop, onTopStart),
          Text(centerText, style: TextStyle(color: disabledStyle ? C.disabled : C.text, fontSize: 21, fontWeight: FontWeight.w800)),
          segment(bottomText, onBottom, onBottomStart),
        ],
      ),
    );
  }
}

class _RoundButton extends StatelessWidget {
  final IconData icon;
  final VoidCallback onTap;
  final double size;
  final Color iconColor;
  const _RoundButton({required this.icon, required this.onTap, this.size = 62, this.iconColor = C.text});

  @override
  Widget build(BuildContext context) {
    return InkWell(
      customBorder: const CircleBorder(),
      onTap: onTap,
      child: Container(
        width: size,
        height: size,
        decoration: const BoxDecoration(shape: BoxShape.circle, color: C.card),
        child: Icon(icon, color: iconColor, size: size * .42),
      ),
    );
  }
}

class _OutlineCircle extends StatelessWidget {
  final IconData icon;
  final VoidCallback onTap;
  const _OutlineCircle({required this.icon, required this.onTap});

  @override
  Widget build(BuildContext context) {
    return InkWell(
      customBorder: const CircleBorder(),
      onTap: onTap,
      child: Container(
        width: 82,
        height: 82,
        decoration: BoxDecoration(shape: BoxShape.circle, border: Border.all(color: C.border, width: 1.5)),
        child: Icon(icon, color: C.text, size: 28),
      ),
    );
  }
}

class _ColorKeysButton extends StatelessWidget {
  final ValueChanged<int> key;
  const _ColorKeysButton({required this.key});

  @override
  Widget build(BuildContext context) {
    return PopupMenuButton<int>(
      color: C.card,
      onSelected: key,
      itemBuilder: (_) => const [
        PopupMenuItem(value: 183, child: Text('Rojo', style: TextStyle(color: Colors.redAccent))),
        PopupMenuItem(value: 184, child: Text('Verde', style: TextStyle(color: Colors.greenAccent))),
        PopupMenuItem(value: 185, child: Text('Amarillo', style: TextStyle(color: Colors.amberAccent))),
        PopupMenuItem(value: 186, child: Text('Azul', style: TextStyle(color: Colors.lightBlueAccent))),
      ],
      child: Container(
        width: 82,
        height: 82,
        decoration: BoxDecoration(shape: BoxShape.circle, border: Border.all(color: C.border, width: 1.5)),
        child: Center(
          child: Row(
            mainAxisSize: MainAxisSize.min,
            children: const [
              _Dot(Colors.red),
              _Dot(Colors.green),
              _Dot(Colors.amber),
              _Dot(Colors.blue),
            ],
          ),
        ),
      ),
    );
  }
}

class _Dot extends StatelessWidget {
  final Color color;
  const _Dot(this.color);
  @override
  Widget build(BuildContext context) => Container(width: 7, height: 7, margin: const EdgeInsets.symmetric(horizontal: 1), color: color);
}

class _TextPill extends StatelessWidget {
  final String text;
  final VoidCallback onTap;
  const _TextPill({required this.text, required this.onTap});

  @override
  Widget build(BuildContext context) {
    return OutlinedButton(
      onPressed: onTap,
      style: OutlinedButton.styleFrom(
        foregroundColor: C.text,
        side: const BorderSide(color: C.accentSoft),
        shape: const StadiumBorder(),
        minimumSize: const Size(76, 48),
      ),
      child: Text(text, style: const TextStyle(fontWeight: FontWeight.w700)),
    );
  }
}

class _BottomBar extends StatelessWidget {
  const _BottomBar();
  @override
  Widget build(BuildContext context) {
    return Container(
      height: 84,
      color: C.top,
      child: const Row(
        mainAxisAlignment: MainAxisAlignment.spaceAround,
        children: [
          _BottomItem(icon: Icons.settings_remote_rounded, label: 'Remoto', active: true),
          _BottomItem(icon: Icons.grid_view_rounded, label: 'Apps'),
          _BottomItem(icon: Icons.connected_tv_rounded, label: 'Proyectar'),
          _BottomItem(icon: Icons.tune_rounded, label: 'Configuración'),
        ],
      ),
    );
  }
}

class _BottomItem extends StatelessWidget {
  final IconData icon;
  final String label;
  final bool active;
  const _BottomItem({required this.icon, required this.label, this.active = false});
  @override
  Widget build(BuildContext context) {
    return Opacity(
      opacity: active ? 1 : .45,
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(icon, color: active ? C.accent : C.text, size: 27),
          const SizedBox(height: 5),
          Text(label, style: TextStyle(color: active ? C.accent : C.text, fontSize: 12, fontWeight: FontWeight.w800)),
        ],
      ),
    );
  }
}

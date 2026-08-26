import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'src/services/certificate_manager.dart';
import 'src/ui/tv_remote_screen.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  try {
    await CertificateManager().preGenerate();
  } catch (_) {}
  runApp(const ProviderScope(child: CaixunRemoteApp()));
}

class CaixunRemoteApp extends StatelessWidget {
  const CaixunRemoteApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Control Remoto Caixun+',
      debugShowCheckedModeBanner: false,
      themeMode: ThemeMode.dark,
      theme: ThemeData(
        brightness: Brightness.dark,
        useMaterial3: true,
        colorScheme: const ColorScheme.dark(
          primary: Color(0xFFFF9900),
          secondary: Color(0xFFFFC56D),
          surface: Color(0xFF2E2D33),
        ),
        scaffoldBackgroundColor: const Color(0xFF111015),
      ),
      home: const TVRemoteScreen(),
    );
  }
}

import 'dart:async';
import 'package:flutter/services.dart';

class UssdService {
  static const _ch = MethodChannel('com.wbrandt/ussd_service');

  /// Hace una petición USSD y devuelve el texto de respuesta.
  /// [subscriptionId] usa la SIM por defecto si es null o < 0.
  static Future<String> makeRequest({
    required int subscriptionId,
    required String code,
    Duration timeout = const Duration(seconds: 10),
  }) async {
    final res = await _ch.invokeMethod<String>('makeRequest', <String, dynamic>{
      'subscriptionId': subscriptionId,
      'code': code,
    }).timeout(timeout);
    return res ?? '';
  }

  /// Devuelve el subscriptionId de voz por defecto (o -1 si no hay).
  static Future<int> defaultSubscriptionId() async {
    final res = await _ch.invokeMethod<int>('defaultSubscriptionId');
    return res ?? -1;
  }
}

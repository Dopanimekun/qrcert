# QR Verifier — Android

App Android para verificar QR Codes assinados com ECDSA P-256, gerados pelo emissor HTML da PoC.

## Requisitos

- Android Studio Hedgehog ou superior
- Android 15 (API 35) no dispositivo / emulador
- Google Play Services (necessário para ML Kit)

## Como buildar

```bash
# Clone ou extraia o projeto
cd qr-verifier

# Build debug APK
./gradlew assembleDebug

# Instalar direto no dispositivo conectado
./gradlew installDebug
```

O APK de debug fica em: `app/build/outputs/apk/debug/app-debug.apk`

## Fluxo de uso

1. Abrir o app → câmera traseira inicia automaticamente
2. Apontar para um QR Code gerado pelo emissor HTML
3. ML Kit detecta o QR e extrai o payload `ID|DADO|ASSINATURA_HEX`
4. A verificação ECDSA acontece localmente (sem rede) usando `java.security`
5. Resultado exibido:
   - ✅ **ACESSO AUTORIZADO** — assinatura válida + nível de acesso (Admin / Financeiro / Visitante)
   - ❌ **ASSINATURA INVÁLIDA** — dado adulterado ou chave errada
   - ⚠️ **EMISSOR DESCONHECIDO** — ID não está no TrustStore
   - ⚠️ **QR INVÁLIDO** — não segue o formato da PoC
6. Botão "Escanear outro QR" volta para a câmera

## Adicionando emissores reais

Edite `TrustStore.kt` e adicione uma entrada no mapa `entries`:

```kotlin
"04" to CertEntry(
    id = "04",
    name = "RECEPCAO",
    role = AccessRole.VISITANTE,
    pubHex = "SUA_CHAVE_PUBLICA_P256_128_CHARS_HEX_SEM_PREFIXO_04"
)
```

A chave pública é o campo `pub` do emissor HTML, **removendo o prefixo `04`** (primeiros 2 chars).

## Gerando um par de chaves P-256 válido (para substituir as chaves simuladas)

```bash
# Gerar chave privada
openssl ecparam -name prime256v1 -genkey -noout -out priv.pem

# Extrair chave pública
openssl ec -in priv.pem -pubout -out pub.pem

# Ver os bytes da chave pública (formato uncompressed = 04 + X + Y)
openssl ec -in priv.pem -text -noout
```

O valor `d` (hex) vai para o campo `d` do emissor HTML.
Os bytes `pub X` e `pub Y` concatenados (128 hex chars) vão para `pubHex` no `TrustStore.kt`.

## Segurança

- ✅ Chave privada **nunca** está no app Android — somente a pública
- ✅ Verificação ECDSA feita localmente via `java.security` (sem dependências externas)
- ⚠️ Produção: adicionar timestamp + validade no payload para evitar replay attacks
- ⚠️ Os certificados 02 e 03 usam chaves simuladas — substituir antes de qualquer uso real

## Estrutura do projeto

```
app/src/main/
├── java/com/mssp/qrverifier/
│   ├── crypto/
│   │   └── TrustStore.kt        # Chaves públicas + lógica ECDSA verify
│   ├── model/
│   │   └── QrVerifier.kt        # Parser de payload + orquestração
│   └── ui/
│       └── MainActivity.kt      # CameraX + ML Kit + UI de resultado
└── res/
    ├── layout/activity_main.xml
    ├── values/{colors,themes}.xml
    └── drawable/                # Ícones vetoriais
```

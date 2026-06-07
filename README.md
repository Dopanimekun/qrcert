# 📱 QR Verifier — Android

<p align="center">
  <img src="https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android" />
  <img src="https://img.shields.io/badge/Kotlin-B125EA?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin" />
  <img src="https://img.shields.io/badge/Gradle-02303A?style=for-the-badge&logo=gradle&logoColor=white" alt="Gradle" />
  <img src="https://img.shields.io/badge/ML_Kit-4285F4?style=for-the-badge&logo=google&logoColor=white" alt="ML Kit" />
</p>

## 📖 Sobre o Projeto

Este aplicativo Android atua como a contraparte de validação da Prova de Conceito (PoC) de Defesa Cibernética. Ele é responsável por ler e verificar QR Codes assinados digitalmente com o algoritmo **ECDSA P-256**, gerados pelo emissor HTML do projeto.

A verificação ocorre de forma totalmente local, sem a necessidade de conexão com a internet (offline), assegurando que o dado lido é autêntico e não foi adulterado.

O emissor para este aplicativo encontra-se neste [repositório](https://github.com/Dopanimekun/qr-code-cert).

---

## ⚠️ Avisos de Segurança (PoC)

> **Atenção:** Este aplicativo foi desenvolvido como uma **Prova de Conceito acadêmica**.
> * **Replay Attacks:** Atualmente, não há validação de *timestamp* ou prazo de validade no payload, o que torna o sistema vulnerável a ataques de repetição em um ambiente de produção.
> * **Chaves de Teste:** Os certificados com ID `02` e `03` utilizam chaves simuladas. **É obrigatório** substituí-las antes de qualquer uso em cenário real.
> * **Isolamento de Chaves:** Apenas chaves públicas residem no aplicativo. A chave privada **nunca** deve ser incluída no código Android.

---

## ⚙️ Requisitos do Sistema

Para compilar e rodar este projeto, você precisará de:

* **IDE:** Android Studio Hedgehog ou superior.
* **Dispositivo/Emulador:** Sistema rodando Android 15 (API 35).
* **Dependências:** Google Play Services atualizado no dispositivo (necessário para o funcionamento do ML Kit).

---

## 🚀 Como Buildar e Instalar

Clone ou extraia o projeto e abra o terminal na raiz do diretório `qr-verifier`:

```bash
# Navegue até a pasta do projeto
cd qr-verifier

# Gere o APK de Debug
./gradlew assembleDebug

# Instale diretamente no dispositivo conectado via ADB
./gradlew installDebug
````

> **Nota:** O arquivo APK gerado ficará disponível no diretório: `app/build/outputs/apk/debug/app-debug.apk`.

## 📱 Fluxo de Uso

1. **Início:** Ao abrir o app, a câmera traseira do dispositivo é iniciada automaticamente.
    
2. **Leitura:** Aponte a câmera para um QR Code gerado pelo emissor HTML.
    
3. **Extração:** O Google ML Kit detecta o QR Code e extrai o payload no formato `ID|DADO|ASSINATURA_HEX`.
    
4. **Verificação Offline:** O app utiliza a biblioteca nativa `java.security` para validar a assinatura ECDSA localmente.
    
5. **Resultados Possíveis:**
    
    - ✅ **ACESSO AUTORIZADO:** A assinatura é válida. O app exibe o nível de acesso associado à chave (Admin, Financeiro, Visitante, etc.).
        
    - ❌ **ASSINATURA INVÁLIDA:** O dado foi adulterado ou assinado por uma chave incorreta.
        
    - ⚠️ **EMISSOR DESCONHECIDO:** O ID do emissor não está cadastrado no `TrustStore` do app.
        
    - ⚠️ **QR INVÁLIDO:** A estrutura do texto do QR Code não segue o formato definido pela PoC.
        
6. **Novo Ciclo:** O usuário pode clicar em "Escanear outro QR" para reativar a câmera.
    

## 🔐 Gerenciamento de Chaves (TrustStore)

### 1. Gerando um par de chaves P-256 real

Para substituir as chaves simuladas, utilize o OpenSSL para gerar um novo par:

```bash
# 1. Gerar a chave privada
openssl ecparam -name prime256v1 -genkey -noout -out priv.pem

# 2. Extrair a chave pública
openssl ec -in priv.pem -pubout -out pub.pem

# 3. Ver os bytes da chave pública (formato uncompressed = 04 + X + Y)
openssl ec -in priv.pem -text -noout
```

- **No emissor HTML:** O valor `d` (hexadecimal) vai para o campo `d` do gerador.
    
- **No Android:** Os bytes `pub X` e `pub Y` concatenados (128 caracteres hexadecimais) serão usados no aplicativo.
    

### 2. Adicionando emissores no App

Edite o arquivo `TrustStore.kt` e adicione uma nova entrada no mapa `entries`:

```Kotlin
"04" to CertEntry(
    id = "04",
    name = "RECEPCAO",
    role = AccessRole.VISITANTE,
    pubHex = "SUA_CHAVE_PUBLICA_P256_128_CHARS_HEX_SEM_PREFIXO_04"
)
```

> **Importante:** A chave pública a ser inserida no `pubHex` é a mesma do campo `pub` do emissor HTML, porém **você deve remover o prefixo `04`** (os 2 primeiros caracteres).

## 📂 Estrutura de Diretórios

A arquitetura do projeto segue o padrão Android padrão, focada na clareza e separação de responsabilidades:

```Plaintext
app/src/main/
├── java/com/mssp/qrverifier/
│   ├── crypto/
│   │   └── TrustStore.kt        # Armazena as chaves públicas e contém a lógica de verificação ECDSA
│   ├── model/
│   │   └── QrVerifier.kt        # Realiza o parser do payload e a orquestração dos dados
│   └── ui/
│       └── MainActivity.kt      # Controla o CameraX, a integração com ML Kit e a UI de resultados
└── res/
    ├── layout/
    │   └── activity_main.xml    # Estrutura visual da tela
    ├── values/
    │   └── {colors,themes}.xml  # Definições de design e identidade visual
    └── drawable/                # Ícones vetoriais do sistema
```

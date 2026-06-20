# 🔐 QRCert — Sistema de Autenticação por QR Code com Assinaturas Digitais (PoC)

<p align="center">
  <img src="https://img.shields.io/badge/HTML5-E34F26?style=for-the-badge&logo=html5&logoColor=white" alt="HTML5" />
  <img src="https://img.shields.io/badge/JavaScript-F7DF1E?style=for-the-badge&logo=javascript&logoColor=black" alt="JavaScript" />
  <img src="https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android" />
  <img src="https://img.shields.io/badge/Kotlin-B125EA?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin" />
  <img src="https://img.shields.io/badge/ML_Kit-4285F4?style=for-the-badge&logo=google&logoColor=white" alt="ML Kit" />
  <img src="https://img.shields.io/badge/ECDSA-P--256-blue?style=for-the-badge" alt="ECDSA P-256" />
</p>

## 📖 Sobre o Projeto

Este projeto consiste em uma **Prova de Conceito (PoC)** desenvolvida no contexto de um **Trabalho de Conclusão de Curso em Defesa Cibernética**, demonstrando um sistema de autenticação baseado em **QR Codes assinados digitalmente**.

A proposta é aplicar conceitos similares aos utilizados em certificados digitais SSL/TLS no contexto de QR Codes: um emissor assina criptograficamente as informações contidas no código utilizando uma chave privada, enquanto um verificador valida sua autenticidade utilizando a chave pública correspondente.

O sistema é dividido em três componentes principais:

- 🌐 **Emissor Web:** responsável pela geração do QR Code e assinatura digital do conteúdo utilizando uma chave privada.
- 📱 **Verificador Android:** aplicação nativa responsável pela leitura do QR Code e validação da assinatura utilizando um TrustStore local.
- 🌍 **Verificador PWA:** aplicação web instalável capaz de realizar a leitura do QR Code pela câmera do dispositivo e verificar sua autenticidade utilizando a Web Crypto API.

Toda a validação ocorre localmente, sem necessidade de conexão com a internet, tornando o processo adequado para ambientes com acesso restrito ou indisponibilidade de rede.

---

# ✨ Funcionalidades

## 🌐 Emissor Web

- Geração de QR Codes contendo dados de identificação e autorização.
- Assinatura digital utilizando o algoritmo **ECDSA P-256**.
- Gerenciamento de certificados de demonstração.
- Interface simples baseada em HTML e JavaScript puro.
- Disponibilização via GitHub Pages para testes rápidos.

## 📱 Aplicativo Android

- Leitura de QR Codes utilizando Google ML Kit.
- Extração do payload no formato:

```
ID|DADO|ASSINATURA_HEX
```

- Verificação criptográfica offline utilizando `java.security`.
- Controle de emissores confiáveis através de um TrustStore local.
- Exibição do nível de acesso associado ao certificado.
- Feedback visual indicando sucesso ou falha da autenticação.

## 🌍 Verificador PWA

- Instalação como Progressive Web App (PWA) em dispositivos móveis.
- Leitura de QR Codes utilizando a câmera do dispositivo através das APIs nativas do navegador.
- Decodificação dos QR Codes utilizando a biblioteca jsQR.
- Verificação criptográfica local utilizando Web Crypto API.
- Validação de assinaturas ECDSA P-256 com SHA-256.
- Utilização de um TrustStore compartilhado com os certificados públicos confiáveis.
- Funcionamento totalmente offline após carregamento da aplicação.
- Histórico dos QR Codes escaneados durante a sessão.
- Interface otimizada para dispositivos móveis.
- Fallback para validação manual do payload quando necessário.

---

# 🔐 Fluxo de Funcionamento

                    Chave Privada
                         |
                         v
            +------------------------+
            |      Emissor Web        |
            | Dados + Assinatura ECC  |
            +------------------------+
                         |
                         v
                  QR Code Assinado
                         |
              +----------+-----------+
              |                      |
              v                      v
     +----------------+     +----------------+
     |  Android App   |     |  Verificador   |
     | CameraX        |     |      PWA       |
     | ML Kit         |     | jsQR + Web API |
     +----------------+     +----------------+
              |                      |
              +----------+-----------+
                         |
                         v
               Verificação ECDSA P-256
                  com chave pública
                         |
                  +------+------+
                  |             |
                  v             v
               Válido       Inválido
               Acesso       Bloqueio

# ⚠️ Considerações de Segurança

> **Este projeto é uma Prova de Conceito acadêmica e não deve ser utilizado em produção sem adaptações.**

Algumas limitações conhecidas incluem:

- Não existe controle de validade temporal do QR Code, permitindo potenciais ataques de repetição (*Replay Attack*).
- Algumas chaves presentes no projeto são apenas exemplos e devem ser substituídas em um cenário real.
- A chave privada deve permanecer exclusivamente no emissor, nunca sendo distribuída para clientes ou aplicativos verificadores.
- O aplicativo Android armazena apenas chaves públicas de emissores confiáveis.

---

# 🔑 Gerenciamento de Chaves

O projeto utiliza curvas elípticas **ECDSA P-256 (prime256v1)**.

## Gerando um novo par de chaves

```bash
# Gerar chave privada
openssl ecparam -name prime256v1 -genkey -noout -out priv.pem

# Extrair chave pública
openssl ec -in priv.pem -pubout -out pub.pem

# Visualizar componentes da chave
openssl ec -in priv.pem -text -noout
```

## Configuração no sistema

### Emissor Web

A chave privada é utilizada para assinar os dados presentes no QR Code.

### Aplicativo Android

A chave pública deve ser adicionada ao `TrustStore`:

```kotlin
"04" to CertEntry(
    id = "04",
    name = "RECEPCAO",
    role = AccessRole.VISITANTE,
    pubHex = "CHAVE_PUBLICA_P256_HEX"
)
```

O valor armazenado em `pubHex` deve conter apenas os bytes X e Y da chave pública, removendo o prefixo `04`.

---

# 📂 Estrutura do Projeto

```plaintext
/
├── emitter-web/
│   ├── index.html
│   ├── keys.js
│   └── demais arquivos HTML/CSS/JS
│
├── verifier-pwa/
│   ├── index.html
│   ├── keys.js
│   ├── manifest.json
│   └── demais arquivos PWA
│
└── verifier-android/
    └── app/src/main/
        ├── java/com/mssp/qrverifier/
        │   ├── crypto/
        │   ├── model/
        │   └── ui/
        └── res/
```

> A estrutura pode variar dependendo da organização adotada para a unificação dos repositórios.

---

# 🛠 Tecnologias Utilizadas

## Web

- HTML5
- CSS3
- JavaScript (Vanilla)
- Bibliotecas de geração de QR Code

## Android

- Kotlin
- Android SDK
- CameraX
- Google ML Kit
- Java Cryptography Architecture (JCA)

## Verificador PWA

- HTML5
- CSS3
- JavaScript (Vanilla)
- Progressive Web App (PWA)
- Web Crypto API
- MediaDevices API
- jsQR

## Criptografia

- ECDSA P-256
- Assinaturas digitais
- Certificados baseados em chave pública

---

# 🚀 Como Executar

## Emissor Web

Entre no diretório do emissor e abra o arquivo principal:

```bash
cd web
```

Abra o arquivo `index.html` no navegador.

Também é possível acessar a demonstração via GitHub Pages, ou iniciando um servidor web com:

```bash
python3 -m http.server 8080
```

## Verficador PWA

Entre no diretório do verificador:

```bash
cd verificador-pwa
```

Também é possível acessá-lo via GitHub Pages, abrindo o arquivo direto no navegador ou iniciando um servidor web com:

```bash
python3 -m http.server 8080QRCert — Sistema de Autenticação por QR Code com Assinaturas Digitais (PoC)
```

---

## Aplciativo Verificador Android

Entre no diretório do aplicativo:

```bash
cd android
```

Compile o APK:

```bash
./gradlew assembleDebug
```

Instale em um dispositivo conectado:

```bash
./gradlew installDebug
```

O APK gerado ficará disponível em:

```
app/build/outputs/apk/debug/app-debug.apk
```

---

# 📄 Licença

Este projeto está sob a licença **GPL-3.0**.

Consulte o arquivo `LICENSE` para mais informações.
